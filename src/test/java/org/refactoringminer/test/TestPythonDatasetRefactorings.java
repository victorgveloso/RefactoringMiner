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
					.or(Refactorings.RenameMethod.getValue());
		TestBuilder test = new TestBuilder(detector, REPOS, types);
		RefactoringPopulator.preparePythonRefactorings(test, types);
		test.assertExpectationsWithGitHubAPI(6, 0, 0);
	}
}
