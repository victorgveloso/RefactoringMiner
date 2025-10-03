package org.refactoringminer.test.python.refactorings.parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertReorderParameterRefactoringDetected;

@Isolated
public class ReorderParameterRefactoringDetectionTest {

    @Test
    void detectsSimpleReorderParameter() throws Exception {
        String beforePythonCode = """
            class MathOps:
                def add(self, x, y):
                    return x + y
            """;
        String afterPythonCode = """
            class MathOps:
                def add(self, y, x):
                    return x + y
            """;

        Map<String, String> beforeFiles = Map.of("tests/mathops.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/mathops.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "MathOps",
                "add",
                new String[]{"x : Object", "y : Object"},
                new String[]{"y : Object", "x : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithArithmetic() throws Exception {
        String beforePythonCode = """
        class Calculator:
            def divide(self, dividend, divisor):
                return dividend / divisor
        """;
        String afterPythonCode = """
        class Calculator:
            def divide(self, divisor, dividend):
                return dividend / divisor
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "Calculator",
                "divide",
                new String[]{"dividend : Object", "divisor : Object"},
                new String[]{"divisor : Object", "dividend : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithStringOperation() throws Exception {
        String beforePythonCode = """
        class TextFormatter:
            def format_text(self, text, prefix):
                return prefix + text
        """;
        String afterPythonCode = """
        class TextFormatter:
            def format_text(self, prefix, text):
                return prefix + text
        """;

        Map<String, String> beforeFiles = Map.of("formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("formatter.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "TextFormatter",
                "format_text",
                new String[]{"text : Object", "prefix : Object"},
                new String[]{"prefix : Object", "text : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithListAccess() throws Exception {
        String beforePythonCode = """
        class ListUtils:
            def get_element(self, items, index):
                return items[index]
        """;
        String afterPythonCode = """
        class ListUtils:
            def get_element(self, index, items):
                return items[index]
        """;

        Map<String, String> beforeFiles = Map.of("list_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("list_utils.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "ListUtils",
                "get_element",
                new String[]{"items : Object", "index : Object"},
                new String[]{"index : Object", "items : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithConditional() throws Exception {
        String beforePythonCode = """
        class Comparator:
            def compare(self, a, b):
                if a > b:
                    return a
                return b
        """;
        String afterPythonCode = """
        class Comparator:
            def compare(self, b, a):
                if a > b:
                    return a
                return b
        """;

        Map<String, String> beforeFiles = Map.of("comparator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("comparator.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "Comparator",
                "compare",
                new String[]{"a : Object", "b : Object"},
                new String[]{"b : Object", "a : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithDictionaryOperation() throws Exception {
        String beforePythonCode = """
        class DataStore:
            def store_value(self, key, value):
                data = {key: value}
                return data
        """;
        String afterPythonCode = """
        class DataStore:
            def store_value(self, value, key):
                data = {key: value}
                return data
        """;

        Map<String, String> beforeFiles = Map.of("data_store.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_store.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "DataStore",
                "store_value",
                new String[]{"key : Object", "value : Object"},
                new String[]{"value : Object", "key : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithLoop() throws Exception {
        String beforePythonCode = """
        class LoopProcessor:
            def repeat_action(self, count, action):
                for i in range(count):
                    print(action)
        """;
        String afterPythonCode = """
        class LoopProcessor:
            def repeat_action(self, action, count):
                for i in range(count):
                    print(action)
        """;

        Map<String, String> beforeFiles = Map.of("loop_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("loop_processor.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "LoopProcessor",
                "repeat_action",
                new String[]{"count : Object", "action : Object"},
                new String[]{"action : Object", "count : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithMethodCall() throws Exception {
        String beforePythonCode = """
        class FileHandler:
            def copy_file(self, source, destination):
                return self.transfer(source, destination)
            
            def transfer(self, src, dst):
                return f"Copying {src} to {dst}"
        """;
        String afterPythonCode = """
        class FileHandler:
            def copy_file(self, destination, source):
                return self.transfer(source, destination)
            
            def transfer(self, src, dst):
                return f"Copying {src} to {dst}"
        """;

        Map<String, String> beforeFiles = Map.of("file_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_handler.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "FileHandler",
                "copy_file",
                new String[]{"source : Object", "destination : Object"},
                new String[]{"destination : Object", "source : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithAttributeAssignment() throws Exception {
        String beforePythonCode = """
        class Rectangle:
            def set_dimensions(self, width, height):
                self.width = width
                self.height = height
        """;
        String afterPythonCode = """
        class Rectangle:
            def set_dimensions(self, height, width):
                self.width = width
                self.height = height
        """;

        Map<String, String> beforeFiles = Map.of("rectangle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("rectangle.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "Rectangle",
                "set_dimensions",
                new String[]{"width : Object", "height : Object"},
                new String[]{"height : Object", "width : Object"}
        );
    }

    @Test
    void detectsReorderParameterWithMultipleOperations() throws Exception {
        String beforePythonCode = """
        class MathOperations:
            def calculate(self, base, rate, time):
                result = base * rate * time
                return result + base
        """;
        String afterPythonCode = """
        class MathOperations:
            def calculate(self, rate, time, base):
                result = base * rate * time
                return result + base
        """;

        Map<String, String> beforeFiles = Map.of("math_operations.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("math_operations.py", afterPythonCode);

        assertReorderParameterRefactoringDetected(
                beforeFiles,
                afterFiles,
                "MathOperations",
                "calculate",
                new String[]{"base : Object", "rate : Object", "time : Object"},
                new String[]{"rate : Object", "time : Object", "base : Object"}
        );
    }

}
