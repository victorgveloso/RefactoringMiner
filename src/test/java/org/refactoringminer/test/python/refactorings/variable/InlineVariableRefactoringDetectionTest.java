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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class InlineVariableRefactoringDetectionTest {

    @Test
    void detectsInlineVariable_MethodCallInlining() throws Exception {
        String beforePythonCode = """
            class MathUtils:
                def process_data(self, numbers):
                    max_value = max(numbers)
                    result = max_value * 2
                    return result
            """;

        String afterPythonCode = """
            class MathUtils:
                def process_data(self, numbers):
                    result = max(numbers) * 2
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("math_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("math_utils.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "max_value", "max(numbers)", "process_data", "MathUtils");
    }

    @Test
    void detectsInlineVariable_SimpleAssignmentInlining() throws Exception {
        String beforePythonCode = """
        def calculate_area(width, height):
            area = width * height
            return area
        """;

        String afterPythonCode = """
        def calculate_area(width, height):
            return width * height
        """;

        Map<String, String> beforeFiles = Map.of("geometry.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("geometry.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "area", "width * height", "calculate_area", "");
    }

    @Test
    void detectsInlineVariable_StringConcatenationInlining() throws Exception {
        String beforePythonCode = """
        class MessageFormatter:
            def create_greeting(self, name, title):
                greeting = f"Hello, {title} {name}!"
                return greeting.upper()
        """;

        String afterPythonCode = """
        class MessageFormatter:
            def create_greeting(self, name, title):
                return f"Hello, {title} {name}!".upper()
        """;

        Map<String, String> beforeFiles = Map.of("message_formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("message_formatter.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "greeting", "f\"Hello, {title} {name}!\"", "create_greeting", "MessageFormatter");
    }

    @Test
    void detectsInlineVariable_ListComprehensionInlining() throws Exception {
        String beforePythonCode = """
        def process_numbers(numbers):
            squared_numbers = [n * n for n in numbers if n > 0]
            return sum(squared_numbers)
        """;

        String afterPythonCode = """
        def process_numbers(numbers):
            return sum([n * n for n in numbers if n > 0])
        """;

        Map<String, String> beforeFiles = Map.of("number_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("number_processor.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "squared_numbers", "[n * n for n in numbers if n > 0]", "process_numbers", "");
    }

    @Test
    void detectsInlineVariable_DictionaryAccessInlining() throws Exception {
        String beforePythonCode = """
        class UserService:
            def get_user_email(self, user_data):
                email = user_data.get('email', 'unknown@example.com')
                return email.lower()
            
            def get_user_name(self, user_data):
                return user_data.get('name', 'Unknown')
        """;

        String afterPythonCode = """
        class UserService:
            def get_user_email(self, user_data):
                return user_data.get('email', 'unknown@example.com').lower()
            
            def get_user_name(self, user_data):
                return user_data.get('name', 'Unknown')
        """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "email", "user_data.get('email', 'unknown@example.com')", "get_user_email", "UserService");
    }

    @Test
    void detectsInlineVariable_ConditionalExpressionInlining() throws Exception {
        String beforePythonCode = """
        def validate_score(score):
            is_valid = score >= 0 and score <= 100
            return "Valid" if is_valid else "Invalid"
        """;

        String afterPythonCode = """
        def validate_score(score):
            return "Valid" if score >= 0 and score <= 100 else "Invalid"
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "is_valid", "score >= 0 and score <= 100", "validate_score", "");
    }

    @Test
    void detectsInlineVariable_FunctionCallChainInlining() throws Exception {
        String beforePythonCode = """
        class TextProcessor:
            def process_text(self, text):
                cleaned_text = text.strip().lower()
                word_count = len(cleaned_text.split())
                return word_count
        """;

        String afterPythonCode = """
        class TextProcessor:
            def process_text(self, text):
                word_count = len(text.strip().lower().split())
                return word_count
        """;

        Map<String, String> beforeFiles = Map.of("text_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_processor.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "cleaned_text", "text.strip().lower()", "process_text", "TextProcessor");
    }

    @Test
    void detectsInlineVariable_ArithmeticExpressionInlining() throws Exception {
        String beforePythonCode = """
        def calculate_discount(price, discount_rate):
            discount_amount = price * discount_rate / 100
            final_price = price - discount_amount
            return final_price
        """;

        String afterPythonCode = """
        def calculate_discount(price, discount_rate):
            final_price = price - (price * discount_rate / 100)
            return final_price
        """;

        Map<String, String> beforeFiles = Map.of("pricing.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("pricing.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "discount_amount", "price * discount_rate / 100", "calculate_discount", "");
    }

    @Test
    void detectsInlineVariable_ObjectInstantiationInlining() throws Exception {
        String beforePythonCode = """
        class ReportGenerator:
            def create_report(self, data):
                report_data = {'items': data, 'count': len(data)}
                return self.format_report(report_data)
            
            def format_report(self, report_data):
                return f"Report with {report_data['count']} items"
        """;

        String afterPythonCode = """
        class ReportGenerator:
            def create_report(self, data):
                return self.format_report({'items': data, 'count': len(data)})
            
            def format_report(self, report_data):
                return f"Report with {report_data['count']} items"
        """;

        Map<String, String> beforeFiles = Map.of("report_generator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report_generator.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "report_data", "{'items': data, 'count': len(data)}", "create_report", "ReportGenerator");
    }

    @Test
    void detectsInlineVariable_SlicingOperationInlining() throws Exception {
        String beforePythonCode = """
        def get_first_three_items(items):
            first_three = items[:3]
            return ", ".join(map(str, first_three))
        """;

        String afterPythonCode = """
        def get_first_three_items(items):
            return ", ".join(map(str, items[:3]))
        """;

        Map<String, String> beforeFiles = Map.of("list_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("list_utils.py", afterPythonCode);

        assertInlineVariableRefactoringDetected(beforeFiles, afterFiles,
                "first_three", "items[:3]", "get_first_three_items", "");
    }

    public static void assertInlineVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String inlinedVariableName,
            String inlinedExpression,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== INLINE VARIABLE TEST: " + inlinedVariableName + " ===");
        System.out.println("Inlined expression: " + inlinedExpression);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        // Look for InlineVariableRefactoring
        boolean inlineVariableFound = refactorings.stream()
                .filter(r -> RefactoringType.INLINE_VARIABLE.equals(r.getRefactoringType()))
                .anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.INLINE_VARIABLE);

        assertTrue(inlineVariableFound, "Expected Inline Variable refactoring to be detected");
    }
}