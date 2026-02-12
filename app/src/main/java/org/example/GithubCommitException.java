package org.example;

public class GithubCommitException extends Exception {
    public final int CI_STATUS;
    public GithubCommitException(int ciStatus) {
        super();
        CI_STATUS = ciStatus;
    }
}
