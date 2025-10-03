package org.refactoringminer.test.python.refactorings.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertMovePackageRefactoringDetected;

@Isolated
public class MovePackageRefactoringDetectionTest {

    @Test
    void detectsMovePackage() throws Exception {

        String beforePythonCode = """
            class A:
                pass
            """;
        String afterPythonCode = """
            class A:
                pass
            """;

        // Create hierarchical package paths (with dots) to trigger MOVE_PACKAGE detection
        Map<String, String> beforeFiles = Map.of(
                "src/com/example/oldpkg/A.py", beforePythonCode,
                "src/com/example/oldpkg/__init__.py", "# Package initialization"
        );

        Map<String, String> afterFiles = Map.of(
                "src/org/example/newpkg/A.py", afterPythonCode,
                "src/org/example/newpkg/__init__.py", "# Package initialization"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "com.example.oldpkg", "org.example.newpkg");
    }

    @Test
    void detectsMovePackage_UtilsToCommon() throws Exception {
        String utilsCode = """
    class StringUtils:
        @staticmethod
        def capitalize_words(text):
            return ' '.join(word.capitalize() for word in text.split())
    
    class MathUtils:
        @staticmethod
        def factorial(n):
            return 1 if n <= 1 else n * MathUtils.factorial(n - 1)
    """;

        String helperCode = """
    def format_date(date_obj):
        return date_obj.strftime('%Y-%m-%d')
    
    def parse_config(config_str):
        return dict(line.split('=') for line in config_str.strip().split('\\n'))
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/myapp/utils/string_utils.py", utilsCode,
                "src/myapp/utils/helpers.py", helperCode,
                "src/myapp/utils/__init__.py", "# Utils package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/shared/common/string_utils.py", utilsCode,  // Different parent: myapp → shared
                "src/shared/common/helpers.py", helperCode,
                "src/shared/common/__init__.py", "# Common package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "myapp.utils", "shared.common");
    }

    @Test
    void detectsMovePackage_ModelsToEntities() throws Exception {
        String userModelCode = """
    class User:
        def __init__(self, name, email):
            self.name = name
            self.email = email
        
        def get_display_name(self):
            return f"{self.name} <{self.email}>"
    """;

        String productModelCode = """
    class Product:
        def __init__(self, name, price, category):
            self.name = name
            self.price = price
            self.category = category
        
        def get_formatted_price(self):
            return f"${self.price:.2f}"
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/ecommerce/models/user.py", userModelCode,
                "src/ecommerce/models/product.py", productModelCode,
                "src/ecommerce/models/__init__.py", "# Models package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/domain/entities/user.py", userModelCode,  // Different parent: ecommerce → domain
                "src/domain/entities/product.py", productModelCode,
                "src/domain/entities/__init__.py", "# Entities package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "ecommerce.models", "domain.entities");
    }

    @Test
    void detectsMovePackage_ServicesToBusinessLogic() throws Exception {
        String authServiceCode = """
    class AuthenticationService:
        def __init__(self, user_repository):
            self.user_repository = user_repository
        
        def authenticate(self, username, password):
            user = self.user_repository.find_by_username(username)
            return user and user.check_password(password)
    """;

        String emailServiceCode = """
    class EmailService:
        def __init__(self, smtp_config):
            self.smtp_config = smtp_config
        
        def send_notification(self, recipient, subject, body):
            return f"Sending email to {recipient}: {subject}"
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/webapp/services/auth_service.py", authServiceCode,
                "src/webapp/services/email_service.py", emailServiceCode,
                "src/webapp/services/__init__.py", "# Services package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/core/business/auth_service.py", authServiceCode,  // Different parent: webapp → core
                "src/core/business/email_service.py", emailServiceCode,
                "src/core/business/__init__.py", "# Business logic package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "webapp.services", "core.business");
    }

    @Test
    void detectsMovePackage_ControllersToHandlers() throws Exception {
        String userControllerCode = """
    class UserController:
        def __init__(self, user_service):
            self.user_service = user_service
        
        def create_user(self, request):
            data = request.get_json()
            return self.user_service.create_user(data['name'], data['email'])
        
        def get_user(self, user_id):
            return self.user_service.get_user_by_id(user_id)
    """;

        String orderControllerCode = """
    class OrderController:
        def __init__(self, order_service):
            self.order_service = order_service
        
        def create_order(self, request):
            data = request.get_json()
            return self.order_service.create_order(data['items'], data['customer_id'])
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/api/controllers/user_controller.py", userControllerCode,
                "src/api/controllers/order_controller.py", orderControllerCode,
                "src/api/controllers/__init__.py", "# Controllers package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/web/handlers/user_controller.py", userControllerCode,  // Different parent: api → web
                "src/web/handlers/order_controller.py", orderControllerCode,
                "src/web/handlers/__init__.py", "# Handlers package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "api.controllers", "web.handlers");
    }

    @Test
    void detectsMovePackage_ConfigToSettings() throws Exception {
        String databaseConfigCode = """
    class DatabaseConfig:
        def __init__(self):
            self.host = 'localhost'
            self.port = 5432
            self.database = 'myapp'
            self.username = 'user'
        
        def get_connection_string(self):
            return f"postgresql://{self.username}@{self.host}:{self.port}/{self.database}"
    """;

        String appConfigCode = """
    class AppConfig:
        def __init__(self):
            self.debug = False
            self.secret_key = 'your-secret-key'
            self.log_level = 'INFO'
        
        def is_development(self):
            return self.debug
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/myapp/config/database.py", databaseConfigCode,
                "src/myapp/config/app_config.py", appConfigCode,
                "src/myapp/config/__init__.py", "# Config package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/configuration/settings/database.py", databaseConfigCode,  // Different parent: myapp → configuration
                "src/configuration/settings/app_config.py", appConfigCode,
                "src/configuration/settings/__init__.py", "# Settings package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "myapp.config", "configuration.settings");
    }

    @Test
    void detectsMovePackage_TestsToUnitTests() throws Exception {
        String userTestCode = """
    import unittest
    
    class TestUser(unittest.TestCase):
        def setUp(self):
            self.user = User("John Doe", "john@example.com")
        
        def test_get_display_name(self):
            self.assertEqual(self.user.get_display_name(), "John Doe <john@example.com>")
        
        def test_email_validation(self):
            self.assertTrue("@" in self.user.email)
    """;

        String serviceTestCode = """
    import unittest
    
    class TestUserService(unittest.TestCase):
        def setUp(self):
            self.service = UserService()
        
        def test_create_user(self):
            user = self.service.create_user("Jane", "jane@example.com")
            self.assertIsNotNone(user)
            self.assertEqual(user.name, "Jane")
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/project/tests/test_user.py", userTestCode,
                "src/project/tests/test_service.py", serviceTestCode,
                "src/project/tests/__init__.py", "# Tests package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/quality/unit_tests/test_user.py", userTestCode,
                "src/quality/unit_tests/test_service.py", serviceTestCode,
                "src/quality/unit_tests/__init__.py", "# Unit tests package"
        );

        // project.tests → quality.unit_tests
        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "project.tests", "quality.unit_tests");
    }


    @Test
    void detectsMovePackage_DatabaseToRepository() throws Exception {
        String userDaoCode = """
    class UserDAO:
        def __init__(self, connection):
            self.connection = connection
        
        def find_by_id(self, user_id):
            query = "SELECT * FROM users WHERE id = %s"
            return self.connection.execute(query, (user_id,))
        
        def save(self, user):
            query = "INSERT INTO users (name, email) VALUES (%s, %s)"
            return self.connection.execute(query, (user.name, user.email))
    """;

        String connectionCode = """
    class DatabaseConnection:
        def __init__(self, config):
            self.config = config
            self.connection = None
        
        def connect(self):
            # Implementation for database connection
            pass
        
        def execute(self, query, params):
            # Implementation for query execution
            pass
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/app/database/user_dao.py", userDaoCode,
                "src/app/database/connection.py", connectionCode,
                "src/app/database/__init__.py", "# Database package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/data/repository/user_dao.py", userDaoCode,  // Different parent: app → data
                "src/data/repository/connection.py", connectionCode,
                "src/data/repository/__init__.py", "# Repository package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "app.database", "data.repository");
    }

    @Test
    void detectsMovePackage_HelpersToSupport() throws Exception {
        String validationHelperCode = """
    def validate_email(email):
        import re
        pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'
        return re.match(pattern, email) is not None
    
    def validate_phone(phone):
        import re
        pattern = r'^\\+?1?\\d{9,15}$'
        return re.match(pattern, phone) is not None
    """;

        String formatHelperCode = """
    def format_currency(amount, currency='USD'):
        symbol_map = {'USD': '$', 'EUR': '€', 'GBP': '£'}
        symbol = symbol_map.get(currency, currency)
        return f"{symbol}{amount:.2f}"
    
    def format_percentage(value):
        return f"{value * 100:.1f}%"
    """;

        Map<String, String> beforeFiles = Map.of(
                "src/project/helpers/validation.py", validationHelperCode,
                "src/project/helpers/formatting.py", formatHelperCode,
                "src/project/helpers/__init__.py", "# Helpers package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/utilities/support/validation.py", validationHelperCode,  // Different parent: project → utilities
                "src/utilities/support/formatting.py", formatHelperCode,
                "src/utilities/support/__init__.py", "# Support package"
        );

        assertMovePackageRefactoringDetected(beforeFiles, afterFiles, "project.helpers", "utilities.support");
    }

    @Test
    void detectsMovePackage_CoreToFoundation() throws Exception {
        String baseEntityCode = """
        from abc import ABC, abstractmethod
        
        class BaseEntity(ABC):
            def __init__(self, id=None):
                self.id = id
            
            @abstractmethod
            def validate(self):
                pass
            
            def is_persisted(self):
                return self.id is not None
        """;

        String baseServiceCode = """
        from abc import ABC, abstractmethod
        
        class BaseService(ABC):
            def __init__(self, repository):
                self.repository = repository
            
            @abstractmethod
            def get_all(self):
                pass
            
            def get_by_id(self, entity_id):
                return self.repository.find_by_id(entity_id)
        """;

        Map<String, String> beforeFiles = Map.of(
                "src/framework/core/base_entity.py", baseEntityCode,
                "src/framework/core/base_service.py", baseServiceCode,
                "src/framework/core/__init__.py", "# Core package"
        );

        Map<String, String> afterFiles = Map.of(
                "src/infrastructure/foundation/base_entity.py", baseEntityCode,
                "src/infrastructure/foundation/base_service.py", baseServiceCode,
                "src/infrastructure/foundation/__init__.py", "# Foundation package"
        );


        assertMovePackageRefactoringDetected(beforeFiles, afterFiles,
                "framework.core", "infrastructure.foundation");
    }

}
