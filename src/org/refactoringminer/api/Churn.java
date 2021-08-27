package org.refactoringminer.api;

import java.util.Objects;

/**
 * Code churn's Value Object
 */
public class Churn {
	
	private final int linesAdded;
	private final int linesRemoved;
	
	public Churn(int linesAdded, int linesRemoved) {
		this.linesAdded = linesAdded;
		this.linesRemoved = linesRemoved;
	}

	public int getLinesAdded() {
		return linesAdded;
	}

	public int getLinesRemoved() {
		return linesRemoved;
	}
	
	public int getChurn() {
		return linesAdded + linesRemoved;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Churn)) return false;
		Churn churn = (Churn) o;
		return getLinesAdded() == churn.getLinesAdded() && getLinesRemoved() == churn.getLinesRemoved();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getLinesAdded(), getLinesRemoved());
	}
}
