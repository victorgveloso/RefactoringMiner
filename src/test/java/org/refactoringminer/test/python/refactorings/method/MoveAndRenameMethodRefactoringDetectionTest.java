package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
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
public class MoveAndRenameMethodRefactoringDetectionTest {

    @Test
    void detectsMoveAndRenameMethod_CalculateToMathUtilsCompute() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def calculate(self, x, y):
                    return x + y
                
                def multiply(self, x, y):
                    return x * y
            """;

        String afterCalculatorCode = """
            class Calculator:
                def multiply(self, x, y):
                    return x * y
            """;

        String afterMathUtilsCode = """
            class MathUtils:
                def compute(self, x, y):
                    return x + y
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "calculator.py", afterCalculatorCode,
                "math_utils.py", afterMathUtilsCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "calculate", "compute", "Calculator", "MathUtils");
    }

    @Test
    void detectsMoveAndRenameMethod_ProcessToHandlerExecute() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                def process(self, data):
                    return data.strip().upper()
                
                def validate(self, data):
                    return len(data) > 0
            """;

        String afterProcessorCode = """
            class DataProcessor:
                def validate(self, data):
                    return len(data) > 0
            """;

        String afterHandlerCode = """
            class DataHandler:
                def execute(self, data):
                    return data.strip().upper()
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "processor.py", afterProcessorCode,
                "handler.py", afterHandlerCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "process", "execute", "DataProcessor", "DataHandler");
    }

    @Test
    void detectsMoveAndRenameMethod_SaveToRepositoryStore() throws Exception {
        String beforePythonCode = """
            class UserService:
                def save(self, user_data):
                    return user_data
                
                def find(self, user_id):
                    return user_id
            """;

        String afterServiceCode = """
            class UserService:
                def find(self, user_id):
                    return user_id
            """;

        String afterRepositoryCode = """
            class UserRepository:
                def store(self, user_data):
                    return user_data
            """;

        Map<String, String> beforeFiles = Map.of("service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "service.py", afterServiceCode,
                "repository.py", afterRepositoryCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "save", "store", "UserService", "UserRepository");
    }

    @Test
    void detectsMoveAndRenameMethod_ValidateToCheckerVerify() throws Exception {
        String beforePythonCode = """
            class User:
                def validate(self, email):
                    return "@" in email
            """;

        String afterUserCode = """
            class User:
                pass
            """;

        String afterCheckerCode = """
            class EmailChecker:
                def verify(self, email):
                    return "@" in email
            """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "user.py", afterUserCode,
                "checker.py", afterCheckerCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "validate", "verify", "User", "EmailChecker");
    }

    @Test
    void detectsMoveAndRenameMethod_CountToUtilsCalculate() throws Exception {
        String beforePythonCode = """
        class ListManager:
            def count(self, items):
                total = 0
                for item in items:
                    if item > 0:
                        total += 1
                return total
            
            def add_item(self, items, item):
                items.append(item)
        """;

        String afterManagerCode = """
        class ListManager:
            def add_item(self, items, item):
                items.append(item)
        """;

        String afterUtilsCode = """
        class MathUtils:
            def calculate(self, items):
                total = 0
                for item in items:
                    if item > 0:
                        total += 1
                return total
        """;

        Map<String, String> beforeFiles = Map.of("list_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "list_manager.py", afterManagerCode,
                "math_utils.py", afterUtilsCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "count", "calculate", "ListManager", "MathUtils");
    }

    @Test
    void detectsMoveAndRenameMethod_FormatToRendererDisplay() throws Exception {
        String beforePythonCode = """
        class TextProcessor:
            def format(self, text, width):
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
                return lines
        """;

        String afterProcessorCode = """
        class TextProcessor:
            pass
        """;

        String afterRendererCode = """
        class TextRenderer:
            def display(self, text, width):
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
                return lines
        """;

        Map<String, String> beforeFiles = Map.of("text_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "text_processor.py", afterProcessorCode,
                "text_renderer.py", afterRendererCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "format", "display", "TextProcessor", "TextRenderer");
    }

    @Test
    void detectsMoveAndRenameMethod_SearchToFinderLocate() throws Exception {
        String beforePythonCode = """
        class DataManager:
            def search(self, data, key):
                index = 0
                while index < len(data):
                    if data[index]['id'] == key:
                        return data[index]
                    index += 1
                return None
            
            def update(self, data, item):
                data.append(item)
        """;

        String afterManagerCode = """
        class DataManager:
            def update(self, data, item):
                data.append(item)
        """;

        String afterFinderCode = """
        class ItemFinder:
            def locate(self, data, key):
                index = 0
                while index < len(data):
                    if data[index]['id'] == key:
                        return data[index]
                    index += 1
                return None
        """;

        Map<String, String> beforeFiles = Map.of("data_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "data_manager.py", afterManagerCode,
                "item_finder.py", afterFinderCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "search", "locate", "DataManager", "ItemFinder");
    }

    @Test
    void detectsMoveAndRenameMethod_FilterToSelectorChoose() throws Exception {
        String beforePythonCode = """
        class ListProcessor:
            def filter(self, items, criteria):
                results = []
                for item in items:
                    value = item.get('score', 0)
                    if criteria == 'high' and value > 80:
                        results.append(item)
                    elif criteria == 'medium' and 50 <= value <= 80:
                        results.append(item)
                    elif criteria == 'low' and value < 50:
                        results.append(item)
                return results
        """;

        String afterProcessorCode = """
        class ListProcessor:
            pass
        """;

        String afterSelectorCode = """
        class ItemSelector:
            def choose(self, items, criteria):
                results = []
                for item in items:
                    value = item.get('score', 0)
                    if criteria == 'high' and value > 80:
                        results.append(item)
                    elif criteria == 'medium' and 50 <= value <= 80:
                        results.append(item)
                    elif criteria == 'low' and value < 50:
                        results.append(item)
                return results
        """;

        Map<String, String> beforeFiles = Map.of("list_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "list_processor.py", afterProcessorCode,
                "item_selector.py", afterSelectorCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "filter", "choose", "ListProcessor", "ItemSelector");
    }

    @Test
    void detectsMoveAndRenameMethod_TransformToConverterConvert() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def transform(self, data):
                result = {}
                keys = list(data.keys())
                for key in keys:
                    value = data[key]
                    if isinstance(value, str):
                        result[key.upper()] = value.lower()
                    elif isinstance(value, (int, float)):
                        result[key.upper()] = value * 2
                    else:
                        result[key.upper()] = str(value)
                return result
            
            def cleanup(self, data):
                return {k: v for k, v in data.items() if v is not None}
        """;

        String afterProcessorCode = """
        class DataProcessor:
            def cleanup(self, data):
                return {k: v for k, v in data.items() if v is not None}
        """;

        String afterConverterCode = """
        class DataConverter:
            def convert(self, data):
                result = {}
                keys = list(data.keys())
                for key in keys:
                    value = data[key]
                    if isinstance(value, str):
                        result[key.upper()] = value.lower()
                    elif isinstance(value, (int, float)):
                        result[key.upper()] = value * 2
                    else:
                        result[key.upper()] = str(value)
                return result
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "data_processor.py", afterProcessorCode,
                "data_converter.py", afterConverterCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "transform", "convert", "DataProcessor", "DataConverter");
    }

    @Test
    void detectsMoveAndRenameMethod_GenerateToBuilderCreate() throws Exception {
        String beforePythonCode = """
        class ReportManager:
            def generate(self, data, options):
                sections = []
                sections.append("=== REPORT HEADER ===")
                sections.append(f"Generated on: {options.get('date', 'N/A')}")
                sections.append(f"Type: {options.get('type', 'Standard')}")
                sections.append("")
                
                sections.append("=== DATA SUMMARY ===")
                sections.append(f"Total records: {len(data)}")
                
                if data:
                    scores = [item.get('score', 0) for item in data]
                    avg_score = sum(scores) / len(scores)
                    max_score = max(scores)
                    min_score = min(scores)
                    
                    sections.append(f"Average score: {avg_score:.2f}")
                    sections.append(f"Highest score: {max_score}")
                    sections.append(f"Lowest score: {min_score}")
                
                sections.append("")
                sections.append("=== DETAILED DATA ===")
                for i, item in enumerate(data):
                    sections.append(f"{i+1}. {item}")
                
                sections.append("")
                sections.append("=== END OF REPORT ===")
                
                return "\\n".join(sections)
            
            def save_report(self, content, filename):
                with open(filename, 'w') as f:
                    f.write(content)
        """;

        String afterManagerCode = """
        class ReportManager:
            def save_report(self, content, filename):
                with open(filename, 'w') as f:
                    f.write(content)
        """;

        String afterBuilderCode = """
        class ReportBuilder:
            def create(self, data, options):
                sections = []
                sections.append("=== REPORT HEADER ===")
                sections.append(f"Generated on: {options.get('date', 'N/A')}")
                sections.append(f"Type: {options.get('type', 'Standard')}")
                sections.append("")
                
                sections.append("=== DATA SUMMARY ===")
                sections.append(f"Total records: {len(data)}")
                
                if data:
                    scores = [item.get('score', 0) for item in data]
                    avg_score = sum(scores) / len(scores)
                    max_score = max(scores)
                    min_score = min(scores)
                    
                    sections.append(f"Average score: {avg_score:.2f}")
                    sections.append(f"Highest score: {max_score}")
                    sections.append(f"Lowest score: {min_score}")
                
                sections.append("")
                sections.append("=== DETAILED DATA ===")
                for i, item in enumerate(data):
                    sections.append(f"{i+1}. {item}")
                
                sections.append("")
                sections.append("=== END OF REPORT ===")
                
                return "\\n".join(sections)
        """;

        Map<String, String> beforeFiles = Map.of("report_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of(
                "report_manager.py", afterManagerCode,
                "report_builder.py", afterBuilderCode
        );

        assertMoveAndRenameMethodRefactoringDetected(beforeFiles, afterFiles,
                "generate", "create", "ReportManager", "ReportBuilder");
    }

    public static void assertMoveAndRenameMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalMethodName,
            String renamedMethodName,
            String sourceClassName,
            String targetClassName
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Refactorings:" + refactorings.size());
        refactorings.forEach(System.out::println);

        boolean moveAndRenameDetected = refactorings.stream()
                .filter(r -> r instanceof MoveOperationRefactoring)
                .map(r -> (MoveOperationRefactoring) r)
                .anyMatch(ref ->
                        ref.getRefactoringType() == RefactoringType.MOVE_AND_RENAME_OPERATION &&
                                ref.getOriginalOperation().getClassName().equals(sourceClassName) &&
                                ref.getMovedOperation().getClassName().equals(targetClassName) &&
                                ref.getOriginalOperation().getName().equals(originalMethodName) &&
                                ref.getMovedOperation().getName().equals(renamedMethodName)
                );



        assertTrue(moveAndRenameDetected, "Expected Move and Rename Method refactoring to be detected");
    }
}