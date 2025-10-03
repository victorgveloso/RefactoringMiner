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
public class MoveAndRenameAttributeRefactoringDetectionTest {

    @Test
    void detectsMoveAndRenameAttribute_BetweenClasses() throws Exception {
        String beforePythonCode = """
            class Person:
                def __init__(self, name, age):
                    self.full_name = name
                    self.age = age
                
                def get_info(self):
                    return f"{self.full_name} is {self.age} years old"
            
            class Employee:
                def __init__(self, emp_id):
                    self.emp_id = emp_id
            """;

        String afterPythonCode = """
            class Person:
                def __init__(self, name, age):
                    self.age = age
                
                def get_info(self):
                    return f"{self.age} years old"
            
            class Employee:
                def __init__(self, emp_id, name):
                    self.emp_id = emp_id
                    self.name = name
                
                def get_employee_name(self):
                    return self.name
            """;

        Map<String, String> beforeFiles = Map.of("entities.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("entities.py", afterPythonCode);

        assertMoveAndRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "full_name", "name", "Person", "Employee");
    }

    @Test
    void detectsMoveAndRenameAttribute_ToNestedClass() throws Exception {
        String beforePythonCode = """
            class Company:
                def __init__(self, company_name):
                    self.company_name = company_name
                    self.company_address = "123 Main St"
                
                def get_details(self):
                    return f"{self.company_name} at {self.company_address}"
            """;

        String afterPythonCode = """
            class Company:
                def __init__(self, company_name):
                    self.company_name = company_name
                    self.location = CompanyLocation("123 Main St")
                
                def get_details(self):
                    return f"{self.company_name} at {self.location.address}"
            
            class CompanyLocation:
                def __init__(self, address):
                    self.address = address
            """;

        Map<String, String> beforeFiles = Map.of("company.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("company.py", afterPythonCode);

        assertMoveAndRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "company_address", "address", "Company", "CompanyLocation");
    }

    @Test
    void detectsMoveAndRenameAttribute_InheritanceHierarchy() throws Exception {
        String beforePythonCode = """
            class Vehicle:
                def __init__(self, make, model):
                    self.vehicle_make = make
                    self.model = model
                
                def get_info(self):
                    return f"{self.vehicle_make} {self.model}"
            
            class Car(Vehicle):
                def __init__(self, make, model, doors):
                    super().__init__(make, model)
                    self.doors = doors
            """;

        String afterPythonCode = """
            class Vehicle:
                def __init__(self, model):
                    self.model = model
                
                def get_info(self):
                    return f"{self.model}"
            
            class Car(Vehicle):
                def __init__(self, make, model, doors):
                    super().__init__(model)
                    self.manufacturer = make
                    self.doors = doors
                
                def get_car_info(self):
                    return f"{self.manufacturer} {self.model}"
            """;

        Map<String, String> beforeFiles = Map.of("vehicles.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("vehicles.py", afterPythonCode);

        assertMoveAndRenameAttributeRefactoringDetected(beforeFiles, afterFiles,
                "vehicle_make", "manufacturer", "Vehicle", "Car");
    }

    public static void assertMoveAndRenameAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String oldAttributeName,
            String newAttributeName,
            String sourceClassName,
            String targetClassName
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== MOVE AND RENAME ATTRIBUTE TEST: " + oldAttributeName + " -> " + newAttributeName + " ===");
        System.out.println("Old attribute: " + oldAttributeName + " in " + sourceClassName);
        System.out.println("New attribute: " + newAttributeName + " in " + targetClassName);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean moveAndRenameAttributeFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.MOVE_RENAME_ATTRIBUTE.equals(r.getRefactoringType()) &&
                        r.toString().contains(oldAttributeName) &&
                        r.toString().contains(newAttributeName) &&
                        r.toString().contains(sourceClassName) &&
                        r.toString().contains(targetClassName));

        if (!moveAndRenameAttributeFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected move and rename attribute refactoring from '" + oldAttributeName +
                    "' in class '" + sourceClassName + "' to '" + newAttributeName +
                    "' in class '" + targetClassName + "' was not detected");
        }

        assertTrue(moveAndRenameAttributeFound, "Expected Move and Rename Attribute refactoring to be detected");
    }
}