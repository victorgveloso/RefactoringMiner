package org.refactoringminer.test;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;

public class TestPythonDatasetRefactorings {
	private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		BigInteger types = 
					Refactorings.ExtractMethod.getValue()
					.or(Refactorings.InlineMethod.getValue())
					.or(Refactorings.RenameMethod.getValue())
					.or(Refactorings.RenameParameter.getValue())
					.or(Refactorings.ReorderParameter.getValue())
					.or(Refactorings.AddParameter.getValue())
					.or(Refactorings.RemoveParameter.getValue())
					.or(Refactorings.MoveAndRenameClass.getValue())
					.or(Refactorings.ChangeVariableType.getValue())
					.or(Refactorings.RenameVariable.getValue());
		TestBuilder test = new TestBuilder(detector, REPOS, types);
		RefactoringPopulator.preparePythonRefactorings(test, types);
		test.assertExpectationsWithGitHubAPI(23, 0, 0);
	}
}
