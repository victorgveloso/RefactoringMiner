package gr.uom.java.xmi.decomposition.replacement;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.ObjectCreation;

public class ClassInstanceCreationWithMethodInvocationReplacement extends Replacement {
	private final ObjectCreation objectCreationBefore;
	private final AbstractCall invokedOperationAfter;

	public ClassInstanceCreationWithMethodInvocationReplacement(String before, String after, ReplacementType type,
			ObjectCreation objectCreationBefore, AbstractCall invokedOperationAfter) {
		super(before, after, type);
		this.objectCreationBefore = objectCreationBefore;
		this.invokedOperationAfter = invokedOperationAfter;
	}

	public ObjectCreation getObjectCreationBefore() {
		return objectCreationBefore;
	}

	public AbstractCall getInvokedOperationAfter() {
		return invokedOperationAfter;
	}

}
