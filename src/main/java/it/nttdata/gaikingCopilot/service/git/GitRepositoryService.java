package it.nttdata.gaikingCopilot.service.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;
import java.io.IOException;
import java.net.URISyntaxException;

@Log4j2
@Service
public class GitRepositoryService {

    private static final String DEFAULT_REMOTE = "origin";

    public void initializeRepository(Path projectPath) throws GitAPIException {
        log.info("Initializing Git repository. projectPath={}", projectPath);

        Git.init().setDirectory(projectPath.toFile()).call();
        log.info("Git repository initialized successfully. projectPath={}", projectPath);
    }

    public void checkoutBranch(Path projectPath, String branchName) throws GitAPIException, IOException {
        log.info("Checking out Git branch. projectPath={}, branchName={}", projectPath, branchName);

        try (Git git = Git.open(projectPath.toFile())) {
            git.checkout()
                .setName(branchName)
                .setCreateBranch(true)
                .call();
            log.info("Git branch checked out successfully. projectPath={}, branchName={}", projectPath, branchName);
        }
    }

    public void addRemote(Path projectPath, String remoteUrl) throws IOException, GitAPIException, URISyntaxException {
        log.info("Adding Git remote. projectPath={}, remoteName={}", projectPath, DEFAULT_REMOTE);

        try (Git git = Git.open(projectPath.toFile())) {
            git.remoteAdd()
                .setName(DEFAULT_REMOTE)
                .setUri(new URIish(remoteUrl))
                .call();
            log.info("Git remote added successfully. projectPath={}, remoteName={}", projectPath, DEFAULT_REMOTE);
        }
        
    }

    public void addFiles(Path projectPath) throws IOException, GitAPIException {
        log.info("Adding files to Git index. projectPath={}", projectPath);

        try (Git git = Git.open(projectPath.toFile())) {
            git.add()
                .addFilepattern(".")
                .call();
            log.info("Files added to Git index successfully. projectPath={}", projectPath);
        }
    }

    public void commit(Path projectPath, String message) throws IOException, GitAPIException {
        log.info("Creating Git commit. projectPath={}, messageLength={}", projectPath, message != null ? message.length() : 0);

        try (Git git = Git.open(projectPath.toFile())) {
            git.commit()
                .setMessage(message)
                .call();
            log.info("Git commit created successfully. projectPath={}", projectPath);
        }
    }

    public void push(Path projectPath, String branchName, String username, String token) throws IOException, GitAPIException {
        log.info("Pushing Git branch. projectPath={}, branchName={}, username={}", projectPath, branchName, username);

        try (Git git = Git.open(projectPath.toFile())) {
            git.push()
                .setRemote(DEFAULT_REMOTE)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                .add("refs/heads/" + branchName)
                .call();
            log.info("Git push completed successfully. projectPath={}, branchName={}, username={}", projectPath, branchName, username);
        }
    }

}
