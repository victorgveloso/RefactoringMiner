package gr.uom.java.xmi.diff;

import org.refactoringminer.api.Refactoring;

import gr.uom.java.xmi.UMLClass;

public interface PackageLevelRefactoring extends Refactoring {
	RenamePattern getRenamePattern();
	UMLClass getOriginalClass();
	UMLClass getMovedClass();
	String getOriginalClassName();
	String getMovedClassName();
}
