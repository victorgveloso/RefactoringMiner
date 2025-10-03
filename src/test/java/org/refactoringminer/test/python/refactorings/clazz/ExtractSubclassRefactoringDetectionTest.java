package org.refactoringminer.test.python.refactorings.clazz;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractClassRefactoring;
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
public class ExtractSubclassRefactoringDetectionTest {

    @Test
    void detectsExtractSubclass_VehicleToElectricVehicle_DebugVersion() throws Exception {
        String beforePythonCode = """
            class Vehicle:
                def __init__(self, make, model):
                    self.make = make
                    self.model = model
                    self.is_electric = False
                
                def start_engine(self):
                    return f"{self.make} {self.model} started"
                
                def charge_battery(self):
                    if self.is_electric:
                        return f"Charging {self.make} {self.model}"
                    return "Not an electric vehicle"
            """;

        String afterPythonCode = """
            class Vehicle:
                def __init__(self, make, model):
                    self.make = make
                    self.model = model
                
                def start_engine(self):
                    return f"{self.make} {self.model} started"
            
            class ElectricVehicle(Vehicle):
                def __init__(self, make, model):
                    super().__init__(make, model)
                
                def charge_battery(self):
                    return f"Charging {self.make} {self.model}"
            """;

        Map<String, String> beforeFiles = Map.of("vehicle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("vehicle.py", afterPythonCode);

        // Debug: Print all detected refactorings
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);
        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();
        UMLModelDiff diff = beforeUML.diff(afterUML);

        System.out.println("=== DETECTED REFACTORINGS ===");
        diff.getRefactorings().forEach(r -> {
            System.out.println("Type: " + r.getRefactoringType());
            System.out.println("Name: " + r.getName());
            System.out.println("Details: " + r.toString());
            System.out.println("---");
        });

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles,
                "Vehicle", "ElectricVehicle");
    }

    @Test
    void detectsExtractSubclass_SimpleManagerScenario() throws Exception {
        // Simpler scenario without parameter changes
        String beforePythonCode = """
            class Employee:
                def __init__(self, name):
                    self.name = name
                
                def work(self):
                    return f"{self.name} is working"
                
                def manage_team(self):
                    return f"{self.name} manages team"
            """;

        String afterPythonCode = """
            class Employee:
                def __init__(self, name):
                    self.name = name
                
                def work(self):
                    return f"{self.name} is working"
            
            class Manager(Employee):
                def __init__(self, name):
                    super().__init__(name)
                
                def manage_team(self):
                    return f"{self.name} manages team"
            """;

        Map<String, String> beforeFiles = Map.of("employee.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("employee.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles,
                "Employee", "Manager");
    }

    @Test
    void detectsExtractSubclass_ShapeToSquare() throws Exception {
        String beforePythonCode = """
        class Shape:
            def __init__(self, x, y):
                self.x = x
                self.y = y
                self.is_square = False
            
            def move(self, dx, dy):
                self.x += dx
                self.y += dy
            
            def draw_as_square(self):
                if self.is_square:
                    return f"Drawing square at ({self.x}, {self.y})"
                return "Not a square"
        """;

        String afterPythonCode = """
        class Shape:
            def __init__(self, x, y):
                self.x = x
                self.y = y
            
            def move(self, dx, dy):
                self.x += dx
                self.y += dy
        
        class Square(Shape):
            def __init__(self, x, y):
                super().__init__(x, y)
            
            def draw_as_square(self):
                return f"Drawing square at ({self.x}, {self.y})"
        """;

        Map<String, String> beforeFiles = Map.of("shape.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("shape.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Shape", "Square");
    }

    @Test
    void detectsExtractSubclass_DocumentToPDFDocument() throws Exception {
        String beforePythonCode = """
        class Document:
            def __init__(self, title):
                self.title = title
                self.is_pdf = False
            
            def get_title(self):
                return self.title
            
            def render_pdf(self):
                if self.is_pdf:
                    return f"Rendering PDF: {self.title}"
                return "Not a PDF document"
        """;

        String afterPythonCode = """
        class Document:
            def __init__(self, title):
                self.title = title
            
            def get_title(self):
                return self.title
        
        class PDFDocument(Document):
            def __init__(self, title):
                super().__init__(title)
            
            def render_pdf(self):
                return f"Rendering PDF: {self.title}"
        """;

        Map<String, String> beforeFiles = Map.of("document.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("document.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Document", "PDFDocument");
    }

    @Test
    void detectsExtractSubclass_AccountToPremiumAccount() throws Exception {
        String beforePythonCode = """
        class Account:
            def __init__(self, username):
                self.username = username
                self.is_premium = False
            
            def login(self):
                return f"{self.username} logged in"
            
            def access_premium_features(self):
                if self.is_premium:
                    return f"{self.username} accessing premium features"
                return "Premium features not available"
        """;

        String afterPythonCode = """
        class Account:
            def __init__(self, username):
                self.username = username
            
            def login(self):
                return f"{self.username} logged in"
        
        class PremiumAccount(Account):
            def __init__(self, username):
                super().__init__(username)
            
            def access_premium_features(self):
                return f"{self.username} accessing premium features"
        """;

        Map<String, String> beforeFiles = Map.of("account.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("account.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Account", "PremiumAccount");
    }

    @Test
    void detectsExtractSubclass_ProductToDigitalProduct() throws Exception {
        String beforePythonCode = """
        class Product:
            def __init__(self, name, price):
                self.name = name
                self.price = price
                self.is_digital = False
            
            def get_info(self):
                return f"{self.name}: ${self.price}"
            
            def download(self):
                if self.is_digital:
                    return f"Downloading {self.name}"
                return "Physical product cannot be downloaded"
        """;

        String afterPythonCode = """
        class Product:
            def __init__(self, name, price):
                self.name = name
                self.price = price
            
            def get_info(self):
                return f"{self.name}: ${self.price}"
        
        class DigitalProduct(Product):
            def __init__(self, name, price):
                super().__init__(name, price)
            
            def download(self):
                return f"Downloading {self.name}"
        """;

        Map<String, String> beforeFiles = Map.of("product.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("product.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Product", "DigitalProduct");
    }

    @Test
    void detectsExtractSubclass_TaskToUrgentTask() throws Exception {
        String beforePythonCode = """
        class Task:
            def __init__(self, title):
                self.title = title
                self.is_urgent = False
            
            def get_status(self):
                return f"Task: {self.title}"
            
            def escalate(self):
                if self.is_urgent:
                    return f"Escalating urgent task: {self.title}"
                return "Task is not urgent"
        """;

        String afterPythonCode = """
        class Task:
            def __init__(self, title):
                self.title = title
            
            def get_status(self):
                return f"Task: {self.title}"
        
        class UrgentTask(Task):
            def __init__(self, title):
                super().__init__(title)
            
            def escalate(self):
                return f"Escalating urgent task: {self.title}"
        """;

        Map<String, String> beforeFiles = Map.of("task.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("task.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Task", "UrgentTask");
    }

    @Test
    void detectsExtractSubclass_MessageToEncryptedMessage() throws Exception {
        String beforePythonCode = """
        class Message:
            def __init__(self, content):
                self.content = content
                self.is_encrypted = False
            
            def send(self):
                return f"Sending: {self.content}"
            
            def encrypt_and_send(self):
                if self.is_encrypted:
                    return f"Sending encrypted: {self.content}"
                return "Message is not encrypted"
        """;

        String afterPythonCode = """
        class Message:
            def __init__(self, content):
                self.content = content
            
            def send(self):
                return f"Sending: {self.content}"
        
        class EncryptedMessage(Message):
            def __init__(self, content):
                super().__init__(content)
            
            def encrypt_and_send(self):
                return f"Sending encrypted: {self.content}"
        """;

        Map<String, String> beforeFiles = Map.of("message.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("message.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Message", "EncryptedMessage");
    }

    @Test
    void detectsExtractSubclass_ReportToDetailedReport() throws Exception {
        String beforePythonCode = """
        class Report:
            def __init__(self, title):
                self.title = title
                self.has_details = False
            
            def generate(self):
                return f"Report: {self.title}"
            
            def generate_with_details(self):
                if self.has_details:
                    return f"Detailed report: {self.title} with full analysis"
                return "No detailed information available"
        """;

        String afterPythonCode = """
        class Report:
            def __init__(self, title):
                self.title = title
            
            def generate(self):
                return f"Report: {self.title}"
        
        class DetailedReport(Report):
            def __init__(self, title):
                super().__init__(title)
            
            def generate_with_details(self):
                return f"Detailed report: {self.title} with full analysis"
        """;

        Map<String, String> beforeFiles = Map.of("report.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Report", "DetailedReport");
    }

    @Test
    void detectsExtractSubclass_ConnectionToSecureConnection() throws Exception {
        String beforePythonCode = """
        class Connection:
            def __init__(self, host):
                self.host = host
                self.is_secure = False
            
            def connect(self):
                return f"Connected to {self.host}"
            
            def secure_connect(self):
                if self.is_secure:
                    return f"Secure connection to {self.host} established"
                return "Connection is not secure"
        """;

        String afterPythonCode = """
        class Connection:
            def __init__(self, host):
                self.host = host
            
            def connect(self):
                return f"Connected to {self.host}"
        
        class SecureConnection(Connection):
            def __init__(self, host):
                super().__init__(host)
            
            def secure_connect(self):
                return f"Secure connection to {self.host} established"
        """;

        Map<String, String> beforeFiles = Map.of("connection.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("connection.py", afterPythonCode);

        assertExtractSubclassRefactoringDetected(beforeFiles, afterFiles, "Connection", "SecureConnection");
    }

    public static void assertExtractSubclassRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalClassName,
            String extractedSubclassName
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        // First try: Look for Extract Class refactoring with EXTRACT_SUBCLASS type
        boolean extractSubclassFound = refactorings.stream()
                .filter(r -> r instanceof ExtractClassRefactoring)
                .map(r -> (ExtractClassRefactoring) r)
                .anyMatch(refactoring -> {
                    boolean isExtractSubclass = refactoring.getRefactoringType() == RefactoringType.EXTRACT_SUBCLASS;
                    String extractedName = refactoring.getExtractedClass().getName();
                    String originalName = refactoring.getOriginalClass().getName();

                    return isExtractSubclass &&
                            extractedName.equals(extractedSubclassName) &&
                            originalName.equals(originalClassName);
                });

        assertTrue(extractSubclassFound, "Expected Extract Subclass refactoring to be detected");
    }
}