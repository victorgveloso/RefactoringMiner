package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.NodeTypeEnum;
import extension.ast.node.TypeObjectEnum;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangSimpleName;
import extension.ast.node.literal.LangStringLiteral;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangBlock;
import extension.ast.node.statement.LangExpressionStatement;
import extension.base.lang.python.Python3Parser;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

public class PyDeclarationASTBuilder extends PyBaseASTBuilder {

    public PyDeclarationASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }


    public LangASTNode visitClassdef(Python3Parser.ClassdefContext ctx) {
        LangTypeDeclaration langTypeDeclaration = LangASTNodeFactory.createTypeDeclaration(ctx);

        langTypeDeclaration.setAbstract(isClassAbstract(ctx));
        langTypeDeclaration.setEnum(isClassEnum(ctx));
        langTypeDeclaration.setActualSignature("class " + ctx.name().getText());
        langTypeDeclaration.setVisibility(Visibility.PUBLIC);
        langTypeDeclaration.setTopLevel(true);
        setSuperClasses(ctx, langTypeDeclaration);

        if (ctx.block() != null && !ctx.block().stmt().isEmpty()) {
            for (Python3Parser.StmtContext stmtContext : ctx.block().stmt()) {
                LangASTNode statement = mainBuilder.visit(stmtContext);
                if (statement instanceof LangMethodDeclaration) {
                    langTypeDeclaration.addMethod((LangMethodDeclaration) statement);
                } else if (statement instanceof LangExpressionStatement exprStatement) {
                    if (exprStatement.getExpression() instanceof LangAssignment assignment)
                        langTypeDeclaration.addAssignment(assignment);
                }
            }
        }

        return langTypeDeclaration;
    }


    private void setSuperClasses(Python3Parser.ClassdefContext ctx, LangTypeDeclaration typeDeclaration) {
        if (ctx.arglist() != null) {
            List<String> superClasses = new ArrayList<>();
            Python3Parser.ArglistContext arglist = ctx.arglist();

            for (Python3Parser.ArgumentContext argContext : arglist.argument()) {

                String argText = argContext.getText();
                if (argText.contains("metaclass=")) {
                    continue; // Skip metaclass arguments
                }
                superClasses.add(argText);
            }
            typeDeclaration.setSuperClassNames(superClasses);
        }
    }

    private boolean isClassAbstract(Python3Parser.ClassdefContext ctx){
        // Check inheritance or metaclass for ABC-related types
        if (ctx.arglist() != null) {
            String argsText = ctx.arglist().getText();
            return argsText.contains("metaclass=ABCMeta") ||
                    argsText.contains("metaclass=abc.ABCMeta") ||
                    argsText.contains("ABC") ||
                    argsText.contains("abc.ABC");
        }
        return false;
    }

    private boolean isClassEnum(Python3Parser.ClassdefContext ctx){
        if (ctx.arglist() != null) {
            for (Python3Parser.ArgumentContext argContext : ctx.arglist().argument()) {
                String argText = argContext.getText();

                if (argText.contains("=")) {
                    continue;
                }

                // Check if any parent class is an enum type
                if (argText.equals("Enum") || argText.equals("IntEnum") ||
                        argText.equals("Flag") || argText.equals("IntFlag") ||
                        argText.equals("AutoEnum") || argText.equals("StrEnum") ||
                        argText.endsWith(".Enum")) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public LangASTNode visitFuncdef(Python3Parser.FuncdefContext ctx) {

        if (ctx.block() == null) {
            System.err.println("Warning: Function " + ctx.name().getText() + " has no body, skipping");
            return null;
        }

        // Collect langSingleVariableDeclarations
        List<LangSingleVariableDeclaration> langSingleVariableDeclarations = new ArrayList<>();
        if (ctx.parameters().typedargslist() != null) {
            for (Python3Parser.TfpdefContext paramCtx : ctx.parameters().typedargslist().tfpdef()) {
                if (ctx.parameters().typedargslist().STAR() == null){
                    LangSingleVariableDeclaration singleVariableDeclaration = LangASTNodeFactory.createSingleVariableDeclaration(paramCtx.name().getText(), paramCtx);
                    singleVariableDeclaration.setTypeAnnotation(TypeObjectEnum.OBJECT);
                    singleVariableDeclaration.setParameter(true);
                    langSingleVariableDeclarations.add(singleVariableDeclaration);
                }
            }

            // Check for *args parameter
            if (ctx.parameters().typedargslist().STAR() != null && ctx.parameters().typedargslist().tfpdef(ctx.parameters().typedargslist().tfpdef().size() - 1) != null) {
                // Handle *args - the parameter after STAR token
                Python3Parser.TfpdefContext varargCtx = ctx.parameters().typedargslist().tfpdef(ctx.parameters().typedargslist().tfpdef().size() - 1);
                String varargName = varargCtx.name().getText();
                LangSingleVariableDeclaration varargDecl = LangASTNodeFactory.createSingleVariableDeclaration(varargName, varargCtx);
                varargDecl.setTypeAnnotation(TypeObjectEnum.OBJECT);
                varargDecl.setParameter(true);
                varargDecl.setAttribute(false);
                varargDecl.setVarArgs(true);  // Set the varargs flag
                langSingleVariableDeclarations.add(varargDecl);
            }
        }

        // Visit the function body
        LangBlock body = (LangBlock) mainBuilder.visit(ctx.block());

        String docstring = null;
        if (!body.getStatements().isEmpty() &&
                body.getStatements().get(0) instanceof LangExpressionStatement stmt &&
                stmt.getExpression() instanceof LangStringLiteral str) {
            docstring = str.getValue();
            body.getStatements().remove(0);
        }


        // Create the MethodDeclaration node using the factory
        LangMethodDeclaration methodDeclaration = LangASTNodeFactory.createMethodDeclaration(ctx.name().getText(), ctx, langSingleVariableDeclarations, body);

        methodDeclaration.setConstructor(isMethodAConstructor(methodDeclaration));
        // Following python naming conventions for visibility
        methodDeclaration.setVisibility(getMethodVisibility(methodDeclaration));
        methodDeclaration.setCleanName(extractCleanName(methodDeclaration.getName()));

        if (ctx.test() != null) {
            String returnType = ctx.test().getText();
            methodDeclaration.setReturnTypeAnnotation(returnType);
        } else {
            // Check if the method has a return statement
            // Set return type annotation to "None" if no return statement is found
            boolean hasReturn = false;
            if (body.getStatements() != null) {
                for (LangASTNode statement : body.getStatements()) {
                    if (NodeTypeEnum.RETURN_STATEMENT.equals(statement.getNodeType())) {
                        hasReturn = true;
                        break;
                    }
                }
            }

            if (hasReturn) {
                methodDeclaration.setReturnTypeAnnotation(TypeObjectEnum.OBJECT.getName());
            } else {
                methodDeclaration.setReturnTypeAnnotation("None");
            }
        }

        if (docstring != null) {
            LangComment comment = LangASTNodeFactory.createComment(ctx, docstring, false, true);
            methodDeclaration.addComment(comment);
        }

        return methodDeclaration;
    }


    public LangASTNode visitAsync_funcdef(Python3Parser.Async_funcdefContext ctx) {
        LangASTNode astNode = visitFuncdef(ctx.funcdef());


        if (astNode instanceof LangMethodDeclaration methodDeclaration) {
            LangAnnotation asyncAnnotation = LangASTNodeFactory.createAnnotation(
                    ctx,
                    LangASTNodeFactory.createSimpleName("async", ctx),
                    new ArrayList<>()
            );

            List<LangAnnotation> annotations = methodDeclaration.getLangAnnotations();
            if (annotations == null) {
                annotations = new ArrayList<>();
            }
            annotations.add(asyncAnnotation);
            methodDeclaration.setLangAnnotations(annotations);
            methodDeclaration.setAsync(true);
        }

        return astNode;
    }

    private Visibility getMethodVisibility(LangMethodDeclaration methodDecl) {
        String methodName = methodDecl.getName();

        // In Python, methods starting with double underscore (__method) are considered private
        if (methodName.startsWith("__") && !methodName.endsWith("__")) {
            return Visibility.PRIVATE;
        }
        // Methods starting with single underscore (_method) are considered protected
        else if (methodName.startsWith("_") && !methodName.startsWith("__")) {
            return Visibility.PROTECTED;
        }
        // Double underscore at both ends are special methods (__init__, __str__) and are public
        else if (methodName.startsWith("__") && methodName.endsWith("__")) {
            return Visibility.PUBLIC;
        }
        // All other methods are public by default
        else {
            return Visibility.PUBLIC;
        }
    }

    // TODO: Refactor
    private String extractCleanName(String name) {

        if (name.startsWith("__") && name.endsWith("__")) {
            return name.substring(2, name.length() - 2);
        }

        if (name.startsWith("_") && name.endsWith("__")) {
            return name.substring(1, name.length() - 2);
        }

        // Handle private methods (double underscore prefix)
        if (name.startsWith("__")) {
            return name.substring(2);
        }
        // Handle protected methods (single underscore prefix)
        else if (name.startsWith("_")) {
            return name.substring(1);
        }

        return name;
    }

    private boolean isMethodAConstructor(LangMethodDeclaration methodDecl) {
        return "__init__".equals(methodDecl.getName());
    }

    public LangASTNode visitDecorated(Python3Parser.DecoratedContext ctx) {
        List<LangAnnotation> annotations = new ArrayList<>();

        // Process decorators
        for (Python3Parser.DecoratorContext decoratorCtx : ctx.decorators().decorator()) {
            // Get the name of the decorator
            String decoratorName = decoratorCtx.dotted_name().getText();
            LangSimpleName decoratorSimpleName = LangASTNodeFactory.createSimpleName(decoratorName, decoratorCtx);

            // Process arguments if any
            List<LangASTNode> arguments = new ArrayList<>();

            // Create the annotation
            LangAnnotation annotation = LangASTNodeFactory.createAnnotation(decoratorCtx, decoratorSimpleName, arguments);
            annotations.add(annotation);

        }

        LangASTNode decoratedNode = null;

        if (ctx.funcdef() != null) {
            decoratedNode = mainBuilder.visitFuncdef(ctx.funcdef());
            if (decoratedNode instanceof LangMethodDeclaration method) {

                method.setLangAnnotations(annotations);

                // Apply decorators to the method
                for (LangAnnotation annotation : annotations) {
                    String decoratorName = annotation.getName().getIdentifier();

                    // Handle specific Python decorators
                    if (decoratorName.equals("abstractmethod")) {
                        method.setAbstract(true);
                    } else if (decoratorName.equals("staticmethod")) {
                        method.setStatic(true);
                    } else {
                        method.setAbstract(false);
                        method.setStatic(false);
                    }
                }
            }
        } else if (ctx.classdef() != null) {
            decoratedNode = mainBuilder.visitClassdef(ctx.classdef());
            if (decoratedNode instanceof LangTypeDeclaration classDecl) {
                classDecl.setLangAnnotations(annotations);
            }
        } else if (ctx.async_funcdef() != null) {
            decoratedNode = mainBuilder.visitAsync_funcdef(ctx.async_funcdef());
        }

        return decoratedNode;
    }


}
