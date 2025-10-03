package org.refactoringminer.test.python.refactorings.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertPullUpMethodRefactoringDetected;

@Isolated
public class PullUpMethodRefactoringDetectionTest {

    @Test
    void detectsPullUpMethodWithMultipleSubclasses() throws Exception {
        String beforePythonCode = """
            class Animal:
                pass

            class Dog(Animal):
                def speak(self):
                    return "Woof"

            class Cat(Animal):
                pass
            """;
        String afterPythonCode = """
            class Animal:
                def speak(self):
                    return "Woof"

            class Dog(Animal):
                pass

            class Cat(Animal):
                pass
            """;

        Map<String, String> beforeFiles = Map.of("tests/pullup_multi.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/pullup_multi.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "Dog", "Animal", "speak");
    }

    @Test
    void detectsPullUpMethod_SimpleCalculation() throws Exception {
        String beforePythonCode = """
        class Calculator:
            pass

        class BasicCalculator(Calculator):
            def add_numbers(self, a, b):
                return a + b

        class AdvancedCalculator(Calculator):
            def multiply(self, x, y):
                return x * y
        """;

        String afterPythonCode = """
        class Calculator:
            def add_numbers(self, a, b):
                return a + b

        class BasicCalculator(Calculator):
            pass

        class AdvancedCalculator(Calculator):
            def multiply(self, x, y):
                return x * y
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "BasicCalculator", "Calculator", "add_numbers");
    }

    @Test
    void detectsPullUpMethod_StringFormatting() throws Exception {
        String beforePythonCode = """
        class TextHandler:
            pass

        class EmailHandler(TextHandler):
            def format_message(self, text):
                return text.strip().capitalize()

        class NotificationHandler(TextHandler):
            def send_alert(self, message):
                return f"Alert: {message}"
        """;

        String afterPythonCode = """
        class TextHandler:
            def format_message(self, text):
                return text.strip().capitalize()

        class EmailHandler(TextHandler):
            pass

        class NotificationHandler(TextHandler):
            def send_alert(self, message):
                return f"Alert: {message}"
        """;

        Map<String, String> beforeFiles = Map.of("text_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_handler.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "EmailHandler", "TextHandler", "format_message");
    }

    @Test
    void detectsPullUpMethod_ListProcessing() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            pass

        class NumberProcessor(DataProcessor):
            def filter_positive(self, numbers):
                result = []
                for num in numbers:
                    if num > 0:
                        result.append(num)
                return result

        class StringProcessor(DataProcessor):
            def uppercase_strings(self, strings):
                return [s.upper() for s in strings]
        """;

        String afterPythonCode = """
        class DataProcessor:
            def filter_positive(self, numbers):
                result = []
                for num in numbers:
                    if num > 0:
                        result.append(num)
                return result

        class NumberProcessor(DataProcessor):
            pass

        class StringProcessor(DataProcessor):
            def uppercase_strings(self, strings):
                return [s.upper() for s in strings]
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "NumberProcessor", "DataProcessor", "filter_positive");
    }

    @Test
    void detectsPullUpMethod_ValidationLogic() throws Exception {
        String beforePythonCode = """
        class Validator:
            pass

        class UserValidator(Validator):
            def is_valid_input(self, value):
                if value is None:
                    return False
                if isinstance(value, str) and len(value) == 0:
                    return False
                return True

        class ProductValidator(Validator):
            def check_price(self, price):
                return price > 0 and price < 10000
        """;

        String afterPythonCode = """
        class Validator:
            def is_valid_input(self, value):
                if value is None:
                    return False
                if isinstance(value, str) and len(value) == 0:
                    return False
                return True

        class UserValidator(Validator):
            pass

        class ProductValidator(Validator):
            def check_price(self, price):
                return price > 0 and price < 10000
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "UserValidator", "Validator", "is_valid_input");
    }

    @Test
    void detectsPullUpMethod_ConditionalLogic() throws Exception {
        String beforePythonCode = """
        class StatusManager:
            pass

        class TaskStatusManager(StatusManager):
            def get_status_message(self, status_code):
                if status_code == 1:
                    return "In Progress"
                elif status_code == 2:
                    return "Completed"
                elif status_code == 3:
                    return "Failed"
                else:
                    return "Unknown"

        class OrderStatusManager(StatusManager):
            def update_order(self, order_id):
                return f"Order {order_id} updated"
        """;

        String afterPythonCode = """
        class StatusManager:
            def get_status_message(self, status_code):
                if status_code == 1:
                    return "In Progress"
                elif status_code == 2:
                    return "Completed"
                elif status_code == 3:
                    return "Failed"
                else:
                    return "Unknown"

        class TaskStatusManager(StatusManager):
            pass

        class OrderStatusManager(StatusManager):
            def update_order(self, order_id):
                return f"Order {order_id} updated"
        """;

        Map<String, String> beforeFiles = Map.of("status_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("status_manager.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "TaskStatusManager", "StatusManager", "get_status_message");
    }

    @Test
    void detectsPullUpMethod_DictionaryOperations() throws Exception {
        String beforePythonCode = """
        class ConfigHandler:
            pass

        class DatabaseConfig(ConfigHandler):
            def merge_configs(self, config1, config2):
                merged = {}
                for key, value in config1.items():
                    merged[key] = value
                for key, value in config2.items():
                    merged[key] = value
                return merged

        class CacheConfig(ConfigHandler):
            def clear_cache(self):
                return "Cache cleared"
        """;

        String afterPythonCode = """
        class ConfigHandler:
            def merge_configs(self, config1, config2):
                merged = {}
                for key, value in config1.items():
                    merged[key] = value
                for key, value in config2.items():
                    merged[key] = value
                return merged

        class DatabaseConfig(ConfigHandler):
            pass

        class CacheConfig(ConfigHandler):
            def clear_cache(self):
                return "Cache cleared"
        """;

        Map<String, String> beforeFiles = Map.of("config_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_handler.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "DatabaseConfig", "ConfigHandler", "merge_configs");
    }

    @Test
    void detectsPullUpMethod_ErrorHandling() throws Exception {
        String beforePythonCode = """
        class RequestProcessor:
            pass

        class HttpProcessor(RequestProcessor):
            def handle_error(self, error_code, message):
                try:
                    if error_code >= 500:
                        return {"status": "server_error", "message": message}
                    elif error_code >= 400:
                        return {"status": "client_error", "message": message}
                    else:
                        return {"status": "success", "message": message}
                except Exception as e:
                    return {"status": "error", "message": str(e)}

        class ApiProcessor(RequestProcessor):
            def validate_token(self, token):
                return len(token) > 20
        """;

        String afterPythonCode = """
        class RequestProcessor:
            def handle_error(self, error_code, message):
                try:
                    if error_code >= 500:
                        return {"status": "server_error", "message": message}
                    elif error_code >= 400:
                        return {"status": "client_error", "message": message}
                    else:
                        return {"status": "success", "message": message}
                except Exception as e:
                    return {"status": "error", "message": str(e)}

        class HttpProcessor(RequestProcessor):
            pass

        class ApiProcessor(RequestProcessor):
            def validate_token(self, token):
                return len(token) > 20
        """;

        Map<String, String> beforeFiles = Map.of("request_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("request_processor.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "HttpProcessor", "RequestProcessor", "handle_error");
    }

    @Test
    void detectsPullUpMethod_SimpleLoop() throws Exception {
        String beforePythonCode = """
        class ItemManager:
            pass

        class InventoryManager(ItemManager):
            def count_items(self, items):
                total = 0
                for item in items:
                    if item.get('active', True):
                        total += item.get('quantity', 1)
                return total

        class StockManager(ItemManager):
            def reorder_items(self, items):
                return [item for item in items if item.get('stock', 0) < 10]
        """;

        String afterPythonCode = """
        class ItemManager:
            def count_items(self, items):
                total = 0
                for item in items:
                    if item.get('active', True):
                        total += item.get('quantity', 1)
                return total

        class InventoryManager(ItemManager):
            pass

        class StockManager(ItemManager):
            def reorder_items(self, items):
                return [item for item in items if item.get('stock', 0) < 10]
        """;

        Map<String, String> beforeFiles = Map.of("item_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("item_manager.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "InventoryManager", "ItemManager", "count_items");
    }

    @Test
    void detectsPullUpMethod_DataFormatting() throws Exception {
        String beforePythonCode = """
        class ReportGenerator:
            pass

        class SalesReport(ReportGenerator):
            def format_currency(self, amount):
                if amount < 0:
                    return f"-${abs(amount):.2f}"
                else:
                    return f"${amount:.2f}"

        class FinanceReport(ReportGenerator):
            def calculate_tax(self, amount, rate):
                return amount * (rate / 100)
        """;

        String afterPythonCode = """
        class ReportGenerator:
            def format_currency(self, amount):
                if amount < 0:
                    return f"-${abs(amount):.2f}"
                else:
                    return f"${amount:.2f}"

        class SalesReport(ReportGenerator):
            pass

        class FinanceReport(ReportGenerator):
            def calculate_tax(self, amount, rate):
                return amount * (rate / 100)
        """;

        Map<String, String> beforeFiles = Map.of("report_generator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report_generator.py", afterPythonCode);

        assertPullUpMethodRefactoringDetected(beforeFiles, afterFiles, "SalesReport", "ReportGenerator", "format_currency");
    }


}
