package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Set;

/**
 * Represents expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @Rule ExpectedException, a Single Member annotated field, which provides an expect method
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedAnnotationToAssertThrowsRefactoring implements Refactoring {
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final ModifyMethodAnnotationRefactoring expectedExceptionAnnotation;
    private final AbstractExpression exception;
    private final LambdaExpressionObject lambda;
    private final OperationInvocation assertThrows;

    public ExpectedAnnotationToAssertThrowsRefactoring(UMLOperation operationBefore,
                                                       UMLOperation operationAfter,
                                                       ModifyMethodAnnotationRefactoring expectedExceptionAnnotation,
                                                       AbstractExpression exception,
                                                       LambdaExpressionObject lambda,
                                                       OperationInvocation assertThrows) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.expectedExceptionAnnotation = expectedExceptionAnnotation;
        this.exception = exception;
        this.lambda = lambda;
        this.assertThrows = assertThrows;
    }

    @Override
    public String toString() {
        return "ExpectedAnnotationToAssertThrowsRefactoring{" +
                "operationBefore=" + operationBefore +
                ", operationAfter=" + operationAfter +
                ", expectedExceptionAnnotation=" + expectedExceptionAnnotation +
                ", exception=" + exception +
                ", lambda=" + lambda +
                ", assertThrows=" + assertThrows +
                '}';
    }

    @Override
    public List<CodeRange> leftSide() {
        return null;
    }

    @Override
    public List<CodeRange> rightSide() {
        return null;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        return null;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        return null;
    }
}
