package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.api.Refactoring;

public interface OperationRefactoring extends Refactoring {
    UMLOperation getOperationBefore();

    UMLOperation getOperationAfter();
}
