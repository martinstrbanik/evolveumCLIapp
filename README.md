# Task 1

A hybrid command-line interface (CLI) application for Evolveum MidPoint administration. 
This application communicates with the MidPoint REST API and supports both direct execution for automation scripts and an interactive shell with autocompletion.

## Features
- **Interactive REPL Shell:** JLine3 powered shell with command history and tab-autocompletion.
- **Direct Execution:** Single command execution tailored for cron jobs and CI/CD pipelines.
- **Dockerized:** Multi-stage Docker build for lightweight and portable execution.
- **Audit & Logging:** Daily rotating logs to keep track of operations safely.

## Build Instructions

**Prerequisites:** Java 21, Maven

Compile and build the fat JAR using Maven:
```bash
mvn clean package
```

## Execution Modes

The CLI operates in two modes based on how you invoke it.

### 1. Direct Execution (Automation Mode)
By passing command arguments directly, the application executes the requested operation and exits. Ideal for scripting.
```bash
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar search-users -q "peter"
```

### 2. Interactive Shell Mode (REPL)
Running the application with **no arguments** starts an interactive shell.
```bash
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar
```
```text
evCLIapp> help
evCLIapp> search-users -q "peter"
evCLIapp> exit
```

## Command Reference

### Common Options
All commands support an optional configuration path parameter:
- `-c, --config <configPath>`: Path to the configuration file. If omitted, defaults to `~/.evcliapp.properties`.

---

### `config-init`
Initializes and saves the connection properties required to communicate with MidPoint. Passwords are obfuscated via Base64 before saving to disk.

**Usage:**
```bash
config-init -u <url> -l <login> -p <password> [-c <configPath>]
```
- `-u, --url`: Midpoint base URL (e.g., `https://demo.evolveum.com/midpoint`)
- `-l, --login`: Midpoint API user login
- `-p, --password`: Midpoint API user password

---

### `search-users`
Searches for users based on a partial match of their name/login and displays a formatted table of Usernames and OIDs.

**How it works:** Under the hood, this command constructs a MidPoint JSON Query utilizing the `name contains[origIgnoreCase]` filter. This means you only need to provide a substring of the username and the search is case-insensitive.

**Usage:**
```bash
search-users -q <query>
```
- `-q, --query`: The search query (e.g., `pet` will match `peter`, `Petra`, etc.)

---

### `get-user`
Fetches an exact user object by its standard MidPoint OID and returns the raw JSON payload. Useful for inspecting full user data.

**Usage:**
```bash
get-user -o <oid>
```
- `-o, --oid`: The exact UUID formatted OID of the user.

---

### `modify-user`
Modifies a specific attribute of a user.

**How it works:** This command interacts with the MidPoint REST API by constructing an `objectModification` payload containing an `itemDelta`. It does not overwrite the whole user object, but rather applies a highly specific delta (`add`, `replace`, `delete`) to the targeted attribute path.

**Usage:**
```bash
modify-user -o <oid> -p <path> -v <value> [-t <type>]
```
- `-o, --oid`: The exact UUID formatted OID of the user.
- `-p, --path`: The attribute path to modify (e.g., `description`, `givenName`, `extension/costCenter`).
- `-v, --value`: The new value to be applied.
- `-t, --type`: Modification type. Allowed values: `add`, `replace`, `delete`. *(Default: `replace`)*

## Docker Integration

You can run the CLI application completely inside Docker without installing Java or Maven locally.

### 1. Build the Image
```bash
docker build -t evolveum-cli .
```

### 2. Run via Docker

**Interactive Mode:**
Use `-it` to attach the terminal. Mount the config file to persist credentials:
```bash
docker run -it -v ~/.evcliapp.properties:/root/.evcliapp.properties evolveum-cli
```

**Direct Execution:**
```bash
docker run -v ~/.evcliapp.properties:/root/.evcliapp.properties evolveum-cli search-users -q "peter"
```

**Accessing Logs:**
The container writes logs internally to `/app/logs`. To access them from your host machine, mount the directory:
```bash
docker run -it -v ~/.evcliapp.properties:/root/.evcliapp.properties -v "$(pwd)/logs:/app/logs" evolveum-cli
```

## Troubleshooting & Logs
The application logs its operations (including detailed REST API error responses and HTTP status codes) to the `logs/app.log` file. 
- The log file rotates daily or when it reaches 5MB.
- Standard output (console) only shows human-readable summaries or explicit instructions. For debugging issues like `401 Unauthorized` or `409 Conflict`, always inspect the `app.log` file.

## E2E tests
- End to end tests can be found in e2e.md file

# Task 2

## Proposed Architecture: Connector-Based Synchronization (Hub-and-Spoke)

To synchronize data efficiently between IGA System 1 (Registration) and IGA System 2 (Integration & Provisioning), I would implement a **Hub-and-Spoke architecture** using **optimized connectors running under trusted administrator privileges** rather than utilizing public, high-overhead APIs.

### 1. Inbound Layer (System 1 as a Source)
-   I would configure **System 1** to act as a source system (Spoke). 
-   Instead of exposing a generic REST API, System 1 will expose a dedicated data stream optimized for reading (e.g. special direct database connection to a read-only replica of its database view).

### 2. The Hub Layer (System 2 with an Outbound Connector)
-   **System 2** (Hub) will use a native connector (e.g., ConnId framework) to pull data from System 1.
-   Since the connector is configured by a Trusted Administrator, System 2 bypasses the slow attribute-level ACL checks and approval policy evaluations that would normally slow down a public API caller. The system "trusts" the incoming data from the connector.


# Task 3
I would approach the troubleshooting process using two alternative strategies:

## Strategy A: Fast Diagnostics Using AI
- **Payload Analysis via AI**: As a first step, I would feed the error message and the sent JSON into an AI assistant, asking for potential issues within the SCIM 2.0 context. Note: Since the JSON contains sensitive user data, I would mask or anonymize any personal identifiable information first to comply with data protection regulations.

- The AI would likely immediately point out that the created and lastModified attributes inside the meta object are defined as readOnly according to the RFC 7643 (SCIM 2.0) specification. The client (our connector) should not send these attributes during a POST operation at all, as they must be generated exclusively by the target server.

- **Verification and Fix**: I would verify this rule in the official SCIM 2.0 documentation. Then, I would investigate why our connector or mapping is sending this data. I would check, whether it is a misconfiguration in the IGA outbound mapping or a bug in the connector's codebase.

## Strategy B: Traditional Deep-Dive Analysis (Without AI)
If AI was unavailable, or if the read-only attribute restriction did not come to mind immediately, I would proceed analytically from source to target:

- **Knowledge Base Check**: I would check our internal knowledge base and issue tracker (e.g., Jira) to see if a similar issue has already been resolved for this specific customer infrastructure.

- **Data Flow Analysis (Inbound)**: I would verify the source systems. Are these outdated timestamps already coming into our IGA system from the inbound mappings, or are they generated within the IGA system itself?

- **IGA & Connector Debugging**: If the data inside the IGA system is correct (the source system were not the issue), the issue lies in the outbound phase. I would enable TRACE / DEBUG logging for provisioning and the connector in a test environment, create a test user and analyze the logs to find out exactly where and why the incorrect date is being injected into the payload.

- **Problem Isolation (Postman)**: I would replicate the raw payload and request in Postman and send it directly to the customer's test environment, completely bypassing our solution. This would help isolate the issue, confirming whether the request structure itself is faulty.

- If I got stuck and no other ideas would come to mind, I would consult with my coworkers, either via direct message or some development groupchat.



