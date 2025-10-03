package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.utils.RefactoringAssertUtils.*;

@Isolated
public class ExtractMethodRefactoringDetectionTest {

    @Test
    void detectsExtractFunction_SimpleClassPython() throws Exception {
        String beforePythonCode = """
    class Example:
        def foo(self):
            x = 1
            y = 2
            print(x + y)
            print("Done")
    """;

        String afterPythonCode = """
    class Example:
        def foo(self):
            bar()
            print("Done")

        def bar():
            x = 1
            y = 2
            print(x + y)
    """;


        Map<String, String> beforeFiles = Map.of("tests/example.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/example.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff umlModelDiff = beforeUML.diff(afterUML);

        System.out.println("Refactorings size: " + umlModelDiff.getRefactorings().size() + "\n");
        System.out.println("Refactoring: " + umlModelDiff.getRefactorings().get(0).getName() + "\n");
        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "foo", "bar");

        assertTrue(extractDetected, "Expected extract function refactoring from foo to bar");
    }


    @Test
    void detectsExtractFunction() throws Exception {
        String beforePythonCode = """
        class Calculator:
            def operate(self, x, y):
                sum = x + y
                print("Sum:", sum)
                diff = x - y
                print("Difference:", diff)
        """;

        String afterPythonCode = """
        class Calculator:
            def operate(self, x, y):
                self.print_sum(x, y)
                diff = x - y
                print("Difference:", diff)
            
            def print_sum(self, x, y):
                sum = x + y
                print("Sum:", sum)
        """;

        Map<String, String> beforeFiles = Map.of("tests/calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/calculator.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        // Debug output of models
        dumpModels(beforeUML, afterUML);

        // Manual detection of extract method
        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "operate", "print_sum");

        assertTrue(extractDetected, "Expected extract function refactoring from operate to print_sum");
    }


    @Test
    void detectsExtractMethod_ValidationLogic() throws Exception {
        String beforePythonCode = """
        class UserValidator:
            def validate_user(self, user_data):
                name = user_data.get('name', '')
                if len(name) < 2:
                    return False
                if not name.isalpha():
                    return False
                
                email = user_data.get('email', '')
                age = user_data.get('age', 0)
                return age >= 18
        """;

        String afterPythonCode = """
        class UserValidator:
            def validate_user(self, user_data):
                if not self.validate_name(user_data.get('name', '')):
                    return False
                
                email = user_data.get('email', '')
                age = user_data.get('age', 0)
                return age >= 18
            
            def validate_name(self, name):
                if len(name) < 2:
                    return False
                if not name.isalpha():
                    return False
                return True
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "validate_user", "validate_name");
        assertTrue(extractDetected, "Expected extract method refactoring from validate_user to validate_name");
    }

    @Test
    void detectsExtractMethod_LoopProcessing() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def process_items(self, items):
                results = []
                for item in items:
                    processed = item.strip().upper()
                    if len(processed) > 0:
                        results.append(processed)
                
                return sorted(results)
        """;

        String afterPythonCode = """
        class DataProcessor:
            def process_items(self, items):
                results = []
                for item in items:
                    processed = self.clean_item(item)
                    if len(processed) > 0:
                        results.append(processed)
                
                return sorted(results)
            
            def clean_item(self, item):
                return item.strip().upper()
        """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "process_items", "clean_item");

        assertTrue(extractDetected, "Expected extract method refactoring from process_items to clean_item");
    }

    @Test
    void detectsExtractMethod_MathCalculation() throws Exception {
        String beforePythonCode = """
        class StatisticsCalculator:
            def calculate_metrics(self, values):
                total = sum(values)
                count = len(values)
                average = total / count if count > 0 else 0
                
                variance_sum = sum((x - average) ** 2 for x in values)
                variance = variance_sum / count if count > 0 else 0
                
                return {'average': average, 'variance': variance}
        """;

        String afterPythonCode = """
        class StatisticsCalculator:
            def calculate_metrics(self, values):
                total = sum(values)
                count = len(values)
                average = total / count if count > 0 else 0
                
                variance = self.calculate_variance(values, average)
                
                return {'average': average, 'variance': variance}
            
            def calculate_variance(self, values, average):
                count = len(values)
                variance_sum = sum((x - average) ** 2 for x in values)
                return variance_sum / count if count > 0 else 0
        """;

        Map<String, String> beforeFiles = Map.of("statistics.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("statistics.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "calculate_metrics", "calculate_variance");
        assertTrue(extractDetected, "Expected extract method refactoring from calculate_metrics to calculate_variance");
    }

    @Test
    void detectsExtractMethod_FileHandling() throws Exception {
        String beforePythonCode = """
        class FileManager:
            def save_data(self, data, filename):
                try:
                    with open(filename, 'w') as f:
                        for key, value in data.items():
                            f.write(f"{key}={value}\\n")
                except IOError as e:
                    print(f"Error writing file: {e}")
                    return False
                
                print(f"Data saved to {filename}")
                return True
        """;

        String afterPythonCode = """
        class FileManager:
            def save_data(self, data, filename):
                try:
                    self.write_config_file(data, filename)
                except IOError as e:
                    print(f"Error writing file: {e}")
                    return False
                
                print(f"Data saved to {filename}")
                return True
            
            def write_config_file(self, data, filename):
                with open(filename, 'w') as f:
                    for key, value in data.items():
                        f.write(f"{key}={value}\\n")
        """;

        Map<String, String> beforeFiles = Map.of("file_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_manager.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "save_data", "write_config_file");
        assertTrue(extractDetected, "Expected extract method refactoring from save_data to write_config_file");
    }

    @Test
    void detectsExtractMethod_ConditionalBranching() throws Exception {
        String beforePythonCode = """
        class ScoreProcessor:
            def process_score(self, score, difficulty):
                if difficulty == 'easy':
                    multiplier = 1.0
                    bonus = 10
                elif difficulty == 'medium':
                    multiplier = 1.5
                    bonus = 25
                else:
                    multiplier = 2.0
                    bonus = 50
                
                final_score = (score * multiplier) + bonus
                return int(final_score)
        """;

        String afterPythonCode = """
        class ScoreProcessor:
            def process_score(self, score, difficulty):
                multiplier, bonus = self.get_difficulty_modifiers(difficulty)
                final_score = (score * multiplier) + bonus
                return int(final_score)
            
            def get_difficulty_modifiers(self, difficulty):
                if difficulty == 'easy':
                    return 1.0, 10
                elif difficulty == 'medium':
                    return 1.5, 25
                else:
                    return 2.0, 50
        """;

        Map<String, String> beforeFiles = Map.of("score_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("score_processor.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "process_score", "get_difficulty_modifiers");
        assertTrue(extractDetected, "Expected extract method refactoring from process_score to get_difficulty_modifiers");
    }

    @Test
    void detectsExtractMethod_ListComprehension() throws Exception {
        String beforePythonCode = """
        class DataFilter:
            def filter_and_transform(self, items, threshold):
                filtered_items = [item for item in items if item['value'] > threshold]
                transformed = [{'id': item['id'], 'score': item['value'] * 2} for item in filtered_items]
                
                result = sorted(transformed, key=lambda x: x['score'], reverse=True)
                return result[:10]
        """;

        String afterPythonCode = """
        class DataFilter:
            def filter_and_transform(self, items, threshold):
                filtered_items = [item for item in items if item['value'] > threshold]
                transformed = self.transform_items(filtered_items)
                
                result = sorted(transformed, key=lambda x: x['score'], reverse=True)
                return result[:10]
            
            def transform_items(self, items):
                return [{'id': item['id'], 'score': item['value'] * 2} for item in items]
        """;

        Map<String, String> beforeFiles = Map.of("data_filter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_filter.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "filter_and_transform", "transform_items");
        assertTrue(extractDetected, "Expected extract method refactoring from filter_and_transform to transform_items");
    }

    @Test
    void detectsExtractMethod_StringFormatting() throws Exception {
        String beforePythonCode = """
        class ReportGenerator:
            def generate_report(self, data):
                header = f"{'Name':<20} {'Score':<10} {'Grade':<5}"
                separator = "-" * 35
                
                rows = []
                for item in data:
                    name = item['name'][:20].ljust(20)
                    score = str(item['score']).ljust(10)
                    grade = self.calculate_grade(item['score'])
                    rows.append(f"{name} {score} {grade}")
                
                return header + "\\n" + separator + "\\n" + "\\n".join(rows)
            
            def calculate_grade(self, score):
                return 'A' if score >= 90 else 'B' if score >= 80 else 'C'
        """;

        String afterPythonCode = """
        class ReportGenerator:
            def generate_report(self, data):
                header = self.create_header()
                
                rows = []
                for item in data:
                    name = item['name'][:20].ljust(20)
                    score = str(item['score']).ljust(10)
                    grade = self.calculate_grade(item['score'])
                    rows.append(f"{name} {score} {grade}")
                
                return header + "\\n".join(rows)
            
            def create_header(self):
                header = f"{'Name':<20} {'Score':<10} {'Grade':<5}"
                separator = "-" * 35
                return header + "\\n" + separator + "\\n"
            
            def calculate_grade(self, score):
                return 'A' if score >= 90 else 'B' if score >= 80 else 'C'
        """;

        Map<String, String> beforeFiles = Map.of("report_generator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report_generator.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "generate_report", "create_header");
        assertTrue(extractDetected, "Expected extract method refactoring from generate_report to create_header");
    }

    @Test
    void detectsExtractMethod_DatabaseQuery() throws Exception {
        String beforePythonCode = """
        class UserRepository:
            def find_active_users(self, database, min_age):
                connection = database.connect()
                cursor = connection.cursor()
                
                query = "SELECT id, name, email FROM users WHERE active = 1 AND age >= ?"
                cursor.execute(query, (min_age,))
                raw_results = cursor.fetchall()
                
                users = []
                for row in raw_results:
                    user = {'id': row[0], 'name': row[1], 'email': row[2]}
                    users.append(user)
                
                connection.close()
                return users
        """;

        String afterPythonCode = """
        class UserRepository:
            def find_active_users(self, database, min_age):
                connection = database.connect()
                cursor = connection.cursor()
                
                query = "SELECT id, name, email FROM users WHERE active = 1 AND age >= ?"
                cursor.execute(query, (min_age,))
                raw_results = cursor.fetchall()
                
                users = self.convert_rows_to_users(raw_results)
                
                connection.close()
                return users
            
            def convert_rows_to_users(self, raw_results):
                users = []
                for row in raw_results:
                    user = {'id': row[0], 'name': row[1], 'email': row[2]}
                    users.append(user)
                return users
        """;

        Map<String, String> beforeFiles = Map.of("user_repository.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_repository.py", afterPythonCode);

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        boolean extractDetected = detectExtractMethod(beforeUML, afterUML, "find_active_users", "convert_rows_to_users");
        assertTrue(extractDetected, "Expected extract method refactoring from find_active_users to convert_rows_to_users");
    }
}
