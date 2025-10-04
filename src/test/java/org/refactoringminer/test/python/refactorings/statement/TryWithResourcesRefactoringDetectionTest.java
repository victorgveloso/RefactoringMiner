package org.refactoringminer.test.python.refactorings.statement;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.diff.TryWithResourcesRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TryWithResourcesRefactoringDetectionTest {


    @Test
    void detectsTryWithResources_OpenFile() throws Exception {
        String beforePythonCode = """
            def load_file(filename):
                try:
                    file = open(filename, 'r')
                    data = file.read()
                    file.close()
                    return data
                except FileNotFoundError as e:
                    print(f"File not found: {e}")
                    return None
                except IOError as e:
                    print(f"An error occurred: {e}")
                    return None
            """;

        String afterPythonCode = """
            def load_file(filename):
                try:
                    with open(filename, 'r') as file:
                        data = file.read()
                    return data
                except FileNotFoundError as e:
                    print(f"File not found: {e}")
                    return None
                except IOError as e:
                    print(f"An error occurred: {e}")
                    return None
            """;

        Map<String, String> beforeFiles = Map.of("io.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("io.py", afterPythonCode);

        assertTryWithResourcesRefactoringDetected(beforeFiles, afterFiles, "file");
    }

    private void assertTryWithResourcesRefactoringDetected(Map<String, String> beforeFiles, Map<String, String> afterFiles, String file) throws IOException, RefactoringMinerTimedOutException {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();
        assertTrue(isTryWithResourcesRefactoringFound(file, refactorings), "Expected Try-With-Resources refactoring not detected.");
    }

    private static boolean isTryWithResourcesRefactoringFound(String file, List<Refactoring> refactorings) {
        for (Refactoring r : refactorings) {
            if (RefactoringType.TRY_WITH_RESOURCES.equals(r.getRefactoringType())) {
                TryWithResourcesRefactoring twr = (TryWithResourcesRefactoring) r;
                TryStatementObject tryAfter = twr.getTryAfter();
                for (AbstractExpression v : tryAfter.getExpressions()) {
                    if (v.toString().contains(file)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
