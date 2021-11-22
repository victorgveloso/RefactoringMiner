package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents expecting exception test encoding migration (from JUnit 3 to Rule-based JUnit 4)
 * JUnit3 relies on try-catch containing an Assert.fail call to expect thrown exceptions
 * JUnit4 usually relies on @Rule ExpectedException, a Single Member annotated field, which provides an expect method
 */
public class TryWithFailToExpectedExceptionRuleRefactoring implements Refactoring {
    private UMLOperation operationBefore;
    private UMLOperation operationAfter;
    private final List<TryStatementObject> tryStatement;
    private final List<OperationInvocation> assertFailInvocations;
    private final List<String> capturedExceptions;
    private final List<OperationInvocation> thrownExpectInvocations;
    private final UMLAttribute ruleFieldDeclaration;

    public TryWithFailToExpectedExceptionRuleRefactoring(UMLOperation operationBefore, UMLOperation operationAfter, List<TryStatementObject> tryStatement, List<OperationInvocation> assertFailInvocations, List<String> capturedExceptions, List<OperationInvocation> thrownExpectInvocations, UMLAttribute ruleFieldDeclaration) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.tryStatement = tryStatement;
        this.assertFailInvocations = assertFailInvocations;
        this.capturedExceptions = capturedExceptions;
        this.thrownExpectInvocations = thrownExpectInvocations;
        this.ruleFieldDeclaration = ruleFieldDeclaration;
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
    public String toString() {
        return "";
    }

    @Override
    public RefactoringType getRefactoringType() {
        return null;
    }

    @Override
    public String getName() {
        return getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(operationBefore.getLocationInfo().getFilePath(), operationBefore.getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(operationAfter.getLocationInfo().getFilePath(), operationAfter.getClassName()));
        return pairs;
    }
}
