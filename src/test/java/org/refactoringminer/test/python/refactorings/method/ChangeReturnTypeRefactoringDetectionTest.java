package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class ChangeReturnTypeRefactoringDetectionTest {

    @Test
    void detectsMethodReturnTypeChange() throws Exception {
        String beforePythonCode = """
        class TypeExample:
            def calculate_sum(self, a, b) -> int:
                return a + b
        """;

        String afterPythonCode = """
        class TypeExample:
            def calculate_sum(self, a, b) -> float:
                return a + b
        """;

        Map<String, String> beforeFiles = Map.of("tests/example.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/example.py", afterPythonCode);

        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "calculate_sum", "int",
                "calculate_sum", "float");
    }

    @Test
    void detectsReturnTypeChange_WithNestedLoopsAndConditionals() throws Exception {
        String beforePythonCode = """
        class DataAnalyzer:
            def analyze_matrix(self, matrix) -> list:
                results = []
                for i in range(len(matrix)):
                    row_analysis = {}
                    for j in range(len(matrix[i])):
                        if matrix[i][j] > 0:
                            value = matrix[i][j]
                            while value > 1:
                                if value % 2 == 0:
                                    value = value // 2
                                else:
                                    value = value * 3 + 1
                            row_analysis[f"pos_{j}"] = value
                        elif matrix[i][j] < 0:
                            row_analysis[f"neg_{j}"] = abs(matrix[i][j])
                    results.append(row_analysis)
                return results
        """;

        String afterPythonCode = """
        class DataAnalyzer:
            def analyze_matrix(self, matrix) -> dict:
                results = []
                for i in range(len(matrix)):
                    row_analysis = {}
                    for j in range(len(matrix[i])):
                        if matrix[i][j] > 0:
                            value = matrix[i][j]
                            while value > 1:
                                if value % 2 == 0:
                                    value = value // 2
                                else:
                                    value = value * 3 + 1
                            row_analysis[f"pos_{j}"] = value
                        elif matrix[i][j] < 0:
                            row_analysis[f"neg_{j}"] = abs(matrix[i][j])
                    results.append(row_analysis)
                return results
        """;

        Map<String, String> beforeFiles = Map.of("analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("analyzer.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "analyze_matrix", "list",
                "analyze_matrix", "dict");
    }

    @Test
    void detectsReturnTypeChange_WithExceptionHandling() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def process_files(self, file_paths) -> bool:
                results = {}
                for path in file_paths:
                    try:
                        with open(path, 'r') as file:
                            content = file.read()
                            lines = content.split('\\n')
                            for line in lines:
                                if '=' in line and not line.startswith('#'):
                                    key, value = line.split('=', 1)
                                    results[key.strip()] = value.strip()
                    except FileNotFoundError:
                        results[path] = {'error': 'File not found'}
                    except Exception as e:
                        results[path] = {'error': str(e)}
                return len(results) > 0
        """;

        String afterPythonCode = """
        class FileProcessor:
            def process_files(self, file_paths) -> dict:
                results = {}
                for path in file_paths:
                    try:
                        with open(path, 'r') as file:
                            content = file.read()
                            lines = content.split('\\n')
                            for line in lines:
                                if '=' in line and not line.startswith('#'):
                                    key, value = line.split('=', 1)
                                    results[key.strip()] = value.strip()
                    except FileNotFoundError:
                        results[path] = {'error': 'File not found'}
                    except Exception as e:
                        results[path] = {'error': str(e)}
                return results
        """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "process_files", "bool",
                "process_files", "dict");
    }

    @Test
    void detectsReturnTypeChange_WithListComprehensions() throws Exception {
        String beforePythonCode = """
        class DataTransformer:
            def transform_data(self, raw_data) -> str:
                filtered_data = [
                    {
                        'id': item['id'],
                        'values': [v for v in item['values'] if v is not None and v > 0],
                        'metadata': {k: v for k, v in item.get('meta', {}).items() if k.startswith('important_')}
                    }
                    for item in raw_data 
                    if 'id' in item and 'values' in item and len(item['values']) > 0
                ]
                
                processed_generator = (
                    {
                        'processed_id': data['id'].upper(),
                        'sum_values': sum(data['values']),
                        'avg_values': sum(data['values']) / len(data['values'])
                    }
                    for data in filtered_data
                    if len(data['values']) >= 3
                )
                
                result_list = list(processed_generator)
                return f"Processed {len(result_list)} items"
        """;

        String afterPythonCode = """
        class DataTransformer:
            def transform_data(self, raw_data) -> list:
                filtered_data = [
                    {
                        'id': item['id'],
                        'values': [v for v in item['values'] if v is not None and v > 0],
                        'metadata': {k: v for k, v in item.get('meta', {}).items() if k.startswith('important_')}
                    }
                    for item in raw_data 
                    if 'id' in item and 'values' in item and len(item['values']) > 0
                ]
                
                processed_generator = (
                    {
                        'processed_id': data['id'].upper(),
                        'sum_values': sum(data['values']),
                        'avg_values': sum(data['values']) / len(data['values'])
                    }
                    for data in filtered_data
                    if len(data['values']) >= 3
                )
                
                result_list = list(processed_generator)
                return result_list
        """;

        Map<String, String> beforeFiles = Map.of("transformer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("transformer.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "transform_data", "str",
                "transform_data", "list");
    }

    @Test
    void detectsReturnTypeChange_WithInnerFunctions() throws Exception {
        String beforePythonCode = """
        class TaskManager:
            def execute_workflow(self, tasks) -> int:
                def validate_task(task):
                    required_fields = ['name', 'type', 'priority']
                    for field in required_fields:
                        if field not in task:
                            return False
                    return True
                
                def calculate_priority_score(task):
                    base_score = {'high': 10, 'medium': 5, 'low': 1}.get(task['priority'], 0)
                    type_bonus = {'critical': 5, 'normal': 2, 'optional': 0}.get(task['type'], 0)
                    return base_score + type_bonus
                
                def process_single_task(task):
                    if not validate_task(task):
                        return None
                    
                    priority_score = calculate_priority_score(task)
                    return {
                        'name': task['name'],
                        'priority_score': priority_score,
                        'status': 'completed'
                    }
                
                results = []
                for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                    processed = process_single_task(task)
                    if processed:
                        results.append(processed)
                
                return len(results)
        """;

        String afterPythonCode = """
        class TaskManager:
            def execute_workflow(self, tasks) -> dict:
                def validate_task(task):
                    required_fields = ['name', 'type', 'priority']
                    for field in required_fields:
                        if field not in task:
                            return False
                    return True
                
                def calculate_priority_score(task):
                    base_score = {'high': 10, 'medium': 5, 'low': 1}.get(task['priority'], 0)
                    type_bonus = {'critical': 5, 'normal': 2, 'optional': 0}.get(task['type'], 0)
                    return base_score + type_bonus
                
                def process_single_task(task):
                    if not validate_task(task):
                        return None
                    
                    priority_score = calculate_priority_score(task)
                    return {
                        'name': task['name'],
                        'priority_score': priority_score,
                        'status': 'completed'
                    }
                
                results = []
                for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                    processed = process_single_task(task)
                    if processed:
                        results.append(processed)
                
                return {'results': results, 'count': len(results)}
        """;

        Map<String, String> beforeFiles = Map.of("manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("manager.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "execute_workflow", "int",
                "execute_workflow", "dict");
    }

    @Test
    void detectsReturnTypeChange_ModuleLevelFunction() throws Exception {
        String beforePythonCode = """
        def analyze_log_patterns(log_entries) -> bool:
            pattern_counts = {}
            error_patterns = []
            
            for entry in log_entries:
                if not entry or 'level' not in entry:
                    continue
                
                level = entry['level'].upper()
                message = entry['message']
                
                if level == 'ERROR':
                    error_words = [word for word in message.split() if len(word) > 3]
                    for word in error_words:
                        if word not in pattern_counts:
                            pattern_counts[word] = 0
                        pattern_counts[word] += 1
                    
                    if 'exception' in message.lower():
                        error_patterns.append({
                            'timestamp': entry.get('timestamp', 'unknown'),
                            'pattern': 'critical_error',
                            'message': message[:100]
                        })
            
            return len(error_patterns) > 0
        
        def main():
            sample_logs = [
                {'level': 'ERROR', 'message': 'Database connection failed', 'timestamp': '2023-01-01'}
            ]
            result = analyze_log_patterns(sample_logs)
            print(result)
        """;

        String afterPythonCode = """
        def analyze_log_patterns(log_entries) -> dict:
            pattern_counts = {}
            error_patterns = []
            
            for entry in log_entries:
                if not entry or 'level' not in entry:
                    continue
                
                level = entry['level'].upper()
                message = entry['message']
                
                if level == 'ERROR':
                    error_words = [word for word in message.split() if len(word) > 3]
                    for word in error_words:
                        if word not in pattern_counts:
                            pattern_counts[word] = 0
                        pattern_counts[word] += 1
                    
                    if 'exception' in message.lower():
                        error_patterns.append({
                            'timestamp': entry.get('timestamp', 'unknown'),
                            'pattern': 'critical_error',
                            'message': message[:100]
                        })
            
            return {'patterns': pattern_counts, 'errors': error_patterns}
        
        def main():
            sample_logs = [
                {'level': 'ERROR', 'message': 'Database connection failed', 'timestamp': '2023-01-01'}
            ]
            result = analyze_log_patterns(sample_logs)
            print(result)
        """;

        Map<String, String> beforeFiles = Map.of("analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("analyzer.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "analyze_log_patterns", "bool",
                "analyze_log_patterns", "dict");
    }

    @Test
    void detectsReturnTypeChange_OptionalToRequired() throws Exception {
        String beforePythonCode = """
        from typing import Optional
        
        class UserManager:
            def find_user(self, user_id) -> Optional[dict]:
                users_db = {
                    1: {'name': 'Alice', 'email': 'alice@example.com'},
                    2: {'name': 'Bob', 'email': 'bob@example.com'}
                }
                
                for uid, user_data in users_db.items():
                    if uid == user_id:
                        user_profile = {}
                        for key, value in user_data.items():
                            if key in ['name', 'email']:
                                user_profile[key] = value
                        
                        if len(user_profile) > 0:
                            return user_profile
                
                return None
        """;

        String afterPythonCode = """
        class UserManager:
            def find_user(self, user_id) -> dict:
                users_db = {
                    1: {'name': 'Alice', 'email': 'alice@example.com'},
                    2: {'name': 'Bob', 'email': 'bob@example.com'}
                }
                
                for uid, user_data in users_db.items():
                    if uid == user_id:
                        user_profile = {}
                        for key, value in user_data.items():
                            if key in ['name', 'email']:
                                user_profile[key] = value
                        
                        if len(user_profile) > 0:
                            return user_profile
                
                return {'error': 'User not found'}
        """;

        Map<String, String> beforeFiles = Map.of("user_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_manager.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "find_user", "Optional[dict]",
                "find_user", "dict");
    }

    @Test
    void detectsReturnTypeChange_WithAsyncAndAwait() throws Exception {
        String beforePythonCode = """
        import asyncio
        
        class AsyncDataProcessor:
            async def process_async_data(self, data_sources) -> str:
                results = []
                
                async def fetch_data(source):
                    await asyncio.sleep(0.1)  # Simulate async operation
                    if source['type'] == 'database':
                        return {'data': f"DB data from {source['name']}", 'count': 100}
                    elif source['type'] == 'api':
                        return {'data': f"API data from {source['name']}", 'count': 50}
                    else:
                        return {'data': 'Unknown source', 'count': 0}
                
                tasks = []
                for source in data_sources:
                    if 'name' in source and 'type' in source:
                        task = fetch_data(source)
                        tasks.append(task)
                
                if len(tasks) > 0:
                    fetched_results = await asyncio.gather(*tasks)
                    for result in fetched_results:
                        if result['count'] > 0:
                            results.append(result)
                
                return f"Processed {len(results)} data sources successfully"
        """;

        String afterPythonCode = """
        import asyncio
        
        class AsyncDataProcessor:
            async def process_async_data(self, data_sources) -> list:
                results = []
                
                async def fetch_data(source):
                    await asyncio.sleep(0.1)  # Simulate async operation
                    if source['type'] == 'database':
                        return {'data': f"DB data from {source['name']}", 'count': 100}
                    elif source['type'] == 'api':
                        return {'data': f"API data from {source['name']}", 'count': 50}
                    else:
                        return {'data': 'Unknown source', 'count': 0}
                
                tasks = []
                for source in data_sources:
                    if 'name' in source and 'type' in source:
                        task = fetch_data(source)
                        tasks.append(task)
                
                if len(tasks) > 0:
                    fetched_results = await asyncio.gather(*tasks)
                    for result in fetched_results:
                        if result['count'] > 0:
                            results.append(result)
                
                return results
        """;

        Map<String, String> beforeFiles = Map.of("async_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("async_processor.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "process_async_data", "str",
                "process_async_data", "list");
    }

    @Test
    void detectsReturnTypeChange_WithGenericsAndTypeVars() throws Exception {
        String beforePythonCode = """
        from typing import List, Dict, Any
        
        class GenericProcessor:
            def process_collection(self, items) -> List[Dict[str, Any]]:
                processed_items = []
                
                for item in items:
                    if isinstance(item, dict):
                        processed_item = {}
                        for key, value in item.items():
                            if isinstance(value, (int, float)):
                                if value > 0:
                                    processed_item[f"positive_{key}"] = value * 2
                                elif value < 0:
                                    processed_item[f"negative_{key}"] = abs(value)
                                else:
                                    processed_item[f"zero_{key}"] = 0
                            elif isinstance(value, str):
                                processed_item[f"string_{key}"] = value.upper()
                            else:
                                processed_item[f"other_{key}"] = str(value)
                        
                        if len(processed_item) > 0:
                            processed_items.append(processed_item)
                    
                    elif isinstance(item, (list, tuple)):
                        list_summary = {
                            'type': 'collection',
                            'length': len(item),
                            'first_item': item[0] if len(item) > 0 else None
                        }
                        processed_items.append(list_summary)
                
                return processed_items
        """;

        String afterPythonCode = """
        from typing import Dict, Any
        
        class GenericProcessor:
            def process_collection(self, items) -> Dict[str, Any]:
                processed_items = []
                
                for item in items:
                    if isinstance(item, dict):
                        processed_item = {}
                        for key, value in item.items():
                            if isinstance(value, (int, float)):
                                if value > 0:
                                    processed_item[f"positive_{key}"] = value * 2
                                elif value < 0:
                                    processed_item[f"negative_{key}"] = abs(value)
                                else:
                                    processed_item[f"zero_{key}"] = 0
                            elif isinstance(value, str):
                                processed_item[f"string_{key}"] = value.upper()
                            else:
                                processed_item[f"other_{key}"] = str(value)
                        
                        if len(processed_item) > 0:
                            processed_items.append(processed_item)
                    
                    elif isinstance(item, (list, tuple)):
                        list_summary = {
                            'type': 'collection',
                            'length': len(item),
                            'first_item': item[0] if len(item) > 0 else None
                        }
                        processed_items.append(list_summary)
                
                return {
                    'processed_items': processed_items,
                    'total_count': len(processed_items),
                    'summary': 'Processing completed'
                }
        """;

        Map<String, String> beforeFiles = Map.of("generic_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("generic_processor.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "process_collection", "List[Dict[str,Any]]",
                "process_collection", "Dict[str,Any]");
    }

    @Test
    void detectsReturnTypeChange_WithUnionTypes() throws Exception {
        String beforePythonCode = """
        from typing import Union
        
        class ConfigManager:
            def load_config(self, config_path) -> Union[dict, str]:
                config_data = {}
                
                try:
                    # Simulate reading different config formats
                    if config_path.endswith('.json'):
                        # JSON processing simulation
                        json_content = {
                            'database': {'host': 'localhost', 'port': 5432},
                            'cache': {'enabled': True, 'ttl': 3600},
                            'logging': {'level': 'INFO', 'file': '/var/log/app.log'}
                        }
                        
                        for section, settings in json_content.items():
                            config_data[section] = {}
                            for key, value in settings.items():
                                if isinstance(value, (int, bool)):
                                    config_data[section][key] = value
                                elif isinstance(value, str):
                                    config_data[section][key] = value.strip()
                        
                        return config_data
                    
                    elif config_path.endswith('.yaml'):
                        # YAML processing simulation
                        return "YAML configuration loaded successfully"
                    
                    else:
                        return "Unsupported configuration format"
                
                except Exception as e:
                    return f"Error loading configuration: {str(e)}"
        """;

        String afterPythonCode = """
        class ConfigManager:
            def load_config(self, config_path) -> dict:
                config_data = {}
                
                try:
                    # Simulate reading different config formats
                    if config_path.endswith('.json'):
                        # JSON processing simulation
                        json_content = {
                            'database': {'host': 'localhost', 'port': 5432},
                            'cache': {'enabled': True, 'ttl': 3600},
                            'logging': {'level': 'INFO', 'file': '/var/log/app.log'}
                        }
                        
                        for section, settings in json_content.items():
                            config_data[section] = {}
                            for key, value in settings.items():
                                if isinstance(value, (int, bool)):
                                    config_data[section][key] = value
                                elif isinstance(value, str):
                                    config_data[section][key] = value.strip()
                        
                        return config_data
                    
                    elif config_path.endswith('.yaml'):
                        # YAML processing simulation
                        return {'status': 'YAML configuration loaded successfully'}
                    
                    else:
                        return {'error': 'Unsupported configuration format'}
                
                except Exception as e:
                    return {'error': f"Error loading configuration: {str(e)}"}
        """;

        Map<String, String> beforeFiles = Map.of("config_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config_manager.py", afterPythonCode);
        assertReturnTypeChangeDetected(beforeFiles, afterFiles,
                "load_config", "Union[dict,str]",
                "load_config", "dict");
    }


    private void assertReturnTypeChangeDetected(Map<String, String> beforeFiles, Map<String, String> afterFiles,
                                                String beforeMethodName, String beforeReturnType,
                                                String afterMethodName, String afterReturnType) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        System.out.println("Refactorings size " + diff.getRefactorings().size());

        boolean returnTypeChangeDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof ChangeReturnTypeRefactoring returnTypeChange) {
                        VariableDeclarationContainer originalOperation = returnTypeChange.getOperationBefore();
                        VariableDeclarationContainer changedOperation = returnTypeChange.getOperationAfter();

                        // Use toQualifiedString() instead of getClassType()
                        String originalTypeString = returnTypeChange.getOriginalType().toQualifiedString();
                        String changedTypeString = returnTypeChange.getChangedType().toQualifiedString();

                        return originalOperation.getName().equals(beforeMethodName) &&
                                changedOperation.getName().equals(afterMethodName) &&
                                originalTypeString.equals(beforeReturnType) &&
                                changedTypeString.equals(afterReturnType);
                    }
                    return false;
                });

        assertTrue(returnTypeChangeDetected,
                "Expected a return type change from " + beforeReturnType +
                        " to " + afterReturnType + " for method " + beforeMethodName);
    }

}
