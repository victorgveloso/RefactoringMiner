
package org.refactoringminer.test.python.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.AddMethodModifierRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.utils.RefactoringAssertUtils.dumpOperation;

public class AddMethodModifierRefactoringDetectionTest {

    @Test
    void detectsAddStaticModifier() throws Exception {
        String beforePythonCode = """
            class Calculator:
                def compute(self, x, y):
                    return x + y
            """;

        String afterPythonCode = """
            class Calculator:
                @staticmethod
                def compute(x, y):
                    return x + y
            """;

        Map<String, String> beforeFiles = Map.of("tests/calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/calculator.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "compute", "static");
    }

    @Test
    void detectsAddAbstractModifier() throws Exception {
        String beforePythonCode = """
            class Shape:
                def area(self):
                    pass
            """;

        String afterPythonCode = """
            from abc import abstractmethod
            
            class Shape:
                @abstractmethod
                def area(self):
                    pass
            """;

        Map<String, String> beforeFiles = Map.of("tests/shape.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/shape.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "area", "abstract");
    }

    @Test
    void detectsAddStaticModifier_WithComplexMethod() throws Exception {
        String beforePythonCode = """
            class DataProcessor:
                def process_data(self, data_list):
                    results = []
                    for item in data_list:
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
                            results.append(processed_item)
                    return results
            """;

        String afterPythonCode = """
            class DataProcessor:
                @staticmethod
                def process_data(data_list):
                    results = []
                    for item in data_list:
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
                            results.append(processed_item)
                    return results
            """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "process_data", "static");
    }

    @Test
    void detectsAddAbstractModifier_WithNestedLoops() throws Exception {
        String beforePythonCode = """
            class Algorithm:
                def execute_algorithm(self, matrix):
                    results = []
                    for i in range(len(matrix)):
                        row_results = []
                        for j in range(len(matrix[i])):
                            value = matrix[i][j]
                            while value > 1:
                                if value % 2 == 0:
                                    value = value // 2
                                else:
                                    value = value * 3 + 1
                            row_results.append(value)
                        results.append(row_results)
                    return results
            """;

        String afterPythonCode = """
            from abc import abstractmethod
            
            class Algorithm:
                @abstractmethod
                def execute_algorithm(self, matrix):
                    results = []
                    for i in range(len(matrix)):
                        row_results = []
                        for j in range(len(matrix[i])):
                            value = matrix[i][j]
                            while value > 1:
                                if value % 2 == 0:
                                    value = value // 2
                                else:
                                    value = value * 3 + 1
                            row_results.append(value)
                        results.append(row_results)
                    return results
            """;

        Map<String, String> beforeFiles = Map.of("algorithm.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("algorithm.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "execute_algorithm", "abstract");
    }

    @Test
    void detectsAddStaticModifier_WithExceptionHandling() throws Exception {
        String beforePythonCode = """
            class FileHandler:
                def process_files(self, file_paths):
                    results = {}
                    for path in file_paths:
                        try:
                            with open(path, 'r') as file:
                                content = file.read()
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
                    return results
            """;

        String afterPythonCode = """
            class FileHandler:
                @staticmethod
                def process_files(file_paths):
                    results = {}
                    for path in file_paths:
                        try:
                            with open(path, 'r') as file:
                                content = file.read()
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
                    return results
            """;

        Map<String, String> beforeFiles = Map.of("file_handler.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_handler.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "process_files", "static");
    }

    @Test
    void detectsAddAbstractModifier_WithListComprehensions() throws Exception {
        String beforePythonCode = """
            class DataTransformer:
                def transform_collection(self, raw_data):
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
                    
                    return list(processed_generator)
            """;

        String afterPythonCode = """
            from abc import abstractmethod
            
            class DataTransformer:
                @abstractmethod
                def transform_collection(self, raw_data):
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
                    
                    return list(processed_generator)
            """;

        Map<String, String> beforeFiles = Map.of("transformer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("transformer.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "transform_collection", "abstract");
    }

    @Test
    void detectsAddStaticModifier_WithInnerFunctions() throws Exception {
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
                    
                    results = []
                    for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                        is_valid, error = validate_task(task)
                        if is_valid:
                            results.append({
                                'name': task['name'],
                                'priority_score': calculate_priority_score(task),
                                'status': 'completed'
                            })
                    
                    return results
            """;

        String afterPythonCode = """
            class TaskManager:
                @staticmethod
                def execute_workflow(tasks):
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
                    
                    results = []
                    for task in sorted(tasks, key=lambda t: calculate_priority_score(t), reverse=True):
                        is_valid, error = validate_task(task)
                        if is_valid:
                            results.append({
                                'name': task['name'],
                                'priority_score': calculate_priority_score(task),
                                'status': 'completed'
                            })
                    
                    return results
            """;

        Map<String, String> beforeFiles = Map.of("task_manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("task_manager.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "execute_workflow", "static");
    }

    @Test
    void detectsAddAbstractModifier_WithWhileLoops() throws Exception {
        String beforePythonCode = """
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
                                
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value
                                })
                            
                            data_index += 1
                        
                        results[f"set_{index}"] = processed_data
                        index += 1
                    
                    return results
            """;

        String afterPythonCode = """
            from abc import abstractmethod
            
            class AlgorithmProcessor:
                @abstractmethod
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
                                
                                processed_data.append({
                                    'original': item,
                                    'processed_value': temp_value
                                })
                            
                            data_index += 1
                        
                        results[f"set_{index}"] = processed_data
                        index += 1
                    
                    return results
            """;

        Map<String, String> beforeFiles = Map.of("algorithm_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("algorithm_processor.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "process_algorithm", "abstract");
    }

    @Test
    void detectsAddStaticModifier_WithComplexConditionals() throws Exception {
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
                            else:
                                letter_grade = 'F'
                                gpa_points = 0.0
                            
                            weighted_score = gpa_points * weight
                            total_weighted_score += weighted_score
                            total_weight += weight
                            
                            student_result['grades'].append({
                                'subject': subject,
                                'letter_grade': letter_grade,
                                'gpa_points': gpa_points,
                                'weighted_score': weighted_score
                            })
                        
                        if total_weight > 0:
                            overall_gpa = total_weighted_score / total_weight
                            student_result['overall_gpa'] = overall_gpa
                        
                        final_results.append(student_result)
                    
                    return final_results
            """;

        String afterPythonCode = """
            class ScoreCalculator:
                @staticmethod
                def calculate_grades(student_scores):
                    final_results = []
                    
                    for student_id, scores in student_scores.items():
                        student_result = {'student_id': student_id, 'grades': []}
                        total_weighted_score = 0
                        total_weight = 0
                        
                        for subject, score_data in scores.items():
                            raw_score = score_data.get('score', 0)
                            weight = score_data.get('weight', 1)
                            extra_credit = score_data.get('extra_credit', 0)
                            
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
                            else:
                                letter_grade = 'F'
                                gpa_points = 0.0
                            
                            weighted_score = gpa_points * weight
                            total_weighted_score += weighted_score
                            total_weight += weight
                            
                            student_result['grades'].append({
                                'subject': subject,
                                'letter_grade': letter_grade,
                                'gpa_points': gpa_points,
                                'weighted_score': weighted_score
                            })
                        
                        if total_weight > 0:
                            overall_gpa = total_weighted_score / total_weight
                            student_result['overall_gpa'] = overall_gpa
                        
                        final_results.append(student_result)
                    
                    return final_results
            """;

        Map<String, String> beforeFiles = Map.of("score_calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("score_calculator.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "calculate_grades", "static");
    }

    @Test
    void detectsAddAbstractModifier_ModuleLevelFunction() throws Exception {
        String beforePythonCode = """
            def analyze_log_patterns(log_entries):
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
            """;

        String afterPythonCode = """
            from abc import abstractmethod
            
                @abstractmethod
                def analyze_log_patterns(self, log_entries):
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
            """;

        Map<String, String> beforeFiles = Map.of("log_analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("log_analyzer.py", afterPythonCode);

        assertAddMethodModifierDetected(beforeFiles, afterFiles,
                "analyze_log_patterns", "abstract");
    }

    private void assertAddMethodModifierDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String methodName, String modifier) throws Exception {

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

        // Verify operations have expected modifiers in the models
        Optional<UMLOperation> beforeOperation = findOperationByName(beforeUML, methodName);
        Optional<UMLOperation> afterOperation = findOperationByName(afterUML, methodName);

        assertTrue(beforeOperation.isPresent(), "Operation not found in before model: " + methodName);
        assertTrue(afterOperation.isPresent(), "Operation not found in after model: " + methodName);

        // Check that the modifier was added
        boolean beforeHasModifier = checkModifier(beforeOperation.get(), modifier);
        boolean afterHasModifier = checkModifier(afterOperation.get(), modifier);

        assertEquals(false, beforeHasModifier,
                "Before operation should not have " + modifier + " modifier");
        assertEquals(true, afterHasModifier,
                "After operation should have " + modifier + " modifier");

        // Check for the add method modifier refactoring
        UMLModelDiff diff = beforeUML.diff(afterUML);
        System.out.println("Total refactorings: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(System.out::println);

        boolean addModifierDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> {
                    if (ref instanceof AddMethodModifierRefactoring addModifierRef) {
                        return addModifierRef.getOperationBefore().getName().equals(methodName) &&
                                addModifierRef.getOperationAfter().getName().equals(methodName) &&
                                addModifierRef.getModifier().equals(modifier);
                    }
                    return false;
                });

        assertTrue(addModifierDetected,
                "Expected add " + modifier + " modifier refactoring for operation " + methodName);
    }

    private boolean checkModifier(UMLOperation operation, String modifier) {
        switch (modifier) {
            case "static":
                return operation.isStatic();
            case "abstract":
                return operation.isAbstract();
            default:
                return false;
        }
    }

    private Optional<UMLOperation> findOperationByName(UMLModel model, String operationName) {
        return model.getClassList().stream()
                .flatMap(umlClass -> umlClass.getOperations().stream())
                .filter(operation -> operation.getName().equals(operationName))
                .findFirst();
    }
}