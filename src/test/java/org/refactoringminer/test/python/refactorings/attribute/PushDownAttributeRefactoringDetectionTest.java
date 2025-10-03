package org.refactoringminer.test.python.refactorings.attribute;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertPushDownAttributeRefactoringDetected;

public class PushDownAttributeRefactoringDetectionTest {

    @Test
    void detectsPushDownAttribute() throws Exception {

        String beforePythonCode = """
        class Animal:
            def __init__(self):
                self.tail = True
        
        class Dog(Animal):
            pass
        
        class Cat(Animal):
            pass
        """;
                String afterPythonCode = """
        class Animal:
            def __init__(self):
                pass
        
        class Dog(Animal):
            def __init__(self):
                self.tail = True
        
        class Cat(Animal):
            def __init__(self):
                pass
        """;

        Map<String, String> beforeFiles = Map.of("tests/animal.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/animal.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "Animal", "Dog", "tail");
    }


    @Test
    void detectsPushDownAttribute_DatabaseConnectionToPostgresConnection() throws Exception {
        String beforePythonCode = """
        class DatabaseConnection:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.schema_version = "latest"
                self.connection_pool = None
            
            def connect(self):
                return f"Connecting to {self.host}:{self.port}"
            
            def execute_query(self, query):
                return f"Executing: {query}"
        
        class MySQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
            
            def show_tables(self):
                return "SHOW TABLES"
        
        class PostgresConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
            
            def analyze_performance(self):
                return f"EXPLAIN ANALYZE using schema {self.schema_version}"
        """;

        String afterPythonCode = """
        class DatabaseConnection:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.connection_pool = None
            
            def connect(self):
                return f"Connecting to {self.host}:{self.port}"
            
            def execute_query(self, query):
                return f"Executing: {query}"
        
        class MySQLConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
            
            def show_tables(self):
                return "SHOW TABLES"
        
        class PostgresConnection(DatabaseConnection):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.schema_version = "latest"
            
            def analyze_performance(self):
                return f"EXPLAIN ANALYZE using schema {self.schema_version}"
        """;

        Map<String, String> beforeFiles = Map.of("database/connection.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database/connection.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "DatabaseConnection", "PostgresConnection", "schema_version");
    }

    @Test
    void detectsPushDownAttribute_MediaPlayerToVideoPlayer() throws Exception {
        String beforePythonCode = """
        class MediaPlayer:
            def __init__(self, filename):
                self.filename = filename
                self.is_playing = False
                self.subtitle_tracks = []
                self.current_position = 0
            
            def play(self):
                self.is_playing = True
                return f"Playing {self.filename}"
            
            def pause(self):
                self.is_playing = False
                return "Paused"
            
            def get_duration(self):
                return "Unknown duration"
        
        class AudioPlayer(MediaPlayer):
            def __init__(self, filename):
                super().__init__(filename)
                self.equalizer_settings = {}
            
            def adjust_volume(self, level):
                return f"Volume set to {level}"
        
        class VideoPlayer(MediaPlayer):
            def __init__(self, filename):
                super().__init__(filename)
                self.resolution = "1080p"
            
            def toggle_subtitles(self):
                if self.subtitle_tracks:
                    return "Subtitles enabled"
                return "No subtitles available"
        """;

        String afterPythonCode = """
        class MediaPlayer:
            def __init__(self, filename):
                self.filename = filename
                self.is_playing = False
                self.current_position = 0
            
            def play(self):
                self.is_playing = True
                return f"Playing {self.filename}"
            
            def pause(self):
                self.is_playing = False
                return "Paused"
            
            def get_duration(self):
                return "Unknown duration"
        
        class AudioPlayer(MediaPlayer):
            def __init__(self, filename):
                super().__init__(filename)
                self.equalizer_settings = {}
            
            def adjust_volume(self, level):
                return f"Volume set to {level}"
        
        class VideoPlayer(MediaPlayer):
            def __init__(self, filename):
                super().__init__(filename)
                self.resolution = "1080p"
                self.subtitle_tracks = []
            
            def toggle_subtitles(self):
                if self.subtitle_tracks:
                    return "Subtitles enabled"
                return "No subtitles available"
        """;

        Map<String, String> beforeFiles = Map.of("media/player.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("media/player.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "MediaPlayer", "VideoPlayer", "subtitle_tracks");
    }

    @Test
    void detectsPushDownAttribute_HttpClientToGraphQLClient() throws Exception {
        String beforePythonCode = """
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
                self.headers = {}
                self.query_cache = {}
                self.timeout = 30
            
            def set_header(self, key, value):
                self.headers[key] = value
            
            def make_request(self, method, endpoint):
                return f"{method} {self.base_url}/{endpoint}"
            
            def clear_cache(self):
                self.query_cache.clear()
        
        class RestClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.api_version = "v1"
            
            def get_resource(self, resource_id):
                return self.make_request("GET", f"api/{self.api_version}/resource/{resource_id}")
        
        class GraphQLClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.introspection_enabled = True
            
            def execute_query(self, query):
                cached_result = self.query_cache.get(query)
                if cached_result:
                    return cached_result
                result = self.make_request("POST", "graphql")
                self.query_cache[query] = result
                return result
        """;

        String afterPythonCode = """
        class HttpClient:
            def __init__(self, base_url):
                self.base_url = base_url
                self.headers = {}
                self.timeout = 30
            
            def set_header(self, key, value):
                self.headers[key] = value
            
            def make_request(self, method, endpoint):
                return f"{method} {self.base_url}/{endpoint}"
                
            def clear_cache(self):
                self.query_cache.clear()
                
        class RestClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.api_version = "v1"
            
            def get_resource(self, resource_id):
                return self.make_request("GET", f"api/{self.api_version}/resource/{resource_id}")
        
        class GraphQLClient(HttpClient):
            def __init__(self, base_url):
                super().__init__(base_url)
                self.introspection_enabled = True
                self.query_cache = {}
            
            def execute_query(self, query):
                cached_result = self.query_cache.get(query)
                if cached_result:
                    return cached_result
                result = self.make_request("POST", "graphql")
                self.query_cache[query] = result
                return result   
        """;

        Map<String, String> beforeFiles = Map.of("http/client.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("http/client.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "HttpClient", "GraphQLClient", "query_cache");
    }

    @Test
    void detectsPushDownAttribute_FileProcessorToImageProcessor() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def __init__(self, filepath):
                self.filepath = filepath
                self.metadata = {}
                self.color_profile = "sRGB"
                self.processing_history = []
            
            def load_file(self):
                return f"Loading {self.filepath}"
            
            def get_file_size(self):
                return "File size: 1MB"
            
            def add_to_history(self, operation):
                self.processing_history.append(operation)
        
        class TextProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.encoding = "utf-8"
            
            def count_words(self):
                return "Word count: 1000"
        
        class ImageProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.compression_quality = 85
            
            def adjust_colors(self):
                self.add_to_history(f"Color adjustment using {self.color_profile}")
                return f"Colors adjusted with profile {self.color_profile}"
        """;

        String afterPythonCode = """
        class FileProcessor:
            def __init__(self, filepath):
                self.filepath = filepath
                self.metadata = {}
                self.processing_history = []
            
            def load_file(self):
                return f"Loading {self.filepath}"
            
            def get_file_size(self):
                return "File size: 1MB"
            
            def add_to_history(self, operation):
                self.processing_history.append(operation)
        
        class TextProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.encoding = "utf-8"
            
            def count_words(self):
                return "Word count: 1000"
        
        class ImageProcessor(FileProcessor):
            def __init__(self, filepath):
                super().__init__(filepath)
                self.compression_quality = 85
                self.color_profile = "sRGB"
            
            def adjust_colors(self):
                self.add_to_history(f"Color adjustment using {self.color_profile}")
                return f"Colors adjusted with profile {self.color_profile}"
        """;

        Map<String, String> beforeFiles = Map.of("processing/file.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processing/file.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "FileProcessor", "ImageProcessor", "color_profile");
    }

    @Test
    void detectsPushDownAttribute_PaymentProcessorToCreditCardProcessor() throws Exception {
        String beforePythonCode = """
        class PaymentProcessor:
            def __init__(self, merchant_id):
                self.merchant_id = merchant_id
                self.transaction_fees = {}
                self.fraud_detection_rules = []
                self.chargeback_protection = True
            
            def process_payment(self, amount):
                return f"Processing ${amount} for merchant {self.merchant_id}"
            
            def calculate_fee(self, amount):
                return amount * 0.029
            
            def validate_transaction(self, transaction):
                for rule in self.fraud_detection_rules:
                    if not rule.validate(transaction):
                        return False
                return True
        
        class BankTransferProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.routing_number = None
            
            def verify_account(self, account_number):
                return f"Verifying account {account_number}"
        
        class CreditCardProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.supported_cards = ["visa", "mastercard", "amex"]
            
            def handle_chargeback(self, transaction_id):
                if self.chargeback_protection:
                    return f"Chargeback protection applied for {transaction_id}"
                return "No chargeback protection available"
        """;

        String afterPythonCode = """
        class PaymentProcessor:
            def __init__(self, merchant_id):
                self.merchant_id = merchant_id
                self.transaction_fees = {}
                self.fraud_detection_rules = []
            
            def process_payment(self, amount):
                return f"Processing ${amount} for merchant {self.merchant_id}"
            
            def calculate_fee(self, amount):
                return amount * 0.029
            
            def validate_transaction(self, transaction):
                for rule in self.fraud_detection_rules:
                    if not rule.validate(transaction):
                        return False
                return True
        
        class BankTransferProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.routing_number = None
            
            def verify_account(self, account_number):
                return f"Verifying account {account_number}"
        
        class CreditCardProcessor(PaymentProcessor):
            def __init__(self, merchant_id):
                super().__init__(merchant_id)
                self.supported_cards = ["visa", "mastercard", "amex"]
                self.chargeback_protection = True
            
            def handle_chargeback(self, transaction_id):
                if self.chargeback_protection:
                    return f"Chargeback protection applied for {transaction_id}"
                return "No chargeback protection available"
        """;

        Map<String, String> beforeFiles = Map.of("payments/processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("payments/processor.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "PaymentProcessor", "CreditCardProcessor", "chargeback_protection");
    }

    @Test
    void detectsPushDownAttribute_CacheManagerToRedisCache() throws Exception {
        String beforePythonCode = """
        class CacheManager:
            def __init__(self, config):
                self.config = config
                self.stats = {}
                self.cluster_nodes = []
                self.max_memory = "1GB"
            
            def get(self, key):
                return f"Getting value for {key}"
            
            def set(self, key, value, ttl=3600):
                return f"Setting {key} = {value} (TTL: {ttl})"
            
            def get_statistics(self):
                return self.stats
        
        class MemoryCache(CacheManager):
            def __init__(self, config):
                super().__init__(config)
                self.data = {}
            
            def clear_all(self):
                self.data.clear()
                return "Memory cache cleared"
        
        class RedisCache(CacheManager):
            def __init__(self, config):
                super().__init__(config)
                self.connection_pool = None
            
            def setup_cluster(self):
                if self.cluster_nodes:
                    return f"Setting up Redis cluster with {len(self.cluster_nodes)} nodes"
                return "Single Redis instance"
        """;

        String afterPythonCode = """
        class CacheManager:
            def __init__(self, config):
                self.config = config
                self.stats = {}
                self.max_memory = "1GB"
            
            def get(self, key):
                return f"Getting value for {key}"
            
            def set(self, key, value, ttl=3600):
                return f"Setting {key} = {value} (TTL: {ttl})"
            
            def get_statistics(self):
                return self.stats
        
        class MemoryCache(CacheManager):
            def __init__(self, config):
                super().__init__(config)
                self.data = {}
            
            def clear_all(self):
                self.data.clear()
                return "Memory cache cleared"
        
        class RedisCache(CacheManager):
            def __init__(self, config):
                super().__init__(config)
                self.connection_pool = None
                self.cluster_nodes = []
            
            def setup_cluster(self):
                if self.cluster_nodes:
                    return f"Setting up Redis cluster with {len(self.cluster_nodes)} nodes"
                return "Single Redis instance"
        """;

        Map<String, String> beforeFiles = Map.of("cache/manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("cache/manager.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "CacheManager", "RedisCache", "cluster_nodes");
    }

    @Test
    void detectsPushDownAttribute_ServerToWebServer() throws Exception {
        String beforePythonCode = """
        class Server:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.is_running = False
                self.request_handlers = {}
                self.ssl_context = None
            
            def start(self):
                self.is_running = True
                return f"Server started on {self.host}:{self.port}"
            
            def stop(self):
                self.is_running = False
                return "Server stopped"
            
            def register_handler(self, path, handler):
                self.request_handlers[path] = handler
        
        class FTPServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.passive_mode = True
            
            def list_files(self, directory):
                return f"Listing files in {directory}"
        
        class WebServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.document_root = "/var/www"
            
            def enable_https(self):
                if self.ssl_context:
                    return "HTTPS enabled"
                return "SSL context not configured"
        """;

        String afterPythonCode = """
        class Server:
            def __init__(self, host, port):
                self.host = host
                self.port = port
                self.is_running = False
                self.request_handlers = {}
            
            def start(self):
                self.is_running = True
                return f"Server started on {self.host}:{self.port}"
            
            def stop(self):
                self.is_running = False
                return "Server stopped"
            
            def register_handler(self, path, handler):
                self.request_handlers[path] = handler
        
        class FTPServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.passive_mode = True
            
            def list_files(self, directory):
                return f"Listing files in {directory}"
        
        class WebServer(Server):
            def __init__(self, host, port):
                super().__init__(host, port)
                self.document_root = "/var/www"
                self.ssl_context = None
            
            def enable_https(self):
                if self.ssl_context:
                    return "HTTPS enabled"
                return "SSL context not configured"
        """;

        Map<String, String> beforeFiles = Map.of("network/server.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("network/server.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "Server", "WebServer", "ssl_context");
    }

    @Test
    void detectsPushDownAttribute_LoggerToStructuredLogger() throws Exception {
        String beforePythonCode = """
        class Logger:
            def __init__(self, name):
                self.name = name
                self.level = "INFO"
                self.formatters = []
                self.structured_format = None
            
            def log(self, level, message):
                return f"[{level}] {self.name}: {message}"
            
            def set_level(self, level):
                self.level = level
            
            def add_formatter(self, formatter):
                self.formatters.append(formatter)
        
        class FileLogger(Logger):
            def __init__(self, name, filename):
                super().__init__(name)
                self.filename = filename
            
            def write_to_file(self, message):
                return f"Writing to {self.filename}: {message}"
        
        class StructuredLogger(Logger):
            def __init__(self, name):
                super().__init__(name)
                self.output_format = "json"
            
            def log_structured(self, level, data):
                if self.structured_format:
                    return f"Structured log [{level}]: {self.structured_format.format(data)}"
                return f"Structured log [{level}]: {data}"
        """;

        String afterPythonCode = """
        class Logger:
            def __init__(self, name):
                self.name = name
                self.level = "INFO"
                self.formatters = []
            
            def log(self, level, message):
                return f"[{level}] {self.name}: {message}"
            
            def set_level(self, level):
                self.level = level
            
            def add_formatter(self, formatter):
                self.formatters.append(formatter)
        
        class FileLogger(Logger):
            def __init__(self, name, filename):
                super().__init__(name)
                self.filename = filename
            
            def write_to_file(self, message):
                return f"Writing to {self.filename}: {message}"
        
        class StructuredLogger(Logger):
            def __init__(self, name):
                super().__init__(name)
                self.output_format = "json"
                self.structured_format = None
            
            def log_structured(self, level, data):
                if self.structured_format:
                    return f"Structured log [{level}]: {self.structured_format.format(data)}"
                return f"Structured log [{level}]: {data}"
        """;

        Map<String, String> beforeFiles = Map.of("logging/logger.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("logging/logger.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "Logger", "StructuredLogger", "structured_format");
    }

    @Test
    void detectsPushDownAttribute_DocumentProcessorToSpreadsheetProcessor() throws Exception {
        String beforePythonCode = """
        class DocumentProcessor:
            def __init__(self, filename):
                self.filename = filename
                self.metadata = {}
                self.worksheet_names = []
                self.current_sheet = 0
            
            def open_document(self):
                return f"Opening document {self.filename}"
            
            def extract_metadata(self):
                return self.metadata
            
            def close_document(self):
                return "Document closed"
        
        class WordProcessor(DocumentProcessor):
            def __init__(self, filename):
                super().__init__(filename)
                self.styles = {}
            
            def extract_text(self):
                return "Document text content"
        
        class SpreadsheetProcessor(DocumentProcessor):
            def __init__(self, filename):
                super().__init__(filename)
                self.formulas = []
            
            def switch_worksheet(self, sheet_name):
                if sheet_name in self.worksheet_names:
                    self.current_sheet = self.worksheet_names.index(sheet_name)
                    return f"Switched to worksheet: {sheet_name}"
                return "Worksheet not found"
        """;

        String afterPythonCode = """
        class DocumentProcessor:
            def __init__(self, filename):
                self.filename = filename
                self.metadata = {}
                self.current_sheet = 0

            def open_document(self):
                return f"Opening document {self.filename}"
            
            def extract_metadata(self):
                return self.metadata
            
            def close_document(self):
                return "Document closed"
        
        class WordProcessor(DocumentProcessor):
            def __init__(self, filename):
                super().__init__(filename)
                self.styles = {}
            
            def extract_text(self):
                return "Document text content"
        
        class SpreadsheetProcessor(DocumentProcessor):
            def __init__(self, filename):
                super().__init__(filename)
                self.formulas = []
                self.worksheet_names = []
            
            def switch_worksheet(self, sheet_name):
                if sheet_name in self.worksheet_names:
                    return f"Switched to worksheet: {sheet_name}"
                return "Worksheet not found"
        """;

        Map<String, String> beforeFiles = Map.of("office/processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("office/processor.py", afterPythonCode);

        assertPushDownAttributeRefactoringDetected(beforeFiles, afterFiles, "DocumentProcessor", "SpreadsheetProcessor", "worksheet_names");
    }
}
