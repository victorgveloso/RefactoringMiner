package org.refactoringminer.test.python.refactorings.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertInlineMethodRefactoringDetected;

@Isolated
public class InlineMethodRefactoringDetectionTest {

    @Test
    void detectsInlineMethodWithExpression() throws Exception {
        String beforePythonCode =
                "class Calculator:\n" +
                        "    def add(self, a, b):\n" +
                        "        result = sum_impl(a, b)\n" +
                        "        return result\n" +
                        "\n" +
                        "    def sum_impl(self, a, b):\n" +
                        "        return a + b\n";

        String afterPythonCode =
                "class Calculator:\n" +
                        "    def add(self, a, b):\n" +
                        "        result = a + b\n" +
                        "        return result\n";

        Map<String, String> beforeFiles = Map.of("tests/calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/calculator.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "add", "sum_impl");
    }

    @Test
    void detectsInlineMethodWithSimpleReturn() throws Exception {
        String beforePythonCode = """
        class MathUtils:
            def calculate(self, x, y):
                doubled = self.double_value(x)
                return doubled + y
            
            def double_value(self, value):
                return value * 2
        """;

        String afterPythonCode = """
        class MathUtils:
            def calculate(self, x, y):
                doubled = x * 2
                return doubled + y
        """;

        Map<String, String> beforeFiles = Map.of("tests/math_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/math_utils.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "calculate", "double_value");
    }

    @Test
    void detectsInlineMethodWithStringOperation() throws Exception {
        String beforePythonCode = """
        class TextProcessor:
            def format_message(self, text):
                cleaned = self.clean_text(text)
                return f"Message: {cleaned}"
            
            def clean_text(self, text):
                return text.strip().lower()
        """;

        String afterPythonCode = """
        class TextProcessor:
            def format_message(self, text):
                cleaned = text.strip().lower()
                return f"Message: {cleaned}"
        """;

        Map<String, String> beforeFiles = Map.of("text_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_processor.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "format_message", "clean_text");
    }

    @Test
    void detectsInlineMethodWithListOperation() throws Exception {
        String beforePythonCode = """
        class DataManager:
            def process_numbers(self, numbers):
                filtered = self.get_positive_numbers(numbers)
                return sum(filtered)
            
            def get_positive_numbers(self, numbers):
                return [n for n in numbers if n > 0]
        """;

        String afterPythonCode = """
        class DataManager:
            def process_numbers(self, numbers):
                filtered = [n for n in numbers if n > 0]
                return sum(filtered)
        """;

        Map<String, String> beforeFiles = Map.of("data_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_manager.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "process_numbers", "get_positive_numbers");
    }

    @Test
    void detectsInlineMethodWithConditional() throws Exception {
        String beforePythonCode = """
        class Validator:
            def check_input(self, value):
                is_valid = self.validate_range(value)
                return "Valid" if is_valid else "Invalid"
            
            def validate_range(self, value):
                return 0 <= value <= 100
        """;

        String afterPythonCode = """
        class Validator:
            def check_input(self, value):
                is_valid = 0 <= value <= 100
                return "Valid" if is_valid else "Invalid"
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "check_input", "validate_range");
    }

    @Test
    void detectsInlineMethodWithDictionaryAccess() throws Exception {
        String beforePythonCode = """
        class ConfigReader:
            def get_setting(self, config, key):
                default_val = self.get_default_value()
                return config.get(key, default_val)
            
            def get_default_value(self):
                return "unknown"
        """;

        String afterPythonCode = """
        class ConfigReader:
            def get_setting(self, config, key):
                default_val = "unknown"
                return config.get(key, default_val)
        """;

        Map<String, String> beforeFiles = Map.of("config_reader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_reader.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "get_setting", "get_default_value");
    }

    @Test
    void detectsInlineMethodWithForLoop() throws Exception {
        String beforePythonCode = """
        class ItemCounter:
            def count_active_items(self, items):
                count = self.calculate_count(items)
                return f"Total active: {count}"
            
            def calculate_count(self, items):
                total = 0
                for item in items:
                    if item.get('active', False):
                        total += 1
                return total
        """;

        String afterPythonCode = """
        class ItemCounter:
            def count_active_items(self, items):
                total = 0
                for item in items:
                    if item.get('active', False):
                        total += 1
                count = total
                return f"Total active: {count}"
        """;

        Map<String, String> beforeFiles = Map.of("item_counter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("item_counter.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "count_active_items", "calculate_count");
    }

    @Test
    void detectsInlineMethodWithArithmetic() throws Exception {
        String beforePythonCode = """
        class PriceCalculator:
            def calculate_total(self, price, tax_rate):
                tax_amount = self.compute_tax(price, tax_rate)
                return price + tax_amount
            
            def compute_tax(self, price, rate):
                return price * rate / 100
        """;

        String afterPythonCode = """
        class PriceCalculator:
            def calculate_total(self, price, tax_rate):
                tax_amount = price * tax_rate / 100
                return price + tax_amount
        """;

        Map<String, String> beforeFiles = Map.of("price_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("price_calculator.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "calculate_total", "compute_tax");
    }

    @Test
    void detectsInlineMethodWithWhileLoop() throws Exception {
        String beforePythonCode = """
        class NumberProcessor:
            def find_first_even(self, numbers):
                result = self.locate_even(numbers)
                return result if result is not None else -1
            
            def locate_even(self, numbers):
                index = 0
                while index < len(numbers):
                    if numbers[index] % 2 == 0:
                        return numbers[index]
                    index += 1
                return None
        """;

        String afterPythonCode = """
        class NumberProcessor:
            def find_first_even(self, numbers):
                index = 0
                while index < len(numbers):
                    if numbers[index] % 2 == 0:
                        result = numbers[index]
                        return result if result is not None else -1
                    index += 1
                result = None
                return result if result is not None else -1
        """;

        Map<String, String> beforeFiles = Map.of("number_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("number_processor.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "find_first_even", "locate_even");
    }

    @Test
    void detectsInlineMethodWithErrorHandling() throws Exception {
        String beforePythonCode = """
        class FileHandler:
            def read_config(self, filename):
                try:
                    content = self.safe_read_file(filename)
                    return content
                except Exception as e:
                    return f"Error: {str(e)}"
            
            def safe_read_file(self, filename):
                with open(filename, 'r') as f:
                    return f.read()
        """;

        String afterPythonCode = """
        class FileHandler:
            def read_config(self, filename):
                try:
                    with open(filename, 'r') as f:
                        content = f.read()
                    return content
                except Exception as e:
                    return f"Error: {str(e)}"
        """;

        Map<String, String> beforeFiles = Map.of("file_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_handler.py", afterPythonCode);

        assertInlineMethodRefactoringDetected(beforeFiles, afterFiles, "read_config", "safe_read_file");
    }

}