package org.refactoringminer.test.python.refactorings.attribute;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class RenameAttributeRefactoringDetectionTest {

    @Test
    void detectsRenameAttribute_SimpleRename() throws Exception {
        String beforePythonCode = """
            class Person:
                def __init__(self, name):
                    self.full_name = name
                
                def get_info(self):
                    return f"Person: {self.full_name}"
                
                def update_name(self, new_name):
                    self.full_name = new_name
            """;

        String afterPythonCode = """
            class Person:
                def __init__(self, name):
                    self.name = name
                
                def get_info(self):
                    return f"Person: {self.name}"
                
                def update_name(self, new_name):
                    self.name = new_name
            """;

        Map<String, String> beforeFiles = Map.of("person.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("person.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "full_name", "name", "Person");
    }

    @Test
    void detectsRenameAttribute_PrivateToPublic() throws Exception {
        String beforePythonCode = """
            class BankAccount:
                def __init__(self, balance):
                    self._balance = balance
                
                def get_balance(self):
                    return self._balance
                
                def deposit(self, amount):
                    self._balance += amount
            """;

        String afterPythonCode = """
            class BankAccount:
                def __init__(self, balance):
                    self.balance = balance
                
                def get_balance(self):
                    return self.balance
                
                def deposit(self, amount):
                    self.balance += amount
            """;

        Map<String, String> beforeFiles = Map.of("bank.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("bank.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "_balance", "balance", "BankAccount");
    }

    @Test
    void detectsRenameAttribute_ImprovingNaming() throws Exception {
        String beforePythonCode = """
            class Rectangle:
                def __init__(self, width, height):
                    self.w = width
                    self.h = height
                
                def area(self):
                    return self.w * self.h
                
                def perimeter(self):
                    return 2 * (self.w + self.h)
            """;

        String afterPythonCode = """
            class Rectangle:
                def __init__(self, width, height):
                    self.width = width
                    self.h = h
                
                def area(self):
                    return self.width * self.height
                
                def perimeter(self):
                    return 2 * (self.width + self.height)
            """;

        Map<String, String> beforeFiles = Map.of("rectangle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("rectangle.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "w", "width", "Rectangle");
    }

    @Test
    void detectsRenameAttribute_DatabaseFields() throws Exception {
        String beforePythonCode = """
        class User:
            def __init__(self, email, password):
                self.usr_email = email
                self.pwd = password
            
            def login(self):
                return f"Login with {self.usr_email}"
            
            def change_password(self, new_pwd):
                self.pwd = new_pwd
                return "Password changed"
        """;

        String afterPythonCode = """
        class User:
            def __init__(self, email, password):
                self.email = email
                self.pwd = password
            
            def login(self):
                return f"Login with {self.email}"
            
            def change_password(self, new_pwd):
                self.password = new_pwd
                return "Password changed"
        """;

        Map<String, String> beforeFiles = Map.of("user.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "usr_email", "email", "User");
    }

    @Test
    void detectsRenameAttribute_ConfigurationSettings() throws Exception {
        String beforePythonCode = """
        class AppConfig:
            def __init__(self):
                self.db_host = "localhost"
                self.db_port = 5432
            
            def get_connection_string(self):
                return f"postgresql://{self.db_host}:{self.db_port}"
            
            def update_database_host(self, host):
                self.db_host = host
        """;

        String afterPythonCode = """
        class AppConfig:
            def __init__(self):
                self.database_host = "localhost"
                self.db_port = 5432
            
            def get_connection_string(self):
                return f"postgresql://{self.database_host}:{self.db_port}"
            
            def update_database_host(self, host):
                self.database_host = host
        """;

        Map<String, String> beforeFiles = Map.of("config.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("config.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "db_host", "database_host", "AppConfig");
    }

    @Test
    void detectsRenameAttribute_MathematicalOperations() throws Exception {
        String beforePythonCode = """
        class Circle:
            def __init__(self, radius):
                self.r = radius
            
            def calculate_area(self):
                return 3.14159 * self.r * self.r
            
            def calculate_circumference(self):
                return 2 * 3.14159 * self.r
            
            def scale(self, factor):
                self.r *= factor
        """;

        String afterPythonCode = """
        class Circle:
            def __init__(self, radius):
                self.radius = radius
            
            def calculate_area(self):
                return 3.14159 * self.radius * self.radius
            
            def calculate_circumference(self):
                return 2 * 3.14159 * self.radius
            
            def scale(self, factor):
                self.radius *= factor
        """;

        Map<String, String> beforeFiles = Map.of("circle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("circle.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "r", "radius", "Circle");
    }

    @Test
    void detectsRenameAttribute_FileProcessing() throws Exception {
        String beforePythonCode = """
        class FileProcessor:
            def __init__(self, filename):
                self.fname = filename
                self.data = []
            
            def load_file(self):
                with open(self.fname, 'r') as f:
                    self.data = f.readlines()
            
            def get_filename(self):
                return self.fname
            
            def process_lines(self):
                for line in self.data:
                    print(f"Processing line from {self.fname}")
        """;

        String afterPythonCode = """
        class FileProcessor:
            def __init__(self, filename):
                self.filename = filename
                self.data = []
            
            def load_file(self):
                with open(self.filename, 'r') as f:
                    self.data = f.readlines()
            
            def get_filename(self):
                return self.filename
            
            def process_lines(self):
                for line in self.data:
                    print(f"Processing line from {self.filename}")
        """;

        Map<String, String> beforeFiles = Map.of("file_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("file_processor.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "fname", "filename", "FileProcessor");
    }

    @Test
    void detectsRenameAttribute_CounterWithLoop() throws Exception {
        String beforePythonCode = """
        class ItemCounter:
            def __init__(self):
                self.cnt = 0
                self.items = []
            
            def add_item(self, item):
                self.items.append(item)
                self.cnt += 1
            
            def count_active_items(self):
                active_count = 0
                for item in self.items:
                    if item.get('active', True):
                        active_count += 1
                return active_count
            
            def get_total_count(self):
                return self.cnt
        """;

        String afterPythonCode = """
        class ItemCounter:
            def __init__(self):
                self.count = 0
                self.items = []
            
            def add_item(self, item):
                self.items.append(item)
                self.count += 1
            
            def count_active_items(self):
                active_count = 0
                for item in self.items:
                    if item.get('active', True):
                        active_count += 1
                return active_count
            
            def get_total_count(self):
                return self.count
        """;

        Map<String, String> beforeFiles = Map.of("counter.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("counter.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "cnt", "count", "ItemCounter");
    }

    @Test
    void detectsRenameAttribute_GameScore() throws Exception {
        String beforePythonCode = """
        class Player:
            def __init__(self, name):
                self.player_name = name
                self.pts = 0
                self.level = 1
            
            def earn_points(self, points):
                self.pts += points
                if self.pts >= 100:
                    self.level += 1
                    self.pts = 0
            
            def get_status(self):
                return f"{self.player_name}: Level {self.level}, Points {self.pts}"
            
            def reset_score(self):
                self.pts = 0
                self.level = 1
        """;

        String afterPythonCode = """
        class Player:
            def __init__(self, name):
                self.player_name = name
                self.points = 0
                self.level = 1
            
            def earn_points(self, points):
                self.points += points
                if self.points >= 100:
                    self.level += 1
                    self.points = 0
            
            def get_status(self):
                return f"{self.player_name}: Level {self.level}, Points {self.points}"
            
            def reset_score(self):
                self.points = 0
                self.level = 1
        """;

        Map<String, String> beforeFiles = Map.of("player.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("player.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "pts", "points", "Player");
    }

    @Test
    void detectsRenameAttribute_DataProcessor() throws Exception {
        String beforePythonCode = """
        class DataProcessor:
            def __init__(self):
                self.raw_data = []
                self.processed_cnt = 0
            
            def add_data(self, data):
                self.raw_data.append(data)
            
            def process_all_data(self):
                index = 0
                while index < len(self.raw_data):
                    item = self.raw_data[index]
                    if self.validate_item(item):
                        self.processed_cnt += 1
                    index += 1
            
            def validate_item(self, item):
                return item is not None and len(str(item)) > 0
            
            def get_processed_count(self):
                return self.processed_cnt
        """;

        String afterPythonCode = """
        class DataProcessor:
            def __init__(self):
                self.raw_data = []
                self.processed_count = 0
            
            def add_data(self, data):
                self.raw_data.append(data)
            
            def process_all_data(self):
                index = 0
                while index < len(self.raw_data):
                    item = self.raw_data[index]
                    if self.validate_item(item):
                        self.processed_count += 1
                    index += 1
            
            def validate_item(self, item):
                return item is not None and len(str(item)) > 0
            
            def get_processed_count(self):
                return self.processed_count
        """;

        Map<String, String> beforeFiles = Map.of("data_processor.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("data_processor.py", afterPythonCode);

        assertRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "processed_cnt", "processed_count", "DataProcessor");
    }

    public static void assertRenameAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String oldAttributeName,
            String newAttributeName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== RENAME ATTRIBUTE TEST: " + oldAttributeName + " -> " + newAttributeName + " ===");
        System.out.println("Old attribute: " + oldAttributeName);
        System.out.println("New attribute: " + newAttributeName);
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean renameAttributeFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.RENAME_ATTRIBUTE.equals(r.getRefactoringType()) &&
                        r.toString().contains(oldAttributeName) &&
                        r.toString().contains(newAttributeName) &&
                        r.toString().contains(className));

        if (!renameAttributeFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected rename attribute refactoring from '" + oldAttributeName +
                    "' to '" + newAttributeName + "' in class '" + className + "' was not detected");
        }

        assertTrue(renameAttributeFound, "Expected Rename Attribute refactoring to be detected");
    }
}