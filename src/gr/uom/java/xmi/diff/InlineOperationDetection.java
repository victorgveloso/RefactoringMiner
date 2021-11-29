package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InlineOperationDetection {
	private final UMLOperationBodyMapper mapper;
	private final List<UMLOperation> removedOperations;
	private final UMLClassBaseDiff classDiff;
	private final UMLModelDiff modelDiff;
	private final List<AbstractCall> operationInvocations;
	private final Map<CallTreeNode, CallTree> callTreeMap = new LinkedHashMap<>();
	
	public InlineOperationDetection(UMLOperationBodyMapper mapper, List<UMLOperation> removedOperations, UMLClassBaseDiff classDiff, UMLModelDiff modelDiff) {
		this.mapper = mapper;
		this.removedOperations = removedOperations;
		this.classDiff = classDiff;
		this.modelDiff = modelDiff;
		this.operationInvocations = getInvocationsInTargetOperationBeforeInline(mapper);
	}

	public List<InlineOperationRefactoring> check(UMLOperation removedOperation) throws RefactoringMinerTimedOutException {
		List<InlineOperationRefactoring> refactorings = new ArrayList<>();
		if(!mapper.getNonMappedLeavesT2().isEmpty() || !mapper.getNonMappedInnerNodesT2().isEmpty() ||
			!mapper.getReplacementsInvolvingMethodInvocation().isEmpty()) {
			List<AbstractCall> removedOperationInvocations = matchingInvocations(removedOperation, operationInvocations, mapper.getOperation1());
			if(removedOperationInvocations.size() > 0 && !invocationMatchesWithAddedOperation(removedOperationInvocations.get(0), mapper.getOperation1(), mapper.getOperation2().getAllOperationInvocations())) {
				AbstractCall removedOperationInvocation = removedOperationInvocations.get(0);
				CallTreeNode root = new CallTreeNode(mapper.getOperation1(), removedOperation, removedOperationInvocation);
				CallTree callTree;
				if(callTreeMap.containsKey(root)) {
					callTree = callTreeMap.get(root);
				}
				else {
					callTree = new CallTree(root);
					generateCallTree(removedOperation, root, callTree);
					callTreeMap.put(root, callTree);
				}
				UMLOperationBodyMapper operationBodyMapper = createMapperForInlinedMethod(mapper, removedOperation, removedOperationInvocation);
				List<AbstractCodeMapping> additionalExactMatches = new ArrayList<>();
				List<CallTreeNode> nodesInBreadthFirstOrder = callTree.getNodesInBreadthFirstOrder();
				for(int i=1; i<nodesInBreadthFirstOrder.size(); i++) {
					CallTreeNode node = nodesInBreadthFirstOrder.get(i);
					if(matchingInvocations(node.getInvokedOperation(), operationInvocations, mapper.getOperation1()).size() == 0) {
						UMLOperationBodyMapper nestedMapper = createMapperForInlinedMethod(mapper, node.getInvokedOperation(), node.getInvocation());
						additionalExactMatches.addAll(nestedMapper.getExactMatches());
						if(inlineMatchCondition(nestedMapper, mapper)) {
							List<AbstractCall> nestedMatchingInvocations = matchingInvocations(node.getInvokedOperation(), node.getOriginalOperation().getAllOperationInvocations(), node.getOriginalOperation());
							InlineOperationRefactoring nestedRefactoring = new InlineOperationRefactoring(nestedMapper, mapper.getOperation1(), nestedMatchingInvocations);
							refactorings.add(nestedRefactoring);
							operationBodyMapper.addChildMapper(nestedMapper);
						}
					}
				}
				if(inlineMatchCondition(operationBodyMapper, mapper)) {
					InlineOperationRefactoring inlineOperationRefactoring =	new InlineOperationRefactoring(operationBodyMapper, mapper.getOperation1(), removedOperationInvocations);
					refactorings.add(inlineOperationRefactoring);
				}
			}
		}
		return refactorings;
	}

	private List<AbstractCall> matchingInvocations(UMLOperation removedOperation, List<AbstractCall> operationInvocations, UMLOperation callerOperation) {
		List<AbstractCall> removedOperationInvocations = new ArrayList<>();
		for(AbstractCall invocation : operationInvocations) {
			if(invocation.matchesOperation(removedOperation, callerOperation, modelDiff)) {
				removedOperationInvocations.add(invocation);
			}
		}
		return removedOperationInvocations;
	}

	private UMLOperationBodyMapper createMapperForInlinedMethod(UMLOperationBodyMapper mapper,
			UMLOperation removedOperation, AbstractCall removedOperationInvocation) throws RefactoringMinerTimedOutException {
		List<String> arguments = removedOperationInvocation.getArguments();
		List<String> parameters = removedOperation.getParameterNameList();
		Map<String, String> parameterToArgumentMap = new LinkedHashMap<>();
		//special handling for methods with varargs parameter for which no argument is passed in the matching invocation
		int size = Math.min(arguments.size(), parameters.size());
		for(int i=0; i<size; i++) {
			parameterToArgumentMap.put(parameters.get(i), arguments.get(i));
		}
		UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, mapper, parameterToArgumentMap, new LinkedHashMap<>(), classDiff);
		return operationBodyMapper;
	}

	private void generateCallTree(UMLOperation operation, CallTreeNode parent, CallTree callTree) {
		List<AbstractCall> invocations = operation.getAllOperationInvocations();
		for(UMLOperation removedOperation : removedOperations) {
			for(AbstractCall invocation : invocations) {
				if(invocation.matchesOperation(removedOperation, operation, modelDiff)) {
					if(!callTree.containsInPathToRootOrSibling(parent, removedOperation)) {
						CallTreeNode node = new CallTreeNode(parent, operation, removedOperation, invocation);
						parent.addChild(node);
						generateCallTree(removedOperation, node, callTree);
					}
				}
			}
		}
	}

	private List<AbstractCall> getInvocationsInTargetOperationBeforeInline(UMLOperationBodyMapper mapper) {
		List<AbstractCall> operationInvocations = mapper.getOperation1().getAllOperationInvocations();
		for(AbstractCodeFragment statement : mapper.getNonMappedLeavesT1()) {
			ExtractOperationDetection.addStatementInvocations(operationInvocations, statement);
			for(UMLAnonymousClass anonymousClass : classDiff.getRemovedAnonymousClasses()) {
				if(statement.getLocationInfo().subsumes(anonymousClass.getLocationInfo())) {
					for(UMLOperation anonymousOperation : anonymousClass.getOperations()) {
						for(AbstractCall anonymousInvocation : anonymousOperation.getAllOperationInvocations()) {
							if(!ExtractOperationDetection.containsInvocation(operationInvocations, anonymousInvocation)) {
								operationInvocations.add(anonymousInvocation);
							}
						}
					}
				}
			}
		}
		return operationInvocations;
	}

	private boolean inlineMatchCondition(UMLOperationBodyMapper operationBodyMapper, UMLOperationBodyMapper parentMapper) {
		int delegateStatements = 0;
		for(AbstractCodeFragment statement : operationBodyMapper.getNonMappedLeavesT1()) {
			AbstractCall invocation = statement.invocationCoveringEntireFragment();
			if(invocation != null && invocation.matchesOperation(operationBodyMapper.getOperation1(), parentMapper.getOperation1(), modelDiff)) {
				delegateStatements++;
			}
		}
		int mappings = operationBodyMapper.mappingsWithoutBlocks();
		int nonMappedElementsT1 = operationBodyMapper.nonMappedElementsT1()-delegateStatements;
		List<AbstractCodeMapping> exactMatchList = operationBodyMapper.getExactMatches();
		int exactMatches = exactMatchList.size();
		return mappings > 0 && (mappings > nonMappedElementsT1 ||
				(exactMatches == 1 && !exactMatchList.get(0).getFragment1().throwsNewException() && nonMappedElementsT1-exactMatches < 10) ||
				(exactMatches > 1 && nonMappedElementsT1-exactMatches < 20));
	}

	private boolean invocationMatchesWithAddedOperation(AbstractCall removedOperationInvocation, UMLOperation callerOperation, List<AbstractCall> operationInvocationsInNewMethod) {
		if(operationInvocationsInNewMethod.contains(removedOperationInvocation)) {
			for(UMLOperation addedOperation : classDiff.getAddedOperations()) {
				if(removedOperationInvocation.matchesOperation(addedOperation, callerOperation, modelDiff)) {
					return true;
				}
			}
		}
		return false;
	}
}
