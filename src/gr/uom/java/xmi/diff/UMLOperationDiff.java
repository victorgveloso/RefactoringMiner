package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.decomposition.VariableReferenceExtractor;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class UMLOperationDiff {
	@Getter private boolean operationRenamed;
	@Getter private UMLOperation removedOperation;
	@Getter private UMLOperation addedOperation;
	@Getter private List<UMLParameter> addedParameters;
	@Getter private List<UMLParameter> removedParameters;
	@Getter private List<UMLParameterDiff> parameterDiffList;
	private boolean visibilityChanged;
	private boolean abstractionChanged;
	private boolean finalChanged;
	private boolean staticChanged;
	private boolean synchronizedChanged;
	private boolean returnTypeChanged;
	private boolean qualifiedReturnTypeChanged;
	private boolean parametersReordered;
	private Set<AbstractCodeMapping> mappings = new LinkedHashSet<>();
	private Set<Pair<VariableDeclaration, VariableDeclaration>> matchedVariables = new LinkedHashSet<>();
	private Set<Refactoring> refactorings = new LinkedHashSet<>();
	private UMLAnnotationListDiff annotationListDiff;
	private List<UMLType> addedExceptionTypes;
	private List<UMLType> removedExceptionTypes;
	private SimpleEntry<Set<UMLType>, Set<UMLType>> changedExceptionTypes;
	@Getter private TestOperationDiff testOperationDiff;

	public UMLOperationDiff(UMLOperation removedOperation, UMLOperation addedOperation) {
		process(removedOperation, addedOperation);
	}

	public UMLOperationDiff(UMLOperationBodyMapper mapper) {
		this.mappings = mapper.getMappings();
		this.matchedVariables = mapper.getMatchedVariables();
		this.refactorings = mapper.getRefactoringsAfterPostProcessing();
		process(mapper.getOperation1(), mapper.getOperation2());
	}

	public boolean isEmpty() {
		return addedParameters.isEmpty() && removedParameters.isEmpty() && parameterDiffList.isEmpty() &&
		!visibilityChanged && !abstractionChanged && !finalChanged && !staticChanged && !synchronizedChanged && !returnTypeChanged && !operationRenamed && annotationListDiff.isEmpty();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty())
			sb.append("\t").append(removedOperation).append("\n");
		if(operationRenamed)
			sb.append("\t").append("renamed from ").append(removedOperation.getName()).append(" to ").append(addedOperation.getName()).append("\n");
		if(visibilityChanged)
			sb.append("\t").append("visibility changed from ").append(removedOperation.getVisibility()).append(" to ").append(addedOperation.getVisibility()).append("\n");
		if(abstractionChanged)
			sb.append("\t").append("abstraction changed from ").append(removedOperation.isAbstract() ? "abstract" : "concrete").append(" to ").append(addedOperation.isAbstract() ? "abstract" : "concrete").append("\n");
		if(returnTypeChanged || qualifiedReturnTypeChanged)
			sb.append("\t").append("return type changed from ").append(removedOperation.getReturnParameter()).append(" to ").append(addedOperation.getReturnParameter()).append("\n");
		for(UMLParameter umlParameter : removedParameters) {
			sb.append("\t").append("parameter ").append(umlParameter).append(" removed").append("\n");
		}
		for(UMLParameter umlParameter : addedParameters) {
			sb.append("\t").append("parameter ").append(umlParameter).append(" added").append("\n");
		}
		for(UMLParameterDiff parameterDiff : parameterDiffList) {
			sb.append(parameterDiff);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			sb.append("\t").append("annotation ").append(annotation).append(" removed").append("\n");
		}
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			sb.append("\t").append("annotation ").append(annotation).append(" added").append("\n");
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
			sb.append("\t").append("annotation ").append(annotationDiff.getRemovedAnnotation()).append(" modified to ").append(annotationDiff.getAddedAnnotation()).append("\n");
		}
		return sb.toString();
	}

	public Set<Refactoring> getRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		refactorings.addAll(getReturnRefactorings());
		refactorings.addAll(getParameterRefactorings());
		refactorings.addAll(getAnnotationRefactorings());
		refactorings.addAll(getExceptionRefactorings());
		refactorings.addAll(getModifierRefactorings());

		return refactorings;
	}

	public Set<Refactoring> getRefactoringsAfterPostProcessing() {
		return refactorings;
	}

	private Set<Refactoring> getReturnRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		if(returnTypeChanged || qualifiedReturnTypeChanged) {
			UMLParameter removedOperationReturnParameter = removedOperation.getReturnParameter();
			UMLParameter addedOperationReturnParameter = addedOperation.getReturnParameter();
			if(removedOperationReturnParameter != null && addedOperationReturnParameter != null) {
				Set<AbstractCodeMapping> references = VariableReferenceExtractor.findReturnReferences(mappings);
				ChangeReturnTypeRefactoring refactoring = new ChangeReturnTypeRefactoring(removedOperationReturnParameter.getType(), addedOperationReturnParameter.getType(),
						removedOperation, addedOperation, references);
				refactorings.add(refactoring);
			}
		}
		return refactorings;
	}

	private Set<Refactoring> getParameterRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		for(UMLParameterDiff parameterDiff : getParameterDiffList()) {
			boolean conflictFound = false;
			for(Pair<VariableDeclaration, VariableDeclaration> matchedPair : matchedVariables) {
				if(matchedPair.getLeft().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
						!matchedPair.getRight().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
					conflictFound = true;
					if(matchedPair.getLeft().isParameter() && matchedPair.getRight().isLocalVariable()) {
						Refactoring rename = new RenameVariableRefactoring(matchedPair.getLeft(), matchedPair.getRight(), removedOperation, addedOperation,
								VariableReferenceExtractor.findReferences(matchedPair.getLeft(), matchedPair.getRight(), mappings));
						refactorings.add(rename);
						Refactoring addParameter = new AddParameterRefactoring(parameterDiff.getAddedParameter(), removedOperation, addedOperation);
						refactorings.add(addParameter);
					}
					break;
				}
				if(matchedPair.getRight().equals(parameterDiff.getAddedParameter().getVariableDeclaration()) &&
						!matchedPair.getLeft().equals(parameterDiff.getRemovedParameter().getVariableDeclaration())) {
					conflictFound = true;
					if(matchedPair.getLeft().isLocalVariable() && matchedPair.getRight().isParameter()) {
						Refactoring rename = new RenameVariableRefactoring(matchedPair.getLeft(), matchedPair.getRight(), removedOperation, addedOperation,
								VariableReferenceExtractor.findReferences(matchedPair.getLeft(), matchedPair.getRight(), mappings));
						refactorings.add(rename);
						Refactoring removeParameter = new RemoveParameterRefactoring(parameterDiff.getRemovedParameter(), removedOperation, addedOperation);
						refactorings.add(removeParameter);
					}
					break;
				}
			}
			for(Refactoring refactoring : this.refactorings) {
				if(refactoring instanceof RenameVariableRefactoring) {
					RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
					if(rename.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							!rename.getRenamedVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
					else if(!rename.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							rename.getRenamedVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
				}
				else if(refactoring instanceof ChangeVariableTypeRefactoring) {
					ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
					if(changeType.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							!changeType.getChangedTypeVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
					else if(!changeType.getOriginalVariable().equals(parameterDiff.getRemovedParameter().getVariableDeclaration()) &&
							changeType.getChangedTypeVariable().equals(parameterDiff.getAddedParameter().getVariableDeclaration())) {
						conflictFound = true;
						break;
					}
				}
			}
			if(!conflictFound) {
				refactorings.addAll(parameterDiff.getRefactorings());
			}
		}
		int exactMappings = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.isExact()) {
				exactMappings++;
			}
		}
		if(removedParameters.isEmpty() || exactMappings > 0) {
			for(UMLParameter umlParameter : addedParameters) {
				boolean conflictFound = false;
				for(Refactoring refactoring : this.refactorings) {
					if(refactoring instanceof RenameVariableRefactoring) {
						RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
						if(rename.getRenamedVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
					else if(refactoring instanceof ChangeVariableTypeRefactoring) {
						ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
						if(changeType.getChangedTypeVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
				}
				if(!conflictFound) {
					AddParameterRefactoring refactoring = new AddParameterRefactoring(umlParameter, removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
		}
		if(addedParameters.isEmpty() || exactMappings > 0) {
			for(UMLParameter umlParameter : removedParameters) {
				boolean conflictFound = false;
				for(Refactoring refactoring : this.refactorings) {
					if(refactoring instanceof RenameVariableRefactoring) {
						RenameVariableRefactoring rename = (RenameVariableRefactoring)refactoring;
						if(rename.getOriginalVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
					else if(refactoring instanceof ChangeVariableTypeRefactoring) {
						ChangeVariableTypeRefactoring changeType = (ChangeVariableTypeRefactoring)refactoring;
						if(changeType.getOriginalVariable().equals(umlParameter.getVariableDeclaration())) {
							conflictFound = true;
							break;
						}
					}
				}
				if(!conflictFound) {
					RemoveParameterRefactoring refactoring = new RemoveParameterRefactoring(umlParameter, removedOperation, addedOperation);
					refactorings.add(refactoring);
				}
			}
		}
		if(parametersReordered) {
			ReorderParameterRefactoring refactoring = new ReorderParameterRefactoring(removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		return refactorings;
	}

	private Set<Refactoring> getAnnotationRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		for(UMLAnnotation annotation : annotationListDiff.getAddedAnnotations()) {
			AddMethodAnnotationRefactoring refactoring = new AddMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLAnnotation annotation : annotationListDiff.getRemovedAnnotations()) {
			RemoveMethodAnnotationRefactoring refactoring = new RemoveMethodAnnotationRefactoring(annotation, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLAnnotationDiff annotationDiff : annotationListDiff.getAnnotationDiffList()) {
			ModifyMethodAnnotationRefactoring refactoring = new ModifyMethodAnnotationRefactoring(annotationDiff.getRemovedAnnotation(), annotationDiff.getAddedAnnotation(), removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		return refactorings;
	}

	private Set<Refactoring> getExceptionRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		for(UMLType exceptionType : addedExceptionTypes) {
			AddThrownExceptionTypeRefactoring refactoring = new AddThrownExceptionTypeRefactoring(exceptionType, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		for(UMLType exceptionType : removedExceptionTypes) {
			RemoveThrownExceptionTypeRefactoring refactoring = new RemoveThrownExceptionTypeRefactoring(exceptionType, removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		if(changedExceptionTypes != null) {
			ChangeThrownExceptionTypeRefactoring refactoring = new ChangeThrownExceptionTypeRefactoring(changedExceptionTypes.getKey(), changedExceptionTypes.getValue(), removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		return refactorings;
	}

	private Set<Refactoring> getModifierRefactorings() {
		Set<Refactoring> refactorings = new LinkedHashSet<>();
		if(visibilityChanged) {
			ChangeOperationAccessModifierRefactoring refactoring = new ChangeOperationAccessModifierRefactoring(removedOperation.getVisibility(), addedOperation.getVisibility(), removedOperation, addedOperation);
			refactorings.add(refactoring);
		}
		if(finalChanged) {
			if(addedOperation.isFinal()) {
				AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring(Modifier.FINAL, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			else if(removedOperation.isFinal()) {
				RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring(Modifier.FINAL, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
		}
		if(abstractionChanged) {
			if(addedOperation.isAbstract()) {
				AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring(Modifier.ABSTRACT, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			else if(removedOperation.isAbstract()) {
				RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring(Modifier.ABSTRACT, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
		}
		if(staticChanged) {
			if(addedOperation.isStatic()) {
				AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring(Modifier.STATIC, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			else if(removedOperation.isStatic()) {
				RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring(Modifier.STATIC, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
		}
		if(synchronizedChanged) {
			if(addedOperation.isSynchronized()) {
				AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring(Modifier.SYNCHRONIZED, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
			else if(removedOperation.isSynchronized()) {
				RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring(Modifier.SYNCHRONIZED, removedOperation, addedOperation);
				refactorings.add(refactoring);
			}
		}
		return refactorings;
	}

	private void process(UMLOperation removedOperation, UMLOperation addedOperation) {
		this.removedOperation = removedOperation;
		this.addedOperation = addedOperation;
		this.addedParameters = new ArrayList<>();
		this.removedParameters = new ArrayList<>();
		this.parameterDiffList = new ArrayList<>();
		this.addedExceptionTypes = new ArrayList<>();
		this.removedExceptionTypes = new ArrayList<>();
		operationRenamed = !removedOperation.getName().equals(addedOperation.getName());
		visibilityChanged = !removedOperation.getVisibility().equals(addedOperation.getVisibility());
		abstractionChanged = removedOperation.isAbstract() != addedOperation.isAbstract();
		returnTypeChanged = !removedOperation.equalReturnParameter(addedOperation);
		finalChanged = removedOperation.isFinal() != addedOperation.isFinal();
		staticChanged = removedOperation.isStatic() != addedOperation.isStatic();
		synchronizedChanged = removedOperation.isSynchronized() != addedOperation.isSynchronized();
		if(!returnTypeChanged)
			qualifiedReturnTypeChanged = !removedOperation.equalQualifiedReturnParameter(addedOperation);
		processThrownExceptionTypes(removedOperation.getThrownExceptionTypes(), addedOperation.getThrownExceptionTypes());
		this.annotationListDiff = new UMLAnnotationListDiff(removedOperation.getAnnotations(), addedOperation.getAnnotations());
		List<SimpleEntry<UMLParameter, UMLParameter>> matchedParameters = updateAddedRemovedParameters(removedOperation, addedOperation);
		for(SimpleEntry<UMLParameter, UMLParameter> matchedParameter : matchedParameters) {
			UMLParameter parameter1 = matchedParameter.getKey();
			UMLParameter parameter2 = matchedParameter.getValue();
			UMLParameterDiff parameterDiff = new UMLParameterDiff(parameter1, parameter2, removedOperation, addedOperation, mappings);
			if(!parameterDiff.isEmpty()) {
				parameterDiffList.add(parameterDiff);
			}
		}
		int matchedParameterCount = matchedParameters.size()/2;
		if(isNumberOfParamsUnchanged(removedOperation, addedOperation, matchedParameterCount) && isParamListChanged(removedOperation, addedOperation)) {
			parametersReordered = true;
		}
		//first round match parameters with the same name
		matchParamSameName(removedOperation, addedOperation);
		//second round match parameters with the same type
		matchParamSameType(removedOperation, addedOperation);
		//third round match parameters with different type and name
		matchParamDifferentTypeAndName(removedOperation, addedOperation, matchedParameterCount);
	}

	private boolean isParamListChanged(UMLOperation removedOperation, UMLOperation addedOperation) {
		return !removedOperation.getParameterNameList().equals(addedOperation.getParameterNameList());
	}

	private boolean isNumberOfParamsUnchanged(UMLOperation removedOperation, UMLOperation addedOperation, int matchedParameterCount) {
		return removedParameters.isEmpty() && addedParameters.isEmpty() &&
				matchedParameterCount == removedOperation.getParameterNameList().size() && matchedParameterCount == addedOperation.getParameterNameList().size() &&
				removedOperation.getParameterNameList().size() > 1;
	}

	/**
	 * Third round: match parameters with different type and name
	 * @param removedOperation old operation
	 * @param addedOperation new operation
	 * @param matchedParameterCount number of matched parameter pairs
	 */
	private void matchParamDifferentTypeAndName(UMLOperation removedOperation, UMLOperation addedOperation, int matchedParameterCount) {
		List<UMLParameter> removedParametersWithoutReturnType = removedOperation.getParametersWithoutReturnType();
		List<UMLParameter> addedParametersWithoutReturnType = addedOperation.getParametersWithoutReturnType();
		if(matchedParameterCount == removedParametersWithoutReturnType.size()-1 && matchedParameterCount == addedParametersWithoutReturnType.size()-1) {
			for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
				UMLParameter removedParameter = removedParameterIterator.next();
				int indexOfRemovedParameter = indexOfParameter(removedParametersWithoutReturnType, removedParameter);
				for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
					UMLParameter addedParameter = addedParameterIterator.next();
					int indexOfAddedParameter = indexOfParameter(addedParametersWithoutReturnType, addedParameter);
					if(indexOfRemovedParameter == indexOfAddedParameter) {
						UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings);
						if(!parameterDiff.isEmpty()) {
							parameterDiffList.add(parameterDiff);
						}
						addedParameterIterator.remove();
						removedParameterIterator.remove();
						break;
					}
				}
			}
		}
	}

	/**
	 * Second round: match parameters with the same type
	 * @param removedOperation old operation
	 * @param addedOperation new operation
	 */
	private void matchParamSameType(UMLOperation removedOperation, UMLOperation addedOperation) {
		for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			UMLParameter removedParameter = removedParameterIterator.next();
			for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				UMLParameter addedParameter = addedParameterIterator.next();
				if(removedParameter.getType().equalsQualified(addedParameter.getType()) &&
						!existsAnotherAddedParameterWithTheSameType(addedParameter)) {
					UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings);
					if(!parameterDiff.isEmpty()) {
						parameterDiffList.add(parameterDiff);
					}
					addedParameterIterator.remove();
					removedParameterIterator.remove();
					break;
				}
			}
		}
	}

	/**
	 * First round: match parameters with the same name
	 * @param removedOperation old operation
	 * @param addedOperation new operation
	 */
	private void matchParamSameName(UMLOperation removedOperation, UMLOperation addedOperation) {
		for(Iterator<UMLParameter> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext();) {
			UMLParameter removedParameter = removedParameterIterator.next();
			for(Iterator<UMLParameter> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext();) {
				UMLParameter addedParameter = addedParameterIterator.next();
				if(removedParameter.getName().equals(addedParameter.getName())) {
					UMLParameterDiff parameterDiff = new UMLParameterDiff(removedParameter, addedParameter, removedOperation, addedOperation, mappings);
					if(!parameterDiff.isEmpty()) {
						parameterDiffList.add(parameterDiff);
					}
					addedParameterIterator.remove();
					removedParameterIterator.remove();
					break;
				}
			}
		}
	}

	private int indexOfParameter(List<UMLParameter> parameters, UMLParameter parameter) {
		int index = 0;
		for(UMLParameter p : parameters) {
			if(p.equalsIncludingName(parameter)) {
				return index;
			}
			index++;
		}
		return -1;
	}

	private void processThrownExceptionTypes(List<UMLType> exceptionTypes1, List<UMLType> exceptionTypes2) {
		Set<UMLType> addedExceptionTypes = compareUmlTypes(exceptionTypes2, exceptionTypes1);
		Set<UMLType> removedExceptionTypes = compareUmlTypes(exceptionTypes1, exceptionTypes2);

		if(removedExceptionTypes.size() > 0 && addedExceptionTypes.size() > 0) {
			this.changedExceptionTypes = new SimpleEntry<>(removedExceptionTypes, addedExceptionTypes);
		} else if(removedExceptionTypes.size() > 0) {
			this.removedExceptionTypes.addAll(removedExceptionTypes);
		} else if(addedExceptionTypes.size() > 0) {
			this.addedExceptionTypes.addAll(addedExceptionTypes);
		}
	}

	private Set<UMLType> compareUmlTypes(List<UMLType> exceptionTypes1, List<UMLType> exceptionTypes2) {
		Set<UMLType> result = new LinkedHashSet<>();
		for(UMLType exceptionType1 : exceptionTypes1) {
			if(!exceptionTypes2.contains(exceptionType1)){
				result.add(exceptionType1);
			}
		}
		return result;
	}

	private boolean existsAnotherAddedParameterWithTheSameType(UMLParameter parameter) {
		if(removedOperation.hasTwoParametersWithTheSameType() && addedOperation.hasTwoParametersWithTheSameType()) {
			return false;
		}
		for(UMLParameter addedParameter : addedParameters) {
			if(!addedParameter.getName().equals(parameter.getName()) &&
					addedParameter.getType().equalsQualified(parameter.getType())) {
				return true;
			}
		}
		return false;
	}

	private List<SimpleEntry<UMLParameter, UMLParameter>> updateAddedRemovedParameters(UMLOperation removedOperation, UMLOperation addedOperation) {
		List<SimpleEntry<UMLParameter, UMLParameter>> matchedParameters = new ArrayList<>();
		for(UMLParameter parameter1 : removedOperation.getParameters()) {
			if(!parameter1.getKind().equals("return")) {
				boolean found = false;
				for(UMLParameter parameter2 : addedOperation.getParameters()) {
					if(parameter1.equalsIncludingName(parameter2)) {
						matchedParameters.add(new SimpleEntry<>(parameter1, parameter2));
						found = true;
						break;
					}
				}
				if(!found) {
					this.removedParameters.add(parameter1);
				}
			}
		}
		for(UMLParameter parameter1 : addedOperation.getParameters()) {
			if(!parameter1.getKind().equals("return")) {
				boolean found = false;
				for(UMLParameter parameter2 : removedOperation.getParameters()) {
					if(parameter1.equalsIncludingName(parameter2)) {
						matchedParameters.add(new SimpleEntry<>(parameter2, parameter1));
						found = true;
						break;
					}
				}
				if(!found) {
					this.addedParameters.add(parameter1);
				}
			}
		}
		return matchedParameters;
	}
}
