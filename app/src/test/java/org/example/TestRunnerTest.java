package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TestRunner class.
 */

public class TestRunnerTest {

    /**
     * Negative test: runTests should return an error if the project directory is null.
     * Test case: projectDirectory = null.
     * Expected: BuildResult.status = ERROR.
     */

    @Test
    void testRunTestWithNullDirectory() {
        BuildResult result = TestRunner.runTests(null);
        assertEquals(BuildResult.Status.ERROR, result.status);
    }

    /**
     * Negative test: runTests should return an error if the project directory does not exist.
     * Test case: projectDirectory points to a non-existent path
     * Expected: BuildResult.status = ERROR
     */

    @Test
    void testRunTestsWithNonExistentDirectory() {
        File nonExistentDir = new File("path/does/not/exist");
        BuildResult result = TestRunner.runTests(nonExistentDir);
        assertEquals(BuildResult.Status.ERROR, result.status);
    }

    /**
     * Negative test: runTests should return an error if the given File is not a directory.
     * Test case: projectDirectory points to a regular file
     * Expected: BuildResult.status = ERROR
     * @param tempDir temporary directory provided by JUnit
     */

    @Test
    void testRunTestsWithFileInsteadOfDirectory(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("myFile.txt").toFile();
        file.createNewFile();
        BuildResult result = TestRunner.runTests(file);
        assertEquals(BuildResult.Status.ERROR, result.status);
    }

}
