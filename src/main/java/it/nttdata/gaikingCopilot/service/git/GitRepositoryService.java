package it.nttdata.gaikingCopilot.service.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.io.IOException;
import java.net.URISyntaxException;

@Service
public class GitRepositoryService {

    private static final String DEFAULT_REMOTE = "origin";

    public void initializeRepository(Path projectPath) throws GitAPIException {
        Git.init().setDirectory(projectPath.toFile()).call();
    }

    public void checkoutBranch(Path projectPath, String branchName) throws GitAPIException, IOException {

        try (Git git = Git.open(projectPath.toFile())) {
            git.checkout()
                .setName(branchName)
                .setCreateBranch(true)
                .call();
        }
    }

    public void addRemote(Path projectPath, String remoteUrl) throws IOException, GitAPIException, URISyntaxException {
        try (Git git = Git.open(projectPath.toFile())) {
            git.remoteAdd()
                .setName(DEFAULT_REMOTE)
                .setUri(new URIish(remoteUrl))
                .call();
        }        
        
    }

    public void addFiles(Path projectPath) throws IOException, GitAPIException {

        try (Git git = Git.open(projectPath.toFile())) {
            git.add()
                .addFilepattern(".")
                .call();
        }
    }

    public void commit(Path projectPath, String message) throws IOException, GitAPIException {

        try (Git git = Git.open(projectPath.toFile())) {
            git.commit()
                .setMessage(message)
                .call();
        }
    }

    public void push(Path projectPath, String branchName, String username, String token) throws IOException, GitAPIException {

        try (Git git = Git.open(projectPath.toFile())) {
            git.push()
                .setRemote(DEFAULT_REMOTE)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                .add("refs/heads/" + branchName)
                .call();
        }
    }

}
