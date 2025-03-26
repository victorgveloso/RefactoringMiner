package org.refactoringminer.rm1;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class GitHistoryRefactoringMinerImplTest {
    private static Git localGit;
    GitHistoryRefactoringMinerImpl sut = new GitHistoryRefactoringMinerImpl();

    @BeforeAll
    static void beforeAll(@TempDir File tmpDir) throws GitAPIException {
        localGit = Git.cloneRepository().setDirectory(tmpDir).setURI("https://github.com/victorgveloso/SOEN6491MidtermExam.git").call();
    }

    @ParameterizedTest
    @CsvSource({
            "19,true",
            "21,false"
    })
    void testFetchAndDetectFromDate(int dayOfMonth, boolean isRefactoringExpected) throws Exception {
        final boolean[] hasRefactorings = {false};
        sut.fetchAndDetectFromDate(localGit.getRepository(), new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                super.handle(commitId, refactorings);
                hasRefactorings[0] = true;
            }
        }, LocalDate.of(2023,2,dayOfMonth));
        assertEquals(isRefactoringExpected, hasRefactorings[0]);
    }

    @AfterAll
    static void afterAll() {
        localGit.close();
    }
}