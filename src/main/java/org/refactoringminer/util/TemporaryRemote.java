package org.refactoringminer.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TemporaryRemote implements AutoCloseable {
    Git tempGit;
    Repository tmpRepository;
    Git localGit;
    Repository localRepository;

    @Override
    public void close() throws Exception {
        Optional<RemoteConfig> remotes = tempGit.remoteList().call().stream().filter(rem -> rem.getName().equals("origin")).findAny();
        assert remotes.isPresent() : "No remote named 'origin' found";
        RemoteConfig origin = remotes.get();

        Optional<URIish> first = origin.getURIs().stream().findFirst();
        assert first.isPresent() : "No URI found for remote 'origin'";
        URIish uri = first.get();

        var cfg = localGit.remoteSetUrl().setRemoteName(origin.getName()).setRemoteUri(uri).call();
        tempGit.close();
        tmpRepository.close();
    }


    public TemporaryRemote(Repository localRepository, @Nullable Path dir) throws GitAPIException, IOException, URISyntaxException {
        dir = dir == null ? Path.of("tmp-repo") : dir;
        this.localGit = new Git(localRepository);
        this.localRepository = localRepository;

        Optional<RemoteConfig> remotes = localGit.remoteList().call().stream().filter(rem -> rem.getName().equals("origin")).findAny();
        assert remotes.isPresent() : "No remote named 'origin' found";
        RemoteConfig origin = remotes.get();

        Optional<URIish> first = origin.getURIs().stream().findFirst();
        assert first.isPresent() : "No URI found for remote 'origin'";
        URIish uri = first.get();

        Path tempDir = Files.createTempDirectory(dir, null);
        File tempDirFile = tempDir.toFile();
        tempGit = Git.cloneRepository().setURI(uri.toString()).setDirectory(tempDirFile).call();
        tmpRepository = tempGit.getRepository();

        URIish tmpOrigin = new URIish(this.tmpRepository.getDirectory().toString());
        localGit.remoteSetUrl().setRemoteName(origin.getName()).setRemoteUri(tmpOrigin).call();
    }

    public TemporaryRemote(Git tempGit, Repository tmpRepository, Git localGit, Repository localRepository) {
        this.tempGit = tempGit;
        this.tmpRepository = tmpRepository;
        this.localGit = localGit;
        this.localRepository = localRepository;
    }

    public Git getTempGit() {
        return tempGit;
    }

    public Repository getTmpRepository() {
        return tmpRepository;
    }

    public Git getLocalGit() {
        return localGit;
    }

    public Repository getLocalRepository() {
        return localRepository;
    }
}
