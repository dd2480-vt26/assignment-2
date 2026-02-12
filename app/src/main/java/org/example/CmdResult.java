package org.example;

/**
 * Represents the result of a build or test execution.
 */

public class CmdResult{
    /**
     * Enum representing the possible outcomes of a build or test process.
     * SUCCESS: the process completed successfully (exit code 0).
     * FAILURE: the process completed but failed.
     * ERROR: the process could not complete.
     */
    public enum Type{
        SUCCESS,
        FAILURE,
        ERROR,
        NON_EXISTENT;
    }

    /**
     * status: the final status of the build/test
     */
    public final Type status;

    /**
     * logs: the standard output/error logs captured during execution.
     */

    public final String log;

    /**
     * errorMessage: a descriptive error message if an error occurred.
     */

    public String errorMessage;

    public CmdResult(Type status) {
        this.status = status;
        this.log = null;
    }

    public CmdResult(Type status, String log) {
        this.status = status;
        this.log = log;
    }


}
