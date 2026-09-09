package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.IOException;

/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url = "https://github.com/1c-syntax/bsl-language-server/commit/0ce5f524b36ca7e85513e5dee24893a9fdb4f3cd";
        url = "https://github.com/asyml/texar/commit/b5b06c0f262413ef62c4bfff996f3189673507b1";
        url = "https://github.com/OpenGamma/Strata/commit/b2b9b629685ebc7e89e9a1667de88f2e878d5fc4"; // Interesting test case nikos has shown me with many assertion related refactorings
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
        new WebDiff(projectASTDiff).openInBrowser();
    }
}
