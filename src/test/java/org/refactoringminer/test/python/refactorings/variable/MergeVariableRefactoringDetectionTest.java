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
public class MergeVariableRefactoringDetectionTest {

    @Test
    void detectsMergeVariable_TwoVariablesToOne() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def calculate_total(self, items):
                    subtotal = sum(item.price for item in items)
                    tax_amount = subtotal * 0.1
                    return subtotal + tax_amount
            """;

        String afterPythonCode = """
            class Calculator:
                def calculate_total(self, items):
                    total = sum(item.price for item in items)
                    total += total * 0.1
                    return total
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertMergeVariableRefactoringDetected(beforeFiles, afterFiles,
                Set.of("subtotal", "tax_amount"), "total", "calculate_total", "Calculator");
    }

    @Test
    void detectsMergeVariable_MultipleTemporaryVariables() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                def process_data(self, data):
                    filtered_data = [x for x in data if x > 0]
                    sorted_data = sorted(filtered_data)
                    transformed_data = [x * 2 for x in sorted_data]
                    return transformed_data
            """;

        String afterPythonCode = """
            class DataProcessor:
                def process_data(self, data):
                    result = [x for x in data if x > 0]
                    result = sorted(result)
                    result = [x * 2 for x in result]
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertMergeVariableRefactoringDetected(beforeFiles, afterFiles,
                Set.of("filtered_data", "sorted_data", "transformed_data"), "result", "process_data", "DataProcessor");
    }

    @Test
    void detectsMergeVariable_CoordinateVariables() throws Exception {
        String beforePythonCode = """
            class Point:
                def calculate_distance(self, other):
                    x_diff = self.x - other.x
                    y_diff = self.y - other.y
                    return (x_diff ** 2 + y_diff ** 2) ** 0.5
            """;

        String afterPythonCode = """
            class Point:
                def calculate_distance(self, other):
                    diff = self.x - other.x
                    diff_y = self.y - other.y
                    return (diff ** 2 + diff_y ** 2) ** 0.5
            """;

        Map<String, String> beforeFiles = Map.of("point.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("point.py", afterPythonCode);

        assertMergeVariableRefactoringDetected(beforeFiles, afterFiles,
                Set.of("x_diff"), "diff", "calculate_distance", "Point");
    }

    @Test
    void detectsMergeVariable_StringConcatenation() throws Exception {
        String beforePythonCode = """
            def format_message(name, age):
                greeting = f"Hello {name}"
                age_part = f"You are {age} years old"
                return greeting + ", " + age_part
            """;

        String afterPythonCode = """
            def format_message(name, age):
                message = f"Hello {name}"
                message += f", You are {age} years old"
                return message
            """;

        Map<String, String> beforeFiles = Map.of("formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("formatter.py", afterPythonCode);

        assertMergeVariableRefactoringDetected(beforeFiles, afterFiles,
                Set.of("greeting", "age_part"), "message", "format_message", "");
    }

    public static void assertMergeVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            Set<String> mergedVariableNames,
            String newVariableName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== MERGE VARIABLE TEST: " + mergedVariableNames + " -> " + newVariableName + " ===");
        System.out.println("Merged variables: " + mergedVariableNames);
        System.out.println("New variable: " + newVariableName);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean mergeVariableFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.MERGE_VARIABLE.equals(r.getRefactoringType()) &&
                        r.toString().contains(newVariableName) &&
                        r.toString().contains(methodName));

        if (!mergeVariableFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected merge variable refactoring from variables " + mergedVariableNames +
                    " to '" + newVariableName + "' in method '" + methodName + "' was not detected");
        }

        assertTrue(mergeVariableFound, "Expected Merge Variable refactoring to be detected");
    }
}