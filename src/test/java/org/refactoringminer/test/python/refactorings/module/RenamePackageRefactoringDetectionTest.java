package org.refactoringminer.test.python.refactorings.module;

import extension.umladapter.UMLAdapterUtil;
import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RenamePackageRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenamePackageRefactoringDetectionTest {

    @Test
    void detectsRenamePackage_utilsToHelpers() throws Exception {
        String beforeCode = """
        class MathUtils:
            def __init__(self):
                self.pi = 3.14159
            
            def add(self, a, b):
                return a + b
        """;

        String afterCode = """
        class MathUtils:
            def __init__(self):
                self.pi = 3.14159
            
            def add(self, a, b):
                return a + b
        """;

        Map<String, String> beforeFiles = Map.of("src/myapp/utils/math.py", beforeCode);
        Map<String, String> afterFiles = Map.of("src/myapp/helpers/math.py", afterCode);

        // Same parent: myapp.utils → myapp.helpers
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "myapp.utils", "myapp.helpers");
    }

    @Test
    void detectsRenamePackage_modelToEntities() throws Exception {
        String beforeUserCode = """
        class User:
            def __init__(self, username):
                self.username = username
                self.email = None
            
            def get_profile(self):
                return f"Profile for {self.username}"
        """;

        String beforeProductCode = """
        class Product:
            def __init__(self, name, price):
                self.name = name
                self.price = price
            
            def calculate_tax(self):
                return self.price * 0.1
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/ecommerce/model/user.py", beforeUserCode,
                "src/ecommerce/model/product.py", beforeProductCode
        );
        Map<String, String> afterFiles = Map.of(
                "src/ecommerce/entities/user.py", beforeUserCode,
                "src/ecommerce/entities/product.py", beforeProductCode
        );

        // Same parent: ecommerce.model → ecommerce.entities
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "ecommerce.model", "ecommerce.entities");
    }

    @Test
    void detectsRenamePackage_servicesToBusinessLogic() throws Exception {
        String beforeServiceCode = """
        class PaymentService:
            def __init__(self):
                self.processor = None
            
            def process_payment(self, amount):
                return f"Processing ${amount}"
        """;

        String beforeEmailCode = """
        class EmailService:
            def __init__(self):
                self.smtp_server = "localhost"
            
            def send_email(self, to, subject, body):
                return f"Sending email to {to}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/webapp/services/payment.py", beforeServiceCode,
                "src/webapp/services/email.py", beforeEmailCode
        );
        Map<String, String> afterFiles = Map.of(
                "src/webapp/business_logic/payment.py", beforeServiceCode,
                "src/webapp/business_logic/email.py", beforeEmailCode
        );

        // Same parent: webapp.services → webapp.business_logic
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "webapp.services", "webapp.business_logic");
    }

    @Test
    void detectsRenamePackage_controllersToHandlers() throws Exception {
        String beforeControllerCode = """
        class UserController:
            def __init__(self):
                self.user_service = None
            
            def create_user(self, user_data):
                return "User created"
            
            def get_user(self, user_id):
                return f"Fetching user {user_id}"
        """;

        Map<String, String> beforeFiles = Map.of("src/api/controllers/user.py", beforeControllerCode);
        Map<String, String> afterFiles = Map.of("src/api/handlers/user.py", beforeControllerCode);

        // Same parent: api.controllers → api.handlers
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "api.controllers", "api.handlers");
    }

    @Test
    void detectsRenamePackage_daoToRepositories() throws Exception {
        String beforeDaoCode = """
        class UserDAO:
            def __init__(self, connection):
                self.connection = connection
            
            def find_by_id(self, user_id):
                return f"SELECT * FROM users WHERE id = {user_id}"
            
            def save(self, user):
                return "INSERT INTO users..."
        """;

        Map<String, String> beforeFiles = Map.of("src/database/dao/user_dao.py", beforeDaoCode);
        Map<String, String> afterFiles = Map.of("src/database/repositories/user_dao.py", beforeDaoCode);

        // Same parent: database.dao → database.repositories
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "database.dao", "database.repositories");
    }

    @Test
    void detectsRenamePackage_viewsToTemplates() throws Exception {
        String beforeViewCode = """
        class ProductView:
            def __init__(self):
                self.template_engine = "jinja2"
            
            def render_list(self, products):
                return "Rendering product list"
            
            def render_detail(self, product):
                return f"Rendering product {product}"
        """;

        Map<String, String> beforeFiles = Map.of("src/frontend/views/product.py", beforeViewCode);
        Map<String, String> afterFiles = Map.of("src/frontend/templates/product.py", beforeViewCode);

        // Same parent: frontend.views → frontend.templates
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "frontend.views", "frontend.templates");
    }

    @Test
    void detectsRenamePackage_configToSettings() throws Exception {
        String beforeConfigCode = """
        class DatabaseConfig:
            def __init__(self):
                self.host = "localhost"
                self.port = 5432
                self.database = "myapp"
            
            def get_connection_string(self):
                return f"postgresql://{self.host}:{self.port}/{self.database}"
        """;

        String beforeAppConfigCode = """
        class AppConfig:
            def __init__(self):
                self.debug = True
                self.secret_key = "dev-key"
            
            def is_development(self):
                return self.debug
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/myapp/config/database.py", beforeConfigCode,
                "src/myapp/config/app.py", beforeAppConfigCode
        );
        Map<String, String> afterFiles = Map.of(
                "src/myapp/settings/database.py", beforeConfigCode,
                "src/myapp/settings/app.py", beforeAppConfigCode
        );

        // Same parent: myapp.config → myapp.settings
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "myapp.config", "myapp.settings");
    }

    @Test
    void detectsRenamePackage_helpersToUtilities() throws Exception {
        String beforeStringHelperCode = """
        class StringHelper:
            @staticmethod
            def capitalize_words(text):
                return " ".join(word.capitalize() for word in text.split())
            
            @staticmethod
            def remove_whitespace(text):
                return "".join(text.split())
        """;

        String beforeDateHelperCode = """
        class DateHelper:
            @staticmethod
            def format_date(date, format_string):
                return date.strftime(format_string)
            
            @staticmethod
            def days_between(date1, date2):
                return abs((date2 - date1).days)
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/common/helpers/string_helper.py", beforeStringHelperCode,
                "src/common/helpers/date_helper.py", beforeDateHelperCode
        );
        Map<String, String> afterFiles = Map.of(
                "src/common/utilities/string_helper.py", beforeStringHelperCode,
                "src/common/utilities/date_helper.py", beforeDateHelperCode
        );

        // Same parent: common.helpers → common.utilities
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "common.helpers", "common.utilities");
    }

    @Test
    void detectsRenamePackage_managersToProcessors() throws Exception {
        String beforeFileManagerCode = """
        class FileManager:
            def __init__(self):
                self.upload_path = "/uploads"
            
            def save_file(self, file_data, filename):
                return f"Saving {filename} to {self.upload_path}"
            
            def delete_file(self, filename):
                return f"Deleting {filename}"
        """;

        Map<String, String> beforeFiles = Map.of("src/core/managers/file_manager.py", beforeFileManagerCode);
        Map<String, String> afterFiles = Map.of("src/core/processors/file_manager.py", beforeFileManagerCode);

        // Same parent: core.managers → core.processors
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "core.managers", "core.processors");
    }

    @Test
    void detectsRenamePackage_middlewareToInterceptors() throws Exception {
        String beforeAuthMiddlewareCode = """
        class AuthenticationMiddleware:
            def __init__(self):
                self.secret_key = "auth-secret"
            
            def process_request(self, request):
                token = request.headers.get('Authorization')
                return self.validate_token(token)
            
            def validate_token(self, token):
                return token is not None
        """;

        String beforeLoggingMiddlewareCode = """
        class LoggingMiddleware:
            def __init__(self):
                self.log_file = "requests.log"
            
            def process_request(self, request):
                return f"Logging request to {request.path}"
            
            def process_response(self, response):
                return f"Logging response with status {response.status}"
        """;

        // Fixed: Use nested structure with same parent hierarchy
        Map<String, String> beforeFiles = Map.of(
                "src/web/middleware/auth.py", beforeAuthMiddlewareCode,
                "src/web/middleware/logging.py", beforeLoggingMiddlewareCode
        );
        Map<String, String> afterFiles = Map.of(
                "src/web/interceptors/auth.py", beforeAuthMiddlewareCode,
                "src/web/interceptors/logging.py", beforeLoggingMiddlewareCode
        );

        // Same parent: web.middleware → web.interceptors
        assertRenamePackageRefactoringDetected(beforeFiles, afterFiles, "web.middleware", "web.interceptors");
    }

    private void assertRenamePackageRefactoringDetected(Map<String, String> beforeFiles,
                                                        Map<String, String> afterFiles,
                                                        String oldPackageName,
                                                        String newPackageName) throws Exception {
        // Debug package extraction
//        System.out.println("\n=== DEBUGGING PACKAGE EXTRACTION ===");
//        for (String file : beforeFiles.keySet()) {
//            String sourceFolder = UMLAdapterUtil.extractSourceFolder(file);
//            String packageName = UMLAdapterUtil.extractPackageName(file);
//            System.out.println("BEFORE: " + file + " → sourceFolder: '" + sourceFolder + "', package: '" + packageName + "'");
//        }
//
//        for (String file : afterFiles.keySet()) {
//            String sourceFolder = UMLAdapterUtil.extractSourceFolder(file);
//            String packageName = UMLAdapterUtil.extractPackageName(file);
//            System.out.println("AFTER: " + file + " → sourceFolder: '" + sourceFolder + "', package: '" + packageName + "'");
//        }

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

//        // Debug UML classes
//        System.out.println("\n=== UML CLASSES ===");
//        System.out.println("BEFORE UML classes: " + beforeUML.getClassList().size());
//        beforeUML.getClassList().forEach(cls ->
//                System.out.println("  - " + cls.getName() + " in package: '" + cls.getPackageName() + "'"));
//
//        System.out.println("AFTER UML classes: " + afterUML.getClassList().size());
//        afterUML.getClassList().forEach(cls ->
//                System.out.println("  - " + cls.getName() + " in package: '" + cls.getPackageName() + "'"));

        UMLModelDiff diff = beforeUML.diff(afterUML);

        // Debug refactorings
        System.out.println("\n=== REFACTORINGS DETECTED ===");
        System.out.println("Total refactorings: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(ref ->
                System.out.println("  - " + ref.getRefactoringType() + ": " + ref));

        boolean renamePackageDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof RenamePackageRefactoring renamePackage) {
                        String originalPackage = renamePackage.getPattern().getBefore();
                        String renamedPackage = renamePackage.getPattern().getAfter();

                        // Handle trailing dots (existing Java logic adds them)
                        String cleanOriginal = originalPackage.endsWith(".") ?
                                originalPackage.substring(0, originalPackage.length() - 1) : originalPackage;
                        String cleanRenamed = renamedPackage.endsWith(".") ?
                                renamedPackage.substring(0, renamedPackage.length() - 1) : renamedPackage;

                        return cleanOriginal.equals(oldPackageName) &&
                                cleanRenamed.equals(newPackageName);
                    }
                    return false;
                });


        assertTrue(renamePackageDetected,
                String.format("Expected Rename Package refactoring from '%s' to '%s'",
                        oldPackageName, newPackageName));
    }
}