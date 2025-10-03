package org.refactoringminer.test.python.refactorings.variable;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class SplitVariableRefactoringDetectionTest {

    @Test
    void detectsSplitVariable_SingleResultToMultipleVariables() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                def process_data(self, data):
                    result = [x for x in data if x > 0]
                    result = sorted(result)
                    result = [x * 2 for x in result]
                    return result
            """;

        String afterPythonCode = """
            class DataProcessor:
                def process_data(self, data):
                    filtered_data = [x for x in data if x > 0]
                    sorted_data = sorted(filtered_data)
                    transformed_data = [x * 2 for x in sorted_data]
                    return transformed_data
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertSplitVariableRefactoringDetected(beforeFiles, afterFiles,
                "result", Set.of("filtered_data", "sorted_data", "transformed_data"), "process_data", "DataProcessor");
    }

    @Test
    void detectsSplitVariable_TupleUnpacking() throws Exception {
        String beforePythonCode = """
            def calculate_stats(numbers):
                stats = (sum(numbers), len(numbers))
                avg = stats[0] / stats[1]
                return f"Average: {avg}, Total: {stats[0]}, Count: {stats[1]}"
            """;

        String afterPythonCode = """
            def calculate_stats(numbers):
                total = sum(numbers)
                count = len(numbers)
                avg = total / count
                return f"Average: {avg}, Total: {total}, Count: {count}"
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertSplitVariableRefactoringDetected(beforeFiles, afterFiles,
                "stats", Set.of("total", "count"), "calculate_stats", "");
    }

    @Test
    void detectsSplitVariable_CoordinateToSeparateComponents() throws Exception {
        String beforePythonCode = """
            class Point:
                def move_by(self, delta):
                    delta_x = delta[0]
                    delta_y = delta[1]
                    self.x += delta_x
                    self.y += delta_y
            """;

        String afterPythonCode = """
            class Point:
                def move_by(self, delta_x, delta_y):
                    self.x += delta_x
                    self.y += delta_y
            """;

        Map<String, String> beforeFiles = Map.of("point.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("point.py", afterPythonCode);

        assertSplitVariableRefactoringDetected(beforeFiles, afterFiles,
                "delta", Set.of("delta_x", "delta_y"), "move_by", "Point");
    }

    @Test
    void detectsSplitVariable_FilePathToComponents() throws Exception {
        String beforePythonCode = """
            def process_file(file_path):
                path_info = file_path.rsplit('.', 1)
                name = path_info[0]
                extension = path_info[1] if len(path_info) > 1 else ""
                return f"Processing {name} with extension {extension}"
            """;

        String afterPythonCode = """
            def process_file(file_path):
                name, extension = file_path.rsplit('.', 1) if '.' in file_path else (file_path, "")
                return f"Processing {name} with extension {extension}"
            """;

        Map<String, String> beforeFiles = Map.of("file_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_utils.py", afterPythonCode);

        assertSplitVariableRefactoringDetected(beforeFiles, afterFiles,
                "path_info", Set.of("name", "extension"), "process_file", "");
    }

    public static void assertSplitVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalVariableName,
            Set<String> splitVariableNames,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== SPLIT VARIABLE TEST: " + originalVariableName + " -> " + splitVariableNames + " ===");
        System.out.println("Original variable: " + originalVariableName);
        System.out.println("Split variables: " + splitVariableNames);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());

        boolean splitVariableFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.SPLIT_VARIABLE.equals(r.getRefactoringType()) &&
                        r.toString().contains(originalVariableName) &&
                        r.toString().contains(methodName));

        if (!splitVariableFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected split variable refactoring from variable '" + originalVariableName +
                    "' to variables " + splitVariableNames + " in method '" + methodName + "' was not detected");
        }

        assertTrue(splitVariableFound, "Expected Split Variable refactoring to be detected");
    }
}