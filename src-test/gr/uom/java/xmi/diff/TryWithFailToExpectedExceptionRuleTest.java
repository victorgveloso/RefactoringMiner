package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(Enclosed.class)
public class TryWithFailToExpectedExceptionRuleTest {
    abstract public static class ModelDiffFieldSetUp {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
            var tryCatchVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_TryCatchVersion();
            var tryCatchVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(tryCatchVersionTestMethod);
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", tryCatchVersionTestClass), Set.of()).getUmlModel();
            var ruleVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_RuleVersion();
            var ruleVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(ruleVersionTestMethod);
            var after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", ruleVersionTestClass), Set.of()).getUmlModel();
            modelDiff = before.diff(after);
        }
    }
    public static class ImplementationTest extends ModelDiffFieldSetUp {
        @Test
        public void testFromTryFailToRule() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var operationMappers = classDiff.getOperationBodyMapperList();
            Assert.assertEquals(2, operationMappers.size());
            var possibleMapper = operationMappers.stream().filter(op-> !(op.operationNameEditDistance() == 0 && op.getOperation1().getName().equals("setUp"))).findAny();
            Assert.assertTrue(possibleMapper.isPresent());
            var mapper = possibleMapper.get();
            var detector = new TryWithFailToExpectedExceptionRuleDetection(mapper, classDiff.addedAttributes);
            var refactoring = detector.check();
            Assert.assertNotNull(refactoring);
            Assert.assertNotNull(detector.getExpectedExceptionFieldDeclaration());
            Assert.assertEquals(1, detector.getTryStatements().size());
            Assert.assertEquals(1, detector.getAssertFailInvocationsFound().size());
            Assert.assertEquals(1, detector.getCapturedExceptions().size());
            Assert.assertEquals(1, detector.getExpectInvocations().size());
        }

    }
    public static class ExploringTest extends ModelDiffFieldSetUp {

        @Test
        public void testFromTryFailToRule() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var operationMappers = classDiff.getOperationBodyMapperList();
            Assert.assertEquals(2, operationMappers.size());
            var possibleMapper = operationMappers.stream().filter(op-> !(op.operationNameEditDistance() == 0 && op.getOperation1().getName().equals("setUp"))).findAny();
            Assert.assertTrue(possibleMapper.isPresent());
            var mapper = possibleMapper.get();

            var removedCompositeStmts = mapper.getNonMappedInnerNodesT1();
            var addedStmts = mapper.getNonMappedLeavesT2();
            Assert.assertEquals(3, removedCompositeStmts.size() + addedStmts.size());

            TryStatementObject tryStatement = getTryStatement(removedCompositeStmts);
            List<String> capturedExceptions = getExceptions(tryStatement);
            Assert.assertEquals(1, capturedExceptions.size());
            Assert.assertEquals("IllegalArgumentException", capturedExceptions.get(0));
            Assert.assertEquals(2, tryStatement.getStatements().size());
            Assert.assertTrue("Assert fail should be invoked at the end of TryStatement", hasAssertFailInvocationAtTheEnd(tryStatement));

            var thrownExpectInvocations = extractMethodInvocations(addedStmts).filter(invocation -> hasExpectedException(capturedExceptions, invocation)).collect(Collectors.toList());
            Assert.assertEquals(1, thrownExpectInvocations.size());
            var thrownExpect = thrownExpectInvocations.get(0);
            var ruleAnnotatedVariableName = thrownExpect.getExpression();
            Assert.assertEquals(1, thrownExpect.getArguments().size());

            var ruleFieldDeclaration = classDiff.getAddedAttributes().get(0);
            Assert.assertEquals("ExpectedException", ruleFieldDeclaration.getType().getClassType());
            Assert.assertEquals(ruleAnnotatedVariableName, ruleFieldDeclaration.getName());
            Assert.assertEquals(1, thrownExpect.getArguments().size());
            Assert.assertEquals("IllegalArgumentException.class", thrownExpect.getArguments().get(0));
        }

        private boolean hasExpectedException(List<String> capturedExceptions, OperationInvocation invocation) {
            return invocation.getMethodName().equals("expect") && isAnyArgumentPassedTo(capturedExceptions, invocation);
        }

        private boolean isAnyArgumentPassedTo(List<String> arguments, OperationInvocation invocation) {
            return arguments.contains(invocation.getArguments().get(0).replace(".class",""));
        }

        private Stream<OperationInvocation> extractMethodInvocations(List<StatementObject> addedStmts) {
            return addedStmts.stream().flatMap(st -> st.getMethodInvocationMap().values().stream().flatMap(Collection::stream));
        }

        private boolean hasAssertFailInvocationAtTheEnd(TryStatementObject tryStatement) {
            var lastStatement = tryStatement.getStatements().get(tryStatement.getStatements().size() - 1);
            var operationInvocationsInLastStatement = new ArrayList<>(lastStatement.getMethodInvocationMap().values()).get(0);
            var assertFailInvocations = operationInvocationsInLastStatement.stream().filter(invocation -> invocation.getExpression().equals("Assert") && invocation.getMethodName().equals("fail")).collect(Collectors.toList());
            return assertFailInvocations.size() > 0;
        }

        private List<String> getExceptions(TryStatementObject tryStatement) {
            return tryStatement.getCatchClauses().stream().flatMap(clause -> clause.getVariableDeclarations().stream().map(variable -> variable.getType().getClassType())).filter(classType -> classType.endsWith("Exception")).collect(Collectors.toList());
        }

        private TryStatementObject getTryStatement(List<CompositeStatementObject> a1) {
            Optional<TryStatementObject> possibleTry = a1.stream().filter(st->st instanceof TryStatementObject).map(st -> (TryStatementObject)st).findAny();
            Assert.assertTrue(possibleTry.isPresent());
            return possibleTry.get();
        }

    }
}
