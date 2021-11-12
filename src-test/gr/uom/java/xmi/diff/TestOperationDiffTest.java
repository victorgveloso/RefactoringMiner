package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


public class TestOperationDiffTest {
    UMLModelDiff modelDiff;
    @Before
    public void setUp() throws RefactoringMinerTimedOutException {
        var inlineVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_InlineVersion();
        var inlineVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(inlineVersionTestMethod);
        var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", inlineVersionTestClass), Set.of()).getUmlModel();
        var assertVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_AssertVersion();
        var assertVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(assertVersionTestMethod);
        var after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", assertVersionTestClass), Set.of()).getUmlModel();
        modelDiff = before.diff(after);
    }
    @Test
    public void testFromInlineToAssertThrows() throws RefactoringMinerTimedOutException {
        var refactorings = modelDiff.getRefactorings();
        Assert.assertEquals(refactorings.toString(), 1, refactorings.size());
        var refactoring = refactorings.get(0);
        Assert.assertEquals(RefactoringType.MODIFY_METHOD_ANNOTATION, refactoring.getRefactoringType());
        var modifyAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
        var before = modifyAnnotationRefactoring.getAnnotationBefore();
        Assert.assertTrue(hasExpectedException(before));
        var expectedException = before.getMemberValuePairs().get("expected");
        Assert.assertEquals(1, expectedException.getTypeLiterals().size());
        Assert.assertTrue(hasAssertThrows(modifyAnnotationRefactoring.getOperationAfter()));
    }

    private boolean hasExpectedException(gr.uom.java.xmi.UMLAnnotation before) {
        return before.isNormalAnnotation() && before.getTypeName().equals("Test") && before.getMemberValuePairs().containsKey("expected");
    }

    private boolean hasAssertThrows(UMLOperation operation) {
        return operation.hasTestAnnotation() && recurseThroughStmtTree(
                operation.getBody().getCompositeStatement(),
                (i) -> i.getMethodName().equals("assertThrows") && i.getExpression().equals("Assert")
        );
    }

    private boolean recurseThroughStmtTree(AbstractStatement stmt, Predicate<OperationInvocation> cb) {
        if (stmt instanceof CompositeStatementObject) {
            var composite = (CompositeStatementObject) stmt;
            for (var childStmt : composite.getStatements()) {
                return recurseThroughStmtTree(childStmt, cb);
            }
        }
        else if (stmt instanceof StatementObject) {
            var simple = (StatementObject) stmt;
            var methodInvocations = simple.getMethodInvocationMap();
            for (var mapping: methodInvocations.keySet()) {
                for (var invocation: methodInvocations.get(mapping)) {
                    if (cb.test(invocation)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
