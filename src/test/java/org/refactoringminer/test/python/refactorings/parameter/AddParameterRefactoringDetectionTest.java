package org.refactoringminer.test.python.refactorings.parameter;

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
public class AddParameterRefactoringDetectionTest {

    @Test
    void detectsAddParameter_SimpleAddition() throws Exception {
        String beforePythonCode = """
            class UserService:
                def create_user(self, name):
                    return User(name)
                
                def greet_user(self, user):
                    return f"Hello {user.name}!"
            """;

        String afterPythonCode = """
            class UserService:
                def create_user(self, name, email):
                    return User(name, email)
                
                def greet_user(self, user):
                    return f"Hello {user.name}!"
            """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "email", "create_user", "UserService");
    }

    @Test
    void detectsAddParameter_DefaultValueParameter() throws Exception {
        String beforePythonCode = """
            def calculate_area(width, height):
                return width * height
            """;

        String afterPythonCode = """
            def calculate_area(width, height, unit="square_meters"):
                result = width * height
                return f"{result} {unit}"
            """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "unit", "calculate_area", "");
    }


    @Test
    void detectsAddParameter_DatabaseConnection() throws Exception {
        String beforePythonCode = """
        class DatabaseManager:
            def connect(self, host, port):
                return f"Connecting to {host}:{port}"
            
            def execute_query(self, query):
                return f"Executing: {query}"
        """;

        String afterPythonCode = """
        class DatabaseManager:
            def connect(self, host, port, timeout=30):
                return f"Connecting to {host}:{port} (timeout: {timeout})"
            
            def execute_query(self, query):
                if params:
                    return f"Executing: {query} with params {params}"
                return f"Executing: {query}"
        """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "timeout", "connect", "DatabaseManager");
    }

    @Test
    void detectsAddParameter_FileProcessing() throws Exception {
        String beforePythonCode = """
        def read_file(filename):
            with open(filename, 'r') as f:
                return f.read()
        """;

        String afterPythonCode = """
        def read_file(filename, encoding='utf-8'):
            with open(filename, 'r', encoding=encoding) as f:
                return f.read()
        """;

        Map<String, String> beforeFiles = Map.of("file_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_utils.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "encoding", "read_file", "");
    }

    @Test
    void detectsAddParameter_MathCalculation() throws Exception {
        String beforePythonCode = """
        class MathUtils:
            def calculate_interest(self, principal, rate):
                return principal * rate / 100
            
            def compound_interest(self, principal, rate, time):
                return principal * ((1 + rate/100) ** time)
        """;

        String afterPythonCode = """
        class MathUtils:
            def calculate_interest(self, principal, rate, tax_rate=0.0):
                interest = principal * rate / 100
                if tax_rate > 0:
                    interest = interest * (1 - tax_rate/100)
                return interest
            
            def compound_interest(self, principal, rate, time, frequency=1):
                return principal * ((1 + rate/(100*frequency)) ** (frequency*time))
        """;

        Map<String, String> beforeFiles = Map.of("math_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("math_utils.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "tax_rate", "calculate_interest", "MathUtils");
    }

    @Test
    void detectsAddParameter_APIRequest() throws Exception {
        String beforePythonCode = """
        def make_request(url, method):
            return f"Making {method} request to {url}"
        """;

        String afterPythonCode = """
        def make_request(url, method, headers=None):
            result = f"Making {method} request to {url}"
            if headers:
                result += f" with headers: {headers}"
            return result
        """;

        Map<String, String> beforeFiles = Map.of("api_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("api_client.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "headers", "make_request", "");
    }

    @Test
    void detectsAddParameter_GameScore() throws Exception {
        String beforePythonCode = """
        class GameManager:
            def calculate_score(self, points, level):
                return points * level
            
            def save_score(self, player_name, score):
                return f"Saved score {score} for {player_name}"
        """;

        String afterPythonCode = """
        class GameManager:
            def calculate_score(self, points, level):
                return points * level * multiplier
            
            def save_score(self, player_name, score, timestamp=None):
                if timestamp:
                    return f"Saved score {score} for {player_name} at {timestamp}"
                return f"Saved score {score} for {player_name}"
        """;

        Map<String, String> beforeFiles = Map.of("game_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("game_manager.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "timestamp", "save_score", "GameManager");
    }

    @Test
    void detectsAddParameter_LoggingSystem() throws Exception {
        String beforePythonCode = """
        def log_message(message, level):
            print(f"[{level}] {message}")
        """;

        String afterPythonCode = """
        def log_message(message, level, timestamp=True):
            import datetime
            if timestamp:
                now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                print(f"[{now}] [{level}] {message}")
            else:
                print(f"[{level}] {message}")
        """;

        Map<String, String> beforeFiles = Map.of("logger.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("logger.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "timestamp", "log_message", "");
    }

    @Test
    void detectsAddParameter_DataValidation() throws Exception {
        String beforePythonCode = """
        class Validator:
            def validate_email(self, email):
                return "@" in email and "." in email
            
            def validate_password(self, password):
                return len(password) >= 8
        """;

        String afterPythonCode = """
        class Validator:
            def validate_email(self, email, strict=False):
                basic_check = "@" in email and "." in email
                if strict:
                    import re
                    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+[a-zA-Z]{2,}$'
                    return re.match(pattern, email) is not None
                return basic_check
            
            def validate_password(self, password):
                return len(password) >= 8
        """;

        Map<String, String> beforeFiles = Map.of("validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("validator.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "strict", "validate_email", "Validator");
    }

    @Test
    void detectsAddParameter_ConfigurationLoader() throws Exception {
        String beforePythonCode = """
        class ConfigLoader:
            def load_config(self, filename):
                config = {}
                with open(filename, 'r') as f:
                    for line in f:
                        if '=' in line:
                            key, value = line.strip().split('=', 1)
                            config[key] = value
                return config
            
            def get_setting(self, key, config):
                return config.get(key)
        """;

        String afterPythonCode = """
        class ConfigLoader:
            def load_config(self, filename, ignore_comments=True):
                config = {}
                with open(filename, 'r') as f:
                    for line in f:
                        line = line.strip()
                        if ignore_comments and line.startswith('#'):
                            continue
                        if '=' in line:
                            key, value = line.split('=', 1)
                            config[key] = value
                return config
            
            def get_setting(self, key, config):
                return config.get(key)
        """;

        Map<String, String> beforeFiles = Map.of("config_loader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_loader.py", afterPythonCode);

        assertAddParameterRefactoringDetected(beforeFiles, afterFiles,
                "ignore_comments", "load_config", "ConfigLoader");
    }

    public static void assertAddParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String addedParameterName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== ADD PARAMETER TEST: " + addedParameterName + " ===");
        System.out.println("Added parameter: " + addedParameterName);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean addParameterFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.ADD_PARAMETER.equals(r.getRefactoringType()) &&
                        r.toString().contains(addedParameterName) &&
                        r.toString().contains(methodName));

        if (!addParameterFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected add parameter refactoring for parameter '" + addedParameterName +
                    "' in method '" + methodName + "' was not detected");
        }

        assertTrue(addParameterFound, "Expected Add Parameter refactoring to be detected");
    }
}