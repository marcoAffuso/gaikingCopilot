package it.nttdata.gaikingCopilot.utility;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;

@Log4j2
public class OperationOnFileSystem {

    public void createProjectGit(Path projectPath) throws IOException {
        log.info("Creating project Git: {}", projectPath.getFileName());
    }

    public byte[] zipProjectDirectory(Path projectPath) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
             Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .forEach(path -> addFileToZip(projectPath, path, zipOutputStream));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    public void addFileToZip(Path projectRoot, Path file, ZipOutputStream zipOutputStream) {
        try {
            String entryName = projectRoot.getFileName() + "/" + projectRoot.relativize(file).toString().replace('\\', '/');
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(Files.readAllBytes(file));
            zipOutputStream.closeEntry();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public void deleteProjectDirectory(Path projectPath) throws IOException {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(this::deletePath);
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

}
