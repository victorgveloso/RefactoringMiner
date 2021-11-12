package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.api.Refactoring;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Compares two test methods and detect test-related refactorings
 */
public class TestOperationDiff {
    private final UMLOperation removed;
    private final UMLOperation added;
    private final Set<Refactoring> refactorings = new LinkedHashSet<>();

    public TestOperationDiff(UMLOperationDiff operationDiff) {
        this(operationDiff.getRemovedOperation(), operationDiff.getAddedOperation(), operationDiff.getRefactoringsAfterPostProcessing());
    }

    public TestOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation, Set<Refactoring> refactorings) {
        this(removedOperation, addedOperation);
        this.refactorings.addAll(refactorings);
    }

    TestOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation) {
        removed = removedOperation;
        added = addedOperation;
    }

    static boolean isTestOperation(UMLOperation operation) {
        return operation.hasTestAnnotation();
    }

    public Set<Refactoring> getRefactorings() {
        Set<Refactoring> refactorings = new LinkedHashSet<>();
        var jUnit3To4RuleBasedRefactoring = getJUnit3AssertFailToJUnit4ExpectedExceptionRefactoring();
        if (Objects.nonNull(jUnit3To4RuleBasedRefactoring)) {
            refactorings.add(jUnit3To4RuleBasedRefactoring);
        }
        var jUnit4To5Refactoring = getJUnit4ExpectedExceptionToJUnit5AssertThrowsRefactoring();
        if (Objects.nonNull(jUnit4To5Refactoring)) {
            refactorings.add(jUnit4To5Refactoring);
        }
        return refactorings;
    }

    public ExpectedAnnotationToAssertThrowsRefactoring getJUnit3AssertFailToJUnit4ExpectedExceptionRefactoring() {
        return null;
    }

    public ExpectedAnnotationToAssertThrowsRefactoring getJUnit4ExpectedExceptionToJUnit5AssertThrowsRefactoring() {
        var detector = new ExpectedAnnotationToAssertThrowsDetection(removed, added, refactorings);
        return detector.check();
    }
}
