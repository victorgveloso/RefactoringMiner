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
public class MergeAttributeRefactoringDetectionTest {

    @Test
    void detectsMergeAttribute_FirstLastNameToFullName() throws Exception {
        String beforePythonCode = """
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

        String afterPythonCode = """
            class Person:
                def __init__(self, full_name):
                    self.full_name = full_name
                
                def get_full_name(self):
                    return self.full_name
                
                def update_name(self, full_name):
                    self.full_name = full_name
            """;

        Map<String, String> beforeFiles = Map.of("person.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("person.py", afterPythonCode);

        assertMergeAttributeRefactoringDetected(beforeFiles, afterFiles,
                Set.of("first_name", "last_name"), "full_name", "Person");
    }

    @Test
    void detectsMergeAttribute_CoordinatesToPosition() throws Exception {
        String beforePythonCode = """
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

        String afterPythonCode = """
            class Point:
                def __init__(self, position):
                    self.position = position
                
                def move(self, new_position):
                    self.position = new_position
                
                def get_position(self):
                    return self.position
            """;

        Map<String, String> beforeFiles = Map.of("point.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("point.py", afterPythonCode);

        assertMergeAttributeRefactoringDetected(beforeFiles, afterFiles,
                Set.of("x", "y"), "position", "Point");
    }

    public static void assertMergeAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            Set<String> mergedAttributeNames,
            String newAttributeName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== MERGE ATTRIBUTE TEST: " + mergedAttributeNames + " -> " + newAttributeName + " ===");
        System.out.println("Merged attributes: " + mergedAttributeNames);
        System.out.println("New attribute: " + newAttributeName);
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean mergeAttributeFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.MERGE_ATTRIBUTE.equals(r.getRefactoringType()) &&
                        r.toString().contains(newAttributeName) &&
                        r.toString().contains(className));

        if (!mergeAttributeFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected merge attribute refactoring from attributes " + mergedAttributeNames +
                    " to '" + newAttributeName + "' in class '" + className + "' was not detected");
        }

        assertTrue(mergeAttributeFound, "Expected Merge Attribute refactoring to be detected");
    }
}