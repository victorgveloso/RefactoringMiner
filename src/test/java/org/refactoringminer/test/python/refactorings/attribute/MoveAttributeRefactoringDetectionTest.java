package org.refactoringminer.test.python.refactorings.attribute;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class MoveAttributeRefactoringDetectionTest {

    @Test
    void detectsMoveAttributeBetweenClasses() throws Exception {
        String beforePythonCode = """
        class User:
            def __init__(self):
                self.name = "John"
                self.email = "john@example.com"

        class Profile:
            def __init__(self):
                self.bio = "Software developer"
        """;

        String afterPythonCode = """
        class User:
            def __init__(self):
                self.name = "John"

        class Profile:
            def __init__(self):
                self.bio = "Software developer"
                self.email = "john@example.com"
        """;

        Map<String, String> beforeFiles = Map.of("tests/user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/user.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "User", "Profile", "email");
    }

    @Test
    void detectsMoveAttributeFromParentToChild() throws Exception {
        String beforePythonCode = """
        class Vehicle:
            def __init__(self):
                self.wheels = 4
                self.engine = "V6"

        class Car(Vehicle):
            def __init__(self):
                super().__init__()
                self.doors = 4
        """;

        String afterPythonCode = """
        class Vehicle:
            def __init__(self):
                self.wheels = 4

        class Car(Vehicle):
            def __init__(self):
                super().__init__()
                self.doors = 4
                self.engine = "V6"
        """;

        Map<String, String> beforeFiles = Map.of("tests/vehicle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/vehicle.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "Vehicle", "Car", "engine");
    }

    @Test
    void detectsMoveAttributeFromChildToParent() throws Exception {
        String beforePythonCode = """
        class Shape:
            def __init__(self):
                self.x = 0
                self.y = 0

        class Circle(Shape):
            def __init__(self):
                super().__init__()
                self.radius = 5
                self.color = "red"

        class Rectangle(Shape):
            def __init__(self):
                super().__init__()
                self.width = 10
                self.height = 8
                self.color = "blue"
        """;

        String afterPythonCode = """
        class Shape:
            def __init__(self):
                self.x = 0
                self.y = 0
                self.color = "black"

        class Circle(Shape):
            def __init__(self):
                super().__init__()
                self.radius = 5

        class Rectangle(Shape):
            def __init__(self):
                super().__init__()
                self.width = 10
                self.height = 8
        """;

        Map<String, String> beforeFiles = Map.of("tests/shape.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/shape.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "Circle", "Shape", "color");
    }

    @Test
    void detectsMoveAttributeBetweenSiblingClasses() throws Exception {
        String beforePythonCode = """
        class Employee:
            def __init__(self):
                self.name = "Alice"
                self.id = 12345

        class Manager:
            def __init__(self):
                self.department = "Engineering"
                self.team_size = 10
        """;

        String afterPythonCode = """
        class Employee:
            def __init__(self):
                self.name = "Alice"
                self.department = "Engineering"

        class Manager:
            def __init__(self):
                self.id = 12345
                self.team_size = 10
        """;

        Map<String, String> beforeFiles = Map.of("tests/employee.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/employee.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "Employee", "Manager", "id");
    }

    @Test
    void detectsMoveAttributeWithComplexInitializer() throws Exception {
        String beforePythonCode = """
        class Database:
            def __init__(self):
                self.connection = self.create_connection()
                self.tables = []

        class Cache:
            def __init__(self):
                self.size = 1000
        """;

        String afterPythonCode = """
        class Database:
            def __init__(self):
                self.tables = []

        class Cache:
            def __init__(self):
                self.size = 1000
                self.connection = self.create_connection()
        """;

        Map<String, String> beforeFiles = Map.of("tests/database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/database.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "Database", "Cache", "connection");
    }

    @Test
    void detectsMoveAttribute_validationRules_FromFormToValidator() throws Exception {
        String beforePythonCode = """
    class ContactForm:
        def __init__(self):
            self.name_field = ""
            self.email_field = ""
            self.validation_rules = {
                "name": "required",
                "email": "email_format"
            }
        
        def submit(self):
            return "Form submitted"

    class FormValidator:
        def __init__(self):
            self.error_messages = []
    """;

        String afterPythonCode = """
    class ContactForm:
        def __init__(self):
            self.name_field = ""
            self.email_field = ""
        
        def submit(self):
            return "Form submitted"

    class FormValidator:
        def __init__(self):
            self.error_messages = []
            self.validation_rules = {
                "name": "required",
                "email": "email_format"
            }
        
        def validate(self, form_data):
            return all(rule(form_data[field]) for field, rule in self.validation_rules.items())
    """;

        Map<String, String> beforeFiles = Map.of("forms/contact.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("forms/contact.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "ContactForm", "FormValidator", "validation_rules");
    }

    @Test
    void detectsMoveAttribute_metricsData_FromServiceToAnalytics() throws Exception {
        String beforePythonCode = """
    class PaymentService:
        def __init__(self):
            self.processor = None
            self.metrics_data = {
                "total_transactions": 0,
                "failed_transactions": 0,
                "average_amount": 0.0
            }
        
        def process_payment(self, amount):
            self.metrics_data["total_transactions"] += 1
            return f"Processing payment of ${amount}"

    class AnalyticsEngine:
        def __init__(self):
            self.reports = []
            self.data_sources = []
    """;

        String afterPythonCode = """
    class PaymentService:
        def __init__(self):
            self.processor = None
        
        def process_payment(self, amount):
            return f"Processing payment of ${amount}"

    class AnalyticsEngine:
        def __init__(self):
            self.reports = []
            self.data_sources = []
            self.metrics_data = {
                "total_transactions": 0,
                "failed_transactions": 0,
                "average_amount": 0.0
            }
        
        def generate_report(self):
            return f"Report: {self.metrics_data['total_transactions']} transactions"
    """;

        Map<String, String> beforeFiles = Map.of("services/payment.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("services/payment.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "PaymentService", "AnalyticsEngine", "metrics_data");
    }

    @Test
    void detectsMoveAttribute_templateEngine_FromViewToRenderer() throws Exception {
        String beforePythonCode = """
    class WebView:
        def __init__(self):
            self.context_data = {}
            self.template_name = "default.html"
            self.template_engine = "jinja2"
        
        def render_response(self):
            return f"Rendering {self.template_name} with {self.template_engine}"

    class TemplateRenderer:
        def __init__(self):
            self.cache_enabled = True
            self.template_paths = ["/templates"]
    """;

        String afterPythonCode = """
    class WebView:
        def __init__(self):
            self.context_data = {}
            self.template_name = "default.html"
        
        def render_response(self):
            return f"Rendering {self.template_name}"

    class TemplateRenderer:
        def __init__(self):
            self.cache_enabled = True
            self.template_paths = ["/templates"]
            self.template_engine = "jinja2"
        
        def render_template(self, template_name):
            return f"Rendering {template_name} with {self.template_engine}"
    """;

        Map<String, String> beforeFiles = Map.of("web/views.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("web/views.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "WebView", "TemplateRenderer", "template_engine");
    }

    @Test
    void detectsMoveAttribute_compressionLevel_FromFileUploaderToCompressionService() throws Exception {
        String beforePythonCode = """
    class FileUploader:
        def __init__(self):
            self.upload_directory = "/uploads"
            self.max_file_size = "10MB"
            self.compression_level = 6
            self.allowed_types = [".jpg", ".png", ".pdf"]
        
        def upload_file(self, file_data):
            return f"Uploading file to {self.upload_directory}"

    class CompressionService:
        def __init__(self):
            self.algorithm = "gzip"
            self.temp_directory = "/tmp"
    """;

        String afterPythonCode = """
    class FileUploader:
        def __init__(self):
            self.upload_directory = "/uploads"
            self.max_file_size = "10MB"
            self.allowed_types = [".jpg", ".png", ".pdf"]
        
        def upload_file(self, file_data):
            return f"Uploading file to {self.upload_directory}"

    class CompressionService:
        def __init__(self):
            self.algorithm = "gzip"
            self.temp_directory = "/tmp"
            self.compression_level = 6
        
        def compress_file(self, file_path):
            return f"Compressing {file_path} at level {self.compression_level}"
    """;

        Map<String, String> beforeFiles = Map.of("storage/uploader.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("storage/uploader.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "FileUploader", "CompressionService", "compression_level");
    }

    @Test
    void detectsMoveAttribute_sessionTimeout_FromWebServerToSessionManager() throws Exception {
        String beforePythonCode = """
    class WebServer:
        def __init__(self):
            self.host = "localhost"
            self.port = 8080
            self.session_timeout = 3600
            self.middleware = []
        
        def start_server(self):
            return f"Server started on {self.host}:{self.port}"
        
        def handle_request(self, request):
            return "Request handled"

    class SessionManager:
        def __init__(self):
            self.active_sessions = {}
            self.session_store = "memory"
    """;

        String afterPythonCode = """
    class WebServer:
        def __init__(self):
            self.host = "localhost"
            self.port = 8080
            self.middleware = []
        
        def start_server(self):
            return f"Server started on {self.host}:{self.port}"
        
        def handle_request(self, request):
            return "Request handled"

    class SessionManager:
        def __init__(self):
            self.active_sessions = {}
            self.session_store = "memory"
            self.session_timeout = 3600
        
        def cleanup_expired_sessions(self):
            return f"Cleaning sessions older than {self.session_timeout} seconds"
    """;

        Map<String, String> beforeFiles = Map.of("web/server.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("web/server.py", afterPythonCode);

        assertMoveAttributeRefactoringDetected(beforeFiles, afterFiles, "WebServer", "SessionManager", "session_timeout");
    }

    public static void assertMoveAttributeRefactoringDetected(Map<String, String> beforeFiles,
                                                              Map<String, String> afterFiles,
                                                              String sourceClassName,
                                                              String targetClassName,
                                                              String attributeName) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        java.util.List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean moveAttributeDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof MoveAttributeRefactoring moveAttribute) {
                        UMLAttribute originalAttribute = moveAttribute.getOriginalAttribute();
                        UMLAttribute movedAttribute = moveAttribute.getMovedAttribute();

                        return originalAttribute.getName().equals(attributeName) &&
                                movedAttribute.getName().equals(attributeName) &&
                                moveAttribute.getSourceClassName().equals(sourceClassName) &&
                                moveAttribute.getTargetClassName().equals(targetClassName);
                    }
                    return false;
                });

        assertTrue(moveAttributeDetected,
                String.format("Expected Move Attribute refactoring: attribute '%s' from class '%s' to class '%s'",
                        attributeName, sourceClassName, targetClassName));
    }

}
