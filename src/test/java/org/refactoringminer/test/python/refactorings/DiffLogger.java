package org.refactoringminer.test.python.refactorings;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiffLogger {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUB_SEPARATOR = "-".repeat(40);

    public static void logFullDiffAnalysis(UMLModel beforeModel, UMLModel afterModel,
                                           UMLModelDiff diff, String testName) {
        System.out.println("\n" + SEPARATOR);
        System.out.println("FULL DIFF ANALYSIS: " + testName);
        System.out.println(SEPARATOR);

        logModelComparison(beforeModel, afterModel);
        logClassChanges(diff);
        logAttributeChanges(diff);
        logOperationChanges(diff);
        logRefactorings(diff);
        logDetailedRefactoringAnalysis(diff);

        System.out.println(SEPARATOR);
        System.out.println("END DIFF ANALYSIS: " + testName);
        System.out.println(SEPARATOR + "\n");
    }

    private static void logModelComparison(UMLModel beforeModel, UMLModel afterModel) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("MODEL COMPARISON");
        System.out.println(SUB_SEPARATOR);

        System.out.println("BEFORE MODEL:");
        System.out.println("  Classes: " + beforeModel.getClassList().size());
        beforeModel.getClassList().forEach(clazz -> {
            System.out.println("    - " + clazz.getName() + " (" + clazz.getSourceFile() + ")");
            System.out.println("      Attributes: " + clazz.getAttributes().size());
            clazz.getAttributes().forEach(attr ->
                    System.out.println("        * " + attr.getName() + " : " + attr.getType() +
                            " (line " + attr.getLocationInfo().getStartLine() + ")"));
            System.out.println("      Operations: " + clazz.getOperations().size());
            clazz.getOperations().forEach(op ->
                    System.out.println("        * " + op.getName() + op.getParameterTypeList() +
                            " (lines " + op.getLocationInfo().getStartLine() + "-" +
                            op.getLocationInfo().getEndLine() + ")"));
        });

        System.out.println("\nAFTER MODEL:");
        System.out.println("  Classes: " + afterModel.getClassList().size());
        afterModel.getClassList().forEach(clazz -> {
            System.out.println("    - " + clazz.getName() + " (" + clazz.getSourceFile() + ")");
            System.out.println("      Attributes: " + clazz.getAttributes().size());
            clazz.getAttributes().forEach(attr ->
                    System.out.println("        * " + attr.getName() + " : " + attr.getType() +
                            " (line " + attr.getLocationInfo().getStartLine() + ")"));
            System.out.println("      Operations: " + clazz.getOperations().size());
            clazz.getOperations().forEach(op ->
                    System.out.println("        * " + op.getName() + op.getParameterTypeList() +
                            " (lines " + op.getLocationInfo().getStartLine() + "-" +
                            op.getLocationInfo().getEndLine() + ")"));
        });
    }

    private static void logClassChanges(UMLModelDiff diff) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("CLASS CHANGES");
        System.out.println(SUB_SEPARATOR);

        System.out.println("Added Classes: " + diff.getAddedClasses().size());
        diff.getAddedClasses().forEach(clazz ->
                System.out.println("  + " + clazz.getName()));

        System.out.println("Removed Classes: " + diff.getRemovedClasses().size());
        diff.getRemovedClasses().forEach(clazz ->
                System.out.println("  - " + clazz.getName()));

        System.out.println("Modified Classes: " + diff.getCommonClassDiffList().size());
        diff.getCommonClassDiffList().forEach(classDiff ->
                System.out.println("  ~ " + classDiff.getOriginalClass().getName()));
    }

    private static void logAttributeChanges(UMLModelDiff diff) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("ATTRIBUTE CHANGES");
        System.out.println(SUB_SEPARATOR);

        diff.getCommonClassDiffList().forEach(classDiff -> {
            if (!classDiff.getAddedAttributes().isEmpty() ||
                    !classDiff.getRemovedAttributes().isEmpty() ||
                    !classDiff.getAttributeDiffList().isEmpty()) {

                System.out.println("Class: " + classDiff.getOriginalClass().getName());

                System.out.println("  Added Attributes: " + classDiff.getAddedAttributes().size());
                classDiff.getAddedAttributes().forEach(attr ->
                        System.out.println("    + " + attr.getName() + " : " + attr.getType() +
                                " = " + (attr.getVariableDeclaration().getInitializer() != null ?
                                attr.getVariableDeclaration().getInitializer().toString() : "null") +
                                " (line " + attr.getLocationInfo().getStartLine() + ")"));

                System.out.println("  Removed Attributes: " + classDiff.getRemovedAttributes().size());
                classDiff.getRemovedAttributes().forEach(attr ->
                        System.out.println("    - " + attr.getName() + " : " + attr.getType() +
                                " = " + (attr.getVariableDeclaration().getInitializer() != null ?
                                attr.getVariableDeclaration().getInitializer().toString() : "null") +
                                " (line " + attr.getLocationInfo().getStartLine() + ")"));

                System.out.println("  Modified Attributes: " + classDiff.getAttributeDiffList().size());
                classDiff.getAttributeDiffList().forEach(attrDiff ->
                        System.out.println("    ~ " + attrDiff.getRemovedAttribute().getName() +
                                " -> " + attrDiff.getAddedAttribute().getName()));
            }
        });
    }

    public static void logExtractAttributeSpecific(UMLModelDiff diff, String expectedAttributeName,
                                                   String expectedValue, String expectedClassName) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("EXTRACT ATTRIBUTE SPECIFIC ANALYSIS");
        System.out.println(SUB_SEPARATOR);

        System.out.println("Looking for Extract Attribute refactoring:");
        System.out.println("  Expected attribute: " + expectedAttributeName);
        System.out.println("  Expected value: " + expectedValue);
        System.out.println("  Expected class: " + expectedClassName);

        try {
            List<Refactoring> refactorings = diff.getRefactorings();

            // Check if we have any Extract Attribute refactorings
            List<Refactoring> extractAttributeRefactorings = refactorings.stream()
                    .filter(r -> r.getRefactoringType() == RefactoringType.EXTRACT_ATTRIBUTE)
                    .collect(Collectors.toList());

            System.out.println("\nFound " + extractAttributeRefactorings.size() + " Extract Attribute refactorings:");
            extractAttributeRefactorings.forEach(refactoring -> {
                System.out.println("  - " + refactoring.toString());
                if (refactoring instanceof ExtractAttributeRefactoring) {
                    ExtractAttributeRefactoring extractAttr = (ExtractAttributeRefactoring) refactoring;
                    System.out.println("    Attribute: " + extractAttr.getVariableDeclaration().getName());
                    System.out.println("    Class: " + extractAttr.getVariableDeclaration().getClassName());
                    System.out.println("    References: " + extractAttr.getReferences().size());
                }
            });

            // Look for patterns that should trigger Extract Attribute
            System.out.println("\nLooking for potential Extract Attribute patterns...");

            diff.getCommonClassDiffList().forEach(classDiff -> {
                if (classDiff.getOriginalClass().getName().equals(expectedClassName)) {
                    System.out.println("Analyzing target class: " + expectedClassName);

                    // Check if attribute was added
                    boolean attributeAdded = classDiff.getAddedAttributes().stream()
                            .anyMatch(attr -> attr.getName().equals(expectedAttributeName));
                    System.out.println("  Attribute '" + expectedAttributeName + "' added: " + attributeAdded);

                    // Show ALL operations in both versions
                    System.out.println("\n  BEFORE operations:");
                    classDiff.getOriginalClass().getOperations().forEach(op -> {
                        System.out.println("    - " + op.getName() + " (lines " +
                                op.getLocationInfo().getStartLine() + "-" + op.getLocationInfo().getEndLine() + ")");

                        // Show variables in this operation
                        System.out.println("      Variables:");
                        op.getAllVariableDeclarations().forEach(var ->
                                System.out.println("        * " + var.getVariableName() + " = " +
                                        (var.getInitializer() != null ? var.getInitializer().toString() : "null") +
                                        " (scope: " + var.getScope() + ")"));
                    });

                    System.out.println("\n  AFTER operations:");
                    classDiff.getNextClass().getOperations().forEach(op -> {
                        System.out.println("    - " + op.getName() + " (lines " +
                                op.getLocationInfo().getStartLine() + "-" + op.getLocationInfo().getEndLine() + ")");

                        // Show variables in this operation
                        System.out.println("      Variables:");
                        op.getAllVariableDeclarations().forEach(var ->
                                System.out.println("        * " + var.getVariableName() + " = " +
                                        (var.getInitializer() != null ? var.getInitializer().toString() : "null") +
                                        " (scope: " + var.getScope() + ")"));
                    });

                    // Check operation mappers
                    System.out.println("\n  Operation Mappers: " + classDiff.getOperationBodyMapperList().size());
                    classDiff.getOperationBodyMapperList().forEach(mapper -> {
                        System.out.println("    Mapper: " + mapper.getOperation1().getName() +
                                " -> " + mapper.getOperation2().getName());

                        // Show removed variable declarations (potential extractions)
                        System.out.println("      Variables only in BEFORE:");
                        mapper.getOperation1().getAllVariableDeclarations().stream()
                                .filter(var1 -> mapper.getOperation2().getAllVariableDeclarations().stream()
                                        .noneMatch(var2 -> var1.getVariableName().equals(var2.getVariableName())))
                                .forEach(var ->
                                        System.out.println("        - " + var.getVariableName() + " = " +
                                                (var.getInitializer() != null ? var.getInitializer().toString() : "null")));

                        // Show added variable declarations
                        System.out.println("      Variables only in AFTER:");
                        mapper.getOperation2().getAllVariableDeclarations().stream()
                                .filter(var2 -> mapper.getOperation1().getAllVariableDeclarations().stream()
                                        .noneMatch(var1 -> var1.getVariableName().equals(var2.getVariableName())))
                                .forEach(var ->
                                        System.out.println("        + " + var.getVariableName() + " = " +
                                                (var.getInitializer() != null ? var.getInitializer().toString() : "null")));

                        // Show statement mappings
                        System.out.println("      Statement mappings: " + mapper.getMappings().size());
                        mapper.getMappings().forEach(mapping ->
                                System.out.println("        " + mapping.getFragment1().getString().replaceAll("\\n", "\\\\n") +
                                        " -> " + mapping.getFragment2().getString().replaceAll("\\n", "\\\\n")));

                        // Show non-mapped statements (potential extractions)
                        System.out.println("      Non-mapped statements from BEFORE:");
                        mapper.getNonMappedLeavesT1().forEach(stmt ->
                                System.out.println("        - " + stmt.getString().replaceAll("\\n", "\\\\n")));

                        System.out.println("      Non-mapped statements from AFTER:");
                        mapper.getNonMappedLeavesT2().forEach(stmt ->
                                System.out.println("        + " + stmt.getString().replaceAll("\\n", "\\\\n")));
                    });
                }
            });

        } catch (Exception e) {
            System.out.println("Error in Extract Attribute analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void logOperationChanges(UMLModelDiff diff) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("OPERATION CHANGES");
        System.out.println(SUB_SEPARATOR);

        diff.getCommonClassDiffList().forEach(classDiff -> {
            if (!classDiff.getAddedOperations().isEmpty() ||
                    !classDiff.getRemovedOperations().isEmpty() ||
                    !classDiff.getOperationBodyMapperList().isEmpty()) {

                System.out.println("Class: " + classDiff.getOriginalClass().getName());

                System.out.println("  Added Operations: " + classDiff.getAddedOperations().size());
                classDiff.getAddedOperations().forEach(op ->
                        System.out.println("    + " + op.getName() + op.getParameterTypeList()));

                System.out.println("  Removed Operations: " + classDiff.getRemovedOperations().size());
                classDiff.getRemovedOperations().forEach(op ->
                        System.out.println("    - " + op.getName() + op.getParameterTypeList()));

                System.out.println("  Operation Body Mappers: " + classDiff.getOperationBodyMapperList().size());
                classDiff.getOperationBodyMapperList().forEach(mapper -> {
                    System.out.println("    ~ " + mapper.getOperation1().getName() +
                            " -> " + mapper.getOperation2().getName());
                    if (mapper.getOperationSignatureDiff().isPresent()) {
                        System.out.println("      (signature changed)");
                    }
                });
            }
        });
    }


    private static void logRefactorings(UMLModelDiff diff) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("DETECTED REFACTORINGS");
        System.out.println(SUB_SEPARATOR);

        try {
            List<Refactoring> refactorings = diff.getRefactorings();
            System.out.println("Total Refactorings: " + refactorings.size());

            // Group by type
            Map<RefactoringType, List<Refactoring>> groupedRefactorings =
                    refactorings.stream().collect(Collectors.groupingBy(Refactoring::getRefactoringType));

            groupedRefactorings.forEach((type, refactorings_list) -> {
                System.out.println("\n" + type.getDisplayName() + " (" + refactorings_list.size() + "):");
                refactorings_list.forEach(refactoring ->
                        System.out.println("  - " + refactoring.toString()));
            });

            if (refactorings.isEmpty()) {
                System.out.println("  No refactorings detected");
            }
        } catch (Exception e) {
            System.out.println("Error getting refactorings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void logDetailedRefactoringAnalysis(UMLModelDiff diff) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("DETAILED REFACTORING ANALYSIS");
        System.out.println(SUB_SEPARATOR);

        try {
            List<Refactoring> refactorings = diff.getRefactorings();

            // Special analysis for Extract Variable/Attribute
            long extractVariableCount = refactorings.stream()
                    .filter(r -> r.getRefactoringType() == RefactoringType.EXTRACT_VARIABLE)
                    .count();
            long extractAttributeCount = refactorings.stream()
                    .filter(r -> r.getRefactoringType() == RefactoringType.EXTRACT_ATTRIBUTE)
                    .count();

            System.out.println("Extract Variable Refactorings: " + extractVariableCount);
            System.out.println("Extract Attribute Refactorings: " + extractAttributeCount);

            // Look for potential variable declarations that could become attributes
            diff.getCommonClassDiffList().forEach(classDiff -> {
                System.out.println("\nAnalyzing class: " + classDiff.getOriginalClass().getName());

                // Check for variable patterns in operations
                classDiff.getOperationBodyMapperList().forEach(mapper -> {
                    System.out.println("  Operation: " + mapper.getOperation1().getName());

                    // Variables in before version
                    System.out.println("    Variables in BEFORE:");
                    mapper.getOperation1().getAllVariableDeclarations().forEach(var ->
                            System.out.println("      - " + var.getVariableName() + " = " +
                                    (var.getInitializer() != null ? var.getInitializer().toString() : "null") +
                                    " (isAttribute: " + var.isAttribute() + ", isParameter: " + var.isParameter() + ")"));

                    // Variables in after version
                    System.out.println("    Variables in AFTER:");
                    mapper.getOperation2().getAllVariableDeclarations().forEach(var ->
                            System.out.println("      - " + var.getVariableName() + " = " +
                                    (var.getInitializer() != null ? var.getInitializer().toString() : "null") +
                                    " (isAttribute: " + var.isAttribute() + ", isParameter: " + var.isParameter() + ")"));

                    // Mappings between statements
                    System.out.println("    Statement Mappings: " + mapper.getMappings().size());
                    mapper.getMappings().forEach(mapping ->
                            System.out.println("      " + mapping.getFragment1().getString() +
                                    " -> " + mapping.getFragment2().getString()));
                });
            });

            // Detailed attribute analysis
            System.out.println("\nAttribute Analysis:");
            diff.getCommonClassDiffList().forEach(classDiff -> {
                System.out.println("Class: " + classDiff.getOriginalClass().getName());

                System.out.println("  BEFORE attributes:");
                classDiff.getOriginalClass().getAttributes().forEach(attr ->
                        System.out.println("    - " + attr.getName() + " : " + attr.getType() +
                                " (location: " + attr.getLocationInfo().getCodeElementType() + ")"));

                System.out.println("  AFTER attributes:");
                classDiff.getNextClass().getAttributes().forEach(attr ->
                        System.out.println("    - " + attr.getName() + " : " + attr.getType() +
                                " (location: " + attr.getLocationInfo().getCodeElementType() + ")"));
            });

        } catch (Exception e) {
            System.out.println("Error in detailed analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
