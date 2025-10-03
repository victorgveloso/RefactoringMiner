package extension.umladapter.processor;

import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.expression.LangAssignment;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class UMLAdapterVariableProcessor {

    public static void processVariableDeclarations(LangSingleVariableDeclaration param, UMLParameter umlParam, UMLType typeObject, String sourceFolder, String filePath, LangMethodDeclaration methodDecl){

        String name = param.getLangSimpleName().getIdentifier();
        LocationInfo location = new LocationInfo(sourceFolder, filePath, methodDecl, LocationInfo.CodeElementType.SINGLE_VARIABLE_DECLARATION);

        VariableDeclaration vd = new VariableDeclaration(
                param.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                param
        );

        vd.setAttribute(param.isAttribute());
        vd.setParameter(param.isParameter());

        umlParam.setVariableDeclaration(vd);
    }

    /**
     * Process assignment-based attribute declarations (self.attr = value)
     */
    public static VariableDeclaration processAttributeAssignment(LangAssignment assignment, String sourceFolder,
                                                                 String filePath, String attributeName,
                                                                 VariableDeclarationContainer container) {

        VariableDeclaration variableDeclaration = new VariableDeclaration(
                assignment.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                assignment,
                container,
                attributeName
        );

        // Mark as attribute (this should already be set by the constructor)
        variableDeclaration.setAttribute(true);

        return variableDeclaration;
    }

}
