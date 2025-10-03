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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class SplitAttributeRefactoringDetectionTest {

    @Test
    void detectsSplitAttribute_FullNameToFirstLastName() throws Exception {
        String beforePythonCode = """
            class Person:
                def __init__(self, full_name):
                    self.full_name = full_name
                
                def get_full_name(self):
                    return self.full_name
                
                def update_name(self, full_name):
                    self.full_name = full_name
            """;

        String afterPythonCode = """
            class Person:
                def __init__(self, first_name, last_name):
                    self.first_name = first_name
                    self.last_name = last_name
                
                def get_full_name(self):
                    return f"{self.first_name} {self.last_name}"
                
                def update_name(self, first, last):
                    self.first_name = first
                    self.last_name = last
            """;

        Map<String, String> beforeFiles = Map.of("person.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("person.py", afterPythonCode);

        assertSplitAttributeRefactoringDetected(beforeFiles, afterFiles,
                "full_name", Set.of("first_name", "last_name"), "Person");
    }

    @Test
    void detectsSplitAttribute_PositionToCoordinates() throws Exception {
        String beforePythonCode = """
            class Point:
                def __init__(self, position):
                    self.position = position
                
                def move(self, new_position):
                    self.position = new_position
                
                def get_position(self):
                    return self.position
            """;

        String afterPythonCode = """
            class Point:
                def __init__(self, x, y):
                    self.x = x
                    self.y = y
                
                def move(self, new_x, new_y):
                    self.x = new_x
                    self.y = new_y
                
                def get_position(self):
                    return (self.x, self.y)
            """;

        Map<String, String> beforeFiles = Map.of("point.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("point.py", afterPythonCode);

        assertSplitAttributeRefactoringDetected(beforeFiles, afterFiles,
                "position", Set.of("x", "y"), "Point");
    }

    public static void assertSplitAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalAttributeName,
            Set<String> splitAttributeNames,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== SPLIT ATTRIBUTE TEST: " + originalAttributeName + " -> " + splitAttributeNames + " ===");
        System.out.println("Original attribute: " + originalAttributeName);
        System.out.println("Split attributes: " + splitAttributeNames);
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean splitAttributeFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.SPLIT_ATTRIBUTE.equals(r.getRefactoringType()) &&
                        r.toString().contains(originalAttributeName) &&
                        r.toString().contains(className));

        if (!splitAttributeFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected split attribute refactoring from attribute '" + originalAttributeName +
                    "' to attributes " + splitAttributeNames + " in class '" + className + "' was not detected");
        }

        assertTrue(splitAttributeFound, "Expected Split Attribute refactoring to be detected");
    }
}