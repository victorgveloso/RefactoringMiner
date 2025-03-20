package org.refactoringminer.util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitServiceImplTest {
    GitServiceImpl sut = new GitServiceImpl();
    private static Git tmpGit;
    private static String url;
    private static LocalDate date;

    @BeforeAll
    static void setUp(@TempDir Path tmpDir) throws IOException, GitAPIException {
        url = "https://github.com/EmpiricalSEConcordia/Refactoringminer-Astdiff-Exporter.git";
        tmpGit = Git.cloneRepository().setBare(true).setDirectory(tmpDir.toFile()).setURI(url).call();
        date = LocalDate.of(2025,3,10);
    }

    @Test
    void testResetToLastCommitBefore_SuccessWithNonBareLocalOrigin(@TempDir Path localDir) throws Exception {
        try (Git localGit = Git.cloneRepository().setDirectory(localDir.toFile()).setURI(tmpGit.getRepository().getDirectory().getPath()).call()) {
            Repository repo = localGit.getRepository();
            Ref ref1 = repo.findRef(MessageFormat.format("refs/remotes/origin/{0}", repo.getBranch()));
            ObjectId latestCommit = ref1.getObjectId();
            sut.resetToLastCommitBefore(repo, date);
            Ref ref2 = repo.findRef(MessageFormat.format("refs/remotes/origin/{0}", repo.getBranch()));
            ObjectId oldCommit = ref2.getObjectId();
            assertNotEquals(latestCommit, oldCommit);
        }
    }

    @Test
    void testResetToLastCommitBefore_FailWithBare() {
        assertThrows(IllegalStateException.class, () -> sut.resetToLastCommitBefore(tmpGit.getRepository(), date));
    }

    @Test
    void testResetToLastCommitBefore_FailWithRemoteOrigin(@TempDir Path localDir) throws GitAPIException {
        try (Git localGit = Git.cloneRepository().setDirectory(localDir.toFile()).setURI(url).call()) {
            assertThrows(IllegalStateException.class, () -> sut.resetToLastCommitBefore(localGit.getRepository(), date));
        }
    }
}