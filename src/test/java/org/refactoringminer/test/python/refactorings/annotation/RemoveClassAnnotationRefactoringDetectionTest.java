package org.refactoringminer.test.python.refactorings.annotation;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RemoveClassAnnotationRefactoring;
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
public class RemoveClassAnnotationRefactoringDetectionTest {

    @Test
    void detectsRemoveClassAnnotation_Dataclass() throws Exception {
        String beforePythonCode = """
            @dataclass
            class User:
                def __init__(self, name, email):
                    self.name = name
                    self.email = email
                
                def get_display_name(self):
                    return f"{self.name} <{self.email}>"
            """;

        String afterPythonCode = """
            class User:
                def __init__(self, name, email):
                    self.name = name
                    self.email = email
                
                def get_display_name(self):
                    return f"{self.name} <{self.email}>"
            """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "dataclass", "User");
    }

    @Test
    void detectsRemoveClassAnnotation_DataclassWithFrozen() throws Exception {
        String beforePythonCode = """
            @dataclass(frozen=True)
            class Point:
                def __init__(self, x, y):
                    self.x = x
                    self.y = y
                
                def distance_from_origin(self):
                    return (self.x ** 2 + self.y ** 2) ** 0.5
            """;

        String afterPythonCode = """
            class Point:
                def __init__(self, x, y):
                    self.x = x
                    self.y = y
                
                def distance_from_origin(self):
                    return (self.x ** 2 + self.y ** 2) ** 0.5
            """;

        Map<String, String> beforeFiles = Map.of("point.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("point.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "dataclass", "Point");
    }

    @Test
    void detectsRemoveClassAnnotation_Singleton() throws Exception {
        String beforePythonCode = """
            @singleton
            class DatabaseConnection:
                def __init__(self):
                    self.connection = None
                    self.is_connected = False
                
                def connect(self):
                    if not self.is_connected:
                        self.connection = "database_connection"
                        self.is_connected = True
                    return self.connection
            """;

        String afterPythonCode = """
            class DatabaseConnection:
                def __init__(self):
                    self.connection = None
                    self.is_connected = False
                
                def connect(self):
                    if not self.is_connected:
                        self.connection = "database_connection"
                        self.is_connected = True
                    return self.connection
            """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "singleton", "DatabaseConnection");
    }

    @Test
    void detectsRemoveClassAnnotation_Injectable() throws Exception {
        String beforePythonCode = """
            @injectable
            class UserService:
                def __init__(self, user_repository):
                    self.user_repository = user_repository
                
                def get_user_by_id(self, user_id):
                    return self.user_repository.find_by_id(user_id)
                
                def create_user(self, name, email):
                    return self.user_repository.save(name, email)
            """;

        String afterPythonCode = """
            class UserService:
                def __init__(self, user_repository):
                    self.user_repository = user_repository
                
                def get_user_by_id(self, user_id):
                    return self.user_repository.find_by_id(user_id)
                
                def create_user(self, name, email):
                    return self.user_repository.save(name, email)
            """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "injectable", "UserService");
    }

    @Test
    void detectsRemoveClassAnnotation_ComponentWithName() throws Exception {
        String beforePythonCode = """
            @component(name="email_service")
            class EmailService:
                def __init__(self, smtp_config):
                    self.smtp_config = smtp_config
                
                def send_email(self, to, subject, body):
                    return f"Sending email to {to}: {subject}"
                
                def send_bulk_email(self, recipients, subject, body):
                    for recipient in recipients:
                        self.send_email(recipient, subject, body)
            """;

        String afterPythonCode = """
            class EmailService:
                def __init__(self, smtp_config):
                    self.smtp_config = smtp_config
                
                def send_email(self, to, subject, body):
                    return f"Sending email to {to}: {subject}"
                
                def send_bulk_email(self, recipients, subject, body):
                    for recipient in recipients:
                        self.send_email(recipient, subject, body)
            """;

        Map<String, String> beforeFiles = Map.of("email_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("email_service.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "component", "EmailService");
    }

    @Test
    void detectsRemoveClassAnnotation_Deprecated() throws Exception {
        String beforePythonCode = """
            @deprecated(version="2.0", reason="Use NewApiClient instead")
            class LegacyApiClient:
                def __init__(self, base_url):
                    self.base_url = base_url
                
                def make_request(self, endpoint):
                    return f"Making request to {self.base_url}/{endpoint}"
                
                def get_data(self):
                    return self.make_request("data")
            """;

        String afterPythonCode = """
            class LegacyApiClient:
                def __init__(self, base_url):
                    self.base_url = base_url
                
                def make_request(self, endpoint):
                    return f"Making request to {self.base_url}/{endpoint}"
                
                def get_data(self):
                    return self.make_request("data")
            """;

        Map<String, String> beforeFiles = Map.of("legacy_api.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("legacy_api.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "deprecated", "LegacyApiClient");
    }

    @Test
    void detectsRemoveClassAnnotation_RegisterComponent() throws Exception {
        String beforePythonCode = """
            @register_component
            class PaymentProcessor:
                def __init__(self, payment_gateway):
                    self.payment_gateway = payment_gateway
                
                def process_payment(self, amount, card_info):
                    return self.payment_gateway.charge(amount, card_info)
                
                def refund_payment(self, transaction_id):
                    return self.payment_gateway.refund(transaction_id)
            """;

        String afterPythonCode = """
            class PaymentProcessor:
                def __init__(self, payment_gateway):
                    self.payment_gateway = payment_gateway
                
                def process_payment(self, amount, card_info):
                    return self.payment_gateway.charge(amount, card_info)
                
                def refund_payment(self, transaction_id):
                    return self.payment_gateway.refund(transaction_id)
            """;

        Map<String, String> beforeFiles = Map.of("payment.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("payment.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "register_component", "PaymentProcessor");
    }

    @Test
    void detectsRemoveClassAnnotation_CacheClass() throws Exception {
        String beforePythonCode = """
            @cache_class
            class ConfigManager:
                def __init__(self):
                    self.config_cache = {}
                
                def load_config(self, config_file):
                    if config_file not in self.config_cache:
                        # Expensive config loading operation
                        self.config_cache[config_file] = self.parse_config(config_file)
                    return self.config_cache[config_file]
                
                def parse_config(self, config_file):
                    return {"setting1": "value1", "setting2": "value2"}
            """;

        String afterPythonCode = """
            class ConfigManager:
                def __init__(self):
                    self.config_cache = {}
                
                def load_config(self, config_file):
                    if config_file not in self.config_cache:
                        # Expensive config loading operation
                        self.config_cache[config_file] = self.parse_config(config_file)
                    return self.config_cache[config_file]
                
                def parse_config(self, config_file):
                    return {"setting1": "value1", "setting2": "value2"}
            """;

        Map<String, String> beforeFiles = Map.of("config.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "cache_class", "ConfigManager");
    }

    @Test
    void detectsRemoveClassAnnotation_Serializable() throws Exception {
        String beforePythonCode = """
            @serializable
            class Transaction:
                def __init__(self, id, amount, timestamp):
                    self.id = id
                    self.amount = amount
                    self.timestamp = timestamp
                
                def get_summary(self):
                    return f"Transaction {self.id}: ${self.amount} at {self.timestamp}"
                
                def is_valid(self):
                    return self.amount > 0 and self.id is not None
            """;

        String afterPythonCode = """
            class Transaction:
                def __init__(self, id, amount, timestamp):
                    self.id = id
                    self.amount = amount
                    self.timestamp = timestamp
                
                def get_summary(self):
                    return f"Transaction {self.id}: ${self.amount} at {self.timestamp}"
                
                def is_valid(self):
                    return self.amount > 0 and self.id is not None
            """;

        Map<String, String> beforeFiles = Map.of("transaction.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("transaction.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "serializable", "Transaction");
    }

    @Test
    void detectsRemoveClassAnnotation_LogClassOperations() throws Exception {
        String beforePythonCode = """
            @log_class_operations
            class AuditService:
                def __init__(self):
                    self.audit_log = []
                
                def log_user_action(self, user_id, action):
                    entry = f"User {user_id} performed: {action}"
                    self.audit_log.append(entry)
                    return entry
                
                def get_audit_trail(self, user_id):
                    return [log for log in self.audit_log if f"User {user_id}" in log]
            """;

        String afterPythonCode = """
            class AuditService:
                def __init__(self):
                    self.audit_log = []
                
                def log_user_action(self, user_id, action):
                    entry = f"User {user_id} performed: {action}"
                    self.audit_log.append(entry)
                    return entry
                
                def get_audit_trail(self, user_id):
                    return [log for log in self.audit_log if f"User {user_id}" in log]
            """;

        Map<String, String> beforeFiles = Map.of("audit.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("audit.py", afterPythonCode);

        assertRemoveClassAnnotationRefactoringDetected(beforeFiles, afterFiles,
                "log_class_operations", "AuditService");
    }

    private void assertRemoveClassAnnotationRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String annotationName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== REMOVE CLASS ANNOTATION TEST: @" + annotationName + " ===");
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        // Look for RemoveClassAnnotationRefactoring
        boolean removeAnnotationDetected = refactorings.stream()
                .anyMatch(refactoring -> {
                    if (refactoring instanceof RemoveClassAnnotationRefactoring removeAnnotation) {
                        String refactoringClassName = removeAnnotation.getClassBefore().getName();
                        String refactoringAnnotationName = removeAnnotation.getAnnotation().getTypeName();

                        return refactoringClassName.equals(className) &&
                                refactoringAnnotationName.equals(annotationName);
                    }
                    return false;
                });

        // Fallback: Look for any refactoring of the correct type
        if (!removeAnnotationDetected) {
            removeAnnotationDetected = refactorings.stream()
                    .anyMatch(r -> r.getRefactoringType() == RefactoringType.REMOVE_CLASS_ANNOTATION &&
                            r.toString().contains(className) &&
                            r.toString().contains(annotationName));
        }

        if (!removeAnnotationDetected) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected Remove Class Annotation refactoring for '@" + annotationName +
                    "' on class '" + className + "' was not detected");
        }

        assertTrue(removeAnnotationDetected, "Expected Remove Class Annotation refactoring to be detected");
    }
}