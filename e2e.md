# End-to-End (E2E) Test Cases for evolveumCLIapp

This document contains a set of simple manual test cases to verify the functionality of the entire application against a running instance of Evolveum MidPoint.

**Prerequisites:**
- A running instance of MidPoint (e.g., midpoint demo).
- You need to have at least one valid user `oid` available (for test purposes, we will use `<USER_OID>`, which should be replaced with an actual one).
- Compiled project: `mvn clean package`.

---

## Test Case 1: Configuration Initialization (`config-init`)
**Purpose:** Verify that the application correctly saves the configuration and obfuscates the password.
1. Run the command:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar config-init -u "https://demo.evolveum.com/midpoint" -l administrator -p <password>
   ```
2. **Expected result:** `Configuration successfully saved to ~/.evcliapp.properties` is printed.
3. Check the `~/.evcliapp.properties` file.
4. **Expected result:** The file exists, contains `url`, `login` and obfuscated `password` (it must not be stored as plain-text).

**Actual result:**
Printed `Configuration successfully saved to ~/.evcliapp.properties`, the file `~/.evcliapp.properties` was created correctly and contains all attributes, including the obfuscated password. -> **PASSED**

## Test Case 2: Unsuccessful User Search (`search-users`)
**Purpose:** Verify search for a non-existent record.
1. Run the command with a nonsense query:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar search-users -q "NonsenseName123"
   ```
2. **Expected result:** `No users found matching the query.` is printed to the console. The application exits with code `0` (success from the application's perspective).

**Actual result:**
`No users found matching the query.` was printed correctly to the console. -> **PASSED**

## Test Case 3: Successful User Search (`search-users`)
**Purpose:** Verify user search and retrieval of their OID (for subsequent tests).
1. Run the command:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar search-users -q "admin"
   ```
2. **Expected result:** A formatted table displaying the user (e.g., `administrator`) and their OID is printed. Note down this OID as `<USER_OID>`.

**Actual result:**
The table printed correctly and contains 1 user (administrator). -> **PASSED**

## Test Case 4: User Detail (`get-user`)
**Purpose:** Verify the output of the entire JSON object for an existing user.
1. Run the command with the OID obtained in the previous test:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar get-user -o <USER_OID>
   ```
2. **Expected result:** The full JSON format of the midPoint User object is printed.
3. Run the command with a fake OID that has a valid UUID format (e.g., `11111111-1111-1111-1111-111111111111`).
4. **Expected result:** `Error 404: User not found. Check logs/app.log for details.` is printed to `System.err` and detailed error is written to the log. The application exit code is `1`.

**Actual result:**
Midpoint JSON object was printed correctly. When run with a fake oid, an error was correctly printed to output and logs. -> **PASSED**

## Test Case 5: User Modification (`modify-user`)
**Purpose:** Verify the addition, replacement, and deletion of an attribute using an OID.
1. Add/Change user's description:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar modify-user -o <USER_OID> -p description -v "Tested via CLI"
   ```
2. **Expected result:** `Success: User modified successfully` is printed.
3. (Optional) Verify using `get-user -o <USER_OID>` that the `description` attribute has changed.
4. Test an invalid `path` (path to an attribute that does not exist in the schema):
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar modify-user -o <USER_OID> -p fakeAttribute -v "test"
   ```
5. **Expected result:** Failure. Warning `Error 400: Bad Request. Check logs/app.log for details.` Exit code is `1`.

**Actual result:**
The change was correctly reflected in the demo midpoint. When an invalid attribute was entered, it still resulted in code 204. After checking with Postman, this is actually the expected behavior of the API. -> **PASSED** (note: in this case, I would contact the tester and ask for the test case to be modified)

## Test Case 6: Interactive Mode (REPL shell)
**Purpose:** Verify that the application works smoothly in the built-in shell.
1. Start the shell without arguments:
   ```bash
   java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar
   ```
2. Type a command into it and press Enter:
   ```text
   evCLIapp> search-users -q "admin"
   ```
3. **Expected result:** The results table is printed identically to Direct Execution.
4. Use the Up Arrow.
5. **Expected result:** The previous command is loaded from history.
6. Exit the shell by typing `exit` or pressing `Ctrl+D`.
7. **Expected result:** The application shuts down safely.

**Actual result:**
The command executed in the built-in shell ran correctly, the up arrow loaded the previous command, and the shell exited gracefully by typing exit as well as Ctrl+D. -> **PASSED**

## Test Case 7: Docker Sanity Check
**Purpose:** Verify that all aforementioned operations can be performed from an isolated container.
1. Build the Docker image:
   ```bash
   docker build -t evolveum-cli .
   ```
2. Run a search via Docker (using your local config):
   ```bash
   docker run -v ~/.evcliapp.properties:/root/.evcliapp.properties evolveum-cli search-users -q "admin"
   ```
3. **Expected result:** The result is printed, identical to Test Case 3. No dependencies are missing, and the container exits (shuts down) after the task is executed.

**Actual result:**
The Docker container was built correctly and the command result is identical to test case 3. The container correctly exited after task execution. -> **PASSED**