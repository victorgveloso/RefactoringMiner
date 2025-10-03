package org.refactoringminer.test.python.refactorings.clazz;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertRenameClassRefactoringDetected;
import static org.refactoringminer.utils.LangASTUtil.readResourceFile;

class RenameClassRefactoringDetectionTest {

    @Test
    void detectsClassRename() throws Exception {
        System.out.println("\n");

        // BEFORE code (Calculator)
        String beforePythonCode = """
            class Calculator:
                def sum(self, x, y):
                    x = x + y
                    return x
            """;
        // AFTER code (AdvancedCalculator)
        String afterPythonCode = """
            class AdvancedCalculator:
                def sum(self, x, y):
                    x = x + y
                    return x
            """;

        Map<String, String> beforeFiles = Map.of("tests/calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/calculator.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "Calculator", "AdvancedCalculator");
    }

    @Test
    void detectsClassRenameWithImports() throws Exception {
        System.out.println("\n");

        // BEFORE code (Calculator with imports)
        String beforePythonCode = """
        import math
        from statistics import mean, median
        import numpy as np
        
        class Calculator:
            def sum(self, x, y):
                x = x + y
                return x
        """;

        // AFTER code (AdvancedCalculator with the same imports)
        String afterPythonCode = """
        import math
        from statistics import mean, median
        import numpy as np
        
        class AdvancedCalculator:
            def sum(self, x, y):
                x = x + y
                return x
        """;

        Map<String, String> beforeFiles = Map.of("tests/calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/calculator.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "Calculator", "AdvancedCalculator");
    }

    @Test
    void detectsClassRename_GreeterToFriendlyGreeter() throws Exception {
        System.out.println("\n");

        String beforePythonCode = """
        class Greeter:
            def greet(self, name):
                return "Hello, " + name
        """;
        String afterPythonCode = """
        class FriendlyGreeter:
            def greet(self, name):
                return "Hello, " + name
        """;

        Map<String, String> beforeFiles = Map.of("tests/greeter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/greeter.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "Greeter", "FriendlyGreeter");
    }

    @Test
    void detectsClassRename_AnimalToMammal() throws Exception {
        System.out.println("\n");
        String beforePythonCode = """
        class Animal:
            def speak(self):
                return "..."
        """;
        String afterPythonCode = """
        class Mammal:
            def speak(self):
                return "..."
        """;

        Map<String, String> beforeFiles = Map.of("tests/animal.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/animal.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "Animal", "Mammal");
    }


    @Test
    void detectsClassRename_WithForLoop() throws Exception {
        System.out.println("\n");

        // BEFORE code (DataProcessor)
        String beforePythonCode = """
                class DataProcessor:
                    def process_list(self, items):
                        result = 0
                        for item in items:
                            processed = item * 2
                            result.append(processed)
                        return result
                
                    def calculate_sum(self, numbers):
                        total = 0
                        for number in numbers:
                            total = total + number
                        return total
                """;

        // AFTER code (DataHandler)
        String afterPythonCode = """
                class DataHandler:
                    def process_list(self, items):
                        result = 0
                        for item in items:
                            processed = item * 2
                        return result
                
                    def calculate_sum(self, numbers):
                        total = 0
                        for number in numbers:
                            total = total + number
                        return total
                """;

        Map<String, String> beforeFiles = Map.of("tests/data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/data_handler.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "DataProcessor", "DataHandler");
    }

    @Test
    void detectsClassRename_WithNestedControlFlow() throws Exception {
        System.out.println("\n");

        // BEFORE code (DataAnalyzer with nested loops and conditions)
        String beforePythonCode = """
        class DataAnalyzer:
            def process_data(self, data_sets):
                results = []
                for dataset in data_sets:
                    if dataset is not None:
                        processed_items = []
                        for item in dataset:
                            if item > 0:
                                processed_value = item * 2
                                processed_items.append(processed_value)
                            else:
                                processed_items.append(0)
                        results.append(processed_items)
                return results
            
            def validate_results(self, results):
                valid_count = 0
                for result_set in results:
                    for value in result_set:
                        if value >= 0:
                            valid_count += 1
                return valid_count > 0
        """;

        // AFTER code (SmartAnalyzer with same nested structure)
        String afterPythonCode = """
        class SmartAnalyzer:
            def process_data(self, data_sets):
                results = []
                for dataset in data_sets:
                    if dataset is not None:
                        processed_items = []
                        for item in dataset:
                            if item > 0:
                                processed_value = item * 2
                                processed_items.append(processed_value)
                            else:
                                processed_items.append(0)
                        results.append(processed_items)
                return results
            
            def validate_results(self, results):
                valid_count = 0
                for result_set in results:
                    for value in result_set:
                        if value >= 0:
                            valid_count += 1
                return valid_count > 0
        """;

        Map<String, String> beforeFiles = Map.of("analyzer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("analyzer.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "DataAnalyzer", "SmartAnalyzer");
    }

    @Test
    void detectsClassRename_WithWhileLoopsAndExceptionHandling() throws Exception {
        System.out.println("\n");

        // BEFORE code (FileProcessor with while loops and try-catch)
        String beforePythonCode = """
        class FileProcessor:
            def read_files(self, file_paths):
                results = {}
                index = 0
                while index < len(file_paths):
                    file_path = file_paths[index]
                    try:
                        with open(file_path, 'r') as file:
                            content = file.read()
                            line_count = 0
                            lines = content.split('\\n')
                            while line_count < len(lines):
                                if lines[line_count].strip():
                                    results[f"{file_path}_{line_count}"] = lines[line_count]
                                line_count += 1
                    except FileNotFoundError:
                        results[file_path] = "ERROR: File not found"
                    except Exception as e:
                        results[file_path] = f"ERROR: {str(e)}"
                    finally:
                        index += 1
                return results
        """;

        // AFTER code (DocumentProcessor with same structure)
        String afterPythonCode = """
        class DocumentProcessor:
            def read_files(self, file_paths):
                results = {}
                index = 0
                while index < len(file_paths):
                    file_path = file_paths[index]
                    try:
                        with open(file_path, 'r') as file:
                            content = file.read()
                            line_count = 0
                            lines = content.split('\\n')
                            while line_count < len(lines):
                                if lines[line_count].strip():
                                    results[f"{file_path}_{line_count}"] = lines[line_count]
                                line_count += 1
                    except FileNotFoundError:
                        results[file_path] = "ERROR: File not found"
                    except Exception as e:
                        results[file_path] = f"ERROR: {str(e)}"
                    finally:
                        index += 1
                return results
        """;

        Map<String, String> beforeFiles = Map.of("processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("processor.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "FileProcessor", "DocumentProcessor");
    }

    @Test
    void detectsClassRename_WithComplexConditionals() throws Exception {
        System.out.println("\n");

        // BEFORE code (ScoreCalculator with complex if-elif-else chains)
        String beforePythonCode = """
        class ScoreCalculator:
            def calculate_grade(self, scores):
                final_grades = []
                for score in scores:
                    if score >= 90:
                        grade = 'A'
                        bonus = 5
                    elif score >= 80:
                        grade = 'B'
                        bonus = 3
                    elif score >= 70:
                        grade = 'C' 
                        bonus = 1
                    elif score >= 60:
                        grade = 'D'
                        bonus = 0
                    else:
                        grade = 'F'
                        bonus = -2
                    
                    final_score = score + bonus
                    if final_score > 100:
                        final_score = 100
                    elif final_score < 0:
                        final_score = 0
                    
                    final_grades.append({
                        'original': score,
                        'grade': grade,
                        'final': final_score
                    })
                return final_grades
        """;

        // AFTER code (GradeEvaluator with same complex structure)
        String afterPythonCode = """
        class GradeEvaluator:
            def calculate_grade(self, scores):
                final_grades = []
                for score in scores:
                    if score >= 90:
                        grade = 'A'
                        bonus = 5
                    elif score >= 80:
                        grade = 'B'
                        bonus = 3
                    elif score >= 70:
                        grade = 'C' 
                        bonus = 1
                    elif score >= 60:
                        grade = 'D'
                        bonus = 0
                    else:
                        grade = 'F'
                        bonus = -2
                    
                    final_score = score + bonus
                    if final_score > 100:
                        final_score = 100
                    elif final_score < 0:
                        final_score = 0
                    
                    final_grades.append({
                        'original': score,
                        'grade': grade,
                        'final': final_score
                    })
                return final_grades
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "ScoreCalculator", "GradeEvaluator");
    }

    @Test
    void detectsClassRename_WithListComprehensionsAndGenerators() throws Exception {
        System.out.println("\n");

        // BEFORE code (DataFilter with comprehensions and generators)
        String beforePythonCode = """
        class DataFilter:
            def filter_and_transform(self, datasets):
                results = {}
                for name, data in datasets.items():
                    # List comprehension with nested conditions
                    filtered_positive = [x for x in data if x > 0 and x % 2 == 0]
                    
                    # Dictionary comprehension
                    squared_map = {x: x**2 for x in filtered_positive if x < 100}
                    
                    # Generator expression with conditions
                    cubed_generator = (x**3 for x in filtered_positive if x in squared_map)
                    cubed_values = list(cubed_generator)
                    
                    # Nested comprehension
                    matrix_data = [[y * i for y in filtered_positive] for i in range(1, 4)]
                    
                    if len(filtered_positive) > 0:
                        results[name] = {
                            'filtered': filtered_positive,
                            'squared': squared_map,
                            'cubed': cubed_values,
                            'matrix': matrix_data
                        }
                    else:
                        results[name] = {'empty': True}
                        
                return results
        """;

        // AFTER code (DataProcessor with same comprehension structure)
        String afterPythonCode = """
        class DataProcessor:
            def filter_and_transform(self, datasets):
                results = {}
                for name, data in datasets.items():
                    # List comprehension with nested conditions
                    filtered_positive = [x for x in data if x > 0 and x % 2 == 0]
                    
                    # Dictionary comprehension
                    squared_map = {x: x**2 for x in filtered_positive if x < 100}
                    
                    # Generator expression with conditions
                    cubed_generator = (x**3 for x in filtered_positive if x in squared_map)
                    cubed_values = list(cubed_generator)
                    
                    # Nested comprehension
                    matrix_data = [[y * i for y in filtered_positive] for i in range(1, 4)]
                    
                    if len(filtered_positive) > 0:
                        results[name] = {
                            'filtered': filtered_positive,
                            'squared': squared_map,
                            'cubed': cubed_values,
                            'matrix': matrix_data
                        }
                    else:
                        results[name] = {'empty': True}
                        
                return results
        """;

        Map<String, String> beforeFiles = Map.of("filter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("filter.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "DataFilter", "DataProcessor");
    }

    @Test
    void detectsClassRename_WithFunctionDefinitionsAndDecorators() throws Exception {
        System.out.println("\n");

        // BEFORE code (TaskManager with inner functions and decorators)
        String beforePythonCode = """
        class TaskManager:
            def execute_tasks(self, tasks):
                def validate_task(task):
                    if not task or 'name' not in task:
                        return False
                    return True
                
                def process_task(task):
                    result = {'name': task['name'], 'status': 'processing'}
                    if 'priority' in task:
                        if task['priority'] == 'high':
                            result['urgency'] = 1
                        elif task['priority'] == 'medium':
                            result['urgency'] = 2
                        else:
                            result['urgency'] = 3
                    return result
                
                executed_tasks = []
                failed_tasks = []
                
                for task in tasks:
                    if validate_task(task):
                        try:
                            processed = process_task(task)
                            if processed['urgency'] <= 2:
                                processed['status'] = 'completed'
                            executed_tasks.append(processed)
                        except Exception as e:
                            failed_tasks.append({'task': task, 'error': str(e)})
                    else:
                        failed_tasks.append({'task': task, 'error': 'Invalid task format'})
                
                return {
                    'successful': executed_tasks,
                    'failed': failed_tasks,
                    'total': len(tasks)
                }
        """;

        // AFTER code (JobScheduler with same inner function structure)
        String afterPythonCode = """
        class JobScheduler:
            def execute_tasks(self, tasks):
                def validate_task(task):
                    if not task or 'name' not in task:
                        return False
                    return True
                
                def process_task(task):
                    result = {'name': task['name'], 'status': 'processing'}
                    if 'priority' in task:
                        if task['priority'] == 'high':
                            result['urgency'] = 1
                        elif task['priority'] == 'medium':
                            result['urgency'] = 2
                        else:
                            result['urgency'] = 3
                    return result
                
                executed_tasks = []
                failed_tasks = []
                
                for task in tasks:
                    if validate_task(task):
                        try:
                            processed = process_task(task)
                            if processed['urgency'] <= 2:
                                processed['status'] = 'completed'
                            executed_tasks.append(processed)
                        except Exception as e:
                            failed_tasks.append({'task': task, 'error': str(e)})
                    else:
                        failed_tasks.append({'task': task, 'error': 'Invalid task format'})
                
                return {
                    'successful': executed_tasks,
                    'failed': failed_tasks,
                    'total': len(tasks)
                }
        """;

        Map<String, String> beforeFiles = Map.of("manager.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("manager.py", afterPythonCode);
        assertRenameClassRefactoringDetected(beforeFiles, afterFiles, "TaskManager", "JobScheduler");
    }



}