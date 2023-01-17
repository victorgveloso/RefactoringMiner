package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;

public class UMLClassDiff extends UMLClassBaseDiff {
	
	private String className;
	public UMLClassDiff(UMLClass originalClass, UMLClass nextClass, UMLModelDiff modelDiff) {
		super(originalClass, nextClass, modelDiff);
		this.className = originalClass.getName();
	}

	private void reportAddedOperation(UMLOperation umlOperation) {
		this.addedOperations.add(umlOperation);
	}

	private void reportRemovedOperation(UMLOperation umlOperation) {
		this.removedOperations.add(umlOperation);
	}

	private void reportAddedAttribute(UMLAttribute umlAttribute) {
		this.addedAttributes.add(umlAttribute);
	}

	private void reportRemovedAttribute(UMLAttribute umlAttribute) {
		this.removedAttributes.add(umlAttribute);
	}

	protected void processAttributes() throws RefactoringMinerTimedOutException {
		for(UMLAttribute attribute : originalClass.getAttributes()) {
			UMLAttribute matchingAttribute = nextClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportRemovedAttribute(attribute);
    		}
    		else {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(attribute, matchingAttribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(attribute, matchingAttribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
    	for(UMLAttribute attribute : nextClass.getAttributes()) {
    		UMLAttribute matchingAttribute = originalClass.containsAttribute(attribute);
    		if(matchingAttribute == null) {
    			this.reportAddedAttribute(attribute);
    		}
    		else {
    			UMLAttributeDiff attributeDiff = new UMLAttributeDiff(matchingAttribute, attribute, this, modelDiff);
    			if(!attributeDiff.isEmpty()) {
	    			refactorings.addAll(attributeDiff.getRefactorings());
	    			if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
    			}
    			else {
    				Pair<UMLAttribute, UMLAttribute> pair = Pair.of(matchingAttribute, attribute);
    				if(!commonAtrributes.contains(pair)) {
    					commonAtrributes.add(pair);
    				}
    				if(attributeDiff.encapsulated()) {
    					refactorings.addAll(attributeDiff.getRefactorings());
    				}
    			}
    		}
    	}
	}

	protected void processOperations() {
		for(UMLOperation operation : originalClass.getOperations()) {
			int index = nextClass.getOperations().indexOf(operation);
			UMLOperation operation2 = null;
			if(index != -1) {
				operation2 = nextClass.getOperations().get(index);
			}
    		if(index == -1 || differentParameterNames(operation, operation2))
    			this.reportRemovedOperation(operation);
    	}
    	for(UMLOperation operation : nextClass.getOperations()) {
    		int index = originalClass.getOperations().indexOf(operation);
    		UMLOperation operation1 = null;
    		if(index != -1) {
    			operation1 = originalClass.getOperations().get(index);
    		}
    		if(index == -1 || differentParameterNames(operation1, operation))
    			this.reportAddedOperation(operation);
    	}
	}

	private boolean differentParameterNames(UMLOperation operation1, UMLOperation operation2) {
		if(operation1 != null && operation2 != null && !operation1.getParameterNameList().equals(operation2.getParameterNameList())) {
			int methodsWithIdenticalName1 = 0;
			for(UMLOperation operation : originalClass.getOperations()) {
				if(operation != operation1 && operation.getName().equals(operation1.getName()) && !operation.hasVarargsParameter()) {
					methodsWithIdenticalName1++;
				}
			}
			int methodsWithIdenticalName2 = 0;
			for(UMLOperation operation : nextClass.getOperations()) {
				if(operation != operation2 && operation.getName().equals(operation2.getName()) && !operation.hasVarargsParameter()) {
					methodsWithIdenticalName2++;
				}
			}
			if(methodsWithIdenticalName1 > 0 && methodsWithIdenticalName2 > 0) {
				return true;
			}
		}
		return false;
	}

	protected void createBodyMappers() throws RefactoringMinerTimedOutException {
		for(UMLOperation originalOperation : originalClass.getOperations()) {
			for(UMLOperation nextOperation : nextClass.getOperations()) {
				if(originalOperation.equalsQualified(nextOperation) && !differentParameterNames(originalOperation, nextOperation)) {
					if(getModelDiff() != null) {
						List<UMLOperationBodyMapper> mappers = getModelDiff().findMappersWithMatchingSignature2(nextOperation);
						if(mappers.size() > 0) {
							UMLOperation operation1 = mappers.get(0).getOperation1();
							if(!operation1.equalSignature(originalOperation) &&
									getModelDiff().commonlyImplementedOperations(operation1, nextOperation, this)) {
								if(!removedOperations.contains(originalOperation)) {
									removedOperations.add(originalOperation);
								}
								break;
							}
						}
					}
	    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(originalOperation, nextOperation, this);
	    			this.addOperationBodyMapper(operationBodyMapper);
				}
			}
		}
		for(UMLOperation operation : originalClass.getOperations()) {
			int index = nextClass.getOperations().indexOf(operation);
			if(!containsMapperForOperation1(operation) && index != -1 && !removedOperations.contains(operation) && !differentParameterNames(operation, nextClass.getOperations().get(index))) {
    			int lastIndex = nextClass.getOperations().lastIndexOf(operation);
    			int finalIndex = index;
    			if(index != lastIndex) {
    				if(containsMapperForOperation2(nextClass.getOperations().get(index))) {
    					finalIndex = lastIndex;
    				}
    				else if(!operation.isConstructor()) {
	    				double d1 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(index).getReturnParameter().getType());
	    				double d2 = operation.getReturnParameter().getType().normalizedNameDistance(nextClass.getOperations().get(lastIndex).getReturnParameter().getType());
	    				if(d2 < d1) {
	    					finalIndex = lastIndex;
	    				}
    				}
    			}
    			UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(operation, nextClass.getOperations().get(finalIndex), this);
    			this.addOperationBodyMapper(operationBodyMapper);
    		}
    	}
		List<UMLOperation> removedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		List<UMLOperation> addedOperationsToBeRemoved = new ArrayList<UMLOperation>();
		for(UMLOperation removedOperation : removedOperations) {
			for(UMLOperation addedOperation : addedOperations) {
				if(removedOperation.equalsIgnoringVisibility(addedOperation) && !differentParameterNames(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
				else if(removedOperation.equalsIgnoringNameCase(addedOperation) && !differentParameterNames(removedOperation, addedOperation)) {
					UMLOperationBodyMapper operationBodyMapper = new UMLOperationBodyMapper(removedOperation, addedOperation, this);
					if(!removedOperation.getName().equals(addedOperation.getName()) &&
							!(removedOperation.isConstructor() && addedOperation.isConstructor())) {
						RenameOperationRefactoring rename = new RenameOperationRefactoring(operationBodyMapper, new HashSet<MethodInvocationReplacement>());
						refactorings.add(rename);
					}
					this.addOperationBodyMapper(operationBodyMapper);
					removedOperationsToBeRemoved.add(removedOperation);
					addedOperationsToBeRemoved.add(addedOperation);
				}
			}
		}
		removedOperations.removeAll(removedOperationsToBeRemoved);
		addedOperations.removeAll(addedOperationsToBeRemoved);
	}

	protected void checkForAttributeChanges() throws RefactoringMinerTimedOutException {
		for(Iterator<UMLAttribute> removedAttributeIterator = removedAttributes.iterator(); removedAttributeIterator.hasNext();) {
			UMLAttribute removedAttribute = removedAttributeIterator.next();
			for(Iterator<UMLAttribute> addedAttributeIterator = addedAttributes.iterator(); addedAttributeIterator.hasNext();) {
				UMLAttribute addedAttribute = addedAttributeIterator.next();
				if(removedAttribute.getName().equals(addedAttribute.getName())) {
					UMLAttributeDiff attributeDiff = new UMLAttributeDiff(removedAttribute, addedAttribute, this, modelDiff);
					refactorings.addAll(attributeDiff.getRefactorings());
					addedAttributeIterator.remove();
					removedAttributeIterator.remove();
					if(!attributeDiffList.contains(attributeDiff)) {
						attributeDiffList.add(attributeDiff);
					}
					break;
				}
			}
		}
	}

	protected boolean containsMapperForOperation1(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getOperation1() != null && mapper.getOperation1().equalsQualified(operation)) {
				return true;
			}
		}
		return false;
	}

	protected boolean containsMapperForOperation2(UMLOperation operation) {
		for(UMLOperationBodyMapper mapper : getOperationBodyMapperList()) {
			if(mapper.getOperation2() != null && mapper.getOperation2().equalsQualified(operation)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches(String className) {
		return this.className.equals(className);
	}

	public boolean matches(UMLType type) {
		return this.className.endsWith("." + type.getClassType());
	}
}
