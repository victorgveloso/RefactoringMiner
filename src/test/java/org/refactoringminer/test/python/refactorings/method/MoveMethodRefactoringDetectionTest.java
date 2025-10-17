package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.utils.RefactoringAssertUtils.dumpOperation;

class MoveMethodRefactoringDetectionTest {

    @Test
    void detectsMethodMove_CalculateFormula_FromCalculatorToMathHelper() throws Exception {
        // BEFORE: Method is in the Calculator class
        String beforePythonCode1 = """
            class Calculator:
                def add(self, x, y):
                    return x + y
                    
                def calculate_formula(self, a, b, c):
                    result = a * b + c
                    return result
            """;

        String beforePythonCode2 = """
            class MathHelper:
                def square(self, x):
                    return x * x
            """;

        // AFTER: Method is moved to MathHelper class
        String afterPythonCode1 = """
            class Calculator:
                def add(self, x, y):
                    return x + y
            """;

        String afterPythonCode2 = """
            class MathHelper:
                def square(self, x):
                    return x * x
                    
                def calculate_formula(self, a, b, c):
                    result = a * b + c
                    return result
            """;

        Map<String, String> beforeFiles = Map.of(
                "tests/calculator.py", beforePythonCode1,
                "tests/math_helper.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "tests/calculator.py", afterPythonCode1,
                "tests/math_helper.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles,
                afterFiles,
                "Calculator",
                "MathHelper",
                "calculate_formula"
        );
    }

    @Test
    void detectsMethodMove_ProcessData_FromProcessorToHandler() throws Exception {
        // BEFORE: Method is in the DataProcessor class
        String beforePythonCode1 = """
            class DataProcessor:
                def sanitize(self, data):
                    return data.strip()
                    
                def process_data(self, data_list):
                    results = []
                    for item in data_list:
                        processed = item.upper()
                        results.append(processed)
                    return results
            """;

        String beforePythonCode2 = """
            class DataHandler:
                def validate(self, data):
                    return len(data) > 0
            """;

        // AFTER: Method is moved to DataHandler class
        String afterPythonCode1 = """
            class DataProcessor:
                def sanitize(self, data):
                    return data.strip()
            """;

        String afterPythonCode2 = """
            class DataHandler:
                def validate(self, data):
                    return len(data) > 0
                    
                def process_data(self, data_list):
                    results = []
                    for item in data_list:
                        processed = item.upper()
                        results.append(processed)
                    return results
            """;

        Map<String, String> beforeFiles = Map.of(
                "tests/processor.py", beforePythonCode1,
                "tests/handler.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "tests/processor.py", afterPythonCode1,
                "tests/handler.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles,
                afterFiles,
                "DataProcessor",
                "DataHandler",
                "process_data"
        );
    }

    @Test
    void detectsMethodMove_ValidateInput_FromValidatorToChecker() throws Exception {
        String beforePythonCode1 = """
        class InputValidator:
            def check_format(self, text):
                return text.isalnum()
                
            def validate_input(self, data):
                if not data:
                    return False
                for item in data:
                    if len(item) == 0:
                        return False
                return True
        """;

        String beforePythonCode2 = """
        class DataChecker:
            def verify_structure(self, obj):
                return isinstance(obj, dict)
        """;

        String afterPythonCode1 = """
        class InputValidator:
            def check_format(self, text):
                return text.isalnum()
        """;

        String afterPythonCode2 = """
        class DataChecker:
            def verify_structure(self, obj):
                return isinstance(obj, dict)
                
            def validate_input(self, data):
                if not data:
                    return False
                for item in data:
                    if len(item) == 0:
                        return False
                return True
        """;

        Map<String, String> beforeFiles = Map.of(
                "validator.py", beforePythonCode1,
                "checker.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "validator.py", afterPythonCode1,
                "checker.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "InputValidator", "DataChecker", "validate_input"
        );
    }

    @Test
    void detectsMethodMove_CountItems_FromManagerToCounter() throws Exception {
        String beforePythonCode1 = """
        class ItemManager:
            def add_item(self, item):
                return f"Added {item}"
                
            def count_items(self, items):
                total = 0
                index = 0
                while index < len(items):
                    if items[index].get('active', True):
                        total += 1
                    index += 1
                return total
        """;

        String beforePythonCode2 = """
        class ItemCounter:
            def reset_count(self):
                return 0
        """;

        String afterPythonCode1 = """
        class ItemManager:
            def add_item(self, item):
                return f"Added {item}"
        """;

        String afterPythonCode2 = """
        class ItemCounter:
            def reset_count(self):
                return 0
                
            def count_items(self, items):
                total = 0
                index = 0
                while index < len(items):
                    if items[index].get('active', True):
                        total += 1
                    index += 1
                return total
        """;

        Map<String, String> beforeFiles = Map.of(
                "manager.py", beforePythonCode1,
                "counter.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "manager.py", afterPythonCode1,
                "counter.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "ItemManager", "ItemCounter", "count_items"
        );
    }

    @Test
    void detectsMethodMove_ParseConfig_FromLoaderToParser() throws Exception {
        String beforePythonCode1 = """
        class ConfigLoader:
            def load_file(self, filename):
                return f"Loading {filename}"
                
            def parse_config(self, config_text):
                settings = {}
                lines = config_text.split('\\n')
                for line in lines:
                    if '=' in line:
                        key, value = line.split('=', 1)
                        settings[key.strip()] = value.strip()
                return settings
        """;

        String beforePythonCode2 = """
        class ConfigParser:
            def validate_syntax(self, text):
                return len(text) > 0
        """;

        String afterPythonCode1 = """
        class ConfigLoader:
            def load_file(self, filename):
                return f"Loading {filename}"
        """;

        String afterPythonCode2 = """
        class ConfigParser:
            def validate_syntax(self, text):
                return len(text) > 0
                
            def parse_config(self, config_text):
                settings = {}
                lines = config_text.split('\\n')
                for line in lines:
                    if '=' in line:
                        key, value = line.split('=', 1)
                        settings[key.strip()] = value.strip()
                return settings
        """;

        Map<String, String> beforeFiles = Map.of(
                "loader.py", beforePythonCode1,
                "parser.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "loader.py", afterPythonCode1,
                "parser.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "ConfigLoader", "ConfigParser", "parse_config"
        );
    }

    @Test
    void detectsMethodMove_FilterData_FromProcessorToFilter() throws Exception {
        String beforePythonCode1 = """
        class DataProcessor:
            def transform(self, data):
                return data.upper()
                
            def filter_data(self, data_list, criteria):
                results = []
                for item in data_list:
                    if criteria == 'positive' and item > 0:
                        results.append(item)
                    elif criteria == 'negative' and item < 0:
                        results.append(item)
                    elif criteria == 'zero' and item == 0:
                        results.append(item)
                return results
        """;

        String beforePythonCode2 = """
        class DataFilter:
            def sort_results(self, items):
                return sorted(items)
        """;

        String afterPythonCode1 = """
        class DataProcessor:
            def transform(self, data):
                return data.upper()
        """;

        String afterPythonCode2 = """
        class DataFilter:
            def sort_results(self, items):
                return sorted(items)
                
            def filter_data(self, data_list, criteria):
                results = []
                for item in data_list:
                    if criteria == 'positive' and item > 0:
                        results.append(item)
                    elif criteria == 'negative' and item < 0:
                        results.append(item)
                    elif criteria == 'zero' and item == 0:
                        results.append(item)
                return results
        """;

        Map<String, String> beforeFiles = Map.of(
                "processor.py", beforePythonCode1,
                "filter.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "processor.py", afterPythonCode1,
                "filter.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "DataProcessor", "DataFilter", "filter_data"
        );
    }

    @Test
    void detectsMethodMove_FormatOutput_FromGeneratorToFormatter() throws Exception {
        String beforePythonCode1 = """
        class ReportGenerator:
            def create_report(self, data):
                return f"Report: {data}"
                
            def format_output(self, values):
                formatted = []
                index = 0
                while index < len(values):
                    value = values[index]
                    if isinstance(value, float):
                        formatted.append(f"{value:.2f}")
                    else:
                        formatted.append(str(value))
                    index += 1
                return ", ".join(formatted)
        """;

        String beforePythonCode2 = """
        class OutputFormatter:
            def align_text(self, text):
                return text.center(50)
        """;

        String afterPythonCode1 = """
        class ReportGenerator:
            def create_report(self, data):
                return f"Report: {data}"
        """;

        String afterPythonCode2 = """
        class OutputFormatter:
            def align_text(self, text):
                return text.center(50)
                
            def format_output(self, values):
                formatted = []
                index = 0
                while index < len(values):
                    value = values[index]
                    if isinstance(value, float):
                        formatted.append(f"{value:.2f}")
                    else:
                        formatted.append(str(value))
                    index += 1
                return ", ".join(formatted)
        """;

        Map<String, String> beforeFiles = Map.of(
                "generator.py", beforePythonCode1,
                "formatter.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "generator.py", afterPythonCode1,
                "formatter.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "ReportGenerator", "OutputFormatter", "format_output"
        );
    }

    @Test
    void detectsMethodMove_CalculateScore_FromAnalyzerToCalculator() throws Exception {
        String beforePythonCode1 = """
        class DataAnalyzer:
            def analyze_trends(self, data):
                return "Trending up"
                
            def calculate_score(self, scores):
                total = 0
                count = 0
                for score in scores:
                    if score >= 0 and score <= 100:
                        total += score
                        count += 1
                return total / count if count > 0 else 0
        """;

        String beforePythonCode2 = """
        class ScoreCalculator:
            def normalize_score(self, score):
                return min(max(score, 0), 100)
        """;

        String afterPythonCode1 = """
        class DataAnalyzer:
            def analyze_trends(self, data):
                return "Trending up"
        """;

        String afterPythonCode2 = """
        class ScoreCalculator:
            def normalize_score(self, score):
                return min(max(score, 0), 100)
                
            def calculate_score(self, scores):
                total = 0
                count = 0
                for score in scores:
                    if score >= 0 and score <= 100:
                        total += score
                        count += 1
                return total / count if count > 0 else 0
        """;

        Map<String, String> beforeFiles = Map.of(
                "analyzer.py", beforePythonCode1,
                "calculator.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "analyzer.py", afterPythonCode1,
                "calculator.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "DataAnalyzer", "ScoreCalculator", "calculate_score"
        );
    }

    @Test
    void detectsMethodMove_SearchItems_FromManagerToSearcher() throws Exception {
        String beforePythonCode1 = """
        class ItemManager:
            def update_item(self, item_id, data):
                return f"Updated item {item_id}"
                
            def search_items(self, items, query):
                matches = []
                for item in items:
                    name = item.get('name', '').lower()
                    if query.lower() in name:
                        matches.append(item)
                return matches
        """;

        String beforePythonCode2 = """
        class ItemSearcher:
            def build_index(self, items):
                return len(items)
        """;

        String afterPythonCode1 = """
        class ItemManager:
            def update_item(self, item_id, data):
                return f"Updated item {item_id}"
        """;

        String afterPythonCode2 = """
        class ItemSearcher:
            def build_index(self, items):
                return len(items)
                
            def search_items(self, items, query):
                matches = []
                for item in items:
                    name = item.get('name', '').lower()
                    if query.lower() in name:
                        matches.append(item)
                return matches
        """;

        Map<String, String> beforeFiles = Map.of(
                "manager.py", beforePythonCode1,
                "searcher.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "manager.py", afterPythonCode1,
                "searcher.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "ItemManager", "ItemSearcher", "search_items"
        );
    }

    @Test
    void detectsMethodMove_ProcessBatch_FromHandlerToProcessor() throws Exception {
        String beforePythonCode1 = """
        class RequestHandler:
            def handle_single(self, request):
                return f"Handled {request}"
                
            def process_batch(self, requests):
                results = []
                current = 0
                while current < len(requests):
                    request = requests[current]
                    if request.get('priority', 0) > 5:
                        results.append(f"High priority: {request['id']}")
                    else:
                        results.append(f"Normal: {request['id']}")
                    current += 1
                return results
        """;

        String beforePythonCode2 = """
        class BatchProcessor:
            def initialize_batch(self):
                return []
        """;

        String afterPythonCode1 = """
        class RequestHandler:
            def handle_single(self, request):
                return f"Handled {request}"
        """;

        String afterPythonCode2 = """
        class BatchProcessor:
            def initialize_batch(self):
                return []
                
            def process_batch(self, requests):
                results = []
                current = 0
                while current < len(requests):
                    request = requests[current]
                    if request.get('priority', 0) > 5:
                        results.append(f"High priority: {request['id']}")
                    else:
                        results.append(f"Normal: {request['id']}")
                    current += 1
                return results
        """;

        Map<String, String> beforeFiles = Map.of(
                "handler.py", beforePythonCode1,
                "processor.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "handler.py", afterPythonCode1,
                "processor.py", afterPythonCode2
        );

        assertMoveOperationRefactoringDetected(
                beforeFiles, afterFiles,
                "RequestHandler", "BatchProcessor", "process_batch"
        );
    }

    private void assertMoveOperationRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName,
            String methodName) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        System.out.println("=== BEFORE MODEL OPERATIONS ===");
        beforeUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        });

        System.out.println("=== AFTER MODEL OPERATIONS ===");
        afterUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        });

        UMLModelDiff diff = beforeUML.diff(afterUML);
        boolean moveMethodDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof MoveOperationRefactoring moveRef) {
                        VariableDeclarationContainer originalOperation = moveRef.getOriginalOperation();
                        VariableDeclarationContainer movedOperation = moveRef.getMovedOperation();
                        UMLAbstractClass originalClass = diff.findClassInParentModel(originalOperation.getClassName());
                        UMLAbstractClass nextClass = diff.findClassInChildModel(movedOperation.getClassName());

                        return originalOperation.getName().equals(methodName) &&
                                originalOperation.getClassName().equals(originalClass.getPackageName() + "." + sourceClassName) &&
                                movedOperation.getName().equals(methodName) &&
                                movedOperation.getClassName().equals(nextClass.getPackageName() + "." + targetClassName);
                    }
                    return false;
                });

        System.out.println("==== DIFF ====");
        System.out.println("Move method detected: " + moveMethodDetected);
        System.out.println("Total refactorings: " + diff.getRefactorings().size());

        diff.getRefactorings().forEach(System.out::println);
        System.out.println("\n");

        assertTrue(moveMethodDetected,
                String.format("Expected a MoveOperationRefactoring of method '%s' from class '%s' to class '%s'",
                        methodName, sourceClassName, targetClassName));
    }
}