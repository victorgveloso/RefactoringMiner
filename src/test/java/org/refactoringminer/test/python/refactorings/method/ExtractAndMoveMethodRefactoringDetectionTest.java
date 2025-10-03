package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class ExtractAndMoveMethodRefactoringDetectionTest {

    @Test
    void detectsExtractAndMoveMethod_CalculateToMathUtils() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def add(self, x, y):
                    result = x + y
                    return result
                
                def multiply(self, x, y):
                    result = x * y
                    return result
            """;

        String afterCalculatorCode = """
            class Calculator:
                def add(self, x, y):
                    return MathUtils.calculate_sum(x, y)
                
                def multiply(self, x, y):
                    result = x * y
                    return result
            """;

        String afterMathUtilsCode = """
            class MathUtils:
                @staticmethod
                def calculate_sum(x, y):
                    result = x + y
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "calculator.py", afterCalculatorCode,
                "math_utils.py", afterMathUtilsCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles, "add", "calculate_sum", "Calculator", "MathUtils");
    }

    @Test
    void detectsExtractAndMoveMethod_ProcessToHelper() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                def process_data(self, data):
                    cleaned = data.strip()
                    return cleaned.upper()
            """;

        String afterProcessorCode = """
            class DataProcessor:
                def process_data(self, data):
                    return Helper.clean_and_format(data)
            """;

        String afterHelperCode = """
            class Helper:
                def clean_and_format(self, data):
                    cleaned = data.strip()
                    return cleaned.upper()
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "processor.py", afterProcessorCode,
                "helper.py", afterHelperCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles, "process_data", "clean_and_format", "DataProcessor", "Helper");
    }

    @Test
    void detectsExtractAndMoveMethod_ValidateToValidator() throws Exception {
        String beforePythonCode = """
            class User:
                def __init__(self, email):
                    self.email = email
                
                def validate_email(self):
                    return "@" in self.email
            """;

        String afterUserCode = """
            class User:
                def __init__(self, email):
                    self.email = email
                
                def validate_email(self):
                    return EmailValidator.is_valid(self.email)
            """;

        String afterValidatorCode = """
            class EmailValidator:
                @staticmethod
                def is_valid(email):
                    return "@" in email
            """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "user.py", afterUserCode,
                "validator.py", afterValidatorCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles, "validate_email", "is_valid", "User", "EmailValidator");
    }

    public static void assertExtractAndMoveMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalMethodName,
            String extractedMethodName,
            String sourceClassName,
            String targetClassName
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        // Look for Extract Method refactoring
        boolean extractFound = refactorings.stream()
                .filter(r -> r instanceof ExtractOperationRefactoring)
                .map(r -> (ExtractOperationRefactoring) r)
                .anyMatch(refactoring -> {
                    String extractedName = refactoring.getExtractedOperation().getName();
                    String extractedClass = refactoring.getExtractedOperation().getClassName();
                    return extractedName.equals(extractedMethodName) &&
                            extractedClass.equals(targetClassName);
                });

        // Look for Move Method refactoring
        boolean moveFound = refactorings.stream()
                .filter(r -> r instanceof MoveOperationRefactoring)
                .map(r -> (MoveOperationRefactoring) r)
                .anyMatch(refactoring -> {
                    String originalName = refactoring.getOriginalOperation().getName();
                    String originalClass = refactoring.getOriginalOperation().getClassName();
                    String movedName = refactoring.getMovedOperation().getName();
                    String movedClass = refactoring.getMovedOperation().getClassName();
                    return originalName.equals(originalMethodName) &&
                            originalClass.equals(sourceClassName) &&
                            movedName.equals(extractedMethodName) &&
                            movedClass.equals(targetClassName);
                });

        boolean refactoringDetected = extractFound && moveFound;
        System.out.println("Refactorings detected: " + refactorings.size());

        if (!refactoringDetected) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Extract and Move Method refactoring not detected.\n");
            errorMessage.append("Expected: Extract/Move method '").append(originalMethodName)
                    .append("' from class '").append(sourceClassName)
                    .append("' to method '").append(extractedMethodName)
                    .append("' in class '").append(targetClassName).append("'\n");

            errorMessage.append("Detected refactorings:\n");
            for (Refactoring refactoring : refactorings) {
                errorMessage.append("  - ").append(refactoring.getName()).append(": ")
                        .append(refactoring.toString()).append("\n");
            }

            fail(errorMessage.toString());
        }

        assertTrue(refactoringDetected, "Expected Extract and Move Method refactoring to be detected");
    }

    @Test
    void detectsExtractAndMoveMethod_FilterToUtils() throws Exception {
        String beforePythonCode = """
        class ListProcessor:
            def filter_positive(self, numbers):
                result = []
                for num in numbers:
                    if num > 0:
                        result.append(num)
                return result
        """;

        String afterProcessorCode = """
        class ListProcessor:
            def filter_positive(self, numbers):
                return FilterUtils.get_positive_numbers(numbers)
        """;

        String afterUtilsCode = """
        class FilterUtils:
            @staticmethod
            def get_positive_numbers(numbers):
                result = []
                for num in numbers:
                    if num > 0:
                        result.append(num)
                return result
        """;

        Map<String, String> beforeFiles = Map.of("list_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "list_processor.py", afterProcessorCode,
                "filter_utils.py", afterUtilsCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "filter_positive", "get_positive_numbers", "ListProcessor", "FilterUtils");
    }

    @Test
    void detectsExtractAndMoveMethod_FormatToFormatter() throws Exception {
        String beforePythonCode = """
        class TextHandler:
            def format_text(self, text, width):
                lines = []
                words = text.split()
                current_line = ""
                for word in words:
                    if len(current_line + word) <= width:
                        current_line += word + " "
                    else:
                        lines.append(current_line.strip())
                        current_line = word + " "
                if current_line:
                    lines.append(current_line.strip())
                return "\\n".join(lines)
        """;

        String afterHandlerCode = """
        class TextHandler:
            def format_text(self, text, width):
                return TextFormatter.wrap_text(text, width)
        """;

        String afterFormatterCode = """
        class TextFormatter:
            @staticmethod
            def wrap_text(text, width):
                lines = []
                words = text.split()
                current_line = ""
                for word in words:
                    if len(current_line + word) <= width:
                        current_line += word + " "
                    else:
                        lines.append(current_line.strip())
                        current_line = word + " "
                if current_line:
                    lines.append(current_line.strip())
                return "\\n".join(lines)
        """;

        Map<String, String> beforeFiles = Map.of("text_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "text_handler.py", afterHandlerCode,
                "text_formatter.py", afterFormatterCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "format_text", "wrap_text", "TextHandler", "TextFormatter");
    }

    @Test
    void detectsExtractAndMoveMethod_CalculateToStatistics() throws Exception {
        String beforePythonCode = """
        class DataAnalyzer:
            def calculate_average(self, values):
                if not values:
                    return 0
                total = sum(values)
                count = len(values)
                return total / count
        """;

        String afterAnalyzerCode = """
        class DataAnalyzer:
            def calculate_average(self, values):
                return Statistics.compute_mean(values)
        """;

        String afterStatisticsCode = """
        class Statistics:
            @staticmethod
            def compute_mean(values):
                if not values:
                    return 0
                total = sum(values)
                count = len(values)
                return total / count
        """;

        Map<String, String> beforeFiles = Map.of("data_analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "data_analyzer.py", afterAnalyzerCode,
                "statistics.py", afterStatisticsCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "calculate_average", "compute_mean", "DataAnalyzer", "Statistics");
    }

    @Test
    void detectsExtractAndMoveMethod_SearchToSearchEngine() throws Exception {
        String beforePythonCode = """
        class DocumentManager:
            def search_documents(self, documents, query):
                matches = []
                query_lower = query.lower()
                for doc in documents:
                    content = doc.get('content', '').lower()
                    if query_lower in content:
                        matches.append(doc)
                return matches
        """;

        String afterManagerCode = """
        class DocumentManager:
            def search_documents(self, documents, query):
                return SearchEngine.find_matching_docs(documents, query)
        """;

        String afterSearchEngineCode = """
        class SearchEngine:
            @staticmethod
            def find_matching_docs(documents, query):
                matches = []
                query_lower = query.lower()
                for doc in documents:
                    content = doc.get('content', '').lower()
                    if query_lower in content:
                        matches.append(doc)
                return matches
        """;

        Map<String, String> beforeFiles = Map.of("document_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "document_manager.py", afterManagerCode,
                "search_engine.py", afterSearchEngineCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "search_documents", "find_matching_docs", "DocumentManager", "SearchEngine");
    }

    @Test
    void detectsExtractAndMoveMethod_SortToSorter() throws Exception {
        String beforePythonCode = """
        class ItemProcessor:
            def sort_by_score(self, items):
                return sorted(items, key=lambda x: x.get('score', 0), reverse=True)
            
            def process_items(self, items):
                return [item for item in items if item.get('active', False)]
        """;

        String afterProcessorCode = """
        class ItemProcessor:
            def sort_by_score(self, items):
                return ItemSorter.sort_descending_by_score(items)
            
            def process_items(self, items):
                return [item for item in items if item.get('active', False)]
        """;

        String afterSorterCode = """
        class ItemSorter:
            @staticmethod
            def sort_descending_by_score(items):
                return sorted(items, key=lambda x: x.get('score', 0), reverse=True)
        """;

        Map<String, String> beforeFiles = Map.of("item_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "item_processor.py", afterProcessorCode,
                "item_sorter.py", afterSorterCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "sort_by_score", "sort_descending_by_score", "ItemProcessor", "ItemSorter");
    }

    @Test
    void detectsExtractAndMoveMethod_TransformToConverter() throws Exception {
        String beforePythonCode = """
        class DataHandler:
            def transform_data(self, data):
                result = {}
                for key, value in data.items():
                    if isinstance(value, str):
                        result[key.upper()] = value.lower()
                    elif isinstance(value, (int, float)):
                        result[key.upper()] = value * 2
                    else:
                        result[key.upper()] = str(value)
                return result
        """;

        String afterHandlerCode = """
        class DataHandler:
            def transform_data(self, data):
                return DataConverter.normalize_data(data)
        """;

        String afterConverterCode = """
        class DataConverter:
            @staticmethod
            def normalize_data(data):
                result = {}
                for key, value in data.items():
                    if isinstance(value, str):
                        result[key.upper()] = value.lower()
                    elif isinstance(value, (int, float)):
                        result[key.upper()] = value * 2
                    else:
                        result[key.upper()] = str(value)
                return result
        """;

        Map<String, String> beforeFiles = Map.of("data_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "data_handler.py", afterHandlerCode,
                "data_converter.py", afterConverterCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "transform_data", "normalize_data", "DataHandler", "DataConverter");
    }

    @Test
    void detectsExtractAndMoveMethod_CountToCounter() throws Exception {
        String beforePythonCode = """
        class ListAnalyzer:
            def count_occurrences(self, items, target):
                count = 0
                for item in items:
                    if item == target:
                        count += 1
                return count
        """;

        String afterAnalyzerCode = """
        class ListAnalyzer:
            def count_occurrences(self, items, target):
                return ItemCounter.count_matches(items, target)
        """;

        String afterCounterCode = """
        class ItemCounter:
            @staticmethod
            def count_matches(items, target):
                count = 0
                for item in items:
                    if item == target:
                        count += 1
                return count
        """;

        Map<String, String> beforeFiles = Map.of("list_analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "list_analyzer.py", afterAnalyzerCode,
                "item_counter.py", afterCounterCode
        );

        assertExtractAndMoveMethodRefactoringDetected(beforeFiles, afterFiles,
                "count_occurrences", "count_matches", "ListAnalyzer", "ItemCounter");
    }

}