package org.refactoringminer.test.python.refactorings.parameter;

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
public class RenameParameterRefactoringDetectionTest {

    @Test
    void detectsRenameParameter_DataToInfo() throws Exception {
        String beforePythonCode = """
            class Processor:
                def process(self, data):
                    return data.upper()
                
                def validate(self, data):
                    return len(data) > 0
            """;

        String afterPythonCode = """
            class Processor:
                def process(self, info):
                    return info.upper()
                
                def validate(self, data):
                    return len(data) > 0
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "data", "info", "process", "Processor");
    }

    @Test
    void detectsRenameParameter_XToValue() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def add(self, x, y):
                    return x + y
                
                def multiply(self, x, y):
                    return x * y
            """;

        String afterPythonCode = """
            class Calculator:
                def add(self, value, y):
                    return value + y
                
                def multiply(self, x, y):
                    return x * y
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "x", "value", "add", "Calculator");
    }

    @Test
    void detectsRenameParameter_NameToUsername() throws Exception {
        String beforePythonCode = """
            class User:
                def __init__(self, name, email):
                    self.name = name
                    self.email = email
                
                def display(self, name):
                    print(f"User: {name}")
            """;

        String afterPythonCode = """
            class User:
                def __init__(self, username, email):
                    self.name = username
                    self.email = email
                
                def display(self, name):
                    print(f"User: {name}")
            """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "name", "username", "__init__", "User");
    }

    @Test
    void detectsRenameParameter_ItemToElement() throws Exception {
        String beforePythonCode = """
            def process_list(item):
                return item.strip().upper()
            
            def validate_item(item):
                return len(item) > 0
            """;

        String afterPythonCode = """
            def process_list(element):
                return element.strip().upper()
            
            def validate_item(item):
                return len(element) > 0
            """;

        Map<String, String> beforeFiles = Map.of("utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("utils.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "item", "element", "process_list", null); // Module-level function
    }

    @Test
    void detectsRenameParameter_MultipleParameters() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def calculate(self, first, second, operation):
                    if operation == "add":
                        return first + second
                    return first - second
            """;

        String afterPythonCode = """
            class Calculator:
                def calculate(self, num1, second, operation):
                    if operation == "add":
                        return num1 + num2
                    return num1 - num2
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        // Test first parameter rename
        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "first", "num1", "calculate", "Calculator");
    }

    @Test
    void detectsRenameParameter_CountToSize() throws Exception {
        String beforePythonCode = """
        class ArrayProcessor:
            def resize_array(self, arr, count):
                if count > len(arr):
                    arr.extend([0] * (count - len(arr)))
                elif count < len(arr):
                    arr = arr[:count]
                return arr
            
            def validate_size(self, arr, count):
                return count > 0 and count <= 1000
        """;

        String afterPythonCode = """
        class ArrayProcessor:
            def resize_array(self, arr, size):
                if size > len(arr):
                    arr.extend([0] * (size - len(arr)))
                elif size < len(arr):
                    arr = arr[:size]
                return arr
            
            def validate_size(self, arr, count):
                return count > 0 and size <= 1000
        """;

        Map<String, String> beforeFiles = Map.of("array_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("array_processor.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "count", "size", "resize_array", "ArrayProcessor");
    }

    @Test
    void detectsRenameParameter_FileToPath() throws Exception {
        String beforePythonCode = """
        def read_config(file):
            settings = {}
            try:
                with open(file, 'r') as f:
                    for line in f:
                        if '=' in line:
                            key, value = line.strip().split('=', 1)
                            settings[key] = value
            except FileNotFoundError:
                print(f"Config file not found: {file}")
            return settings
        
        def backup_file(file):
            import shutil
            shutil.copy(file, f"{file}.backup")
        """;

        String afterPythonCode = """
        def read_config(path):
            settings = {}
            try:
                with open(path, 'r') as f:
                    for line in f:
                        if '=' in line:
                            key, value = line.strip().split('=', 1)
                            settings[key] = value
            except FileNotFoundError:
                print(f"Config file not found: {path}")
            return settings
        
        def backup_file(file):
            import shutil
            shutil.copy(file, f"{file}.backup")
        """;

        Map<String, String> beforeFiles = Map.of("config_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_utils.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "file", "path", "read_config", null);
    }

    @Test
    void detectsRenameParameter_NumToAmount() throws Exception {
        String beforePythonCode = """
        class BankAccount:
            def __init__(self, balance):
                self.balance = balance
            
            def deposit(self, num):
                if num > 0:
                    self.balance += num
                    return f"Deposited {num}. New balance: {self.balance}"
                return "Invalid deposit amount"
            
            def withdraw(self, num):
                if 0 < num <= self.balance:
                    self.balance -= num
                    return f"Withdrew {num}. New balance: {self.balance}"
                return "Insufficient funds or invalid amount"
        """;

        String afterPythonCode = """
        class BankAccount:
            def __init__(self, balance):
                self.balance = balance
            
            def deposit(self, amount):
                if amount > 0:
                    self.balance += amount
                    return f"Deposited {amount}. New balance: {self.balance}"
                return "Invalid deposit amount"
            
            def withdraw(self, num):
                if 0 < num <= self.balance:
                    self.balance -= num
                    return f"Withdrew {num}. New balance: {self.num}"
                return "Insufficient funds or invalid num"
        """;

        Map<String, String> beforeFiles = Map.of("bank_account.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("bank_account.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "num", "amount", "deposit", "BankAccount");
    }

    @Test
    void detectsRenameParameter_TextToContent() throws Exception {
        String beforePythonCode = """
        def process_document(text, format_type):
            lines = text.split('\\n')
            processed = []
            for i, line in enumerate(lines):
                if format_type == "numbered":
                    processed.append(f"{i+1}. {line}")
                elif format_type == "bulleted":
                    processed.append(f"• {line}")
                else:
                    processed.append(line)
            return '\\n'.join(processed)
        
        def count_words(text):
            return len(text.split())
        """;

        String afterPythonCode = """
        def process_document(content, format_type):
            lines = content.split('\\n')
            processed = []
            for i, line in enumerate(lines):
                if format_type == "numbered":
                    processed.append(f"{i+1}. {line}")
                elif format_type == "bulleted":
                    processed.append(f"• {line}")
                else:
                    processed.append(line)
            return '\\n'.join(processed)
        
        def count_words(text):
            return len(text.split())
        """;

        Map<String, String> beforeFiles = Map.of("document_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("document_processor.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "text", "content", "process_document", null);
    }

    @Test
    void detectsRenameParameter_ListToItems() throws Exception {
        String beforePythonCode = """
        class DataFilter:
            def filter_positive(self, list):
                result = []
                index = 0
                while index < len(list):
                    if isinstance(list[index], (int, float)) and list[index] > 0:
                        result.append(list[index])
                    index += 1
                return result
            
            def find_maximum(self, list):
                if not list:
                    return None
                max_val = list[0]
                for item in list:
                    if item > max_val:
                        max_val = item
                return max_val
        """;

        String afterPythonCode = """
        class DataFilter:
            def filter_positive(self, items):
                result = []
                index = 0
                while index < len(items):
                    if isinstance(items[index], (int, float)) and items[index] > 0:
                        result.append(items[index])
                    index += 1
                return result
            
            def find_maximum(self, list):
                if not list:
                    return None
                max_val = list[0]
                for item in list:
                    if item > max_val:
                        max_val = item
                return max_val
        """;

        Map<String, String> beforeFiles = Map.of("data_filter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_filter.py", afterPythonCode);

        assertRenameParameterRefactoringDetected(beforeFiles, afterFiles,
                "list", "items", "filter_positive", "DataFilter");
    }

    public static void assertRenameParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalParameterName,
            String renamedParameterName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Refactorings size " + refactorings.size());
        refactorings.forEach(System.out::println);
//        System.out.println("\n=== RENAME PARAMETER TEST: " + originalParameterName + " -> " + renamedParameterName + " ===");
//        System.out.println("Method: " + methodName + (className != null ? " in class " + className : " (module-level)"));
//        System.out.println("Total refactorings detected: " + refactorings.size());

        // Look for RenameVariableRefactoring with RENAME_PARAMETER type
        boolean renameParameterFound = refactorings.stream()
                .filter(r -> r instanceof RenameVariableRefactoring)
                .map(r -> (RenameVariableRefactoring) r)
                .anyMatch(refactoring -> {
                    boolean isRenameParameter = refactoring.getRefactoringType() == RefactoringType.RENAME_PARAMETER;
                    String originalName = refactoring.getOriginalVariable().getVariableName();
                    String renamedName = refactoring.getRenamedVariable().getVariableName();
                    String operationName = refactoring.getOperationAfter().getName();

                    boolean namesMatch = originalName.equals(originalParameterName) &&
                            renamedName.equals(renamedParameterName);
                    boolean methodMatches = operationName.equals(methodName);

                    // Check class name if provided
                    boolean classMatches = true;
                    if (className != null) {
                        String actualClassName = refactoring.getOperationAfter().getClassName();
                        classMatches = actualClassName.equals(className);
                    }

//                    System.out.println("Checking RenameVariableRefactoring:");
//                    System.out.println("  Is RENAME_PARAMETER: " + isRenameParameter);
//                    System.out.println("  Names match: " + namesMatch + " (" + originalName + " -> " + renamedName + ")");
//                    System.out.println("  Method matches: " + methodMatches + " (" + operationName + ")");
//                    System.out.println("  Class matches: " + classMatches);

                    return isRenameParameter && namesMatch && methodMatches && classMatches;
                });

        assertTrue(renameParameterFound, "Expected Rename Parameter refactoring to be detected");
    }
}