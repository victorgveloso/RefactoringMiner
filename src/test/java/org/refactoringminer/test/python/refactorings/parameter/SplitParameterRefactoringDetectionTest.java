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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class SplitParameterRefactoringDetectionTest {


    @Test
    void detectsSplitParameter_ConfigObjectToSeparateParameters() throws Exception {
        String beforePythonCode = """
            def setup_database(db_config):
                host = db_config.get('host', 'localhost')
                port = db_config.get('port', 5432)
                username = db_config.get('username', 'user')
                password = db_config.get('password', 'pass')
                database_name = db_config.get('database', 'mydb')
                
                return DatabaseConnection(host, port, username, password, database_name)
            """;

        String afterPythonCode = """
            def setup_database(host, port, username, password, database_name):
                return DatabaseConnection(host, port, username, password, database_name)
            """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "db_config", Set.of("host", "port", "username", "password", "database_name"), "setup_database", "");
    }

    @Test
    void detectsSplitParameter_UserDataToIndividualFields() throws Exception {
        String beforePythonCode = """
        class UserService:
            def create_user(self, user_data):
                name = user_data['name']
                email = user_data['email']
                age = user_data['age']
                department = user_data['department']
                
                return User(name, email, age, department)
        """;

        String afterPythonCode = """
        class UserService:
            def create_user(self, name, email, age, department):
                return User(name, email, age, department)
        """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "user_data", Set.of("name", "email", "age", "department"), "create_user", "UserService");
    }

    @Test
    void detectsSplitParameter_SettingsObjectToConfigValues() throws Exception {
        String beforePythonCode = """
        def initialize_app(settings):
            debug_mode = settings.get('debug', False)
            log_level = settings.get('log_level', 'INFO')
            max_connections = settings.get('max_connections', 100)
            cache_enabled = settings.get('cache_enabled', True)
            
            app = Application()
            app.configure(debug_mode, log_level, max_connections, cache_enabled)
            return app
        """;

        String afterPythonCode = """
        def initialize_app(debug_mode, log_level, max_connections, cache_enabled):
            app = Application()
            app.configure(debug_mode, log_level, max_connections, cache_enabled)
            return app
        """;

        Map<String, String> beforeFiles = Map.of("app_initializer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("app_initializer.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "settings", Set.of("debug_mode", "log_level", "max_connections", "cache_enabled"), "initialize_app", "");
    }

    @Test
    void detectsSplitParameter_CoordinatesToXYZ() throws Exception {
        String beforePythonCode = """
        class GeometryCalculator:
            def calculate_distance(self, point):
                x = point[0]
                y = point[1]
                z = point[2]
                
                return (x**2 + y**2 + z**2) ** 0.5
            
            def translate_point(self, point, offset):
                return [point[0] + offset, point[1] + offset, point[2] + offset]
        """;

        String afterPythonCode = """
        class GeometryCalculator:
            def calculate_distance(self, x, y, z):
                return (x**2 + y**2 + z**2) ** 0.5
            
            def translate_point(self, point, offset):
                return [point[0] + offset, point[1] + offset, point[2] + offset]
        """;

        Map<String, String> beforeFiles = Map.of("geometry_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("geometry_calculator.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "point", Set.of("x", "y", "z"), "calculate_distance", "GeometryCalculator");
    }

    @Test
    void detectsSplitParameter_PersonInfoToBasicFields() throws Exception {
        String beforePythonCode = """
        def format_person_info(person_info):
            first_name = person_info.get('first_name', '')
            last_name = person_info.get('last_name', '')
            title = person_info.get('title', 'Mr.')
            
            return f"{title} {first_name} {last_name}".strip()
        """;

        String afterPythonCode = """
        def format_person_info(first_name, last_name, title):
            return f"{title} {first_name} {last_name}".strip()
        """;

        Map<String, String> beforeFiles = Map.of("person_formatter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("person_formatter.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "person_info", Set.of("first_name", "last_name", "title"), "format_person_info", "");
    }

    @Test
    void detectsSplitParameter_ConnectionParamsToComponents() throws Exception {
        String beforePythonCode = """
        class NetworkClient:
            def connect(self, connection_params):
                server_host = connection_params['host']
                server_port = connection_params['port']
                use_ssl = connection_params.get('ssl', False)
                timeout_seconds = connection_params.get('timeout', 30)
                
                if use_ssl:
                    return f"SSL connection to {server_host}:{server_port} (timeout: {timeout_seconds}s)"
                else:
                    return f"Connection to {server_host}:{server_port} (timeout: {timeout_seconds}s)"
        """;

        String afterPythonCode = """
        class NetworkClient:
            def connect(self, server_host, server_port, use_ssl, timeout_seconds):
                if use_ssl:
                    return f"SSL connection to {server_host}:{server_port} (timeout: {timeout_seconds}s)"
                else:
                    return f"Connection to {server_host}:{server_port} (timeout: {timeout_seconds}s)"
        """;

        Map<String, String> beforeFiles = Map.of("network_client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("network_client.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "connection_params", Set.of("server_host", "server_port", "use_ssl", "timeout_seconds"), "connect", "NetworkClient");
    }

    @Test
    void detectsSplitParameter_EmailConfigToProperties() throws Exception {
        String beforePythonCode = """
        def send_email(email_config):
            recipient = email_config['to']
            subject = email_config['subject']
            body = email_config['body']
            sender = email_config.get('from', 'noreply@company.com')
            
            email_message = f"From: {sender}\\nTo: {recipient}\\nSubject: {subject}\\n\\n{body}"
            return f"Sending email: {email_message}"
        """;

        String afterPythonCode = """
        def send_email(recipient, subject, body, sender):
            email_message = f"From: {sender}\\nTo: {recipient}\\nSubject: {subject}\\n\\n{body}"
            return f"Sending email: {email_message}"
        """;

        Map<String, String> beforeFiles = Map.of("email_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("email_service.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "email_config", Set.of("recipient", "subject", "body", "sender"), "send_email", "");
    }

    @Test
    void detectsSplitParameter_ReportOptionsToFlags() throws Exception {
        String beforePythonCode = """
        class ReportGenerator:
            def generate_report(self, data, options):
                include_header = options.get('include_header', True)
                include_footer = options.get('include_footer', True)
                show_summary = options.get('show_summary', False)
                detailed_view = options.get('detailed_view', False)
                
                lines = []
                if include_header:
                    lines.append("=== REPORT ===")
                
                if show_summary:
                    lines.append(f"Total items: {len(data)}")
                
                if detailed_view:
                    lines.extend([str(item) for item in data])
                else:
                    lines.append("Data available")
                
                if include_footer:
                    lines.append("=== END ===")
                
                return "\\n".join(lines)
        """;

        String afterPythonCode = """
        class ReportGenerator:
            def generate_report(self, data, include_header, include_footer, show_summary, detailed_view):
                lines = []
                if include_header:
                    lines.append("=== REPORT ===")
                
                if show_summary:
                    lines.append(f"Total items: {len(data)}")
                
                if detailed_view:
                    lines.extend([str(item) for item in data])
                else:
                    lines.append("Data available")
                
                if include_footer:
                    lines.append("=== END ===")
                
                return "\\n".join(lines)
        """;

        Map<String, String> beforeFiles = Map.of("report_generator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("report_generator.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "options", Set.of("include_header", "include_footer", "show_summary", "detailed_view"), "generate_report", "ReportGenerator");
    }

    @Test
    void detectsSplitParameter_AuthCredentialsToFields() throws Exception {
        String beforePythonCode = """
        def authenticate_user(credentials):
            username = credentials['username']
            password = credentials['password']
            domain = credentials.get('domain', 'default')
            
            if len(username) > 0 and len(password) >= 8:
                return f"User {username}@{domain} authenticated successfully"
            return "Authentication failed"
        """;

        String afterPythonCode = """
        def authenticate_user(username, password, domain):
            if len(username) > 0 and len(password) >= 8:
                return f"User {username}@{domain} authenticated successfully"
            return "Authentication failed"
        """;

        Map<String, String> beforeFiles = Map.of("auth_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("auth_service.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "credentials", Set.of("username", "password", "domain"), "authenticate_user", "");
    }

    @Test
    void detectsSplitParameter_AddressObjectToComponents() throws Exception {
        String beforePythonCode = """
        class AddressValidator:
            def validate_address(self, address):
                street = address.get('street', '')
                city = address.get('city', '')
                state = address.get('state', '')
                zip_code = address.get('zip_code', '')
                country = address.get('country', 'US')
                
                is_valid = (len(street) > 0 and 
                           len(city) > 0 and 
                           len(state) >= 2 and 
                           len(zip_code) >= 5)
                
                return {
                    'valid': is_valid,
                    'formatted': f"{street}, {city}, {state} {zip_code}, {country}"
                }
        """;

        String afterPythonCode = """
        class AddressValidator:
            def validate_address(self, street, city, state, zip_code, country):
                is_valid = (len(street) > 0 and 
                           len(city) > 0 and 
                           len(state) >= 2 and 
                           len(zip_code) >= 5)
                
                return {
                    'valid': is_valid,
                    'formatted': f"{street}, {city}, {state} {zip_code}, {country}"
                }
        """;

        Map<String, String> beforeFiles = Map.of("address_validator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("address_validator.py", afterPythonCode);

        assertSplitParameterRefactoringDetected(beforeFiles, afterFiles,
                "address", Set.of("street", "city", "state", "zip_code", "country"), "validate_address", "AddressValidator");
    }

    public static void assertSplitParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalParameterName,
            Set<String> splitParameterNames,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

//        System.out.println("\n=== SPLIT PARAMETER TEST: " + originalParameterName + " -> " + splitParameterNames + " ===");
//        System.out.println("Original parameter: " + originalParameterName);
//        System.out.println("Split parameters: " + splitParameterNames);
//        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
//        System.out.println("Total refactorings detected: " + refactorings.size());

        boolean splitParameterFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.SPLIT_PARAMETER.equals(r.getRefactoringType()) &&
                        r.toString().contains(originalParameterName) &&
                        r.toString().contains(methodName));


        System.out.println("Available refactorings:");
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));


        assertTrue(splitParameterFound, "Expected Split Parameter refactoring to be detected");
    }
}