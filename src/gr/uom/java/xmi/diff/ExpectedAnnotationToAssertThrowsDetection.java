package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @Rule ExpectedException, a Single Member annotated field, which provides an expect method
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedAnnotationToAssertThrowsDetection {
    private final Set<Refactoring> refactorings;
    private OperationInvocation operationInvocation;
    private ModifyMethodAnnotationRefactoring annotationChange;
    private AbstractExpression exception;
    private LambdaExpressionObject lambda;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ExpectedAnnotationToAssertThrowsDetection(UMLOperation operationBefore, UMLOperation operationAfter, Set<Refactoring> refactorings) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.refactorings = refactorings;
    }

    public ExpectedAnnotationToAssertThrowsRefactoring check() {
        var expectedRemovalFromTestAnnotation = getRemovedExpectedAttributeFromTestAnnotation();
        try {
            annotationChange = expectedRemovalFromTestAnnotation.get();
            exception = annotationChange.getAnnotationBefore().getMemberValuePairs().get("expected");
            var assertThrows = getAssertThrows(operationAfter).stream()
                    .filter(i -> i.getArguments().get(0).equals(exception.getExpression()))
                    .filter(i -> containsAtLeastOneLineInCommon(operationBefore, i.getArguments().get(1)))
                    .findAny();
            operationInvocation = assertThrows.get();
            var expr = operationAfter.getAllLambdas().stream()
                    .filter(lambda -> isEnclosedBy(lambda, operationInvocation))
                    .findAny();
            lambda = expr.get();
            return new ExpectedAnnotationToAssertThrowsRefactoring(operationBefore, operationAfter, annotationChange, exception, lambda, operationInvocation);
        } catch (NoSuchElementException ex) {
            return null;
        }
    }

    private boolean isEnclosedBy(LambdaExpressionObject lambda, OperationInvocation invocation) {
        var assertThrowsRange = invocation.codeRange();
        var range = lambda.codeRange();
        return assertThrowsRange.getStartLine() <= range.getStartLine() &&
                assertThrowsRange.getEndLine() >= range.getEndLine() &&
                assertThrowsRange.getStartColumn() <= range.getStartColumn() &&
                assertThrowsRange.getEndColumn() >= range.getEndColumn();
    }

    private boolean containsAtLeastOneLineInCommon(UMLOperation operation, String lambda) {
        return lambda
                .lines()
                .skip(1)
                .map(String::strip)
                .filter(line -> line.length() > 1) // Ignore "{" and "}" lines
                .anyMatch(lambdaLine -> operationContainsLine(operation, lambdaLine));
    }

    private boolean operationContainsLine(UMLOperation operation, String line) {
        return operation.getBody().stringRepresentation().stream()
                .map(String::strip)
                .filter(s -> s.length() > 1) // Ignore "{" and "}" lines
                .anyMatch(operationBodyLine -> operationBodyLine.equals(line));
    }

    private Optional<ModifyMethodAnnotationRefactoring> getRemovedExpectedAttributeFromTestAnnotation() {
        return refactorings.stream()
                .filter(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION))
                .map(r -> (ModifyMethodAnnotationRefactoring) r)
                .filter(r -> hasExpectedException(r.getAnnotationBefore()))
                .filter(r -> !hasExpectedException(r.getAnnotationAfter()))
                .findAny();
    }

    private boolean hasExpectedException(gr.uom.java.xmi.UMLAnnotation before) {
        return before.isNormalAnnotation() && before.getTypeName().equals("Test") && before.getMemberValuePairs().containsKey("expected");
    }

    private List<OperationInvocation> getAssertThrows(UMLOperation operation) {
        return operation.getAllOperationInvocations().stream()
                .filter((op) -> op.getMethodName().equals("assertThrows") &&
                        (op.getExpression().equals("Assert") || op.getExpression().equals("Assertions")))
                .collect(Collectors.toList());
    }
}
