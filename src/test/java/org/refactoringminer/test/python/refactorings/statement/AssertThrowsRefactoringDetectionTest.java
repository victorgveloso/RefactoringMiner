package org.refactoringminer.test.python.refactorings.statement;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssertThrowsRefactoringDetectionTest {


    @Test
    void detectsAssertThrows_pytest() throws Exception {
        String beforePythonCode = """
            import mymod
            
            def test1():
                try:
                    mymod.myfunc(5)
                except SomeCoolException:
                    pass
                else:
                    assert False, "SomeCoolException not raised"
            """;

        String afterPythonCode = """
            import mymod
            
            def test1():
                with pytest.raises(SomeCoolException) as e_info:
                    mymod.myfunc(5)
            """;

        Map<String, String> beforeFiles = Map.of("exc_test.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("exc_test.py", afterPythonCode);

        assertAssertThrowsRefactoringDetected(beforeFiles, afterFiles, "SomeCoolException");
    }


    @Test
    void detectsAssertThrows_unittest_withMethodReference() throws Exception {
        String beforePythonCode = """
            import mymod
            
            class MyTestCase(unittest.TestCase):
                def test1(self):
                    try:
                        mymod.myfunc()
                    except SomeCoolException:
                        pass
                    else:
                        self.fail("SomeCoolException not raised")
            """;

        String afterPythonCode = """
            import mymod
            
            class MyTestCase(unittest.TestCase):
                def test1(self):
                    self.assertRaises(SomeCoolException, mymod.myfunc)
            """;

        Map<String, String> beforeFiles = Map.of("io.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("io.py", afterPythonCode);

        assertAssertThrowsRefactoringDetected(beforeFiles, afterFiles, "SomeCoolException");
    }


    @Test
    void detectsAssertThrows_unittest_withLambda() throws Exception {
        String beforePythonCode = """
            import mymod
            
            class MyTestCase(unittest.TestCase):
                def test1(self):
                    try:
                        mymod.myfunc(5)
                    except SomeCoolException:
                        pass
                    else:
                        self.fail("SomeCoolException not raised")
            """;

        String afterPythonCode = """
            import mymod
            
            class MyTestCase(unittest.TestCase):
                def test1(self):
                    self.assertRaises(SomeCoolException, lambda: mymod.myfunc(5))
            """;

        Map<String, String> beforeFiles = Map.of("io.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("io.py", afterPythonCode);

        assertAssertThrowsRefactoringDetected(beforeFiles, afterFiles, "SomeCoolException");
    }

    private void assertAssertThrowsRefactoringDetected(Map<String, String> beforeFiles, Map<String, String> afterFiles, String thrownException) throws IOException, RefactoringMinerTimedOutException {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertTrue(isAssertThrowsRefactoringFound(thrownException, refactorings), "Expected Try-With-Resources refactoring not detected.");
    }

    private static boolean isAssertThrowsRefactoringFound(String thrownException, List<Refactoring> refactorings) {
        for (Refactoring r : refactorings) {
            if (RefactoringType.ASSERT_THROWS.equals(r.getRefactoringType())) {
                AssertThrowsRefactoring atr = (AssertThrowsRefactoring) r;
                if (atr.getCall().arguments().get(0).equals(thrownException)) {
                    return true;
                }
            }
        }
        return false;
    }
}
