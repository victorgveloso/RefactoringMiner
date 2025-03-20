package org.refactoringminer.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteListCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class TemporaryRemoteTest {
    static Stream<Arguments> testClose() {
        return Stream.of(Arguments.arguments("https://github.com/victorgveloso/ExampleJavaTestRefactoring.git"));
    }

    @ParameterizedTest
    @MethodSource("testClose")
    void testClosePreconditions(String remoteUri, @TempDir Path tmpDir) throws Exception {
        Path tmp = Files.createDirectory(tmpDir.resolve("tmp"));
        Path local = Files.createDirectory(tmpDir.resolve("local"));
        Git tmpGit = Git.cloneRepository().setBare(true).setDirectory(tmp.toFile()).setURI(remoteUri).call();
        Git localGit = Git.cloneRepository().setDirectory(local.toFile()).setURI(tmpGit.getRepository().getDirectory().getPath()).call();
        var temporaryRemote = new TemporaryRemote(tmpGit, tmpGit.getRepository(), localGit, localGit.getRepository());
        assertNotEquals(remoteUri, tmpGit.getRepository().getDirectory().getPath());
        assertEquals(tmpGit.getRepository().getDirectory().getPath(), localGit.remoteList().call().get(0).getURIs().get(0).toString());
        assertEquals(remoteUri, tmpGit.remoteList().call().get(0).getURIs().get(0).toString());
    }

    @ParameterizedTest
    @MethodSource
    void testClose(String remoteUri, @TempDir Path tmpDir) throws Exception {
        Path tmp = Files.createDirectory(tmpDir.resolve("tmp"));
        Path local = Files.createDirectory(tmpDir.resolve("local"));
        Git tmpGit = Git.cloneRepository().setBare(true).setDirectory(tmp.toFile()).setURI(remoteUri).call();
        Git localGit = Git.cloneRepository().setDirectory(local.toFile()).setURI(tmpGit.getRepository().getDirectory().getPath()).call();
        var temporaryRemote = new TemporaryRemote(tmpGit, tmpGit.getRepository(), localGit, localGit.getRepository());
        temporaryRemote.close();
        assertEquals(remoteUri, localGit.remoteList().call().get(0).getURIs().get(0).toString());
    }

    @ParameterizedTest
    @MethodSource("testClose")
    void testMockedClose(String remoteUri, @TempDir Path tmpDir) throws Exception {
        RemoteConfig remoteCfg = mock(RemoteConfig.class);
        when(remoteCfg.getName()).thenReturn("origin");
        var uri = new URIish(remoteUri);
        when(remoteCfg.getURIs()).thenReturn(Collections.singletonList(uri));
        Repository tmpRepo = mock(Repository.class);
        Git tmpGit = spy(new Git(tmpRepo));
        when(tmpGit.getRepository()).thenReturn(tmpRepo);
        when(tmpRepo.getDirectory()).thenReturn(tmpDir.resolve("non-existent file").toFile());
        RemoteListCommand remotes = mock(RemoteListCommand.class);
        when(tmpGit.remoteList()).thenReturn(remotes);
        when(remotes.call()).thenReturn(Collections.singletonList(remoteCfg));

        Git localGit = Git.cloneRepository().setDirectory(tmpDir.toFile()).setURI(remoteUri).call();
        var temporaryRemote = new TemporaryRemote(tmpGit, tmpRepo, localGit, localGit.getRepository());
        temporaryRemote.close();
        assertEquals(remoteUri, localGit.remoteList().call().get(0).getURIs().get(0).toString());
    }

    @ParameterizedTest
    @MethodSource("testClose")
    void testConstructor(String remoteUri, @TempDir Path tmpDir) throws Exception {
        Path local = Files.createDirectory(tmpDir.resolve("local"));
        Git localGit = Git.cloneRepository().setDirectory(local.toFile()).setURI(remoteUri).call();
        Path tmp = Files.createDirectory(tmpDir.resolve("tmp"));
        var temporaryRemote = new TemporaryRemote(localGit.getRepository(), tmp);

        assertNotEquals(remoteUri, localGit.remoteList().call().get(0).getURIs().get(0).toString());
        assertEquals(remoteUri, temporaryRemote.getTmpGit().remoteList().call().get(0).getURIs().get(0).toString());

        temporaryRemote.close();

        assertEquals(remoteUri, localGit.remoteList().call().get(0).getURIs().get(0).toString());
    }

    private static String remoteListToString(Git localGit) throws GitAPIException {
        return localGit.remoteList().call().stream().map(r -> r.getName() + r.getURIs().stream().map(u -> u.toString()).collect(Collectors.joining(","))).collect(Collectors.joining(","));
    }
}