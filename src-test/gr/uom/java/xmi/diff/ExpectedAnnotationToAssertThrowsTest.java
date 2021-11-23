package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class ExpectedAnnotationToAssertThrowsTest {
    abstract public static class ModelDiffFieldSetUp {
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
    }

    public static class ImplementationTest extends ModelDiffFieldSetUp {
        @Ignore("Replaced by testFromInlineToAssertThrows_classDiff since TestOperationDiff depends on UMLClassBaseDiff")
        @Test
        public void testFromInlineToAssertThrows() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            Assert.assertEquals(2, classDiff.operationBodyMapperList.size());
            var testMethodMapperOptional = classDiff.operationBodyMapperList.stream().filter(UMLOperationBodyMapper::involvesTestMethods).findAny();
            Assert.assertTrue(testMethodMapperOptional.isPresent());
            var mapper = testMethodMapperOptional.get();
            var opDiff = new UMLOperationDiff(mapper);
            Assert.assertNull(opDiff.getTestOperationDiff());
            var refactorings = opDiff.getRefactorings();
            Assert.assertNotNull(opDiff.getTestOperationDiff());
            Assert.assertEquals(2, refactorings.size());
            var expectedClassNames = Set.of("gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring", "gr.uom.java.xmi.diff.ExpectedAnnotationToAssertThrowsRefactoring");
            Assert.assertTrue(expectedClassNames.containsAll(refactorings.stream().map(Object::getClass).map(Class::getName).collect(Collectors.toUnmodifiableSet())));
        }
        @Test
        public void testFromInlineToAssertThrows_classDiff() throws RefactoringMinerTimedOutException {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var refactorings = classDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            var expectedClassNames = Set.of("gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring", "gr.uom.java.xmi.diff.ExpectedAnnotationToAssertThrowsRefactoring");
            Assert.assertTrue(expectedClassNames.containsAll(refactorings.stream().map(Object::getClass).map(Class::getName).collect(Collectors.toUnmodifiableSet())));
        }
        @Test
        public void testFromInlineToAssertThrows_modelDiff() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            var expectedClassNames = Set.of("gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring", "gr.uom.java.xmi.diff.ExpectedAnnotationToAssertThrowsRefactoring");
            Assert.assertTrue(expectedClassNames.containsAll(refactorings.stream().map(Object::getClass).map(Class::getName).collect(Collectors.toUnmodifiableSet())));
        }
    }

    @Ignore("The base from what the implementation emerged (Do not test implementation but the implementation's dependencies)")
    public static class ExploringTest extends ModelDiffFieldSetUp {

        @Test
        public void testFromInlineToAssertThrows() throws RefactoringMinerTimedOutException {
            ModifyMethodAnnotationRefactoring modifyAnnotationRefactoring = detectModifyMethodAnnotationRefactoring();
            String expectedException = detectExpectedExceptionTypeLiteral(modifyAnnotationRefactoring.getAnnotationBefore());

            var after = modifyAnnotationRefactoring.getOperationAfter();
            var assertThrows = getAssertThrows(after);
            Assert.assertEquals("Number of assertThrows call is not 1", 1, assertThrows.size());
            var args = assertThrows.get(0).getArguments();
            var exceptionClassLiteral = args.get(0);
            Assert.assertEquals(expectedException, exceptionClassLiteral);

            verifyAssertThrowsLambdaHasPreviousTestBodyStatements(after, args.get(1));
        }

        private ModifyMethodAnnotationRefactoring detectModifyMethodAnnotationRefactoring() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals("There should be two refactorings in the example test method", 2, refactorings.size());
            var refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.REPLACE_EXPECTED_WITH_ASSERT_THROWS)).findAny();
            Assert.assertTrue("Migration from @Test(expected) to assertThrows not detected", refactoring.isPresent());
            refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION)).findAny();
            Assert.assertTrue("@Test memberValuePair change not detected", refactoring.isPresent());
            return (ModifyMethodAnnotationRefactoring) refactoring.get();
        }

        private String detectExpectedExceptionTypeLiteral(UMLAnnotation before) {
            Assert.assertTrue("@Test(expected) normalAnnotation not found", hasExpectedException(before));
            var expectedException = before.getMemberValuePairs().get("expected");
            Assert.assertEquals("@Test(expected) should contain a single type literal", 1, expectedException.getTypeLiterals().size());
            return expectedException.getExpression();
        }

        private void verifyAssertThrowsLambdaHasPreviousTestBodyStatements(UMLOperation after, String lambdaExpression) {
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
                    .filter((op) -> op.getMethodName().equals("assertThrows") &&
                            (op.getExpression().equals("Assert") || op.getExpression().equals("Assertions")))
                    .collect(Collectors.toList());
        }
    }


}
