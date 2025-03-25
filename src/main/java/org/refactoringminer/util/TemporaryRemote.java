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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class TemporaryRemote implements AutoCloseable {
    Git tmpGit;
    Repository tmpRepository;
    Git localGit;
    Repository localRepository;

    @Override
    public void close() throws Exception {
        Optional<RemoteConfig> remotes = tmpGit.remoteList().call().stream().filter(rem -> rem.getName().equals("origin")).findAny();
        assert remotes.isPresent() : "No remote named 'origin' found";
        RemoteConfig origin = remotes.get();

        Optional<URIish> first = origin.getURIs().stream().findFirst();
        assert first.isPresent() : "No URI found for remote 'origin'";
        URIish uri = first.get();

        localGit.remoteSetUrl().setRemoteName(origin.getName()).setRemoteUri(uri).call();
        Path tmpPath = tmpRepository.getDirectory().toPath();
        tmpGit.close();
        if (Files.isDirectory(tmpPath)) {
            try (Stream<Path> walk = Files.walk(tmpPath)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }


    public TemporaryRemote(Repository localRepository, @Nullable Path dir) throws GitAPIException, IOException, URISyntaxException {
        dir = dir == null ? Path.of("tmp-repo") : dir;
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        localGit = new Git(localRepository);
        this.localRepository = localRepository;

        Optional<RemoteConfig> remotes = localGit.remoteList().call().stream().filter(rem -> rem.getName().equals("origin")).findAny();
        assert remotes.isPresent() : "No remote named 'origin' found";
        RemoteConfig origin = remotes.get();

        Optional<URIish> first = origin.getURIs().stream().findFirst();
        assert first.isPresent() : "No URI found for remote 'origin'";
        URIish uri = first.get();

        Path tempDir = Files.createTempDirectory(dir, null);
        File tempDirFile = tempDir.toFile();
        tmpGit = Git.cloneRepository()
                .setNoTags()
                .setNoCheckout(true)
                .setBare(true)
                .setURI(uri.toString())
                .setDirectory(tempDirFile)
                .call();
        tmpRepository = tmpGit.getRepository();

        Path tmpPath = this.tmpRepository.getDirectory().toPath();
        Path localPath = localRepository.getDirectory().toPath().getParent();
        URIish tmpOrigin = new URIish(localPath.relativize(tmpPath).toString());
        localGit.remoteSetUrl().setRemoteName(origin.getName()).setRemoteUri(tmpOrigin).call();
    }

    public TemporaryRemote(Git tmpGit, Repository tmpRepository, Git localGit, Repository localRepository) {
        this.tmpGit = tmpGit;
        this.tmpRepository = tmpRepository;
        this.localGit = localGit;
        this.localRepository = localRepository;
    }

    public Git getTmpGit() {
        return tmpGit;
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
