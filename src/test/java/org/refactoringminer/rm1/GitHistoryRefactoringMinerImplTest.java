package org.refactoringminer.rm1;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.ValueSource;
import org.refactoringminer.api.RefactoringHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GitHistoryRefactoringMinerImplTest {
    GitHistoryRefactoringMinerImpl sut = new GitHistoryRefactoringMinerImpl();

    @Test
    void testFetchAndDetectFromDate(@TempDir Path tmpDir, @TempDir Path localDir) throws Exception {
        String remoteUri = "https://github.com/EmpiricalSEConcordia/Refactoringminer-Astdiff-Exporter.git";
        try (Git localGit = Git.cloneRepository().setDirectory(localDir.toFile()).setURI(remoteUri).call()) {
            Repository repo = localGit.getRepository();
            ObjectId latestCommit = repo.findRef(MessageFormat.format("refs/remotes/origin/{0}", repo.getBranch())).getObjectId();
            sut.fetchAndDetectFromDate(localGit.getRepository(), new RefactoringHandler() {}, new Date("Mar 10, 2025"), tmpDir);
            ObjectId olderCommit = repo.findRef(MessageFormat.format("refs/remotes/origin/{0}", repo.getBranch())).getObjectId();
            assertNotEquals(latestCommit, olderCommit);
        }
    }
}