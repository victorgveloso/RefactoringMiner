package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;

public class SimpleNameVisitorUtils {
    /**
     * Evaluates whether node should be skipped
     * @param node the node being evaluated
     * @return True if node is a method invocation name, a super method invocation name, a type name,
     * a marker annotation name, a method declaration name, a method parameter name,
     * a catch clause formal parameter name, or a name being part of qualified names
     */
    static boolean shouldSkip(SimpleName node) {
        return isMethodInvocation(node) || isSuperMethodInvocation(node) || isType(node) || isMarkerAnnotation(node) || isMethodDeclaration(node) || isMethodParameter(node) || isCatchClauseFormalParameter(node) || isPartOfQualifiedNames(node);
    }

    private static boolean isPartOfQualifiedNames(SimpleName node) {
        return node.getParent() instanceof QualifiedName &&
                (node.getParent().getParent() instanceof QualifiedName ||
                node.getParent().getParent() instanceof MethodInvocation ||
                node.getParent().getParent() instanceof SuperMethodInvocation ||
                node.getParent().getParent() instanceof ClassInstanceCreation);
    }

    private static boolean isCatchClauseFormalParameter(SimpleName node) {
        return node.getParent() instanceof SingleVariableDeclaration &&
                node.getParent().getParent() instanceof CatchClause;
    }

    private static boolean isMethodParameter(SimpleName node) {
        return node.getParent() instanceof SingleVariableDeclaration &&
                node.getParent().getParent() instanceof MethodDeclaration;
    }

    private static boolean isMethodDeclaration(SimpleName node) {
        return node.getParent() instanceof MethodDeclaration &&
                ((MethodDeclaration)node.getParent()).getName().equals(node);
    }

    private static boolean isMarkerAnnotation(SimpleName node) {
        return node.getParent() instanceof MarkerAnnotation &&
                ((MarkerAnnotation)node.getParent()).getTypeName().equals(node);
    }

    private static boolean isType(SimpleName node) {
        return node.getParent() instanceof Type;
    }

    private static boolean isSuperMethodInvocation(SimpleName node) {
        return node.getParent() instanceof SuperMethodInvocation &&
                ((SuperMethodInvocation)node.getParent()).getName().equals(node);
    }

    private static boolean isMethodInvocation(SimpleName node) {
        return node.getParent() instanceof MethodInvocation &&
                ((MethodInvocation)node.getParent()).getName().equals(node);
    }
}
