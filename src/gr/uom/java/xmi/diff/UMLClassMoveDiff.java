package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;

public class UMLClassMoveDiff extends UMLClassBaseDiff {
	private MatchResult matchResult;
	public UMLClassMoveDiff(UMLClass originalClass, UMLClass movedClass, UMLModelDiff modelDiff, MatchResult matchResult) {
		super(originalClass, movedClass, modelDiff);
		this.matchResult = matchResult;
	}

	public UMLClass getMovedClass() {
		return nextClass;
	}

	public MatchResult getMatchResult() {
		return matchResult;
	}

	public String toString() {
		return "class " +
				originalClass.getName() +
				" was moved to " +
				nextClass.getName() +
				"\n";
	}

	public boolean equals(Object o) {
		if(this == o) {
    		return true;
    	}
		
		if(o instanceof UMLClassMoveDiff) {
			UMLClassMoveDiff classMoveDiff = (UMLClassMoveDiff)o;
			return this.originalClass.equals(classMoveDiff.originalClass) && this.nextClass.equals(classMoveDiff.nextClass);
		}
		return false;
	}
}
