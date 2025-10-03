package org.refactoringminer.test.python.refactorings.clazz;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RemoveClassModifierRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveClassModifierRefactoringDetectionTest {

    @Test
    void detectsRemoveAbstractModifier_SimpleShape() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class Shape(ABC):
                @abstractmethod
                def area(self):
                    pass
                
                @abstractmethod 
                def perimeter(self):
                    pass
            """;

        String afterPythonCode = """
            class Shape:
                def area(self):
                    return 0
                
                def perimeter(self):
                    return 0
            """;

        Map<String, String> beforeFiles = Map.of("shape.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("shape.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "Shape", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_DataProcessor() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class DataProcessor(ABC):
                def __init__(self, config):
                    self.config = config
                    self.results = []
                
                def validate_input(self, data):
                    if not isinstance(data, (list, dict)):
                        return False
                    return True
                
                @abstractmethod
                def process_data(self, raw_data):
                    if not self.validate_input(raw_data):
                        raise ValueError("Invalid input data")
                    
                    processed = []
                    for item in raw_data:
                        if isinstance(item, dict):
                            transformed = {}
                            for key, value in item.items():
                                if isinstance(value, (int, float)):
                                    if value > 0:
                                        transformed[f"positive_{key}"] = value * 2
                                    elif value < 0:
                                        transformed[f"negative_{key}"] = abs(value)
                                    else:
                                        transformed[f"zero_{key}"] = 0
                                elif isinstance(value, str):
                                    transformed[f"string_{key}"] = value.upper()
                            processed.append(transformed)
                    
                    self.results.extend(processed)
                    return processed
                
                def get_results(self):
                    return self.results.copy()
            """;

        String afterPythonCode = """
            class DataProcessor:
                def __init__(self, config):
                    self.config = config
                    self.results = []
                
                def validate_input(self, data):
                    if not isinstance(data, (list, dict)):
                        return False
                    return True
                
                def process_data(self, raw_data):
                    if not self.validate_input(raw_data):
                        raise ValueError("Invalid input data")
                    
                    processed = []
                    for item in raw_data:
                        if isinstance(item, dict):
                            transformed = {}
                            for key, value in item.items():
                                if isinstance(value, (int, float)):
                                    if value > 0:
                                        transformed[f"positive_{key}"] = value * 2
                                    elif value < 0:
                                        transformed[f"negative_{key}"] = abs(value)
                                    else:
                                        transformed[f"zero_{key}"] = 0
                                elif isinstance(value, str):
                                    transformed[f"string_{key}"] = value.upper()
                            processed.append(transformed)
                    
                    self.results.extend(processed)
                    return processed
                
                def get_results(self):
                    return self.results.copy()
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "DataProcessor", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_AnimalHierarchy() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class Animal(ABC):
                def __init__(self, name):
                    self.name = name
                
                @abstractmethod
                def speak(self):
                    return f"{self.name} makes a sound"
                
                @abstractmethod
                def move(self):
                    return f"{self.name} moves"
                
                def eat(self, food):
                    nutrition_values = {
                        'grass': {'energy': 10, 'protein': 2},
                        'meat': {'energy': 20, 'protein': 15},
                        'fruits': {'energy': 15, 'protein': 1}
                    }
                    
                    if food in nutrition_values:
                        nutrients = nutrition_values[food]
                        energy_gained = nutrients['energy']
                        protein_gained = nutrients['protein']
                        
                        return {
                            'message': f"{self.name} ate {food}",
                            'energy': energy_gained,
                            'protein': protein_gained
                        }
                    else:
                        return {
                            'message': f"{self.name} couldn't eat {food}",
                            'energy': 0,
                            'protein': 0
                        }
            """;

        String afterPythonCode = """
            class Animal:
                def __init__(self, name):
                    self.name = name
                
                def speak(self):
                    return f"{self.name} makes a sound"
                
                def move(self):
                    return f"{self.name} moves"
                
                def eat(self, food):
                    nutrition_values = {
                        'grass': {'energy': 10, 'protein': 2},
                        'meat': {'energy': 20, 'protein': 15},
                        'fruits': {'energy': 15, 'protein': 1}
                    }
                    
                    if food in nutrition_values:
                        nutrients = nutrition_values[food]
                        energy_gained = nutrients['energy']
                        protein_gained = nutrients['protein']
                        
                        return {
                            'message': f"{self.name} ate {food}",
                            'energy': energy_gained,
                            'protein': protein_gained
                        }
                    else:
                        return {
                            'message': f"{self.name} couldn't eat {food}",
                            'energy': 0,
                            'protein': 0
                        }
            """;

        Map<String, String> beforeFiles = Map.of("animal.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("animal.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "Animal", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_DatabaseConnection() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class DatabaseConnection(ABC):
                def __init__(self, host, port, database):
                    self.host = host
                    self.port = port
                    self.database = database
                    self.connection = None
                
                @abstractmethod
                def connect(self):
                    connection_string = f"postgresql://{self.host}:{self.port}/{self.database}"
                    try:
                        self.connection = connection_string
                        return True
                    except Exception as e:
                        return False
                
                @abstractmethod
                def execute_query(self, query, params=None):
                    if not self.connection:
                        raise ConnectionError("Not connected to database")
                    
                    results = []
                    if query.lower().startswith('select'):
                        # Simulate SELECT query results
                        for i in range(3):
                            row_data = {'id': i + 1, 'value': f"sample_data_{i}"}
                            if params:
                                for key, value in params.items():
                                    row_data[f"param_{key}"] = value
                            results.append(row_data)
                    
                    return results
                
                def close(self):
                    if self.connection:
                        self.connection = None
                        return True
                    return False
            """;

        String afterPythonCode = """
            class DatabaseConnection:
                def __init__(self, host, port, database):
                    self.host = host
                    self.port = port
                    self.database = database
                    self.connection = None
                
                def connect(self):
                    connection_string = f"postgresql://{self.host}:{self.port}/{self.database}"
                    try:
                        self.connection = connection_string
                        return True
                    except Exception as e:
                        return False
                
                def execute_query(self, query, params=None):
                    if not self.connection:
                        raise ConnectionError("Not connected to database")
                    
                    results = []
                    if query.lower().startswith('select'):
                        # Simulate SELECT query results
                        for i in range(3):
                            row_data = {'id': i + 1, 'value': f"sample_data_{i}"}
                            if params:
                                for key, value in params.items():
                                    row_data[f"param_{key}"] = value
                            results.append(row_data)
                    
                    return results
                
                def close(self):
                    if self.connection:
                        self.connection = None
                        return True
                    return False
            """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "DatabaseConnection", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_MessageHandler() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class MessageHandler(ABC):
                def __init__(self, config):
                    self.config = config
                    self.processed_messages = []
                
                def validate_message(self, message):
                    required_fields = ['id', 'content', 'timestamp']
                    for field in required_fields:
                        if field not in message:
                            return False, f"Missing required field: {field}"
                    return True, None
                
                @abstractmethod
                def process_message(self, message):
                    is_valid, error = self.validate_message(message)
                    if not is_valid:
                        return {'status': 'error', 'message': error}
                    
                    processed = {
                        'id': message['id'],
                        'content_length': len(message['content']),
                        'timestamp': message['timestamp'],
                        'processed_at': 'now'
                    }
                    
                    if 'priority' in message:
                        priority_mapping = {'high': 3, 'medium': 2, 'low': 1}
                        processed['priority_score'] = priority_mapping.get(message['priority'], 1)
                    
                    self.processed_messages.append(processed)
                    return {'status': 'success', 'processed': processed}
                
                def get_statistics(self):
                    total_messages = len(self.processed_messages)
                    avg_content_length = 0
                    if total_messages > 0:
                        total_length = sum(msg['content_length'] for msg in self.processed_messages)
                        avg_content_length = total_length / total_messages
                    
                    return {
                        'total_processed': total_messages,
                        'average_content_length': avg_content_length
                    }
            """;

        String afterPythonCode = """
            class MessageHandler:
                def __init__(self, config):
                    self.config = config
                    self.processed_messages = []
                
                def validate_message(self, message):
                    required_fields = ['id', 'content', 'timestamp']
                    for field in required_fields:
                        if field not in message:
                            return False, f"Missing required field: {field}"
                    return True, None
                
                def process_message(self, message):
                    is_valid, error = self.validate_message(message)
                    if not is_valid:
                        return {'status': 'error', 'message': error}
                    
                    processed = {
                        'id': message['id'],
                        'content_length': len(message['content']),
                        'timestamp': message['timestamp'],
                        'processed_at': 'now'
                    }
                    
                    if 'priority' in message:
                        priority_mapping = {'high': 3, 'medium': 2, 'low': 1}
                        processed['priority_score'] = priority_mapping.get(message['priority'], 1)
                    
                    self.processed_messages.append(processed)
                    return {'status': 'success', 'processed': processed}
                
                def get_statistics(self):
                    total_messages = len(self.processed_messages)
                    avg_content_length = 0
                    if total_messages > 0:
                        total_length = sum(msg['content_length'] for msg in self.processed_messages)
                        avg_content_length = total_length / total_messages
                    
                    return {
                        'total_processed': total_messages,
                        'average_content_length': avg_content_length
                    }
            """;

        Map<String, String> beforeFiles = Map.of("message_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("message_handler.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "MessageHandler", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_FileProcessor() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class FileProcessor(ABC):
                def __init__(self, base_path):
                    self.base_path = base_path
                    self.processed_files = []
                
                def read_file(self, file_path):
                    try:
                        full_path = f"{self.base_path}/{file_path}"
                        # Simulate file reading
                        file_content = {
                            'path': full_path,
                            'size': len(file_path) * 10,  # Mock size
                            'content': f"Content of {file_path}",
                            'metadata': {
                                'created': '2023-01-01',
                                'modified': '2023-12-31'
                            }
                        }
                        return file_content
                    except Exception as e:
                        return {'error': str(e)}
                
                @abstractmethod
                def process_file(self, file_path):
                    file_data = self.read_file(file_path)
                    if 'error' in file_data:
                        return file_data
                    
                    processed = {
                        'original_path': file_data['path'],
                        'processed_size': file_data['size'],
                        'word_count': len(file_data['content'].split()),
                        'processing_status': 'completed'
                    }
                    
                    # Simulate different processing based on file extension
                    if file_path.endswith('.txt'):
                        processed['type'] = 'text'
                        processed['line_count'] = file_data['content'].count('\\n') + 1
                    elif file_path.endswith('.json'):
                        processed['type'] = 'json'
                        processed['estimated_objects'] = file_data['content'].count('{')
                    
                    self.processed_files.append(processed)
                    return processed
            """;

        String afterPythonCode = """
            class FileProcessor:
                def __init__(self, base_path):
                    self.base_path = base_path
                    self.processed_files = []
                
                def read_file(self, file_path):
                    try:
                        full_path = f"{self.base_path}/{file_path}"
                        # Simulate file reading
                        file_content = {
                            'path': full_path,
                            'size': len(file_path) * 10,  # Mock size
                            'content': f"Content of {file_path}",
                            'metadata': {
                                'created': '2023-01-01',
                                'modified': '2023-12-31'
                            }
                        }
                        return file_content
                    except Exception as e:
                        return {'error': str(e)}
                
                def process_file(self, file_path):
                    file_data = self.read_file(file_path)
                    if 'error' in file_data:
                        return file_data
                    
                    processed = {
                        'original_path': file_data['path'],
                        'processed_size': file_data['size'],
                        'word_count': len(file_data['content'].split()),
                        'processing_status': 'completed'
                    }
                    
                    # Simulate different processing based on file extension
                    if file_path.endswith('.txt'):
                        processed['type'] = 'text'
                        processed['line_count'] = file_data['content'].count('\\n') + 1
                    elif file_path.endswith('.json'):
                        processed['type'] = 'json'
                        processed['estimated_objects'] = file_data['content'].count('{')
                    
                    self.processed_files.append(processed)
                    return processed
            """;

        Map<String, String> beforeFiles = Map.of("file_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_processor.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "FileProcessor", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_CacheManager() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class CacheManager(ABC):
                def __init__(self, max_size=1000):
                    self.max_size = max_size
                    self.cache = {}
                    self.access_count = {}
                
                @abstractmethod
                def get(self, key):
                    if key in self.cache:
                        self.access_count[key] = self.access_count.get(key, 0) + 1
                        return self.cache[key]
                    return None
                
                @abstractmethod
                def put(self, key, value):
                    if len(self.cache) >= self.max_size:
                        # Simple LRU eviction
                        least_used_key = min(self.access_count.keys(), key=self.access_count.get)
                        del self.cache[least_used_key]
                        del self.access_count[least_used_key]
                    
                    self.cache[key] = value
                    self.access_count[key] = 1
                
                def evict(self, key):
                    if key in self.cache:
                        del self.cache[key]
                        if key in self.access_count:
                            del self.access_count[key]
                        return True
                    return False
                
                def clear(self):
                    cleared_count = len(self.cache)
                    self.cache.clear()
                    self.access_count.clear()
                    return cleared_count
                
                def get_stats(self):
                    return {
                        'size': len(self.cache),
                        'max_size': self.max_size,
                        'total_accesses': sum(self.access_count.values())
                    }
            """;

        String afterPythonCode = """
            class CacheManager:
                def __init__(self, max_size=1000):
                    self.max_size = max_size
                    self.cache = {}
                    self.access_count = {}
                
                def get(self, key):
                    if key in self.cache:
                        self.access_count[key] = self.access_count.get(key, 0) + 1
                        return self.cache[key]
                    return None
                
                def put(self, key, value):
                    if len(self.cache) >= self.max_size:
                        # Simple LRU eviction
                        least_used_key = min(self.access_count.keys(), key=self.access_count.get)
                        del self.cache[least_used_key]
                        del self.access_count[least_used_key]
                    
                    self.cache[key] = value
                    self.access_count[key] = 1
                
                def evict(self, key):
                    if key in self.cache:
                        del self.cache[key]
                        if key in self.access_count:
                            del self.access_count[key]
                        return True
                    return False
                
                def clear(self):
                    cleared_count = len(self.cache)
                    self.cache.clear()
                    self.access_count.clear()
                    return cleared_count
                
                def get_stats(self):
                    return {
                        'size': len(self.cache),
                        'max_size': self.max_size,
                        'total_accesses': sum(self.access_count.values())
                    }
            """;

        Map<String, String> beforeFiles = Map.of("cache_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("cache_manager.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "CacheManager", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_PaymentProcessor() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class PaymentProcessor(ABC):
                def __init__(self, merchant_id, api_key):
                    self.merchant_id = merchant_id
                    self.api_key = api_key
                    self.transactions = []
                
                def validate_payment_data(self, payment_data):
                    required_fields = ['amount', 'currency', 'card_number']
                    validation_errors = []
                    
                    for field in required_fields:
                        if field not in payment_data:
                            validation_errors.append(f"Missing {field}")
                    
                    if 'amount' in payment_data:
                        if not isinstance(payment_data['amount'], (int, float)):
                            validation_errors.append("Amount must be numeric")
                        elif payment_data['amount'] <= 0:
                            validation_errors.append("Amount must be positive")
                    
                    return len(validation_errors) == 0, validation_errors
                
                @abstractmethod
                def process_payment(self, payment_data):
                    is_valid, errors = self.validate_payment_data(payment_data)
                    if not is_valid:
                        return {
                            'success': False,
                            'errors': errors,
                            'transaction_id': None
                        }
                    
                    # Simulate payment processing
                    transaction = {
                        'transaction_id': f"txn_{len(self.transactions) + 1}",
                        'amount': payment_data['amount'],
                        'currency': payment_data['currency'],
                        'status': 'completed',
                        'timestamp': 'now'
                    }
                    
                    # Simulate different processing fees
                    if payment_data['amount'] > 1000:
                        transaction['fee'] = payment_data['amount'] * 0.025
                    else:
                        transaction['fee'] = payment_data['amount'] * 0.03
                    
                    self.transactions.append(transaction)
                    return {
                        'success': True,
                        'transaction': transaction,
                        'errors': []
                    }
            """;

        String afterPythonCode = """
            class PaymentProcessor:
                def __init__(self, merchant_id, api_key):
                    self.merchant_id = merchant_id
                    self.api_key = api_key
                    self.transactions = []
                
                def validate_payment_data(self, payment_data):
                    required_fields = ['amount', 'currency', 'card_number']
                    validation_errors = []
                    
                    for field in required_fields:
                        if field not in payment_data:
                            validation_errors.append(f"Missing {field}")
                    
                    if 'amount' in payment_data:
                        if not isinstance(payment_data['amount'], (int, float)):
                            validation_errors.append("Amount must be numeric")
                        elif payment_data['amount'] <= 0:
                            validation_errors.append("Amount must be positive")
                    
                    return len(validation_errors) == 0, validation_errors
                
                def process_payment(self, payment_data):
                    is_valid, errors = self.validate_payment_data(payment_data)
                    if not is_valid:
                        return {
                            'success': False,
                            'errors': errors,
                            'transaction_id': None
                        }
                    
                    # Simulate payment processing
                    transaction = {
                        'transaction_id': f"txn_{len(self.transactions) + 1}",
                        'amount': payment_data['amount'],
                        'currency': payment_data['currency'],
                        'status': 'completed',
                        'timestamp': 'now'
                    }
                    
                    # Simulate different processing fees
                    if payment_data['amount'] > 1000:
                        transaction['fee'] = payment_data['amount'] * 0.025
                    else:
                        transaction['fee'] = payment_data['amount'] * 0.03
                    
                    self.transactions.append(transaction)
                    return {
                        'success': True,
                        'transaction': transaction,
                        'errors': []
                    }
            """;

        Map<String, String> beforeFiles = Map.of("payment_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("payment_processor.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "PaymentProcessor", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_LogAnalyzer() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class LogAnalyzer(ABC):
                def __init__(self, log_level='INFO'):
                    self.log_level = log_level
                    self.analyzed_logs = []
                    self.patterns = {}
                
                def parse_log_entry(self, log_line):
                    parts = log_line.split(' ', 3)
                    if len(parts) >= 4:
                        return {
                            'timestamp': parts[0],
                            'level': parts[1],
                            'source': parts[2],
                            'message': parts[3]
                        }
                    return None
                
                @abstractmethod
                def analyze_patterns(self, log_entries):
                    for entry in log_entries:
                        parsed = self.parse_log_entry(entry)
                        if not parsed:
                            continue
                        
                        level = parsed['level']
                        if level not in self.patterns:
                            self.patterns[level] = {
                                'count': 0,
                                'sources': set(),
                                'keywords': {}
                            }
                        
                        self.patterns[level]['count'] += 1
                        self.patterns[level]['sources'].add(parsed['source'])
                        
                        # Extract keywords from message
                        words = parsed['message'].lower().split()
                        for word in words:
                            if len(word) > 3:  # Only consider words longer than 3 chars
                                if word not in self.patterns[level]['keywords']:
                                    self.patterns[level]['keywords'][word] = 0
                                self.patterns[level]['keywords'][word] += 1
                        
                        self.analyzed_logs.append(parsed)
                    
                    # Convert sets to lists for JSON serialization
                    result = {}
                    for level, data in self.patterns.items():
                        result[level] = {
                            'count': data['count'],
                            'sources': list(data['sources']),
                            'top_keywords': sorted(data['keywords'].items(), 
                                                 key=lambda x: x[1], reverse=True)[:5]
                        }
                    
                    return result
            """;

        String afterPythonCode = """
            class LogAnalyzer:
                def __init__(self, log_level='INFO'):
                    self.log_level = log_level
                    self.analyzed_logs = []
                    self.patterns = {}
                
                def parse_log_entry(self, log_line):
                    parts = log_line.split(' ', 3)
                    if len(parts) >= 4:
                        return {
                            'timestamp': parts[0],
                            'level': parts[1],
                            'source': parts[2],
                            'message': parts[3]
                        }
                    return None
                
                def analyze_patterns(self, log_entries):
                    for entry in log_entries:
                        parsed = self.parse_log_entry(entry)
                        if not parsed:
                            continue
                        
                        level = parsed['level']
                        if level not in self.patterns:
                            self.patterns[level] = {
                                'count': 0,
                                'sources': set(),
                                'keywords': {}
                            }
                        
                        self.patterns[level]['count'] += 1
                        self.patterns[level]['sources'].add(parsed['source'])
                        
                        # Extract keywords from message
                        words = parsed['message'].lower().split()
                        for word in words:
                            if len(word) > 3:  # Only consider words longer than 3 chars
                                if word not in self.patterns[level]['keywords']:
                                    self.patterns[level]['keywords'][word] = 0
                                self.patterns[level]['keywords'][word] += 1
                        
                        self.analyzed_logs.append(parsed)
                    
                    # Convert sets to lists for JSON serialization
                    result = {}
                    for level, data in self.patterns.items():
                        result[level] = {
                            'count': data['count'],
                            'sources': list(data['sources']),
                            'top_keywords': sorted(data['keywords'].items(), 
                                                 key=lambda x: x[1], reverse=True)[:5]
                        }
                    
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("log_analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("log_analyzer.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "LogAnalyzer", "abstract");
    }

    @Test
    void detectsRemoveAbstractModifier_ConfigurationManager() throws Exception {
        String beforePythonCode = """
            from abc import ABC, abstractmethod
            
            class ConfigurationManager(ABC):
                def __init__(self, config_path=None):
                    self.config_path = config_path
                    self.configurations = {}
                    self.defaults = {
                        'database': {
                            'host': 'localhost',
                            'port': 5432,
                            'timeout': 30
                        },
                        'cache': {
                            'enabled': True,
                            'ttl': 3600,
                            'max_size': 1000
                        }
                    }
                
                @abstractmethod
                def load_configuration(self, config_data):
                    merged_config = {}
                    
                    # Start with defaults
                    for section, default_values in self.defaults.items():
                        merged_config[section] = default_values.copy()
                    
                    # Override with provided configuration
                    for section, values in config_data.items():
                        if section not in merged_config:
                            merged_config[section] = {}
                        
                        for key, value in values.items():
                            # Type validation
                            if section in self.defaults and key in self.defaults[section]:
                                expected_type = type(self.defaults[section][key])
                                if isinstance(value, expected_type):
                                    merged_config[section][key] = value
                                else:
                                    print(f"Warning: Type mismatch for {section}.{key}")
                            else:
                                merged_config[section][key] = value
                    
                    self.configurations = merged_config
                    return merged_config
                
                def get_config_value(self, section, key, default=None):
                    if section in self.configurations:
                        return self.configurations[section].get(key, default)
                    return default
                
                def update_config_value(self, section, key, value):
                    if section not in self.configurations:
                        self.configurations[section] = {}
                    self.configurations[section][key] = value
                    return True
            """;

        String afterPythonCode = """
            class ConfigurationManager:
                def __init__(self, config_path=None):
                    self.config_path = config_path
                    self.configurations = {}
                    self.defaults = {
                        'database': {
                            'host': 'localhost',
                            'port': 5432,
                            'timeout': 30
                        },
                        'cache': {
                            'enabled': True,
                            'ttl': 3600,
                            'max_size': 1000
                        }
                    }
                
                def load_configuration(self, config_data):
                    merged_config = {}
                    
                    # Start with defaults
                    for section, default_values in self.defaults.items():
                        merged_config[section] = default_values.copy()
                    
                    # Override with provided configuration
                    for section, values in config_data.items():
                        if section not in merged_config:
                            merged_config[section] = {}
                        
                        for key, value in values.items():
                            # Type validation
                            if section in self.defaults and key in self.defaults[section]:
                                expected_type = type(self.defaults[section][key])
                                if isinstance(value, expected_type):
                                    merged_config[section][key] = value
                                else:
                                    print(f"Warning: Type mismatch for {section}.{key}")
                            else:
                                merged_config[section][key] = value
                    
                    self.configurations = merged_config
                    return merged_config
                
                def get_config_value(self, section, key, default=None):
                    if section in self.configurations:
                        return self.configurations[section].get(key, default)
                    return default
                
                def update_config_value(self, section, key, value):
                    if section not in self.configurations:
                        self.configurations[section] = {}
                    self.configurations[section][key] = value
                    return True
            """;

        Map<String, String> beforeFiles = Map.of("config_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_manager.py", afterPythonCode);

        assertRemoveClassModifierDetected(beforeFiles, afterFiles, "ConfigurationManager", "abstract");
    }

    private void assertRemoveClassModifierDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String className, String modifier) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        // Print out all classes for debugging
        System.out.println("=== BEFORE MODEL CLASSES ===");
        beforeUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName() + " - Abstract: " + umlClass.isAbstract());
        });

        System.out.println("=== AFTER MODEL CLASSES ===");
        afterUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName() + " - Abstract: " + umlClass.isAbstract());
        });

        // Verify classes have expected modifiers in the models
        Optional<UMLClass> beforeClass = findClassByName(beforeUML, className);
        Optional<UMLClass> afterClass = findClassByName(afterUML, className);

        assertTrue(beforeClass.isPresent(), "Class not found in before model: " + className);
        assertTrue(afterClass.isPresent(), "Class not found in after model: " + className);

        // Check that the modifier was removed
        boolean beforeHasModifier = checkModifier(beforeClass.get(), modifier);
        boolean afterHasModifier = checkModifier(afterClass.get(), modifier);

        assertEquals(true, beforeHasModifier,
                "Before class should have " + modifier + " modifier");
        assertEquals(false, afterHasModifier,
                "After class should not have " + modifier + " modifier");

        // Check for the remove class modifier refactoring
        UMLModelDiff diff = beforeUML.diff(afterUML);
        System.out.println("Total refactorings: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(System.out::println);

        boolean removeModifierDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof RemoveClassModifierRefactoring removeModifierRef) {
                        return removeModifierRef.getClassBefore().getName().equals(className) &&
                                removeModifierRef.getClassAfter().getName().equals(className) &&
                                removeModifierRef.getModifier().equals(modifier);
                    }
                    return false;
                });

        assertTrue(removeModifierDetected,
                "Expected remove " + modifier + " modifier refactoring for class " + className);
    }

    private boolean checkModifier(UMLClass umlClass, String modifier) {
        switch (modifier) {
            case "abstract":
                return umlClass.isAbstract();
            default:
                return false;
        }
    }

    private Optional<UMLClass> findClassByName(UMLModel model, String className) {
        return model.getClassList().stream()
                .filter(umlClass -> umlClass.getName().equals(className))
                .findFirst();
    }
}