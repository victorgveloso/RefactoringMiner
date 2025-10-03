package org.refactoringminer.utils;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RefactoringAssertUtils {

    public static void assertRenameClassRefactoringDetected(Map<String, String> beforeFiles, Map<String, String> afterFiles, String beforeClassName, String afterClassName) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        boolean classRenameDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof RenameClassRefactoring &&
                        ((RenameClassRefactoring)ref).getOriginalClassName().equals(beforeClassName) &&
                        ((RenameClassRefactoring)ref).getRenamedClassName().equals(afterClassName));

        System.out.println("==== DIFF ====");
        System.out.println("Animal to Mammal");
        System.out.println("Class rename detected: " + classRenameDetected);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(classRenameDetected, "Expected a RenameClassRefactoring from " + beforeClassName + " to " + afterClassName);
    }

    public static void dumpModels(UMLModel beforeUML, UMLModel afterUML) {
        System.out.println("=== BEFORE MODEL OPERATIONS ===");
        for (UMLClass umlClass : beforeUML.getClassList()) {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        }

        System.out.println("=== AFTER MODEL OPERATIONS ===");
        for (UMLClass umlClass : afterUML.getClassList()) {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  " + dumpOperation(op));
            }
        }
    }


    public static boolean detectExtractMethod(UMLModel beforeUML, UMLModel afterUML,
                                              String sourceMethodName, String extractedMethodName) throws RefactoringMinerTimedOutException {
        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean extractDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof ExtractOperationRefactoring &&
                        ((ExtractOperationRefactoring) ref).getSourceOperationBeforeExtraction().getName().equals(sourceMethodName) &&
                        ((ExtractOperationRefactoring) ref).getExtractedOperation().getName().equals(extractedMethodName));

        System.out.println("==== DIFF ====");
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        return extractDetected;
    }

    private static int countAllStatements(CompositeStatementObject composite) {
        int count = 0;
        for (AbstractStatement stmt : composite.getStatements()) {
            count++;
            if (stmt instanceof CompositeStatementObject) {
                // Recursively count nested statements
                count += countAllStatements((CompositeStatementObject) stmt);
            }
        }
        return count;
    }

    public static void assertExtractFunctionRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceMethodName,
            String extractedMethodName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean extractDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof ExtractOperationRefactoring &&
                        ((ExtractOperationRefactoring) ref).getSourceOperationBeforeExtraction().getName().equals(sourceMethodName) &&
                        ((ExtractOperationRefactoring) ref).getExtractedOperation().getName().equals(extractedMethodName));

        System.out.println("==== DIFF ====");
        System.out.println("Extract function detected: " + extractDetected);
        System.out.println("Total refactorings: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(System.out::println);

        assertTrue(extractDetected, "Expected ExtractOperationRefactoring for extracted method: " + extractedMethodName);
    }

    public static void assertRenamePackageRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourcePackage,
            String targetPackage
    ) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean renamePackageDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof RenamePackageRefactoring
                        && ((RenamePackageRefactoring) ref).getPattern().getBefore().equals(sourcePackage)
                        && ((RenamePackageRefactoring) ref).getPattern().getAfter().equals(targetPackage));

        System.out.println("==== DIFF ====");
        System.out.println("Source package: " + sourcePackage + " to " + targetPackage);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(renamePackageDetected,
                "Expected a RenamePackageRefactoring from " + sourcePackage + " to " + targetPackage);
    }

    public static void assertMovePackageRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourcePackage,
            String targetPackage
    ) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean movePackageDetected = diff.getRefactorings().stream()
                .filter(ref -> ref instanceof RenamePackageRefactoring)
                .map(ref -> (RenamePackageRefactoring) ref)
                .anyMatch(ref -> {
                    String before = ref.getPattern().getBefore();
                    String after = ref.getPattern().getAfter();

                    // Remove trailing dots if present
                    if (before.endsWith(".")) before = before.substring(0, before.length() - 1);
                    if (after.endsWith(".")) after = after.substring(0, after.length() - 1);

                    return before.equals(sourcePackage)
                            && after.equals(targetPackage)
                            && RefactoringType.MOVE_PACKAGE.equals(ref.getRefactoringType());
                });


        System.out.println("==== DIFF ====");
        System.out.println("Source package: " + sourcePackage + " to " + targetPackage);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(movePackageDetected,
                "Expected a MovePackageRefactoring from " + sourcePackage + " to " + targetPackage);
    }

    public static void assertMoveSourceFolderRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceFolderBefore,
            String sourceFolderAfter
    ) throws Exception {

        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean moveSourceFolderDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref.getRefactoringType().getDisplayName().equals("Move Source Folder")
                        && ref.toString().contains(sourceFolderBefore)
                        && ref.toString().contains(sourceFolderAfter));

        System.out.println("==== DIFF ====");
        System.out.println("Source folder: " + sourceFolderBefore + " to " + sourceFolderAfter);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(moveSourceFolderDetected,
                "Expected a Move Source Folder refactoring from " + sourceFolderBefore + " to " + sourceFolderAfter);
    }


    public static void assertInlineMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String targetMethod,
            String inlinedMethod
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean inlineDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof InlineOperationRefactoring &&
                        ((InlineOperationRefactoring) ref).getTargetOperationAfterInline().getName().equals(targetMethod) &&
                        ((InlineOperationRefactoring) ref).getInlinedOperation().getName().equals(inlinedMethod));

        System.out.println("==== DIFF ====");
        System.out.println("Inline method detected: " + inlineDetected);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(inlineDetected, "Expected InlineOperationRefactoring for inlined method: " + inlinedMethod);
    }

    public static void assertPushDownMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName,
            String methodName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean pushDownDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof PushDownOperationRefactoring &&
                        ((PushDownOperationRefactoring) ref).getOriginalOperation().getClassName().equals(sourceClassName) &&
                        ((PushDownOperationRefactoring) ref).getMovedOperation().getClassName().equals(targetClassName) &&
                        ((PushDownOperationRefactoring) ref).getMovedOperation().getName().equals(methodName)
                );

        System.out.println("==== DIFF ====");
        System.out.printf("Push Down detected: %s\n", pushDownDetected);
        System.out.println("Source: " + sourceClassName + ", Target: " + targetClassName + ", Method: " + methodName);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(pushDownDetected, "Expected PushDownOperationRefactoring for method '" + methodName +
                "' from " + sourceClassName + " to " + targetClassName);
    }

    public static void assertPullUpMethodRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName,
            String methodName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean pullUpDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof PullUpOperationRefactoring &&
                        ((PullUpOperationRefactoring) ref).getOriginalOperation().getClassName().equals(sourceClassName) &&
                        ((PullUpOperationRefactoring) ref).getMovedOperation().getClassName().equals(targetClassName) &&
                        ((PullUpOperationRefactoring) ref).getMovedOperation().getName().equals(methodName)
                );

        System.out.println("==== DIFF ====");
        System.out.printf("Pull Up detected: %s\n", pullUpDetected);
        System.out.println("Source: " + sourceClassName + ", Target: " + targetClassName + ", Method: " + methodName);
        diff.getRefactorings().forEach(System.out::println);

        assertTrue(pullUpDetected, "Expected PullUpOperationRefactoring for method '" + methodName +
                "' from " + sourceClassName + " to " + targetClassName);
    }

    public static void assertExtractClassRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean extractDetected = diff.getRefactorings().stream()
                .anyMatch(ref -> ref instanceof ExtractClassRefactoring &&
                        ((ExtractClassRefactoring) ref).getOriginalClass().getName().equals(sourceClassName) &&
                        ((ExtractClassRefactoring) ref).getExtractedClass().getName().equals(targetClassName)
                );

        System.out.println("==== DIFF ====");
        System.out.printf("Extract Class detected: %s\n", extractDetected);
        System.out.println("Source: " + sourceClassName + ", Target: " + targetClassName);
        diff.getRefactorings().forEach(System.out::println);

        assertTrue(extractDetected, "Expected ExtractClassRefactoring from '" + sourceClassName +
                "' to '" + targetClassName + "'");
    }

    public static void assertReorderParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String className,
            String methodName,
            String[] paramsBefore,
            String[] paramsAfter
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean reorderDetected = diff.getRefactorings().stream()
                .filter(ref -> ref instanceof ReorderParameterRefactoring)
                .map(ref -> (ReorderParameterRefactoring) ref)
                .anyMatch(ref ->
                        ref.getOperationBefore().getClassName().equals(className) &&
                                ref.getOperationBefore().getName().equals(methodName) &&
                                java.util.Arrays.equals(
                                        ref.getParametersBefore().stream().map(Object::toString).toArray(String[]::new),
                                        paramsBefore) &&
                                java.util.Arrays.equals(
                                        ref.getParametersAfter().stream().map(Object::toString).toArray(String[]::new),
                                        paramsAfter)
                );

        System.out.println("==== DIFF ====");
        System.out.printf("Reorder Parameter detected: %s\n", reorderDetected);
        System.out.println("Class: " + className + ", Method: " + methodName);
        System.out.println("Params before: " + java.util.Arrays.toString(paramsBefore));
        System.out.println("Params after : " + java.util.Arrays.toString(paramsAfter));
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(reorderDetected, String.format(
                "Expected ReorderParameterRefactoring in '%s.%s' from %s to %s",
                className, methodName,
                java.util.Arrays.toString(paramsBefore),
                java.util.Arrays.toString(paramsAfter)
        ));
    }

    public static void assertPullUpAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName,
            String attributeName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        System.out.println("Total refactorings detected: " + diff.getRefactorings().size());
        diff.getRefactorings().forEach(System.out::println);

        boolean pullUpDetected = diff.getRefactorings().stream()
                .anyMatch(ref ->
                        ref.getRefactoringType().equals(RefactoringType.PULL_UP_ATTRIBUTE) &&
                                ref.toString().contains(sourceClassName) &&
                                ref.toString().contains(targetClassName) &&
                                ref.toString().contains(attributeName)
                );

        assertTrue(pullUpDetected, "Expected PullUpAttributeRefactoring of '" + attributeName +
                "' from '" + sourceClassName + "' to '" + targetClassName + "'");
    }

    public static void assertPushDownAttributeRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String sourceClassName,
            String targetClassName,
            String attributeName
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);

        boolean pushDownDetected = diff.getRefactorings().stream()
                .anyMatch(ref ->
                        ref.getRefactoringType().equals(RefactoringType.PUSH_DOWN_ATTRIBUTE) &&
                                ref.toString().contains(sourceClassName) &&
                                ref.toString().contains(targetClassName) &&
                                ref.toString().contains(attributeName)
                );

        System.out.println("==== DIFF ====");
        System.out.printf("Push Down Attribute detected: %s\n", pushDownDetected);
        System.out.println("Source: " + sourceClassName + ", Target: " + targetClassName + ", Attribute: " + attributeName);
        List<org.refactoringminer.api.Refactoring> refactorings = diff.getRefactorings();
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        assertTrue(pushDownDetected, "Expected PushDownAttributeRefactoring of '" + attributeName +
                "' from '" + sourceClassName + "' to '" + targetClassName + "'");
    }


    public static String dumpOperation(UMLOperation op) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(
            "name=%s, params=%s, signature=%s, isConstructor=%b, static=%b, visibility=%s",
            op.getName(),
            op.getParameters().stream().map(UMLParameter::getName).toList(),
            op.getActualSignature(),
            op.isConstructor(),
            op.isStatic(),
            op.getVisibility()
    ));

    // Enhanced body logging
    OperationBody body = op.getBody();
    if (body != null) {
        sb.append("\n    BODY: hashCode=").append(body.getBodyHashCode());

        CompositeStatementObject composite = body.getCompositeStatement();
        sb.append("\n    STATEMENTS: ").append(composite.getStatements().size())
                .append(", LEAVES: ").append(composite.getLeaves().size())
                .append(", INNER NODES: ").append(composite.getInnerNodes().size())
                .append(", EXPRESSIONS: ").append(composite.getExpressions().size());

        sb.append("\n    VARIABLES: ").append(body.getAllVariables());
        sb.append("\n    METHOD CALLS: ").append(body.getAllOperationInvocations().size());

        sb.append("\n    STRING REPRESENTATION:");
        List<String> stringRep = body.stringRepresentation();
        for (String line : stringRep) {
            sb.append("\n      ").append(line);
        }
    } else {
        sb.append("\n    BODY: null");
    }

    return sb.toString();
}

}
