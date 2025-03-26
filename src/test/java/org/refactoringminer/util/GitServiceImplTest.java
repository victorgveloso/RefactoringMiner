package org.refactoringminer.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceImplTest {
    GitServiceImpl sut = new GitServiceImpl();
    private static Git tmpGit;
    private static String url;
    private static LocalDate startDate;
    private static LocalDate endDate;

    @BeforeAll
    static void setUp(@TempDir Path tmpDir) throws IOException, GitAPIException {
        url = "https://github.com/EmpiricalSEConcordia/Refactoringminer-Astdiff-Exporter.git";
        tmpGit = Git.cloneRepository().setDirectory(tmpDir.toFile()).setURI(url).call();
        startDate = LocalDate.of(2025,3,10);
        endDate = LocalDate.of(2025,3,18);
    }

    @Test
    void createRevsWalkSince_hasNext() throws Exception {
        RevWalk revCommits = sut.createRevsWalkSince(tmpGit.getRepository(), startDate);
        Iterator<RevCommit> iterator = revCommits.iterator();
        assertTrue(iterator.hasNext());
    }

    @Test
    void createRevsWalkSince_oldestCommit() throws Exception {
        RevWalk revCommits = sut.createRevsWalkSince(tmpGit.getRepository(), startDate);
        RevCommit oldestCommit = null;
        for (RevCommit revCommit : revCommits) {
            oldestCommit = revCommit;
        }
        assertNotNull(oldestCommit);
        assertEquals("204181e144aea0428737b391f97b72bedb851043", oldestCommit.getName());
        assertEquals(LocalDate.of(2025,3,17), LocalDate.ofInstant(Instant.ofEpochSecond(oldestCommit.getCommitTime()), ZoneId.systemDefault()));
    }

    @Test
    void createRevsWalkSince_hasTwoCommits(@TempDir File repoDir) throws Exception {
        try (var git = Git.cloneRepository().setDirectory(repoDir).setURI("https://github.com/victorgveloso/DiameterAlgorithm.git").call()) {
            RevWalk revCommits = sut.createRevsWalkSince(git.getRepository(), LocalDate.of(2020,9,10));
            Iterator<RevCommit> iterator = revCommits.iterator();
            int commitsCount = 0;
            while (iterator.hasNext()) {
                commitsCount++;
                iterator.next();
            }
            assertEquals(3, commitsCount);
        }
    }

    @Test
    void fetchAndCreateRevsWalkInRange_hasNext() throws Exception {
        RevWalk revCommits = sut.fetchAndCreateRevsWalkInRange(tmpGit.getRepository(), null, startDate, endDate);
        Iterator<RevCommit> iterator = revCommits.iterator();
        assertTrue(iterator.hasNext());
    }

    @Test
    void fetchAndCreateRevsWalkInRange_firstCommit() throws Exception {
        RevWalk revCommits = sut.fetchAndCreateRevsWalkInRange(tmpGit.getRepository(), null, startDate, endDate);
        RevCommit firstCommit = revCommits.next();
        assertNotNull(firstCommit);
        assertEquals("f63489f4197fd8a6f054975bdbd8722de430fc7a", firstCommit.getName());
        assertEquals(LocalDate.of(2025,3,17), LocalDate.ofInstant(Instant.ofEpochSecond(firstCommit.getCommitTime()), ZoneId.systemDefault()));
    }

    @Test
    void fetchAndCreateRevsWalkInRange_oldestCommit() throws Exception {
        RevWalk revCommits = sut.fetchAndCreateRevsWalkInRange(tmpGit.getRepository(), null, startDate, endDate);
        RevCommit oldestCommit = null;
        for (RevCommit revCommit : revCommits) {
            oldestCommit = revCommit;
        }
        assertNotNull(oldestCommit);
        assertEquals("204181e144aea0428737b391f97b72bedb851043", oldestCommit.getName());
        assertEquals(LocalDate.of(2025,3,17), LocalDate.ofInstant(Instant.ofEpochSecond(oldestCommit.getCommitTime()), ZoneId.systemDefault()));
    }

    @Test
    void fetchAndCreateRevsWalkInRange_hasTwoCommits() throws Exception {
        RevWalk revCommits = sut.fetchAndCreateRevsWalkInRange(tmpGit.getRepository(), null, startDate, endDate);
        Iterator<RevCommit> iterator = revCommits.iterator();
        int commitsCount = 0;
        while (iterator.hasNext()) {
            commitsCount++;
            iterator.next();
        }
        assertEquals(2, commitsCount);
    }

    @Test
    void createRevsWalkBetweenCommits_hasNext() throws Exception {
        Iterable<RevCommit> revCommits = sut.createRevsWalkBetweenCommits(tmpGit.getRepository(), "204181e144aea0428737b391f97b72bedb851043", "f63489f4197fd8a6f054975bdbd8722de430fc7a");
        Iterator<RevCommit> iterator = revCommits.iterator();
        assertTrue(iterator.hasNext());
    }

    @Test
    void createRevsWalkBetweenCommits_firstCommit() throws Exception {
        Iterable<RevCommit> revCommits = sut.createRevsWalkBetweenCommits(tmpGit.getRepository(), "204181e144aea0428737b391f97b72bedb851043", "f63489f4197fd8a6f054975bdbd8722de430fc7a");
        Iterator<RevCommit> iterator = revCommits.iterator();
        RevCommit firstCommit = iterator.next();
        assertEquals("204181e144aea0428737b391f97b72bedb851043", firstCommit.getName());
        assertEquals(LocalDate.of(2025,3,17), LocalDate.ofInstant(Instant.ofEpochSecond(firstCommit.getCommitTime()), ZoneId.systemDefault()));
    }

    @Test
    void createRevsWalkBetweenCommits_hasTwoCommits() throws Exception {
        Iterable<RevCommit> revCommits = sut.createRevsWalkBetweenCommits(tmpGit.getRepository(), "204181e144aea0428737b391f97b72bedb851043", "f63489f4197fd8a6f054975bdbd8722de430fc7a");
        Iterator<RevCommit> iterator = revCommits.iterator();
        int commitsCount = 0;
        while (iterator.hasNext()) {
            iterator.next();
            commitsCount++;
        }
        assertEquals(2, commitsCount);
    }
}