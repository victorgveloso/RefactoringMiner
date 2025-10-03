package org.refactoringminer.test.python.refactorings.attribute;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.test.python.refactorings.DiffLogger.logExtractAttributeSpecific;
import static org.refactoringminer.test.python.refactorings.DiffLogger.logFullDiffAnalysis;

@Isolated
public class ExtractAttributeRefactoringDetectionTest {

    @Test
    void detectsExtractAttribute_SimpleConstant() throws Exception {
        String beforePythonCode = """
        class Calculator:
        
            def __init__(self):
                pass
        
            def add_tax(self, amount):
                tax_rate = 0.10
                return amount * tax_rate
            
            def calculate_tax(self, price):
                tax_rate = 0.10
                return price * tax_rate
        """;

        String afterPythonCode = """
        class Calculator:
            def __init__(self):
                self.tax_rate = 0.10
            
            def add_tax(self, amount):
                return amount * self.tax_rate
            
            def calculate_tax(self, price):
                return price * self.tax_rate
        """;

        Map<String, String> beforeFiles = Map.of("calculator.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("calculator.py", afterPythonCode);

        assertExtractAttributeRefactoringDetectedWithDebug(beforeFiles, afterFiles,
                "tax_rate", "0.10", "Calculator");
    }

    @Test
    void detectsExtractAttribute_ConstantToClassAttribute() throws Exception {
        String beforePythonCode = """
            class Circle:
                def __init__(self, radius):
                    self.radius = radius
                
                def calculate_area(self):
                    pi = 3.14159
                    return pi * self.radius * self.radius
                
                def calculate_circumference(self):
                    pi = 3.14159
                    return 2 * pi * self.radius
            """;

        String afterPythonCode = """
            class Circle:
                def __init__(self, radius):
                    self.radius = radius
                    self.pi = 3.14159
                
                def calculate_area(self):
                    return self.pi * self.radius * self.radius
                
                def calculate_circumference(self):
                    return 2 * self.pi * self.radius
            """;

        Map<String, String> beforeFiles = Map.of("circle.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("circle.py", afterPythonCode);

        assertExtractAttributeRefactoringDetectedWithDebug(beforeFiles, afterFiles,
                "pi", "3.14159", "Circle");
    }

    @Test
    void detectsExtractAttribute_ConfigurationValue() throws Exception {
        String beforePythonCode = """
            class DatabaseConnection:
                def __init__(self, host):
                    self.host = host
                
                def connect(self):
                    timeout = 30
                    return self.establish_connection(timeout)
                
                def reconnect(self):
                    timeout = 30
                    return self.establish_connection(timeout)
            """;

        String afterPythonCode = """
            class DatabaseConnection:
                def __init__(self, host):
                    self.host = host
                    self.timeout = 30
                
                def connect(self):
                    return self.establish_connection(self.timeout)
                
                def reconnect(self):
                    return self.establish_connection(self.timeout)
            """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertExtractAttributeRefactoringDetectedWithDebug(beforeFiles, afterFiles,
                "timeout", "30", "DatabaseConnection");
    }

    public static void assertExtractAttributeRefactoringDetectedWithDebug(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String attributeName,
            String attributeValue,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        System.out.println("================================================================================");
        System.out.println("COMPREHENSIVE EXTRACT ATTRIBUTE DEBUG ANALYSIS");
        System.out.println("================================================================================");
        System.out.println("Expected: Extract " + attributeName + " = " + attributeValue + " from class " + className);
        System.out.println();

        // 1. Debug UML Model Classes
        debugUMLClasses(beforeUML, afterUML, className);

        // 2. Debug Variables in Operations
        debugOperationVariables(beforeUML, afterUML, className, attributeName, attributeValue);

        // 3. Debug Attributes
        debugClassAttributes(beforeUML, afterUML, className, attributeName);

        // 4. Perform diff and debug
        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        // 5. Debug Class Diffs
        debugClassDiffs(diff, className);

        // 6. Debug Operation Mappers
        debugOperationMappers(diff, className, attributeName);

        debugRefactoringCreationConditions(diff, className, attributeName, attributeValue);

        // 7. Debug Variable Replacement Analysis
        debugVariableReplacementAnalysis(diff, className, attributeName);

        logFullDiffAnalysis(beforeUML, afterUML, diff, "Extract Attribute Refactoring Analysis");
        logExtractAttributeSpecific(diff, attributeName, attributeValue, className);

        System.out.println("\n=== FINAL REFACTORING RESULTS ===");
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> {
            System.out.println("  " + r.getRefactoringType() + ": " + r.toString());
        });

        // Look for ExtractAttributeRefactoring
        boolean extractAttributeFound = refactorings.stream()
                .filter(r -> RefactoringType.EXTRACT_ATTRIBUTE.equals(r.getRefactoringType()))
                .anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.EXTRACT_ATTRIBUTE);

        System.out.println("Extract Attribute refactoring found: " + extractAttributeFound);
        System.out.println("================================================================================");

        assertTrue(extractAttributeFound, "Expected Extract Attribute refactoring to be detected");
    }

    private static void debugUMLClasses(UMLModel beforeUML, UMLModel afterUML, String className) {
        System.out.println("----------------------------------------");
        System.out.println("1. UML CLASSES DEBUG");
        System.out.println("----------------------------------------");

        System.out.println("BEFORE Classes:");
        beforeUML.getClassList().forEach(cls -> {
            System.out.println("  - " + cls.getName() + " (file: " + cls.getSourceFile() + ")");
            if (cls.getName().contains(className)) {
                System.out.println("    ^ TARGET CLASS FOUND");
            }
        });

        System.out.println("AFTER Classes:");
        afterUML.getClassList().forEach(cls -> {
            System.out.println("  - " + cls.getName() + " (file: " + cls.getSourceFile() + ")");
            if (cls.getName().contains(className)) {
                System.out.println("    ^ TARGET CLASS FOUND");
            }
        });

        for(UMLClass clazz : beforeUML.getClassList()) {
            for(UMLOperation op : clazz.getOperations()) {
                System.out.println("Method: " + op.getName() + " | isConstructor: " + op.isConstructor());
            }
        }

        System.out.println();
    }

    private static void debugOperationVariables(UMLModel beforeUML, UMLModel afterUML, String className, String attributeName, String attributeValue) {
        System.out.println("----------------------------------------");
        System.out.println("2. OPERATION VARIABLES DEBUG");
        System.out.println("----------------------------------------");

        UMLClass beforeClass = findClass(beforeUML, className);
        UMLClass afterClass = findClass(afterUML, className);

        if (beforeClass != null) {
            System.out.println("BEFORE Class Operations and Variables:");
            for (UMLOperation op : beforeClass.getOperations()) {
                System.out.println("  Operation: " + op.getName() + " (lines " + op.getLocationInfo().getStartLine() + "-" + op.getLocationInfo().getEndLine() + ")");
                debugOperationVariables(op, attributeName, attributeValue, "    ");
            }
        }

        if (afterClass != null) {
            System.out.println("AFTER Class Operations and Variables:");
            for (UMLOperation op : afterClass.getOperations()) {
                System.out.println("  Operation: " + op.getName() + " (lines " + op.getLocationInfo().getStartLine() + "-" + op.getLocationInfo().getEndLine() + ")");
                debugOperationVariables(op, attributeName, attributeValue, "    ");
            }
        }
        System.out.println();
    }

    private static void debugOperationVariables(UMLOperation operation, String targetVarName, String targetVarValue, String indent) {
        if (operation.getBody() != null) {
            System.out.println(indent + "All variables:");
            operation.getBody().getAllVariableDeclarations().forEach(var -> {
                System.out.println(indent + "  - " + var.getVariableName() + " = " + var.getInitializer() +
                        " (isParameter: " + var.isParameter() + ")");
                if (var.getVariableName().equals(targetVarName)) {
                    System.out.println(indent + "    ^ TARGET VARIABLE FOUND!");
                    if (var.getInitializer() != null && var.getInitializer().toString().contains(targetVarValue)) {
                        System.out.println(indent + "    ^ WITH EXPECTED VALUE!");
                    }
                }
            });

            System.out.println(indent + "Statements containing target variable:");
            debugStatementsForVariable(operation.getBody().getCompositeStatement(), targetVarName, indent + "  ");
        }
    }

    private static void debugStatementsForVariable(CompositeStatementObject composite, String targetVarName, String indent) {
        if (composite != null) {
            for (AbstractStatement stmt : composite.getStatements()) {
                String stmtStr = stmt.toString();
                if (stmtStr.contains(targetVarName)) {
                    System.out.println(indent + "Statement: " + stmtStr);
                    if (stmt instanceof StatementObject) {
                        StatementObject stmtObj = (StatementObject) stmt;
                        System.out.println(indent + "  Variables: " + stmtObj.getVariableDeclarations());
                    }
                }
                if (stmt instanceof CompositeStatementObject) {
                    debugStatementsForVariable((CompositeStatementObject) stmt, targetVarName, indent + "  ");
                }
            }
        }
    }

    private static void debugClassAttributes(UMLModel beforeUML, UMLModel afterUML, String className, String attributeName) {
        System.out.println("----------------------------------------");
        System.out.println("3. CLASS ATTRIBUTES DEBUG");
        System.out.println("----------------------------------------");

        UMLClass beforeClass = findClass(beforeUML, className);
        UMLClass afterClass = findClass(afterUML, className);

        if (beforeClass != null) {
            System.out.println("BEFORE Class Attributes:");
            for (UMLAttribute attr : beforeClass.getAttributes()) {
                System.out.println("  - " + attr.getName() + " : " + attr.getType() + " = " + attr.getVariableDeclaration().getInitializer());
            }
            if (beforeClass.getAttributes().isEmpty()) {
                System.out.println("  (no attributes)");
            }
        }

        if (afterClass != null) {
            System.out.println("AFTER Class Attributes:");
            for (UMLAttribute attr : afterClass.getAttributes()) {
                System.out.println("  - " + attr.getName() + " : " + attr.getType() + " = " + attr.getVariableDeclaration().getInitializer());
                if (attr.getName().equals(attributeName)) {
                    System.out.println("    ^ TARGET ATTRIBUTE FOUND!");
                }
            }
            if (afterClass.getAttributes().isEmpty()) {
                System.out.println("  (no attributes)");
            }
        }
        System.out.println();
    }

    private static void debugClassDiffs(UMLModelDiff diff, String className) {
        System.out.println("----------------------------------------");
        System.out.println("4. CLASS DIFFS DEBUG");
        System.out.println("----------------------------------------");

        for (UMLAbstractClassDiff classDiff : diff.getCommonClassDiffList()) {
            if (classDiff.getOriginalClass().getName().contains(className)) {
                System.out.println("Found class diff for: " + classDiff.getOriginalClass().getName());
                System.out.println("  Added attributes: " + classDiff.getAddedAttributes().size());
                classDiff.getAddedAttributes().forEach(attr -> {
                    System.out.println("    + " + attr.getName() + " = " + attr.getVariableDeclaration().getInitializer());
                });
                System.out.println("  Removed attributes: " + classDiff.getRemovedAttributes().size());
                System.out.println("  Operation body mappers: " + classDiff.getOperationBodyMapperList().size());
            }
        }
        System.out.println();
    }

    private static void debugOperationMappers(UMLModelDiff diff, String className, String attributeName) {
        System.out.println("----------------------------------------");
        System.out.println("5. OPERATION MAPPERS DEBUG");
        System.out.println("----------------------------------------");

        for (UMLAbstractClassDiff classDiff : diff.getCommonClassDiffList()) {
            if (classDiff.getOriginalClass().getName().contains(className)) {
                System.out.println("Class: " + classDiff.getOriginalClass().getName());
                for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                    System.out.println("  Mapper: " + mapper.getOperation1().getName() + " -> " + mapper.getOperation2().getName());
                    System.out.println("    Mappings: " + mapper.getMappings().size());
                    System.out.println("    Non-mapped statements from operation1: " + mapper.getNonMappedLeavesT1().size());
                    mapper.getNonMappedLeavesT1().forEach(stmt -> {
                        if (stmt.toString().contains(attributeName)) {
                            System.out.println("      - " + stmt.toString() + " (CONTAINS TARGET VARIABLE!)");
                        } else {
                            System.out.println("      - " + stmt.toString());
                        }
                    });
                    System.out.println("    Non-mapped statements from operation2: " + mapper.getNonMappedLeavesT2().size());
                    mapper.getNonMappedLeavesT2().forEach(stmt -> {
                        System.out.println("      - " + stmt.toString());
                    });

                    // Debug variable declarations
                    List<VariableDeclaration> varsOnly1 = mapper.getOperation1().getAllVariableDeclarations();
                    List<VariableDeclaration> varsOnly2 = mapper.getOperation2().getAllVariableDeclarations();
                    System.out.println("    Variables only in BEFORE: " + varsOnly1.size());
                    varsOnly1.forEach(var -> {
                        if (var.getVariableName().equals(attributeName)) {
                            System.out.println("      - " + var.getVariableName() + " = " + var.getInitializer() + " (TARGET VARIABLE!)");
                        } else {
                            System.out.println("      - " + var.getVariableName() + " = " + var.getInitializer());
                        }
                    });
                    System.out.println("    Variables only in AFTER: " + varsOnly2.size());
                    varsOnly2.forEach(var -> {
                        System.out.println("      - " + var.getVariableName() + " = " + var.getInitializer());
                    });
                }
            }
        }
        System.out.println();
    }

    private static UMLClass findClass(UMLModel model, String className) {
        return model.getClassList().stream()
                .filter(cls -> cls.getName().contains(className))
                .findFirst()
                .orElse(null);
    }

    private static void debugRefactoringCreationConditions(UMLModelDiff diff, String className, String attributeName, String attributeValue) {
        System.out.println("----------------------------------------");
        System.out.println("6. REFACTORING CREATION CONDITIONS DEBUG");
        System.out.println("----------------------------------------");

        for (UMLAbstractClassDiff classDiff : diff.getCommonClassDiffList()) {
            if (classDiff.getOriginalClass().getName().contains(className)) {
                System.out.println("Analyzing class: " + classDiff.getOriginalClass().getName());

                // Check added attributes
                System.out.println("Added attributes: " + classDiff.getAddedAttributes().size());
                for (UMLAttribute addedAttr : classDiff.getAddedAttributes()) {
                    System.out.println("  - " + addedAttr.getName() + " = " + addedAttr.getVariableDeclaration().getInitializer());

                    if (addedAttr.getName().equals(attributeName)) {
                        System.out.println("    ^ FOUND TARGET ATTRIBUTE");

                        // Check if initializer matches expected value
                        String initializer = addedAttr.getVariableDeclaration().getInitializer() != null ?
                                addedAttr.getVariableDeclaration().getInitializer().toString() : "null";
                        System.out.println("    Initializer: '" + initializer + "'");
                        System.out.println("    Expected: '" + attributeValue + "'");
                        System.out.println("    Matches: " + initializer.equals(attributeValue));

                        // Now check each operation mapper for this attribute
                        System.out.println("    Checking operation mappers for references...");
                        for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                            System.out.println("      Mapper: " + mapper.getOperation1().getName() + " -> " + mapper.getOperation2().getName());

                            // Check for variables that match our target
                            List<VariableDeclaration> varsInOp1 = mapper.getOperation1().getAllVariableDeclarations();
                            boolean foundMatchingVariable = false;

                            for (VariableDeclaration var : varsInOp1) {
                                if (var.getVariableName().equals(attributeName)) {
                                    String varInitializer = var.getInitializer() != null ? var.getInitializer().toString() : "null";
                                    System.out.println("        Found variable: " + var.getVariableName() + " = " + varInitializer);

                                    // Check if values match
                                    boolean valuesMatch = varInitializer.equals(attributeValue) ||
                                            (varInitializer.equals(initializer) && !initializer.equals("null"));
                                    System.out.println("        Values match: " + valuesMatch);

                                    if (valuesMatch) {
                                        foundMatchingVariable = true;
                                        System.out.println("        ^ MATCHING VARIABLE FOUND!");

                                        // Check statement mappings for transformation
                                        System.out.println("        Checking statement mappings...");
                                        mapper.getMappings().forEach(mapping -> {
                                            String stmt1 = mapping.getFragment1().toString();
                                            String stmt2 = mapping.getFragment2().toString();
                                            if (stmt1.contains(attributeName) || stmt2.contains(attributeName)) {
                                                System.out.println("          " + stmt1 + " -> " + stmt2);

                                                // Check if it's a proper transformation (local var to self.attr)
                                                boolean isProperTransformation = stmt1.contains(attributeName) &&
                                                        stmt2.contains("self." + attributeName);
                                                System.out.println("          Is proper transformation: " + isProperTransformation);
                                            }
                                        });
                                    }
                                }
                            }

                            System.out.println("      Found matching variable in this mapper: " + foundMatchingVariable);
                        }
                    }
                }
            }
        }
        System.out.println();
    }

    private static void debugVariableReplacementAnalysis(UMLModelDiff diff, String className, String attributeName) {
        System.out.println("----------------------------------------");
        System.out.println("7. VARIABLE REPLACEMENT ANALYSIS DEBUG");
        System.out.println("----------------------------------------");

        for (UMLAbstractClassDiff classDiff : diff.getCommonClassDiffList()) {
            if (classDiff.getOriginalClass().getName().contains(className)) {
                System.out.println("Class: " + classDiff.getOriginalClass().getName());

                // Check each added attribute
                for (UMLAttribute addedAttr : classDiff.getAddedAttributes()) {
                    if (addedAttr.getName().equals(attributeName)) {
                        System.out.println("Processing added attribute: " + addedAttr.getName());

                        // Check each operation mapper
                        for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                            System.out.println("  Checking mapper: " + mapper.getOperation1().getName() + " -> " + mapper.getOperation2().getName());

                            // Get variables from operation1 that match our attribute
                            List<VariableDeclaration> varsInOp1 = mapper.getOperation1().getAllVariableDeclarations();
                            for (VariableDeclaration var : varsInOp1) {
                                if (var.getVariableName().equals(attributeName)) {
                                    System.out.println("    Found matching variable: " + var.getVariableName() + " = " + var.getInitializer());

                                    // Check if this should trigger refactoring creation
                                    String varInitializer = var.getInitializer() != null ? var.getInitializer().toString() : "null";
                                    String attrInitializer = addedAttr.getVariableDeclaration().getInitializer() != null ?
                                            addedAttr.getVariableDeclaration().getInitializer().toString() : "null";

                                    System.out.println("    Variable initializer: '" + varInitializer + "'");
                                    System.out.println("    Attribute initializer: '" + attrInitializer + "'");
                                    System.out.println("    Should create refactoring: " + varInitializer.equals(attrInitializer));

                                    // Check statement mappings
                                    boolean hasProperMapping = mapper.getMappings().stream().anyMatch(mapping -> {
                                        String stmt1 = mapping.getFragment1().toString();
                                        String stmt2 = mapping.getFragment2().toString();
                                        return stmt1.contains(attributeName) && stmt2.contains("self." + attributeName);
                                    });
                                    System.out.println("    Has proper statement mapping: " + hasProperMapping);

                                    // This is where ExtractAttributeRefactoring should be created!
                                    System.out.println("    *** REFACTORING SHOULD BE CREATED HERE ***");
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println();
    }

    public static void assertExtractAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String attributeName,
            String attributeValue,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();
        logFullDiffAnalysis(beforeUML, afterUML, diff, "Extract Attribute Refactoring Analysis");
        logExtractAttributeSpecific(diff, attributeName, attributeValue, className);
        System.out.println("\n=== EXTRACT ATTRIBUTE TEST: " + attributeName + " ===");
        System.out.println("Attribute: " + attributeName + " = " + attributeValue);
        System.out.println("Class: " + className);
        System.out.println("Total refactorings detected: " + refactorings.size());

        // Look for ExtractAttributeRefactoring
        boolean extractAttributeFound = refactorings.stream()
                .filter(r -> RefactoringType.EXTRACT_ATTRIBUTE.equals(r.getRefactoringType()))
                .anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.EXTRACT_ATTRIBUTE);

        assertTrue(extractAttributeFound, "Expected Extract Attribute refactoring to be detected");
    }


}