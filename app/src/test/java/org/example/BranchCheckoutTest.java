package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BranchCheckoutTest {

    @TempDir
    Path tempDir;

    /**
     * Positive test: If the repo is valid and the branch name isnt null,
     * {@code runGitCheckout} should be called.
     * Test case: directory and branch excsists and are valid.
     * Expected: {@code runGitCheckout} does not throw.
     */
    @Test
    void validRepo_checkoutRuns() throws IOException {
        Path repoDir = tempDir.resolve("repo");
        Files.createDirectories(repoDir.resolve(".git"));

        TestableBranchCheckout checkout = new TestableBranchCheckout();
        checkout.checkoutBranch(repoDir, "branch");

        assertTrue(checkout.checkoutCalled);
    }

    /**
     * Negative test: If there is no .git folder,
     * {@code checkoutBranch} should throw.
     * Test case: no .git folder exists.
     * Expected: {@code checkoutBranch} throws.
     */
    @Test
    void missingGitDirectory_throws() {
        Path repoDir = tempDir.resolve("repo");
        BranchCheckout checkout = new BranchCheckout();

        assertThrows(IOException.class, () -> checkout.checkoutBranch(repoDir, "branch"));
    }

    /**
     * Negative test: If the branch name is blank,
     * {@code checkoutBranch} should throw.
     * Test case: {@code branchName} is blank.
     * Expected: {@code checkoutBranch} throws.
     */
    @Test
    void blankBranch_throws() {
        BranchCheckout checkout = new BranchCheckout();

        assertThrows(IllegalArgumentException.class, () -> checkout.checkoutBranch(tempDir, " "));
    }

    private static class TestableBranchCheckout extends BranchCheckout {
        boolean checkoutCalled = false;

        /**
         * Test hook: Simulate a successful checkout without invoking real Git.
         * Expected: Mark {@code checkoutCalled} true.
         */
        @Override
        protected void runGitCheckout(Path repoDir, String branchName) throws IOException {
            checkoutCalled = true;
        }
    }
}