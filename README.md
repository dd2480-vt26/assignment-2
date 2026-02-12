# Continuous Integration Server

This program builds a continuous integration server.

## Program Workflow

The workflow of the program is as follows:

1. The CI server receives GitHub webhooks when push event on the repository occures, parses the payload and extracts relevant elements.
2. The CI server clones the repository.
3. Checkout on the commited branch.
4. Builds the project. 
5. Runs the tests.
6. Stores the build and test logs.
7. Notifies the commit status on GitHub

## Requirements

- Java Development Kit (JDK): Java 21
- Gradle: 9.3.0, provided via the Gradle Wrapper (`./gradlew`)
    - **NOTE:** No system Gradle installation is required
- Test framework: JUnit 5, configured in [gradle/libs.versions.toml](gradle/libs.versions.toml)
    - **NOTE:** Installed and managed automatically by Gradle

## How to Use

### Set up ngrok

Download ngrok:

```
https://ngrok.com/
```

Run the command:

```
ngrok http 8019
```
Copy the generated URL (Forwarding) and go to GitHub:

```
https://github.com/dd2480-vt26/assignment-2
```

Then go to:

```
Settings > Webhooks
```

Then click on Add webhook and paste the generated URL in:

```
Payload URL *
```

Finally, click on Add webhook.

### Set up GitHub token
Create a config.properties file in the root.

Create a new GitHub token:

```
https://github.com/settings/personal-access-tokens/new
```

Add the GitHub token in the file.

This is made for the Rest API: allows it to modify the commit statuses on GitHub.

### Running
To run the program (builds automatically if needed):
```
./gradlew run
```

### Tests

Tests are implemented using JUnit 5. The unit tests are located in `app/src/test/java/org/example/`, and covers cloning, building, testing, etc.

To run all tests:
```
./gradlew test
```

### API documentation

To generate the API documentation in a browsable format:
```
./gradlew javadoc
```
The generated document can be found: 

```
/app/build/docs/index.html
```

### Build and test logs history

List all build files:
```
http://localhost:8019/logs/dd2480-vt26/assignment-2
```

List specifib build file (also available after listing all build files):
```
http://localhost:8019/logs/dd2480-vt26/assignment-2/2026-02-12T18:03:57.43653696+01:00.json
http://localhost:8019/logs/dd2480-vt26/assignment-2/2026-02-12T18:04:53.776641063+01:00.json

etc
```


## Contributions
- **Jonathan Skantz:** Implemented HTTP handler, list all builds (visualization), integrate all the CI server steps, main file and corresponding tests.
- **Elias Hollstrand:** Implemented updating of commit status in ContinousIntegrationServer, GithubUtils; Everything related to the REST API and corresponding tests.
- **Elias Gaghlasian:** Implemented RepoCloner, GradleBuildRunner, and corresponding tests; wrote README.
- **Fabian Holm:** Implemented set up of template for project, PushPayload, BranchCheckout, and corresponding tests.
- **Vadim El Guedj:** Implemented TestRunner, BuildResult, RepoCleanup and corresponding tests; wrote README.

Something that can be considered a valuable and remarkable achievement is that we have a dynamic workflow with GitHub issues, PR reviewing with protected main (not being able to directly push or merge to main), and Kanban board (notes, backlog, ready, in progress, in review, and done) to track our progress on each issue and avoid duplicate work and missunderstandings. This, in turn, allows us to work more efficiently, especially with the kanban board, a new addition to our workflow, which lets us more concretely see how it is going for each member and at which stage each issue is. We also have a structurally and correctly isolated file structure with individual checks in each class to avoid unnecessary clutter and confusion in the main. For a new collaborator or viewer, it will be an easy task to analyze, comprehend, and test each part of the simulated workflow individually without any difficulties. We also have a BuildResult class that compiles the information into one easy object that is viewable with all build logs, tests, etc. Therefore, the remarkability becomes our way of working, functioning, and organizing to, in turn, improve our workspace for current and future development.



Additionally, we used GitHub Issues for tracking features, bugs, and improvements. Most commits reference issue IDs, and those are in the format "{What this commit will change}. Fix #{issue num}".

