package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.replacement.AddVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.ClassInstanceCreationWithMethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationWithClassInstanceCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ObjectCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.SwapArgumentReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.CandidateMergeVariableRefactoring;
import gr.uom.java.xmi.diff.CandidateSplitVariableRefactoring;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithLambdaRefactoring;
import gr.uom.java.xmi.diff.ReplaceLoopWithPipelineRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.util.PrefixSuffixUtils;

import java.util.*;
import java.util.regex.Pattern;

public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper> {
	private UMLOperation operation1;
	private UMLOperation operation2;
	private UMLAttribute attribute1;
	private UMLAttribute attribute2;
	private final Set<AbstractCodeMapping> mappings;
	private final List<AbstractCodeFragment> nonMappedLeavesT1;
	private final List<AbstractCodeFragment> nonMappedLeavesT2;
	private final List<CompositeStatementObject> nonMappedInnerNodesT1;
	private final List<CompositeStatementObject> nonMappedInnerNodesT2;
	private final Set<Refactoring> refactorings = new LinkedHashSet<>();
	private final Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables = new LinkedHashSet<>();
	private final Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<>();
	private final Set<CandidateMergeVariableRefactoring> candidateAttributeMerges = new LinkedHashSet<>();
	private final Set<CandidateSplitVariableRefactoring> candidateAttributeSplits = new LinkedHashSet<>();
	private final List<UMLOperationBodyMapper> childMappers = new ArrayList<>();
	private UMLOperationBodyMapper parentMapper;
	private static final Pattern SPLIT_CONDITIONAL_PATTERN = Pattern.compile("(\\|\\|)|(&&)|(\\?)|(:)");
	public static final Pattern SPLIT_CONCAT_STRING_PATTERN = Pattern.compile("(\\s)*(\\+)(\\s)*");
	private static final int MAXIMUM_NUMBER_OF_COMPARED_STRINGS = 100;
	private UMLClassBaseDiff classDiff;
	private UMLModelDiff modelDiff;
	private UMLOperation callSiteOperation;
	private final Map<AbstractCodeFragment, UMLOperation> codeFragmentOperationMap1 = new LinkedHashMap<>();
	private final Map<AbstractCodeFragment, UMLOperation> codeFragmentOperationMap2 = new LinkedHashMap<>();
	
	private Set<AbstractCodeFragment> statementsWithStreamAPICalls(List<AbstractCodeFragment> leaves) {
		Set<AbstractCodeFragment> streamAPICalls = new LinkedHashSet<AbstractCodeFragment>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(" -> ") ||
					invocation.actualString().contains("::"))) {
				Map<String, List<AbstractCall>> methodInvocationMap = statement.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					List<AbstractCall> invocations = methodInvocationMap.get(key);
					for(AbstractCall inv : invocations) {
						if(streamAPIName(inv.getName())) {
							streamAPICalls.add(statement);
							break;
						}
					}
				}
			}
		}
		return streamAPICalls;
	}

	private boolean streamAPIName(String name) {
		return name.equals("stream") || name.equals("filter") || name.equals("forEach") || name.equals("collect") || name.equals("map");
	}

	private List<AbstractCall> streamAPICalls(AbstractCodeFragment leaf) {
		List<AbstractCodeFragment> list = new ArrayList<>();
		list.add(leaf);
		return streamAPICalls(list);
	}

	private List<AbstractCall> streamAPICalls(List<AbstractCodeFragment> leaves) {
		List<AbstractCall> streamAPICalls = new ArrayList<AbstractCall>();
		for(AbstractCodeFragment statement : leaves) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation == null) {
				invocation = statement.assignmentInvocationCoveringEntireStatement();
			}
			if(invocation != null && (invocation.actualString().contains(" -> ") ||
					invocation.actualString().contains("::"))) {
				Map<String, List<AbstractCall>> methodInvocationMap = statement.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					List<AbstractCall> invocations = methodInvocationMap.get(key);
					for(AbstractCall inv : invocations) {
						if(streamAPIName(inv.getName())) {
							streamAPICalls.add(inv);
						}
					}
				}
			}
		}
		return streamAPICalls;
	}

	public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2, UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
		this.classDiff = classDiff;
		if(classDiff != null)
			this.modelDiff = classDiff.getModelDiff();
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.mappings = new LinkedHashSet<>();
		this.nonMappedLeavesT1 = new ArrayList<>();
		this.nonMappedLeavesT2 = new ArrayList<>();
		this.nonMappedInnerNodesT1 = new ArrayList<>();
		this.nonMappedInnerNodesT2 = new ArrayList<>();
		OperationBody body1 = operation1.getBody();
		OperationBody body2 = operation2.getBody();
		if(body1 != null && body2 != null) {
			List<AnonymousClassDeclarationObject> anonymous1 = body1.getAllAnonymousClassDeclarations();
			List<AnonymousClassDeclarationObject> anonymous2 = body2.getAllAnonymousClassDeclarations();
			CompositeStatementObject composite1 = body1.getCompositeStatement();
			CompositeStatementObject composite2 = body2.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = composite1.getLeaves();
			List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			int totalNodes1 = leaves1.size() + innerNodes1.size();
			int totalNodes2 = leaves2.size() + innerNodes2.size();
			boolean anonymousCollapse = Math.abs(totalNodes1 - totalNodes2) > 2*Math.min(totalNodes1, totalNodes2);
			if(!operation1.isDeclaredInAnonymousClass() && !operation2.isDeclaredInAnonymousClass() && anonymousCollapse) {
				if(anonymous1.size() == 1 && anonymous2.size() == 0) {
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf1 : leaves1) {
						if(leaf1.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf1;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1);
					}
				}
				else if(anonymous1.size() == 0 && anonymous2.size() == 1) {
					AbstractCodeFragment anonymousFragment = null;
					for(AbstractCodeFragment leaf2 : leaves2) {
						if(leaf2.getAnonymousClassDeclarations().size() > 0) {
							anonymousFragment = leaf2;
							break;
						}
					}
					if(anonymousFragment != null) {
						expandAnonymousAndLambdas(anonymousFragment, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2);
					}
				}
			}
			Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
			Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements2) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2);
					}
				}
			}
			else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
				for(AbstractCodeFragment streamAPICall : streamAPIStatements1) {
					if(streamAPICall.getLambdas().size() > 0) {
						expandAnonymousAndLambdas(streamAPICall, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1);
					}
				}
			}
			UMLOperationDiff operationDiff = new UMLOperationDiff(operation1, operation2);
			Map<String, String> parameterToArgumentMap1 = new LinkedHashMap<>();
			Map<String, String> parameterToArgumentMap2 = new LinkedHashMap<>();
			List<UMLParameter> addedParameters = operationDiff.getAddedParameters();
			if(addedParameters.size() == 1) {
				UMLParameter addedParameter = addedParameters.get(0);
				if(UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName())) {
					parameterToArgumentMap1.put("this.", "");
					//replace "parameterName." with ""
					parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
				}
			}
			List<UMLParameter> removedParameters = operationDiff.getRemovedParameters();
			if(removedParameters.size() == 1) {
				UMLParameter removedParameter = removedParameters.get(0);
				if(UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
					parameterToArgumentMap2.put("this.", "");
				}
			}
			List<UMLParameterDiff> parameterDiffList = operationDiff.getParameterDiffList();
			for(UMLParameterDiff parameterDiff : parameterDiffList) {
				UMLParameter addedParameter = parameterDiff.getAddedParameter();
				UMLParameter removedParameter = parameterDiff.getRemovedParameter();
				if(UMLModelDiff.looksLikeSameType(addedParameter.getType().getClassType(), operation1.getClassName()) &&
						UMLModelDiff.looksLikeSameType(removedParameter.getType().getClassType(), operation2.getClassName())) {
					parameterToArgumentMap1.put("this.", "");
					parameterToArgumentMap2.put(addedParameter.getName() + ".", "");
					parameterToArgumentMap1.put(removedParameter.getName() + ".", "");
					parameterToArgumentMap2.put("this.", "");
				}
			}
			if(classDiff != null) {
				for(UMLAttribute attribute : classDiff.getOriginalClass().getAttributes()) {
					if(UMLModelDiff.looksLikeSameType(attribute.getType().getClassType(), operation2.getClassName())) {
						parameterToArgumentMap1.put(attribute.getName() + ".", "");
						parameterToArgumentMap2.put("this.", "");
					}
				}
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			processLeaves(leaves1, leaves2, new LinkedHashMap<>());
			
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNodes2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<>());
			
			if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
				processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
			}
			
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
			}
		}
	}

	public UMLOperationBodyMapper(UMLAttribute removedAttribute, UMLAttribute addedAttribute) throws RefactoringMinerTimedOutException {
		this.attribute1 = removedAttribute;
		this.attribute2 = addedAttribute;
		AbstractExpression expression1 = removedAttribute.getVariableDeclaration().getInitializer();
		AbstractExpression expression2 = addedAttribute.getVariableDeclaration().getInitializer();
		this.mappings = new LinkedHashSet<>();
		this.nonMappedLeavesT1 = new ArrayList<>();
		this.nonMappedLeavesT2 = new ArrayList<>();
		this.nonMappedInnerNodesT1 = new ArrayList<>();
		this.nonMappedInnerNodesT2 = new ArrayList<>();
		if(expression1 != null && expression2 != null) {
			List<AbstractExpression> leaves1 = new ArrayList<>();
			leaves1.add(expression1);
			List<AbstractExpression> leaves2 = new ArrayList<>();
			leaves2.add(expression2);
			processLeaves(leaves1, leaves2, new LinkedHashMap<>());
		}
	}

	private UMLOperationBodyMapper(LambdaExpressionObject lambda1, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
		this.classDiff = parentMapper.classDiff;
		if(classDiff != null)
			this.modelDiff = classDiff.getModelDiff();
		this.operation1 = parentMapper.operation1;
		this.operation2 = parentMapper.operation2;
		this.mappings = new LinkedHashSet<>();
		this.nonMappedLeavesT1 = new ArrayList<>();
		this.nonMappedLeavesT2 = new ArrayList<>();
		this.nonMappedInnerNodesT1 = new ArrayList<>();
		this.nonMappedInnerNodesT2 = new ArrayList<>();
		
		if(lambda1.getExpression() != null && lambda2.getExpression() != null) {
			List<AbstractExpression> leaves1 = new ArrayList<>();
			leaves1.add(lambda1.getExpression());
			List<AbstractExpression> leaves2 = new ArrayList<>();
			leaves2.add(lambda2.getExpression());
			processLeaves(leaves1, leaves2, new LinkedHashMap<>());
		}
		else if(lambda1.getBody() != null && lambda2.getBody() != null) {
			CompositeStatementObject composite1 = lambda1.getBody().getCompositeStatement();
			CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
			processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
		}
	}

	private void processCompositeStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2, List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2)
			throws RefactoringMinerTimedOutException {
		Set<AbstractCodeFragment> streamAPIStatements1 = statementsWithStreamAPICalls(leaves1);
		Set<AbstractCodeFragment> streamAPIStatements2 = statementsWithStreamAPICalls(leaves2);
		if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
			for(AbstractCodeFragment streamAPICall : streamAPIStatements2) {
				if(streamAPICall.getLambdas().size() > 0) {
					expandAnonymousAndLambdas(streamAPICall, leaves2, innerNodes2, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList2(), codeFragmentOperationMap2, operation2);
				}
			}
		}
		else if(streamAPIStatements1.size() > 0 && streamAPIStatements2.size() == 0) {
			for(AbstractCodeFragment streamAPICall : streamAPIStatements1) {
				if(streamAPICall.getLambdas().size() > 0) {
					expandAnonymousAndLambdas(streamAPICall, leaves1, innerNodes1, new LinkedHashSet<>(), new LinkedHashSet<>(), anonymousClassList1(), codeFragmentOperationMap1, operation1);
				}
			}
		}
		processLeaves(leaves1, leaves2, new LinkedHashMap<>());
		
		processInnerNodes(innerNodes1, innerNodes2, new LinkedHashMap<>());
		
		if(streamAPIStatements1.size() == 0 && streamAPIStatements2.size() > 0) {
			processStreamAPIStatements(leaves1, leaves2, innerNodes1, streamAPIStatements2);
		}
		
		nonMappedLeavesT1.addAll(leaves1);
		nonMappedLeavesT2.addAll(leaves2);
		nonMappedInnerNodesT1.addAll(innerNodes1);
		nonMappedInnerNodesT2.addAll(innerNodes2);
		
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			temporaryVariableAssignment(statement, nonMappedLeavesT2);
		}
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			inlinedVariableAssignment(statement, nonMappedLeavesT2);
		}
	}

	private List<UMLAnonymousClass> anonymousClassList1() {
		if(operation1 != null)
			return operation1.getAnonymousClassList();
		if(attribute1 != null)
			return attribute1.getAnonymousClassList();
		return Collections.emptyList();
	}

	private List<UMLAnonymousClass> anonymousClassList2() {
		if(operation2 != null)
			return operation2.getAnonymousClassList();
		if(attribute2 != null)
			return attribute2.getAnonymousClassList();
		return Collections.emptyList();
	}

	private void processStreamAPIStatements(List<AbstractCodeFragment> leaves1, List<AbstractCodeFragment> leaves2,
			List<CompositeStatementObject> innerNodes1, Set<AbstractCodeFragment> streamAPIStatements2)
			throws RefactoringMinerTimedOutException {
		//match expressions in inner nodes from T1 with leaves from T2
		List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
		for(CompositeStatementObject composite : innerNodes1) {
			if(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT1.add(expression);
				}
			}
		}
		int numberOfMappings = mappings.size();
		processLeaves(expressionsT1, leaves2, new LinkedHashMap<String, String>());
		
		List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
		if(numberOfMappings == mappings.size()) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				for(AbstractCodeMapping mapping : mappings) {
					AbstractCodeFragment fragment1 = mapping.getFragment1();
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if(composite.getLeaves().contains(fragment1) &&
							(composite.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							composite.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT))) {
						AbstractCodeFragment streamAPICallStatement = null;
						List<AbstractCall> streamAPICalls = null;
						for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
							if(leaves2.contains(leaf2)) {
								boolean matchingLambda = false;
								for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
									if(lambda.getExpression() != null && lambda.getExpression().equals(fragment2)) {
										matchingLambda = true;
										break;
									}
								}
								if(matchingLambda) {
									streamAPICallStatement = leaf2;
									streamAPICalls = streamAPICalls(leaf2);
									break;
								}
							}
						}
						if(streamAPICallStatement != null && streamAPICalls != null) {
							Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
							additionallyMatchedStatements1.add(fragment1);
							Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
							additionallyMatchedStatements2.add(streamAPICallStatement);
							for(AbstractCall streamAPICall : streamAPICalls) {
								if(streamAPICall.getName().equals("forEach")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(composite);
												break;
											}
										}
									}
								}
								else if(streamAPICall.getName().equals("stream")) {
									if(!additionallyMatchedStatements1.contains(composite)) {
										for(AbstractExpression expression : composite.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(composite);
												break;
											}
											for(String argument : streamAPICall.getArguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements1.add(composite);
													break;
												}
											}
										}
									}
								}
							}
							CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
							Set<Replacement> replacements = new LinkedHashSet<>();
							replacements.add(replacement);
							LeafMapping newMapping = createLeafMapping(composite, streamAPICallStatement, new LinkedHashMap<String, String>());
							newMapping.addReplacements(replacements);
							TreeSet<LeafMapping> mappingSet = new TreeSet<>();
							mappingSet.add(newMapping);
							ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, operation1, operation2);
							newMapping.addRefactoring(ref);
							addToMappings(newMapping, mappingSet);
							leaves2.remove(newMapping.getFragment2());
							innerNodeIterator1.remove();
						}
					}
				}
			}
		}
		for(int i = numberOfMappings; i < mappings.size(); i++) {
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject composite = innerNodeIterator1.next();
				AbstractCodeMapping mapping = mappings.get(i);
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(composite.getExpressions().contains(fragment1)) {
					AbstractCodeFragment streamAPICallStatement = null;
					List<AbstractCall> streamAPICalls = null;
					for(AbstractCodeFragment leaf2 : streamAPIStatements2) {
						if(leaves2.contains(leaf2)) {
							boolean matchingLambda = false;
							for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
								if(lambda.getExpression() != null && lambda.getExpression().equals(fragment2)) {
									matchingLambda = true;
									break;
								}
							}
							if(matchingLambda) {
								streamAPICallStatement = leaf2;
								streamAPICalls = streamAPICalls(leaf2);
								break;
							}
						}
					}
					if(streamAPICallStatement != null && streamAPICalls != null) {
						Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
						additionallyMatchedStatements1.add(composite);
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						additionallyMatchedStatements2.add(streamAPICallStatement);
						for(AbstractCall streamAPICall : streamAPICalls) {
							if(streamAPICall.getName().equals("filter")) {
								for(AbstractCodeFragment leaf1 : leaves1) {
									AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
									if(invocation != null && invocation.getName().equals("add")) {
										for(String argument : invocation.getArguments()) {
											if(streamAPICall.getArguments().get(0).startsWith(argument + " -> ")) {
												additionallyMatchedStatements1.add(leaf1);
												break;
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("stream")) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
											for(String argument : streamAPICall.getArguments()) {
												if(expression.getString().equals(argument)) {
													additionallyMatchedStatements1.add(comp1);
													break;
												}
											}
										}
									}
								}
							}
							else if(streamAPICall.getName().equals("forEach")) {
								for(CompositeStatementObject comp1 : innerNodes1) {
									if(!additionallyMatchedStatements1.contains(comp1)) {
										for(AbstractExpression expression : comp1.getExpressions()) {
											if(expression.getString().equals(streamAPICall.getExpression())) {
												additionallyMatchedStatements1.add(comp1);
												break;
											}
										}
									}
								}
							}
						}
						CompositeReplacement replacement = new CompositeReplacement(composite.getString(), streamAPICallStatement.getString(), additionallyMatchedStatements1, additionallyMatchedStatements2);
						Set<Replacement> replacements = new LinkedHashSet<>();
						replacements.add(replacement);
						LeafMapping newMapping = createLeafMapping(composite, streamAPICallStatement, new LinkedHashMap<String, String>());
						newMapping.addReplacements(replacements);
						TreeSet<LeafMapping> mappingSet = new TreeSet<>();
						mappingSet.add(newMapping);
						ReplaceLoopWithPipelineRefactoring ref = new ReplaceLoopWithPipelineRefactoring(additionallyMatchedStatements1, additionallyMatchedStatements2, operation1, operation2);
						newMapping.addRefactoring(ref);
						addToMappings(newMapping, mappingSet);
						leaves2.remove(newMapping.getFragment2());
						innerNodeIterator1.remove();
					}
				}
			}
		}
	}

	private UMLOperationBodyMapper(UMLOperation anonymousClassOperation, LambdaExpressionObject lambda2, UMLOperationBodyMapper parentMapper) throws RefactoringMinerTimedOutException {
		this.classDiff = parentMapper.classDiff;
		if(classDiff != null)
			this.modelDiff = classDiff.getModelDiff();
		this.operation1 = parentMapper.operation1;
		this.operation2 = parentMapper.operation2;
		this.mappings = new LinkedHashSet<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedLeavesT2 = new ArrayList<AbstractCodeFragment>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		if(anonymousClassOperation.getBody() != null) {
			CompositeStatementObject composite1 = anonymousClassOperation.getBody().getCompositeStatement();
			if(lambda2.getBody() != null) {
				CompositeStatementObject composite2 = lambda2.getBody().getCompositeStatement();
				processCompositeStatements(composite1.getLeaves(), composite2.getLeaves(), composite1.getInnerNodes(), composite2.getInnerNodes());
			}
			else if(lambda2.getExpression() != null) {
				List<AbstractCodeFragment> leaves2 = new ArrayList<AbstractCodeFragment>();
				leaves2.add(lambda2.getExpression());
				processCompositeStatements(composite1.getLeaves(), leaves2, composite1.getInnerNodes(), new ArrayList<CompositeStatementObject>());
			}
		}
	}

	public void addChildMapper(UMLOperationBodyMapper mapper) {
		this.childMappers.add(mapper);
		//TODO add logic to remove the mappings from "this" mapper,
		//which are less similar than the mappings of the mapper passed as parameter
	}

	public List<UMLOperationBodyMapper> getChildMappers() {
		return childMappers;
	}

	public UMLOperationBodyMapper getParentMapper() {
		return parentMapper;
	}

	public UMLOperation getCallSiteOperation() {
		return callSiteOperation;
	}

	private void resetNodes(List<? extends AbstractCodeFragment> nodes) {
		for(AbstractCodeFragment node : nodes) {
			node.resetArgumentization();
		}
	}
	
	private boolean returnWithVariableReplacement(AbstractCodeMapping mapping) {
		if(mapping.getReplacements().size() == 1) {
			Replacement r = mapping.getReplacements().iterator().next();
			if(r.getType().equals(ReplacementType.VARIABLE_NAME)) {
				String fragment1 = mapping.getFragment1().getString();
				String fragment2 = mapping.getFragment2().getString();
				return fragment1.equals("return " + r.getBefore() + ";\n") && fragment2.equals("return " + r.getAfter() + ";\n");
			}
		}
		return false;
	}

	private boolean nullLiteralReplacements(AbstractCodeMapping mapping) {
		int numberOfReplacements = mapping.getReplacements().size();
		int nullLiteralReplacements = 0;
		int methodInvocationReplacementsToIgnore = 0;
		int variableNameReplacementsToIgnore = 0;
		for(Replacement replacement : mapping.getReplacements()) {
			if(replacement.getType().equals(ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION) ||
					replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL) ||
					(replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE) && (replacement.getBefore().equals("null") || replacement.getAfter().equals("null")))) {
				nullLiteralReplacements++;
			}
			else if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement invocationReplacement = (MethodInvocationReplacement)replacement;
				AbstractCall invokedOperationBefore = invocationReplacement.getInvokedOperationBefore();
				AbstractCall invokedOperationAfter = invocationReplacement.getInvokedOperationAfter();
				if(invokedOperationBefore.getName().equals(invokedOperationAfter.getName()) &&
						invokedOperationBefore.getArguments().size() == invokedOperationAfter.getArguments().size()) {
					methodInvocationReplacementsToIgnore++;
				}
			}
			else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				variableNameReplacementsToIgnore++;
			}
		}
		return nullLiteralReplacements > 0 && numberOfReplacements == nullLiteralReplacements + methodInvocationReplacementsToIgnore + variableNameReplacementsToIgnore;
	}

	public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2, UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
		this.parentMapper = operationBodyMapper;
		this.operation1 = operationBodyMapper.operation1;
		this.callSiteOperation = operationBodyMapper.operation2;
		this.operation2 = addedOperation;
		this.classDiff = classDiff;
		this.mappings = new LinkedHashSet<>();
		this.nonMappedLeavesT1 = new ArrayList<>();
		this.nonMappedLeavesT2 = new ArrayList<>();
		this.nonMappedInnerNodesT1 = new ArrayList<>();
		this.nonMappedInnerNodesT2 = new ArrayList<>();
		
		OperationBody addedOperationBody = addedOperation.getBody();
		if(addedOperationBody != null) {
			CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = operationBodyMapper.getNonMappedLeavesT1();
			List<CompositeStatementObject> innerNodes1 = operationBodyMapper.getNonMappedInnerNodesT1();
			//adding leaves that were mapped with replacements
			Set<AbstractCodeFragment> addedLeaves1 = new LinkedHashSet<>();
			Set<CompositeStatementObject> addedInnerNodes1 = new LinkedHashSet<>();
			for(AbstractCodeFragment nonMappedLeaf1 : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT1())) {
				expandAnonymousAndLambdas(nonMappedLeaf1, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, operation1);
			}
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!returnWithVariableReplacement(mapping) && !nullLiteralReplacements(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, operationBodyMapper.anonymousClassList1(), codeFragmentOperationMap1, operation1);
				}
			}
			for(UMLOperationBodyMapper childMapper : operationBodyMapper.childMappers) {
				if(childMapper.getOperation1().getClassName().equals(addedOperation.getClassName()) || classDiff instanceof UMLClassMoveDiff) {
					for(AbstractCodeMapping mapping : childMapper.getMappings()) {
						if(!returnWithVariableReplacement(mapping) && !nullLiteralReplacements(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
							AbstractCodeFragment fragment = mapping.getFragment1();
							expandAnonymousAndLambdas(fragment, leaves1, innerNodes1, addedLeaves1, addedInnerNodes1, childMapper.anonymousClassList1(), codeFragmentOperationMap1, operation1);
						}
					}
				}
			}
			List<AbstractCodeFragment> leaves2 = composite2.getLeaves();
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			Set<AbstractCodeFragment> addedLeaves2 = new LinkedHashSet<AbstractCodeFragment>();
			Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<CompositeStatementObject>();
			for(AbstractCodeFragment statement : new ArrayList<>(composite2.getLeaves())) {
				expandAnonymousAndLambdas(statement, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, anonymousClassList2(), codeFragmentOperationMap2, operation2);
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap2);
			
			//adding innerNodes that were mapped with replacements
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject statement = (CompositeStatementObject)fragment;
						if(!innerNodes1.contains(statement)) {
							innerNodes1.add(statement);
							addedInnerNodes1.add(statement);
						}
					}
				}
			}
			innerNodes2.remove(composite2);
			innerNodes2.addAll(addedInnerNodes2);
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNode2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap2);
			
			//match expressions in inner nodes from T1 with leaves from T2
			List<AbstractExpression> expressionsT1 = new ArrayList<>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expression.replaceParametersWithArguments(parameterToArgumentMap1);
					expressionsT1.add(expression);
				}
			}
			int numberOfMappings = mappings.size();
			for(AbstractCodeMapping mapping : this.mappings) {
				if(mapping instanceof LeafMapping) {
					AbstractCodeFragment fragment2 = mapping.getFragment2();
					if(fragment2 instanceof StatementObject) {
						addedLeaves2.add((StatementObject)fragment2);
						leaves2.add((StatementObject)fragment2);
					}
				}
			}
			processLeaves(expressionsT1, leaves2, parameterToArgumentMap2);
			List<AbstractCodeMapping> mappings = new ArrayList<>(this.mappings);
			for(int i = numberOfMappings; i < mappings.size(); i++) {
				mappings.get(i).temporaryVariableAssignment(refactorings, parentMapper != null);
			}
			// TODO remove non-mapped inner nodes from T1 corresponding to mapped expressions
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes1.removeAll(addedInnerNodes1);
			innerNodes2.removeAll(addedInnerNodes2);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
			}
		}
	}

	private void expandAnonymousAndLambdas(AbstractCodeFragment fragment, List<AbstractCodeFragment> leaves, List<CompositeStatementObject> innerNodes,
			Set<AbstractCodeFragment> addedLeaves, Set<CompositeStatementObject> addedInnerNodes,
			List<UMLAnonymousClass> anonymousClassList, Map<AbstractCodeFragment, UMLOperation> map, UMLOperation parentOperation) {
		if(fragment instanceof StatementObject) {
			StatementObject statement = (StatementObject)fragment;
			if(!leaves.contains(statement)) {
				leaves.add(statement);
				addedLeaves.add(statement);
			}
			if(!statement.getAnonymousClassDeclarations().isEmpty()) {
				for(UMLAnonymousClass anonymous : anonymousClassList) {
					if(statement.getLocationInfo().subsumes(anonymous.getLocationInfo())) {
						for(UMLOperation anonymousOperation : anonymous.getOperations()) {
							if(anonymousOperation.getBody() != null) {
								List<AbstractCodeFragment> anonymousClassLeaves = anonymousOperation.getBody().getCompositeStatement().getLeaves();
								for(AbstractCodeFragment anonymousLeaf : anonymousClassLeaves) {
									if(!leaves.contains(anonymousLeaf)) {
										leaves.add(anonymousLeaf);
										addedLeaves.add(anonymousLeaf);
										map.put(anonymousLeaf, anonymousOperation);
									}
								}
								List<CompositeStatementObject> anonymousClassInnerNodes = anonymousOperation.getBody().getCompositeStatement().getInnerNodes();
								for(CompositeStatementObject anonymousInnerNode : anonymousClassInnerNodes) {
									if(!innerNodes.contains(anonymousInnerNode)) {
										innerNodes.add(anonymousInnerNode);
										addedInnerNodes.add(anonymousInnerNode);
										map.put(anonymousInnerNode, anonymousOperation);
									}
								}
							}
						}
					}
				}
			}
			if(!statement.getLambdas().isEmpty()) {
				for(LambdaExpressionObject lambda : statement.getLambdas()) {
					if(lambda.getBody() != null) {
						List<AbstractCodeFragment> lambdaLeaves = lambda.getBody().getCompositeStatement().getLeaves();
						for(AbstractCodeFragment lambdaLeaf : lambdaLeaves) {
							if(!leaves.contains(lambdaLeaf)) {
								leaves.add(lambdaLeaf);
								addedLeaves.add(lambdaLeaf);
								map.put(lambdaLeaf, parentOperation);
							}
						}
						List<CompositeStatementObject> lambdaInnerNodes = lambda.getBody().getCompositeStatement().getInnerNodes();
						for(CompositeStatementObject lambdaInnerNode : lambdaInnerNodes) {
							if(!innerNodes.contains(lambdaInnerNode)) {
								innerNodes.add(lambdaInnerNode);
								addedInnerNodes.add(lambdaInnerNode);
								map.put(lambdaInnerNode, parentOperation);
							}
						}
					}
					else if(lambda.getExpression() != null) {
						AbstractCodeFragment lambdaLeaf = lambda.getExpression();
						if(!leaves.contains(lambdaLeaf)) {
							leaves.add(lambdaLeaf);
							addedLeaves.add(lambdaLeaf);
							map.put(lambdaLeaf, parentOperation);
						}
					}
				}
			}
		}
	}

	public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper operationBodyMapper,
			Map<String, String> parameterToArgumentMap1, Map<String, String> parameterToArgumentMap2, UMLClassBaseDiff classDiff) throws RefactoringMinerTimedOutException {
		this.parentMapper = operationBodyMapper;
		this.operation1 = removedOperation;
		this.operation2 = operationBodyMapper.operation2;
		this.callSiteOperation = operationBodyMapper.operation1;
		this.classDiff = classDiff;
		this.mappings = new LinkedHashSet<>();
		this.nonMappedLeavesT1 = new ArrayList<>();
		this.nonMappedLeavesT2 = new ArrayList<>();
		this.nonMappedInnerNodesT1 = new ArrayList<>();
		this.nonMappedInnerNodesT2 = new ArrayList<>();
		
		OperationBody removedOperationBody = removedOperation.getBody();
		if(removedOperationBody != null) {
			CompositeStatementObject composite1 = removedOperationBody.getCompositeStatement();
			List<AbstractCodeFragment> leaves1 = composite1.getLeaves();
			List<AbstractCodeFragment> leaves2 = operationBodyMapper.getNonMappedLeavesT2();
			//adding leaves that were mapped with replacements or are inexact matches
			Set<AbstractCodeFragment> addedLeaves2 = new LinkedHashSet<>();
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			List<CompositeStatementObject> innerNodes2 = operationBodyMapper.getNonMappedInnerNodesT2();
			//adding innerNodes that were mapped with replacements or are inexact matches
			Set<CompositeStatementObject> addedInnerNodes2 = new LinkedHashSet<>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!returnWithVariableReplacement(mapping) && !nullLiteralReplacements(mapping) && (!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2()))) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof StatementObject) {
						StatementObject statement = (StatementObject)fragment;
						if(!leaves2.contains(statement)) {
							leaves2.add(statement);
							addedLeaves2.add(statement);
						}
					}
				}
			}
			resetNodes(leaves1);
			//replace parameters with arguments in leaves1
			if(!parameterToArgumentMap1.isEmpty()) {
				//check for temporary variables that the argument might be assigned to
				for(AbstractCodeFragment leave2 : leaves2) {
					List<VariableDeclaration> variableDeclarations = leave2.getVariableDeclarations();
					for(VariableDeclaration variableDeclaration : variableDeclarations) {
						for(String parameter : parameterToArgumentMap1.keySet()) {
							String argument = parameterToArgumentMap1.get(parameter);
							if(variableDeclaration.getInitializer() != null && argument.equals(variableDeclaration.getInitializer().toString())) {
								parameterToArgumentMap1.put(parameter, variableDeclaration.getVariableName());
							}
						}
					}
				}
				for(AbstractCodeFragment leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			for(AbstractCodeFragment statement : new ArrayList<>(operationBodyMapper.getNonMappedLeavesT2())) {
				expandAnonymousAndLambdas(statement, leaves2, innerNodes2, addedLeaves2, addedInnerNodes2, anonymousClassList2(), codeFragmentOperationMap2, operation2);
			}
			resetNodes(leaves2);
			//replace parameters with arguments in leaves2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(AbstractCodeFragment leave2 : leaves2) {
					leave2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2, parameterToArgumentMap1);

			innerNodes1.remove(composite1);
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty() || !mapping.getFragment1().equalFragment(mapping.getFragment2())) {
					AbstractCodeFragment fragment = mapping.getFragment2();
					if(fragment instanceof CompositeStatementObject) {
						CompositeStatementObject statement = (CompositeStatementObject)fragment;
						if(!innerNodes2.contains(statement)) {
							innerNodes2.add(statement);
							addedInnerNodes2.add(statement);
						}
					}
				}
			}
			resetNodes(innerNodes1);
			//replace parameters with arguments in innerNodes1
			if(!parameterToArgumentMap1.isEmpty()) {
				for(CompositeStatementObject innerNode1 : innerNodes1) {
					innerNode1.replaceParametersWithArguments(parameterToArgumentMap1);
				}
			}
			resetNodes(innerNodes2);
			//replace parameters with arguments in innerNode2
			if(!parameterToArgumentMap2.isEmpty()) {
				for(CompositeStatementObject innerNode2 : innerNodes2) {
					innerNode2.replaceParametersWithArguments(parameterToArgumentMap2);
				}
			}
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2, parameterToArgumentMap1);
			
			//match expressions in inner nodes from T2 with leaves from T1
			List<AbstractExpression> expressionsT2 = new ArrayList<>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expression.replaceParametersWithArguments(parameterToArgumentMap2);
					expressionsT2.add(expression);
				}
			}
			processLeaves(leaves1, expressionsT2, parameterToArgumentMap1);
			
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves2.removeAll(addedLeaves2);
			//remove the innerNodes that were mapped with replacement, if they are not mapped again for a second time
			innerNodes2.removeAll(addedInnerNodes2);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
			
			for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
				temporaryVariableAssignment(statement, nonMappedLeavesT2);
			}
			for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
				inlinedVariableAssignment(statement, nonMappedLeavesT2);
			}
		}
	}

	public UMLOperation getOperation1() {
		return operation1;
	}

	public UMLOperation getOperation2() {
		return operation2;
	}

	public Set<Pair<VariableDeclaration, VariableDeclaration>> getMatchedVariables() {
		return matchedVariables;
	}

	public Set<Refactoring> getRefactorings() {
		VariableReplacementAnalysis analysis = new VariableReplacementAnalysis(this, refactorings, classDiff);
		refactorings.addAll(analysis.getVariableRenames());
		refactorings.addAll(analysis.getVariableMerges());
		refactorings.addAll(analysis.getVariableSplits());
		matchedVariables.addAll(analysis.getMatchedVariables());
		candidateAttributeRenames.addAll(analysis.getCandidateAttributeRenames());
		candidateAttributeMerges.addAll(analysis.getCandidateAttributeMerges());
		candidateAttributeSplits.addAll(analysis.getCandidateAttributeSplits());
		return refactorings;
	}

	public Set<Refactoring> getRefactoringsAfterPostProcessing() {
		return refactorings;
	}

	public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	public Set<CandidateMergeVariableRefactoring> getCandidateAttributeMerges() {
		return candidateAttributeMerges;
	}

	public Set<CandidateSplitVariableRefactoring> getCandidateAttributeSplits() {
		return candidateAttributeSplits;
	}

	public Set<AbstractCodeMapping> getMappings() {
		return mappings;
	}

	public List<AbstractCodeFragment> getNonMappedLeavesT1() {
		return nonMappedLeavesT1;
	}

	public List<AbstractCodeFragment> getNonMappedLeavesT2() {
		return nonMappedLeavesT2;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT1() {
		return nonMappedInnerNodesT1;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT2() {
		return nonMappedInnerNodesT2;
	}

	public List<CompositeReplacement> getCompositeReplacements() {
		List<CompositeReplacement> composites = new ArrayList<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			CompositeReplacement composite = mapping.containsCompositeReplacement();
			if(composite != null) {
				composites.add(composite);
			}
		}
		return composites;
	}

	public int mappingsWithoutBlocks() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement())
				count++;
		}
		return count;
	}

	public int nonMappedElementsT1() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(composite.countableStatement())
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement())
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT1() {
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement())
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	public int nonMappedElementsT2() {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(composite.countableStatement())
				nonMappedInnerNodeCount++;
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement() && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedLeafElementsT2() {
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement() && !isTemporaryVariableAssignment(statement))
				nonMappedLeafCount++;
		}
		return nonMappedLeafCount;
	}

	private boolean isTemporaryVariableAssignment(AbstractCodeFragment statement) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariable = (ExtractVariableRefactoring)refactoring;
				if(statement.getVariableDeclarations().contains(extractVariable.getVariableDeclaration())) {
					return true;
				}
			}
		}
		return false;
	}

	private void inlinedVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) {
		for(AbstractCodeMapping mapping : getMappings()) {
			mapping.inlinedVariableAssignment(statement, nonMappedLeavesT2, parentMapper != null);
			refactorings.addAll(mapping.getRefactorings());
		}
	}

	private void temporaryVariableAssignment(AbstractCodeFragment statement, List<AbstractCodeFragment> nonMappedLeavesT2) {
		for(AbstractCodeMapping mapping : getMappings()) {
			UMLClassBaseDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
			mapping.temporaryVariableAssignment(statement, nonMappedLeavesT2, classDiff, parentMapper != null);
			refactorings.addAll(mapping.getRefactorings());
		}
	}

	public int nonMappedElementsT2CallingAddedOperation(List<UMLOperation> addedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT2()) {
			if(composite.countableStatement()) {
				Map<String, List<AbstractCall>> methodInvocationMap = composite.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					for(AbstractCall invocation : methodInvocationMap.get(key)) {
						for(UMLOperation operation : addedOperations) {
							if(invocation.matchesOperation(operation, operation2, modelDiff)) {
								nonMappedInnerNodeCount++;
								break;
							}
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT2()) {
			if(statement.countableStatement()) {
				Map<String, List<AbstractCall>> methodInvocationMap = statement.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					for(AbstractCall invocation : methodInvocationMap.get(key)) {
						for(UMLOperation operation : addedOperations) {
							if(invocation.matchesOperation(operation, operation2, modelDiff)) {
								nonMappedLeafCount++;
								break;
							}
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public int nonMappedElementsT1CallingRemovedOperation(List<UMLOperation> removedOperations) {
		int nonMappedInnerNodeCount = 0;
		for(CompositeStatementObject composite : getNonMappedInnerNodesT1()) {
			if(composite.countableStatement()) {
				Map<String, List<AbstractCall>> methodInvocationMap = composite.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					for(AbstractCall invocation : methodInvocationMap.get(key)) {
						for(UMLOperation operation : removedOperations) {
							if(invocation.matchesOperation(operation, operation1, modelDiff)) {
								nonMappedInnerNodeCount++;
								break;
							}
						}
					}
				}
			}
		}
		int nonMappedLeafCount = 0;
		for(AbstractCodeFragment statement : getNonMappedLeavesT1()) {
			if(statement.countableStatement()) {
				Map<String, List<AbstractCall>> methodInvocationMap = statement.getMethodInvocationMap();
				for(String key : methodInvocationMap.keySet()) {
					for(AbstractCall invocation : methodInvocationMap.get(key)) {
						for(UMLOperation operation : removedOperations) {
							if(invocation.matchesOperation(operation, operation1, modelDiff)) {
								nonMappedLeafCount++;
								break;
							}
						}
					}
				}
			}
		}
		return nonMappedLeafCount + nonMappedInnerNodeCount;
	}

	public boolean callsRemovedAndAddedOperation(List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		boolean removedOperationCalled = false;
		for(AbstractCall invocation : operation1.getAllOperationInvocations()) {
			for(UMLOperation operation : removedOperations) {
				if(invocation.matchesOperation(operation, operation1, modelDiff)) {
					removedOperationCalled = true;
					break;
				}
			}
			if(removedOperationCalled)
				break;
		}
		boolean addedOperationCalled = false;
		for(AbstractCall invocation : operation2.getAllOperationInvocations()) {
			for(UMLOperation operation : addedOperations) {
				if(invocation.matchesOperation(operation, operation2, modelDiff)) {
					addedOperationCalled = true;
					break;
				}
			}
			if(addedOperationCalled)
				break;
		}
		return removedOperationCalled && addedOperationCalled;
	}

	public int exactMatches() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try"))
				count++;
		}
		return count;
	}

	public List<AbstractCodeMapping> getExactMatches() {
		List<AbstractCodeMapping> exactMatches = new ArrayList<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact() && mapping.getFragment1().countableStatement() && mapping.getFragment2().countableStatement() &&
					!mapping.getFragment1().getString().equals("try"))
				exactMatches.add(mapping);
		}
		return exactMatches;
	}

	public boolean allMappingsAreExactMatches() {
		int mappings = this.mappingsWithoutBlocks();
		int tryMappings = 0;
		int mappingsWithTypeReplacement = 0;
		int mappingsWithVariableReplacement = 0;
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.getFragment1().getString().equals("try") && mapping.getFragment2().getString().equals("try")) {
				tryMappings++;
			}
			if(mapping.containsOnlyReplacement(ReplacementType.TYPE)) {
				mappingsWithTypeReplacement++;
			}
			if(mapping.containsOnlyReplacement(ReplacementType.VARIABLE_NAME)) {
				mappingsWithVariableReplacement++;
			}
		}
		if(mappings == this.exactMatches() + tryMappings) {
			return true;
		}
		if(mappings == this.exactMatches() + tryMappings + mappingsWithTypeReplacement && mappings > mappingsWithTypeReplacement) {
			return true;
		}
		return mappings == this.exactMatches() + tryMappings + mappingsWithVariableReplacement && mappings > mappingsWithVariableReplacement;
	}

	private int editDistance() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				continue;
			}
			String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
			String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
			if(!s1.equals(s2)) {
				count += StringDistance.editDistance(s1, s2);
			}
		}
		return count;
	}

	public double normalizedEditDistance() {
		double editDistance = 0;
		double maxLength = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				continue;
			}
			String s1 = preprocessInput1(mapping.getFragment1(), mapping.getFragment2());
			String s2 = preprocessInput2(mapping.getFragment1(), mapping.getFragment2());
			if(!s1.equals(s2)) {
				editDistance += StringDistance.editDistance(s1, s2);
				maxLength += Math.max(s1.length(), s2.length());
			}
		}
		return editDistance/maxLength;
	}

	public int operationNameEditDistance() {
		return StringDistance.editDistance(this.operation1.getName(), this.operation2.getName());
	}

	public Set<Replacement> getReplacements() {
		Set<Replacement> replacements = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			replacements.addAll(mapping.getReplacements());
		}
		return replacements;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
		Set<Replacement> replacements = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(involvesMethodInvocation(replacement)) {
					replacements.add(replacement);
				}
			}
		}
		if(replacements.isEmpty()) {
			for(UMLOperationBodyMapper childMapper : childMappers) {
				for(AbstractCodeMapping mapping : childMapper.getMappings()) {
					for(Replacement replacement : mapping.getReplacements()) {
						if(involvesMethodInvocation(replacement)) {
							replacements.add(replacement);
						}
					}
				}
			}
		}
		return replacements;
	}

	private boolean involvesMethodInvocation(Replacement replacement) {
		return replacement instanceof MethodInvocationReplacement ||
				replacement instanceof VariableReplacementWithMethodInvocation ||
				replacement instanceof ClassInstanceCreationWithMethodInvocationReplacement ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION) ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) ||
				replacement instanceof IntersectionReplacement ||
				replacement.getType().equals(ReplacementType.ANONYMOUS_CLASS_DECLARATION);
	}

	public Set<MethodInvocationReplacement> getMethodInvocationRenameReplacements() {
		Set<MethodInvocationReplacement> replacements = new LinkedHashSet<>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) ||
						replacement.getType().equals(ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
					replacements.add((MethodInvocationReplacement) replacement);
				}
			}
		}
		return replacements;
	}

	public boolean involvesTestMethods() {
		return operation1.hasTestAnnotation() && operation2.hasTestAnnotation();
	}

	public void processInnerNodes(List<CompositeStatementObject> innerNodes1, List<CompositeStatementObject> innerNodes2,
			Map<String, String> parameterToArgumentMap) throws RefactoringMinerTimedOutException {
		List<UMLOperation> removedOperations = classDiff != null ? classDiff.getRemovedOperations() : new ArrayList<>();
		List<UMLOperation> addedOperations = classDiff != null ? classDiff.getAddedOperations() : new ArrayList<>();
		int tryWithResources1 = tryWithResourcesCount(innerNodes1);
		int tryWithResources2 = tryWithResourcesCount(innerNodes2);
		boolean tryWithResourceMigration = (tryWithResources1 == 0 && tryWithResources2 > 0) || (tryWithResources1 > 0 && tryWithResources2 == 0);
		if(innerNodes1.size() <= innerNodes2.size()) {
			//exact string+depth matching - inner nodes
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject statement1 = innerNodeIterator1.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement2 : innerNodes2) {
					if ((statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
							statement1.getDepth() == statement2.getDepth()) {
						double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
						if (score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
							CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
							mappingSet.add(mapping);
						}
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes2.remove(minStatementMapping.getFragment2());
					innerNodeIterator1.remove();
				}
			}
			
			//exact string matching - inner nodes - finds moves to another level
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject statement1 = innerNodeIterator1.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement2 : innerNodes2) {
					if (statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) {
						double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
						if (score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
							CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
							mappingSet.add(mapping);
						}
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes2.remove(minStatementMapping.getFragment2());
					innerNodeIterator1.remove();
				}
			}
			
			// exact matching - inner nodes - with variable renames
			for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
				CompositeStatementObject statement1 = innerNodeIterator1.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement2 : innerNodes2) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, innerNodes1, innerNodes2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo);

					double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
					if (score == 0 && replacements != null && replacements.size() == 1 &&
							(replacements.iterator().next().getType().equals(ReplacementType.INFIX_OPERATOR) || replacements.iterator().next().getType().equals(ReplacementType.INVERT_CONDITIONAL))) {
						//special handling when there is only an infix operator or invert conditional replacement, but no children mapped
						score = 1;
					}
					if (replacements != null &&
							(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
						CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
						mapping.addReplacements(replacements);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes2.remove(minStatementMapping.getFragment2());
					innerNodeIterator1.remove();
				}
			}
		}
		else {
			//exact string+depth matching - inner nodes
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement1 : innerNodes1) {
					if ((statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) &&
							statement1.getDepth() == statement2.getDepth()) {
						double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
						if (score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
							CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
							mappingSet.add(mapping);
						}
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes1.remove(minStatementMapping.getFragment1());
					innerNodeIterator2.remove();
				}
			}
			
			//exact string matching - inner nodes - finds moves to another level
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement1 : innerNodes1) {
					if (statement1.getString().equals(statement2.getString()) || statement1.getArgumentizedString().equals(statement2.getArgumentizedString())) {
						double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
						if (score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0) {
							CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
							mappingSet.add(mapping);
						}
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes1.remove(minStatementMapping.getFragment1());
					innerNodeIterator2.remove();
				}
			}
			
			// exact matching - inner nodes - with variable renames
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<>();
				for (CompositeStatementObject statement1 : innerNodes1) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(statement1, statement2, innerNodes1, innerNodes2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2, parameterToArgumentMap, replacementInfo);

					double score = computeScore(statement1, statement2, removedOperations, addedOperations, tryWithResourceMigration);
					if (score == 0 && replacements != null && replacements.size() == 1 &&
							(replacements.iterator().next().getType().equals(ReplacementType.INFIX_OPERATOR) || replacements.iterator().next().getType().equals(ReplacementType.INVERT_CONDITIONAL))) {
						//special handling when there is only an infix operator or invert conditional replacement, but no children mapped
						score = 1;
					}
					if(replacements != null &&
							(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
						CompositeStatementObjectMapping mapping = createCompositeMapping(statement1, statement2, parameterToArgumentMap, score);
						mapping.addReplacements(replacements);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					innerNodes1.remove(minStatementMapping.getFragment1());
					innerNodeIterator2.remove();
				}
			}
		}
	}

	private static int tryWithResourcesCount(List<CompositeStatementObject> innerNodes) {
		int tryWithResources = 0;
		for(CompositeStatementObject comp1 : innerNodes) {
			if(comp1 instanceof TryStatementObject) {
				if(comp1.getExpressions().size() > 0) {
					tryWithResources++;
				}
			}
		}
		return tryWithResources;
	}

	private double computeScore(CompositeStatementObject statement1, CompositeStatementObject statement2,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			return compositeChildMatchingScore((TryStatementObject)statement1, (TryStatementObject)statement2, mappings, removedOperations, addedOperations, tryWithResourceMigration);
		}
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
			for(AbstractCodeMapping mapping : mappings) {
				if(mapping.getFragment1() instanceof TryStatementObject && mapping.getFragment2() instanceof TryStatementObject) {
					TryStatementObject try1 = (TryStatementObject)mapping.getFragment1();
					TryStatementObject try2 = (TryStatementObject)mapping.getFragment2();
					if(try1.getCatchClauses().contains(statement1) && try2.getCatchClauses().contains(statement2)) {
						return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);
					}
				}
			}
			return -1;
		}
		return compositeChildMatchingScore(statement1, statement2, mappings, removedOperations, addedOperations);
	}

	private CompositeStatementObjectMapping createCompositeMapping(CompositeStatementObject statement1,
			CompositeStatementObject statement2, Map<String, String> parameterToArgumentMap, double score) {
		UMLOperation operation1 = codeFragmentOperationMap1.containsKey(statement1) ? codeFragmentOperationMap1.get(statement1) : this.operation1;
		UMLOperation operation2 = codeFragmentOperationMap2.containsKey(statement2) ? codeFragmentOperationMap2.get(statement2) : this.operation2;
		CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
		for(String key : parameterToArgumentMap.keySet()) {
			String value = parameterToArgumentMap.get(key);
			if(!key.equals(value) && ReplacementUtil.contains(statement2.getString(), key) && ReplacementUtil.contains(statement1.getString(), value)) {
				mapping.addReplacement(new Replacement(value, key, ReplacementType.VARIABLE_NAME));
			}
		}
		return mapping;
	}

	public void processLeaves(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2,
			Map<String, String> parameterToArgumentMap) throws RefactoringMinerTimedOutException {
		List<TreeSet<LeafMapping>> postponedMappingSets = new ArrayList<>();
		if(leaves1.size() <= leaves2.size()) {
			//exact string+depth matching - leaf nodes
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf2 : leaves2) {
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves2.remove(minStatementMapping.getFragment2());
					leafIterator1.remove();
				}
			}
			
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf2 : leaves2) {
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves2.remove(minStatementMapping.getFragment2());
					leafIterator1.remove();
				}
			}
			
			// exact matching with variable renames
			for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
				AbstractCodeFragment leaf1 = leafIterator1.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf2 : leaves2) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mapping.addReplacements(replacements);
						for(AbstractCodeFragment leaf : leaves2) {
							if(leaf.equals(leaf2)) {
								break;
							}
							UMLClassBaseDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
							mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
							if(mapping.isIdenticalWithExtractedVariable()) {
								break;
							}
						}
						for (AbstractCodeFragment leaf : leaves1) {
							if (leaf.equals(leaf1)) {
								break;
							}
							mapping.inlinedVariableAssignment(leaf, leaves2, parentMapper != null);
							if(mapping.isIdenticalWithInlinedVariable()) {
								break;
							}
						}
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> switchParentEntry;
					if(variableDeclarationMappingsWithSameReplacementTypes(mappingSet)) {
						//postpone mapping
						postponedMappingSets.add(mappingSet);
					}
					else if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
						LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
						addToMappings(bestMapping, mappingSet);
						leaves2.remove(bestMapping.getFragment2());
						leafIterator1.remove();
					}
					else {
						LeafMapping minStatementMapping = mappingSet.first();
						if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
							addToMappings(minStatementMapping, mappingSet);
							leaves2.remove(minStatementMapping.getFragment2());
							leafIterator1.remove();
						}
					}
				}
			}
		}
		else {
			//exact string+depth matching - leaf nodes
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf1 : leaves1) {
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2)) && leaf1.getDepth() == leaf2.getDepth()) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves1.remove(minStatementMapping.getFragment1());
					leafIterator2.remove();
				}
			}
			
			//exact string matching - leaf nodes - finds moves to another level
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf1 : leaves1) {
					String argumentizedString1 = preprocessInput1(leaf1, leaf2);
					String argumentizedString2 = preprocessInput2(leaf1, leaf2);
					if ((leaf1.getString().equals(leaf2.getString()) || argumentizedString1.equals(argumentizedString2))) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					LeafMapping minStatementMapping = mappingSet.first();
					mappings.add(minStatementMapping);
					leaves1.remove(minStatementMapping.getFragment1());
					leafIterator2.remove();
				}
			}
			
			// exact matching with variable renames
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				TreeSet<LeafMapping> mappingSet = new TreeSet<>();
				for (AbstractCodeFragment leaf1 : leaves1) {
					ReplacementInfo replacementInfo = initializeReplacementInfo(leaf1, leaf2, leaves1, leaves2);
					Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2, parameterToArgumentMap, replacementInfo);
					if (replacements != null) {
						LeafMapping mapping = createLeafMapping(leaf1, leaf2, parameterToArgumentMap);
						mapping.addReplacements(replacements);
						for (AbstractCodeFragment leaf : leaves2) {
							if (leaf.equals(leaf2)) {
								break;
							}
							UMLClassBaseDiff classDiff = this.classDiff != null ? this.classDiff : parentMapper != null ? parentMapper.classDiff : null;
							mapping.temporaryVariableAssignment(leaf, leaves2, classDiff, parentMapper != null);
							if(mapping.isIdenticalWithExtractedVariable()) {
								break;
							}
						}
						for (AbstractCodeFragment leaf : leaves1) {
							if (leaf.equals(leaf1)) {
								break;
							}
							mapping.inlinedVariableAssignment(leaf, leaves2, parentMapper != null);
							if(mapping.isIdenticalWithInlinedVariable()) {
								break;
							}
						}
						mappingSet.add(mapping);
					}
				}
				if(!mappingSet.isEmpty()) {
					AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> switchParentEntry;
					if(variableDeclarationMappingsWithSameReplacementTypes(mappingSet)) {
						//postpone mapping
						postponedMappingSets.add(mappingSet);
					}
					else if((switchParentEntry = multipleMappingsUnderTheSameSwitch(mappingSet)) != null) {
						LeafMapping bestMapping = findBestMappingBasedOnMappedSwitchCases(switchParentEntry, mappingSet);
						addToMappings(bestMapping, mappingSet);
						leaves1.remove(bestMapping.getFragment1());
						leafIterator2.remove();
					}
					else {
						LeafMapping minStatementMapping = mappingSet.first();
						if(canBeAdded(minStatementMapping, parameterToArgumentMap)) {
							addToMappings(minStatementMapping, mappingSet);
							leaves1.remove(minStatementMapping.getFragment1());
							leafIterator2.remove();
						}
					}
				}
			}
		}
		for(TreeSet<LeafMapping> postponed : postponedMappingSets) {
			Set<LeafMapping> mappingsToBeAdded = new LinkedHashSet<>();
			for(LeafMapping variableDeclarationMapping : postponed) {
				for(AbstractCodeMapping previousMapping : this.mappings) {
					Set<Replacement> intersection = variableDeclarationMapping.commonReplacements(previousMapping);
					if(!intersection.isEmpty()) {
						for(Replacement commonReplacement : intersection) {
							if(commonReplacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
									variableDeclarationMapping.getFragment1().getVariableDeclaration(commonReplacement.getBefore()) != null &&
									variableDeclarationMapping.getFragment2().getVariableDeclaration(commonReplacement.getAfter()) != null) {
								mappingsToBeAdded.add(variableDeclarationMapping);
							}
						}
					}
				}
			}
			if(mappingsToBeAdded.size() == 1) {
				LeafMapping minStatementMapping = mappingsToBeAdded.iterator().next();
				addToMappings(minStatementMapping, postponed);
				leaves1.remove(minStatementMapping.getFragment1());
				leaves2.remove(minStatementMapping.getFragment2());
			}
			else {
				LeafMapping minStatementMapping = postponed.first();
				addToMappings(minStatementMapping, postponed);
				leaves1.remove(minStatementMapping.getFragment1());
				leaves2.remove(minStatementMapping.getFragment2());
			}
		}
	}

	private void addToMappings(LeafMapping mapping, TreeSet<LeafMapping> mappingSet) {
		mappings.add(mapping);
		refactorings.addAll(mapping.getRefactorings());
		CompositeReplacement compositeReplacement = mapping.containsCompositeReplacement();
		for(LeafMapping leafMapping : mappingSet) {
			if(!leafMapping.equals(mapping)) {
				if(compositeReplacement != null) {
					if(compositeReplacement.getAdditionallyMatchedStatements1().contains(leafMapping.getFragment1()) ||
							compositeReplacement.getAdditionallyMatchedStatements2().contains(leafMapping.getFragment2())) {
						refactorings.addAll(leafMapping.getRefactorings());
					}
				}
				if(leafMapping.isIdenticalWithExtractedVariable() || leafMapping.isIdenticalWithInlinedVariable()) {
					refactorings.addAll(leafMapping.getRefactorings());
				}
				//remove from this.mappings nested mappings (inside anonymous or lambdas) corresponding to loser mappings
				Set<AbstractCodeMapping> mappingsToBeRemoved = new LinkedHashSet<AbstractCodeMapping>();
				for(AbstractCodeMapping m : this.mappings) {
					if(leafMapping.getFragment1().getLocationInfo().subsumes(m.getFragment1().getLocationInfo()) &&
							!leafMapping.getFragment1().equals(m.getFragment1()) &&
							leafMapping.getFragment2().getLocationInfo().subsumes(m.getFragment2().getLocationInfo()) &&
							!leafMapping.getFragment2().equals(m.getFragment2())) {
						mappingsToBeRemoved.add(m);
					}
				}
				this.mappings.removeAll(mappingsToBeRemoved);
				//remove from this.refactorings nested refactorings (inside anonymous or lambdas) corresponding to loser mappings
				Set<Refactoring> refactoringsToBeRemoved = new LinkedHashSet<Refactoring>();
				for(Refactoring r : this.refactorings) {
					if(r instanceof ChangeReturnTypeRefactoring) {
						ChangeReturnTypeRefactoring changeReturnType = (ChangeReturnTypeRefactoring)r;
						Set<AbstractCodeMapping> returnReferences = changeReturnType.getReturnReferences();
						Set<AbstractCodeMapping> intersection = new LinkedHashSet<AbstractCodeMapping>();
						intersection.addAll(returnReferences);
						intersection.retainAll(mappingsToBeRemoved);
						if(!intersection.isEmpty()) {
							refactoringsToBeRemoved.add(r);
						}
					}
					else if(r instanceof RenameVariableRefactoring) {
						RenameVariableRefactoring renameVariable = (RenameVariableRefactoring)r;
						Set<AbstractCodeMapping> variableReferences = renameVariable.getVariableReferences();
						Set<AbstractCodeMapping> intersection = new LinkedHashSet<AbstractCodeMapping>();
						intersection.addAll(variableReferences);
						intersection.retainAll(mappingsToBeRemoved);
						if(!intersection.isEmpty()) {
							refactoringsToBeRemoved.add(r);
						}
					}
					else if(r instanceof ChangeVariableTypeRefactoring) {
						ChangeVariableTypeRefactoring changeVariableType = (ChangeVariableTypeRefactoring)r;
						Set<AbstractCodeMapping> variableReferences = changeVariableType.getVariableReferences();
						Set<AbstractCodeMapping> intersection = new LinkedHashSet<AbstractCodeMapping>();
						intersection.addAll(variableReferences);
						intersection.retainAll(mappingsToBeRemoved);
						if(!intersection.isEmpty()) {
							refactoringsToBeRemoved.add(r);
						}
					}
					else if(r instanceof ReplaceAnonymousWithLambdaRefactoring) {
						ReplaceAnonymousWithLambdaRefactoring lambdaRef = (ReplaceAnonymousWithLambdaRefactoring)r;
						LambdaExpressionObject lambda = lambdaRef.getLambda();
						if(lambda.getExpression() != null) {
							for(AbstractCodeMapping m : mappingsToBeRemoved) {
								if(m.getFragment2().equals(lambda.getExpression())) {
									refactoringsToBeRemoved.add(r);
									break;
								}
							}
						}
					}
				}
				this.refactorings.removeAll(refactoringsToBeRemoved);
			}
		}
	}

	private boolean canBeAdded(LeafMapping minStatementMapping, Map<String, String> parameterToArgumentMap) {
		int newMappingReplacents = validReplacements(minStatementMapping, parameterToArgumentMap);
		AbstractCodeMapping mappingToBeRemoved = null;
		boolean conflictingMappingFound = false;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getFragment1().equals(minStatementMapping.getFragment1()) ||
					mapping.getFragment2().equals(minStatementMapping.getFragment2())) {
				conflictingMappingFound = true;
				int oldMappingReplacements = validReplacements(mapping, parameterToArgumentMap);
				if(newMappingReplacents < oldMappingReplacements) {
					mappingToBeRemoved = mapping;
					break;
				}
			}
		}
		if(mappingToBeRemoved != null) {
			mappings.remove(mappingToBeRemoved);
		}
		else return !conflictingMappingFound;
		return true;
	}

	private int validReplacements(AbstractCodeMapping mapping, Map<String, String> parameterToArgumentMap) {
		int validReplacements = 0;
		for(Replacement r : mapping.getReplacements()) {
			if (!parameterToArgumentMap.containsKey(r.getAfter()) || !parameterToArgumentMap.get(r.getAfter()).equals(r.getBefore())) {
				validReplacements++;
			}
		}
		return validReplacements;
	}

	private ReplacementInfo initializeReplacementInfo(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2,
			List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2) {
		List<? extends AbstractCodeFragment> l1 = new ArrayList<AbstractCodeFragment>(leaves1);
		l1.remove(leaf1);
		List<? extends AbstractCodeFragment> l2 = new ArrayList<AbstractCodeFragment>(leaves2);
		l2.remove(leaf2);
		return new ReplacementInfo(
				preprocessInput1(leaf1, leaf2),
				preprocessInput2(leaf1, leaf2),
				l1, l2);
	}

	private boolean variableDeclarationMappingsWithSameReplacementTypes(Set<LeafMapping> mappingSet) {
		if(mappingSet.size() > 1) {
			Set<LeafMapping> variableDeclarationMappings = new LinkedHashSet<>();
			for(LeafMapping mapping : mappingSet) {
				if(mapping.getFragment1().getVariableDeclarations().size() > 0 &&
						mapping.getFragment2().getVariableDeclarations().size() > 0) {
					variableDeclarationMappings.add(mapping);
				}
			}
			if(variableDeclarationMappings.size() == mappingSet.size()) {
				Set<ReplacementType> replacementTypes = null;
				Set<LeafMapping> mappingsWithSameReplacementTypes = new LinkedHashSet<>();
				for(LeafMapping mapping : variableDeclarationMappings) {
					if(replacementTypes == null) {
						replacementTypes = mapping.getReplacementTypes();
						mappingsWithSameReplacementTypes.add(mapping);
					}
					else if(mapping.getReplacementTypes().equals(replacementTypes) &&
							!(replacementTypes.size() == 1 && replacementTypes.contains(ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA))) {
						mappingsWithSameReplacementTypes.add(mapping);
					}
					else if(mapping.getReplacementTypes().containsAll(replacementTypes) || replacementTypes.containsAll(mapping.getReplacementTypes())) {
						AbstractCall invocation1 = mapping.getFragment1().invocationCoveringEntireFragment();
						AbstractCall invocation2 = mapping.getFragment2().invocationCoveringEntireFragment();
						if(invocation1 != null && invocation2 != null) {
							for(Replacement replacement : mapping.getReplacements()) {
								if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
									if(invocation1.getName().equals(replacement.getBefore()) && invocation2.getName().equals(replacement.getAfter())) {
										mappingsWithSameReplacementTypes.add(mapping);
										break;
									}
								}
							}
						}
					}
				}
				return mappingsWithSameReplacementTypes.size() == mappingSet.size();
			}
		}
		return false;
	}

	private LeafMapping findBestMappingBasedOnMappedSwitchCases(AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> switchParentEntry, TreeSet<LeafMapping> mappingSet) {
		CompositeStatementObject switchParent1 = switchParentEntry.getKey();
		CompositeStatementObject switchParent2 = switchParentEntry.getValue();
		AbstractCodeMapping currentSwitchCase = null;
		for(AbstractCodeMapping mapping : this.mappings) {
			AbstractCodeFragment fragment1 = mapping.getFragment1();
			AbstractCodeFragment fragment2 = mapping.getFragment2();
			if(fragment1 instanceof AbstractStatement && fragment2 instanceof AbstractStatement) {
				AbstractStatement statement1 = (AbstractStatement)fragment1;
				AbstractStatement statement2 = (AbstractStatement)fragment2;
				CompositeStatementObject parent1 = statement1.getParent();
				CompositeStatementObject parent2 = statement2.getParent();
				if(parent1 == switchParent1 && parent2 == switchParent2 && mapping.isExact() &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_CASE)) {
					currentSwitchCase = mapping;
				}
				else if(parent1 == switchParent1 && parent2 == switchParent2 &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.BREAK_STATEMENT) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.BREAK_STATEMENT)) {
					if(currentSwitchCase != null) {
						for(LeafMapping leafMapping : mappingSet) {
							if(leafMapping.getFragment1().getIndex() > currentSwitchCase.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() > currentSwitchCase.getFragment2().getIndex() &&
									leafMapping.getFragment1().getIndex() < mapping.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() < mapping.getFragment2().getIndex()) {
								return leafMapping;
							}
						}
					}
				}
				else if(parent1 == switchParent1 && parent2 == switchParent2 &&
						statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT) &&
						statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.RETURN_STATEMENT)) {
					if(currentSwitchCase != null) {
						for(LeafMapping leafMapping : mappingSet) {
							if(leafMapping.getFragment1().getIndex() > currentSwitchCase.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() > currentSwitchCase.getFragment2().getIndex() &&
									leafMapping.getFragment1().getIndex() < mapping.getFragment1().getIndex() &&
									leafMapping.getFragment2().getIndex() < mapping.getFragment2().getIndex()) {
								return leafMapping;
							}
						}
					}
				}
			}
		}
		return mappingSet.first();
	}

	private AbstractMap.SimpleEntry<CompositeStatementObject, CompositeStatementObject> multipleMappingsUnderTheSameSwitch(Set<LeafMapping> mappingSet) {
		CompositeStatementObject switchParent1 = null;
		CompositeStatementObject switchParent2 = null;
		if(mappingSet.size() > 1) {
			for(LeafMapping mapping : mappingSet) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment1 instanceof AbstractStatement && fragment2 instanceof AbstractStatement) {
					AbstractStatement statement1 = (AbstractStatement)fragment1;
					AbstractStatement statement2 = (AbstractStatement)fragment2;
					CompositeStatementObject parent1 = statement1.getParent();
					CompositeStatementObject parent2 = statement2.getParent();
					if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT) &&
							parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.SWITCH_STATEMENT)) {
						if(switchParent1 == null && switchParent2 == null) {
							switchParent1 = parent1;
							switchParent2 = parent2;
						}
						else if(switchParent1 != parent1 || switchParent2 != parent2) {
							return null;
						}
					}
					else {
						return null;
					}
				}
			}
		}
		if(switchParent1 != null && switchParent2 != null) {
			return new AbstractMap.SimpleEntry<>(switchParent1, switchParent2);
		}
		return null;
	}

	private LeafMapping createLeafMapping(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2, Map<String, String> parameterToArgumentMap) {
		UMLOperation operation1 = codeFragmentOperationMap1.containsKey(leaf1) ? codeFragmentOperationMap1.get(leaf1) : this.operation1;
		UMLOperation operation2 = codeFragmentOperationMap2.containsKey(leaf2) ? codeFragmentOperationMap2.get(leaf2) : this.operation2;
		LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
		for(String key : parameterToArgumentMap.keySet()) {
			String value = parameterToArgumentMap.get(key);
			if(!key.equals(value) && ReplacementUtil.contains(leaf2.getString(), key) && ReplacementUtil.contains(leaf1.getString(), value)) {
				mapping.addReplacement(new Replacement(value, key, ReplacementType.VARIABLE_NAME));
			}
		}
		return mapping;
	}

	private String preprocessInput1(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf1, leaf2);
	}

	private String preprocessInput2(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		return preprocessInput(leaf2, leaf1);
	}

	private String preprocessInput(AbstractCodeFragment leaf1, AbstractCodeFragment leaf2) {
		String argumentizedString = leaf1.getArgumentizedString();
		if (leaf1 instanceof StatementObject && leaf2 instanceof AbstractExpression) {
			if (argumentizedString.startsWith("return ") && argumentizedString.endsWith(";\n")) {
				argumentizedString = argumentizedString.substring("return ".length(),
						argumentizedString.lastIndexOf(";\n"));
			}
		}
		return argumentizedString;
	}

	private static class ReplacementInfo {
		private String argumentizedString1;
		private final String argumentizedString2;
		private int rawDistance;
		private final Set<Replacement> replacements;
		private final List<? extends AbstractCodeFragment> statements1;
		private final List<? extends AbstractCodeFragment> statements2;
		
		public ReplacementInfo(String argumentizedString1, String argumentizedString2,
				List<? extends AbstractCodeFragment> statements1, List<? extends AbstractCodeFragment> statements2) {
			this.argumentizedString1 = argumentizedString1;
			this.argumentizedString2 = argumentizedString2;
			this.statements1 = statements1;
			this.statements2 = statements2;
			this.rawDistance = StringDistance.editDistance(argumentizedString1, argumentizedString2);
			this.replacements = new LinkedHashSet<>();
		}
		public String getArgumentizedString1() {
			return argumentizedString1;
		}
		public String getArgumentizedString2() {
			return argumentizedString2;
		}
		public void setArgumentizedString1(String string) {
			this.argumentizedString1 = string;
			this.rawDistance = StringDistance.editDistance(this.argumentizedString1, this.argumentizedString2);
		}
		public int getRawDistance() {
			return rawDistance;
		}
		public void addReplacement(Replacement r) {
			this.replacements.add(r);
		}
		public void addReplacements(Set<Replacement> replacementsToBeAdded) {
			this.replacements.addAll(replacementsToBeAdded);
		}
		public void removeReplacements(Set<Replacement> replacementsToBeRemoved) {
			this.replacements.removeAll(replacementsToBeRemoved);
		}
		public Set<Replacement> getReplacements() {
			return replacements;
		}
		public List<Replacement> getReplacements(ReplacementType type) {
			List<Replacement> replacements = new ArrayList<>();
			for(Replacement replacement : this.replacements) {
				if(replacement.getType().equals(type)) {
					replacements.add(replacement);
				}
			}
			return replacements;
		}
	}

	private boolean nonMatchedStatementUsesVariableInArgument(List<? extends AbstractCodeFragment> statements, String variable, String otherArgument) {
		for(AbstractCodeFragment statement : statements) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null) {
				for(String argument : invocation.getArguments()) {
					String argumentNoWhiteSpace = argument.replaceAll("\\s","");
					if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
							!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) && !argument.equals(otherArgument)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo) throws RefactoringMinerTimedOutException {
		List<VariableDeclaration> variableDeclarations1 = new ArrayList<>(statement1.getVariableDeclarations());
		List<VariableDeclaration> variableDeclarations2 = new ArrayList<>(statement2.getVariableDeclarations());
		VariableDeclaration variableDeclarationWithArrayInitializer1 = declarationWithArrayInitializer(variableDeclarations1);
		VariableDeclaration variableDeclarationWithArrayInitializer2 = declarationWithArrayInitializer(variableDeclarations2);
		AbstractCall invocationCoveringTheEntireStatement1 = statement1.invocationCoveringEntireFragment();
		AbstractCall invocationCoveringTheEntireStatement2 = statement2.invocationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		Map<String, List<? extends AbstractCall>> methodInvocationMap1 = new LinkedHashMap<>(statement1.getMethodInvocationMap());
		Map<String, List<? extends AbstractCall>> methodInvocationMap2 = new LinkedHashMap<>(statement2.getMethodInvocationMap());
		Set<String> variables1 = new LinkedHashSet<>(statement1.getVariables());
		Set<String> variables2 = new LinkedHashSet<>(statement2.getVariables());
		Set<String> variableIntersection = new LinkedHashSet<>(variables1);
		variableIntersection.retainAll(variables2);
		// ignore the variables in the intersection that also appear with "this." prefix in the sets of variables
		// ignore the variables in the intersection that are static fields
		// ignore the variables in the intersection that one of them is a variable declaration and the other is not
		// ignore the variables in the intersection that one of them is part of a method invocation, but the same method invocation does not appear on the other side
		Set<String> variablesToBeRemovedFromTheIntersection = new LinkedHashSet<>();
		for(String variable : variableIntersection) {
			if(!variable.startsWith("this.") && !variableIntersection.contains("this."+variable) &&
					(variables1.contains("this."+variable) || variables2.contains("this."+variable))) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
						invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement1.getArguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.statements1, variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
				else if(invocationCoveringTheEntireStatement1.getArguments().contains(variable) &&
						!invocationCoveringTheEntireStatement2.getArguments().contains(variable)) {
					for(String argument : invocationCoveringTheEntireStatement2.getArguments()) {
						String argumentNoWhiteSpace = argument.replaceAll("\\s","");
						if(argument.contains(variable) && !argument.equals(variable) && !argumentNoWhiteSpace.contains("+" + variable + "+") &&
								!argumentNoWhiteSpace.contains(variable + "+") && !argumentNoWhiteSpace.contains("+" + variable) &&
								!nonMatchedStatementUsesVariableInArgument(replacementInfo.statements2, variable, argument)) {
							variablesToBeRemovedFromTheIntersection.add(variable);
						}
					}
				}
			}
			if(variable.toUpperCase().equals(variable) && !ReplacementUtil.sameCharsBeforeAfter(statement1.getString(), statement2.getString(), variable)) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			boolean foundInDeclaration1 = false;
			for(VariableDeclaration declaration : variableDeclarations1) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration1 = true;
					break;
				}
			}
			boolean foundInDeclaration2 = false;
			for(VariableDeclaration declaration : variableDeclarations2) {
				if(declaration.getVariableName().equals(variable)) {
					foundInDeclaration2 = true;
					break;
				}
			}
			if(foundInDeclaration1 != foundInDeclaration2) {
				variablesToBeRemovedFromTheIntersection.add(variable);
			}
			else if(!variable.contains(".")) {
				boolean foundInInvocation1 = false;
				for(String key : methodInvocationMap1.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation1 = true;
						break;
					}
				}
				boolean foundInInvocation2 = false;
				for(String key : methodInvocationMap2.keySet()) {
					if(key.startsWith(variable + ".")) {
						foundInInvocation2 = true;
						break;
					}
				}
				boolean sameCoverageInvocations = invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
						invocationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage());
				boolean sameCoverageCreations = creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
						creationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage());
				if((sameCoverageInvocations || sameCoverageCreations) && foundInInvocation1 != foundInInvocation2) {
					variablesToBeRemovedFromTheIntersection.add(variable);
				}
			}
		}
		variableIntersection.removeAll(variablesToBeRemovedFromTheIntersection);
		// remove common variables from the two sets
		variables1.removeAll(variableIntersection);
		variables2.removeAll(variableIntersection);
		
		// replace variables with the corresponding arguments
		replaceVariablesWithArguments(variables1, parameterToArgumentMap);
		replaceVariablesWithArguments(variables2, parameterToArgumentMap);
		
		Set<String> methodInvocations1 = new LinkedHashSet<>(methodInvocationMap1.keySet());
		Set<String> methodInvocations2 = new LinkedHashSet<>(methodInvocationMap2.keySet());
		
		Map<String, List<? extends AbstractCall>> creationMap1 = new LinkedHashMap<>(statement1.getCreationMap());
		Map<String, List<? extends AbstractCall>> creationMap2 = new LinkedHashMap<>(statement2.getCreationMap());
		Set<String> creations1 = new LinkedHashSet<>(creationMap1.keySet());
		Set<String> creations2 = new LinkedHashSet<>(creationMap2.keySet());
		
		Set<String> arguments1 = new LinkedHashSet<>(statement1.getArguments());
		Set<String> arguments2 = new LinkedHashSet<>(statement2.getArguments());
		removeCommonElements(arguments1, arguments2);
		
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, variables1, variables2)) {
			findReplacements(arguments1, variables2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_VARIABLE);
		}
		
		Map<String, String> map = new LinkedHashMap<>();
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<>();
		for(Replacement r : replacementInfo.getReplacements()) {
			map.put(r.getBefore(), r.getAfter());
			if(methodInvocationMap1.containsKey(r.getBefore())) {
				Replacement replacement = new VariableReplacementWithMethodInvocation(r.getBefore(), r.getAfter(), methodInvocationMap1.get(r.getBefore()).get(0), Direction.INVOCATION_TO_VARIABLE);
				replacementsToBeAdded.add(replacement);
				replacementsToBeRemoved.add(r);
			}
		}
		replacementInfo.getReplacements().removeAll(replacementsToBeRemoved);
		replacementInfo.getReplacements().addAll(replacementsToBeAdded);
		
		// replace variables with the corresponding arguments in method invocations
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, parameterToArgumentMap);
		replaceVariablesWithArguments(methodInvocationMap2, methodInvocations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(methodInvocationMap1, methodInvocations1, map);
		
		//remove methodInvocation covering the entire statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String methodInvocation1 : methodInvocationMap1.keySet()) {
				for(AbstractCall call : methodInvocationMap1.get(methodInvocation1)) {
					if(invocationCoveringTheEntireStatement1.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations1.remove(methodInvocation1);
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation2 : methodInvocationMap2.keySet()) {
				for(AbstractCall call : methodInvocationMap2.get(methodInvocation2)) {
					if(invocationCoveringTheEntireStatement2.getLocationInfo().equals(call.getLocationInfo())) {
						methodInvocations2.remove(methodInvocation2);
					}
				}
			}
		}
		Set<String> methodInvocationIntersection = new LinkedHashSet<>(methodInvocations1);
		methodInvocationIntersection.retainAll(methodInvocations2);
		Set<String> methodInvocationsToBeRemovedFromTheIntersection = new LinkedHashSet<>();
		for(String methodInvocation : methodInvocationIntersection) {
			if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
					invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				if(!invocationCoveringTheEntireStatement1.getArguments().contains(methodInvocation) &&
						invocationCoveringTheEntireStatement2.getArguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
				else if(invocationCoveringTheEntireStatement1.getArguments().contains(methodInvocation) &&
						!invocationCoveringTheEntireStatement2.getArguments().contains(methodInvocation)) {
					methodInvocationsToBeRemovedFromTheIntersection.add(methodInvocation);
				}
			}
		}
		methodInvocationIntersection.removeAll(methodInvocationsToBeRemovedFromTheIntersection);
		// remove common methodInvocations from the two sets
		methodInvocations1.removeAll(methodInvocationIntersection);
		methodInvocations2.removeAll(methodInvocationIntersection);
		
		Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<>();
		//variablesAndMethodInvocations1.addAll(methodInvocations1);
		//variablesAndMethodInvocations1.addAll(variables1);
		
		Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<>();
		variablesAndMethodInvocations2.addAll(methodInvocations2);
		variablesAndMethodInvocations2.addAll(variables2);
		
		Set<String> types1 = new LinkedHashSet<>(statement1.getTypes());
		Set<String> types2 = new LinkedHashSet<>(statement2.getTypes());
		removeCommonTypes(types1, types2, statement1.getTypes(), statement2.getTypes());
		
		// replace variables with the corresponding arguments in object creations
		replaceVariablesWithArguments(creationMap1, creations1, parameterToArgumentMap);
		replaceVariablesWithArguments(creationMap2, creations2, parameterToArgumentMap);
		
		replaceVariablesWithArguments(creationMap1, creations1, map);
		
		//remove objectCreation covering the entire statement
		for(String objectCreation1 : creationMap1.keySet()) {
			for(AbstractCall creation1 : creationMap1.get(objectCreation1)) {
				if(creationCoveringTheEntireStatement1 != null && 
						creationCoveringTheEntireStatement1.getLocationInfo().equals(creation1.getLocationInfo())) {
					creations1.remove(objectCreation1);
				}
				if(((ObjectCreation)creation1).getAnonymousClassDeclaration() != null) {
					creations1.remove(objectCreation1);
				}
			}
		}
		for(String objectCreation2 : creationMap2.keySet()) {
			for(AbstractCall creation2 : creationMap2.get(objectCreation2)) {
				if(creationCoveringTheEntireStatement2 != null &&
						creationCoveringTheEntireStatement2.getLocationInfo().equals(creation2.getLocationInfo())) {
					creations2.remove(objectCreation2);
				}
				if(((ObjectCreation)creation2).getAnonymousClassDeclaration() != null) {
					creations2.remove(objectCreation2);
				}
			}
		}
		Set<String> creationIntersection = new LinkedHashSet<>(creations1);
		creationIntersection.retainAll(creations2);
		// remove common creations from the two sets
		creations1.removeAll(creationIntersection);
		creations2.removeAll(creationIntersection);
		
		Set<String> stringLiterals1 = new LinkedHashSet<>(statement1.getStringLiterals());
		Set<String> stringLiterals2 = new LinkedHashSet<>(statement2.getStringLiterals());
		removeCommonElements(stringLiterals1, stringLiterals2);
		
		Set<String> numberLiterals1 = new LinkedHashSet<>(statement1.getNumberLiterals());
		Set<String> numberLiterals2 = new LinkedHashSet<>(statement2.getNumberLiterals());
		removeCommonElements(numberLiterals1, numberLiterals2);
		
		Set<String> booleanLiterals1 = new LinkedHashSet<>(statement1.getBooleanLiterals());
		Set<String> booleanLiterals2 = new LinkedHashSet<>(statement2.getBooleanLiterals());
		removeCommonElements(booleanLiterals1, booleanLiterals2);
		
		Set<String> infixOperators1 = new LinkedHashSet<>(statement1.getInfixOperators());
		Set<String> infixOperators2 = new LinkedHashSet<>(statement2.getInfixOperators());
		removeCommonElements(infixOperators1, infixOperators2);
		
		Set<String> arrayAccesses1 = new LinkedHashSet<>(statement1.getArrayAccesses());
		Set<String> arrayAccesses2 = new LinkedHashSet<>(statement2.getArrayAccesses());
		removeCommonElements(arrayAccesses1, arrayAccesses2);
		
		Set<String> prefixExpressions1 = new LinkedHashSet<>(statement1.getPrefixExpressions());
		Set<String> prefixExpressions2 = new LinkedHashSet<>(statement2.getPrefixExpressions());
		removeCommonElements(prefixExpressions1, prefixExpressions2);
		
		//perform type replacements
		findReplacements(types1, types2, replacementInfo, ReplacementType.TYPE);
		
		//perform operator replacements
		findReplacements(infixOperators1, infixOperators2, replacementInfo, ReplacementType.INFIX_OPERATOR);
		
		//apply existing replacements on method invocations
		for(String methodInvocation1 : methodInvocations1) {
			String temp = methodInvocation1;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				temp = ReplacementUtil.performReplacement(temp, replacement.getBefore(), replacement.getAfter());
			}
			if(!temp.equals(methodInvocation1)) {
				variablesAndMethodInvocations1.add(temp);
				methodInvocationMap1.put(temp, methodInvocationMap1.get(methodInvocation1));
			}
		}
		//add updated method invocation to the original list of invocations
		methodInvocations1.addAll(variablesAndMethodInvocations1);
		variablesAndMethodInvocations1.addAll(methodInvocations1);
		variablesAndMethodInvocations1.addAll(variables1);
		
		if (replacementInfo.getRawDistance() > 0) {
			for(String s1 : variablesAndMethodInvocations1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
				int minDistance = replacementInfo.getRawDistance();
				for(String s2 : variablesAndMethodInvocations2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2(), minDistance);
					boolean multipleInstances = ReplacementUtil.countInstances(temp, s2) > 1;
					if(distanceRaw == -1 && multipleInstances) {
						distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					}
					boolean multipleInstanceRule = multipleInstances && Math.abs(s1.length() - s2.length()) == Math.abs(distanceRaw - minDistance) && !s1.equals(s2);
					if(distanceRaw >= 0 && (distanceRaw < replacementInfo.getRawDistance() || multipleInstanceRule)) {
						minDistance = distanceRaw;
						Replacement replacement = null;
						if(variables1.contains(s1) && variables2.contains(s2) && variablesStartWithSameCase(s1, s2, parameterToArgumentMap, replacementInfo)) {
							replacement = new Replacement(s1, s2, ReplacementType.VARIABLE_NAME);
							if(s1.startsWith("(") && s2.startsWith("(") && s1.contains(")") && s2.contains(")")) {
								String prefix1 = s1.substring(0, s1.indexOf(")")+1);
								String prefix2 = s2.substring(0, s2.indexOf(")")+1);
								if(prefix1.equals(prefix2)) {
									String suffix1 = s1.substring(prefix1.length());
									String suffix2 = s2.substring(prefix2.length());
									replacement = new Replacement(suffix1, suffix2, ReplacementType.VARIABLE_NAME);
								}
							}
							VariableDeclaration v1 = statement1.searchVariableDeclaration(s1);
							if(v1 == null && operation1 != null) {
								for(VariableDeclaration declaration : operation1.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s1)) {
										v1 = declaration;
										break;
									}
								}
							}
							VariableDeclaration v2 = statement2.searchVariableDeclaration(s2);
							if(v2 == null && operation2 != null) {
								for(VariableDeclaration declaration : operation2.getParameterDeclarationList()) {
									if(declaration.getVariableName().equals(s2)) {
										v2 = declaration;
										break;
									}
								}
							}
							if(inconsistentVariableMappingCount(statement1, statement2, v1, v2) > 1 && !existsVariableDeclarationForV2InitializedWithV1(v1, v2, replacementInfo) && operation2 != null && operation2.loopWithVariables(v1.getVariableName(), v2.getVariableName()) == null) {
								replacement = null;
							}
						}
						else if(variables1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
						}
						else if(methodInvocations1.contains(s1) && methodInvocations2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							AbstractCall invokedOperationAfter = methodInvocationMap2.get(s2).get(0);
							if(invokedOperationBefore.compatibleExpression(invokedOperationAfter)) {
								replacement = new MethodInvocationReplacement(s1, s2, invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION);
							}
						}
						else if(methodInvocations1.contains(s1) && variables2.contains(s2)) {
							AbstractCall invokedOperationBefore = methodInvocationMap1.get(s1).get(0);
							replacement = new VariableReplacementWithMethodInvocation(s1, s2, invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
						}
						if(replacement != null) {
							double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
							replacementMap.put(distancenormalized, replacement);
						}
						if(distanceRaw == 0 && !replacementInfo.getReplacements().isEmpty()) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							invocationCoveringTheEntireStatement1.methodNameContainsArgumentName() &&
							invocationCoveringTheEntireStatement2.methodNameContainsArgumentName() &&
							replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
						for(Replacement r : replacementMap.values()) {
							if(!replacement.equals(r) && r.getType().equals(ReplacementType.VARIABLE_NAME) &&
									invocationCoveringTheEntireStatement1.getName().toLowerCase().endsWith(r.getBefore().toLowerCase()) &&
									invocationCoveringTheEntireStatement2.getName().toLowerCase().endsWith(r.getAfter().toLowerCase())) {
								replacement = r;
								break;
							}
						}
					}
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		
		//perform creation replacements
		findReplacements(creations1, creations2, replacementInfo, ReplacementType.CLASS_INSTANCE_CREATION);
		
		//perform literal replacements
		findReplacements(stringLiterals1, stringLiterals2, replacementInfo, ReplacementType.STRING_LITERAL);
		findReplacements(numberLiterals1, numberLiterals2, replacementInfo, ReplacementType.NUMBER_LITERAL);
		if(!statement1.containsInitializerOfVariableDeclaration(numberLiterals1) && !statement2.containsInitializerOfVariableDeclaration(variables2) &&
				!statement1.getString().endsWith("=0;\n")) {
			findReplacements(numberLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NUMBER_LITERAL);
		}
		findReplacements(variables1, arrayAccesses2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);
		findReplacements(arrayAccesses1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS);
		
		findReplacements(methodInvocations1, arrayAccesses2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);
		findReplacements(arrayAccesses1, methodInvocations2, replacementInfo, ReplacementType.ARRAY_ACCESS_REPLACED_WITH_METHOD_INVOCATION);
		
		findReplacements(variables1, prefixExpressions2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
		findReplacements(prefixExpressions1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_PREFIX_EXPRESSION);
		findReplacements(stringLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_STRING_LITERAL);
		if(statement1.getNullLiterals().isEmpty() && !statement2.getNullLiterals().isEmpty()) {
			Set<String> nullLiterals2 = new LinkedHashSet<>();
			nullLiterals2.add("null");
			findReplacements(variables1, nullLiterals2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL);
			if(invocationCoveringTheEntireStatement1 != null) {
				String expression = invocationCoveringTheEntireStatement1.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement1.getName().equals("empty") &&
						invocationCoveringTheEntireStatement1.getArguments().size() == 0) {
					Set<String> invocations1 = new LinkedHashSet<>();
					invocations1.add(invocationCoveringTheEntireStatement1.actualString());
					findReplacements(invocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
				}
			}
			if(methodInvocations1.contains("Optional.empty()")) {
				findReplacements(methodInvocations1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
			}
		}
		else if(!statement1.getNullLiterals().isEmpty() && statement2.getNullLiterals().isEmpty()) {
			Set<String> nullLiterals1 = new LinkedHashSet<>();
			nullLiterals1.add("null");
			findReplacements(nullLiterals1, variables2, replacementInfo, ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL);
			if(invocationCoveringTheEntireStatement2 != null) {
				String expression = invocationCoveringTheEntireStatement2.getExpression();
				if(expression != null && expression.equals("Optional") && invocationCoveringTheEntireStatement2.getName().equals("empty") &&
						invocationCoveringTheEntireStatement2.getArguments().size() == 0) {
					Set<String> invocations2 = new LinkedHashSet<>();
					invocations2.add(invocationCoveringTheEntireStatement2.actualString());
					findReplacements(nullLiterals1, invocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
				}
			}
			if(methodInvocations2.contains("Optional.empty()")) {
				findReplacements(nullLiterals1, methodInvocations2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_OPTIONAL_EMPTY);
			}
		}

		if(statement1.getTernaryOperatorExpressions().isEmpty() && !statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement1.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals1 = new LinkedHashSet<>();
				nullLiterals1.add("null");
				Set<String> ternaryExpressions2 = new LinkedHashSet<>();
				for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
					ternaryExpressions2.add(ternary.getExpression());	
				}
				findReplacements(nullLiterals1, ternaryExpressions2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
		}
		else if(!statement1.getTernaryOperatorExpressions().isEmpty() && statement2.getTernaryOperatorExpressions().isEmpty()) {
			if(!statement2.getNullLiterals().isEmpty()) {
				Set<String> nullLiterals2 = new LinkedHashSet<>();
				nullLiterals2.add("null");
				Set<String> ternaryExpressions1 = new LinkedHashSet<>();
				for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
					ternaryExpressions1.add(ternary.getExpression());	
				}
				findReplacements(ternaryExpressions1, nullLiterals2, replacementInfo, ReplacementType.NULL_LITERAL_REPLACED_WITH_CONDITIONAL_EXPRESSION);
			}
		}
		if(!statement1.getString().endsWith("=true;\n") && !statement1.getString().endsWith("=false;\n")) {
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if(!statement2.getString().endsWith("=true;\n") && !statement2.getString().endsWith("=false;\n")) {
			findReplacements(arguments1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_ARGUMENT);
		}
		if((statement2.getString().endsWith("true;\n") && statement1.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement2.getString().endsWith("false;\n") && statement1.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(variables1, booleanLiterals2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if((statement1.getString().endsWith("true;\n") && statement2.getString().endsWith("Boolean.TRUE;\n")) ||
				(statement1.getString().endsWith("false;\n") && statement2.getString().endsWith("Boolean.FALSE;\n"))) {
			findReplacements(booleanLiterals1, variables2, replacementInfo, ReplacementType.BOOLEAN_REPLACED_WITH_VARIABLE);
		}
		if(!argumentsWithIdenticalMethodCalls(arguments1, arguments2, methodInvocations1, methodInvocations2) && !replacementInfo.getReplacements().isEmpty()) {
			findReplacements(arguments1, methodInvocations2, replacementInfo, ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION);
		}
		
		String s1 = preprocessInput1(statement1, statement2);
		String s2 = preprocessInput2(statement1, statement2);
		replacementsToBeRemoved = new LinkedHashSet<>();
		replacementsToBeAdded = new LinkedHashSet<>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			s1 = ReplacementUtil.performReplacement(s1, s2, replacement.getBefore(), replacement.getAfter());
			//find variable replacements within method invocation replacements
			Set<Replacement> set = replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, methodInvocations2, methodInvocationMap2, Direction.VARIABLE_TO_INVOCATION);
			set.addAll(replacementsWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), methodInvocations1, variables2, methodInvocationMap1, Direction.INVOCATION_TO_VARIABLE));
			if(!set.isEmpty()) {
				replacementsToBeRemoved.add(replacement);
				replacementsToBeAdded.addAll(set);
			}
			Set<Replacement> r = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), variables1, variables2);
			if(!r.isEmpty()) {
				replacementsToBeRemoved.add(replacement);
				replacementsToBeAdded.addAll(r);
			}
			Set<Replacement> r2 = variableReplacementWithinMethodInvocations(replacement.getBefore(), replacement.getAfter(), stringLiterals1, variables2);
			if(!r2.isEmpty()) {
				replacementsToBeRemoved.add(replacement);
				replacementsToBeAdded.addAll(r2);
			}
		}
		replacementInfo.removeReplacements(replacementsToBeRemoved);
		replacementInfo.addReplacements(replacementsToBeAdded);
		boolean isEqualWithReplacement = s1.equals(s2) || (s1 + ";\n").equals(s2) || (s2 + ";\n").equals(s1) || replacementInfo.argumentizedString1.equals(replacementInfo.argumentizedString2) || differOnlyInCastExpressionOrPrefixOperator(s1, s2, methodInvocationMap1, methodInvocationMap2, replacementInfo) ||
				differOnlyInFinalModifier(s1, s2) ||
				oneIsVariableDeclarationTheOtherIsVariableAssignment(s1, s2, replacementInfo) || identicalVariableDeclarationsWithDifferentNames(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo) ||
				oneIsVariableDeclarationTheOtherIsReturnStatement(s1, s2) || oneIsVariableDeclarationTheOtherIsReturnStatement(statement1.getString(), statement2.getString()) ||
				(containsValidOperatorReplacements(replacementInfo) && (equalAfterInfixExpressionExpansion(s1, s2, replacementInfo, statement1.getInfixExpressions()) || commonConditional(s1, s2, replacementInfo))) ||
				equalAfterArgumentMerge(s1, s2, replacementInfo) ||
				equalAfterNewArgumentAdditions(s1, s2, replacementInfo) ||
				(validStatementForConcatComparison(statement1, statement2) && commonConcat(s1, s2, replacementInfo));
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = statement1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = statement2.getAnonymousClassDeclarations();
		List<LambdaExpressionObject> lambdas1 = statement1.getLambdas();
		List<LambdaExpressionObject> lambdas2 = statement2.getLambdas();
		List<UMLOperationBodyMapper> lambdaMappers = new ArrayList<UMLOperationBodyMapper>();
		if(isEqualWithReplacement) {
			List<Replacement> typeReplacements = replacementInfo.getReplacements(ReplacementType.TYPE);
			if(typeReplacements.size() > 0 && invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
				for(Replacement typeReplacement : typeReplacements) {
					if(invocationCoveringTheEntireStatement1.getName().contains(typeReplacement.getBefore()) && invocationCoveringTheEntireStatement2.getName().contains(typeReplacement.getAfter())) {
						if(invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2)) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
									invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
							replacementInfo.addReplacement(replacement);
						}
						else {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
						}
						break;
					}
				}
			}
			if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
					!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
					!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
				return null;
			}
			if(variableAssignmentWithEverythingReplaced(statement1, statement2, replacementInfo)) {
				return null;
			}
			if(classInstanceCreationWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(operatorExpressionWithEverythingReplaced(statement1, statement2, replacementInfo, parameterToArgumentMap)) {
				return null;
			}
			if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty()) {
				Set<Replacement> replacementsInsideAnonymous = new LinkedHashSet<>();
				for(Replacement replacement : replacementInfo.getReplacements()) {
					if(replacement instanceof MethodInvocationReplacement) {
						for (AnonymousClassDeclarationObject classDeclarationObject : anonymousClassDeclarations1) {
							for (AnonymousClassDeclarationObject anonymousClassDeclarationObject : anonymousClassDeclarations2) {
								AnonymousClassDeclarationObject anonymousClassDeclaration1 = classDeclarationObject;
								AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarationObject;
								if (anonymousClassDeclaration1.getMethodInvocationMap().containsKey(replacement.getBefore()) &&
										anonymousClassDeclaration2.getMethodInvocationMap().containsKey(replacement.getAfter())) {
									replacementsInsideAnonymous.add(replacement);
									break;
								}
							}
							if(replacementsInsideAnonymous.contains(replacement)) {
								break;
							}
						}
					}
				}
				for(Replacement replacement : replacementsInsideAnonymous) {
					equalAfterNewArgumentAdditions(replacement.getBefore(), replacement.getAfter(), replacementInfo);
				}
			}
			processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
					invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, methodInvocationMap1, methodInvocationMap2,
					anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, lambdaMappers);
			return replacementInfo.getReplacements();
		}
		Set<Replacement> replacements = processAnonymousAndLambdas(statement1, statement2, parameterToArgumentMap, replacementInfo,
				invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, methodInvocationMap1, methodInvocationMap2,
				anonymousClassDeclarations1, anonymousClassDeclarations2, lambdas1, lambdas2, lambdaMappers);
		if(replacements != null) {
			return replacements;
		}
		//match traditional for with enhanced for
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions2 = for2.getExpressions();
			AbstractExpression enhancedForExpression = expressions2.get(expressions2.size()-1);
			for(AbstractExpression expression1 : for1.getExpressions()) {
				if(expression1.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression1.getString().contains(enhancedForExpression.getString() + ".iterator()")) {
					return replacementInfo.getReplacements();
				}
			}
		}
		if(statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) &&
				statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
			CompositeStatementObject for1 = (CompositeStatementObject)statement1;
			CompositeStatementObject for2 = (CompositeStatementObject)statement2;
			List<AbstractExpression> expressions1 = for1.getExpressions();
			AbstractExpression enhancedForExpression = expressions1.get(expressions1.size()-1);
			for(AbstractExpression expression2 : for2.getExpressions()) {
				if(expression2.getString().contains(enhancedForExpression.getString() + ".length") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".size()") ||
						expression2.getString().contains(enhancedForExpression.getString() + ".iterator()")) {
					return replacementInfo.getReplacements();
				}
			}
		}
		//match try-with-resources with regular try
		if(statement1 instanceof TryStatementObject && statement2 instanceof TryStatementObject) {
			TryStatementObject try1 = (TryStatementObject)statement1;
			TryStatementObject try2 = (TryStatementObject)statement2;
			if(!try1.isTryWithResources() && try2.isTryWithResources()) {
				List<AbstractStatement> tryStatements1 = try1.getStatements();
				List<AbstractStatement> tryStatements2 = try2.getStatements();
				List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
				List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
				for(AbstractCodeMapping mapping : mappings) {
					if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
						matchedChildStatements1.add(mapping.getFragment1());
						matchedChildStatements2.add(mapping.getFragment2());
					}
				}
				if(matchedChildStatements1.size() > 0 && matchedChildStatements2.size() > 0) {
					List<AbstractStatement> unmatchedStatementsTry1 = new ArrayList<>();
					for(AbstractStatement tryStatement1 : tryStatements1) {
						if(!matchedChildStatements1.contains(tryStatement1)) {
							unmatchedStatementsTry1.add(tryStatement1);
						}
					}
					List<AbstractExpression> unmatchedExpressionsTry2 = new ArrayList<>();
					unmatchedExpressionsTry2.addAll(try2.getExpressions());
					processLeaves(unmatchedStatementsTry1, unmatchedExpressionsTry2, parameterToArgumentMap);
					Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
					additionallyMatchedStatements1.addAll(try1.getCatchClauses());
					Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
					replacementInfo.addReplacement(composite);
					return replacementInfo.getReplacements();
				}
			}
			else if(try1.isTryWithResources() && try2.isTryWithResources()) {
				if((creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) ||
						(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null)) {
					List<AbstractStatement> tryStatements1 = try1.getStatements();
					List<AbstractStatement> tryStatements2 = try2.getStatements();
					List<AbstractCodeFragment> matchedChildStatements1 = new ArrayList<>();
					List<AbstractCodeFragment> matchedChildStatements2 = new ArrayList<>();
					for(AbstractCodeMapping mapping : mappings) {
						if(tryStatements1.contains(mapping.getFragment1()) && tryStatements2.contains(mapping.getFragment2())) {
							matchedChildStatements1.add(mapping.getFragment1());
							matchedChildStatements2.add(mapping.getFragment2());
						}
					}
					if(matchedChildStatements1.size() > 0 && matchedChildStatements1.size() == matchedChildStatements2.size() &&
							(tryStatements1.size() == matchedChildStatements1.size() || tryStatements2.size() == matchedChildStatements2.size())) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		AbstractCall assignmentInvocationCoveringTheEntireStatement1 = invocationCoveringTheEntireStatement1 == null ? statement1.assignmentInvocationCoveringEntireStatement() : invocationCoveringTheEntireStatement1;
		//method invocation is identical
		if(assignmentInvocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(invocation1.identical(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers) &&
							!assignmentInvocationCoveringTheEntireStatement1.getArguments().contains(key1)) {
						String expression1 = assignmentInvocationCoveringTheEntireStatement1.getExpression();
						if(expression1 == null || !expression1.contains(key1)) {
							return replacementInfo.getReplacements();
						}
					}
					else if(invocation1.identicalName(invocationCoveringTheEntireStatement2) && invocation1.equalArguments(invocationCoveringTheEntireStatement2) &&
							!assignmentInvocationCoveringTheEntireStatement1.getArguments().contains(key1) && invocationCoveringTheEntireStatement2.getExpression() != null) {
						boolean expressionMatched = false;
						Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
						Map<VariableDeclaration, AbstractCodeFragment> variableDeclarationsInUnmatchedStatements2 = new LinkedHashMap<>();
						for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
							for(VariableDeclaration variableDeclaration : codeFragment.getVariableDeclarations()) {
								variableDeclarationsInUnmatchedStatements2.put(variableDeclaration, codeFragment);
							}
						}
						for(AbstractCodeFragment codeFragment : replacementInfo.statements2) {
							VariableDeclaration variableDeclaration = codeFragment.getVariableDeclaration(invocationCoveringTheEntireStatement2.getExpression());
							AbstractCall invocationCoveringEntireCodeFragment = codeFragment.invocationCoveringEntireFragment();
							if(variableDeclaration != null && variableDeclaration.getInitializer() != null) {
								String initializer = variableDeclaration.getInitializer().getString();
								if(invocation1.getExpression() != null && invocation1.getExpression().equals(initializer)) {
									Replacement r = new Replacement(invocation1.getExpression(), variableDeclaration.getVariableName(), ReplacementType.VARIABLE_REPLACED_WITH_EXPRESSION_OF_METHOD_INVOCATION);
									replacementInfo.getReplacements().add(r);
									additionallyMatchedStatements2.add(codeFragment);
									expressionMatched = true;
								}
								else if(invocation1.getExpression() != null) {
									String temp = initializer;
									Set<VariableDeclaration> matchingDeclarations = new LinkedHashSet<>();
									for(VariableDeclaration decl : variableDeclarationsInUnmatchedStatements2.keySet()) {
										if(temp.contains(decl.getVariableName() + ".") && decl.getInitializer() != null) {
											temp = ReplacementUtil.performReplacement(temp, decl.getVariableName(), decl.getInitializer().getString());
											matchingDeclarations.add(decl);
										}
									}
									if(invocation1.getExpression().equals(temp)) {
										expressionMatched = true;
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
									}
									else if(invocation1.getExpression().startsWith(temp + ".")) {
										additionallyMatchedStatements2.add(codeFragment);
										for(VariableDeclaration decl : matchingDeclarations) {
											additionallyMatchedStatements2.add(variableDeclarationsInUnmatchedStatements2.get(decl));
										}
										for(AbstractCodeFragment codeFragment2 : replacementInfo.statements2) {
											AbstractCall invocationCoveringEntireCodeFragment2 = codeFragment2.invocationCoveringEntireFragment();
											if(invocationCoveringEntireCodeFragment2 != null) {
												String extendedTemp = temp + "." + invocationCoveringEntireCodeFragment2.actualString().substring(
														invocationCoveringEntireCodeFragment2.getExpression() != null ? invocationCoveringEntireCodeFragment2.getExpression().length()+1 : 0);
												if(invocation1.getExpression().startsWith(extendedTemp)) {
													additionallyMatchedStatements2.add(codeFragment2);
													temp = extendedTemp;
												}
											}
										}
										if(invocation1.getExpression().equals(temp)) {
											expressionMatched = true;
										}
									}
								}
							}
							if(invocationCoveringEntireCodeFragment != null && assignmentInvocationCoveringTheEntireStatement1.identicalName(invocationCoveringEntireCodeFragment) &&
									assignmentInvocationCoveringTheEntireStatement1.equalArguments(invocationCoveringEntireCodeFragment)) {
								additionallyMatchedStatements2.add(codeFragment);
							}
						}
						if(expressionMatched) {
							if(additionallyMatchedStatements2.size() > 0) {
								Replacement r = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
								replacementInfo.getReplacements().add(r);
							}
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		//method invocation is identical with a difference in the expression call chain
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(((OperationInvocation)invocationCoveringTheEntireStatement1).identicalWithExpressionCallChainDifference((OperationInvocation)invocationCoveringTheEntireStatement2)) {
				List<? extends AbstractCall> invokedOperationsBefore = methodInvocationMap1.get(invocationCoveringTheEntireStatement1.getExpression());
				List<? extends AbstractCall> invokedOperationsAfter = methodInvocationMap2.get(invocationCoveringTheEntireStatement2.getExpression());
				if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0 && invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, invokedOperationAfter, ReplacementType.METHOD_INVOCATION_EXPRESSION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsBefore != null && invokedOperationsBefore.size() > 0) {
					AbstractCall invokedOperationBefore = invokedOperationsBefore.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationBefore, Direction.INVOCATION_TO_VARIABLE);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				else if(invokedOperationsAfter != null && invokedOperationsAfter.size() > 0) {
					AbstractCall invokedOperationAfter = invokedOperationsAfter.get(0);
					Replacement replacement = new VariableReplacementWithMethodInvocation(invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression(), invokedOperationAfter, Direction.VARIABLE_TO_INVOCATION);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
				if(((OperationInvocation)invocationCoveringTheEntireStatement1).numberOfSubExpressions() == ((OperationInvocation)invocationCoveringTheEntireStatement2).numberOfSubExpressions() &&
						invocationCoveringTheEntireStatement1.getExpression().contains(".") == invocationCoveringTheEntireStatement2.getExpression().contains(".")) {
					return replacementInfo.getReplacements();
				}
			}
			String expression1 = invocationCoveringTheEntireStatement1.getExpression();
			String expression2 = invocationCoveringTheEntireStatement2.getExpression();
			boolean staticVSNonStatic = (expression1 == null && expression2 != null && operation1 != null && operation1.getClassName().endsWith("." + expression2)) ||
					(expression1 != null && expression2 == null && operation2 != null && operation2.getClassName().endsWith("." + expression1));
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) && staticVSNonStatic &&
					invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				return replacementInfo.getReplacements();
			}
		}
		//method invocation is identical if arguments are replaced
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocation2, replacementInfo.getReplacements(), lambdaMappers)) {
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation is identical if arguments are wrapped or concatenated
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.identicalExpression(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) &&
				invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2) ) {
			for(String key : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key)) {
					if(invocationCoveringTheEntireStatement1.identicalOrWrappedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_WRAPPED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					if(invocationCoveringTheEntireStatement1.identicalOrConcatenatedArguments(invocation2)) {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT_CONCATENATED);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has been renamed but the expression and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed and the expression is different but the arguments are identical, and the variable declarations are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString()) &&
				invocationCoveringTheEntireStatement1.variableDeclarationInitializersRenamedWithIdenticalArguments(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed but the expressions are null and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalArgumentsAndNoExpression(invocationCoveringTheEntireStatement2, UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(),
					invocationCoveringTheEntireStatement2.getName(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed (one name contains the other), one expression is null, but the other is not null, and arguments are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithDifferentExpressionAndIdenticalArguments(invocationCoveringTheEntireStatement2)) {
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_NAME_AND_EXPRESSION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//method invocation has been renamed and arguments changed, but the expressions are identical
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers)) {
			ReplacementType type = invocationCoveringTheEntireStatement1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
			Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
					invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, type);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.renamedWithIdenticalExpressionAndDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), UMLClassBaseDiff.MAX_OPERATION_NAME_DISTANCE, lambdaMappers) &&
							!isExpressionOfAnotherMethodInvocation(operationInvocation1, methodInvocationMap1)) {
						ReplacementType type = operationInvocation1.getName().equals(invocationCoveringTheEntireStatement2.getName()) ? ReplacementType.METHOD_INVOCATION_ARGUMENT : ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT;
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, type);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//method invocation has only changes in the arguments (different number of arguments)
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
				return replacementInfo.getReplacements();
			}
			else if(invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(!methodInvocations1.isEmpty() && invocationCoveringTheEntireStatement2 != null) {
			for(String methodInvocation1 : methodInvocations1) {
				for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
					if(operationInvocation1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
						return replacementInfo.getReplacements();
					}
					else if(operationInvocation1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new MethodInvocationReplacement(operationInvocation1.actualString(),
								invocationCoveringTheEntireStatement2.actualString(), operationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the first statement is returned in the second statement
		Replacement r;
		if(invocationCoveringTheEntireStatement1 != null && (r = invocationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation1 : methodInvocations1) {
			for(AbstractCall operationInvocation1 : methodInvocationMap1.get(methodInvocation1)) {
				if(statement1.getString().endsWith(methodInvocation1 + ";\n") && (r = operationInvocation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					if(operationInvocation1.makeReplacementForReturnedArgument(statement2.getString()) != null) {
						replacementInfo.addReplacement(r);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is returned in the first statement
		if(invocationCoveringTheEntireStatement2 != null && (r = invocationCoveringTheEntireStatement2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String methodInvocation2 : methodInvocations2) {
			for(AbstractCall operationInvocation2 : methodInvocationMap2.get(methodInvocation2)) {
				if(statement2.getString().endsWith(methodInvocation2 + ";\n") && (r = operationInvocation2.makeReplacementForWrappedCall(replacementInfo.getArgumentizedString1())) != null) {
					if(operationInvocation2.makeReplacementForWrappedCall(statement1.getString()) != null) {
						replacementInfo.addReplacement(r);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the method call in the second statement is the right hand side of an assignment in the first statement
		if(invocationCoveringTheEntireStatement2 != null &&
				(r = invocationCoveringTheEntireStatement2.makeReplacementForAssignedArgument(replacementInfo.getArgumentizedString1())) != null &&
				methodInvocationMap1.containsKey(invocationCoveringTheEntireStatement2.getArguments().get(0))) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		//check if the method call in the second statement is the expression (or sub-expression) of the method invocation in the first statement
		if(invocationCoveringTheEntireStatement2 != null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						if(methodInvocationMap2.containsKey(invocation1.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
									invocationCoveringTheEntireStatement2.actualString(), invocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement2.getArguments().contains(invocation1.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation1 instanceof OperationInvocation) {
							for(String subExpression1 : ((OperationInvocation)invocation1).getSubExpressions()) {
								if(methodInvocationMap2.containsKey(subExpression1)) {
									AbstractCall subOperationInvocation1 = null;
									for(String key : methodInvocationMap1.keySet()) {
										if(key.endsWith(subExpression1)) {
											subOperationInvocation1 = methodInvocationMap1.get(key).get(0);
											break;
										}
									}
									Replacement replacement = new MethodInvocationReplacement(subExpression1,
											invocationCoveringTheEntireStatement2.actualString(), subOperationInvocation1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		//check if the method call in the first statement is the expression (or sub-expression) of the method invocation in the second statement
		if(invocationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + ";\n")) {
						if(methodInvocationMap1.containsKey(invocation2.getExpression())) {
							Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
									invocation2.actualString(), invocationCoveringTheEntireStatement1, invocation2, ReplacementType.METHOD_INVOCATION);
							replacementInfo.addReplacement(replacement);
							if(variableDeclarationsWithEverythingReplaced(variableDeclarations1, variableDeclarations2, replacementInfo) &&
									invocationCoveringTheEntireStatement1.getArguments().contains(invocation2.getExpression()) &&
									!statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
									!statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
								return null;
							}
							return replacementInfo.getReplacements();
						}
						if(invocation2 instanceof OperationInvocation) {
							for(String subExpression2 : ((OperationInvocation)invocation2).getSubExpressions()) {
								if(methodInvocationMap1.containsKey(subExpression2)) {
									AbstractCall subOperationInvocation2 = null;
									for(String key : methodInvocationMap2.keySet()) {
										if(key.endsWith(subExpression2)) {
											subOperationInvocation2 = methodInvocationMap2.get(key).get(0);
											break;
										}
									}
									Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
											subExpression2, invocationCoveringTheEntireStatement1, subOperationInvocation2, ReplacementType.METHOD_INVOCATION);
									replacementInfo.addReplacement(replacement);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		//check if the argument of the class instance creation in the first statement is the expression of the method invocation in the second statement
		if(creationCoveringTheEntireStatement1 != null) {
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(statement2.getString().endsWith(key2 + ";\n") && invocation2.getExpression() != null &&
							(creationCoveringTheEntireStatement1.getArguments().contains(invocation2.getExpression()) ||
							invocation2.getExpression().startsWith(creationCoveringTheEntireStatement1.actualString()))) {
						Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
								invocation2.getName(), ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION, creationCoveringTheEntireStatement1, invocation2);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//check if the argument of the class instance creation in the second statement is the class instance creation of the first statement
		if(creationCoveringTheEntireStatement2 != null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n") &&
							creationCoveringTheEntireStatement2.getArguments().contains(creation1.actualString())) {
						if(variableDeclarations1.size() > 0) {
							VariableDeclaration declaration1 = variableDeclarations1.get(0);
							for(AbstractCodeFragment fragment1 : replacementInfo.statements1) {
								Map<String, List<ObjectCreation>> fragmentCreationMap1 = fragment1.getCreationMap();
								for(String fragmentKey1 : fragmentCreationMap1.keySet()) {
									for(AbstractCall fragmentCreation1 : fragmentCreationMap1.get(fragmentKey1)) {
										if(fragmentCreation1.getArguments().contains(declaration1.getVariableName()) &&
												creationCoveringTheEntireStatement2.identicalName(fragmentCreation1)) {
											Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
											additionallyMatchedStatements1.add(fragment1);
											Replacement replacement = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
											replacementInfo.addReplacement(replacement);
											return replacementInfo.getReplacements();
										}
									}
								}
							}
						}
						Replacement replacement = new ObjectCreationReplacement(creation1.getName(),
								creationCoveringTheEntireStatement2.getName(), (ObjectCreation)creation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
				}
			}
		}
		//builder call chain in the first statement is replaced with class instance creation in the second statement
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement1.getName().equals("build")) {
			if(creationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							Set<String> argumentIntersection = invocation1.argumentIntersection(creationCoveringTheEntireStatement2);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
							creationCoveringTheEntireStatement2.getName(), ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION, invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
				for(String key1 : methodInvocationMap1.keySet()) {
					if(invocationCoveringTheEntireStatement1.actualString().startsWith(key1)) {
						for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
							if(invocation1.equalArguments(invocationCoveringTheEntireStatement2)) {
								commonArguments += invocation1.getArguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation1.argumentIntersection(invocationCoveringTheEntireStatement2);
								int threshold = Math.max(invocation1.getArguments().size(), invocationCoveringTheEntireStatement2.getArguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.statements2) { 
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
								if(invocation != null) { 
									if(invocation.identical(invocation1, replacementInfo.getReplacements(), lambdaMappers)) { 
										additionallyMatchedStatements2.add(codeFragment); 
									} 
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation1.actualString())) ||
											(invocation instanceof OperationInvocation && ((OperationInvocation)invocation).callChainIntersection((OperationInvocation)invocation1).size() > 0)) {
										additionallyMatchedStatements2.add(codeFragment); 
									} 
								} 
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements2.size() > 0) { 
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
						replacementInfo.addReplacement(composite); 
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(), invocationCoveringTheEntireStatement2.getName(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//class instance creation in the first statement is replaced with builder call chain in the second statement
		if(invocationCoveringTheEntireStatement2 != null && invocationCoveringTheEntireStatement2.getName().equals("build")) {
			if(creationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							Set<String> argumentIntersection = invocation2.argumentIntersection(creationCoveringTheEntireStatement1);
							commonArguments += argumentIntersection.size();
						}
					}
				}
				if(commonArguments > 0) {
					Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
							invocationCoveringTheEntireStatement2.getName(), ReplacementType.BUILDER_REPLACED_WITH_CLASS_INSTANCE_CREATION, creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2);
					replacementInfo.addReplacement(replacement);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null) {
				int commonArguments = 0;
				Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
				for(String key2 : methodInvocationMap2.keySet()) {
					if(invocationCoveringTheEntireStatement2.actualString().startsWith(key2)) {
						for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
							if(invocation2.equalArguments(invocationCoveringTheEntireStatement1)) {
								commonArguments += invocation2.getArguments().size();
							}
							else {
								Set<String> argumentIntersection = invocation2.argumentIntersection(invocationCoveringTheEntireStatement1);
								int threshold = Math.max(invocation2.getArguments().size(), invocationCoveringTheEntireStatement1.getArguments().size())/2;
								if(argumentIntersection.size() > threshold) {
									commonArguments += argumentIntersection.size();
								}
							}
							for(AbstractCodeFragment codeFragment : replacementInfo.statements1) {
								AbstractCall invocation = codeFragment.invocationCoveringEntireFragment();
								if(invocation != null) {
									if(invocation.identical(invocation2, replacementInfo.getReplacements(), lambdaMappers)) {
										additionallyMatchedStatements1.add(codeFragment);
									}
									if((invocation.getExpression() != null && invocation.getExpression().equals(invocation2.actualString())) ||
											(invocation instanceof OperationInvocation && ((OperationInvocation)invocation).callChainIntersection((OperationInvocation)invocation2).size() > 0)) {
										additionallyMatchedStatements1.add(codeFragment);
									}
								}
							}
						}
					}
				}
				if(commonArguments > 0) {
					if(additionallyMatchedStatements1.size() > 0) {
						Replacement composite = new CompositeReplacement(statement1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
						replacementInfo.addReplacement(composite);
					}
					else {
						Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.getName(), invocationCoveringTheEntireStatement2.getName(),
								invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION);
						replacementInfo.addReplacement(replacement);
					}
					return replacementInfo.getReplacements();
				}
			}
		}
		//object creation is identical
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identical(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
			boolean identicalArrayInitializer = true;
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray()) {
				identicalArrayInitializer = creationCoveringTheEntireStatement1.identicalArrayInitializer(creationCoveringTheEntireStatement2);
			}
			if(identicalArrayInitializer) {
				String anonymousClassDeclaration1 = creationCoveringTheEntireStatement1.getAnonymousClassDeclaration();
				String anonymousClassDeclaration2 = creationCoveringTheEntireStatement2.getAnonymousClassDeclaration();
				if(anonymousClassDeclaration1 != null && anonymousClassDeclaration2 != null && !anonymousClassDeclaration1.equals(anonymousClassDeclaration2)) {
					Replacement replacement = new Replacement(anonymousClassDeclaration1, anonymousClassDeclaration2, ReplacementType.ANONYMOUS_CLASS_DECLARATION);
					replacementInfo.addReplacement(replacement);
				}
				return replacementInfo.getReplacements();
			}
		}
		//object creation has identical arguments, but different type
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.getArguments().size() > 0 && creationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2)) {
			Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
					creationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION);
			replacementInfo.addReplacement(replacement);
			return replacementInfo.getReplacements();
		}
		//object creation has only changes in the arguments
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
				return replacementInfo.getReplacements();
			}
			else if(creationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
				Replacement replacement = new ObjectCreationReplacement(creationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		//check if the argument lists are identical after replacements
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				creationCoveringTheEntireStatement1.identicalName(creationCoveringTheEntireStatement2) &&
				creationCoveringTheEntireStatement1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
			if(creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
					extractSubstringBetweenBrackets(s1).equals(extractSubstringBetweenBrackets(s2)) &&
					extractSubstringBetweenBrackets(s1).length() > 0) {
				return replacementInfo.getReplacements();
			}
			if(!creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
					extractSubstringBetween(s1, "(", ")").equals(extractSubstringBetween(s2, "(", ")")) &&
					extractSubstringBetween(s1, "(", ")").length() > 0) {
				return replacementInfo.getReplacements();
			}
		}
		//check if array creation is replaced with data structure creation
		if(creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
			VariableDeclaration v1 = variableDeclarations1.get(0);
			VariableDeclaration v2 = variableDeclarations2.get(0);
			String initializer1 = v1.getInitializer() != null ? v1.getInitializer().getString() : null;
			String initializer2 = v2.getInitializer() != null ? v2.getInitializer().getString() : null;
			if(v1.getType().getArrayDimension() == 1 && v2.getType().containsTypeArgument(v1.getType().getClassType()) &&
					creationCoveringTheEntireStatement1.isArray() && !creationCoveringTheEntireStatement2.isArray() &&
					initializer1 != null && initializer2 != null &&
					extractSubstringBetweenBrackets(initializer1).equals(extractSubstringBetween(initializer2, "(", ")"))) {
				r = new ObjectCreationReplacement(initializer1, initializer2,
						creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			if(v2.getType().getArrayDimension() == 1 && v1.getType().containsTypeArgument(v2.getType().getClassType()) &&
					!creationCoveringTheEntireStatement1.isArray() && creationCoveringTheEntireStatement2.isArray() &&
					initializer1 != null && initializer2 != null &&
					extractSubstringBetween(initializer1, "(", ")").equals(extractSubstringBetweenBrackets(initializer2))) {
				r = new ObjectCreationReplacement(initializer1, initializer2,
						creationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2, ReplacementType.ARRAY_CREATION_REPLACED_WITH_DATA_STRUCTURE_CREATION);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(!creations1.isEmpty() && creationCoveringTheEntireStatement2 != null) {
			for(String creation1 : creations1) {
				for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
					if(objectCreation1.identicalWithMergedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
						return replacementInfo.getReplacements();
					}
					else if(objectCreation1.identicalWithDifferentNumberOfArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)) {
						Replacement replacement = new ObjectCreationReplacement(objectCreation1.getName(),
								creationCoveringTheEntireStatement2.getName(), (ObjectCreation)objectCreation1, creationCoveringTheEntireStatement2, ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT);
						replacementInfo.addReplacement(replacement);
						return replacementInfo.getReplacements();
					}
					//check if the argument lists are identical after replacements
					if(objectCreation1.identicalName(creationCoveringTheEntireStatement2) &&
							objectCreation1.identicalExpression(creationCoveringTheEntireStatement2, replacementInfo.getReplacements())) {
						if(((ObjectCreation)objectCreation1).isArray() && creationCoveringTheEntireStatement2.isArray() && s1.contains("[") && s2.contains("[") &&
								extractSubstringBetweenBrackets(s1).equals(extractSubstringBetweenBrackets(s2)) &&
								extractSubstringBetweenBrackets(s1).length() > 0) {
							return replacementInfo.getReplacements();
						}
						if(!((ObjectCreation)objectCreation1).isArray() && !creationCoveringTheEntireStatement2.isArray() && s1.contains("(") && s2.contains("(") &&
								extractSubstringBetween(s1, "(", ")").equals(extractSubstringBetween(s2, "(", ")")) &&
								extractSubstringBetween(s1, "(", ")").length() > 0) {
							return replacementInfo.getReplacements();
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && (r = creationCoveringTheEntireStatement1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
			replacementInfo.addReplacement(r);
			return replacementInfo.getReplacements();
		}
		for(String creation1 : creations1) {
			for(AbstractCall objectCreation1 : creationMap1.get(creation1)) {
				if(statement1.getString().endsWith(creation1 + ";\n") && (r = objectCreation1.makeReplacementForReturnedArgument(replacementInfo.getArgumentizedString2())) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
		}
		if(variableDeclarationWithArrayInitializer1 != null && invocationCoveringTheEntireStatement2 != null && variableDeclarations2.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = extractSubstringBetween(s1, "{", "}");
			String args2 = extractSubstringBetween(s2, "(", ")");
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(variableDeclarationWithArrayInitializer2 != null && invocationCoveringTheEntireStatement1 != null && variableDeclarations1.isEmpty() &&
				!containsMethodSignatureOfAnonymousClass(statement1.getString()) && !containsMethodSignatureOfAnonymousClass(statement2.getString())) {
			String args1 = extractSubstringBetween(s1, "(", ")");
			String args2 = extractSubstringBetween(s2, "{", "}");
			if(args1.equals(args2)) {
				r = new Replacement(args1, args2, ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		List<TernaryOperatorExpression> ternaryOperatorExpressions1 = statement1.getTernaryOperatorExpressions();
		List<TernaryOperatorExpression> ternaryOperatorExpressions2 = statement2.getTernaryOperatorExpressions();
		if(ternaryOperatorExpressions1.isEmpty() && ternaryOperatorExpressions2.size() == 1) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions2.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(methodInvocation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(invocationCoveringTheEntireStatement1.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement1 != null && (r = ternary.makeReplacementWithTernaryOnTheRight(creationCoveringTheEntireStatement1.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			for(String creation2 : creations2) {
				if((r = ternary.makeReplacementWithTernaryOnTheRight(creation2)) != null) {
					for(AbstractCall c2 : creationMap2.get(creation2)) {
						for(String creation1 : creations1) {
							for(AbstractCall c1 : creationMap1.get(creation1)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(ternaryOperatorExpressions1.size() == 1 && ternaryOperatorExpressions2.isEmpty()) {
			TernaryOperatorExpression ternary = ternaryOperatorExpressions1.get(0);
			for(String creation : creationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			for(String methodInvocation : methodInvocationIntersection) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(methodInvocation)) != null) {
					replacementInfo.addReplacement(r);
					return replacementInfo.getReplacements();
				}
			}
			if(invocationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(invocationCoveringTheEntireStatement2.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			if(creationCoveringTheEntireStatement2 != null && (r = ternary.makeReplacementWithTernaryOnTheLeft(creationCoveringTheEntireStatement2.actualString())) != null) {
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
			for(String creation1 : creations1) {
				if((r = ternary.makeReplacementWithTernaryOnTheLeft(creation1)) != null) {
					for(AbstractCall c1 : creationMap1.get(creation1)) {
						for(String creation2 : creations2) {
							for(AbstractCall c2 : creationMap2.get(creation2)) {
								if(((ObjectCreation)c1).getType().compatibleTypes(((ObjectCreation)c2).getType()) && c1.equalArguments(c2)) {
									replacementInfo.addReplacement(r);
									return replacementInfo.getReplacements();
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement2 != null && statement2.getString().equals(invocationCoveringTheEntireStatement2.actualString() + ";\n") &&
				invocationCoveringTheEntireStatement2.getArguments().size() == 1 && statement1.getString().endsWith("=" + invocationCoveringTheEntireStatement2.getArguments().get(0) + ";\n") &&
				invocationCoveringTheEntireStatement2.expressionIsNullOrThis() && invocationCoveringTheEntireStatement2.getName().startsWith("set")) {
			String prefix1 = statement1.getString().substring(0, statement1.getString().lastIndexOf("="));
			if(variables1.contains(prefix1)) {
				String before = prefix1 + "=" + invocationCoveringTheEntireStatement2.getArguments().get(0);
				String after = invocationCoveringTheEntireStatement2.actualString();
				r = new Replacement(before, after, ReplacementType.FIELD_ASSIGNMENT_REPLACED_WITH_SETTER_METHOD_INVOCATION);
				replacementInfo.addReplacement(r);
				return replacementInfo.getReplacements();
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == variableDeclarations2.size()) {
			if(creationCoveringTheEntireStatement1.equalArguments(invocationCoveringTheEntireStatement2) && creationCoveringTheEntireStatement1.getArguments().size() > 0) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION, creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(creationCoveringTheEntireStatement1 == null && invocationCoveringTheEntireStatement2 == null) {
			for(String key1 : creationMap1.keySet()) {
				for(AbstractCall creation1 : creationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						for(String key2 : methodInvocationMap2.keySet()) {
							for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + ";\n")) {
									if(creation1.equalArguments(invocation2) && creation1.getArguments().size() > 0) {
										Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creation1.getName(),
												invocation2.getName(), ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION, (ObjectCreation)creation1, invocation2);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null &&
				variableDeclarations1.size() == variableDeclarations2.size()) {
			if(invocationCoveringTheEntireStatement1.equalArguments(creationCoveringTheEntireStatement2) && invocationCoveringTheEntireStatement1.getArguments().size() > 0) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION, invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 == null && creationCoveringTheEntireStatement2 == null) {
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(statement1.getString().endsWith(key1 + ";\n")) {
						for(String key2 : creationMap2.keySet()) {
							for(AbstractCall creation2 : creationMap2.get(key2)) {
								if(statement2.getString().endsWith(key2 + ";\n")) {
									if(invocation1.equalArguments(creation2) && invocation1.getArguments().size() > 0) {
										Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocation1.getName(),
												creation2.getName(), ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION, invocation1, (ObjectCreation)creation2);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(creationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null) {
			if(creationCoveringTheEntireStatement1.getArguments().size() > 1 && creationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2).size() > 0 &&
					creationCoveringTheEntireStatement1.getCoverage().equals(invocationCoveringTheEntireStatement2.getCoverage()) &&
					creationCoveringTheEntireStatement1.identicalOrReplacedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				Replacement replacement = new ClassInstanceCreationWithMethodInvocationReplacement(creationCoveringTheEntireStatement1.getName(),
						invocationCoveringTheEntireStatement2.getName(), ReplacementType.CLASS_INSTANCE_CREATION_REPLACED_WITH_METHOD_INVOCATION, creationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement2 != null) {
			if(invocationCoveringTheEntireStatement1.getArguments().size() > 1 && invocationCoveringTheEntireStatement1.argumentIntersection(creationCoveringTheEntireStatement2).size() > 0 &&
					invocationCoveringTheEntireStatement1.getCoverage().equals(creationCoveringTheEntireStatement2.getCoverage()) &&
					invocationCoveringTheEntireStatement1.identicalOrReplacedArguments(creationCoveringTheEntireStatement2, replacementInfo.getReplacements(), lambdaMappers)) {
				Replacement replacement = new MethodInvocationWithClassInstanceCreationReplacement(invocationCoveringTheEntireStatement1.getName(),
						creationCoveringTheEntireStatement2.getName(), ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CLASS_INSTANCE_CREATION, invocationCoveringTheEntireStatement1, creationCoveringTheEntireStatement2);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		if(invocationCoveringTheEntireStatement1 instanceof OperationInvocation && invocationCoveringTheEntireStatement2 instanceof MethodReference) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		else if(invocationCoveringTheEntireStatement1 instanceof MethodReference && invocationCoveringTheEntireStatement2 instanceof OperationInvocation) {
			if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
				Replacement replacement = new MethodInvocationReplacement(invocationCoveringTheEntireStatement1.actualString(),
						invocationCoveringTheEntireStatement2.actualString(), invocationCoveringTheEntireStatement1, invocationCoveringTheEntireStatement2, ReplacementType.METHOD_INVOCATION_REPLACED_WITH_METHOD_REFERENCE);
				replacementInfo.addReplacement(replacement);
				return replacementInfo.getReplacements();
			}
		}
		return null;
	}

	private Set<Replacement> processAnonymousAndLambdas(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo,
			AbstractCall invocationCoveringTheEntireStatement1,
			AbstractCall invocationCoveringTheEntireStatement2,
			Map<String, List<? extends AbstractCall>> methodInvocationMap1,
			Map<String, List<? extends AbstractCall>> methodInvocationMap2,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations1,
			List<AnonymousClassDeclarationObject> anonymousClassDeclarations2,
			List<LambdaExpressionObject> lambdas1,
			List<LambdaExpressionObject> lambdas2,
			List<UMLOperationBodyMapper> lambdaMappers)
			throws RefactoringMinerTimedOutException {
		boolean replacementAdded = false;
		if(!anonymousClassDeclarations1.isEmpty() && !anonymousClassDeclarations2.isEmpty() && (operation1 != null || attribute1 != null) && (operation2 != null || attribute2 != null)) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				for(int j=0; j<anonymousClassDeclarations2.size(); j++) {
					AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
					AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(j);
					String statementWithoutAnonymous1 = statementWithoutAnonymous(statement1, anonymousClassDeclaration1, operation1);
					String statementWithoutAnonymous2 = statementWithoutAnonymous(statement2, anonymousClassDeclaration2, operation2);
					if(statementWithoutAnonymous1.equals(statementWithoutAnonymous2) ||
							identicalAfterVariableAndTypeReplacements(statementWithoutAnonymous1, statementWithoutAnonymous2, replacementInfo.getReplacements()) ||
							(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							(invocationCoveringTheEntireStatement1.identicalWithMergedArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements()) ||
							invocationCoveringTheEntireStatement1.identicalWithDifferentNumberOfArguments(invocationCoveringTheEntireStatement2, replacementInfo.getReplacements(), parameterToArgumentMap)))) {
						UMLAnonymousClass anonymousClass1 = null;
						if(operation1 != null)
							anonymousClass1 = operation1.findAnonymousClass(anonymousClassDeclaration1);
						if(attribute1 != null)
							anonymousClass1 = attribute1.findAnonymousClass(anonymousClassDeclaration1);
						UMLAnonymousClass anonymousClass2 = null;
						if(operation2 != null)
							anonymousClass2 = operation2.findAnonymousClass(anonymousClassDeclaration2);
						if(attribute2 != null)
							anonymousClass2 = attribute2.findAnonymousClass(anonymousClassDeclaration2);
						UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(anonymousClass1, anonymousClass2, classDiff, modelDiff);
						anonymousClassDiff.process();
						List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
						if(matchedOperationMappers.size() > 0) {
							for(UMLOperationBodyMapper mapper : matchedOperationMappers) {
								this.mappings.addAll(mapper.mappings);
								this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
								this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
								this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
								this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
							}
							this.refactorings.addAll(anonymousClassDiff.getRefactorings());
							if(!anonymousClassDeclaration1.toString().equals(anonymousClassDeclaration2.toString())) {
								Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), anonymousClassDeclaration2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION);
								replacementInfo.addReplacement(replacement);
								replacementAdded = true;
							}
						}
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 0 && anonymousClassDeclarations2.size() == 1 && (operation2 != null || attribute2 != null)) {
			AnonymousClassDeclarationObject anonymousClassDeclaration2 = anonymousClassDeclarations2.get(0);
			UMLAnonymousClass anonymousClass2 = null;
			if(operation2 != null)
				anonymousClass2 = operation2.findAnonymousClass(anonymousClassDeclaration2);
			if(attribute2 != null)
				anonymousClass2 = attribute2.findAnonymousClass(anonymousClassDeclaration2);
			if(anonymousClass2.getOperations().size() == 1) {
				UMLOperation anonymousClass2Operation = anonymousClass2.getOperations().get(0);
				if(anonymousClass2Operation.getBody() != null) {
					List<AbstractStatement> statements = anonymousClass2Operation.getBody().getCompositeStatement().getStatements();
					if(statements.size() == 1) {
						AbstractStatement statement = statements.get(0);
						AbstractCall invocation2 = statement.invocationCoveringEntireFragment();
						if(invocation2 != null) {
							for(String key1 : methodInvocationMap1.keySet()) {
								for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), Collections.emptyList())) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		else if(anonymousClassDeclarations1.size() == 1 && anonymousClassDeclarations2.size() == 0 && (operation1 != null || attribute1 != null)) {
			AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(0);
			UMLAnonymousClass anonymousClass1 = null;
			if(operation1 != null)
				anonymousClass1 = operation1.findAnonymousClass(anonymousClassDeclaration1);
			if(attribute1 != null)
				anonymousClass1 = attribute1.findAnonymousClass(anonymousClassDeclaration1);
			if(anonymousClass1.getOperations().size() == 1) {
				UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
				if(anonymousClass1Operation.getBody() != null) {
					List<AbstractStatement> statements = anonymousClass1Operation.getBody().getCompositeStatement().getStatements();
					if(statements.size() == 1) {
						AbstractStatement statement = statements.get(0);
						AbstractCall invocation1 = statement.invocationCoveringEntireFragment();
						if(invocation1 != null) {
							for(String key2 : methodInvocationMap2.keySet()) {
								for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
									if(invocation1.identical(invocation2, replacementInfo.getReplacements(), Collections.emptyList())) {
										Replacement replacement = new MethodInvocationReplacement(invocation1.actualString(),
												invocation2.actualString(), invocation1, invocation2, ReplacementType.METHOD_INVOCATION_WRAPPED_IN_ANONYMOUS_CLASS_DECLARATION);
										replacementInfo.addReplacement(replacement);
										return replacementInfo.getReplacements();
									}
								}
							}
						}
					}
				}
			}
		}
		if(!lambdas1.isEmpty() && !lambdas2.isEmpty()) {
			for(int i=0; i<lambdas1.size(); i++) {
				for(int j=0; j<lambdas2.size(); j++) {
					LambdaExpressionObject lambda1 = lambdas1.get(i);
					LambdaExpressionObject lambda2 = lambdas2.get(j);
					UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(lambda1, lambda2, this);
					int mappings = mapper.mappingsWithoutBlocks();
					if(mappings > 0) {
						int nonMappedElementsT1 = mapper.nonMappedElementsT1();
						int nonMappedElementsT2 = mapper.nonMappedElementsT2();
						List<AbstractCall> invocations1 = streamAPICalls(mapper.getNonMappedLeavesT1());
						List<AbstractCall> invocations2 = streamAPICalls(mapper.getNonMappedLeavesT2());
						if(invocations1.size() > 0 && invocations2.size() == 0) {
							nonMappedElementsT2 = nonMappedElementsT2 - ignoredNonMappedElements(invocations1, mapper.getNonMappedLeavesT2(), mapper.getNonMappedInnerNodesT2());
						}
						else if(invocations1.size() == 0 && invocations2.size() > 0) {
							nonMappedElementsT1 = nonMappedElementsT1 - ignoredNonMappedElements(invocations2, mapper.getNonMappedLeavesT1(), mapper.getNonMappedInnerNodesT1());
						}
						if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
								nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0) {
							this.mappings.addAll(mapper.mappings);
							this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
							this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
							this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
							this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
							if(this.operation1 != null && this.operation2 != null) {
								this.refactorings.addAll(mapper.getRefactorings());
							}
							lambdaMappers.add(mapper);
						}
					}
				}
			}
		}
		if(anonymousClassDeclarations1.size() >= 1 && (operation1 != null || attribute1 != null) && lambdas2.size() >= 1) {
			for(int i=0; i<anonymousClassDeclarations1.size(); i++) {
				AnonymousClassDeclarationObject anonymousClassDeclaration1 = anonymousClassDeclarations1.get(i);
				UMLAnonymousClass anonymousClass1 = null;
				if(operation1 != null)
					anonymousClass1 = operation1.findAnonymousClass(anonymousClassDeclaration1);
				if(attribute1 != null)
					anonymousClass1 = attribute1.findAnonymousClass(anonymousClassDeclaration1);
				if(anonymousClass1.getOperations().size() == 1) {
					UMLOperation anonymousClass1Operation = anonymousClass1.getOperations().get(0);
					for(int j=0; j<lambdas2.size(); j++) {
						LambdaExpressionObject lambda2 = lambdas2.get(j);
						UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(anonymousClass1Operation, lambda2, this);
						int mappings = mapper.mappingsWithoutBlocks();
						if(mappings > 0) {
							int nonMappedElementsT1 = mapper.nonMappedElementsT1();
							int nonMappedElementsT2 = mapper.nonMappedElementsT2();
							if((mappings > nonMappedElementsT1 && mappings > nonMappedElementsT2) ||
									nonMappedElementsT1 == 0 || nonMappedElementsT2 == 0) {
								this.mappings.addAll(mapper.mappings);
								this.nonMappedInnerNodesT1.addAll(mapper.nonMappedInnerNodesT1);
								this.nonMappedInnerNodesT2.addAll(mapper.nonMappedInnerNodesT2);
								this.nonMappedLeavesT1.addAll(mapper.nonMappedLeavesT1);
								this.nonMappedLeavesT2.addAll(mapper.nonMappedLeavesT2);
								if(this.operation1 != null && this.operation2 != null) {
									ReplaceAnonymousWithLambdaRefactoring ref = new ReplaceAnonymousWithLambdaRefactoring(anonymousClass1, lambda2, operation1, operation2);
									this.refactorings.add(ref);
									this.refactorings.addAll(mapper.getRefactorings());
								}
								else if(this.attribute1 != null && this.attribute2 != null) {
									ReplaceAnonymousWithLambdaRefactoring ref = new ReplaceAnonymousWithLambdaRefactoring(anonymousClass1, lambda2, attribute1, attribute2);
									this.refactorings.add(ref);
								}
								Replacement replacement = new Replacement(anonymousClassDeclaration1.toString(), lambda2.toString(), ReplacementType.ANONYMOUS_CLASS_DECLARATION_REPLACED_WITH_LAMBDA);
								replacementInfo.addReplacement(replacement);
								replacementAdded = true;
								lambdaMappers.add(mapper);
							}
						}
					}
				}
			}
		}
		if(replacementAdded) {
			return replacementInfo.getReplacements();
		}
		return null;
	}

	private int ignoredNonMappedElements(List<AbstractCall> invocations, List<AbstractCodeFragment> nonMappedLeaves, List<CompositeStatementObject> nonMappedInnerNodes) {
		int counter = 0;
		for(AbstractCall inv : invocations) {
			if(inv.getName().equals("forEach")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
							comp.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
						counter++;
					}
				}
				for(AbstractCodeFragment statement : nonMappedLeaves) {
					Map<String, List<AbstractCall>> methodInvocationMap = statement.getMethodInvocationMap();
					for(String key : methodInvocationMap.keySet()) {
						List<AbstractCall> statementInvocations = methodInvocationMap.get(key);
						for(AbstractCall statementInvocation : statementInvocations) {
							if(statementInvocation.getName().equals("iterator") || statementInvocation.getName().equals("next")) {
								counter++;
							}
						}
					}
					for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
						if(declaration.getInitializer() != null && declaration.getInitializer().getNumberLiterals().contains(declaration.getInitializer().getExpression())) {
							counter++;
						}
					}
				}
			}
			else if(inv.getName().equals("filter")) {
				for(CompositeStatementObject comp : nonMappedInnerNodes) {
					if(comp.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
						counter++;
					}
				}
			}
		}
		return counter;
	}

	private String extractSubstringBetweenBrackets(String s1) {
		return extractSubstringBetween(s1, "[", "]");
	}

	private String extractSubstringBetween(String s1, String s, String s2) {
		return s1.substring(s1.indexOf(s) + 1, s1.lastIndexOf(s2));
	}

	private boolean equalAfterInfixExpressionExpansion(String s1, String s2, ReplacementInfo replacementInfo, List<String> infixExpressions1) {
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<>();
		String originalArgumentizedString1 = replacementInfo.getArgumentizedString1();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			String before = replacement.getBefore();
			for(String infixExpression1 : infixExpressions1) {
				if(infixExpression1.startsWith(before)) {
					String suffix = infixExpression1.substring(before.length());
					String after = replacement.getAfter();
					if(s1.contains(after + suffix)) {
						String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), after + suffix, after);
						int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
						if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
							replacementsToBeRemoved.add(replacement);
							Replacement newReplacement = new Replacement(infixExpression1, after, ReplacementType.INFIX_EXPRESSION);
							replacementsToBeAdded.add(newReplacement);
							replacementInfo.setArgumentizedString1(temp);
						}
					}
				}
			}
		}
		if(replacementInfo.getRawDistance() == 0) {
			replacementInfo.removeReplacements(replacementsToBeRemoved);
			replacementInfo.addReplacements(replacementsToBeAdded);
			return true;
		}
		else {
			replacementInfo.setArgumentizedString1(originalArgumentizedString1);
			return false;
		}
	}

	private boolean isExpressionOfAnotherMethodInvocation(AbstractCall invocation, Map<String, List<? extends AbstractCall>> invocationMap) {
		for(String key : invocationMap.keySet()) {
			List<? extends AbstractCall> invocations = invocationMap.get(key);
			for(AbstractCall call : invocations) {
				if(!call.equals(invocation) && call.getExpression() != null && call.getExpression().equals(invocation.actualString())) {
					for(String argument : call.getArguments()) {
						if(invocationMap.containsKey(argument)) {
							List<? extends AbstractCall> argumentInvocations = invocationMap.get(argument);
							for(AbstractCall argumentCall : argumentInvocations) {
								if(argumentCall.identicalName(invocation) && argumentCall.equalArguments(invocation)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean validStatementForConcatComparison(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		List<VariableDeclaration> variableDeclarations1 = statement1.getVariableDeclarations();
		List<VariableDeclaration> variableDeclarations2 = statement2.getVariableDeclarations();
		if(variableDeclarations1.size() == variableDeclarations2.size()) {
			return true;
		}
		else {
			if(variableDeclarations1.size() > 0 && variableDeclarations2.size() == 0 && statement2.getString().startsWith("return ")) {
				return true;
			}
			else return variableDeclarations1.size() == 0 && variableDeclarations2.size() > 0 && statement1.getString().startsWith("return ");
		}
	}

	private void removeCommonElements(Set<String> strings1, Set<String> strings2) {
		Set<String> intersection = new LinkedHashSet<>(strings1);
		intersection.retainAll(strings2);
		strings1.removeAll(intersection);
		strings2.removeAll(intersection);
	}

	private void removeCommonTypes(Set<String> strings1, Set<String> strings2, List<String> types1, List<String> types2) {
		if(types1.size() == types2.size()) {
			Set<String> removeFromIntersection = new LinkedHashSet<>();
			for(int i=0; i<types1.size(); i++) {
				String type1 = types1.get(i);
				String type2 = types2.get(i);
				if(!type1.equals(type2)) {
					removeFromIntersection.add(type1);
					removeFromIntersection.add(type2);
				}
			}
			Set<String> intersection = new LinkedHashSet<>(strings1);
			intersection.retainAll(strings2);
			intersection.removeAll(removeFromIntersection);
			strings1.removeAll(intersection);
			strings2.removeAll(intersection);
		}
		else {
			removeCommonElements(strings1, strings2);
		}
	}

	private String statementWithoutAnonymous(AbstractCodeFragment statement, AnonymousClassDeclarationObject anonymousClassDeclaration, UMLOperation operation) {
		int index = statement.getString().indexOf(anonymousClassDeclaration.toString());
		if(index != -1) {
			return statement.getString().substring(0, index);
		}
		else {
			for(LambdaExpressionObject lambda : statement.getLambdas()) {
				OperationBody body = lambda.getBody();
				if(body != null) {
					List<AbstractCodeFragment> leaves = body.getCompositeStatement().getLeaves();
					for(AbstractCodeFragment leaf : leaves) {
						for(AnonymousClassDeclarationObject anonymousObject : leaf.getAnonymousClassDeclarations()) {
							if(anonymousObject.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo())) {
								String statementWithoutAnonymous = statementWithoutAnonymous(leaf, anonymousClassDeclaration, operation);
								if(statementWithoutAnonymous != null) {
									return statementWithoutAnonymous;
								}
							}
						}
					}
				}
			}
			Map<String, List<ObjectCreation>> creationMap = statement.getCreationMap();
			for(String key : creationMap.keySet()) {
				List<ObjectCreation> objectCreations = creationMap.get(key);
				for(ObjectCreation creation : objectCreations) {
					if(creation.getAnonymousClassDeclaration() != null && creation.getAnonymousClassDeclaration().equals(anonymousClassDeclaration.toString()) &&
							creation.getLocationInfo().subsumes(anonymousClassDeclaration.getLocationInfo())) {
						return creation.actualString();
					}
				}
			}
			List<UMLOperation> anonymousOperations = new ArrayList<>();
			for(AnonymousClassDeclarationObject anonymousObject : statement.getAnonymousClassDeclarations()) {
				for(UMLAnonymousClass anonymousClass : operation.getAnonymousClassList()) {
					if(anonymousClass.getLocationInfo().equals(anonymousObject.getLocationInfo())) {
						anonymousOperations.addAll(anonymousClass.getOperations());
					}
				}
			}
			for(UMLOperation anonymousOperation : anonymousOperations) {
				OperationBody body = anonymousOperation.getBody();
				if(body != null) {
					List<AbstractCodeFragment> leaves = body.getCompositeStatement().getLeaves();
					for(AbstractCodeFragment leaf : leaves) {
						for(AnonymousClassDeclarationObject anonymousObject : leaf.getAnonymousClassDeclarations()) {
							if(anonymousObject.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo()) ||
									anonymousObject.getLocationInfo().subsumes(anonymousClassDeclaration.getLocationInfo())) {
								return statementWithoutAnonymous(leaf, anonymousClassDeclaration, anonymousOperation);
							}
						}
					}
				}
			}
		}
		return null;
	}

	private boolean identicalAfterVariableAndTypeReplacements(String s1, String s2, Set<Replacement> replacements) {
		String s1AfterReplacements = s1;
		for(Replacement replacement : replacements) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) || replacement.getType().equals(ReplacementType.TYPE)) {
				s1AfterReplacements = ReplacementUtil.performReplacement(s1AfterReplacements, s2, replacement.getBefore(), replacement.getAfter());
			}
		}
		return s1AfterReplacements.equals(s2);
	}

	private boolean classInstanceCreationWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo, Map<String, String> parameterToArgumentMap) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		if(string1.contains("=") && string1.endsWith(";\n") && string2.startsWith("return ") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
			String assignment1 = extractSubstringBetween(string1, "=", ";\n");
			String assignment2 = string2.substring(7, string2.lastIndexOf(";\n"));
			UMLType type1 = null, type2 = null;
			ObjectCreation objectCreation1 = null, objectCreation2 = null;
			Map<String, String> argumentToParameterMap = new LinkedHashMap<>();
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					objectCreation1 = creationMap1.get(creation1).get(0);
					type1 = objectCreation1.getType();
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					objectCreation2 = creationMap2.get(creation2).get(0);
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.getArguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.getArguments().contains(replacement.getBefore()) &&
						(objectCreation2.getArguments().contains(replacement.getAfter()) || objectCreation2.getArguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
					replacedArguments++;
				}
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			return typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement;
		}
		else if(string1.startsWith("return ") && string1.endsWith(";\n") && string2.contains("=") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
			String assignment1 = string1.substring(7, string1.lastIndexOf(";\n"));
			String assignment2 = extractSubstringBetween(string2, "=", ";\n");
			UMLType type1 = null, type2 = null;
			ObjectCreation objectCreation1 = null, objectCreation2 = null;
			Map<String, String> argumentToParameterMap = new LinkedHashMap<>();
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					objectCreation1 = creationMap1.get(creation1).get(0);
					type1 = objectCreation1.getType();
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					objectCreation2 = creationMap2.get(creation2).get(0);
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.getArguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.getArguments().contains(replacement.getBefore()) &&
						(objectCreation2.getArguments().contains(replacement.getAfter()) || objectCreation2.getArguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
					replacedArguments++;
				}
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			return typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement;
		}
		return false;
	}

	private boolean variableAssignmentWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		if(string1.contains("=") && string1.endsWith(";\n") && string2.contains("=") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, variableRename = false, classInstanceCreationReplacement = false;
			String variableName1 = string1.substring(0, string1.indexOf("="));
			String variableName2 = string2.substring(0, string2.indexOf("="));
			String assignment1 = extractSubstringBetween(string1, "=", ";\n");
			String assignment2 = extractSubstringBetween(string2, "=", ";\n");
			UMLType type1 = null, type2 = null;
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					type1 = creationMap1.get(creation1).get(0).getType();
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					type2 = creationMap2.get(creation2).get(0).getType();
				}
			}
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
			}
			AbstractCall inv1 = null, inv2 = null;
			Map<String, List<AbstractCall>> methodInvocationMap1 = statement1.getMethodInvocationMap();
			for(String invocation1 : methodInvocationMap1.keySet()) {
				if(invocation1.equals(assignment1)) {
					inv1 = methodInvocationMap1.get(invocation1).get(0);
				}
			}
			Map<String, List<AbstractCall>> methodInvocationMap2 = statement2.getMethodInvocationMap();
			for(String invocation2 : methodInvocationMap2.keySet()) {
				if(invocation2.equals(assignment2)) {
					inv2 = methodInvocationMap2.get(invocation2).get(0);
				}
			}
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
						(variableName1.equals(replacement.getBefore()) || variableName1.endsWith(" " + replacement.getBefore())) &&
						(variableName2.equals(replacement.getAfter()) || variableName2.endsWith(" " + replacement.getAfter())))
					variableRename = true;
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			if(typeReplacement && !compatibleTypes && variableRename && classInstanceCreationReplacement) {
				return true;
			}
			if(variableRename && inv1 != null && inv2 != null && inv1.differentExpressionNameAndArguments(inv2)) {
				if(inv1.getArguments().size() > inv2.getArguments().size()) {
					for(String argument : inv1.getArguments()) {
						List<AbstractCall> argumentInvocations = methodInvocationMap1.get(argument);
						if(argumentInvocations != null) {
							for(AbstractCall argumentInvocation : argumentInvocations) {
								if(!argumentInvocation.differentExpressionNameAndArguments(inv2)) {
									return false;
								}
							}
						}
					}
				}
				else if(inv1.getArguments().size() < inv2.getArguments().size()) {
					for(String argument : inv2.getArguments()) {
						List<AbstractCall> argumentInvocations = methodInvocationMap2.get(argument);
						if(argumentInvocations != null) {
							for(AbstractCall argumentInvocation : argumentInvocations) {
								if(!inv1.differentExpressionNameAndArguments(argumentInvocation)) {
									return false;
								}
							}
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	private boolean operatorExpressionWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo, Map<String, String> parameterToArgumentMap) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		List<String> operators1 = statement1.getInfixOperators();
		List<String> operators2 = statement2.getInfixOperators();
		if(operators1.size() == 1 && operators2.size() == 1) {
			String operator1 = operators1.get(0);
			String operator2 = operators2.get(0);
			int indexOfOperator1 = string1.indexOf(operator1);
			int indexOfOperator2 = string2.indexOf(operator2);
			if(indexOfOperator1 != -1 && indexOfOperator2 != -1) {
				String leftOperand1 = string1.substring(0, indexOfOperator1);
				String leftOperand2 = string2.substring(0, indexOfOperator2);
				String rightOperand1 = string1.substring(indexOfOperator1 + operator1.length());
				String rightOperand2 = string2.substring(indexOfOperator2 + operator2.length());
				boolean operatorReplacement = false;
				boolean leftOperandReplacement = false;
				boolean rightOperandReplacement = false;
				for(Replacement replacement : replacementInfo.getReplacements()) {
					
					if(parameterToArgumentMap.containsValue(replacement.getAfter())) {
						for(String key : parameterToArgumentMap.keySet()) {
							if(parameterToArgumentMap.get(key).equals(replacement.getAfter())) {
								if(leftOperand1.contains(replacement.getBefore()) && leftOperand2.contains(key)) {
									leftOperandReplacement = true;
								}
								if(rightOperand1.contains(replacement.getBefore()) && rightOperand2.contains(key)) {
									rightOperandReplacement = true;
								}
								break;
							}
						}
					}
					if(replacement.getType().equals(ReplacementType.INFIX_OPERATOR)) {
						if(replacement.getBefore().equals(operator1) && replacement.getAfter().equals(operator2)) {
							operatorReplacement = true;
						}
					}
					else if(leftOperand1.contains(replacement.getBefore()) && leftOperand2.contains(replacement.getAfter())) {
						leftOperandReplacement = true;
					}
					else if(rightOperand1.contains(replacement.getBefore()) && rightOperand2.contains(replacement.getAfter())) {
						rightOperandReplacement = true;
					}
				}
				return operatorReplacement && leftOperandReplacement && rightOperandReplacement;
			}
		}
		return false;
	}

	private boolean variableDeclarationsWithEverythingReplaced(List<VariableDeclaration> variableDeclarations1,
			List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
			boolean typeReplacement = false, variableRename = false, initializerReplacement = false, nullInitializer = false, zeroArgumentClassInstantiation = false, classInstantiationArgumentReplacement = false;
			UMLType type1 = variableDeclarations1.get(0).getType();
			UMLType type2 = variableDeclarations2.get(0).getType();
			AbstractExpression initializer1 = variableDeclarations1.get(0).getInitializer();
			AbstractExpression initializer2 = variableDeclarations2.get(0).getInitializer();
			if(initializer1 == null && initializer2 == null) {
				nullInitializer = true;
			}
			else if(initializer1 != null && initializer2 != null) {
				nullInitializer = initializer1.getExpression().equals("null") || initializer2.getExpression().equals("null");
				if(initializer1.getCreationMap().size() == 1 && initializer2.getCreationMap().size() == 1) {
					ObjectCreation creation1 = initializer1.getCreationMap().values().iterator().next().get(0);
					ObjectCreation creation2 = initializer2.getCreationMap().values().iterator().next().get(0);
					if(creation1.getArguments().size() == 0 && creation2.getArguments().size() == 0) {
						zeroArgumentClassInstantiation = true;
					}
					else if(creation1.getArguments().size() == 1 && creation2.getArguments().size() == 1) {
						String argument1 = creation1.getArguments().get(0);
						String argument2 = creation2.getArguments().get(0);
						for(Replacement replacement : replacementInfo.getReplacements()) {
							if(replacement.getBefore().equals(argument1) && replacement.getAfter().equals(argument2)) {
								classInstantiationArgumentReplacement = true;
								break;
							}
						}
					}
				}
			}
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE) &&
						variableDeclarations1.get(0).getType().toQualifiedString().equals(replacement.getBefore()) &&
						variableDeclarations2.get(0).getType().toQualifiedString().equals(replacement.getAfter()))
					typeReplacement = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
						variableDeclarations1.get(0).getVariableName().equals(replacement.getBefore()) &&
						variableDeclarations2.get(0).getVariableName().equals(replacement.getAfter()))
					variableRename = true;
				else if(initializer1 != null && initializer1.getExpression().equals(replacement.getBefore()) &&
						initializer2 != null && initializer2.getExpression().equals(replacement.getAfter())) {
					initializerReplacement = true;
				}
			}
			if(typeReplacement && !type1.compatibleTypes(type2) && variableRename && (initializerReplacement || nullInitializer || zeroArgumentClassInstantiation || classInstantiationArgumentReplacement)) {
				return true;
			}
		}
		return false;
	}

	private VariableDeclaration declarationWithArrayInitializer(List<VariableDeclaration> declarations) {
		for(VariableDeclaration declaration : declarations) {
			AbstractExpression initializer = declaration.getInitializer();
			if(initializer != null && initializer.getString().startsWith("{") && initializer.getString().endsWith("}")) {
				return declaration;
			}
		}
		return null;
	}

	private boolean argumentsWithIdenticalMethodCalls(Set<String> arguments1, Set<String> arguments2,
			Set<String> variables1, Set<String> variables2) {
		int identicalMethodCalls = 0;
		if(arguments1.size() == arguments2.size()) {
			Iterator<String> it1 = arguments1.iterator();
			Iterator<String> it2 = arguments2.iterator();
			while(it1.hasNext() && it2.hasNext()) {
				String arg1 = it1.next();
				String arg2 = it2.next();
				if(arg1.contains("(") && arg2.contains("(") && arg1.contains(")") && arg2.contains(")")) {
					int indexOfOpeningParenthesis1 = arg1.indexOf("(");
					int indexOfClosingParenthesis1 = arg1.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes1 = ReplacementUtil.isInsideSingleQuotes(arg1, indexOfOpeningParenthesis1);
					boolean openingParenthesisInsideDoubleQuotes1 = ReplacementUtil.isInsideDoubleQuotes(arg1, indexOfOpeningParenthesis1);
					boolean closingParenthesisInsideSingleQuotes1 = ReplacementUtil.isInsideSingleQuotes(arg1, indexOfClosingParenthesis1);
					boolean closingParenthesisInsideDoubleQuotes1 = ReplacementUtil.isInsideDoubleQuotes(arg1, indexOfClosingParenthesis1);
					int indexOfOpeningParenthesis2 = arg2.indexOf("(");
					int indexOfClosingParenthesis2 = arg2.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes2 = ReplacementUtil.isInsideSingleQuotes(arg2, indexOfOpeningParenthesis2);
					boolean openingParenthesisInsideDoubleQuotes2 = ReplacementUtil.isInsideDoubleQuotes(arg2, indexOfOpeningParenthesis2);
					boolean closingParenthesisInsideSingleQuotes2 = ReplacementUtil.isInsideSingleQuotes(arg2, indexOfClosingParenthesis2);
					boolean closingParenthesisInsideDoubleQuotes2 = ReplacementUtil.isInsideDoubleQuotes(arg2, indexOfClosingParenthesis2);
					if(!openingParenthesisInsideSingleQuotes1 && !closingParenthesisInsideSingleQuotes1 &&
							!openingParenthesisInsideDoubleQuotes1 && !closingParenthesisInsideDoubleQuotes1 &&
							!openingParenthesisInsideSingleQuotes2 && !closingParenthesisInsideSingleQuotes2 &&
							!openingParenthesisInsideDoubleQuotes2 && !closingParenthesisInsideDoubleQuotes2) {
						String s1 = arg1.substring(0, indexOfOpeningParenthesis1);
						String s2 = arg2.substring(0, indexOfOpeningParenthesis2);
						if(s1.equals(s2) && s1.length() > 0) {
							String args1 = arg1.substring(indexOfOpeningParenthesis1+1, indexOfClosingParenthesis1);
							String args2 = arg2.substring(indexOfOpeningParenthesis2+1, indexOfClosingParenthesis2);
							if(variables1.contains(args1) && variables2.contains(args2)) {
								identicalMethodCalls++;
							}
						}
					}
				}
			}
		}
		return identicalMethodCalls == arguments1.size() && arguments1.size() > 0;
	}

	private boolean equalAfterNewArgumentAdditions(String s1, String s2, ReplacementInfo replacementInfo) {
		if(operation1 == null && operation2 == null) {
			return false;
		}
		UMLOperationDiff operationDiff = classDiff != null ? classDiff.getOperationDiff(operation1, operation2) : null;
		if(operationDiff == null) {
			operationDiff = new UMLOperationDiff(operation1, operation2);
		}
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty() && !commonPrefix.equals("return ")) {
			int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = s1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = s2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
			if(beginIndexS1 > endIndexS1) {
				diff2 = diff2 + commonSuffix.substring(0, beginIndexS1 - endIndexS1);
				if(diff2.charAt(diff2.length()-1) == ',') {
					diff2 = diff2.substring(0, diff2.length()-1);
				}
			}
			String characterAfterCommonPrefix = s1.equals(commonPrefix) ? "" : Character.toString(s1.charAt(commonPrefix.length())); 
			if(commonPrefix.contains(",") && commonPrefix.lastIndexOf(",") < commonPrefix.length()-1 &&
					!characterAfterCommonPrefix.equals(",") && !characterAfterCommonPrefix.equals(")")) {
				String prepend = commonPrefix.substring(commonPrefix.lastIndexOf(",")+1);
				diff1 = prepend + diff1;
				diff2 = prepend + diff2;
			}
			//check for argument swap
			if(diff1.contains(",") && diff2.contains(",")) {
				String beforeComma1 = diff1.substring(0, diff1.indexOf(","));
				String afterComma1 = diff1.substring(diff1.indexOf(",") + 1);
				String beforeComma2 = diff2.substring(0, diff2.indexOf(","));
				String afterComma2 = diff2.substring(diff2.indexOf(",") + 1);
				if(beforeComma1.equals(afterComma2) && beforeComma2.equals(afterComma1)) {
					boolean conflictReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getAfter().equals(beforeComma2)) {
							conflictReplacement = true;
							break;
						}
					}
					if(!conflictReplacement) {
						SwapArgumentReplacement r = new SwapArgumentReplacement(beforeComma1, beforeComma2);
						replacementInfo.getReplacements().add(r);
						return true;
					}
				}
			}
			//if there is a variable replacement diff1 should be empty, otherwise diff1 should include a single variable
			if(diff1.isEmpty() ||
					(operation1.getParameterNameList().contains(diff1) && !operation2.getParameterNameList().contains(diff1) && !containsMethodSignatureOfAnonymousClass(diff2)) ||
					(classDiff != null && classDiff.getOriginalClass().containsAttributeWithName(diff1) && !classDiff.getNextClass().containsAttributeWithName(diff1) && !containsMethodSignatureOfAnonymousClass(diff2))) {
				List<UMLParameter> matchingAddedParameters = new ArrayList<>();
				for(UMLParameter addedParameter : operationDiff.getAddedParameters()) {
					if(diff2.contains(addedParameter.getName())) {
						matchingAddedParameters.add(addedParameter);
					}
				}
				if(matchingAddedParameters.size() > 0) {
					Replacement matchingReplacement = null;
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
							for(UMLParameterDiff parameterDiff : operationDiff.getParameterDiffList()) {
								if(parameterDiff.isNameChanged() &&
										replacement.getBefore().equals(parameterDiff.getRemovedParameter().getName()) &&
										replacement.getAfter().equals(parameterDiff.getAddedParameter().getName())) {
									matchingReplacement = replacement;
									break;
								}
							}
						}
						if(matchingReplacement != null) {
							break;
						}
					}
					if(matchingReplacement != null) {
						Set<String> splitVariables = new LinkedHashSet<>();
						splitVariables.add(matchingReplacement.getAfter());
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							splitVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().remove(matchingReplacement);
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
					else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
						Set<String> addedVariables = new LinkedHashSet<>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							addedVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						if(concat.toString().equals(diff2)) {
							AddVariableReplacement r = new AddVariableReplacement(addedVariables);
							replacementInfo.getReplacements().add(r);
							return true;
						}
					}
					if(operation1.getParameterNameList().contains(diff1)) {
						Set<String> splitVariables = new LinkedHashSet<>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							splitVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
				}
				if(classDiff != null) {
					List<UMLAttribute> matchingAttributes = new ArrayList<>();
					for(UMLAttribute attribute : classDiff.getNextClass().getAttributes()) {
						if(diff2.contains(attribute.getName())) {
							matchingAttributes.add(attribute);
						}
					}
					if(matchingAttributes.size() > 0) {
						Replacement matchingReplacement = null;
						for(Replacement replacement : replacementInfo.getReplacements()) {
							if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
								if(classDiff.getOriginalClass().containsAttributeWithName(replacement.getBefore()) &&
										classDiff.getNextClass().containsAttributeWithName(replacement.getAfter())) {
									matchingReplacement = replacement;
									break;
								}
							}
						}
						if(matchingReplacement != null) {
							Set<String> splitVariables = new LinkedHashSet<>();
							splitVariables.add(matchingReplacement.getAfter());
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								splitVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
							if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
								replacementInfo.getReplacements().remove(matchingReplacement);
								replacementInfo.getReplacements().add(split);
								return true;
							}
						}
						else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
							Set<String> addedVariables = new LinkedHashSet<>();
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								addedVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							if(concat.toString().equals(diff2)) {
								AddVariableReplacement r = new AddVariableReplacement(addedVariables);
								replacementInfo.getReplacements().add(r);
								return true;
							}
						}
						if(classDiff.getOriginalClass().containsAttributeWithName(diff1)) {
							Set<String> splitVariables = new LinkedHashSet<>();
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								splitVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
							if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
								replacementInfo.getReplacements().add(split);
								return true;
							}
						}
					}
				}
				List<VariableDeclaration> matchingVariableDeclarations = new ArrayList<>();
				for(VariableDeclaration declaration : operation2.getAllVariableDeclarations()) {
					if(diff2.contains(declaration.getVariableName())) {
						matchingVariableDeclarations.add(declaration);
					}
				}
				if(matchingVariableDeclarations.size() > 0) {
					Replacement matchingReplacement = null;
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
							int indexOf1 = s1.indexOf(replacement.getAfter());
							int indexOf2 = s2.indexOf(replacement.getAfter());
							int characterIndex1 = indexOf1 + replacement.getAfter().length();
							int characterIndex2 = indexOf2 + replacement.getAfter().length();
							boolean isVariableDeclarationReplacement =
									characterIndex1 < s1.length() && (s1.charAt(characterIndex1) == '=' || s1.charAt(characterIndex1) == '.') &&
									characterIndex2 < s2.length() && (s2.charAt(characterIndex2) == '=' || s2.charAt(characterIndex2) == '.');
							if(!isVariableDeclarationReplacement &&
									operation1.getVariableDeclaration(replacement.getBefore()) != null &&
									operation2.getVariableDeclaration(replacement.getAfter()) != null) {
								matchingReplacement = replacement;
								break;
							}
						}
					}
					if(matchingReplacement != null) {
						Set<String> splitVariables = new LinkedHashSet<>();
						splitVariables.add(matchingReplacement.getAfter());
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							splitVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().remove(matchingReplacement);
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
					else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
						Set<String> addedVariables = new LinkedHashSet<>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							addedVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						if(concat.toString().equals(diff2)) {
							AddVariableReplacement r = new AddVariableReplacement(addedVariables);
							replacementInfo.getReplacements().add(r);
							return true;
						}
					}
					if(operation1.getVariableDeclaration(diff1) != null) {
						Set<String> splitVariables = new LinkedHashSet<>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							splitVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean equalAfterArgumentMerge(String s1, String s2, ReplacementInfo replacementInfo) {
		Map<String, Set<Replacement>> commonVariableReplacementMap = new LinkedHashMap<>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				String key = replacement.getAfter();
				if(commonVariableReplacementMap.containsKey(key)) {
					commonVariableReplacementMap.get(key).add(replacement);
					int index = s1.indexOf(key);
					if(index != -1) {
						if(s1.charAt(index+key.length()) == ',') {
							s1 = s1.substring(0, index) + s1.substring(index+key.length()+1);
						}
						else if(index > 0 && s1.charAt(index-1) == ',') {
							s1 = s1.substring(0, index-1) + s1.substring(index+key.length());
						}
					}
				}
				else {
					Set<Replacement> replacements = new LinkedHashSet<>();
					replacements.add(replacement);
					commonVariableReplacementMap.put(key, replacements);
				}
			}
		}
		if(s1.equals(s2)) {
			for(String key : commonVariableReplacementMap.keySet()) {
				Set<Replacement> replacements = commonVariableReplacementMap.get(key);
				if(replacements.size() > 1) {
					replacementInfo.getReplacements().removeAll(replacements);
					Set<String> mergedVariables = new LinkedHashSet<>();
					for(Replacement replacement : replacements) {
						mergedVariables.add(replacement.getBefore());
					}
					MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
					replacementInfo.getReplacements().add(merge);
				}
			}
			return true;
		}
		return false;
	}

	private boolean identicalVariableDeclarationsWithDifferentNames(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(variableDeclarations1.size() == variableDeclarations2.size() && variableDeclarations1.size() == 1) {
			VariableDeclaration declaration1 = variableDeclarations1.get(0);
			VariableDeclaration declaration2 = variableDeclarations2.get(0);
			if(!declaration1.getVariableName().equals(declaration2.getVariableName())) {
				String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
				String composedString1 = declaration1.getType() + " " + declaration1.getVariableName() + commonSuffix;
				String composedString2 = declaration2.getType() + " " + declaration2.getVariableName() + commonSuffix;
				if(s1.equals(composedString1) && s2.equals(composedString2)) {
					Replacement replacement = new Replacement(declaration1.getVariableName(), declaration2.getVariableName(), ReplacementType.VARIABLE_NAME);
					replacementInfo.addReplacement(replacement);
					return true;
				}
			}
		}
		return false;
	}

	private boolean oneIsVariableDeclarationTheOtherIsVariableAssignment(String s1, String s2, ReplacementInfo replacementInfo) {
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(s1.contains("=") && s2.contains("=") && (s1.equals(commonSuffix) || s2.equals(commonSuffix))) {
			if(replacementInfo.getReplacements().size() == 2) {
				StringBuilder sb = new StringBuilder();
				int counter = 0;
				for(Replacement r : replacementInfo.getReplacements()) {
					sb.append(r.getAfter());
					if(counter == 0) {
						sb.append("=");
					}
					else if(counter == 1) {
						sb.append(";\n");
					}
					counter++;
				}
				return !commonSuffix.equals(sb.toString());
			}
			return true;
		}
		return false;
	}

	private boolean oneIsVariableDeclarationTheOtherIsReturnStatement(String s1, String s2) {
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonSuffix.equals("null;\n") && !commonSuffix.equals("true;\n") && !commonSuffix.equals("false;\n") && !commonSuffix.equals("0;\n")) {
			if(s1.startsWith("return ") && s1.substring(7).equals(commonSuffix) &&
					s2.contains("=") && s2.substring(s2.indexOf("=")+1).equals(commonSuffix)) {
				return true;
			}
			return s2.startsWith("return ") && s2.substring(7).equals(commonSuffix) &&
					s1.contains("=") && s1.substring(s1.indexOf("=") + 1).equals(commonSuffix);
		}
		return false;
	}

	private boolean differOnlyInFinalModifier(String s1, String s2) {
		return differOnlyInFinalModifier(s1, s2, "for(", "for(final ") ||
				differOnlyInFinalModifier(s1, s2, "catch(", "catch(final ");
	}

	private boolean differOnlyInFinalModifier(String s1, String s2, String prefixWithoutFinalModifier, String prefixWithFinalModifier) {
		if(s1.startsWith(prefixWithoutFinalModifier) && s2.startsWith(prefixWithFinalModifier)) {
			String suffix1 = s1.substring(prefixWithoutFinalModifier.length());
			String suffix2 = s2.substring(prefixWithFinalModifier.length());
			if(suffix1.equals(suffix2)) {
				return true;
			}
		}
		if(s1.startsWith(prefixWithFinalModifier) && s2.startsWith(prefixWithoutFinalModifier)) {
			String suffix1 = s1.substring(prefixWithFinalModifier.length());
			String suffix2 = s2.substring(prefixWithoutFinalModifier.length());
			return suffix1.equals(suffix2);
		}
		return false;
	}

	private boolean differOnlyInCastExpressionOrPrefixOperator(String s1, String s2, Map<String, List<? extends AbstractCall>> methodInvocationMap1, Map<String, List<? extends AbstractCall>> methodInvocationMap2, ReplacementInfo info) {
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
			int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = s1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = s2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
			if(cast(diff1, diff2)) {
				return true;
			}
			if(cast(diff2, diff1)) {
				return true;
			}
			if(diff1.isEmpty() && (diff2.equals("!") || diff2.equals("~"))) {
				Replacement r = new Replacement(s1, s2, ReplacementType.INVERT_CONDITIONAL);
				info.addReplacement(r);
				return true;
			}
			if(diff2.isEmpty() && (diff1.equals("!") || diff1.equals("~"))) {
				Replacement r = new Replacement(s1, s2, ReplacementType.INVERT_CONDITIONAL);
				info.addReplacement(r);
				return true;
			}
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(invocation1.actualString().equals(diff1) && invocation1.getArguments().contains(diff2) &&
							(invocation1.getArguments().size() == 1 || (diff2.contains("?") && diff2.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation1, Direction.INVOCATION_TO_VARIABLE);
						info.addReplacement(r);
						return true;
					}
				}
			}
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(invocation2.actualString().equals(diff2) && invocation2.getArguments().contains(diff1) &&
							(invocation2.getArguments().size() == 1 || (diff1.contains("?") && diff1.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation2, Direction.VARIABLE_TO_INVOCATION);
						info.addReplacement(r);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean cast(String diff1, String diff2) {
		return (diff1.isEmpty() && diff2.startsWith("(") && diff2.endsWith(")")) || diff2.equals("(" + diff1 + ")");
	}

	private boolean containsValidOperatorReplacements(ReplacementInfo replacementInfo) {
		List<Replacement> operatorReplacements = replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR);
		for(Replacement replacement : operatorReplacements) {
			if(replacement.getBefore().equals("==") && !replacement.getAfter().equals("!="))
				return false;
			if(replacement.getBefore().equals("!=") && !replacement.getAfter().equals("=="))
				return false;
			if(replacement.getBefore().equals("&&") && !replacement.getAfter().equals("||"))
				return false;
			if(replacement.getBefore().equals("||") && !replacement.getAfter().equals("&&"))
				return false;
		}
		return true;
	}

	private boolean commonConcat(String s1, String s2, ReplacementInfo info) {
		if(s1.contains("+") && s2.contains("+") && !s1.contains("++") && !s2.contains("++") &&
				!containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
			Set<String> tokens1 = new LinkedHashSet<>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s1)));
			Set<String> tokens2 = new LinkedHashSet<>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s2)));
			Set<String> intersection = new LinkedHashSet<>(tokens1);
			intersection.retainAll(tokens2);
			Set<String> filteredIntersection = new LinkedHashSet<>();
			for(String common : intersection) {
				boolean foundInReplacements = false;
				for(Replacement r : info.replacements) {
					if(r.getBefore().contains(common) || r.getAfter().contains(common)) {
						foundInReplacements = true;
						break;
					}
				}
				if(!foundInReplacements) {
					filteredIntersection.add(common);
				}
			}
			int size = filteredIntersection.size();
			int threshold = Math.max(tokens1.size(), tokens2.size()) - size;
			if((size > 0 && size > threshold) || (size > 1 && size >= threshold)) {
				List<String> tokens1AsList = new ArrayList<>(tokens1);
				List<String> tokens2AsList = new ArrayList<>(tokens2);
				int counter = 0;
				boolean allTokensMatchInTheSameOrder = true;
				for(String s : filteredIntersection) {
					if(!tokens1AsList.get(counter).equals(s)) {
						allTokensMatchInTheSameOrder = false;
						break;
					}
					if(!tokens2AsList.get(counter).equals(s)) {
						allTokensMatchInTheSameOrder = false;
						break;
					}
					counter++;
				}
				if(allTokensMatchInTheSameOrder && tokens1.size() == size+1 && tokens2.size() == size+1) {
					return false;
				}
				IntersectionReplacement r = new IntersectionReplacement(s1, s2, intersection, ReplacementType.CONCATENATION);
				info.getReplacements().add(r);
				return true;
			}
		}
		return false;
	}

	private boolean commonConditional(String s1, String s2, ReplacementInfo info) {
		if(!containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
			if((s1.contains("||") || s1.contains("&&") || s2.contains("||") || s2.contains("&&"))) {
				String conditional1 = prepareConditional(s1);
				String conditional2 = prepareConditional(s2);
				String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
				String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
				List<String> subConditionsAsList1 = new ArrayList<>();
				for(String s : subConditions1) {
					subConditionsAsList1.add(s.trim());
				}
				List<String> subConditionsAsList2 = new ArrayList<>();
				for(String s : subConditions2) {
					subConditionsAsList2.add(s.trim());
				}
				Set<String> intersection = new LinkedHashSet<>(subConditionsAsList1);
				intersection.retainAll(subConditionsAsList2);
				int matches = 0;
				if(!intersection.isEmpty()) {
					for(String element : intersection) {
						boolean replacementFound = false;
						for(Replacement r : info.getReplacements()) {
							if(element.equals(r.getAfter()) || element.equals("(" + r.getAfter()) || element.equals(r.getAfter() + ")")) {
								replacementFound = true;
								break;
							}
							if(element.equals("!" + r.getAfter())) {
								replacementFound = true;
								break;
							}
							if(r.getType().equals(ReplacementType.INFIX_OPERATOR) && element.contains(r.getAfter())) {
								replacementFound = true;
								break;
							}
							if(ReplacementUtil.contains(element, r.getAfter()) && element.startsWith(r.getAfter()) &&
									(element.endsWith(" != null") || element.endsWith(" == null"))) {
								replacementFound = true;
								break;
							}
						}
						if(!replacementFound) {
							matches++;
						}
					}
				}
				if(matches > 0) {
					Replacement r = new IntersectionReplacement(s1, s2, intersection, ReplacementType.CONDITIONAL);
					info.addReplacement(r);
				}
				boolean invertConditionalFound = false;
				for(String subCondition1 : subConditionsAsList1) {
					for(String subCondition2 : subConditionsAsList2) {
						if(subCondition1.equals("!" + subCondition2)) {
							Replacement r = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
							info.addReplacement(r);
							invertConditionalFound = true;
						}
						if(subCondition2.equals("!" + subCondition1)) {
							Replacement r = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
							info.addReplacement(r);
							invertConditionalFound = true;
						}
					}
				}
				if(invertConditionalFound || matches > 0) {
					return true;
				}
			}
			if(s1.contains(" >= ") && s2.contains(" <= ")) {
				Replacement r = invertConditionalDirection(s1, s2, " >= ", " <= ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" <= ") && s2.contains(" >= ")) {
				Replacement r = invertConditionalDirection(s1, s2, " <= ", " >= ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" > ") && s2.contains(" < ")) {
				Replacement r = invertConditionalDirection(s1, s2, " > ", " < ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" < ") && s2.contains(" > ")) {
				Replacement r = invertConditionalDirection(s1, s2, " < ", " > ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
		}
		return false;
	}

	private Replacement invertConditionalDirection(String s1, String s2, String operator1, String operator2) {
		int indexS1 = s1.indexOf(operator1);
		int indexS2 = s2.indexOf(operator2);
		//s1 goes right, s2 goes left
		int i = indexS1 + operator1.length();
		int j = indexS2 - 1;
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		while(i < s1.length() && j >= 0) {
			sb1.append(s1.charAt(i));
			sb2.insert(0, s2.charAt(j));
			if(sb1.toString().equals(sb2.toString())) {
				String subCondition1 = operator1 + sb1;
				String subCondition2 = sb2 + operator2;
				return new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
			}
			i++;
			j--;
		}
		//s1 goes left, s2 goes right
		i = indexS1 - 1;
		j = indexS2 + operator2.length();
		sb1 = new StringBuilder();
		sb2 = new StringBuilder();
		while(i >= 0 && j < s2.length()) {
			sb1.insert(0, s1.charAt(i));
			sb2.append(s2.charAt(j));
			if(sb1.toString().equals(sb2.toString())) {
				String subCondition1 = sb1 + operator1;
				String subCondition2 = operator2 + sb2;
				return new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
			}
			i--;
			j++;
		}
		return null;
	}

	private String prepareConditional(String s) {
		String conditional = s;
		if(s.startsWith("if(") && s.endsWith(")")) {
			conditional = s.substring(3, s.length()-1);
		}
		if(s.startsWith("while(") && s.endsWith(")")) {
			conditional = s.substring(6, s.length()-1);
		}
		if(s.startsWith("return ") && s.endsWith(";\n")) {
			conditional = s.substring(7, s.length()-2);
		}
		int indexOfEquals = s.indexOf("=");
		if(indexOfEquals > -1 && s.charAt(indexOfEquals+1) != '=' && s.charAt(indexOfEquals-1) != '!' && s.endsWith(";\n")) {
			conditional = s.substring(indexOfEquals+1, s.length()-2);
		}
		return conditional;
	}

	private void replaceVariablesWithArguments(Set<String> variables, Map<String, String> parameterToArgumentMap) {
		for(String parameter : parameterToArgumentMap.keySet()) {
			String argument = parameterToArgumentMap.get(parameter);
			if(variables.contains(parameter)) {
				variables.add(argument);
				if(argument.contains("(") && argument.contains(")")) {
					int indexOfOpeningParenthesis = argument.indexOf("(");
					int indexOfClosingParenthesis = argument.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisInsideSingleQuotes = ReplacementUtil.isInsideSingleQuotes(argument, indexOfClosingParenthesis);
					boolean openingParenthesisInsideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfOpeningParenthesis);
					boolean closingParenthesisIndideDoubleQuotes = ReplacementUtil.isInsideDoubleQuotes(argument, indexOfClosingParenthesis);
					if(indexOfOpeningParenthesis < indexOfClosingParenthesis &&
							!openingParenthesisInsideSingleQuotes && !closingParenthesisInsideSingleQuotes &&
							!openingParenthesisInsideDoubleQuotes && !closingParenthesisIndideDoubleQuotes) {
						String arguments = argument.substring(indexOfOpeningParenthesis+1, indexOfClosingParenthesis);
						if(!arguments.isEmpty() && !arguments.contains(",") && !arguments.contains("(") && !arguments.contains(")")) {
							variables.add(arguments);
						}
					}
				}
			}
		}
	}

	private boolean isCallChain(Collection<List<? extends AbstractCall>> calls) {
		if(calls.size() > 1) {
			AbstractCall previous;
			AbstractCall current = null;
			int chainLength = 0;
			for(List<? extends AbstractCall> list : calls) {
				previous = current;
				current = list.get(0);
				if(current != null && previous != null) {
					if(previous.getExpression() != null && previous.getExpression().equals(current.actualString())) {
						chainLength++;
					}
					else {
						return false;
					}
				}
			}
			return chainLength == calls.size() - 1;
		}
		return false;
	}

	private void replaceVariablesWithArguments(Map<String, List<? extends AbstractCall>> callMap,
			Set<String> calls, Map<String, String> parameterToArgumentMap) {
		if(isCallChain(callMap.values())) {
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					calls.addAll(toBeAdded);
				}
			}
		}
		else {
			Set<String> finalNewCalls = new LinkedHashSet<>();
			for(String parameter : parameterToArgumentMap.keySet()) {
				String argument = parameterToArgumentMap.get(parameter);
				if(!parameter.equals(argument)) {
					Set<String> toBeAdded = new LinkedHashSet<>();
					for(String call : calls) {
						String afterReplacement = ReplacementUtil.performArgumentReplacement(call, parameter, argument);
						if(!call.equals(afterReplacement)) {
							toBeAdded.add(afterReplacement);
							List<? extends AbstractCall> oldCalls = callMap.get(call);
							List<AbstractCall> newCalls = new ArrayList<>();
							for(AbstractCall oldCall : oldCalls) {
								AbstractCall newCall = oldCall.update(parameter, argument);
								newCalls.add(newCall);
							}
							callMap.put(afterReplacement, newCalls);
						}
					}
					finalNewCalls.addAll(toBeAdded);
				}
			}
			calls.addAll(finalNewCalls);
		}
	}

	private void findReplacements(Set<String> strings1, Set<String> strings2, ReplacementInfo replacementInfo, ReplacementType type) throws RefactoringMinerTimedOutException {
		if(strings1.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS || strings2.size() > MAXIMUM_NUMBER_OF_COMPARED_STRINGS) {
			return;
		}
		TreeMap<Double, Set<Replacement>> globalReplacementMap = new TreeMap<>();
		TreeMap<Double, Set<Replacement>> replacementCache = new TreeMap<>();
		if(strings1.size() <= strings2.size()) {
			for(String s1 : strings1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
				for(String s2 : strings2) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							operation1 != null && operation2 != null &&
							operation1.getVariableDeclaration(s1) == null && operation2.getVariableDeclaration(s2) == null) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(distancenormalized == 0) {
						break;
					}
				}
			}
		}
		else {
			for(String s2 : strings2) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<>();
				for(String s1 : strings1) {
					if(Thread.interrupted()) {
						throw new RefactoringMinerTimedOutException();
					}
					boolean containsMethodSignatureOfAnonymousClass1 = containsMethodSignatureOfAnonymousClass(s1);
					boolean containsMethodSignatureOfAnonymousClass2 = containsMethodSignatureOfAnonymousClass(s2);
					if(containsMethodSignatureOfAnonymousClass1 != containsMethodSignatureOfAnonymousClass2 &&
							operation1 != null && operation2 != null &&
							operation1.getVariableDeclaration(s1) == null && operation2.getVariableDeclaration(s2) == null) {
						continue;
					}
					String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), s1, s2);
					int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
					if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
						Replacement replacement = new Replacement(s1, s2, type);
						double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), replacementInfo.getArgumentizedString2().length());
						replacementMap.put(distancenormalized, replacement);
						if(replacementCache.containsKey(distancenormalized)) {
							replacementCache.get(distancenormalized).add(replacement);
						}
						else {
							Set<Replacement> r = new LinkedHashSet<>();
							r.add(replacement);
							replacementCache.put(distancenormalized, r);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					Double distancenormalized = replacementMap.firstEntry().getKey();
					Replacement replacement = replacementMap.firstEntry().getValue();
					if(globalReplacementMap.containsKey(distancenormalized)) {
						globalReplacementMap.get(distancenormalized).add(replacement);
					}
					else {
						Set<Replacement> r = new LinkedHashSet<>();
						r.add(replacement);
						globalReplacementMap.put(distancenormalized, r);
					}
					if(replacementMap.firstEntry().getKey() == 0) {
						break;
					}
				}
			}
		}
		if(!globalReplacementMap.isEmpty()) {
			Double distancenormalized = globalReplacementMap.firstEntry().getKey();
			if(distancenormalized == 0) {
				Set<Replacement> replacements = globalReplacementMap.firstEntry().getValue();
				for(Replacement replacement : replacements) {
					replacementInfo.addReplacement(replacement);
					replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
				}
			}
			else {
				Set<Replacement> conflictingReplacements = conflictingReplacements(globalReplacementMap);
				Set<String> processedBefores = new LinkedHashSet<>();
				for(Set<Replacement> replacements : globalReplacementMap.values()) {
					for(Replacement replacement : replacements) {
						if(!conflictingReplacements.contains(replacement)) {
							if(!processedBefores.contains(replacement.getBefore())) {
								replacementInfo.addReplacement(replacement);
								replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement.getBefore(), replacement.getAfter()));
								processedBefores.add(replacement.getBefore());
							}
							else {
								//find the next best match for replacement.getAfter() from the replacement cache
								for(Set<Replacement> replacements2 : replacementCache.values()) {
									boolean found = false;
									for(Replacement replacement2 : replacements2) {
										if(replacement2.getAfter().equals(replacement.getAfter()) && !replacement2.equals(replacement)) {
											replacementInfo.addReplacement(replacement2);
											replacementInfo.setArgumentizedString1(ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), replacementInfo.getArgumentizedString2(), replacement2.getBefore(), replacement2.getAfter()));
											processedBefores.add(replacement2.getBefore());
											found = true;
											break;
										}
									}
									if(found) {
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private Set<Replacement> conflictingReplacements(TreeMap<Double, Set<Replacement>> globalReplacementMap) {
		Map<String, Set<Replacement>> map = new LinkedHashMap<>();
		for(Set<Replacement> replacements : globalReplacementMap.values()) {
			for(Replacement replacement : replacements) {
				String after = replacement.getAfter();
				if(map.containsKey(after)) {
					map.get(after).add(replacement);
				}
				else {
					Set<Replacement> set = new LinkedHashSet<>();
					set.add(replacement);
					map.put(after, set);
				}
			}
		}
		Set<Replacement> conflictingReplacements = new LinkedHashSet<>();
		for(String key : map.keySet()) {
			Set<Replacement> replacements = map.get(key);
			if(replacements.size() > 1) {
				conflictingReplacements.add(replacements.iterator().next());
			}
		}
		return conflictingReplacements;
	}

	private Set<Replacement> variableReplacementWithinMethodInvocations(String s1, String s2, Set<String> variables1, Set<String> variables2) {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(String variable1 : variables1) {
			if(s1.contains(variable1) && !s1.equals(variable1) && !s1.equals("this." + variable1) && !s1.equals("_" + variable1)) {
				int startIndex1 = s1.indexOf(variable1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + variable1.length());
				for(String variable2 : variables2) {
					if(variable2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						variable2 = variable2.substring(0, variable2.indexOf(substringAfterIndex1));
					}
					if(s2.contains(variable2) && !s2.equals(variable2)) {
						int startIndex2 = s2.indexOf(variable2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + variable2.length(), s2.length());
						boolean suffixMatch = false;
						if(substringAfterIndex1.equals(substringAfterIndex2)) {
							suffixMatch = true;
						}
						else {
							String variableContainedInSubstringAfterIndex1 = null;
							for(String var1 : variables1) {
								if(substringAfterIndex1.contains(var1)) {
									variableContainedInSubstringAfterIndex1 = var1;
									break;
								}
							}
							String variableContainedInSubstringAfterIndex2 = null;
							for(String var2 : variables2) {
								if(substringAfterIndex2.contains(var2)) {
									variableContainedInSubstringAfterIndex2 = var2;
									break;
								}
							}
							if(variableContainedInSubstringAfterIndex1 != null && variableContainedInSubstringAfterIndex2 != null) {
								Set<Replacement> r = variableReplacementWithinMethodInvocations(substringAfterIndex1, substringAfterIndex2, variables1, variables2);
								if(!r.isEmpty()) {
									suffixMatch = true;
								}
							}
						}
						boolean prefixMatch = false;
						if(substringBeforeIndex1.equals(substringBeforeIndex2)) {
							prefixMatch = true;
						}
						else {
							String variableContainedInSubstringBeforeIndex1 = null;
							for(String var1 : variables1) {
								if(substringBeforeIndex1.contains(var1)) {
									variableContainedInSubstringBeforeIndex1 = var1;
									break;
								}
							}
							String variableContainedInSubstringBeforeIndex2 = null;
							for(String var2 : variables2) {
								if(substringBeforeIndex2.contains(var2)) {
									variableContainedInSubstringBeforeIndex2 = var2;
									break;
								}
							}
							if(variableContainedInSubstringBeforeIndex1 != null && variableContainedInSubstringBeforeIndex2 != null) {
								Set<Replacement> r = variableReplacementWithinMethodInvocations(substringBeforeIndex1, substringBeforeIndex2, variables1, variables2);
								if(!r.isEmpty()) {
									prefixMatch = true;
								}
							}
						}
						if(prefixMatch && suffixMatch) {
							replacements.add(new Replacement(variable1, variable2, ReplacementType.VARIABLE_NAME));
						}
					}
				}
			}
		}
		return replacements;
	}

	private Set<Replacement> replacementsWithinMethodInvocations(String s1, String s2, Set<String> set1, Set<String> set2, Map<String, List<? extends AbstractCall>> methodInvocationMap, Direction direction) {
		Set<Replacement> replacements = new LinkedHashSet<>();
		for(String element1 : set1) {
			if(s1.contains(element1) && !s1.equals(element1) && !s1.equals("this." + element1) && !s1.equals("_" + element1)) {
				int startIndex1 = s1.indexOf(element1);
				String substringBeforeIndex1 = s1.substring(0, startIndex1);
				String substringAfterIndex1 = s1.substring(startIndex1 + element1.length());
				for(String element2 : set2) {
					if(element2.endsWith(substringAfterIndex1) && substringAfterIndex1.length() > 1) {
						element2 = element2.substring(0, element2.indexOf(substringAfterIndex1));
					}
					if(s2.contains(element2) && !s2.equals(element2)) {
						int startIndex2 = s2.indexOf(element2);
						String substringBeforeIndex2 = s2.substring(0, startIndex2);
						String substringAfterIndex2 = s2.substring(startIndex2 + element2.length());
						List<? extends AbstractCall> methodInvocationList = null;
						if(direction.equals(Direction.VARIABLE_TO_INVOCATION))
							methodInvocationList = methodInvocationMap.get(element2);
						else if(direction.equals(Direction.INVOCATION_TO_VARIABLE))
							methodInvocationList = methodInvocationMap.get(element1);
						if(substringBeforeIndex1.equals(substringBeforeIndex2) && !substringAfterIndex1.isEmpty() && !substringAfterIndex2.isEmpty() && methodInvocationList != null) {
							Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
							replacements.add(r);
						}
						else if(substringAfterIndex1.equals(substringAfterIndex2) && !substringBeforeIndex1.isEmpty() && !substringBeforeIndex2.isEmpty() && methodInvocationList != null) {
							Replacement r = new VariableReplacementWithMethodInvocation(element1, element2, methodInvocationList.get(0), direction);
							replacements.add(r);
						}
					}
				}
			}
		}
		return replacements;
	}

	public static boolean containsMethodSignatureOfAnonymousClass(String s) {
		String[] lines = s.split("\\n");
		if(s.contains(" -> ") && lines.length > 1) {
			return true;
		}
		for(String line : lines) {
			line = VariableReplacementAnalysis.prepareLine(line);
			if(SubMethodNodeVisitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}

	private boolean variablesStartWithSameCase(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo replacementInfo) {
		if(parameterToArgumentMap.containsValue(s2)) {
			return true;
		}
		if(s1.length() > 0 && s2.length() > 0) {
			if(Character.isUpperCase(s1.charAt(0)) && Character.isUpperCase(s2.charAt(0)))
				return true;
			if(Character.isLowerCase(s1.charAt(0)) && Character.isLowerCase(s2.charAt(0)))
				return true;
			if(s1.charAt(0) == '_' || s2.charAt(0) == '_')
				return true;
			if(s1.charAt(0) == '(' || s2.charAt(0) == '(')
				return true;
			if((s1.contains(".") || s2.contains(".")) && !replacementInfo.argumentizedString1.equals("return " + s1 + ";\n") &&
					!replacementInfo.argumentizedString2.equals("return " + s2 + ";\n"))
				return true;
			if(s1.equalsIgnoreCase(s2))
				return true;
			if(replacementInfo.argumentizedString1.startsWith(s1 + "=") && replacementInfo.argumentizedString2.startsWith(s2 + "=")) {
				String suffix1 = replacementInfo.argumentizedString1.substring(s1.length(), replacementInfo.argumentizedString1.length());
				String suffix2 = replacementInfo.argumentizedString2.substring(s2.length(), replacementInfo.argumentizedString2.length());
				if(suffix1.equals(suffix2)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return getNonMappedLeavesT1().isEmpty() && getNonMappedInnerNodesT1().isEmpty() &&
				getNonMappedLeavesT2().isEmpty() && getNonMappedInnerNodesT2().isEmpty();
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLOperationBodyMapper) {
    		UMLOperationBodyMapper other = (UMLOperationBodyMapper)o;
    		return this.operation1.equals(other.operation1) && this.operation2.equals(other.operation2);
    	}
    	return false;
	}

	public String toString() {
		if(operation1 != null && operation2 != null)
			return operation1.toString() + " -> " + operation2.toString();
		else if(attribute1 != null && attribute2 != null)
			return attribute1.toString() + " -> " + attribute2.toString();
		return null;
	}

	@Override
	public int compareTo(UMLOperationBodyMapper operationBodyMapper) {
		boolean identicalStringRepresentation1 = this.identicalBody();
		boolean identicalStringRepresentation2 = operationBodyMapper.identicalBody();
		if(identicalStringRepresentation1 != identicalStringRepresentation2) {
			if(identicalStringRepresentation1) {
				return -1;
			}
			else if(identicalStringRepresentation2) {
				return 1;
			}
		}
		int thisMappings = this.mappingsWithoutBlocks();
		for(AbstractCodeMapping mapping : this.getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				thisMappings++;
			}
		}
		int otherMappings = operationBodyMapper.mappingsWithoutBlocks();
		for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
			if(mapping.isIdenticalWithExtractedVariable() || mapping.isIdenticalWithInlinedVariable()) {
				otherMappings++;
			}
		}
		if(thisMappings != otherMappings) {
			return -Integer.compare(thisMappings, otherMappings);
		}
		else {
			int thisExactMatches = this.exactMatches();
			int otherExactMatches = operationBodyMapper.exactMatches();
			if(thisExactMatches != otherExactMatches) {
				return -Integer.compare(thisExactMatches, otherExactMatches);
			}
			else {
				int thisNonMapped = this.nonMappedElementsT2();
				int otherNonMapped = operationBodyMapper.nonMappedElementsT2();
				if(thisNonMapped != otherNonMapped) {
					return Integer.compare(thisNonMapped, otherNonMapped);
				}
				else {
					int thisEditDistance = this.editDistance();
					int otherEditDistance = operationBodyMapper.editDistance();
					if(thisEditDistance != otherEditDistance) {
						return Integer.compare(thisEditDistance, otherEditDistance);
					}
					else {
						int thisOperationNameEditDistance = this.operationNameEditDistance();
						int otherOperationNameEditDistance = operationBodyMapper.operationNameEditDistance();
						return Integer.compare(thisOperationNameEditDistance, otherOperationNameEditDistance);
					}
				}
			}
		}
	}

	private boolean identicalBody() {
		if(this.operation1 != null && this.operation2 != null) {
			OperationBody body1 = operation1.getBody();
			OperationBody body2 = operation2.getBody();
			if(body1 != null && body2 != null) {
				return body1.getBodyHashCode() == body2.getBodyHashCode();
			}
		}
		return false;
	}

	private boolean existsVariableDeclarationForV2InitializedWithV1(VariableDeclaration v1, VariableDeclaration v2, ReplacementInfo info) {
		for(AbstractCodeFragment fragment2 : info.statements2) {
			if(fragment2.getVariableDeclarations().contains(v2)) {
				AbstractExpression initializer = v2.getInitializer();
				if(initializer != null && initializer.getVariables().contains(v1.getVariableName())) {
					return true;
				}
				
			}
			if(fragment2.getString().equals(v2.getVariableName() + "=" + v1.getVariableName() + ";\n")) {
				return true;
			}
			VariableDeclaration v1DeclarationInFragment2 = fragment2.getVariableDeclaration(v1.getVariableName());
			if(v1DeclarationInFragment2 != null) {
				AbstractExpression initializer = v1DeclarationInFragment2.getInitializer();
				if(initializer != null && initializer.getVariables().contains(v2.getVariableName())) {
					return true;
				}
			}
			if(fragment2.getString().equals(v1.getVariableName() + "=" + v2.getVariableName() + ";\n")) {
				return true;
			}
		}
		return false;
	}

	private int inconsistentVariableMappingCount(AbstractCodeFragment statement1, AbstractCodeFragment statement2, VariableDeclaration v1, VariableDeclaration v2) {
		int count = 0;
		if(v1 != null && v2 != null) {
			boolean variableDeclarationMismatch = false;
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1) &&
						variableDeclarations2.size() > 0 &&
						!variableDeclarations2.contains(v2)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(variableDeclarations2.contains(v2) &&
						variableDeclarations1.size() > 0 &&
						!variableDeclarations1.contains(v1)) {
					variableDeclarationMismatch = true;
					count++;
				}
				if(mapping.isExact()) {
					boolean containsMapping = true;
					if(statement1 instanceof CompositeStatementObject && statement2 instanceof CompositeStatementObject &&
							statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT)) {
						CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
						CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
						containsMapping = comp1.contains(mapping.getFragment1()) && comp2.contains(mapping.getFragment2());
					}
					if(containsMapping) {
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v1, mapping)) {
							VariableDeclaration otherV1 = mapping.getFragment1().getVariableDeclaration(v1.getVariableName());
							if(otherV1 != null) {
								VariableScope otherV1Scope = otherV1.getScope();
								VariableScope v1Scope = v1.getScope();
								if(otherV1Scope.overlaps(v1Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
						if(VariableReplacementAnalysis.bothFragmentsUseVariable(v2, mapping)) {
							VariableDeclaration otherV2 = mapping.getFragment2().getVariableDeclaration(v2.getVariableName());
							if(otherV2 != null) {
								VariableScope otherV2Scope = otherV2.getScope();
								VariableScope v2Scope = v2.getScope();
								if(otherV2Scope.overlaps(v2Scope)) {
									count++;
								}
							}
							else {
								count++;
							}
						}
					}
				}
				else if(variableDeclarationMismatch && !variableDeclarations1.contains(v1) && !variableDeclarations2.contains(v2)) {
					for(Replacement r : mapping.getReplacements()) {
						if(r.getBefore().equals(v1.getVariableName()) && !r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
						else if(!r.getBefore().equals(v1.getVariableName()) && r.getAfter().equals(v2.getVariableName())) {
							count++;
						}
					}
				}
			}
		}
		return count;
	}

	public boolean containsExtractOperationRefactoring(UMLOperation extractedOperation) {
		if(classDiff != null) {
			if(classDiff.containsExtractOperationRefactoring(operation1, extractedOperation)) {
				return true;
			}
		}
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractOperationRefactoring) {
				ExtractOperationRefactoring extractRef = (ExtractOperationRefactoring)ref;
				if(extractRef.getExtractedOperation().equals(extractedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsInlineOperationRefactoring(UMLOperation inlinedOperation) {
		if(classDiff != null) {
			if(classDiff.containsInlineOperationRefactoring(inlinedOperation, operation2)) {
				return true;
			}
		}
		for(Refactoring ref : refactorings) {
			if(ref instanceof InlineOperationRefactoring) {
				InlineOperationRefactoring inlineRef = (InlineOperationRefactoring)ref;
				if(inlineRef.getInlinedOperation().equals(inlinedOperation)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<CompositeStatementObject> getNestedTryCatch(List<AbstractStatement> compStatements) {
		List<CompositeStatementObject> nestedTryCatch = new ArrayList<CompositeStatementObject>();
		for(AbstractStatement statement : compStatements) {
			if(statement.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) ||
					statement.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				nestedTryCatch.add((CompositeStatementObject)statement);
			}
		}
		return nestedTryCatch;
	}

	private double compositeChildMatchingScore(CompositeStatementObject comp1, CompositeStatementObject comp2, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations) {
		List<AbstractStatement> compStatements1 = comp1.getStatements();
		List<AbstractStatement> compStatements2 = comp2.getStatements();
		int childrenSize1 = compStatements1.size();
		int childrenSize2 = compStatements2.size();
		List<CompositeStatementObject> nestedTryCatch1 = getNestedTryCatch(compStatements1);
		List<CompositeStatementObject> nestedTryCatch2 = getNestedTryCatch(compStatements2);

		if(parentMapper != null && comp1.getLocationInfo().getCodeElementType().equals(comp2.getLocationInfo().getCodeElementType()) &&
				childrenSize1 == 1 && childrenSize2 == 1 && !comp1.getString().equals("{") && !comp2.getString().equals("{")) {
			if(compStatements1.get(0).getString().equals("{") && !compStatements2.get(0).getString().equals("{")) {
				CompositeStatementObject block = (CompositeStatementObject)compStatements1.get(0);
				compStatements1.addAll(block.getStatements());
			}
			if(!compStatements1.get(0).getString().equals("{") && compStatements2.get(0).getString().equals("{")) {
				CompositeStatementObject block = (CompositeStatementObject)compStatements2.get(0);
				compStatements2.addAll(block.getStatements());
			}
		}
		int mappedChildrenSize = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(compStatements1.contains(mapping.getFragment1()) && compStatements2.contains(mapping.getFragment2())) {
				mappedChildrenSize++;
			}
		}
		if(mappedChildrenSize == 0) {
			List<AbstractCodeFragment> leaves1 = comp1.getLeaves();
			List<AbstractCodeFragment> leaves2 = comp2.getLeaves();
			int leaveSize1 = leaves1.size();
			int leaveSize2 = leaves2.size();
			int mappedLeavesSize = 0;
			for(AbstractCodeMapping mapping : mappings) {
				if(leaves1.contains(mapping.getFragment1()) && leaves2.contains(mapping.getFragment2())) {
					boolean mappingUnderNestedTryCatch = false;
					if(nestedTryCatch1.isEmpty() && !nestedTryCatch2.isEmpty()) {
						for(CompositeStatementObject statement : nestedTryCatch2) {
							List<AbstractStatement> directlyNestedStatements = statement.getStatements();
							if(directlyNestedStatements.contains(mapping.getFragment2())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
							List<AbstractCodeFragment> leaves = statement.getLeaves();
							if(leaves.contains(mapping.getFragment2())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
						}
					}
					else if(!nestedTryCatch1.isEmpty() && nestedTryCatch2.isEmpty()) {
						for(CompositeStatementObject statement : nestedTryCatch1) {
							List<AbstractStatement> directlyNestedStatements = statement.getStatements();
							if(directlyNestedStatements.contains(mapping.getFragment1())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
							List<AbstractCodeFragment> leaves = statement.getLeaves();
							if(leaves.contains(mapping.getFragment1())) {
								mappingUnderNestedTryCatch = true;
								break;
							}
						}
					}
					if(!mappingUnderNestedTryCatch) {
						mappedLeavesSize++;
					}
				}
			}
			if(mappedLeavesSize == 0) {
				//check for possible extract or inline
				if(leaveSize2 <= 2) {
					for(AbstractCodeFragment leaf2 : leaves2) {
						AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
						if(invocation == null) {
							invocation = leaf2.assignmentInvocationCoveringEntireStatement();
						}
						if(invocation != null && matchesOperation(invocation, addedOperations, operation2)) {
							mappedLeavesSize++;
						}
						if(invocation != null && invocation.actualString().contains(" -> ")) {
							for(LambdaExpressionObject lambda : leaf2.getLambdas()) {
								if(lambda.getBody() != null) {
									for(AbstractCall inv : lambda.getBody().getAllOperationInvocations()) {
										if(matchesOperation(inv, addedOperations, operation2)) {
											mappedLeavesSize++;
										}
									}
								}
								else if(lambda.getExpression() != null) {
									Map<String, List<AbstractCall>> methodInvocationMap = lambda.getExpression().getMethodInvocationMap();
									for(String key : methodInvocationMap.keySet()) {
										List<AbstractCall> invocations = methodInvocationMap.get(key);
										for(AbstractCall inv : invocations) {
											if(matchesOperation(inv, addedOperations, operation2)) {
												mappedLeavesSize++;
											}
										}
									}
								}
							}
						}
					}
				}
				else if(leaveSize1 <= 2) {
					for(AbstractCodeFragment leaf1 : leaves1) {
						AbstractCall invocation = leaf1.invocationCoveringEntireFragment();
						if(invocation == null) {
							invocation = leaf1.assignmentInvocationCoveringEntireStatement();
						}
						if(invocation != null && matchesOperation(invocation, removedOperations, operation1)) {
							mappedLeavesSize++;
						}
						if(invocation != null && invocation.actualString().contains(" -> ")) {
							for(LambdaExpressionObject lambda : leaf1.getLambdas()) {
								if(lambda.getBody() != null) {
									for(AbstractCall inv : lambda.getBody().getAllOperationInvocations()) {
										if(matchesOperation(inv, removedOperations, operation1)) {
											mappedLeavesSize++;
										}
									}
								}
								else if(lambda.getExpression() != null) {
									Map<String, List<AbstractCall>> methodInvocationMap = lambda.getExpression().getMethodInvocationMap();
									for(String key : methodInvocationMap.keySet()) {
										List<AbstractCall> invocations = methodInvocationMap.get(key);
										for(AbstractCall inv : invocations) {
											if(matchesOperation(inv, removedOperations, operation1)) {
												mappedLeavesSize++;
											}
										}
									}
								}
							}
						}
					}
				}
				if(leaveSize1 == 1 && leaveSize2 == 1 && leaves1.get(0).getString().equals("continue;\n") && leaves2.get(0).getString().equals("return null;\n")) {
					mappedLeavesSize++;
				}
				if(leaveSize1 == 2 && leaveSize2 == 1 && !leaves1.get(0).getString().equals("break;\n") && leaves1.get(1).getString().equals("break;\n") && leaves2.get(0).getString().startsWith("return ")) {
					mappedLeavesSize++;
				}
			}
			int max = Math.max(leaveSize1, leaveSize2);
			if(max == 0) {
				return 0;
			}
			else {
				if(mappedLeavesSize > 0) {
					return (double)mappedLeavesSize/(double)max;
				}
				if(comp1.getString().equals(comp2.getString()) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.BLOCK) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.SYNCHRONIZED_STATEMENT) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.TRY_STATEMENT) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
						!comp1.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) &&
						!parentMapperContainsExactMapping(comp1)) {
					return 1;
				}
			}
		}
		
		int max = Math.max(childrenSize1, childrenSize2);
		if(max == 0)
			return 0;
		else
			return (double)mappedChildrenSize/(double)max;
	}

	private boolean parentMapperContainsExactMapping(AbstractStatement statement) {
		if(parentMapper != null) {
			for(AbstractCodeMapping mapping : parentMapper.mappings) {
				AbstractCodeFragment fragment1 = mapping.getFragment1();
				AbstractCodeFragment fragment2 = mapping.getFragment2();
				if(fragment1.equals(statement) || fragment2.equals(statement)) {
					if(fragment1.getString().equals(fragment2.getString())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private double compositeChildMatchingScore(TryStatementObject try1, TryStatementObject try2, Set<AbstractCodeMapping> mappings,
			List<UMLOperation> removedOperations, List<UMLOperation> addedOperations, boolean tryWithResourceMigration) {
		double score = compositeChildMatchingScore(try1, try2, mappings, removedOperations, addedOperations);
		if(!tryWithResourceMigration) {
			List<CompositeStatementObject> catchClauses1 = try1.getCatchClauses();
			List<CompositeStatementObject> catchClauses2 = try2.getCatchClauses();
			if(catchClauses1.size() == catchClauses2.size()) {
				for(int i=0; i<catchClauses1.size(); i++) {
					double tmpScore = compositeChildMatchingScore(catchClauses1.get(i), catchClauses2.get(i), mappings, removedOperations, addedOperations);
					if(tmpScore == 1) {
						score += tmpScore;
					}
				}
			}
			if(try1.getFinallyClause() != null && try2.getFinallyClause() != null) {
				double tmpScore = compositeChildMatchingScore(try1.getFinallyClause(), try2.getFinallyClause(), mappings, removedOperations, addedOperations);
				if(tmpScore == 1) {
					score += tmpScore;
				}
			}
		}
		return score;
	}

	private boolean matchesOperation(AbstractCall invocation, List<UMLOperation> operations, UMLOperation callerOperation) {
		for(UMLOperation operation : operations) {
			if(invocation.matchesOperation(operation, callerOperation, modelDiff))
				return true;
		}
		return false;
	}
}
