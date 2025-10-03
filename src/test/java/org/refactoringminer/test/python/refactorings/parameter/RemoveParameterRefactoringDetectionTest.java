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
public class RemoveParameterRefactoringDetectionTest {

    @Test
    void detectsRemoveParameter_UnusedParameter() throws Exception {
        String beforePythonCode = """
            class UserService:
                def create_user(self, name, email, age):
                    return User(name, email)
                
                def update_user(self, user_id, name, legacy_field):
                    return self.repository.update(user_id, name)
            """;

        String afterPythonCode = """
            class UserService:
                def create_user(self, name, email):
                    return User(name, email)
                
                def update_user(self, user_id, name):
                    return self.repository.update(user_id, name)
            """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "age", "create_user", "UserService");
    }

    @Test
    void detectsRemoveParameter_DefaultParameterRemoval() throws Exception {
        String beforePythonCode = """
            def format_text(text, font_size=12, color="black", bold=False):
                if bold:
                    text = f"<b>{text}</b>"
                return f"<span style='font-size:{font_size}px; color:{color}'>{text}</span>"
            """;

        String afterPythonCode = """
            def format_text(text, font_size=12, color="black"):
                return f"<span style='font-size:{font_size}px; color:{color}'>{text}</span>"
            """;

        Map<String, String> beforeFiles = Map.of("formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("formatter.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "bold", "format_text", "");
    }


    @Test
    void detectsRemoveParameter_DeprecatedConfigParam() throws Exception {
        String beforePythonCode = """
        class DatabaseManager:
            def connect(self, host, port, username, password, ssl_mode):
                connection_string = f"{username}:{password}@{host}:{port}"
                return self.create_connection(connection_string)
            
            def create_connection(self, connection_string):
                return f"Connected to {connection_string}"
        """;

        String afterPythonCode = """
        class DatabaseManager:
            def connect(self, host, port, username, password):
                connection_string = f"{username}:{password}@{host}:{port}"
                return self.create_connection(connection_string)
            
            def create_connection(self, connection_string):
                return f"Connected to {connection_string}"
        """;

        Map<String, String> beforeFiles = Map.of("database_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database_manager.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "ssl_mode", "connect", "DatabaseManager");
    }

    @Test
    void detectsRemoveParameter_UnusedLogLevel() throws Exception {
        String beforePythonCode = """
        class Logger:
            def log_message(self, message, timestamp, log_level, thread_id):
                formatted_message = f"[{timestamp}] {message}"
                print(formatted_message)
            
            def get_current_time(self):
                import datetime
                return datetime.datetime.now()
        """;

        String afterPythonCode = """
        class Logger:
            def log_message(self, message, timestamp, log_level):
                formatted_message = f"[{timestamp}] {message}"
                print(formatted_message)
            
            def get_current_time(self):
                import datetime
                return datetime.datetime.now()
        """;

        Map<String, String> beforeFiles = Map.of("logger.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("logger.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "thread_id", "log_message", "Logger");
    }

    @Test
    void detectsRemoveParameter_OptionalFormatting() throws Exception {
        String beforePythonCode = """
        def generate_report(data, title, include_summary, export_format):
            lines = []
            lines.append(f"Report: {title}")
            
            if include_summary:
                lines.append(f"Total items: {len(data)}")
            
            for item in data:
                lines.append(str(item))
            
            return "\\n".join(lines)
        """;

        String afterPythonCode = """
        def generate_report(data, title, include_summary):
            lines = []
            lines.append(f"Report: {title}")
            
            if include_summary:
                lines.append(f"Total items: {len(data)}")
            
            for item in data:
                lines.append(str(item))
            
            return "\\n".join(lines)
        """;

        Map<String, String> beforeFiles = Map.of("report_generator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report_generator.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "export_format", "generate_report", "");
    }

    @Test
    void detectsRemoveParameter_UnusedCallback() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def process_items(self, items, batch_size, progress_callback, error_handler):
                results = []
                for i in range(0, len(items), batch_size):
                    batch = items[i:i + batch_size]
                    processed_batch = [item.upper() for item in batch]
                    results.extend(processed_batch)
                return results
        """;

        String afterPythonCode = """
        class DataProcessor:
            def process_items(self, items, batch_size, progress_callback):
                results = []
                for i in range(0, len(items), batch_size):
                    batch = items[i:i + batch_size]
                    processed_batch = [item.upper() for item in batch]
                    results.extend(processed_batch)
                return results
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "error_handler", "process_items", "DataProcessor");
    }

    @Test
    void detectsRemoveParameter_RedundantValidationFlag() throws Exception {
        String beforePythonCode = """
        class UserValidator:
            def validate_email(self, email, strict_mode, case_sensitive):
                if not email or "@" not in email:
                    return False
                
                if strict_mode:
                    parts = email.split("@")
                    return len(parts) == 2 and "." in parts[1]
                
                return True
        """;

        String afterPythonCode = """
        class UserValidator:
            def validate_email(self, email, strict_mode):
                if not email or "@" not in email:
                    return False
                
                if strict_mode:
                    parts = email.split("@")
                    return len(parts) == 2 and "." in parts[1]
                
                return True
        """;

        Map<String, String> beforeFiles = Map.of("user_validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_validator.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "case_sensitive", "validate_email", "UserValidator");
    }

    @Test
    void detectsRemoveParameter_UnusedTimeoutValue() throws Exception {
        String beforePythonCode = """
        def fetch_data(url, headers, timeout, retry_count):
            import time
            attempts = 0
            while attempts < retry_count:
                try:
                    response = f"Data from {url} with headers {headers}"
                    return response
                except Exception:
                    attempts += 1
                    time.sleep(1)
            return None
        """;

        String afterPythonCode = """
        def fetch_data(url, headers, retry_count):
            import time
            attempts = 0
            while attempts < retry_count:
                try:
                    response = f"Data from {url} with headers {headers}"
                    return response
                except Exception:
                    attempts += 1
                    time.sleep(1)
            return None
        """;

        Map<String, String> beforeFiles = Map.of("http_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("http_client.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "timeout", "fetch_data", "");
    }

    @Test
    void detectsRemoveParameter_DeprecatedSortOption() throws Exception {
        String beforePythonCode = """
        class ListManager:
            def sort_items(self, items, key_func, reverse, stable_sort):
                if reverse:
                    return sorted(items, key=key_func, reverse=True)
                else:
                    return sorted(items, key=key_func)
            
            def filter_items(self, items, predicate):
                return [item for item in items if predicate(item)]
        """;

        String afterPythonCode = """
        class ListManager:
            def sort_items(self, items, key_func, reverse):
                if reverse:
                    return sorted(items, key=key_func, reverse=True)
                else:
                    return sorted(items, key=key_func)
            
            def filter_items(self, items, predicate):
                return [item for item in items if predicate(item)]
        """;

        Map<String, String> beforeFiles = Map.of("list_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("list_manager.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "stable_sort", "sort_items", "ListManager");
    }

    @Test
    void detectsRemoveParameter_UnusedEncodingParam() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def read_file_content(self, file_path, encoding, buffer_size):
                chunks = []
                with open(file_path, 'r') as file:
                    while True:
                        chunk = file.read(buffer_size)
                        if not chunk:
                            break
                        chunks.append(chunk)
                return ''.join(chunks)
            
            def write_file_content(self, file_path, content):
                with open(file_path, 'w') as file:
                    file.write(content)
        """;

        String afterPythonCode = """
        class FileProcessor:
            def read_file_content(self, file_path, buffer_size):
                chunks = []
                with open(file_path, 'r') as file:
                    while True:
                        chunk = file.read(buffer_size)
                        if not chunk:
                            break
                        chunks.append(chunk)
                return ''.join(chunks)
            
            def write_file_content(self, file_path, content):
                with open(file_path, 'w') as file:
                    file.write(content)
        """;

        Map<String, String> beforeFiles = Map.of("file_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_processor.py", afterPythonCode);

        assertRemoveParameterRefactoringDetected(beforeFiles, afterFiles,
                "encoding", "read_file_content", "FileProcessor");
    }

    public static void assertRemoveParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String removedParameterName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== REMOVE PARAMETER TEST: " + removedParameterName + " ===");
        System.out.println("Removed parameter: " + removedParameterName);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean removeParameterFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.REMOVE_PARAMETER.equals(r.getRefactoringType()) &&
                        r.toString().contains(removedParameterName) &&
                        r.toString().contains(methodName));

        if (!removeParameterFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected remove parameter refactoring for parameter '" + removedParameterName +
                    "' in method '" + methodName + "' was not detected");
        }

        assertTrue(removeParameterFound, "Expected Remove Parameter refactoring to be detected");
    }
}