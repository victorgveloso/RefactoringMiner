package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher.MatchResult;

public class UMLClassRenameDiff extends UMLClassBaseDiff {
	private MatchResult matchResult;
	public UMLClassRenameDiff(UMLClass originalClass, UMLClass renamedClass, UMLModelDiff modelDiff, MatchResult matchResult) {
		super(originalClass, renamedClass, modelDiff);
		this.matchResult = matchResult;
	}

	public UMLClass getRenamedClass() {
		return nextClass;
	}

	public boolean samePackage() {
		return originalClass.getPackageName().equals(nextClass.getPackageName());
	}

	public MatchResult getMatchResult() {
		return matchResult;
	}

	public String toString() {
		return "class " +
				originalClass.getName() +
				" was renamed to " +
				nextClass.getName() +
				"\n";
	}
}
