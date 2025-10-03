package org.refactoringminer.test.python.refactorings.annotation;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RemoveMethodAnnotationRefactoring;
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
public class RemoveMethodAnnotationRefactoringDetectionTest {

    @Test
    void detectsRemoveMethodAnnotation_StaticMethod() throws Exception {
        String beforePythonCode = """
            class MathUtils:
                @cache
                def calculate_area(radius):
                    return 3.14159 * radius * radius
            """;

        String afterPythonCode = """
            class MathUtils:
                def calculate_area(radius):
                    return 3.14159 * radius * radius
            """;

        Map<String, String> beforeFiles = Map.of("math_utils.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("math_utils.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "cache", "calculate_area", "MathUtils");
    }

    @Test
    void detectsRemoveMethodAnnotation_ClassMethod() throws Exception {
        String beforePythonCode = """
            class User:
                def __init__(self, name):
                    self.name = name
                
                @classmethod
                def from_string(cls, user_string):
                    name = user_string.split(':')[1]
                    return cls(name)
            """;

        String afterPythonCode = """
            class User:
                def __init__(self, name):
                    self.name = name
                
                def from_string(cls, user_string):
                    name = user_string.split(':')[1]
                    return cls(name)
            """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "classmethod", "from_string", "User");
    }

    @Test
    void detectsRemoveMethodAnnotation_Property() throws Exception {
        String beforePythonCode = """
            class Circle:
                def __init__(self, radius):
                    self._radius = radius
                
                @property
                def area(self):
                    return 3.14159 * self._radius ** 2
            """;

        String afterPythonCode = """
            class Circle:
                def __init__(self, radius):
                    self._radius = radius
                
                def area(self):
                    return 3.14159 * self._radius ** 2
            """;

        Map<String, String> beforeFiles = Map.of("circle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("circle.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "property", "area", "Circle");
    }

    @Test
    void detectsRemoveMethodAnnotation_PropertySetter() throws Exception {
        String beforePythonCode = """
            class Temperature:
                def __init__(self):
                    self._celsius = 0
                
                @property
                def celsius(self):
                    return self._celsius
                
                @celsius.setter
                def celsius(self, value):
                    self._celsius = value
            """;

        String afterPythonCode = """
            class Temperature:
                def __init__(self):
                    self._celsius = 0
                
                @property
                def celsius(self):
                    return self._celsius
                
                def celsius(self, value):
                    self._celsius = value
            """;

        Map<String, String> beforeFiles = Map.of("temperature.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("temperature.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "celsius.setter", "celsius", "Temperature");
    }

    @Test
    void detectsRemoveMethodAnnotation_CacheDecorator() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                @cache
                def expensive_calculation(self, data):
                    # Simulate expensive operation
                    result = sum(x * x for x in data)
                    return result
            """;

        String afterPythonCode = """
            class DataProcessor:
                def expensive_calculation(self, data):
                    # Simulate expensive operation
                    result = sum(x * x for x in data)
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "cache", "expensive_calculation", "DataProcessor");
    }

    @Test
    void detectsRemoveMethodAnnotation_TimingDecorator() throws Exception {
        String beforePythonCode = """
            class ApiClient:
                @timing
                def fetch_data(self, url):
                    import requests
                    response = requests.get(url)
                    return response.json()
            """;

        String afterPythonCode = """
            class ApiClient:
                def fetch_data(self, url):
                    import requests
                    response = requests.get(url)
                    return response.json()
            """;

        Map<String, String> beforeFiles = Map.of("api_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("api_client.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "timing", "fetch_data", "ApiClient");
    }

    @Test
    void detectsRemoveMethodAnnotation_RetryDecorator() throws Exception {
        String beforePythonCode = """
            class DatabaseService:
                @retry(max_attempts=3)
                def connect_to_database(self):
                    try:
                        # Database connection logic
                        return "connected"
                    except Exception as e:
                        raise e
            """;

        String afterPythonCode = """
            class DatabaseService:
                def connect_to_database(self):
                    try:
                        # Database connection logic
                        return "connected"
                    except Exception as e:
                        raise e
            """;

        Map<String, String> beforeFiles = Map.of("database_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database_service.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "retry", "connect_to_database", "DatabaseService");
    }

    @Test
    void detectsRemoveMethodAnnotation_ValidateInputDecorator() throws Exception {
        String beforePythonCode = """
            class UserService:
                @validate_input
                def create_user(self, name, email):
                    if not name or not email:
                        raise ValueError("Name and email are required")
                    return {"name": name, "email": email}
            """;

        String afterPythonCode = """
            class UserService:
                def create_user(self, name, email):
                    if not name or not email:
                        raise ValueError("Name and email are required")
                    return {"name": name, "email": email}
            """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "validate_input", "create_user", "UserService");
    }

    @Test
    void detectsRemoveMethodAnnotation_LoggingDecorator() throws Exception {
        String beforePythonCode = """
            class FileManager:
                @log_operation
                def delete_file(self, file_path):
                    import os
                    if os.path.exists(file_path):
                        os.remove(file_path)
                        return True
                    return False
            """;

        String afterPythonCode = """
            class FileManager:
                def delete_file(self, file_path):
                    import os
                    if os.path.exists(file_path):
                        os.remove(file_path)
                        return True
                    return False
            """;

        Map<String, String> beforeFiles = Map.of("file_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_manager.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "log_operation", "delete_file", "FileManager");
    }

    @Test
    void detectsRemoveMethodAnnotation_AuthorizeDecorator() throws Exception {
        String beforePythonCode = """
            class AdminController:
                @require_admin_role
                def delete_all_users(self):
                    # This is a dangerous operation
                    users = self.get_all_users()
                    for user in users:
                        self.delete_user(user.id)
                    return "All users deleted"
                
                def get_all_users(self):
                    return []
                
                def delete_user(self, user_id):
                    pass
            """;

        String afterPythonCode = """
            class AdminController:
                def delete_all_users(self):
                    # This is a dangerous operation
                    users = self.get_all_users()
                    for user in users:
                        self.delete_user(user.id)
                    return "All users deleted"
                
                def get_all_users(self):
                    return []
                
                def delete_user(self, user_id):
                    pass
            """;

        Map<String, String> beforeFiles = Map.of("admin_controller.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("admin_controller.py", afterPythonCode);

        assertRemoveMethodAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "require_admin_role", "delete_all_users", "AdminController");
    }

    private void assertRemoveMethodAnnotationRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String annotationName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== REMOVE METHOD ANNOTATION TEST: @" + annotationName + " ===");
        System.out.println("Method: " + methodName + " in class " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        // Look for RemoveMethodAnnotationRefactoring
        boolean removeAnnotationDetected = refactorings.stream()
                .anyMatch(refactoring -> {
                    if (refactoring instanceof RemoveMethodAnnotationRefactoring removeAnnotation) {
                        String refactoringMethodName = removeAnnotation.getOperationBefore().getName();
                        String refactoringClassName = removeAnnotation.getOperationBefore().getClassName();
                        String refactoringAnnotationName = removeAnnotation.getAnnotation().getTypeName();

                        return refactoringMethodName.equals(methodName) &&
                                refactoringClassName.contains(className) &&
                                refactoringAnnotationName.equals(annotationName);
                    }
                    return false;
                });

        // Fallback: Look for any refactoring of the correct type
        if (!removeAnnotationDetected) {
            removeAnnotationDetected = refactorings.stream()
                    .anyMatch(r -> r.getRefactoringType() == RefactoringType.REMOVE_METHOD_ANNOTATION &&
                            r.toString().contains(methodName) &&
                            r.toString().contains(annotationName));
        }

        if (!removeAnnotationDetected) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected Remove Method Annotation refactoring for '@" + annotationName +
                    "' on method '" + methodName + "' in class '" + className + "' was not detected");
        }

        assertTrue(removeAnnotationDetected, "Expected Remove Method Annotation refactoring to be detected");
    }
}