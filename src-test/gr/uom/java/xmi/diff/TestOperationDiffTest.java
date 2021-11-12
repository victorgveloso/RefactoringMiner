package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


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
        Assert.assertEquals("There should be only one refactoring in the example test method", 1, refactorings.size());
        var refactoring = refactorings.get(0);
        Assert.assertEquals("@Test memberValuePair change not detected", RefactoringType.MODIFY_METHOD_ANNOTATION, refactoring.getRefactoringType());

        var modifyAnnotationRefactoring = (ModifyMethodAnnotationRefactoring) refactoring;
        var before = modifyAnnotationRefactoring.getAnnotationBefore();
        var after = modifyAnnotationRefactoring.getOperationAfter();

        Assert.assertTrue("@Test(expected) normalAnnotation not found", hasExpectedException(before));
        var expectedException = before.getMemberValuePairs().get("expected");
        Assert.assertEquals("@Test(expected) should contain a single type literal",1, expectedException.getTypeLiterals().size());
        var assertThrows = getAssertThrows(after);
        Assert.assertEquals("Number of found assertThrows call is not 1", 1, assertThrows.size());

        var args = assertThrows.get(0).getArguments();
        var exceptionClassLiteral = args.get(0);
        Assert.assertEquals(expectedException.getExpression(), exceptionClassLiteral);
        var lambdaExpression = args.get(1);
        var allLambdas = after.getBody().getAllLambdas();
        Assert.assertEquals(1, allLambdas.size());
        var lambda = allLambdas.get(0);
        var expectedLines = lambdaExpression.lines().collect(Collectors.toList());
        expectedLines.remove(0);
        expectedLines.remove(expectedLines.size() - 1);
        var lines = lambda.getBody().stringRepresentation();
        lines.remove(0);
        lines.remove(lines.size() - 1);
        var expectedIter = expectedLines.iterator();
        var linesIter = lines.iterator();
        for (String expectedLine = expectedIter.next(), line = linesIter.next();
             expectedIter.hasNext() && linesIter.hasNext();
             expectedLine = expectedIter.next(), line = linesIter.next()) {
            Assert.assertEquals(expectedLine.strip(), line.strip());
        }
    }

    private boolean hasExpectedException(gr.uom.java.xmi.UMLAnnotation before) {
        return before.isNormalAnnotation() && before.getTypeName().equals("Test") && before.getMemberValuePairs().containsKey("expected");
    }



    private List<OperationInvocation> getAssertThrows(UMLOperation operation) {
        return operation.getAllOperationInvocations().stream()
                .filter((op) -> op.getMethodName().equals("assertThrows") && op.getExpression().equals("Assert"))
                .collect(Collectors.toList());
    }
}
