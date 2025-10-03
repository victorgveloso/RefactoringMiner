package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
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
public class MoveAndInlineMethodRefactoringDetectionTest {

    @Test
    void detectsMoveAndInlineMethod_BetweenClasses() throws Exception {
        String beforePythonCode = """
            class MathUtils:
                def calculate(self, x, y):
                    processor = DataProcessor()
                    result = processor.double_value(x)
                    return result + y
            
            class DataProcessor:
                def double_value(self, value):
                    return value * 2
            """;

        String afterPythonCode = """
            class MathUtils:
                def calculate(self, x, y):
                    result = x * 2
                    return result + y
            
            class DataProcessor:
                pass
            """;

        Map<String, String> beforeFiles = Map.of("utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("utils.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "double_value", "DataProcessor", "calculate", "MathUtils");
    }

    @Test
    void detectsMoveAndInlineMethod_HelperMethodMigration() throws Exception {
        String beforePythonCode = """
            class StringProcessor:
                def format_text(self, text):
                    formatter = TextFormatter()
                    cleaned = formatter.clean_whitespace(text)
                    return cleaned.upper()
            
            class TextFormatter:
                def clean_whitespace(self, text):
                    return text.strip().replace("  ", " ")
            """;

        String afterPythonCode = """
            class StringProcessor:
                def format_text(self, text):
                    cleaned = text.strip().replace("  ", " ")
                    return cleaned.upper()
            
            class TextFormatter:
                pass
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "clean_whitespace", "TextFormatter", "format_text", "StringProcessor");
    }

    @Test
    void detectsMoveAndInlineMethod_SimpleCalculation() throws Exception {
        String beforePythonCode = """
        class Calculator:
            def compute_total(self, base):
                helper = MathHelper()
                multiplied = helper.multiply_by_two(base)
                return multiplied + 10
        
        class MathHelper:
            def multiply_by_two(self, value):
                return value * 2
        """;

        String afterPythonCode = """
        class Calculator:
            def compute_total(self, base):
                multiplied = base * 2
                return multiplied + 10
        
        class MathHelper:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "multiply_by_two", "MathHelper", "compute_total", "Calculator");
    }

    @Test
    void detectsMoveAndInlineMethod_StringManipulation() throws Exception {
        String beforePythonCode = """
        class TextProcessor:
            def process_message(self, text):
                utilities = StringUtils()
                trimmed = utilities.trim_spaces(text)
                return trimmed.upper()
        
        class StringUtils:
            def trim_spaces(self, text):
                return text.strip()
        """;

        String afterPythonCode = """
        class TextProcessor:
            def process_message(self, text):
                trimmed = text.strip()
                return trimmed.upper()
        
        class StringUtils:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("text_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_processor.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "trim_spaces", "StringUtils", "process_message", "TextProcessor");
    }

    @Test
    void detectsMoveAndInlineMethod_ListOperation() throws Exception {
        String beforePythonCode = """
        class DataHandler:
            def filter_positive(self, numbers):
                filter_service = FilterService()
                filtered = filter_service.get_positive_values(numbers)
                return sorted(filtered)
        
        class FilterService:
            def get_positive_values(self, values):
                return [v for v in values if v > 0]
        """;

        String afterPythonCode = """
        class DataHandler:
            def filter_positive(self, numbers):
                filtered = [v for v in numbers if v > 0]
                return sorted(filtered)
        
        class FilterService:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("data_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_handler.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "get_positive_values", "FilterService", "filter_positive", "DataHandler");
    }

    @Test
    void detectsMoveAndInlineMethod_ConditionalLogic() throws Exception {
        String beforePythonCode = """
        class UserService:
            def validate_user(self, user_data):
                validator = ValidationHelper()
                is_valid = validator.check_email_format(user_data.get('email', ''))
                return is_valid and len(user_data.get('name', '')) > 0
        
        class ValidationHelper:
            def check_email_format(self, email):
                return '@' in email and '.' in email
        """;

        String afterPythonCode = """
        class UserService:
            def validate_user(self, user_data):
                is_valid = '@' in user_data.get('email', '') and '.' in user_data.get('email', '')
                return is_valid and len(user_data.get('name', '')) > 0
        
        class ValidationHelper:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "check_email_format", "ValidationHelper", "validate_user", "UserService");
    }

    @Test
    void detectsMoveAndInlineMethod_DictionaryAccess() throws Exception {
        String beforePythonCode = """
        class ConfigManager:
            def get_setting(self, config, key):
                accessor = DataAccessor()
                value = accessor.safe_get(config, key)
                return value if value is not None else 'default'
        
        class DataAccessor:
            def safe_get(self, data, key):
                return data.get(key)
        """;

        String afterPythonCode = """
        class ConfigManager:
            def get_setting(self, config, key):
                value = config.get(key)
                return value if value is not None else 'default'
        
        class DataAccessor:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("config_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_manager.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "safe_get", "DataAccessor", "get_setting", "ConfigManager");
    }

    @Test
    void detectsMoveAndInlineMethod_LoopOperation() throws Exception {
        String beforePythonCode = """
        class ItemCounter:
            def count_valid(self, items):
                counter = CounterHelper()
                total = counter.sum_active_items(items)
                return f"Total: {total}"
        
        class CounterHelper:
            def sum_active_items(self, items):
                count = 0
                for item in items:
                    if item.get('active', False):
                        count += 1
                return count
        """;

        String afterPythonCode = """
        class ItemCounter:
            def count_valid(self, items):
                count = 0
                for item in items:
                    if item.get('active', False):
                        count += 1
                total = count
                return f"Total: {total}"
        
        class CounterHelper:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("item_counter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("item_counter.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "sum_active_items", "CounterHelper", "count_valid", "ItemCounter");
    }

    @Test
    void detectsMoveAndInlineMethod_ArithmeticExpression() throws Exception {
        String beforePythonCode = """
        class PriceCalculator:
            def calculate_final_price(self, base_price, tax_rate):
                calculator = TaxCalculator()
                tax_amount = calculator.compute_tax(base_price, tax_rate)
                return base_price + tax_amount
        
        class TaxCalculator:
            def compute_tax(self, price, rate):
                return price * rate / 100
        """;

        String afterPythonCode = """
        class PriceCalculator:
            def calculate_final_price(self, base_price, tax_rate):
                tax_amount = base_price * tax_rate / 100
                return base_price + tax_amount
        
        class TaxCalculator:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("price_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("price_calculator.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "compute_tax", "TaxCalculator", "calculate_final_price", "PriceCalculator");
    }

    @Test
    void detectsMoveAndInlineMethod_ErrorHandling() throws Exception {
        String beforePythonCode = """
        class FileReader:
            def read_content(self, filename):
                handler = ErrorHandler()
                safe_content = handler.safe_read(filename)
                return safe_content.decode('utf-8') if safe_content else ""
        
        class ErrorHandler:
            def safe_read(self, filename):
                try:
                    with open(filename, 'rb') as f:
                        return f.read()
                except FileNotFoundError:
                    return None
        """;

        String afterPythonCode = """
        class FileReader:
            def read_content(self, filename):
                try:
                    with open(filename, 'rb') as f:
                        safe_content = f.read()
                except FileNotFoundError:
                    safe_content = None
                return safe_content.decode('utf-8') if safe_content else ""
        
        class ErrorHandler:
            pass
        """;

        Map<String, String> beforeFiles = Map.of("file_reader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_reader.py", afterPythonCode);

        assertMoveAndInlineMethodRefactoringDetected(beforeFiles, afterFiles,
                "safe_read", "ErrorHandler", "read_content", "FileReader");
    }

    public static void assertMoveAndInlineMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String inlinedMethodName,
            String sourceClassName,
            String targetMethodName,
            String targetClassName
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== MOVE AND INLINE METHOD TEST: " + inlinedMethodName + " ===");
        System.out.println("Inlined method: " + inlinedMethodName + " from class " + sourceClassName);
        System.out.println("Target method: " + targetMethodName + " in class " + targetClassName);
        System.out.println("Total refactorings detected: " + refactorings.size());

        boolean moveAndInlineFound = false;

        moveAndInlineFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.MOVE_AND_INLINE_OPERATION.equals(r.getRefactoringType()));


        assertTrue(moveAndInlineFound, "Expected Move and Inline Method refactoring to be detected");
    }
}