package org.refactoringminer.test.python.refactorings.clazz;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class MoveClassRefactoringDetectionTest {

    @Test
    void detectsClassMove_Utils_FromHelperToCommon() throws Exception {
        // BEFORE: Utils class in helper.py
        String beforePythonCode1 = """
            class Utils:
                def greet(self, name):
                    print("Hello", name)
            """;

        String beforePythonCode2 = """
            class Other:
                pass
            """;

        // AFTER: Utils class is now in common.py
        String afterPythonCode1 = """
            class Other:
                pass
            """;

        String afterPythonCode2 = """
            class Utils:
                def greet(self, name):
                    print("Hello", name)
            """;

        Map<String, String> beforeFiles = Map.of(
                "src/helper/helper.py", beforePythonCode1,    // ✅ Now: src/tests/helper.py
                "src/tests/other.py", beforePythonCode2
        );

        Map<String, String> afterFiles = Map.of(
                "src/tests/other.py", afterPythonCode1,
                "src/common/common.py", afterPythonCode2      // ✅ Now: src/tests/common.py
        );


        assertMoveClassRefactoringDetected(
                beforeFiles,
                afterFiles,
                "helper.Utils",
                "common.Utils"
        );
    }


    @Test
    void detectsClassMove_UserModel_FromModelsToEntities() throws Exception {
        String beforeModelsCode = """
        class User:
            def __init__(self, name, email):
                self.name = name
                self.email = email
            
            def get_info(self):
                return f"{self.name} ({self.email})"
        """;

        String beforeEntitiesCode = """
        class Product:
            def __init__(self, name, price):
                self.name = name
                self.price = price
        """;

        String afterModelsCode = """
        class Product:
            def __init__(self, name, price):
                self.name = name
                self.price = price
        """;

        String afterEntitiesCode = """
        class User:
            def __init__(self, name, email):
                self.name = name
                self.email = email
            
            def get_info(self):
                return f"{self.name} ({self.email})"
        """;

        Map<String, String> beforeFiles = Map.of(
                "app/models/user.py", beforeModelsCode,
                "app/entities/product.py", beforeEntitiesCode
        );

        Map<String, String> afterFiles = Map.of(
                "app/models/user.py", afterModelsCode,
                "app/entities/user.py", afterEntitiesCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "models.User", "entities.User");
    }

    @Test
    void detectsClassMove_Calculator_FromUtilsToMath() throws Exception {
        String beforeUtilsCode = """
        class Calculator:
            def add(self, a, b):
                return a + b
            
            def multiply(self, a, b):
                return a * b
        
        class StringHelper:
            def reverse(self, text):
                return text[::-1]
        """;

        String beforeMathCode = """
        class Statistics:
            def mean(self, numbers):
                return sum(numbers) / len(numbers)
        """;

        String afterUtilsCode = """
        class StringHelper:
            def reverse(self, text):
                return text[::-1]
        """;

        String afterMathCode = """
        class Statistics:
            def mean(self, numbers):
                return sum(numbers) / len(numbers)
        
        class Calculator:
            def add(self, a, b):
                return a + b
            
            def multiply(self, a, b):
                return a * b
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/utils/helpers.py", beforeUtilsCode,
                "src/math/stats.py", beforeMathCode
        );

        Map<String, String> afterFiles = Map.of(
                "src/utils/helpers.py", afterUtilsCode,
                "src/math/stats.py", afterMathCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "utils.Calculator", "math.Calculator");
    }

    @Test
    void detectsClassMove_EmailService_FromServicesToNotifications() throws Exception {
        String beforeServicesCode = """
        class EmailService:
            def __init__(self, smtp_host):
                self.smtp_host = smtp_host
            
            def send_email(self, to, subject, body):
                return f"Sending to {to}: {subject}"
        
        class DatabaseService:
            def connect(self):
                return "Connected to database"
        """;

        String beforeNotificationsCode = """
        class SMSService:
            def send_sms(self, phone, message):
                return f"SMS to {phone}: {message}"
        """;

        String afterServicesCode = """
        class DatabaseService:
            def connect(self):
                return "Connected to database"
        """;

        String afterNotificationsCode = """
        class SMSService:
            def send_sms(self, phone, message):
                return f"SMS to {phone}: {message}"
        
        class EmailService:
            def __init__(self, smtp_host):
                self.smtp_host = smtp_host
            
            def send_email(self, to, subject, body):
                return f"Sending to {to}: {subject}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "app/services/core.py", beforeServicesCode,
                "app/notifications/sms.py", beforeNotificationsCode
        );

        Map<String, String> afterFiles = Map.of(
                "app/services/core.py", afterServicesCode,
                "app/notifications/sms.py", afterNotificationsCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "services.EmailService", "notifications.EmailService");
    }

    @Test
    void detectsClassMove_Parser_FromDataToProcessing() throws Exception {
        String beforeDataCode = """
        class JSONParser:
            def parse(self, json_string):
                import json
                return json.loads(json_string)
            
            def stringify(self, data):
                import json
                return json.dumps(data)
        """;

        String beforeProcessingCode = """
        class DataValidator:
            def validate(self, data):
                return isinstance(data, dict)
        """;

        String afterDataCode = """
        # Empty after moving JSONParser
        """;

        String afterProcessingCode = """
        class DataValidator:
            def validate(self, data):
                return isinstance(data, dict)
        
        class JSONParser:
            def parse(self, json_string):
                import json
                return json.loads(json_string)
            
            def stringify(self, data):
                import json
                return json.dumps(data)
        """;

        Map<String, String> beforeFiles = Map.of(
                "lib/data/parser.py", beforeDataCode,
                "lib/processing/validator.py", beforeProcessingCode
        );

        Map<String, String> afterFiles = Map.of(
                "lib/data/parser.py", afterDataCode,
                "lib/processing/validator.py", afterProcessingCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "data.JSONParser", "processing.JSONParser");
    }

    @Test
    void detectsClassMove_Logger_FromCoreToLogging() throws Exception {
        String beforeCoreCode = """
        class Application:
            def start(self):
                return "App started"
        
        class Logger:
            def __init__(self, name):
                self.name = name
            
            def log(self, message):
                print(f"[{self.name}] {message}")
        """;

        String beforeLoggingCode = """
        class FileHandler:
            def write_to_file(self, filename, content):
                return f"Writing to {filename}"
        """;

        String afterCoreCode = """
        class Application:
            def start(self):
                return "App started"
        """;

        String afterLoggingCode = """
        class FileHandler:
            def write_to_file(self, filename, content):
                return f"Writing to {filename}"
        
        class Logger:
            def __init__(self, name):
                self.name = name
            
            def log(self, message):
                print(f"[{self.name}] {message}")
        """;

        Map<String, String> beforeFiles = Map.of(
                "system/core/app.py", beforeCoreCode,
                "system/logging/handlers.py", beforeLoggingCode
        );

        Map<String, String> afterFiles = Map.of(
                "system/core/app.py", afterCoreCode,
                "system/logging/handlers.py", afterLoggingCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "core.Logger", "logging.Logger");
    }

    @Test
    void detectsClassMove_HttpClient_FromNetworkToApi() throws Exception {
        String beforeNetworkCode = """
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
            
            def get(self, endpoint):
                return f"GET {self.base_url}/{endpoint}"
            
            def post(self, endpoint, data):
                return f"POST {self.base_url}/{endpoint} with {data}"
        """;

        String beforeApiCode = """
        class ResponseParser:
            def parse_json(self, response):
                return {"parsed": response}
        """;

        String afterNetworkCode = """
        # HttpClient moved to api package
        """;

        String afterApiCode = """
        class ResponseParser:
            def parse_json(self, response):
                return {"parsed": response}
        
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
            
            def get(self, endpoint):
                return f"GET {self.base_url}/{endpoint}"
            
            def post(self, endpoint, data):
                return f"POST {self.base_url}/{endpoint} with {data}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "client/network/http.py", beforeNetworkCode,
                "client/api/parser.py", beforeApiCode
        );

        Map<String, String> afterFiles = Map.of(
                "client/network/http.py", afterNetworkCode,
                "client/api/parser.py", afterApiCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "network.HttpClient", "api.HttpClient");
    }

    @Test
    void detectsClassMove_FileManager_FromSystemToStorage() throws Exception {
        String beforeSystemCode = """
        class SystemInfo:
            def get_os(self):
                import platform
                return platform.system()
        
        class FileManager:
            def read_file(self, path):
                return f"Reading {path}"
            
            def write_file(self, path, content):
                return f"Writing to {path}: {content}"
        """;

        String beforeStorageCode = """
        class DatabaseStorage:
            def save(self, data):
                return f"Saved {data} to database"
        """;

        String afterSystemCode = """
        class SystemInfo:
            def get_os(self):
                import platform
                return platform.system()
        """;

        String afterStorageCode = """
        class DatabaseStorage:
            def save(self, data):
                return f"Saved {data} to database"
        
        class FileManager:
            def read_file(self, path):
                return f"Reading {path}"
            
            def write_file(self, path, content):
                return f"Writing to {path}: {content}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "os/system/info.py", beforeSystemCode,
                "os/storage/database.py", beforeStorageCode
        );

        Map<String, String> afterFiles = Map.of(
                "os/system/info.py", afterSystemCode,
                "os/storage/database.py", afterStorageCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "system.FileManager", "storage.FileManager");
    }

    @Test
    void detectsClassMove_ConfigManager_FromAppToConfig() throws Exception {
        String beforeAppCode = """
        class ConfigManager:
            def __init__(self):
                self.settings = {}
            
            def get_setting(self, key):
                return self.settings.get(key)
            
            def set_setting(self, key, value):
                self.settings[key] = value
        
        class MainApp:
            def run(self):
                return "Application running"
        """;

        String beforeConfigCode = """
        class Environment:
            def is_production(self):
                return False
        """;

        String afterAppCode = """
        class MainApp:
            def run(self):
                return "Application running"
        """;

        String afterConfigCode = """
        class Environment:
            def is_production(self):
                return False
        
        class ConfigManager:
            def __init__(self):
                self.settings = {}
            
            def get_setting(self, key):
                return self.settings.get(key)
            
            def set_setting(self, key, value):
                self.settings[key] = value
        """;

        Map<String, String> beforeFiles = Map.of(
                "project/app/main.py", beforeAppCode,
                "project/config/env.py", beforeConfigCode
        );

        Map<String, String> afterFiles = Map.of(
                "project/app/main.py", afterAppCode,
                "project/config/env.py", afterConfigCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "app.ConfigManager", "config.ConfigManager");
    }

    @Test
    void detectsClassMove_EventHandler_FromEventsToHandlers() throws Exception {
        String beforeEventsCode = """
        class Event:
            def __init__(self, name, data):
                self.name = name
                self.data = data
        
        class EventHandler:
            def handle(self, event):
                return f"Handling event: {event.name}"
        """;

        String beforeHandlersCode = """
        class RequestHandler:
            def handle_request(self, request):
                return f"Processing request: {request}"
        """;

        String afterEventsCode = """
        class Event:
            def __init__(self, name, data):
                self.name = name
                self.data = data
        """;

        String afterHandlersCode = """
        class RequestHandler:
            def handle_request(self, request):
                return f"Processing request: {request}"
        
        class EventHandler:
            def handle(self, event):
                return f"Handling event: {event.name}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "framework/events/event.py", beforeEventsCode,
                "framework/handlers/request.py", beforeHandlersCode
        );

        Map<String, String> afterFiles = Map.of(
                "framework/events/event.py", afterEventsCode,
                "framework/handlers/request.py", afterHandlersCode
        );

        assertMoveClassRefactoringDetected(beforeFiles, afterFiles, "events.EventHandler", "handlers.EventHandler");
    }

    private void assertMoveClassRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalClassQualifiedName,
            String movedClassQualifiedName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        System.out.println("=== BEFORE UML CLASSES ===");
        beforeUML.getClassList().forEach(cls -> {
            System.out.println("Class non-qualified: '" + cls.getNonQualifiedName() + "'");
            System.out.println("Class qualified (getName): '" + cls.getName() + "'");
            System.out.println("  Package: '" + cls.getPackageName() + "'");
            System.out.println("  Source file: '" + cls.getSourceFile() + "'");
            System.out.println("---");
        });

        System.out.println("=== AFTER UML CLASSES ===");
        afterUML.getClassList().forEach(cls -> {
            System.out.println("Class non-qualified: '" + cls.getNonQualifiedName() + "'");
            System.out.println("Class qualified (getName): '" + cls.getName() + "'");
            System.out.println("  Package: '" + cls.getPackageName() + "'");
            System.out.println("  Source file: '" + cls.getSourceFile() + "'");
            System.out.println("---");
        });


        boolean moveClassDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof MoveClassRefactoring moveRef &&
                        moveRef.getOriginalClassName().equals(originalClassQualifiedName) &&
                        moveRef.getMovedClassName().equals(movedClassQualifiedName));

        System.out.println("Refactorings size: " + diff.getRefactorings().size() + "\n");
        diff.getRefactorings().forEach(ref -> System.out.println(ref.getName()));
        assertTrue(
                moveClassDetected,
                String.format(
                        "Expected a MoveClassRefactoring of class '%s' moved to '%s'",
                        originalClassQualifiedName,
                        movedClassQualifiedName
                )
        );
    }
}