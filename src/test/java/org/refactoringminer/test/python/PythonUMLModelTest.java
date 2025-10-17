package org.refactoringminer.test.python;

import gr.uom.java.xmi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.refactoringminer.utils.LangASTUtil.readResourceFile;

@Isolated
public class PythonUMLModelTest {

    @Test
    public void testProcessPythonFile() throws IOException {
        // Load a Python file
        String pythonCode = readResourceFile("python-samples/before/calculator.py");
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("calculator.py", pythonCode);

        // Create UML model
        UMLModelASTReader reader = new UMLModelASTReader(fileContents, Collections.emptySet(), false);
        UMLModel model = reader.getUmlModel();

        System.out.println("\n===== PYTHON UML MODEL =====");

        for (UMLClass umlClass : model.getClassList()) {
            System.out.println("\nClass: " + umlClass.getName());

            for (UMLOperation operation : umlClass.getOperations()) {
                System.out.println("\n  Method: " + operation.getName());
                System.out.println("  Signature: " + operation.getActualSignature());

                // Parameters (excluding return parameter)
                System.out.println("  Parameters:");
                for (UMLParameter param : operation.getParameters()) {
                    if (!param.getKind().equals("return")) {
                        System.out.println("    " + param.getName() + ": " + param.getType());
                    }
                }

                // Return type
                UMLParameter returnParam = operation.getReturnParameter();
                if (returnParam != null) {
                    System.out.println("  Return type: " + returnParam.getType());
                }

                // Method body
                if (operation.getBody() != null) {
                    System.out.println("  Body:");
                    List<String> stringRep = operation.getBody().stringRepresentation();
                    for (String line : stringRep) {
                        System.out.println("    " + line);
                    }

                    System.out.println("  Body hash code: " + operation.getBody().getBodyHashCode());
                }

                // Detailed parameter information
                System.out.println("  All parameters (including return):");
                List<UMLParameter> allParams = operation.getParameters();
                for (int i = 0; i < allParams.size(); i++) {
                    UMLParameter param = allParams.get(i);
                    System.out.println("    Parameter #" + i + ": " + param.getName() +
                            " (" + param.getType() + "), kind: " + param.getKind());
                }
            }
        }

        // Find Calculator class
        UMLClass calculatorClass = model.getClassList().stream()
                .filter(c -> c.getName().equals("calculator_module.Calculator"))
                .findFirst()
                .orElse(null);

        assertNotNull(calculatorClass, "Calculator class should be found");

        // Find sum method
        UMLOperation sumOperation = calculatorClass.getOperations().stream()
                .filter(op -> op.getName().equals("sum"))
                .findFirst()
                .orElse(null);

        assertNotNull(sumOperation, "Sum method should be found");

        // Verify parameters (should be exactly 3, plus return)
        List<UMLParameter> normalParams = sumOperation.getParameters().stream()
                .filter(p -> !p.getKind().equals("return"))
                .toList();

        assertEquals(2, normalParams.size(), "Sum method should have 3 non-return parameters");
        //assertEquals("self", normalParams.get(0).getName(), "First parameter should be 'self'");
        assertEquals("x", normalParams.get(0).getName(), "Second parameter should be 'x'");
        assertEquals("y", normalParams.get(1).getName(), "Third parameter should be 'y'");

        // Verify return parameter
        UMLParameter returnParam = sumOperation.getReturnParameter();
        assertNotNull(returnParam, "Return parameter should exist");
        assertEquals("Object", returnParam.getType().getClassType(), "Return type should be Object");
    }

    @Test
    public void testConsistentParameterMapping() throws IOException {
        // Load before and after code
        String beforeCode = readResourceFile("python-samples/before/calculator.py");
        String afterCode = readResourceFile("python-samples/after/calculator.py");

        // Create models
        Map<String, String> beforeFiles = new HashMap<>();
        beforeFiles.put("calculator.py", beforeCode);

        Map<String, String> afterFiles = new HashMap<>();
        afterFiles.put("calculator.py", afterCode);

        Set<String> repositories = Collections.emptySet();

        UMLModelASTReader beforeReader = new UMLModelASTReader(beforeFiles, repositories, false);
        UMLModel beforeModel = beforeReader.getUmlModel();

        UMLModelASTReader afterReader = new UMLModelASTReader(afterFiles, repositories, false);
        UMLModel afterModel = afterReader.getUmlModel();

        // Get Calculator classes
        UMLClass beforeCalculator = beforeModel.getClassList().stream()
                .filter(c -> c.getName().equals("calculator_module.Calculator"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Calculator class not found in before model"));

        UMLClass afterCalculator = afterModel.getClassList().stream()
                .filter(c -> c.getName().equals("calculator_module.AdvancedCalculator"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Calculator class not found in after model"));

        // Get operations
        UMLOperation addOperation = beforeCalculator.getOperations().stream()
                .filter(op -> op.getName().equals("sum"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("sum method not found"));

        UMLOperation sumOperation = afterCalculator.getOperations().stream()
                .filter(op -> op.getName().equals("sum"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("add method not found"));

        // Print and verify parameter counts
        System.out.println("SUM OPERATION:");
        printParameterDetails(addOperation);

        System.out.println("\nADD OPERATION:");
        printParameterDetails(sumOperation);

        // Verify parameter counts
        assertEquals(addOperation.getParameters().size(), sumOperation.getParameters().size(),
                "Both methods should have the same number of parameters");

        // Verify return parameter
        assertNotNull(addOperation.getReturnParameter(), "add method should have a return parameter");
        assertNotNull(sumOperation.getReturnParameter(), "sum method should have a return parameter");
        assertEquals(addOperation.getReturnParameter().getType(), sumOperation.getReturnParameter().getType(),
                "Return types should match");

        // Verify body hash codes
        if (addOperation.getBody() != null && sumOperation.getBody() != null) {
            assertEquals(addOperation.getBody().getBodyHashCode(), sumOperation.getBody().getBodyHashCode(),
                    "Body hash codes should match for identical method bodies");
        }
    }

    private void printParameterDetails(UMLOperation operation) {
        System.out.println("  Name: " + operation.getName());
        System.out.println("  Signature: " + operation.getActualSignature());
        System.out.println("  Total parameters: " + operation.getParameters().size());

        // Print all parameters including return
        System.out.println("  All parameters:");
        for (int i = 0; i < operation.getParameters().size(); i++) {
            UMLParameter param = operation.getParameters().get(i);
            System.out.println("    [" + i + "] " + param.getName() +
                    " (Type: " + param.getType() +
                    ", Kind: " + param.getKind() + ")");
        }

        // Print return parameter
        UMLParameter returnParam = operation.getReturnParameter();
        if (returnParam != null) {
            System.out.println("  Return parameter: " + returnParam.getName() +
                    " (Type: " + returnParam.getType() + ")");
        } else {
            System.out.println("  No return parameter found");
        }
    }
}