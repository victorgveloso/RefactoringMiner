package org.refactoringminer.test.python.refactorings.variable;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
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
public class ExtractVariableRefactoringDetectionTest {

    @Test
    void detectsExtractVariable_CalculationToVariable() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def calculate(self, x, y):
                    return x + y * 2 + 5
            """;

        String afterPythonCode = """
            class Calculator:
                def calculate(self, x, y):
                    sum_result = x + y
                    return sum_result * 2 + 5
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "sum_result", "x + y", "calculate", "Calculator");
    }

    @Test
    void detectsExtractVariable_CalculationWithIf() throws Exception {
        String beforePythonCode = """
        class Calculator:
            def calculate(self, x, y):
                if x > 0:
                    print("positive")
                return x + y * 2 + 5
        """;

        String afterPythonCode = """
        class Calculator:
            def calculate(self, x, y):
                if x > 0:
                    print("positive")
                sum_result = x + y
                return sum_result * 2 + 5
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "sum_result", "x + y", "calculate", "Calculator");
    }

    @Test
    void detectsExtractVariable_MethodCallExtraction() throws Exception {
        String beforePythonCode = """
            class FileProcessor:
                def process(self, filename):
                    return open(filename, 'r').read().strip().upper()
            """;

        String afterPythonCode = """
            class FileProcessor:
                def process(self, filename):
                    file_content = open(filename, 'r').read()
                    return file_content.strip().upper()
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "file_content", "open(filename, 'r').read()", "process", "FileProcessor");
    }

    @Test
    void detectsExtractVariable_ConditionalExpression() throws Exception {
        String beforePythonCode = """
            class Validator:
                def validate(self, data):
                    if len(data) > 0 and data.strip() != "":
                        return True
                    return False
            """;

        String afterPythonCode = """
            class Validator:
                def validate(self, data):
                    is_valid_length = len(data) > 0
                    if is_valid_length and data.strip() != "":
                        return True
                    return False
            """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "is_valid_length", "len(data) > 0", "validate", "Validator");
    }


    @Test
    void detectsExtractVariable_StringConcatenation() throws Exception {
        String beforePythonCode = """
            class MessageBuilder:
                def build_message(self, name, age):
                    return f"Hello {name}, you are {age} years old"
            """;

        String afterPythonCode = """
            class MessageBuilder:
                def build_message(self, name, age):
                    greeting = f"Hello {name}"
                    return f"{greeting}, you are {age} years old"
            """;

        Map<String, String> beforeFiles = Map.of("builder.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("builder.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "greeting", "f\"Hello {name}\"", "build_message", "MessageBuilder");
    }


    @Test
    void detectsExtractVariable_ListComprehension() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def process_scores(self, students):
                return [student['score'] * 1.1 for student in students if student['active']]
        """;

        String afterPythonCode = """
        class DataProcessor:
            def process_scores(self, students):
                active_students = [student for student in students if student['active']]
                return [student['score'] * 1.1 for student in active_students]
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "active_students", "[student for student in students if student['active']]",
                "process_scores", "DataProcessor");
    }

    @Test
    void detectsExtractVariable_DictionaryAccess() throws Exception {
        String beforePythonCode = """
        class ConfigReader:
            def get_database_url(self, config):
                return config['database']['host'] + ':' + str(config['database']['port'])
        """;

        String afterPythonCode = """
        class ConfigReader:
            def get_database_url(self, config):
                db_config = config['database']
                return db_config['host'] + ':' + str(db_config['port'])
        """;

        Map<String, String> beforeFiles = Map.of("config_reader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_reader.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "db_config", "config['database']", "get_database_url", "ConfigReader");
    }

    @Test
    void detectsExtractVariable_ComplexArithmetic() throws Exception {
        String beforePythonCode = """
        class GeometryCalculator:
            def calculate_area(self, radius):
                return 3.14159 * radius * radius + 2 * 3.14159 * radius
        """;

        String afterPythonCode = """
        class GeometryCalculator:
            def calculate_area(self, radius):
                circle_area = 3.14159 * radius * radius
                return circle_area + 2 * 3.14159 * radius
        """;

        Map<String, String> beforeFiles = Map.of("geometry_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("geometry_calculator.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "circle_area", "3.14159 * radius * radius", "calculate_area", "GeometryCalculator");
    }

    @Test
    void detectsExtractVariable_NestedMethodCall() throws Exception {
        String beforePythonCode = """
        class TextAnalyzer:
            def analyze_text(self, text):
                return len(text.strip().lower().split())
        """;

        String afterPythonCode = """
        class TextAnalyzer:
            def analyze_text(self, text):
                cleaned_text = text.strip().lower()
                return len(cleaned_text.split())
        """;

        Map<String, String> beforeFiles = Map.of("text_analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("text_analyzer.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "cleaned_text", "text.strip().lower()", "analyze_text", "TextAnalyzer");
    }

    @Test
    void detectsExtractVariable_ConditionalWithComparison() throws Exception {
        String beforePythonCode = """
        class AgeValidator:
            def is_eligible(self, person):
                if person['age'] >= 18 and person['country'] == 'US':
                    return True
                return False
        """;

        String afterPythonCode = """
        class AgeValidator:
            def is_eligible(self, person):
                is_adult = person['age'] >= 18
                if is_adult and person['country'] == 'US':
                    return True
                return False
        """;

        Map<String, String> beforeFiles = Map.of("age_validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("age_validator.py", afterPythonCode);

        assertExtractVariableRefactoringDetected(beforeFiles, afterFiles,
                "is_adult", "person['age'] >= 18", "is_eligible", "AgeValidator");
    }


    public static void assertExtractVariableRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String extractedVariableName,
            String extractedExpression,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Refactorings: " + refactorings.size());

        // === COMPREHENSIVE REFACTORING DEBUG OUTPUT ===
        System.out.println("\n=== EXTRACT VARIABLE TEST: " + extractedVariableName + " ===");
        System.out.println("Expression: " + extractedExpression);
        System.out.println("Method: " + methodName + (className != null ? " in class " + className : " (module-level)"));
        System.out.println("Total refactorings detected: " + refactorings.size());

        if (refactorings.isEmpty()) {
            System.out.println("NO REFACTORINGS DETECTED");
        } else {
            for (int i = 0; i < refactorings.size(); i++) {
                Refactoring r = refactorings.get(i);
                System.out.println("Refactoring #" + (i + 1) + ":");
                System.out.println("  Type: " + r.getRefactoringType());
                System.out.println("  Name: " + r.getName());
                System.out.println("  Details: " + r.toString());

                // Additional details for ExtractVariableRefactoring
                if (r instanceof ExtractVariableRefactoring) {
                    ExtractVariableRefactoring evr = (ExtractVariableRefactoring) r;
                    System.out.println("  [ExtractVariable] Variable: " + evr.getVariableDeclaration().getVariableName());
                    System.out.println("  [ExtractVariable] Operation: " + evr.getOperationAfter().toQualifiedString());
                    System.out.println("  [ExtractVariable] Scope: " + evr.getVariableDeclaration().getScope());
                    if (evr.getVariableDeclaration().getInitializer() != null) {
                        System.out.println("  [ExtractVariable] Initializer: " + evr.getVariableDeclaration().getInitializer().getString());
                    }
                    System.out.println("  [ExtractVariable] References: " + evr.getReferences().size());
                }

                System.out.println("  Involved Classes Before: " + r.getInvolvedClassesBeforeRefactoring());
                System.out.println("  Involved Classes After: " + r.getInvolvedClassesAfterRefactoring());
                System.out.println("  ---");
            }
        }
        System.out.println("=== END REFACTORING DEBUG ===\n");

        // Look for ExtractVariableRefactoring with EXTRACT_VARIABLE type
        boolean extractVariableFound = refactorings.stream()
                .filter(r -> r instanceof ExtractVariableRefactoring)
                .map(r -> (ExtractVariableRefactoring) r)
                .anyMatch(refactoring -> {
                    boolean isExtractVariable = refactoring.getRefactoringType() == RefactoringType.EXTRACT_VARIABLE;
                    String variableName = refactoring.getVariableDeclaration().getVariableName();
                    String operationName = refactoring.getOperationAfter().getName();

                    boolean variableNameMatches = variableName.equals(extractedVariableName);
                    boolean methodMatches = operationName.equals(methodName);

                    // Check class name if provided
                    boolean classMatches = true;
                    if (className != null) {
                        String actualClassName = refactoring.getOperationAfter().getClassName();
                        classMatches = actualClassName.equals(className);
                    }

                    // Check extracted expression if possible
                    boolean expressionMatches = true;
                    if (refactoring.getVariableDeclaration().getInitializer() != null) {
                        String actualExpression = refactoring.getVariableDeclaration().getInitializer().getString();
                        expressionMatches = actualExpression.contains(extractedExpression.replaceAll("\\s+", "")) ||
                                extractedExpression.contains(actualExpression.replaceAll("\\s+", ""));
                    }

                    System.out.println("Checking ExtractVariableRefactoring:");
                    System.out.println("  Is EXTRACT_VARIABLE: " + isExtractVariable);
                    System.out.println("  Variable name matches: " + variableNameMatches + " (" + variableName + ")");
                    System.out.println("  Method matches: " + methodMatches + " (" + operationName + ")");
                    System.out.println("  Class matches: " + classMatches);
                    System.out.println("  Expression matches: " + expressionMatches);

                    return isExtractVariable && variableNameMatches && methodMatches && classMatches;
                });

        assertTrue(extractVariableFound, "Expected Extract Variable refactoring to be detected");
    }
}