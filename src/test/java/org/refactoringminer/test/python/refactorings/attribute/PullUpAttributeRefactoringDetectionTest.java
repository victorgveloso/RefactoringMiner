package org.refactoringminer.test.python.refactorings.attribute;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertPullUpAttributeRefactoringDetected;

public class PullUpAttributeRefactoringDetectionTest {

    @Test
    void detectsPullUpAttribute() throws Exception {
        String beforePythonCode = """
        class Animal:
            pass

        class Dog(Animal):
            def __init__(self):
                self.legs = 4

        class Cat(Animal):
            def __init__(self):
                pass
        """;
        String afterPythonCode = """
        class Animal:
            def __init__(self):
                self.legs = 4

        class Dog(Animal):
            pass

        class Cat(Animal):
            pass
        """;

        Map<String, String> beforeFiles = Map.of("tests/animal.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/animal.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "Dog", "Animal", "legs");
    }

    @Test
    void detectsPullUpAttribute_DatabaseConnectionsToBaseConnection() throws Exception {
        String beforePythonCode = """
        class DatabaseConnection:
            def __init__(self, host, port):
                self.host = host
                self.port = port
            
            def connect(self):
                return f"Connecting to {self.host}:{self.port}"
        
        class MySQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.connection_timeout = 30
                self.charset = "utf8"
            
            def execute_query(self, query):
                return f"MySQL query: {query}"
        
        class PostgreSQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.connection_timeout = 30
                self.schema = "public"
            
            def execute_query(self, query):
                return f"PostgreSQL query: {query}"
        """;

        String afterPythonCode = """
        class DatabaseConnection:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.connection_timeout = 30
            
            def connect(self):
                return f"Connecting to {self.host}:{self.port}"
        
        class MySQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.charset = "utf8"
            
            def execute_query(self, query):
                return f"MySQL query: {query}"
        
        class PostgreSQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.schema = "public"
            
            def execute_query(self, query):
                return f"PostgreSQL query: {query}"
        """;

        Map<String, String> beforeFiles = Map.of("database/connections.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database/connections.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "MySQLConnection", "DatabaseConnection", "connection_timeout");
    }

    @Test
    void detectsPullUpAttribute_HttpClientsToBaseClient() throws Exception {
        String beforePythonCode = """
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
            
            def make_request(self, method, endpoint):
                return f"{method} {self.base_url}/{endpoint}"
        
        class RestClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.default_headers = {"Content-Type": "application/json"}
                self.retry_count = 3
            
            def get_resource(self, resource_id):
                return self.make_request("GET", f"resources/{resource_id}")
        
        class GraphQLClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.introspection_enabled = True
            
            def execute_query(self, query):
                return self.make_request("POST", "graphql")
        """;

        String afterPythonCode = """
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
                self.default_headers = {"Content-Type": "application/json"}
            
            def make_request(self, method, endpoint):
                return f"{method} {self.base_url}/{endpoint}"
        
        class RestClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.retry_count = 3
            
            def get_resource(self, resource_id):
                return self.make_request("GET", f"resources/{resource_id}")
        
        class GraphQLClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.introspection_enabled = True
            
            def execute_query(self, query):
                return self.make_request("POST", "graphql")
        """;

        Map<String, String> beforeFiles = Map.of("http/clients.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("http/clients.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "RestClient", "HttpClient", "default_headers");
    }

    @Test
    void detectsPullUpAttribute_FileProcessorsToBaseProcessor() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def __init__(self, filepath):
                self.filepath = filepath
            
            def load_file(self):
                return f"Loading {self.filepath}"
        
        class ImageProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.processing_history = []
                self.compression_quality = 85
            
            def resize_image(self, width, height):
                self.processing_history.append(f"Resized to {width}x{height}")
                return f"Image resized to {width}x{height}"
        
        class VideoProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.output_format = "mp4"
            
            def convert_format(self, new_format):
                return f"Video converted to {new_format}"
        """;

        String afterPythonCode = """
        class FileProcessor:
            def __init__(self, filepath):
                self.filepath = filepath
                self.processing_history = []
            
            def load_file(self):
                return f"Loading {self.filepath}"
        
        class ImageProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.compression_quality = 85
            
            def resize_image(self, width, height):
                self.processing_history.append(f"Resized to {width}x{height}")
                return f"Image resized to {width}x{height}"
        
        class VideoProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.output_format = "mp4"
            
            def convert_format(self, new_format):
                self.processing_history.append(f"Converted to {new_format}")
                return f"Video converted to {new_format}"
        """;

        Map<String, String> beforeFiles = Map.of("media/processors.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("media/processors.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "ImageProcessor", "FileProcessor", "processing_history");
    }

    @Test
    void detectsPullUpAttribute_PaymentProcessorsToBaseProcessor() throws Exception {
        String beforePythonCode = """
        class PaymentProcessor:
            def __init__(self, merchant_id):
                self.merchant_id = merchant_id
            
            def process_payment(self, amount):
                return f"Processing ${amount} for merchant {self.merchant_id}"
        
        class CreditCardProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.transaction_fee = 0.029
                self.supported_networks = ["visa", "mastercard"]
            
            def charge_card(self, card_number, amount):
                fee = amount * self.transaction_fee
                return f"Charged ${amount} + ${fee} fee"
        
        class PayPalProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.api_credentials = {}
            
            def process_paypal_payment(self, paypal_id, amount):
                fee = amount * 0.29
                return f"PayPal payment ${amount} + ${fee} fee"
        """;

        String afterPythonCode = """
        class PaymentProcessor:
            def __init__(self, merchant_id):
                self.merchant_id = merchant_id
                self.transaction_fee = 0.029
            
            def process_payment(self, amount):
                return f"Processing ${amount} for merchant {self.merchant_id}"
        
        class CreditCardProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.supported_networks = ["visa", "mastercard"]
            
            def charge_card(self, card_number, amount):
                fee = amount * self.transaction_fee
                return f"Charged ${amount} + ${fee} fee"
        
        class PayPalProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.api_credentials = {}
            
            def process_paypal_payment(self, paypal_id, amount):
                fee = amount * self.transaction_fee
                return f"PayPal payment ${amount} + ${fee} fee"
        """;

        Map<String, String> beforeFiles = Map.of("payments/processors.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("payments/processors.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "CreditCardProcessor", "PaymentProcessor", "transaction_fee");
    }

    @Test
    void detectsPullUpAttribute_CacheImplementationsToBaseCache() throws Exception {
        String beforePythonCode = """
        class CacheManager:
            def __init__(self, name):
                self.name = name
            
            def get(self, key):
                return f"Getting {key} from {self.name}"
        
        class MemoryCache(CacheManager):
            def __init__(self, name):
                super().__init__(name)
                self.max_size = 1000
                self.data = {}
            
            def set(self, key, value):
                if len(self.data) < self.max_size:
                    self.data[key] = value
                return f"Set {key} in memory cache"
        
        class RedisCache(CacheManager):
            def __init__(self, name):
                super().__init__(name)
                self.connection_pool = None
            
            def set(self, key, value):
                return f"Set {key} in Redis (max_size: 1000)"
        """;

        String afterPythonCode = """
        class CacheManager:
            def __init__(self, name):
                self.name = name
                self.max_size = 1000
            
            def get(self, key):
                return f"Getting {key} from {self.name}"
        
        class MemoryCache(CacheManager):
            def __init__(self, name):
                super().__init__(name)
                self.data = {}
            
            def set(self, key, value):
                if len(self.data) < self.max_size:
                    self.data[key] = value
                return f"Set {key} in memory cache"
        
        class RedisCache(CacheManager):
            def __init__(self, name):
                super().__init__(name)
                self.connection_pool = None
            
            def set(self, key, value):
                return f"Set {key} in Redis (max_size: {self.max_size})"
        """;

        Map<String, String> beforeFiles = Map.of("cache/managers.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("cache/managers.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "MemoryCache", "CacheManager", "max_size");
    }

    @Test
    void detectsPullUpAttribute_LoggersToBaseLogger() throws Exception {
        String beforePythonCode = """
        class Logger:
            def __init__(self, name):
                self.name = name
            
            def log(self, level, message):
                return f"[{level}] {self.name}: {message}"
        
        class FileLogger(Logger):
            def __init__(self, name, filename):
                super().__init__(name)
                self.filename = filename
                self.log_level = "INFO"
            
            def write_to_file(self, message):
                if self.log_level in ["INFO", "DEBUG", "ERROR"]:
                    return f"Writing to {self.filename}: {message}"
                return "Log level not enabled"
        
        class ConsoleLogger(Logger):
            def __init__(self, name):
                super().__init__(name)
                self.color_enabled = True
            
            def print_to_console(self, message):
                return f"Console: {message}"
        """;

        String afterPythonCode = """
        class Logger:
            def __init__(self, name):
                self.name = name
                self.log_level = "INFO"
            
            def log(self, level, message):
                return f"[{level}] {self.name}: {message}"
        
        class FileLogger(Logger):
            def __init__(self, name, filename):
                super().__init__(name)
                self.filename = filename
            
            def write_to_file(self, message):
                if self.log_level in ["INFO", "DEBUG", "ERROR"]:
                    return f"Writing to {self.filename}: {message}"
                return "Log level not enabled"
        
        class ConsoleLogger(Logger):
            def __init__(self, name):
                super().__init__(name)
                self.color_enabled = True
            
            def print_to_console(self, message):
                if self.log_level in ["INFO", "DEBUG", "ERROR"]:
                    return f"Console: {message}"
                return "Log level not enabled"
        """;

        Map<String, String> beforeFiles = Map.of("logging/loggers.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("logging/loggers.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "FileLogger", "Logger", "log_level");
    }

    @Test
    void detectsPullUpAttribute_ServersToBaseServer() throws Exception {
        String beforePythonCode = """
        class Server:
            def __init__(self, host, port):
                self.host = host
                self.port = port
            
            def start(self):
                return f"Server starting on {self.host}:{self.port}"
        
        class WebServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.max_connections = 100
                self.document_root = "/var/www"
            
            def handle_request(self, request):
                return f"Handling web request (max_conn: {self.max_connections})"
        
        class FTPServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.passive_mode = True
            
            def handle_ftp_command(self, command):
                return f"FTP command processed (max_conn: 100)"
        """;

        String afterPythonCode = """
        class Server:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.max_connections = 100
            
            def start(self):
                return f"Server starting on {self.host}:{self.port}"
        
        class WebServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.document_root = "/var/www"
            
            def handle_request(self, request):
                return f"Handling web request (max_conn: {self.max_connections})"
        
        class FTPServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.passive_mode = True
            
            def handle_ftp_command(self, command):
                return f"FTP command processed (max_conn: {self.max_connections})"
        """;

        Map<String, String> beforeFiles = Map.of("network/servers.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("network/servers.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "WebServer", "Server", "max_connections");
    }

    @Test
    void detectsPullUpAttribute_DocumentEditorsToBaseEditor() throws Exception {
        String beforePythonCode = """
        class DocumentEditor:
            def __init__(self, filename):
                self.filename = filename
            
            def open_document(self):
                return f"Opening {self.filename}"
        
        class TextEditor(DocumentEditor):
            def __init__(self, filename):
                super().__init__(filename)
                self.undo_stack = []
                self.syntax_highlighting = False
            
            def edit_text(self, text):
                self.undo_stack.append(f"Edit: {text}")
                return f"Text edited, undo stack size: {len(self.undo_stack)}"
        
        class CodeEditor(DocumentEditor):
            def __init__(self, filename):
                super().__init__(filename)
                self.language = "python"
            
            def format_code(self):
                return "Code formatted"
        """;

        String afterPythonCode = """
        class DocumentEditor:
            def __init__(self, filename):
                self.filename = filename
                self.undo_stack = []
            
            def open_document(self):
                return f"Opening {self.filename}"
        
        class TextEditor(DocumentEditor):
            def __init__(self, filename):
                super().__init__(filename)
                self.syntax_highlighting = False
            
            def edit_text(self, text):
                self.undo_stack.append(f"Edit: {text}")
                return f"Text edited, undo stack size: {len(self.undo_stack)}"
        
        class CodeEditor(DocumentEditor):
            def __init__(self, filename):
                super().__init__(filename)
                self.language = "python"
            
            def format_code(self):
                self.undo_stack.append("Format code")
                return f"Code formatted, undo stack size: {len(self.undo_stack)}"
        """;

        Map<String, String> beforeFiles = Map.of("editors/document.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("editors/document.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "TextEditor", "DocumentEditor", "undo_stack");
    }

    @Test
    void detectsPullUpAttribute_GameEntitesToBaseEntity() throws Exception {
        String beforePythonCode = """
        class GameEntity:
            def __init__(self, name):
                self.name = name
            
            def update(self):
                return f"Updating {self.name}"
        
        class Player(GameEntity):
            def __init__(self, name):
                super().__init__(name)
                self.health = 100
                self.position = {"x": 0, "y": 0}
            
            def move(self, x, y):
                self.position["x"] += x
                self.position["y"] += y
                return f"Player moved to {self.position}"
        
        class Enemy(GameEntity):
            def __init__(self, name):
                super().__init__(name)
                self.ai_state = "idle"
            
            def attack(self):
                return "Enemy is defeated"
        """;

        String afterPythonCode = """
        class GameEntity:
            def __init__(self, name):
                self.name = name
                self.health = 100
            
            def update(self):
                return f"Updating {self.name}"
        
        class Player(GameEntity):
            def __init__(self, name):
                super().__init__(name)
                self.position = {"x": 0, "y": 0}
            
            def move(self, x, y):
                self.position["x"] += x
                self.position["y"] += y
                return f"Player moved to {self.position}"
        
        class Enemy(GameEntity):
            def __init__(self, name):
                super().__init__(name)
                self.ai_state = "idle"
            
            def attack(self):
                if self.health > 0:
                    return f"Enemy {self.name} attacks!"
                return "Enemy is defeated"
        """;

        Map<String, String> beforeFiles = Map.of("game/entities.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("game/entities.py", afterPythonCode);

        assertPullUpAttributeRefactoringDetected(beforeFiles, afterFiles, "Player", "GameEntity", "health");
    }
}
