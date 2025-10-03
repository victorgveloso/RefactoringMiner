package org.refactoringminer.test.python.refactorings.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertPushDownMethodRefactoringDetected;

@Isolated
public class PushDownRefactoringDetectionTest {

    @Test
    void detectsPushDownMethod() throws Exception {
        String beforePythonCode = """
            class Parent:
                def foo(self):
                    return "bar"

            class Child(Parent):
                pass
            """;
        String afterPythonCode = """
            class Parent:
                pass

            class Child(Parent):
                def foo(self):
                    return "bar"
            """;

        Map<String, String> beforeFiles = Map.of("tests/parent.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/parent.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "Parent", "Child", "foo");
    }

    @Test
    void detectsPushDownMethodWithMultipleSubclasses() throws Exception {
        String beforePythonCode = """
            class Vehicle:
                def horn(self):
                    return "Beep"

            class Car(Vehicle):
                pass

            class Bicycle(Vehicle):
                pass
            """;
        String afterPythonCode = """
            class Vehicle:
                pass

            class Car(Vehicle):
                def horn(self):
                    return "Beep"

            class Bicycle(Vehicle):
                pass
            """;

        Map<String, String> beforeFiles = Map.of("tests/vehicle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/vehicle.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "Vehicle", "Car", "horn");
    }

    @Test
    void detectsPushDownComplexMethodsToMultipleSubclasses() throws Exception {
        String beforePythonCode = """
        class NotificationSender:
            def send_email(self, recipient, message):
                if not recipient:
                    raise ValueError('No recipient!')
                log_message = f"Sending email to {recipient}: {message}"
                self.log(log_message)
                return "Email sent"

            def send_sms(self, number, message):
                if not number.isdigit():
                    raise ValueError('Invalid number')
                print(f"SMS to {number}: {message}")
                return "SMS sent"
                
            def log(self, entry):
                # Imagine this writes to a file
                pass

        class EmailSender(NotificationSender):
            def log(self, entry):
                # Email-specific logging
                print("Email log:", entry)

        class SMSSender(NotificationSender):
            def log(self, entry):
                # SMS-specific logging
                print("SMS log:", entry)
        """;
        String afterPythonCode = """
        class NotificationSender:
            def log(self, entry):
                # Imagine this writes to a file
                pass

        class EmailSender(NotificationSender):
            def send_email(self, recipient, message):
                if not recipient:
                    raise ValueError('No recipient!')
                log_message = f"Sending email to {recipient}: {message}"
                self.log(log_message)
                return "Email sent"

            def log(self, entry):
                # Email-specific logging
                print("Email log:", entry)

        class SMSSender(NotificationSender):
            def send_sms(self, number, message):
                if not number.isdigit():
                    raise ValueError('Invalid number')
                print(f"SMS to {number}: {message}")
                return "SMS sent"

            def log(self, entry):
                # SMS-specific logging
                print("SMS log:", entry)
        """;

        Map<String, String> beforeFiles = Map.of("tests/notification.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/notification.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "NotificationSender", "EmailSender", "send_email");
        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "NotificationSender", "SMSSender", "send_sms");
    }

    @Test
    void detectsPushDownMethod_SimpleCalculation() throws Exception {
        String beforePythonCode = """
        class Calculator:
            def calculate_tax(self, amount, rate):
                return amount * rate

        class SalesCalculator(Calculator):
            def calculate_discount(self, amount, percent):
                return amount * (percent / 100)

        class TaxCalculator(Calculator):
            def get_tax_info(self):
                return "Tax information"
        """;

        String afterPythonCode = """
        class Calculator:
            pass

        class SalesCalculator(Calculator):
            def calculate_tax(self, amount, rate):
                return amount * rate

            def calculate_discount(self, amount, percent):
                return amount * (percent / 100)

        class TaxCalculator(Calculator):
            def get_tax_info(self):
                return "Tax information"
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "Calculator", "SalesCalculator", "calculate_tax");
    }

    @Test
    void detectsPushDownMethod_StringProcessing() throws Exception {
        String beforePythonCode = """
        class TextProcessor:
            def format_text(self, text):
                return text.strip().lower()

        class EmailProcessor(TextProcessor):
            def validate_email(self, email):
                return "@" in email

        class PasswordProcessor(TextProcessor):
            def check_strength(self, password):
                return len(password) >= 8
        """;

        String afterPythonCode = """
        class TextProcessor:
            pass

        class EmailProcessor(TextProcessor):
            def format_text(self, text):
                return text.strip().lower()

            def validate_email(self, email):
                return "@" in email

        class PasswordProcessor(TextProcessor):
            def check_strength(self, password):
                return len(password) >= 8
        """;

        Map<String, String> beforeFiles = Map.of("text_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_processor.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "TextProcessor", "EmailProcessor", "format_text");
    }

    @Test
    void detectsPushDownMethod_ListOperations() throws Exception {
        String beforePythonCode = """
        class ListManager:
            def sort_items(self, items):
                sorted_items = []
                for item in items:
                    if isinstance(item, str):
                        sorted_items.append(item.upper())
                    else:
                        sorted_items.append(item)
                return sorted(sorted_items)

        class NumberListManager(ListManager):
            def sum_numbers(self, numbers):
                return sum(numbers)

        class StringListManager(ListManager):
            def join_strings(self, strings):
                return " ".join(strings)
        """;

        String afterPythonCode = """
        class ListManager:
            pass

        class NumberListManager(ListManager):
            def sort_items(self, items):
                sorted_items = []
                for item in items:
                    if isinstance(item, str):
                        sorted_items.append(item.upper())
                    else:
                        sorted_items.append(item)
                return sorted(sorted_items)

            def sum_numbers(self, numbers):
                return sum(numbers)

        class StringListManager(ListManager):
            def join_strings(self, strings):
                return " ".join(strings)
        """;

        Map<String, String> beforeFiles = Map.of("list_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("list_manager.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "ListManager", "NumberListManager", "sort_items");
    }

    @Test
    void detectsPushDownMethod_DictionaryOperations() throws Exception {
        String beforePythonCode = """
        class DataManager:
            def merge_data(self, data1, data2):
                result = {}
                for key in data1:
                    result[key] = data1[key]
                for key in data2:
                    if key in result:
                        result[key] = result[key] + data2[key]
                    else:
                        result[key] = data2[key]
                return result

        class ConfigManager(DataManager):
            def load_config(self, filename):
                return f"Loading config from {filename}"

        class SettingsManager(DataManager):
            def save_settings(self, settings):
                return f"Saving {len(settings)} settings"
        """;

        String afterPythonCode = """
        class DataManager:
            pass

        class ConfigManager(DataManager):
            def merge_data(self, data1, data2):
                result = {}
                for key in data1:
                    result[key] = data1[key]
                for key in data2:
                    if key in result:
                        result[key] = result[key] + data2[key]
                    else:
                        result[key] = data2[key]
                return result

            def load_config(self, filename):
                return f"Loading config from {filename}"

        class SettingsManager(DataManager):
            def save_settings(self, settings):
                return f"Saving {len(settings)} settings"
        """;

        Map<String, String> beforeFiles = Map.of("data_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_manager.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "DataManager", "ConfigManager", "merge_data");
    }

    @Test
    void detectsPushDownMethod_ConditionalLogic() throws Exception {
        String beforePythonCode = """
        class StatusChecker:
            def check_status(self, value):
                if value > 100:
                    return "high"
                elif value > 50:
                    return "medium"
                elif value > 0:
                    return "low"
                else:
                    return "zero"

        class PerformanceChecker(StatusChecker):
            def analyze_performance(self, metrics):
                return "Analyzing performance metrics"

        class QualityChecker(StatusChecker):
            def verify_quality(self, standards):
                return "Verifying quality standards"
        """;

        String afterPythonCode = """
        class StatusChecker:
            pass

        class PerformanceChecker(StatusChecker):
            def check_status(self, value):
                if value > 100:
                    return "high"
                elif value > 50:
                    return "medium"
                elif value > 0:
                    return "low"
                else:
                    return "zero"

            def analyze_performance(self, metrics):
                return "Analyzing performance metrics"

        class QualityChecker(StatusChecker):
            def verify_quality(self, standards):
                return "Verifying quality standards"
        """;

        Map<String, String> beforeFiles = Map.of("status_checker.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("status_checker.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "StatusChecker", "PerformanceChecker", "check_status");
    }

    @Test
    void detectsPushDownMethod_SimpleLoop() throws Exception {
        String beforePythonCode = """
        class ItemProcessor:
            def process_items(self, items):
                results = []
                for item in items:
                    processed = item * 2 if isinstance(item, (int, float)) else str(item).upper()
                    results.append(processed)
                return results

        class NumberProcessor(ItemProcessor):
            def multiply_numbers(self, numbers, factor):
                return [n * factor for n in numbers]

        class TextProcessor(ItemProcessor):
            def capitalize_text(self, texts):
                return [text.capitalize() for text in texts]
        """;

        String afterPythonCode = """
        class ItemProcessor:
            pass

        class NumberProcessor(ItemProcessor):
            def process_items(self, items):
                results = []
                for item in items:
                    processed = item * 2 if isinstance(item, (int, float)) else str(item).upper()
                    results.append(processed)
                return results

            def multiply_numbers(self, numbers, factor):
                return [n * factor for n in numbers]

        class TextProcessor(ItemProcessor):
            def capitalize_text(self, texts):
                return [text.capitalize() for text in texts]
        """;

        Map<String, String> beforeFiles = Map.of("item_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("item_processor.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "ItemProcessor", "NumberProcessor", "process_items");
    }

    @Test
    void detectsPushDownMethod_ErrorHandling() throws Exception {
        String beforePythonCode = """
        class RequestHandler:
            def handle_request(self, request_data):
                try:
                    if not request_data:
                        raise ValueError("Empty request")
                    
                    response = {
                        'status': 'success',
                        'data': request_data.get('payload', {}),
                        'timestamp': 'now'
                    }
                    return response
                except ValueError as e:
                    return {'status': 'error', 'message': str(e)}
                except Exception as e:
                    return {'status': 'error', 'message': 'Unexpected error'}

        class HttpRequestHandler(RequestHandler):
            def parse_headers(self, headers):
                return dict(headers)

        class ApiRequestHandler(RequestHandler):
            def validate_token(self, token):
                return len(token) > 10
        """;

        String afterPythonCode = """
        class RequestHandler:
            pass

        class HttpRequestHandler(RequestHandler):
            def handle_request(self, request_data):
                try:
                    if not request_data:
                        raise ValueError("Empty request")
                    
                    response = {
                        'status': 'success',
                        'data': request_data.get('payload', {}),
                        'timestamp': 'now'
                    }
                    return response
                except ValueError as e:
                    return {'status': 'error', 'message': str(e)}
                except Exception as e:
                    return {'status': 'error', 'message': 'Unexpected error'}

            def parse_headers(self, headers):
                return dict(headers)

        class ApiRequestHandler(RequestHandler):
            def validate_token(self, token):
                return len(token) > 10
        """;

        Map<String, String> beforeFiles = Map.of("request_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("request_handler.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "RequestHandler", "HttpRequestHandler", "handle_request");
    }

    @Test
    void detectsPushDownMethod_DataValidation() throws Exception {
        String beforePythonCode = """
        class Validator:
            def validate_data(self, data):
                if not isinstance(data, dict):
                    return False, "Data must be a dictionary"
                
                required_fields = ['name', 'type', 'value']
                for field in required_fields:
                    if field not in data:
                        return False, f"Missing required field: {field}"
                
                if not isinstance(data['value'], (int, float)):
                    return False, "Value must be a number"
                
                return True, "Valid data"

        class UserValidator(Validator):
            def validate_email(self, email):
                return "@" in email and "." in email

        class ProductValidator(Validator):
            def validate_price(self, price):
                return price > 0
        """;

        String afterPythonCode = """
        class Validator:
            pass

        class UserValidator(Validator):
            def validate_data(self, data):
                if not isinstance(data, dict):
                    return False, "Data must be a dictionary"
                
                required_fields = ['name', 'type', 'value']
                for field in required_fields:
                    if field not in data:
                        return False, f"Missing required field: {field}"
                
                if not isinstance(data['value'], (int, float)):
                    return False, "Value must be a number"
                
                return True, "Valid data"

            def validate_email(self, email):
                return "@" in email and "." in email

        class ProductValidator(Validator):
            def validate_price(self, price):
                return price > 0
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertPushDownMethodRefactoringDetected(beforeFiles, afterFiles, "Validator", "UserValidator", "validate_data");
    }



}