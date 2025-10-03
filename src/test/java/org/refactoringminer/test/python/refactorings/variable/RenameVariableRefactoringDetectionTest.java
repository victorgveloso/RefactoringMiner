package org.refactoringminer.test.python.refactorings.variable;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class RenameVariableRefactoringDetectionTest {

    @Test
    void detectsRenameVariable_TempToResult() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def compute(self, x, y):
                    temp = x + y
                    temp = temp * 2
                    return temp
            """;

        String afterPythonCode = """
            class Calculator:
                def compute(self, x, y):
                    result = x + y
                    result = result * 2
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "temp", "result", "compute", "Calculator");
    }

    @Test
    void detectsRenameVariable_ItemsToElements() throws Exception {
        String beforePythonCode = """
            def process_data(data):
                items = []
                for entry in data:
                    if entry.is_valid():
                        items.append(entry.name)
                return items
            """;

        String afterPythonCode = """
            def process_data(data):
                elements = []
                for entry in data:
                    if entry.is_valid():
                        elements.append(entry.name)
                return elements
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "items", "elements", "process_data", null); // Module-level function
    }

    @Test
    void detectsRenameVariable_CountToIndex() throws Exception {
        String beforePythonCode = """
            class Iterator:
                def iterate(self, collection):
                    count = 0
                    for item in collection:
                        print(f"Item {count}: {item}")
                        count += 1
                    return count
            """;

        String afterPythonCode = """
            class Iterator:
                def iterate(self, collection):
                    index = 0
                    for item in collection:
                        print(f"Item {index}: {item}")
                        index += 1
                    return index
            """;

        Map<String, String> beforeFiles = Map.of("iterator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("iterator.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "count", "index", "iterate", "Iterator");
    }


    @Test
    void detectsRenameVariable_DataToRecords() throws Exception {
        String beforePythonCode = """
        class DataAnalyzer:
            def analyze(self, input_list):
                data = []
                for item in input_list:
                    if item.get('valid', True):
                        data.append(item)
                return len(data)
        """;

        String afterPythonCode = """
        class DataAnalyzer:
            def analyze(self, input_list):
                records = []
                for item in input_list:
                    if item.get('valid', True):
                        records.append(item)
                return len(records)
        """;

        Map<String, String> beforeFiles = Map.of("analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("analyzer.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "data", "records", "analyze", "DataAnalyzer");
    }

    @Test
    void detectsRenameVariable_SumToTotal() throws Exception {
        String beforePythonCode = """
        def calculate_expenses(amounts):
            sum = 0
            for amount in amounts:
                if amount > 0:
                    sum += amount
            return sum
        """;

        String afterPythonCode = """
        def calculate_expenses(amounts):
            total = 0
            for amount in amounts:
                if amount > 0:
                    total += amount
            return total
        """;

        Map<String, String> beforeFiles = Map.of("expenses.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("expenses.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "sum", "total", "calculate_expenses", null);
    }

    @Test
    void detectsRenameVariable_IToIndex() throws Exception {
        String beforePythonCode = """
        class ListProcessor:
            def process_items(self, items):
                results = []
                i = 0
                while i < len(items):
                    if items[i] is not None:
                        results.append(f"Item {i}: {items[i]}")
                    i += 1
                return results
        """;

        String afterPythonCode = """
        class ListProcessor:
            def process_items(self, items):
                results = []
                index = 0
                while index < len(items):
                    if items[index] is not None:
                        results.append(f"Item {index}: {items[index]}")
                    index += 1
                return results
        """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "i", "index", "process_items", "ListProcessor");
    }

    @Test
    void detectsRenameVariable_ConfigToSettings() throws Exception {
        String beforePythonCode = """
        def load_configuration(filename):
            config = {}
            with open(filename, 'r') as file:
                for line in file:
                    if '=' in line:
                        key, value = line.strip().split('=', 1)
                        config[key] = value
            return config
        """;

        String afterPythonCode = """
        def load_configuration(filename):
            settings = {}
            with open(filename, 'r') as file:
                for line in file:
                    if '=' in line:
                        key, value = line.strip().split('=', 1)
                        settings[key] = value
            return settings
        """;

        Map<String, String> beforeFiles = Map.of("config_loader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_loader.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "config", "settings", "load_configuration", null);
    }

    @Test
    void detectsRenameVariable_ValueToScore() throws Exception {
        String beforePythonCode = """
        class GameScorer:
            def calculate_final_score(self, points, multiplier):
                value = points * multiplier
                if value > 1000:
                    value = value + 100  # bonus
                elif value > 500:
                    value = value + 50   # small bonus
                return value
        """;

        String afterPythonCode = """
        class GameScorer:
            def calculate_final_score(self, points, multiplier):
                score = points * multiplier
                if score > 1000:
                    score = score + 100  # bonus
                elif score > 500:
                    score = score + 50   # small bonus
                return score
        """;

        Map<String, String> beforeFiles = Map.of("scorer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("scorer.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "value", "score", "calculate_final_score", "GameScorer");
    }

    @Test
    void detectsRenameVariable_TextToMessage() throws Exception {
        String beforePythonCode = """
        def format_notification(title, content):
            text = f"{title}: {content}"
            if len(text) > 100:
                text = text[:97] + "..."
            text = text.strip()
            return text.upper()
        """;

        String afterPythonCode = """
        def format_notification(title, content):
            message = f"{title}: {content}"
            if len(message) > 100:
                message = message[:97] + "..."
            message = message.strip()
            return message.upper()
        """;

        Map<String, String> beforeFiles = Map.of("notification.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("notification.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "text", "message", "format_notification", null);
    }

    @Test
    void detectsRenameVariable_NumToCount() throws Exception {
        String beforePythonCode = """
        class InventoryManager:
            def count_valid_items(self, inventory):
                num = 0
                for item in inventory:
                    try:
                        if item['status'] == 'active' and item['quantity'] > 0:
                            num += 1
                    except KeyError:
                        continue
                return num
        """;

        String afterPythonCode = """
        class InventoryManager:
            def count_valid_items(self, inventory):
                count = 0
                for item in inventory:
                    try:
                        if item['status'] == 'active' and item['quantity'] > 0:
                            count += 1
                    except KeyError:
                        continue
                return count
        """;

        Map<String, String> beforeFiles = Map.of("inventory.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("inventory.py", afterPythonCode);

        assertRenameVariableRefactoringDetected(beforeFiles, afterFiles,
                "num", "count", "count_valid_items", "InventoryManager");
    }

    public static void assertRenameVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalVariableName,
            String renamedVariableName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        // === COMPREHENSIVE REFACTORING DEBUG OUTPUT ===
        System.out.println("\n=== RENAME VARIABLE TEST: " + originalVariableName + " -> " + renamedVariableName + " ===");
        System.out.println("Method: " + methodName + (className != null ? " in class " + className : " (module-level)"));
        System.out.println("Total refactorings detected: " + refactorings.size());

        if (refactorings.isEmpty()) {
            System.out.println("NO REFACTORINGS DETECTED");
        } else {
            for (int i = 0; i < refactorings.size(); i++) {
                Refactoring r = refactorings.get(i);
                System.out.println("Refactoring #" + (i + 1) + ":");
                System.out.println("  Type: " + r.getRefactoringType());
                System.out.println("  Name: " + r.getName());
                System.out.println("  Details: " + r.toString());

                // Additional details for RenameVariableRefactoring
                if (r instanceof RenameVariableRefactoring) {
                    RenameVariableRefactoring rvr = (RenameVariableRefactoring) r;
                    System.out.println("  [RenameVariable] Original: " + rvr.getOriginalVariable().getVariableName());
                    System.out.println("  [RenameVariable] Renamed: " + rvr.getRenamedVariable().getVariableName());
                    System.out.println("  [RenameVariable] Operation Before: " + rvr.getOperationBefore().toQualifiedString());
                    System.out.println("  [RenameVariable] Operation After: " + rvr.getOperationAfter().toQualifiedString());
                    System.out.println("  [RenameVariable] Is Local Variable: " + rvr.getOriginalVariable().isLocalVariable());
                    System.out.println("  [RenameVariable] Is Parameter: " + rvr.getOriginalVariable().isParameter());
                    System.out.println("  [RenameVariable] Is Attribute: " + rvr.getOriginalVariable().isAttribute());
                    System.out.println("  [RenameVariable] Refactoring Type: " + rvr.getRefactoringType());
                    System.out.println("  [RenameVariable] Scope: " + rvr.getOriginalVariable().getScope());
                }

                System.out.println("  Involved Classes Before: " + r.getInvolvedClassesBeforeRefactoring());
                System.out.println("  Involved Classes After: " + r.getInvolvedClassesAfterRefactoring());
                System.out.println("  ---");
            }
        }
        System.out.println("=== END REFACTORING DEBUG ===\n");

        // Look for RenameVariableRefactoring with RENAME_VARIABLE type
        boolean renameVariableFound = refactorings.stream()
                .filter(r -> r instanceof RenameVariableRefactoring)
                .map(r -> (RenameVariableRefactoring) r)
                .anyMatch(refactoring -> {
                    boolean isRenameVariable = refactoring.getRefactoringType() == RefactoringType.RENAME_VARIABLE;
                    String originalName = refactoring.getOriginalVariable().getVariableName();
                    String renamedName = refactoring.getRenamedVariable().getVariableName();
                    String operationName = refactoring.getOperationAfter().getName();

                    boolean namesMatch = originalName.equals(originalVariableName) &&
                            renamedName.equals(renamedVariableName);
                    boolean methodMatches = operationName.equals(methodName);

                    // Check class name if provided
                    boolean classMatches = true;
                    if (className != null) {
                        String actualClassName = refactoring.getOperationAfter().getClassName();
                        classMatches = actualClassName.equals(className);
                    }

                    // Ensure it's a local variable (not parameter or attribute)
                    boolean isLocalVariable = refactoring.getOriginalVariable().isLocalVariable() &&
                            refactoring.getRenamedVariable().isLocalVariable();

                    System.out.println("Checking RenameVariableRefactoring:");
                    System.out.println("  Is RENAME_VARIABLE: " + isRenameVariable);
                    System.out.println("  Names match: " + namesMatch + " (" + originalName + " -> " + renamedName + ")");
                    System.out.println("  Method matches: " + methodMatches + " (" + operationName + ")");
                    System.out.println("  Class matches: " + classMatches);
                    System.out.println("  Is Local Variable: " + isLocalVariable);

                    return isRenameVariable && namesMatch && methodMatches && classMatches && isLocalVariable;
                });
        assertTrue(renameVariableFound, "Expected Rename Variable refactoring to be detected");
    }
}