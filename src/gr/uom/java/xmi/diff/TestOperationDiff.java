package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.api.Refactoring;

import java.util.Collections;
import java.util.Set;

/**
 * Compares two test methods and detect test-related refactorings
 */
public class TestOperationDiff {
    private UMLOperation removed;
    private UMLOperation added;

    public TestOperationDiff(UMLOperationDiff operationDiff) {
        this.removed = operationDiff.getRemovedOperation();
        this.added = operationDiff.getAddedOperation();
    }

    public Set<Refactoring> getRefactorings() {
        return Collections.EMPTY_SET;
    }

    static boolean isTestOperation(UMLOperation operation) {
        var annotations = operation.getAnnotations();
        return annotations.stream().anyMatch(annotation -> annotation.getTypeName().equals("Test"));
    }
}
