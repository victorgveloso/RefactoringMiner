package org.refactoringminer.test.python.refactorings.module;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.refactoringminer.utils.RefactoringAssertUtils.assertMoveSourceFolderRefactoringDetected;

class MoveSourceFolderRefactoringDetectionTest {

    @Test
    void detectsMoveSourceFolder() throws Exception {
        // BEFORE: Everything under "src_old"
        String beforePythonCode = """
            # src_old/mypkg/my_module.py
            class A:
                pass
            """;

        Map<String, String> beforeFiles = Map.of("src_old/mypkg/my_module.py", beforePythonCode);

        // AFTER: Everything under "src_new"
        String afterPythonCode = """
            # src_new/mypkg/my_module.py
            class A:
                pass
            """;

        Map<String, String> afterFiles = Map.of("src_new/mypkg/my_module.py", afterPythonCode);

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "src_old", "src_new");
    }


    @Test
    void detectsMoveSourceFolder_ProjectRestructure() throws Exception {
        String mainModuleCode = """
        class MainApplication:
            def __init__(self):
                self.name = "MyApp"
                self.version = "1.0.0"
            
            def start(self):
                print(f"Starting {self.name} v{self.version}")
        """;

        String utilsCode = """
        def format_timestamp(timestamp):
            return timestamp.strftime('%Y-%m-%d %H:%M:%S')
        
        def sanitize_filename(filename):
            import re
            return re.sub(r'[<>:"/\\|?*]', '_', filename)
        """;

        Map<String, String> beforeFiles = Map.of(
                "legacy_src/app/main.py", mainModuleCode,
                "legacy_src/app/utils.py", utilsCode
        );

        Map<String, String> afterFiles = Map.of(
                "modern_src/app/main.py", mainModuleCode,
                "modern_src/app/utils.py", utilsCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "legacy_src", "modern_src");
    }

    @Test
    void detectsMoveSourceFolder_LibraryToApplication() throws Exception {
        String coreCode = """
        class DataProcessor:
            def __init__(self):
                self.cache = {}
            
            def process(self, data):
                key = hash(str(data))
                if key not in self.cache:
                    self.cache[key] = [item.upper() for item in data]
                return self.cache[key]
        """;

        String apiCode = """
        from core.processor import DataProcessor
        
        class APIHandler:
            def __init__(self):
                self.processor = DataProcessor()
            
            def handle_request(self, request_data):
                return self.processor.process(request_data)
        """;

        Map<String, String> beforeFiles = Map.of(
                "lib/core/processor.py", coreCode,
                "lib/api/handler.py", apiCode
        );

        Map<String, String> afterFiles = Map.of(
                "app/core/processor.py", coreCode,
                "app/api/handler.py", apiCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "lib", "app");
    }

    @Test
    void detectsMoveSourceFolder_DevelopmentToProduction() throws Exception {
        String configCode = """
        class Configuration:
            def __init__(self):
                self.database_url = "sqlite:///development.db"
                self.debug = True
                self.secret_key = "dev-secret"
            
            def get_config(self):
                return {
                    'database_url': self.database_url,
                    'debug': self.debug,
                    'secret_key': self.secret_key
                }
        """;

        String serverCode = """
        from config.settings import Configuration
        
        class WebServer:
            def __init__(self):
                self.config = Configuration()
            
            def run(self):
                print(f"Server running with debug={self.config.debug}")
        """;

        Map<String, String> beforeFiles = Map.of(
                "dev_source/config/settings.py", configCode,
                "dev_source/server/app.py", serverCode
        );

        Map<String, String> afterFiles = Map.of(
                "prod_source/config/settings.py", configCode,
                "prod_source/server/app.py", serverCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "dev_source", "prod_source");
    }

    @Test
    void detectsMoveSourceFolder_PrototypeToMain() throws Exception {
        String modelCode = """
        class UserModel:
            def __init__(self, username, email):
                self.username = username
                self.email = email
                self.active = True
            
            def deactivate(self):
                self.active = False
            
            def to_dict(self):
                return {
                    'username': self.username,
                    'email': self.email,
                    'active': self.active
                }
        """;

        String serviceCode = """
        from models.user import UserModel
        
        class UserService:
            def __init__(self):
                self.users = []
            
            def create_user(self, username, email):
                user = UserModel(username, email)
                self.users.append(user)
                return user
            
            def get_active_users(self):
                return [user for user in self.users if user.active]
        """;

        Map<String, String> beforeFiles = Map.of(
                "prototype/models/user.py", modelCode,
                "prototype/services/user_service.py", serviceCode
        );

        Map<String, String> afterFiles = Map.of(
                "main/models/user.py", modelCode,
                "main/services/user_service.py", serviceCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "prototype", "main");
    }

    @Test
    void detectsMoveSourceFolder_ExperimentalToStable() throws Exception {
        String algorithmCode = """
        class SortingAlgorithm:
            @staticmethod
            def bubble_sort(arr):
                n = len(arr)
                for i in range(n):
                    for j in range(0, n - i - 1):
                        if arr[j] > arr[j + 1]:
                            arr[j], arr[j + 1] = arr[j + 1], arr[j]
                return arr
            
            @staticmethod
            def quick_sort(arr):
                if len(arr) <= 1:
                    return arr
                pivot = arr[len(arr) // 2]
                left = [x for x in arr if x < pivot]
                middle = [x for x in arr if x == pivot]
                right = [x for x in arr if x > pivot]
                return SortingAlgorithm.quick_sort(left) + middle + SortingAlgorithm.quick_sort(right)
        """;

        String benchmarkCode = """
        import time
        from algorithms.sorting import SortingAlgorithm
        
        class PerformanceBenchmark:
            def __init__(self):
                self.sorter = SortingAlgorithm()
            
            def benchmark_sort(self, data, algorithm='quick_sort'):
                start_time = time.time()
                if algorithm == 'bubble_sort':
                    result = self.sorter.bubble_sort(data.copy())
                else:
                    result = self.sorter.quick_sort(data.copy())
                end_time = time.time()
                return result, end_time - start_time
        """;

        Map<String, String> beforeFiles = Map.of(
                "experimental/algorithms/sorting.py", algorithmCode,
                "experimental/benchmarks/performance.py", benchmarkCode
        );

        Map<String, String> afterFiles = Map.of(
                "stable/algorithms/sorting.py", algorithmCode,
                "stable/benchmarks/performance.py", benchmarkCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "experimental", "stable");
    }

    @Test
    void detectsMoveSourceFolder_TempToFinal() throws Exception {
        String parserCode = """
        import json
        import xml.etree.ElementTree as ET
        
        class DataParser:
            def parse_json(self, json_string):
                try:
                    return json.loads(json_string)
                except json.JSONDecodeError:
                    return None
            
            def parse_xml(self, xml_string):
                try:
                    root = ET.fromstring(xml_string)
                    return self._xml_to_dict(root)
                except ET.ParseError:
                    return None
            
            def _xml_to_dict(self, element):
                result = {}
                for child in element:
                    result[child.tag] = child.text
                return result
        """;

        String validatorCode = """
        from parsers.data_parser import DataParser
        
        class DataValidator:
            def __init__(self):
                self.parser = DataParser()
            
            def validate_json_structure(self, json_string, required_fields):
                data = self.parser.parse_json(json_string)
                if not data:
                    return False
                return all(field in data for field in required_fields)
        """;

        Map<String, String> beforeFiles = Map.of(
                "temp_code/parsers/data_parser.py", parserCode,
                "temp_code/validators/data_validator.py", validatorCode
        );

        Map<String, String> afterFiles = Map.of(
                "final_code/parsers/data_parser.py", parserCode,
                "final_code/validators/data_validator.py", validatorCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "temp_code", "final_code");
    }

    @Test
    void detectsMoveSourceFolder_BackupToActive() throws Exception {
        String databaseCode = """
        import sqlite3
        
        class DatabaseManager:
            def __init__(self, db_path):
                self.db_path = db_path
                self.connection = None
            
            def connect(self):
                self.connection = sqlite3.connect(self.db_path)
                return self.connection
            
            def execute_query(self, query, params=None):
                if not self.connection:
                    self.connect()
                cursor = self.connection.cursor()
                if params:
                    cursor.execute(query, params)
                else:
                    cursor.execute(query)
                return cursor.fetchall()
            
            def close(self):
                if self.connection:
                    self.connection.close()
        """;

        String repositoryCode = """
        from database.manager import DatabaseManager
        
        class UserRepository:
            def __init__(self, db_path):
                self.db_manager = DatabaseManager(db_path)
            
            def create_user(self, username, email):
                query = "INSERT INTO users (username, email) VALUES (?, ?)"
                return self.db_manager.execute_query(query, (username, email))
            
            def find_user_by_email(self, email):
                query = "SELECT * FROM users WHERE email = ?"
                return self.db_manager.execute_query(query, (email,))
        """;

        Map<String, String> beforeFiles = Map.of(
                "backup_src/database/manager.py", databaseCode,
                "backup_src/repositories/user_repository.py", repositoryCode
        );

        Map<String, String> afterFiles = Map.of(
                "active_src/database/manager.py", databaseCode,
                "active_src/repositories/user_repository.py", repositoryCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "backup_src", "active_src");
    }

    @Test
    void detectsMoveSourceFolder_OldVersionToNewVersion() throws Exception {
        String apiClientCode = """
        import requests
        
        class APIClient:
            def __init__(self, base_url, api_key):
                self.base_url = base_url.rstrip('/')
                self.api_key = api_key
                self.headers = {
                    'Authorization': f'Bearer {api_key}',
                    'Content-Type': 'application/json'
                }
            
            def get(self, endpoint):
                url = f"{self.base_url}/{endpoint.lstrip('/')}"
                response = requests.get(url, headers=self.headers)
                return response.json() if response.status_code == 200 else None
            
            def post(self, endpoint, data):
                url = f"{self.base_url}/{endpoint.lstrip('/')}"
                response = requests.post(url, json=data, headers=self.headers)
                return response.json() if response.status_code in [200, 201] else None
        """;

        String cacheCode = """
        import time
        
        class SimpleCache:
            def __init__(self, ttl=300):  # 5 minutes default TTL
                self.cache = {}
                self.ttl = ttl
            
            def get(self, key):
                if key in self.cache:
                    value, timestamp = self.cache[key]
                    if time.time() - timestamp < self.ttl:
                        return value
                    else:
                        del self.cache[key]
                return None
            
            def set(self, key, value):
                self.cache[key] = (value, time.time())
            
            def clear(self):
                self.cache.clear()
        """;

        Map<String, String> beforeFiles = Map.of(
                "v1_source/client/api_client.py", apiClientCode,
                "v1_source/utils/cache.py", cacheCode
        );

        Map<String, String> afterFiles = Map.of(
                "v2_source/client/api_client.py", apiClientCode,
                "v2_source/utils/cache.py", cacheCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "v1_source", "v2_source");
    }

    @Test
    void detectsMoveSourceFolder_WorkingToRelease() throws Exception {
        String loggerCode = """
        import logging
        import sys
        from datetime import datetime
        
        class CustomLogger:
            def __init__(self, name, level=logging.INFO):
                self.logger = logging.getLogger(name)
                self.logger.setLevel(level)
                
                if not self.logger.handlers:
                    handler = logging.StreamHandler(sys.stdout)
                    formatter = logging.Formatter(
                        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
                    )
                    handler.setFormatter(formatter)
                    self.logger.addHandler(handler)
            
            def info(self, message):
                self.logger.info(f"[{datetime.now().strftime('%H:%M:%S')}] {message}")
            
            def error(self, message):
                self.logger.error(f"[{datetime.now().strftime('%H:%M:%S')}] ERROR: {message}")
            
            def debug(self, message):
                self.logger.debug(f"[{datetime.now().strftime('%H:%M:%S')}] DEBUG: {message}")
        """;

        String monitorCode = """
        from logging.custom_logger import CustomLogger
        import psutil
        import time
        
        class SystemMonitor:
            def __init__(self):
                self.logger = CustomLogger("SystemMonitor")
            
            def monitor_system(self, duration=60):
                self.logger.info(f"Starting system monitoring for {duration} seconds")
                start_time = time.time()
                
                while time.time() - start_time < duration:
                    cpu_percent = psutil.cpu_percent(interval=1)
                    memory_percent = psutil.virtual_memory().percent
                    
                    self.logger.info(f"CPU: {cpu_percent}%, Memory: {memory_percent}%")
                    
                    if cpu_percent > 80:
                        self.logger.error(f"High CPU usage detected: {cpu_percent}%")
                    
                    time.sleep(5)
                
                self.logger.info("System monitoring completed")
        """;

        Map<String, String> beforeFiles = Map.of(
                "working_copy/logging/custom_logger.py", loggerCode,
                "working_copy/monitoring/system_monitor.py", monitorCode
        );

        Map<String, String> afterFiles = Map.of(
                "release_build/logging/custom_logger.py", loggerCode,
                "release_build/monitoring/system_monitor.py", monitorCode
        );

        assertMoveSourceFolderRefactoringDetected(beforeFiles, afterFiles, "working_copy", "release_build");
    }
}