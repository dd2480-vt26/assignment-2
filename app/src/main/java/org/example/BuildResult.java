package org.example;

/**
 * Represents the result of a build or test execution.
 */

public class BuildResult{
    /**
     * Enum representing the possible outcomes of a build or test process.
     * SUCCESS: the process completed successfully (exit code 0).
     * FAILURE: the process completed but failed.
     * ERROR: the process could not complete.
     */
    public enum Status{
        SUCCESS,
        FAILURE,
        ERROR
    }

    /**
     * status: the final status of the build/test
     * logs: the standard output/error logs captured during execution.
     * errorMessage: a descriptive error message if an error occurred.
     */

    public Status status;
    public String logs;
    public String errorMessage;

    public BuildResult(Status status) {
        this.status = status;
        this.logs = "";
    }

}

