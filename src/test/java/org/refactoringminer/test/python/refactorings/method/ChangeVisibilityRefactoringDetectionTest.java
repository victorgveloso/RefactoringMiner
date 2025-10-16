package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.diff.ChangeOperationAccessModifierRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.utils.RefactoringAssertUtils.dumpOperation;

public class ChangeVisibilityRefactoringDetectionTest {

    @Test
    void detectsPublicToPrivateVisibilityChange() throws Exception {
        String beforePythonCode = """
            class VisibilityExample:
                def public_method(self):
                    return "I'm public"
            """;

        String afterPythonCode = """
            class VisibilityExample:
                def __public_method(self):
                    return "I'm now private"
            """;

        Map<String, String> beforeFiles = Map.of("tests/example.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/example.py", afterPythonCode);

        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "public_method", Visibility.PUBLIC,
                "__public_method", Visibility.PRIVATE);
    }

    @Test
    void detectsProtectedToPublicVisibilityChange() throws Exception {
        String beforePythonCode = """
            class VisibilityExample:
                def _protected_method(self):
                    return "I'm protected"
            """;

        String afterPythonCode = """
            class VisibilityExample:
                def protected_method(self):
                    return "I'm now public"
            """;

        Map<String, String> beforeFiles = Map.of("tests/example.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/example.py", afterPythonCode);

        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "_protected_method", Visibility.PROTECTED,
                "protected_method", Visibility.PUBLIC);
    }

    @Test
    void detectsPrivateToProtectedVisibilityChange() throws Exception {
        String beforePythonCode = """
            class VisibilityExample:
                def __private_method(self):
                    return "I'm private"
            """;

        String afterPythonCode = """
            class VisibilityExample:
                def _private_method(self):
                    return "I'm now protected"
            """;

        Map<String, String> beforeFiles = Map.of("tests/visibility/example.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/visibility/example.py", afterPythonCode);

        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "__private_method", Visibility.PRIVATE,
                "_private_method", Visibility.PROTECTED);
    }

    @Test
    void detectsVisibilityChange_WithNestedLoopsAndConditionals() throws Exception {
        String beforePythonCode = """
        class DataAnalyzer:
            def analyze_matrix(self, matrix):
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
            def _analyze_matrix(self, matrix):
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
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "analyze_matrix", Visibility.PUBLIC,
                "_analyze_matrix", Visibility.PROTECTED);
    }

    @Test
    void detectsVisibilityChange_WithExceptionHandling() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def _process_files(self, file_paths):
                results = {}
                for path in file_paths:
                    file_handle = None
                    try:
                        file_handle = open(path, 'r')
                        content = file_handle.read()
                        lines = content.split('\\n')
                        config_data = {}
                        for line in lines:
                            if '=' in line and not line.startswith('#'):
                                key, value = line.split('=', 1)
                                config_data[key.strip()] = value.strip()
                        results[path] = config_data
                    except FileNotFoundError:
                        results[path] = {'error': 'File not found'}
                    except PermissionError:
                        results[path] = {'error': 'Permission denied'}
                    except Exception as e:
                        results[path] = {'error': f'Unexpected error: {str(e)}'}
                    finally:
                        if file_handle:
                            try:
                                file_handle.close()
                            except:
                                pass
                return results
        """;

        String afterPythonCode = """
        class FileProcessor:
            def __process_files(self, file_paths):
                results = {}
                for path in file_paths:
                    file_handle = None
                    try:
                        file_handle = open(path, 'r')
                        content = file_handle.read()
                        lines = content.split('\\n')
                        config_data = {}
                        for line in lines:
                            if '=' in line and not line.startswith('#'):
                                key, value = line.split('=', 1)
                                config_data[key.strip()] = value.strip()
                        results[path] = config_data
                    except FileNotFoundError:
                        results[path] = {'error': 'File not found'}
                    except PermissionError:
                        results[path] = {'error': 'Permission denied'}
                    except Exception as e:
                        results[path] = {'error': f'Unexpected error: {str(e)}'}
                    finally:
                        if file_handle:
                            try:
                                file_handle.close()
                            except:
                                pass
                return results
        """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "_process_files", Visibility.PROTECTED,
                "__process_files", Visibility.PRIVATE);
    }

    @Test
    void detectsVisibilityChange_WithListComprehensions() throws Exception {
        String beforePythonCode = """
        class DataTransformer:
            def __transform_data(self, raw_data):
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
                        'avg_values': sum(data['values']) / len(data['values']),
                        'meta_count': len(data['metadata'])
                    }
                    for data in filtered_data
                    if len(data['values']) >= 3 and sum(data['values']) > 10
                )
                
                result_matrix = [
                    [val * multiplier for val in proc['values'] if val < 100]
                    for proc in processed_generator
                    for multiplier in range(1, 4)
                ]
                
                return list(result_matrix)
        """;

        String afterPythonCode = """
        class DataTransformer:
            def transform_data(self, raw_data):
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
                        'avg_values': sum(data['values']) / len(data['values']),
                        'meta_count': len(data['metadata'])
                    }
                    for data in filtered_data
                    if len(data['values']) >= 3 and sum(data['values']) > 10
                )
                
                result_matrix = [
                    [val * multiplier for val in proc['values'] if val < 100]
                    for proc in processed_generator
                    for multiplier in range(1, 4)
                ]
                
                return list(result_matrix)
        """;

        Map<String, String> beforeFiles = Map.of("transformer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("transformer.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "__transform_data", Visibility.PRIVATE,
                "transform_data", Visibility.PUBLIC);
    }

    @Test
    void detectsVisibilityChange_WithInnerFunctions() throws Exception {
        String beforePythonCode = """
        class TaskManager:
            def execute_workflow(self, tasks):
                def validate_task(task):
                    required_fields = ['name', 'type', 'priority']
                    for field in required_fields:
                        if field not in task:
                            return False, f"Missing field: {field}"
                    return True, None
                
                def calculate_priority_score(task):
                    base_score = {'high': 10, 'medium': 5, 'low': 1}.get(task['priority'], 0)
                    type_bonus = {'critical': 5, 'normal': 2, 'optional': 0}.get(task['type'], 0)
                    return base_score + type_bonus
                
                def process_single_task(task):
                    is_valid, error = validate_task(task)
                    if not is_valid:
                        return {'status': 'failed', 'error': error, 'task': task['name']}
                    
                    priority_score = calculate_priority_score(task)
                    result = {
                        'name': task['name'],
                        'priority_score': priority_score,
                        'status': 'completed'
                    }
                    
                    if priority_score >= 10:
                        result['urgent'] = True
                    
                    return result
                
                results = []
                failed_count = 0
                
                for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                    processed = process_single_task(task)
                    results.append(processed)
                    if processed['status'] == 'failed':
                        failed_count += 1
                
                return {
                    'results': results,
                    'total_processed': len(tasks),
                    'failed_count': failed_count,
                    'success_rate': (len(tasks) - failed_count) / len(tasks) if len(tasks) > 0 else 0
                }
        """;

        String afterPythonCode = """
        class TaskManager:
            def _execute_workflow(self, tasks):
                def validate_task(task):
                    required_fields = ['name', 'type', 'priority']
                    for field in required_fields:
                        if field not in task:
                            return False, f"Missing field: {field}"
                    return True, None
                
                def calculate_priority_score(task):
                    base_score = {'high': 10, 'medium': 5, 'low': 1}.get(task['priority'], 0)
                    type_bonus = {'critical': 5, 'normal': 2, 'optional': 0}.get(task['type'], 0)
                    return base_score + type_bonus
                
                def process_single_task(task):
                    is_valid, error = validate_task(task)
                    if not is_valid:
                        return {'status': 'failed', 'error': error, 'task': task['name']}
                    
                    priority_score = calculate_priority_score(task)
                    result = {
                        'name': task['name'],
                        'priority_score': priority_score,
                        'status': 'completed'
                    }
                    
                    if priority_score >= 10:
                        result['urgent'] = True
                    
                    return result
                
                results = []
                failed_count = 0
                
                for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                    processed = process_single_task(task)
                    results.append(processed)
                    if processed['status'] == 'failed':
                        failed_count += 1
                
                return {
                    'results': results,
                    'total_processed': len(tasks),
                    'failed_count': failed_count,
                    'success_rate': (len(tasks) - failed_count) / len(tasks) if len(tasks) > 0 else 0
                }
        """;

        Map<String, String> beforeFiles = Map.of("manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("manager.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "execute_workflow", Visibility.PUBLIC,
                "_execute_workflow", Visibility.PROTECTED);
    }

    @Test
    void detectsVisibilityChange_WithWhileLoopsAndComplexConditions() throws Exception {
        String beforePythonCode = """
        class AlgorithmProcessor:
            def _process_algorithm(self, data_sets):
                results = {}
                index = 0
                
                while index < len(data_sets):
                    current_set = data_sets[index]
                    processed_data = []
                    
                    data_index = 0
                    while data_index < len(current_set):
                        item = current_set[data_index]
                        
                        if isinstance(item, dict):
                            temp_value = 0
                            for key, value in item.items():
                                if isinstance(value, (int, float)):
                                    if value > 0:
                                        temp_value += value * 2
                                    elif value < 0:
                                        temp_value += abs(value)
                            
                            if temp_value > 10:
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value,
                                    'category': 'high'
                                })
                            elif temp_value > 0:
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value,
                                    'category': 'low'
                                })
                        
                        data_index += 1
                    
                    if len(processed_data) > 0:
                        results[f"set_{index}"] = {
                            'data': processed_data,
                            'count': len(processed_data),
                            'avg_value': sum(item['processed_value'] for item in processed_data) / len(processed_data)
                        }
                    
                    index += 1
                
                return results
        """;

        String afterPythonCode = """
        class AlgorithmProcessor:
            def process_algorithm(self, data_sets):
                results = {}
                index = 0
                
                while index < len(data_sets):
                    current_set = data_sets[index]
                    processed_data = []
                    
                    data_index = 0
                    while data_index < len(current_set):
                        item = current_set[data_index]
                        
                        if isinstance(item, dict):
                            temp_value = 0
                            for key, value in item.items():
                                if isinstance(value, (int, float)):
                                    if value > 0:
                                        temp_value += value * 2
                                    elif value < 0:
                                        temp_value += abs(value)
                            
                            if temp_value > 10:
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value,
                                    'category': 'high'
                                })
                            elif temp_value > 0:
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value,
                                    'category': 'low'
                                })
                        
                        data_index += 1
                    
                    if len(processed_data) > 0:
                        results[f"set_{index}"] = {
                            'data': processed_data,
                            'count': len(processed_data),
                            'avg_value': sum(item['processed_value'] for item in processed_data) / len(processed_data)
                        }
                    
                    index += 1
                
                return results
        """;

        Map<String, String> beforeFiles = Map.of("algorithm.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("algorithm.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "_process_algorithm", Visibility.PROTECTED,
                "process_algorithm", Visibility.PUBLIC);
    }

    @Test
    void detectsVisibilityChange_WithComplexConditionalChains() throws Exception {
        String beforePythonCode = """
        class ScoreCalculator:
            def calculate_grades(self, student_scores):
                final_results = []
                
                for student_id, scores in student_scores.items():
                    student_result = {'student_id': student_id, 'grades': []}
                    total_weighted_score = 0
                    total_weight = 0
                    
                    for subject, score_data in scores.items():
                        raw_score = score_data.get('score', 0)
                        weight = score_data.get('weight', 1)
                        extra_credit = score_data.get('extra_credit', 0)
                        
                        # Complex grading logic
                        if raw_score >= 90:
                            letter_grade = 'A'
                            gpa_points = 4.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.1, 0.3)
                        elif raw_score >= 80:
                            letter_grade = 'B'
                            gpa_points = 3.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.08, 0.2)
                        elif raw_score >= 70:
                            letter_grade = 'C'
                            gpa_points = 2.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.05, 0.15)
                        elif raw_score >= 60:
                            letter_grade = 'D'
                            gpa_points = 1.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.03, 0.1)
                        else:
                            letter_grade = 'F'
                            gpa_points = 0.0
                        
                        # Apply weight and calculate final score
                        weighted_score = gpa_points * weight
                        total_weighted_score += weighted_score
                        total_weight += weight
                        
                        student_result['grades'].append({
                            'subject': subject,
                            'raw_score': raw_score,
                            'letter_grade': letter_grade,
                            'gpa_points': gpa_points,
                            'weight': weight,
                            'weighted_score': weighted_score
                        })
                    
                    # Calculate overall GPA
                    if total_weight > 0:
                        overall_gpa = total_weighted_score / total_weight
                        if overall_gpa >= 3.5:
                            honors = 'Summa Cum Laude'
                        elif overall_gpa >= 3.0:
                            honors = 'Magna Cum Laude'
                        elif overall_gpa >= 2.5:
                            honors = 'Cum Laude'
                        else:
                            honors = None
                        
                        student_result['overall_gpa'] = overall_gpa
                        student_result['honors'] = honors
                    
                    final_results.append(student_result)
                
                return final_results
        """;

        String afterPythonCode = """
        class ScoreCalculator:
            def __calculate_grades(self, student_scores):
                final_results = []
                
                for student_id, scores in student_scores.items():
                    student_result = {'student_id': student_id, 'grades': []}
                    total_weighted_score = 0
                    total_weight = 0
                    
                    for subject, score_data in scores.items():
                        raw_score = score_data.get('score', 0)
                        weight = score_data.get('weight', 1)
                        extra_credit = score_data.get('extra_credit', 0)
                        
                        # Complex grading logic
                        if raw_score >= 90:
                            letter_grade = 'A'
                            gpa_points = 4.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.1, 0.3)
                        elif raw_score >= 80:
                            letter_grade = 'B'
                            gpa_points = 3.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.08, 0.2)
                        elif raw_score >= 70:
                            letter_grade = 'C'
                            gpa_points = 2.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.05, 0.15)
                        elif raw_score >= 60:
                            letter_grade = 'D'
                            gpa_points = 1.0
                            if extra_credit > 0:
                                gpa_points += min(extra_credit * 0.03, 0.1)
                        else:
                            letter_grade = 'F'
                            gpa_points = 0.0
                        
                        # Apply weight and calculate final score
                        weighted_score = gpa_points * weight
                        total_weighted_score += weighted_score
                        total_weight += weight
                        
                        student_result['grades'].append({
                            'subject': subject,
                            'raw_score': raw_score,
                            'letter_grade': letter_grade,
                            'gpa_points': gpa_points,
                            'weight': weight,
                            'weighted_score': weighted_score
                        })
                    
                    # Calculate overall GPA
                    if total_weight > 0:
                        overall_gpa = total_weighted_score / total_weight
                        if overall_gpa >= 3.5:
                            honors = 'Summa Cum Laude'
                        elif overall_gpa >= 3.0:
                            honors = 'Magna Cum Laude'
                        elif overall_gpa >= 2.5:
                            honors = 'Cum Laude'
                        else:
                            honors = None
                        
                        student_result['overall_gpa'] = overall_gpa
                        student_result['honors'] = honors
                    
                    final_results.append(student_result)
                
                return final_results
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "calculate_grades", Visibility.PUBLIC,
                "__calculate_grades", Visibility.PRIVATE);
    }

    @Test
    void detectsVisibilityChange_WithAsyncAndComplexStructures() throws Exception {
        String beforePythonCode = """
        import asyncio
        
        class AsyncDataProcessor:
            async def __process_async_data(self, data_sources):
                results = {}
                semaphore = asyncio.Semaphore(5)  # Limit concurrent operations
                
                async def fetch_and_process_source(source):
                    async with semaphore:
                        try:
                            await asyncio.sleep(0.1)  # Simulate async I/O
                            
                            if source['type'] == 'database':
                                # Simulate database query processing
                                data = []
                                for i in range(source.get('record_count', 10)):
                                    record = {
                                        'id': i,
                                        'value': i * source.get('multiplier', 1),
                                        'category': 'db_record'
                                    }
                                    
                                    # Complex processing logic
                                    if record['value'] % 2 == 0:
                                        record['processed'] = record['value'] ** 2
                                        record['status'] = 'even_squared'
                                    else:
                                        record['processed'] = record['value'] * 3
                                        record['status'] = 'odd_tripled'
                                    
                                    data.append(record)
                                
                                return {
                                    'source_name': source['name'],
                                    'data': data,
                                    'total_records': len(data),
                                    'avg_processed': sum(r['processed'] for r in data) / len(data) if data else 0
                                }
                            
                            elif source['type'] == 'api':
                                # Simulate API response processing
                                response_data = {
                                    'endpoints': [
                                        {'url': f"/api/data/{i}", 'response_time': i * 0.01}
                                        for i in range(source.get('endpoint_count', 5))
                                    ]
                                }
                                
                                # Process API metrics
                                total_time = sum(ep['response_time'] for ep in response_data['endpoints'])
                                avg_time = total_time / len(response_data['endpoints'])
                                
                                return {
                                    'source_name': source['name'],
                                    'api_metrics': {
                                        'total_endpoints': len(response_data['endpoints']),
                                        'total_response_time': total_time,
                                        'avg_response_time': avg_time,
                                        'status': 'healthy' if avg_time < 0.05 else 'slow'
                                    }
                                }
                            
                            else:
                                return {
                                    'source_name': source['name'],
                                    'error': f"Unknown source type: {source['type']}"
                                }
                        
                        except Exception as e:
                            return {
                                'source_name': source.get('name', 'unknown'),
                                'error': f"Processing error: {str(e)}"
                            }
                
                # Create tasks for all sources
                tasks = []
                for source in data_sources:
                    if 'name' in source and 'type' in source:
                        task = fetch_and_process_source(source)
                        tasks.append(task)
                
                # Execute all tasks concurrently
                if tasks:
                    completed_results = await asyncio.gather(*tasks, return_exceptions=True)
                    
                    for i, result in enumerate(completed_results):
                        if isinstance(result, Exception):
                            results[f"source_{i}"] = {'error': f"Task failed: {str(result)}"}
                        else:
                            source_name = result.get('source_name', f"source_{i}")
                            results[source_name] = result
                
                return {
                    'processed_sources': results,
                    'total_sources': len(data_sources),
                    'successful_sources': len([r for r in results.values() if 'error' not in r]),
                    'processing_summary': 'Async processing completed'
                }
        """;

        String afterPythonCode = """
        import asyncio
        
        class AsyncDataProcessor:
            async def _process_async_data(self, data_sources):
                results = {}
                semaphore = asyncio.Semaphore(5)  # Limit concurrent operations
                
                async def fetch_and_process_source(source):
                    async with semaphore:
                        try:
                            await asyncio.sleep(0.1)  # Simulate async I/O
                            
                            if source['type'] == 'database':
                                # Simulate database query processing
                                data = []
                                for i in range(source.get('record_count', 10)):
                                    record = {
                                        'id': i,
                                        'value': i * source.get('multiplier', 1),
                                        'category': 'db_record'
                                    }
                                    
                                    # Complex processing logic
                                    if record['value'] % 2 == 0:
                                        record['processed'] = record['value'] ** 2
                                        record['status'] = 'even_squared'
                                    else:
                                        record['processed'] = record['value'] * 3
                                        record['status'] = 'odd_tripled'
                                    
                                    data.append(record)
                                
                                return {
                                    'source_name': source['name'],
                                    'data': data,
                                    'total_records': len(data),
                                    'avg_processed': sum(r['processed'] for r in data) / len(data) if data else 0
                                }
                            
                            elif source['type'] == 'api':
                                # Simulate API response processing
                                response_data = {
                                    'endpoints': [
                                        {'url': f"/api/data/{i}", 'response_time': i * 0.01}
                                        for i in range(source.get('endpoint_count', 5))
                                    ]
                                }
                                
                                # Process API metrics
                                total_time = sum(ep['response_time'] for ep in response_data['endpoints'])
                                avg_time = total_time / len(response_data['endpoints'])
                                
                                return {
                                    'source_name': source['name'],
                                    'api_metrics': {
                                        'total_endpoints': len(response_data['endpoints']),
                                        'total_response_time': total_time,
                                        'avg_response_time': avg_time,
                                        'status': 'healthy' if avg_time < 0.05 else 'slow'
                                    }
                                }
                            
                            else:
                                return {
                                    'source_name': source['name'],
                                    'error': f"Unknown source type: {source['type']}"
                                }
                        
                        except Exception as e:
                            return {
                                'source_name': source.get('name', 'unknown'),
                                'error': f"Processing error: {str(e)}"
                            }
                
                # Create tasks for all sources
                tasks = []
                for source in data_sources:
                    if 'name' in source and 'type' in source:
                        task = fetch_and_process_source(source)
                        tasks.append(task)
                
                # Execute all tasks concurrently
                if tasks:
                    completed_results = await asyncio.gather(*tasks, return_exceptions=True)
                    
                    for i, result in enumerate(completed_results):
                        if isinstance(result, Exception):
                            results[f"source_{i}"] = {'error': f"Task failed: {str(result)}"}
                        else:
                            source_name = result.get('source_name', f"source_{i}")
                            results[source_name] = result
                
                return {
                    'processed_sources': results,
                    'total_sources': len(data_sources),
                    'successful_sources': len([r for r in results.values() if 'error' not in r]),
                    'processing_summary': 'Async processing completed'
                }
        """;

        Map<String, String> beforeFiles = Map.of("async_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("async_processor.py", afterPythonCode);
        assertVisibilityChangeDetected(beforeFiles, afterFiles,
                "__process_async_data", Visibility.PRIVATE,
                "_process_async_data", Visibility.PROTECTED);
    }

    private void assertVisibilityChangeDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String beforeName, Visibility beforeVisibility,
            String afterName, Visibility afterVisibility) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        // Print out all operations for debugging
        System.out.println("=== BEFORE MODEL OPERATIONS ===");
        beforeUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        });

        System.out.println("=== AFTER MODEL OPERATIONS ===");
        afterUML.getClassList().forEach(umlClass -> {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        });

        // Verify operations have expected visibility in the models
        Optional<UMLOperation> beforeOperation = findOperationByName(beforeUML, beforeName);
        Optional<UMLOperation> afterOperation = findOperationByName(afterUML, afterName);

        assertTrue(beforeOperation.isPresent(), "Operation not found in before model: " + beforeName);
        assertTrue(afterOperation.isPresent(), "Operation not found in after model: " + afterName);

        assertEquals(beforeVisibility, beforeOperation.get().getVisibility(),
                "Before operation should have " + beforeVisibility + " visibility");
        assertEquals(afterVisibility, afterOperation.get().getVisibility(),
                "After operation should have " + afterVisibility + " visibility");

        // Check for the visibility change refactoring
        UMLModelDiff diff = beforeUML.diff(afterUML);
        System.out.println("Refactoring type: " + diff.getRefactorings().get(0).getRefactoringType());
        boolean visibilityChangeDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof ChangeOperationAccessModifierRefactoring visibilityRef) {
                        return visibilityRef.getOperationBefore().getName().equals(beforeName) &&
                                visibilityRef.getOperationAfter().getName().equals(afterName) &&
                                ((UMLOperation) visibilityRef.getOperationBefore()).getVisibility().equals(beforeVisibility) &&
                                ((UMLOperation) visibilityRef.getOperationAfter()).getVisibility().equals(afterVisibility);
                    }
                    return false;
                });

        System.out.println("==== DIFF ====");
        System.out.println("Visibility change detected: " + visibilityChangeDetected);
        System.out.println("Total refactorings: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(System.out::println);

        assertTrue(visibilityChangeDetected,
                "Expected visibility change refactoring from " + beforeVisibility +
                        " to " + afterVisibility + " for operation " + beforeName);
    }

    private Optional<UMLOperation> findOperationByName(UMLModel model, String operationName) {
        return model.getClassList().stream()
                .flatMap(umlClass -> umlClass.getOperations().stream())
                .filter(operation -> operation.getName().equals(operationName))
                .findFirst();
    }

}
