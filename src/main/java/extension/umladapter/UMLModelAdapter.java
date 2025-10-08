package extension.umladapter;


import extension.ast.node.LangASTNode;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangExpressionStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangASTUtil;
import extension.base.LangSupportedEnum;
import extension.umladapter.processor.UMLAdapterVariableProcessor;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

import org.refactoringminer.util.PathFileUtils;

import static extension.umladapter.UMLAdapterUtil.extractUMLImports;
import static extension.umladapter.processor.UMLAdapterVariableProcessor.processVariableDeclarations;

public class UMLModelAdapter {

    private final UMLModel umlModel;
    private String language;

    private static final Logger LOGGER = Logger.getLogger(UMLModelAdapter.class.getName());


    public UMLModelAdapter(Map<String, String> langSupportedFiles) throws IOException {
        // Parse files to custom AST
        Map<String, LangASTNode> langASTMap = parseLangSupportedFiles(langSupportedFiles);

        // Create UML model directly from custom AST
        umlModel = createUMLModel(langASTMap, langSupportedFiles);
    }

    private Map<String, LangASTNode> parseLangSupportedFiles(Map<String, String> langSupportedFiles) throws IOException {
        Map<String, LangASTNode> result = new HashMap<>();

        for (Map.Entry<String, String> entry : langSupportedFiles.entrySet()) {
            this.language = LangSupportedEnum.fromFileName(entry.getKey()).name();
            LangASTNode ast = LangASTUtil.getLangAST(
                    entry.getKey(), // fileName for language detection
                    new StringReader(entry.getValue())); // code content
           // System.out.print("AST Structure: " + ast.toString());
            result.put(entry.getKey(), ast);
        }

        return result;
    }


    private UMLModel createUMLModel(Map<String, LangASTNode> astMap, Map<String, String> langSupportedFiles) {
        UMLModel model = new UMLModel(Collections.emptySet());

        // Process each AST and populate the UML model
        for (Map.Entry<String, LangASTNode> entry : astMap.entrySet()) {
            String filename = entry.getKey();
            LangASTNode ast = entry.getValue();

            // Extract UML entities from AST
            extractUMLEntities(ast, model, filename, langSupportedFiles.get(filename));
        }

        return model;
    }

    private void extractUMLEntities(LangASTNode ast, UMLModel model, String filename, String fileContent) {
        if (ast instanceof LangCompilationUnit compilationUnit) {
            // Process imports
            List<UMLImport> imports = extractUMLImports(compilationUnit, filename);

            for (LangTypeDeclaration typeDecl : compilationUnit.getTypes()) {
                UMLClass umlClass = createUMLClass(model, typeDecl, filename, imports, fileContent);
                model.addClass(umlClass);
            }

            // Handle top level methods
            if (compilationUnit.getMethods() != null && !compilationUnit.getMethods().isEmpty()){
                handleTopLevelMethods(model, filename, compilationUnit, imports, fileContent);
            }
        }
    }

    private void handleTopLevelMethods(UMLModel model, String filename, LangCompilationUnit compilationUnit, List<UMLImport> imports, String fileContent) {
        List<LangMethodDeclaration> topLevelMethods = compilationUnit.getMethods();
        UMLClass moduleClass = createModuleClass(compilationUnit, filename, imports);

        moduleClass.setActualSignature(moduleClass.getName());
        moduleClass.setVisibility(Visibility.PUBLIC);
        moduleClass.setAbstract(false);
        moduleClass.setInterface(false);
        moduleClass.setFinal(false);
        moduleClass.setStatic(true);
        moduleClass.setAnnotation(false);
        moduleClass.setEnum(false);
        moduleClass.setRecord(false);

        if (!topLevelMethods.isEmpty()) {
            String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
            String filepath = UMLAdapterUtil.extractFilePath(filename);
            for (LangMethodDeclaration method : topLevelMethods) {
                UMLOperation operation = createUMLOperation(method, moduleClass.getName(),
                        sourceFolder, filepath, fileContent);
                for (LangAnnotation langAnnotation : method.getAnnotations()) {
                    operation.addAnnotation(new UMLAnnotation(
                            method.getRootCompilationUnit(),
                            sourceFolder,
                            filepath,
                            langAnnotation,
                            fileContent));
                }
                moduleClass.addOperation(operation);
            }
        }
        model.addClass(moduleClass);
    }

    private UMLClass createModuleClass(LangCompilationUnit compilationUnit, String filename, List<UMLImport> imports) {
        String moduleName = UMLAdapterUtil.extractModuleName(filename);
        String packageName = UMLAdapterUtil.extractPackageName(filename);
        String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
        String filepath = UMLAdapterUtil.extractFilePath(filename);

        LocationInfo locationInfo = new LocationInfo(sourceFolder, filepath, compilationUnit,
                LocationInfo.CodeElementType.TYPE_DECLARATION);

        String moduleClassName = moduleName + ".__module__";

        UMLClass moduleClass = new UMLClass(packageName, moduleClassName, locationInfo, true, imports);
        moduleClass.setStatic(true);

        return moduleClass;
    }

    private UMLClass createUMLClass(UMLModel model, LangTypeDeclaration typeDecl, String filename, List<UMLImport> imports, String fileContent) {

        String className = typeDecl.getName();

        String packageName = UMLAdapterUtil.extractPackageName(filename);
        String sourceFolder = UMLAdapterUtil.extractSourceFolder(filename);
        String filepath = UMLAdapterUtil.extractFilePath(filename);

        LocationInfo locationInfo = new LocationInfo(sourceFolder,
                filepath,
                typeDecl,
                LocationInfo.CodeElementType.TYPE_DECLARATION);

        UMLClass umlClass = new UMLClass(packageName, className, locationInfo, typeDecl.isTopLevel(), imports);

        for (LangAnnotation langAnnotation : typeDecl.getAnnotations()) {
            umlClass.addAnnotation(new UMLAnnotation(
                    typeDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filepath,
                    langAnnotation,
                    fileContent));
        }

        if (!typeDecl.getSuperClassNames().isEmpty()) {
            // Qualify and set the first superclass as the main superclass
            String primarySuperClassRaw = typeDecl.getSuperClassNames().get(0);
            String primarySuperClass = UMLAdapterUtil.resolveQualifiedTypeName(primarySuperClassRaw, imports, packageName);
            UMLType superClassType = UMLType.extractTypeObject(primarySuperClass);
            umlClass.setSuperclass(superClassType);
            model.addGeneralization(new UMLGeneralization(umlClass, primarySuperClass));

            // For additional base classes, also add as generalizations (Python multiple inheritance)
            for (int i = 1; i < typeDecl.getSuperClassNames().size(); i++) {
                String additionalSuperClassRaw = typeDecl.getSuperClassNames().get(i);
                String additionalSuperClass = UMLAdapterUtil.resolveQualifiedTypeName(additionalSuperClassRaw, imports, packageName);
                // Create additional generalization for multiple inheritance support
                model.addGeneralization(new UMLGeneralization(umlClass, additionalSuperClass));
            }
        }

      //  storeClassHierarchyInfo(umlClass, typeDecl, model, packageName, imports);

        // Handle class-scope assignments as attributes
        List<UMLAttribute> classLevelAttributes = new ArrayList<>();
        for (LangAssignment classLevelAssignment: typeDecl.getClassLevelAssignments()){
            processClassLevelAssignmentForAttribute(typeDecl, classLevelAssignment, classLevelAttributes, sourceFolder, filepath, null, fileContent);
        }

        // Setters
        umlClass.setActualSignature(typeDecl.getActualSignature());
        umlClass.setVisibility(typeDecl.getVisibility());
        umlClass.setAbstract(typeDecl.isAbstract());
        umlClass.setInterface(typeDecl.isInterface());
        umlClass.setFinal(typeDecl.isFinal());
        umlClass.setStatic(typeDecl.isStatic());
        umlClass.setAnnotation(typeDecl.isAnnotation());
        umlClass.setEnum(typeDecl.isEnum());
        umlClass.setRecord(typeDecl.isRecord());

        for (LangMethodDeclaration methodDecl : typeDecl.getMethods()) {
            UMLOperation umlOperation = createUMLOperation(methodDecl, className, sourceFolder, filepath, fileContent);
            umlClass.addOperation(umlOperation);
            if ("__init__".equals(methodDecl.getName())) {
                List<UMLAttribute> attributes = getAttributes(methodDecl, sourceFolder, filepath, umlOperation, fileContent);
                for (UMLAttribute attribute : attributes) {
                    attribute.setClassName(className);
                    umlClass.addAttribute(attribute);
                }
            }
        }
        return umlClass;
    }

    private UMLOperation createUMLOperation(LangMethodDeclaration methodDecl, String className, String sourceFolder, String filePath, String fileContent) {
        int startSignatureOffset = methodDecl.getStartChar();
        LocationInfo locationInfo = new LocationInfo(sourceFolder, filePath, methodDecl, LocationInfo.CodeElementType.METHOD_DECLARATION);

        String operationName = PathFileUtils.isPythonFile(filePath) ? methodDecl.getCleanName() : methodDecl.getName();
        UMLOperation umlOperation = new UMLOperation(operationName, locationInfo);
        umlOperation.setClassName(className);

        // Convert to UMLAnnotations
        for (LangAnnotation langAnnotation : methodDecl.getAnnotations()) {
            umlOperation.addAnnotation(new UMLAnnotation(
                    methodDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    langAnnotation,
                    fileContent));
        }

        List<LangSingleVariableDeclaration> params = methodDecl.getParameters();
        List<String> parameterNames = new ArrayList<>();

        int paramOffset = UMLAdapterUtil.getParamOffset(methodDecl, params, language);

        for (int i = paramOffset; i < params.size(); i++) {
            LangSingleVariableDeclaration param = params.get(i);
            UMLType typeObject = UMLType.extractTypeObject("Object");
            LocationInfo paramLocationInfo = new LocationInfo(sourceFolder, filePath, param, LocationInfo.CodeElementType.TYPE);
            if (LangSupportedEnum.PYTHON.name().equals(language)) {
                if (param.getTypeAnnotation() != null) {
                    typeObject = UMLType.extractTypeObject(param.getTypeAnnotation().getName(), "[", "]", paramLocationInfo);
                }
            } else {
                if (param.getTypeAnnotation() != null) {
                    String typeName = param.getTypeAnnotation().getName();
                    if (typeName != null && !typeName.isEmpty()) {
                        typeObject = UMLType.extractTypeObject(typeName, "[", "]", paramLocationInfo);
                    }
                }
            }

            UMLParameter umlParam = new UMLParameter(param.getLangSimpleName().getIdentifier(), typeObject, "parameter", param.isVarArgs());
            processVariableDeclarations(param, umlParam, typeObject, sourceFolder, filePath, methodDecl, umlOperation, fileContent);
            umlOperation.addParameter(umlParam);
            parameterNames.add(param.getLangSimpleName().getIdentifier());
        }

        umlOperation.setFinal(methodDecl.isFinal());
        umlOperation.setStatic(methodDecl.isStatic());
        umlOperation.setConstructor(methodDecl.isConstructor());
        umlOperation.setVisibility(methodDecl.getVisibility());
        umlOperation.setAbstract(methodDecl.isAbstract());
        umlOperation.setNative(methodDecl.isNative());
        umlOperation.setSynchronized(methodDecl.isSynchronized());

        processComments(methodDecl, sourceFolder, filePath, umlOperation);

        UMLType returnType;
        LocationInfo returnTypeLocationInfo = new LocationInfo(sourceFolder, filePath, methodDecl, LocationInfo.CodeElementType.TYPE);
        if (LangSupportedEnum.PYTHON.name().equals(language)){
            returnType = UMLType.extractTypeObject(methodDecl.getReturnTypeAnnotation(), "[", "]", returnTypeLocationInfo);
        } else {
            String resolvedReturnType = methodDecl.getReturnTypeAnnotation();
            if (resolvedReturnType == null || resolvedReturnType.isEmpty()) {
                resolvedReturnType = TypeObjectEnum.VOID.name();
            }
            returnType = UMLType.extractTypeObject(resolvedReturnType, "[", "]", returnTypeLocationInfo);
            if (methodDecl.getReturnTypeAnnotation() == null) {
                methodDecl.setReturnTypeAnnotation(resolvedReturnType);
            }
        }
        String returnTypeString = methodDecl.getReturnTypeAnnotation() != null ? methodDecl.getReturnTypeAnnotation() : "void";
        if (!(TypeObjectEnum.VOID.name().equals(returnTypeString))) {
            UMLParameter returnParam = new UMLParameter("", returnType, "return", false);
            umlOperation.addParameter(returnParam);
        }


        OperationBody opBody = new OperationBody(
                methodDecl.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                methodDecl.getBody(),
                umlOperation,
                getAttributes(methodDecl, sourceFolder, filePath, umlOperation, fileContent),
                fileContent
        );

        umlOperation.setBody(opBody);
        //logUMLOperation(umlOperation, methodDecl);
        int endSignatureOffset = methodDecl.getBody() != null ?
                methodDecl.getBody().getStartChar() + 1 :
                methodDecl.getStartChar() + methodDecl.getLength();
        String text = fileContent.substring(startSignatureOffset, endSignatureOffset);
        umlOperation.setActualSignature(text);
        return umlOperation;
    }

    private List<UMLAttribute> getAttributes(LangMethodDeclaration methodDecl, String sourceFolder, String filePath, UMLOperation umlOperation, String fileContent) {
        List<UMLAttribute> attributes = new ArrayList<>();

        // Only process __init__ method for attribute extraction
        if (!"__init__".equals(methodDecl.getName())) {
            return attributes;
        }

        LangBlock methodBody = methodDecl.getBody();
        if (methodBody == null) {
            return attributes;
        }

        if (methodBody.getStatements() != null) {
            for (LangASTNode statement : methodBody.getStatements()) {
                // Handle direct assignments
                if (statement instanceof LangAssignment assignment) {
                    processAssignmentForAttribute(methodDecl, assignment, attributes, sourceFolder, filePath, umlOperation, fileContent);
                }
                // Handle expression statements that contain assignments
                else if (statement instanceof LangExpressionStatement exprStmt) {
                    if (exprStmt.getExpression() instanceof LangAssignment assignment) {
                        processAssignmentForAttribute(methodDecl, assignment, attributes, sourceFolder, filePath, umlOperation, fileContent);
                    }
                }
            }
        }

        return attributes;
    }

    private void processAssignmentForAttribute(LangMethodDeclaration methodDeclaration, LangAssignment assignment, List<UMLAttribute> attributes,
                                               String sourceFolder, String filePath, UMLOperation umlOperation, String fileContent) {
        LangASTNode leftSide = assignment.getLeftSide();

        if (leftSide instanceof LangFieldAccess langFieldAccess) {
            LangASTNode expression = langFieldAccess.getExpression();

            // Check if it's self.attribute
            if (expression instanceof LangSimpleName simpleName) {
                if ("self".equals(simpleName.getIdentifier())) {
                    String attributeName = langFieldAccess.getName().getIdentifier();

                    // Create VariableDeclaration for the attribute using the new constructor
                    VariableDeclaration variableDeclaration = UMLAdapterVariableProcessor.processAttributeAssignment(
                            assignment,
                            sourceFolder,
                            filePath,
                            attributeName,
                            umlOperation,
                            fileContent
                    );


                    // Create UMLAttribute
                    LocationInfo attributeLocationInfo = new LocationInfo(
                            assignment.getRootCompilationUnit(),
                            sourceFolder,
                            filePath,
                            langFieldAccess,
                            LocationInfo.CodeElementType.FIELD_DECLARATION
                    );
                    UMLAttribute attribute = new UMLAttribute(
                            attributeName,
                            variableDeclaration.getType(),
                            attributeLocationInfo
                    );

                    // Set the variable declaration on the attribute
                    attribute.setVariableDeclaration(variableDeclaration);
                    attribute.setVisibility(Visibility.PUBLIC);
                    attribute.setFinal(false);
                    attribute.setStatic(false);

                    if (methodDeclaration.getParent() instanceof LangTypeDeclaration typeDeclaration){
                        attribute.setClassName(typeDeclaration.getName());
                    } else {
                        attribute.setClassName("UnknownClass");
                    }

                    attributes.add(attribute);

//                    LOGGER.info("Created attribute: " + attributeName + " with initializer: " +
//                            (variableDeclaration.getInitializer() != null ? "yes" : "no"));
                }
            }
        }
    }

    private void processClassLevelAssignmentForAttribute(LangTypeDeclaration typeDeclaration, LangAssignment assignment, List<UMLAttribute> attributes,
                                               String sourceFolder, String filePath, UMLOperation umlOperation, String fileContent) {
        LangASTNode leftSide = assignment.getLeftSide();

        if (leftSide instanceof LangFieldAccess langFieldAccess) {
            LangASTNode expression = langFieldAccess.getExpression();

            // Check if it's self.attribute
            if (expression instanceof LangSimpleName simpleName) {
                if ("self".equals(simpleName.getIdentifier())) {
                    String attributeName = langFieldAccess.getName().getIdentifier();

                    // Create VariableDeclaration for the attribute using the new constructor
                    VariableDeclaration variableDeclaration = UMLAdapterVariableProcessor.processAttributeAssignment(
                            assignment,
                            sourceFolder,
                            filePath,
                            attributeName,
                            umlOperation,
                            fileContent
                    );


                    // Create UMLAttribute
                    LocationInfo attributeLocationInfo = new LocationInfo(
                            assignment.getRootCompilationUnit(),
                            sourceFolder,
                            filePath,
                            langFieldAccess,
                            LocationInfo.CodeElementType.FIELD_DECLARATION
                    );
                    UMLAttribute attribute = new UMLAttribute(
                            attributeName,
                            UMLType.extractTypeObject("Object"),
                            attributeLocationInfo
                    );

                    // Set the variable declaration on the attribute
                    attribute.setVariableDeclaration(variableDeclaration);
                    attribute.setVisibility(Visibility.PUBLIC);
                    attribute.setFinal(false);
                    attribute.setStatic(false);

                    attribute.setClassName(typeDeclaration.getName());
                    attributes.add(attribute);

//                    LOGGER.info("Created attribute: " + attributeName + " with initializer: " +
//                            (variableDeclaration.getInitializer() != null ? "yes" : "no"));
                }
            }
        }
    }

    private void processComments(LangMethodDeclaration methodDecl, String sourceFolder, String filePath, UMLOperation umlOperation){
        List<UMLComment> comments = new ArrayList<>();
        for (LangComment langComment: methodDecl.getComments()) {
            if (langComment.isBlockComment() || langComment.isDocComment()){
                comments.add(new UMLComment(langComment.getContent(), new LocationInfo(
                        methodDecl.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        langComment,
                        LocationInfo.CodeElementType.BLOCK_COMMENT
                )));
            } else if (langComment.isLineComment()){
                comments.add(new UMLComment(langComment.getContent(), new LocationInfo(
                        methodDecl.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        langComment,
                        LocationInfo.CodeElementType.LINE_COMMENT
                )));
            }
        }
        umlOperation.getComments().addAll(comments);
    }

    private static void logMethodDetails(CompositeStatementObject composite, UMLOperation container, List<VariableDeclaration> allMethodVarDecls) {
        LOGGER.info("Method " + container.getName() + " has " +
                composite.getVariableDeclarations().size() + " variable declarations:");


        LOGGER.info("=== FINAL UML MODEL DEBUG ===");
        LOGGER.info("Method " + container.getName() + " has " + allMethodVarDecls.size() + " variable declarations:");
        for (VariableDeclaration vd : allMethodVarDecls) {
            LOGGER.info("  - Variable: " + vd.getVariableName() +
                    " | Type: " + vd.getType() +
                    " | Scope: " + vd.getScope() +
                    " | Initializer: " + (vd.getInitializer() != null ? vd.getInitializer().getString() : "null"));
        }

        LOGGER.info("Method body statements: " + composite.getStatements().size());
        for (AbstractStatement stmt : composite.getStatements()) {
            LOGGER.info("  - Statement: " + stmt.getClass().getSimpleName() +
                    " | Variables: " + stmt.getVariables().size() +
                    " | VarDecls: " + stmt.getVariableDeclarations().size());
        }
        LOGGER.info("=== END UML MODEL DEBUG ===");
    }

    private void logUMLClass(UMLClass umlClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("UMLClass created: ").append(umlClass)
                .append("\nName: ").append(umlClass.getName())
                .append("\nPackage: ").append(umlClass.getPackageName())
                .append("\nSource Folder: ").append(umlClass.getLocationInfo().getSourceFolder())
                .append("\nFile Path: ").append(umlClass.getLocationInfo().getFilePath())
                .append("\nActual Signature: ").append(umlClass.getActualSignature())
                .append("\nNon Qualified Name: ").append(umlClass.getNonQualifiedName())
                .append("\nVisibility: ").append(umlClass.getVisibility())
                .append("\nIs Interface: ").append(umlClass.isInterface())
                .append("\nIs Abstract: ").append(umlClass.isAbstract())
                .append("\nSuperclass: ").append(umlClass.getSuperclass())
                .append("\nImplemented Interfaces: ").append(umlClass.getImplementedInterfaces())
                .append("\nAttributes: ").append(umlClass.getAttributes())
                .append("\nOperations (methods): \n");

        for (UMLOperation op : umlClass.getOperations()) {
            sb.append("  - ").append(op.getActualSignature())
                    .append(" [Visibility: ").append(op.getVisibility())
                    .append(", Parameters: ").append(op.getParameters())
                    .append("]\n");
        }

        sb.append("\n");

        LOGGER.info(sb.toString());
    }

//    private void logUMLOperation(UMLOperation umlOperation, LangMethodDeclaration methodDecl){
//        String bodyString = stringify(methodDecl.getBody());
//        LOGGER.info(
//                "UMLOperation created: " + umlOperation +
//                        "\nSignature: " + umlOperation.getActualSignature() +
//                        "\nQualified Name: " + umlOperation.getClassName() + "." + umlOperation.getName() +
//                        "\nName: " + umlOperation.getName() +
//                        "\nClass: " + umlOperation.getClassName() +
//                        "\nVisibility: " + umlOperation.getVisibility() +
//                        "\nParameters: " + umlOperation.getParameters() +
//                        "\nIs Constructor: " + umlOperation.isConstructor() +
//                        "\nIs Final: " + umlOperation.isFinal() +
//                        "\nIs Static: " + umlOperation.isStatic() +
//                        "\nIs Abstract: " + umlOperation.isAbstract() +
//                        "\nIs Native: " + umlOperation.isNative() +
//                        "\nReturn Type: " + umlOperation.getReturnParameter() +
//                        "\nBody Hash Code: " + umlOperation.getBody().getBodyHashCode() +
//                        "\nMethod body stringify result for " + methodDecl.getName() + ": " + bodyString +
//                        "\nString hash: " + bodyString.hashCode() +
//                        "\n\n"
//        );
//
//    }

    public UMLModel getUMLModel() {
        return umlModel;
    }
}