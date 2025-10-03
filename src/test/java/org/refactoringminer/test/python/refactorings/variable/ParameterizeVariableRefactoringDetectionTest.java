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
public class ParameterizeVariableRefactoringDetectionTest {

    @Test
    void detectsParameterizeVariable_HardcodedToParameter() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def calculate_tax(self, amount):
                    tax_rate = 0.15
                    return amount * tax_rate
            """;

        String afterPythonCode = """
            class Calculator:
                def calculate_tax(self, amount, tax_rate=0.15):
                    return amount * tax_rate
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "tax_rate", "0.15", "calculate_tax", "Calculator");
    }

    @Test
    void detectsParameterizeVariable_MaxSizeToParameter() throws Exception {
        String beforePythonCode = """
        class ListProcessor:
            def trim_list(self, items):
                max_size = 10
                return items[:max_size]
        """;

        String afterPythonCode = """
        class ListProcessor:
            def trim_list(self, items, max_size=10):
                return items[:max_size]
        """;

        Map<String, String> beforeFiles = Map.of("list_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("list_processor.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "max_size", "10", "trim_list", "ListProcessor");
    }

    @Test
    void detectsParameterizeVariable_ThresholdToParameter() throws Exception {
        String beforePythonCode = """
        class DataFilter:
            def filter_scores(self, scores):
                threshold = 75
                return [score for score in scores if score >= threshold]
        """;

        String afterPythonCode = """
        class DataFilter:
            def filter_scores(self, scores, threshold=75):
                return [score for score in scores if score >= threshold]
        """;

        Map<String, String> beforeFiles = Map.of("data_filter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_filter.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "threshold", "75", "filter_scores", "DataFilter");
    }

    @Test
    void detectsParameterizeVariable_TimeoutToParameter() throws Exception {
        String beforePythonCode = """
        class NetworkClient:
            def wait_for_response(self):
                timeout_seconds = 30
                import time
                time.sleep(timeout_seconds)
                return "response received"
        """;

        String afterPythonCode = """
        class NetworkClient:
            def wait_for_response(self, timeout_seconds=30):
                import time
                time.sleep(timeout_seconds)
                return "response received"
        """;

        Map<String, String> beforeFiles = Map.of("network_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("network_client.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "timeout_seconds", "30", "wait_for_response", "NetworkClient");
    }

    @Test
    void detectsParameterizeVariable_MultiplierToParameter() throws Exception {
        String beforePythonCode = """
        class ScoreCalculator:
            def boost_score(self, base_score):
                multiplier = 1.5
                boosted = base_score * multiplier
                return int(boosted)
        """;

        String afterPythonCode = """
        class ScoreCalculator:
            def boost_score(self, base_score, multiplier=1.5):
                boosted = base_score * multiplier
                return int(boosted)
        """;

        Map<String, String> beforeFiles = Map.of("score_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("score_calculator.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "multiplier", "1.5", "boost_score", "ScoreCalculator");
    }

    @Test
    void detectsParameterizeVariable_PrefixToParameter() throws Exception {
        String beforePythonCode = """
        class MessageFormatter:
            def format_message(self, message):
                prefix = "INFO: "
                return prefix + message
        """;

        String afterPythonCode = """
        class MessageFormatter:
            def format_message(self, message, prefix="INFO: "):
                return prefix + message
        """;

        Map<String, String> beforeFiles = Map.of("message_formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("message_formatter.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "prefix", "\"INFO: \"", "format_message", "MessageFormatter");
    }

    @Test
    void detectsParameterizeVariable_BatchSizeToParameter() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def process_in_batches(self, items):
                batch_size = 50
                batches = []
                for i in range(0, len(items), batch_size):
                    batch = items[i:i + batch_size]
                    batches.append(batch)
                return batches
        """;

        String afterPythonCode = """
        class DataProcessor:
            def process_in_batches(self, items, batch_size=50):
                batches = []
                for i in range(0, len(items), batch_size):
                    batch = items[i:i + batch_size]
                    batches.append(batch)
                return batches
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "batch_size", "50", "process_in_batches", "DataProcessor");
    }

    @Test
    void detectsParameterizeVariable_DecimalPlacesToParameter() throws Exception {
        String beforePythonCode = """
        class NumberFormatter:
            def format_decimal(self, number):
                decimal_places = 2
                format_string = f"{{:.{decimal_places}f}}"
                return format_string.format(number)
        """;

        String afterPythonCode = """
        class NumberFormatter:
            def format_decimal(self, number, decimal_places=2):
                format_string = f"{{:.{decimal_places}f}}"
                return format_string.format(number)
        """;

        Map<String, String> beforeFiles = Map.of("number_formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("number_formatter.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "decimal_places", "2", "format_decimal", "NumberFormatter");
    }

    @Test
    void detectsParameterizeVariable_RetryCountToParameter() throws Exception {
        String beforePythonCode = """
        class ApiClient:
            def call_with_retry(self, url):
                max_retries = 3
                for attempt in range(max_retries):
                    try:
                        return self.make_request(url)
                    except Exception:
                        if attempt == max_retries - 1:
                            raise
                        continue
            
            def make_request(self, url):
                return f"Response from {url}"
        """;

        String afterPythonCode = """
        class ApiClient:
            def call_with_retry(self, url, max_retries=3):
                for attempt in range(max_retries):
                    try:
                        return self.make_request(url)
                    except Exception:
                        if attempt == max_retries - 1:
                            raise
                        continue
            
            def make_request(self, url):
                return f"Response from {url}"
        """;

        Map<String, String> beforeFiles = Map.of("api_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("api_client.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "max_retries", "3", "call_with_retry", "ApiClient");
    }

    @Test
    void detectsParameterizeVariable_BufferSizeToParameter() throws Exception {
        String beforePythonCode = """
        class FileReader:
            def read_chunks(self, file_path):
                buffer_size = 1024
                chunks = []
                with open(file_path, 'rb') as f:
                    while True:
                        chunk = f.read(buffer_size)
                        if not chunk:
                            break
                        chunks.append(chunk)
                return chunks
        """;

        String afterPythonCode = """
        class FileReader:
            def read_chunks(self, file_path, buffer_size=1024):
                chunks = []
                with open(file_path, 'rb') as f:
                    while True:
                        chunk = f.read(buffer_size)
                        if not chunk:
                            break
                        chunks.append(chunk)
                return chunks
        """;

        Map<String, String> beforeFiles = Map.of("file_reader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_reader.py", afterPythonCode);

        assertParameterizeVariableRefactoringDetected(beforeFiles, afterFiles,
                "buffer_size", "1024", "read_chunks", "FileReader");
    }

    public static void assertParameterizeVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String parameterName,
            String originalValue,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== PARAMETERIZE VARIABLE TEST: " + parameterName + " ===");
        System.out.println("Original value: " + originalValue);
        System.out.println("Method: " + methodName + " in class " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());

        // Look for ParameterizeVariableRefactoring
        boolean parameterizeVariableFound = refactorings.stream()
                .filter(r -> RefactoringType.PARAMETERIZE_VARIABLE.equals(r.getRefactoringType()))
                .anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.PARAMETERIZE_VARIABLE);


        assertTrue(parameterizeVariableFound, "Expected Parameterize Variable refactoring to be detected");
    }
}