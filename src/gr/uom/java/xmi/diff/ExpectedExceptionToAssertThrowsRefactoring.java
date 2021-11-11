package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Set;

/**
 * Represents expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @ExpectedException, a Single Member annotation, with an exception type as its only member
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedExceptionToAssertThrowsRefactoring implements Refactoring {
    private UMLAnnotation expectedExceptionAnnotation;
    private AbstractExpression exception;
    private UMLOperation operationBefore;
    private UMLOperation operationAfter;

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
