package de.exit.jdmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressionstest zur Sicherstellung, dass alle Linux-Shell-Skripte ({@code *.sh})
 * ausschließlich Unix-LF-Zeilenenden ({@code \n}) und keine Windows-CRLF-Bytes ({@code \r}) enthalten.
 */
class ShellScriptLineEndingsTest {

    @Test
    @DisplayName("Alle .sh-Skripte im Projekt-Root müssen frei von CR-Bytes (CRLF) sein")
    void allShellScriptsMustUseUnixLfLineEndings() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        List<Path> shFiles;
        try (Stream<Path> stream = Files.list(root)) {
            shFiles = stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".sh"))
                    .toList();
        }

        assertThat(shFiles).isNotEmpty();

        for (Path shFile : shFiles) {
            byte[] bytes = Files.readAllBytes(shFile);
            boolean containsCr = false;
            for (byte b : bytes) {
                if (b == '\r') {
                    containsCr = true;
                    break;
                }
            }
            assertThat(containsCr)
                    .as("Shell-Skript %s darf keine Windows-CRLF-Zeilenenden (\\r) enthalten", shFile.getFileName())
                    .isFalse();
        }
    }
}
