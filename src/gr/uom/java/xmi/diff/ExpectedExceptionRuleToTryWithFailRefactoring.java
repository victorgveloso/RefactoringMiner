package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Set;

public class ExpectedExceptionRuleToTryWithFailRefactoring implements TryWithFailAndExpectedExceptionRuleRefactoring {
    private final TryWithFailAndExpectedExceptionRuleRefactoring reverseRefactoring;

    public ExpectedExceptionRuleToTryWithFailRefactoring(UMLOperation operationAfter, UMLOperation operationBefore, TryStatementObject tryStatement, AbstractCall assertFailInvocation, String capturedException, AbstractCall thrownExpectInvocation, UMLAttribute ruleFieldDeclaration) {
        reverseRefactoring = new TryWithFailToExpectedExceptionRuleRefactoring(operationBefore, operationAfter, tryStatement, assertFailInvocation, capturedException, thrownExpectInvocation, ruleFieldDeclaration);
    }

    public UMLOperation getOperationBefore() {
        return reverseRefactoring.getOperationAfter();
    }

    public UMLOperation getOperationAfter() {
        return reverseRefactoring.getOperationBefore();
    }

    public TryStatementObject getTryStatement() {
        return reverseRefactoring.getTryStatement();
    }

    public AbstractCall getAssertFailInvocation() {
        return reverseRefactoring.getAssertFailInvocation();
    }

    public String getException() {
        return reverseRefactoring.getException();
    }

    public AbstractCall getThrownExpectInvocations() {
        return reverseRefactoring.getThrownExpectInvocations();
    }

    public UMLAttribute getRuleFieldDeclaration() {
        return reverseRefactoring.getRuleFieldDeclaration();
    }

    @Override
    public List<CodeRange> leftSide() {
        return reverseRefactoring.leftSide();
    }

    @Override
    public List<CodeRange> rightSide() {
        return reverseRefactoring.rightSide();
    }

    @Override
    public String toString() {
        return reverseRefactoring.toString();
    }

    @Override
    public RefactoringType getRefactoringType() {
        return reverseRefactoring.getRefactoringType();
    }

    @Override
    public String getName() {
        return reverseRefactoring.getName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        return reverseRefactoring.getInvolvedClassesBeforeRefactoring();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        return reverseRefactoring.getInvolvedClassesAfterRefactoring();
    }
}
