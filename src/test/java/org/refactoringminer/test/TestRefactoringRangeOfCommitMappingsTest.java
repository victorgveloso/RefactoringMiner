package org.refactoringminer.test;

import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.*;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.refactoringminer.utils.Assertions.assertHasSameElementsAs;

public class TestRefactoringRangeOfCommitMappingsTest {
    public static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";
    private static final String EXPECTED_PATH = System.getProperty("user.dir") + "/src/test/resources/mappings/";
    private GitHistoryRefactoringMinerImpl miner;
    private List<String> actual;
    private List<String> expected;


    @ParameterizedTest
    @CsvSource({
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 1, TestRefactoringsFromStackOverflow-1.txt", // null->null
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 3, TestRefactoringsFromStackOverflow-3.txt", // null->null
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 6, TestRefactoringsFromStackOverflow-6.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 7, TestRefactoringsFromStackOverflow-7.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 9, TestRefactoringsFromStackOverflow-9.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 10, TestRefactoringsFromStackOverflow-10.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 11, TestRefactoringsFromStackOverflow-11.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 12, TestRefactoringsFromStackOverflow-12.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 15, TestRefactoringsFromStackOverflow-15.txt",
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 16, TestRefactoringsFromStackOverflow-16.txt", // null->null
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 19, TestRefactoringsFromStackOverflow-19.txt", // null->null
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 21, TestRefactoringsFromStackOverflow-21.txt", // null->null
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 22, TestRefactoringsFromStackOverflow-22.txt", // null->null
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 25, TestRefactoringsFromStackOverflow-25.txt", // null->null
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 27, TestRefactoringsFromStackOverflow-27.txt", // null->null
            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 33, TestRefactoringsFromStackOverflow-33.txt",
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 2, TestRefactoringsFromStackOverflow-2.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 4, TestRefactoringsFromStackOverflow-4.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 5, TestRefactoringsFromStackOverflow-5.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 8, TestRefactoringsFromStackOverflow-8.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 13, TestRefactoringsFromStackOverflow-13.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 14, TestRefactoringsFromStackOverflow-14.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 17, TestRefactoringsFromStackOverflow-17.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 18, TestRefactoringsFromStackOverflow-18.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 20, TestRefactoringsFromStackOverflow-20.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 23, TestRefactoringsFromStackOverflow-23.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 24, TestRefactoringsFromStackOverflow-24.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 26, TestRefactoringsFromStackOverflow-26.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 28, TestRefactoringsFromStackOverflow-28.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 29, TestRefactoringsFromStackOverflow-29.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 30, TestRefactoringsFromStackOverflow-30.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 31, TestRefactoringsFromStackOverflow-31.txt", // No mappings
//            "https://github.com/victorgveloso/TestRefactoringsFromStackOverflow.git, 32, TestRefactoringsFromStackOverflow-32.txt", // No mappings
    })
    public void testStackOverflow(String url, int pullRequestId, String testResultFileName) throws Exception {
        testRefactoringRangeMappings(url, pullRequestId, testResultFileName, ref -> {
            UMLOperationBodyMapper bodyMapper = null;
            Set<? extends AbstractCodeMapping> codeMappings = new HashSet<>();
            VariableDeclarationContainer left = null;
            VariableDeclarationContainer right = null;
            switch (ref.getRefactoringType()) {
                case EXTRACT_OPERATION -> {var r = (ExtractOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getSourceOperationBeforeExtraction(); right = r.getExtractedOperation();}
                case RENAME_METHOD -> {var r = (RenameOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getOriginalOperation(); right = r.getRenamedOperation();}
                case INLINE_OPERATION -> {var r = (InlineOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getInlinedOperation(); right = r.getTargetOperationAfterInline();}
                case MOVE_OPERATION -> {var r = (MoveOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getOriginalOperation(); right = r.getMovedOperation();}
                case MOVE_AND_RENAME_OPERATION -> {var r = (MoveOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getOriginalOperation(); right = r.getMovedOperation();}
                case PULL_UP_OPERATION -> {var r = (PullUpOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getOriginalOperation(); right = r.getMovedOperation();}
                case PUSH_DOWN_OPERATION -> {var r = (PushDownOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getOriginalOperation(); right = r.getMovedOperation();}
                case EXTRACT_AND_MOVE_OPERATION -> {var r = (ExtractOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getSourceOperationBeforeExtraction(); right = r.getExtractedOperation();}
                case MOVE_AND_INLINE_OPERATION -> {var r = (InlineOperationRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getInlinedOperation(); right = r.getTargetOperationAfterInline();}
                case MOVE_CODE -> {var r = (MoveCodeRefactoring)ref; bodyMapper = r.getBodyMapper(); codeMappings = r.getMappings(); left = r.getSourceContainer(); right = r.getTargetContainer();}
                case PARAMETERIZE_TEST -> {var r = (ParameterizeTestRefactoring)ref; bodyMapper = r.getBodyMapper(); left = r.getRemovedOperation(); right = r.getParameterizedTestOperation();}
                case EXTRACT_VARIABLE -> {var r = (ExtractVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case INLINE_VARIABLE -> {var r = (InlineVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case RENAME_VARIABLE -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case RENAME_PARAMETER -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MERGE_VARIABLE -> {var r = (MergeVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MERGE_PARAMETER -> {var r = (MergeVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case SPLIT_VARIABLE -> {var r = (SplitVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case SPLIT_PARAMETER -> {var r = (SplitVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case PARAMETERIZE_VARIABLE -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case LOCALIZE_PARAMETER -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_RETURN_TYPE -> {var r = (ChangeReturnTypeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_VARIABLE_TYPE -> {var r = (ChangeVariableTypeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_PARAMETER_TYPE -> {var r = (ChangeVariableTypeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ASSERT_THROWS -> {var r = (AssertThrowsRefactoring)ref; codeMappings = r.getAssertThrowsMappings(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MOVE_RENAME_ATTRIBUTE -> {var r = (MoveAndRenameAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOriginalAttribute(); right = r.getMovedAttribute();}
                case REPLACE_ATTRIBUTE -> {var r = (ReplaceAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOriginalAttribute(); right = r.getMovedAttribute();}
                case EXTRACT_ATTRIBUTE -> {var r = (ExtractAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOriginalClass().getAttributes().get(0); right = r.getNextClass().getAttributes().get(0);}
                case INLINE_ATTRIBUTE -> {var r = (InlineAttributeRefactoring)ref; codeMappings = new HashSet<>(r.getSubExpressionMappings()); left = r.getOriginalClass().getAttributes().get(0); right = r.getNextClass().getAttributes().get(0);}
                case RENAME_ATTRIBUTE -> {var r = (RenameAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOriginalAttribute(); right = r.getRenamedAttribute();}
                case MERGE_ATTRIBUTE -> {var r = (MergeAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getMergedAttributes().iterator().next(); right = r.getNewAttribute();}
                case SPLIT_ATTRIBUTE -> {var r = (SplitAttributeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOldAttribute(); right = r.getSplitAttributes().iterator().next();}
                case REPLACE_VARIABLE_WITH_ATTRIBUTE -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_ATTRIBUTE_WITH_VARIABLE -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case PARAMETERIZE_ATTRIBUTE -> {var r = (RenameVariableRefactoring)ref; codeMappings = r.getReferences(); left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_ATTRIBUTE_TYPE -> {var r = (ChangeAttributeTypeRefactoring)ref; codeMappings = r.getReferences(); left = r.getOriginalAttribute(); right = r.getChangedTypeAttribute();}
                case ADD_PARAMETER -> {var r = (AddParameterRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_PARAMETER -> {var r = (RemoveParameterRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REORDER_PARAMETER -> {var r = (ReorderParameterRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_THROWN_EXCEPTION_TYPE -> {var r = (AddThrownExceptionTypeRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_THROWN_EXCEPTION_TYPE -> {var r = (RemoveThrownExceptionTypeRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_THROWN_EXCEPTION_TYPE -> {var r = (ChangeThrownExceptionTypeRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case CHANGE_OPERATION_ACCESS_MODIFIER -> {var r = (ChangeOperationAccessModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_METHOD_MODIFIER -> {var r = (AddMethodModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_METHOD_MODIFIER -> {var r = (RemoveMethodModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_VARIABLE_MODIFIER -> {var r = (AddVariableModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_PARAMETER_MODIFIER -> {var r = (AddVariableModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_VARIABLE_MODIFIER -> {var r = (RemoveVariableModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_PARAMETER_MODIFIER -> {var r = (RemoveVariableModifierRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_LOOP_WITH_PIPELINE -> {var r = (ReplaceLoopWithPipelineRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_PIPELINE_WITH_LOOP -> {var r = (ReplacePipelineWithLoopRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_ANONYMOUS_WITH_LAMBDA -> {var r = (ReplaceAnonymousWithLambdaRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case SPLIT_CONDITIONAL -> {var r = (SplitConditionalRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case INVERT_CONDITION -> {var r = (InvertConditionRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MERGE_CONDITIONAL -> {var r = (MergeConditionalRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MERGE_CATCH -> {var r = (MergeCatchRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ASSERT_TIMEOUT -> {var r = (AssertTimeoutRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case TRY_WITH_RESOURCES -> {var r = (TryWithResourcesRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_GENERIC_WITH_DIAMOND -> {var r = (ReplaceGenericWithDiamondRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REPLACE_CONDITIONAL_WITH_TERNARY -> {var r = (ReplaceConditionalWithTernaryRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
//                case MERGE_OPERATION -> {var r = (MergeOperationRefactoring)ref; left = r.getMergedMethods(); right = r.getNewMethodAfterMerge();}
//                case SPLIT_OPERATION -> {var r = (SplitOperationRefactoring)ref; left = r.getOriginalMethodBeforeSplit(); right = r.getSplitMethods();}

                case MOVE_ATTRIBUTE -> {var r = (MoveAttributeRefactoring)ref; left = r.getOriginalAttribute(); right = r.getMovedAttribute();}
                case PULL_UP_ATTRIBUTE -> {var r = (PullUpAttributeRefactoring)ref; left = r.getOriginalAttribute(); right = r.getMovedAttribute();}
                case PUSH_DOWN_ATTRIBUTE -> {var r = (PushDownAttributeRefactoring)ref; left = r.getOriginalAttribute(); right = r.getMovedAttribute();}
                case CHANGE_ATTRIBUTE_ACCESS_MODIFIER -> {var r = (ChangeAttributeAccessModifierRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
                case ENCAPSULATE_ATTRIBUTE -> {var r = (EncapsulateAttributeRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
                case ADD_ATTRIBUTE_MODIFIER -> {var r = (AddAttributeModifierRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
                case REMOVE_ATTRIBUTE_MODIFIER -> {var r = (RemoveAttributeModifierRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}

//                case CHANGE_TYPE_DECLARATION_KIND -> {var r = (ChangeTypeDeclarationKindRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case RENAME_CLASS -> {var r = (RenameClassRefactoring)ref; left = r.getOriginalClass(); right = r.getRenamedClass();}
//                case MOVE_CLASS -> {var r = (MoveClassRefactoring)ref; left = r.getOriginalClass(); right = r.getMovedClass();}
//                case MOVE_RENAME_CLASS -> {var r = (MoveAndRenameClassRefactoring)ref; left = r.getOriginalClass(); right = r.getRenamedClass();}
//                case EXTRACT_CLASS -> {var r = (ExtractClassRefactoring)ref; left = r.getOriginalClass(); right = r.getExtractedClass();}
//                case REPLACE_ANONYMOUS_WITH_CLASS -> {var r = (ReplaceAnonymousWithClassRefactoring)ref; left = r.getAnonymousClass(); right = r.getAddedClass();}
//                case CHANGE_CLASS_ACCESS_MODIFIER -> {var r = (ChangeClassAccessModifierRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case ADD_CLASS_MODIFIER -> {var r = (AddClassModifierRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case REMOVE_CLASS_MODIFIER -> {var r = (RemoveClassModifierRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case MERGE_CLASS -> {var r = (MergeClassRefactoring)ref; left = r.getOriginalClass(); right = r.getMergedClasses();}
//                case SPLIT_CLASS -> {var r = (SplitClassRefactoring)ref; left = r.getOriginalClass(); right = r.getMovedClass();}
//                case EXTRACT_INTERFACE -> {var r = (ExtractSuperclassRefactoring)ref; left = r.getExtractedClass(); right = r.getExtractedClass();}
//                case EXTRACT_SUPERCLASS -> {var r = (ExtractSuperclassRefactoring)ref; left = r.getExtractedClass(); right = r.getExtractedClass();}
//                case EXTRACT_SUBCLASS -> {var r = (ExtractClassRefactoring)ref; left = r.getOriginalClass(); right = r.getExtractedClass();}
//                case ADD_CLASS_ANNOTATION -> {var r = (AddClassAnnotationRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case REMOVE_CLASS_ANNOTATION -> {var r = (RemoveClassAnnotationRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}
//                case MODIFY_CLASS_ANNOTATION -> {var r = (ModifyClassAnnotationRefactoring)ref; left = r.getClassBefore(); right = r.getClassAfter();}

                case REMOVE_METHOD_ANNOTATION -> {var r = (RemoveMethodAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MODIFY_METHOD_ANNOTATION -> {var r = (ModifyMethodAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_PARAMETER_ANNOTATION -> {var r = (AddVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_PARAMETER_ANNOTATION -> {var r = (RemoveVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MODIFY_PARAMETER_ANNOTATION -> {var r = (ModifyVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_VARIABLE_ANNOTATION -> {var r = (AddVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case REMOVE_VARIABLE_ANNOTATION -> {var r = (RemoveVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case MODIFY_VARIABLE_ANNOTATION -> {var r = (ModifyVariableAnnotationRefactoring)ref; left = r.getOperationBefore(); right = r.getOperationAfter();}
                case ADD_ATTRIBUTE_ANNOTATION -> {var r = (AddAttributeAnnotationRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
                case REMOVE_ATTRIBUTE_ANNOTATION -> {var r = (RemoveAttributeAnnotationRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
                case MODIFY_ATTRIBUTE_ANNOTATION -> {var r = (ModifyAttributeAnnotationRefactoring)ref; left = r.getAttributeBefore(); right = r.getAttributeAfter();}
            }
            if (!codeMappings.isEmpty()) {
                mapperInfo(codeMappings, left, right);
            }
            else if (bodyMapper != null) {
                mapperInfo(bodyMapper.getMappings(), left, right);
            }
            else {
                mapperInfo(codeMappings, left, right);
            }
        });
    }


    @ParameterizedTest
    @CsvSource({
            // apache/commons-math 8e995890ea35399b6da6bc86532f0694accd511b -> b31439f3ec9bb216465ae77de5f7cb8433dd3140 -> 1d5a4e2d3d0fbd894b4e344a3d6ea601c14ab80e -> 229c782087d2eaef17d23682fcd8b36a73bb756b -> 5b9f353eeabc824146443b3c413be1f670985b4d
            "https://github.com/victorgveloso/commons-math.git, 1, commons-math-fork-1.txt",
            // apache/commons-math cd6d71b967019626734e81103a897729e70cd64b -> 73812e41db0aa040b53c6ff3f35804c037aa2a9b
            "https://github.com/victorgveloso/commons-math.git, 2, commons-math-fork-2.txt",
    })
    public void testCommonsMathMappings(String url, int pullRequestId, String testResultFileName) throws Exception {
        testRefactoringRangeMappings(url, pullRequestId, testResultFileName, ref -> {
            if (ref instanceof AssertThrowsRefactoring) {
                AssertThrowsRefactoring assertThrowsRefactoring = (AssertThrowsRefactoring) ref;
                Set<AbstractCodeMapping> mapper = assertThrowsRefactoring.getAssertThrowsMappings();
                mapperInfo(mapper, assertThrowsRefactoring.getOperationBefore(), assertThrowsRefactoring.getOperationAfter());
            }
            else if (ref instanceof PullUpOperationRefactoring) {
                PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) ref;
                UMLOperationBodyMapper bodyMapper = pullUpOperationRefactoring.getBodyMapper();
                Set<AbstractCodeMapping> mapper = bodyMapper.getMappings();
                mapperInfo(mapper, pullUpOperationRefactoring.getOriginalOperation(), pullUpOperationRefactoring.getMovedOperation());
            }
            else if (ref instanceof ExtractOperationRefactoring && ((ExtractOperationRefactoring) ref).getRefactoringType() == RefactoringType.EXTRACT_AND_MOVE_OPERATION) {
                ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) ref;
                UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                Set<AbstractCodeMapping> mapper = bodyMapper.getMappings();
                mapperInfo(mapper, extractOperationRefactoring.getSourceOperationBeforeExtraction(), extractOperationRefactoring.getExtractedOperation());
            }
            else if (ref instanceof ExtractSuperclassRefactoring) {
                ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) ref;
                Set<Pair<? extends LocationInfoProvider, ? extends LocationInfoProvider>> mappings = matchNameWithClasses(extractSuperclassRefactoring.getUMLSubclassSetBefore(), extractSuperclassRefactoring.getUMLSubclassSetAfter(), extractSuperclassRefactoring.getInvolvedClassesAfterRefactoring());
                mapperInfo(mappings, extractSuperclassRefactoring.getUMLSubclassSetBefore(), extractSuperclassRefactoring.getExtractedClass());
            }
        });
    }

    private static Set<Pair<? extends LocationInfoProvider, ? extends LocationInfoProvider>> matchNameWithClasses(Set<UMLClass> umlSubclassSetBefore, Set<UMLClass> umlSubclassSetAfter, Set<ImmutablePair<String, String>> involvedClassesAfterRefactoring) {
        return involvedClassesAfterRefactoring.stream().map(pair -> {
                    Optional<UMLClass> before = umlSubclassSetBefore.stream().filter(clzz -> clzz.getName().equals(pair.left)).findFirst();
                    Optional<UMLClass> after = umlSubclassSetAfter.stream().filter(clzz -> clzz.getName().equals(pair.right)).findFirst();
                    return Pair.of(before, after);
                }).filter(pair -> pair.left().isPresent() && pair.right().isPresent())
                .map(pair -> Pair.of(pair.left().get(), pair.right().get())).collect(Collectors.toSet());
    }

    @BeforeEach
    void setUp() {
        miner = new GitHistoryRefactoringMinerImpl();
        actual = new ArrayList<>();
        expected = new ArrayList<>();
    }

    private void testRefactoringRangeMappings(String url, int pullRequestId, String testResultFileName, final Consumer<Refactoring> consumer) throws Exception {
        ProjectASTDiff diff = miner.diffAtPullRequest(url, pullRequestId, 500);
        for (Refactoring refactoring : diff.getRefactorings()) {
            consumer.accept(refactoring);
        }
        Supplier<String> lazyErrorMessage = () -> actual.stream().collect(Collectors.joining(System.lineSeparator()));
        Assertions.assertDoesNotThrow(() -> {
            expected.addAll(IOUtils.readLines(new FileReader(EXPECTED_PATH + testResultFileName)));
        }, lazyErrorMessage);
        assertHasSameElementsAs(expected, actual, lazyErrorMessage);
    }

    private <T, Y> void mapperInfo(Set<Y> mappings, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        for(Y mapping : mappings) {
            if (mapping instanceof AbstractCodeMapping) {
                mapperInfo((AbstractCodeMapping) mapping, operationBefore, operationAfter);
            }
            else if (mapping instanceof UMLAbstractClass) {
                mapperInfo((UMLAbstractClass) mapping, operationBefore, operationAfter);
            }
            else {
                throw new IllegalArgumentException("Unknown mapping type: " + mapping.getClass().getName());
            }
        }
    }

    private <T> boolean mapperInfo(AbstractCodeMapping mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        if(mapping.getFragment1() instanceof LeafExpression && mapping.getFragment2() instanceof LeafExpression)
            return false;
        String line = mapping.getFragment1().getLocationInfo() + "==" + mapping.getFragment2().getLocationInfo();
        actual.add(line);
        return true;
    }

    private <T> boolean mapperInfo(UMLAbstractClass mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        String line = mapping.getLocationInfo().toString();
        actual.add(line);
        return true;
    }

    private <T,X extends LocationInfoProvider> boolean mapperInfo(Pair<X,X> mapping, T operationBefore, T operationAfter) {
        actual.add(operationBefore + " -> " + operationAfter);
        String line = mapping.left().getLocationInfo() + "==" + mapping.right().getLocationInfo();
        actual.add(line);
        return true;
    }
}
