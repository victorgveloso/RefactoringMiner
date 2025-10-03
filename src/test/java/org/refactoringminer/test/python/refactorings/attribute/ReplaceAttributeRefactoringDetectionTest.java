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
public class ReplaceAttributeRefactoringDetectionTest {

    @Test
    void detectsReplaceAttribute_SimpleRename() throws Exception {
        String beforePythonCode = """
            class Person:
                def __init__(self, name):
                    self.full_name = name
                
                def get_info(self):
                    return f"Person: {self.full_name}"
            """;

        String afterPythonCode = """
            class Person:
                def __init__(self, name):
                    self.name = name
                
                def get_info(self):
                    return f"Person: {self.name}"
            """;

        Map<String, String> beforeFiles = Map.of("person.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("person.py", afterPythonCode);

        assertReplaceAttributeRefactoringDetected(beforeFiles, afterFiles,
                "full_name", "name", "Person");
    }

    @Test
    void detectsReplaceAttribute_DirectToProperty() throws Exception {
        String beforePythonCode = """
            class Circle:
                def __init__(self, radius):
                    self.radius = radius
                    self.area = 3.14159 * radius * radius
                
                def get_area(self):
                    return self.area
            """;

        String afterPythonCode = """
            class Circle:
                def __init__(self, radius):
                    self.radius = radius
                
                @property
                def area(self):
                    return 3.14159 * self.radius * self.radius
                
                def get_area(self):
                    return self.area
            """;

        Map<String, String> beforeFiles = Map.of("circle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("circle.py", afterPythonCode);

        assertReplaceAttributeRefactoringDetected(beforeFiles, afterFiles,
                "area", "area", "Circle");
    }

    public static void assertReplaceAttributeRefactoringDetected(
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

        System.out.println("\n=== REPLACE ATTRIBUTE TEST: " + oldAttributeName + " -> " + newAttributeName + " ===");
        System.out.println("Old attribute: " + oldAttributeName);
        System.out.println("New attribute: " + newAttributeName);
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        // Look for Replace Attribute refactoring
        boolean replaceAttributeFound = false;

        // Primary check: Look for REPLACE_ATTRIBUTE refactoring type
        replaceAttributeFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.REPLACE_ATTRIBUTE.equals(r.getRefactoringType()) &&
                        r.toString().contains(oldAttributeName) &&
                        r.toString().contains(newAttributeName));

        if (!replaceAttributeFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected replace attribute refactoring from '" + oldAttributeName +
                    "' to '" + newAttributeName + "' in class '" + className + "' was not detected");
        }

        assertTrue(replaceAttributeFound, "Expected Replace Attribute refactoring to be detected");
    }
}