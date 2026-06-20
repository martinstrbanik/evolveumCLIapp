# evolveumCLIapp

A hybrid command-line interface application for Midpoint administration. 
This application supports both direct execution for automation and an interactive shell with autocompletion.

## Build

Compile and build the fat JAR using Maven:

```bash
mvn clean package
```

## Execution Modes

The CLI supports two modes of execution depending on the arguments provided.

### 1. Direct Execution (Automation Mode)
If you pass arguments, the application executes the command and exits immediately. This is ideal for CI/CD, crons, or scripts.

```bash
# Initialize configuration (default path ~/.evcliapp.properties)
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar config-init -u https://demo.evolveum.com/midpoint -l administrator -p IGA4ever

# Initialize configuration with custom file path
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar config-init -c ./custom-config.properties -u https://demo.evolveum.com/midpoint -l administrator -p IGA4ever

# Get user by OID
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar get-user -o 00000000-0000-0000-0000-000000000002

# Show help
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar --help
```

### 2. Interactive Shell Mode (REPL)
If you run the application with **no arguments**, it opens an interactive shell using JLine3. It supports command history (Up/Down arrows) and tab-autocompletion.

```bash
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar
```
Inside the interactive shell:
```
evCLIapp> help
evCLIapp> config-init -u https://demo.evolveum.com/midpoint -l administrator -p IGA4ever
evCLIapp> get-user -o 00000000-0000-0000-0000-000000000002
evCLIapp> exit
```

## Common Options

All commands support an optional configuration path parameter:
- `-c, --config <configPath>`: Path to the configuration file. If not provided, it defaults to `~/.evcliapp.properties`.

## Supported Commands

- `config-init`: Set up midPoint connection properties (passwords are obfuscated via Base64 before saving).
- `get-user`: Fetch an exact user by standard midPoint OID and return the JSON payload.
- `modify-user`: Modify a user attribute by OID. Supports `add`, `replace`, and `delete` operations.
- `search-users`: Search users by name/login and display a simple list of matching usernames and OIDs.

### Examples for `search-users`:
```bash
# Direct execution to search for users containing "peter"
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar search-users -q "peter"

# Interactive shell execution
evCLIapp> search-users -q "peter"
```

### Examples for `modify-user`:
```bash
# Direct execution to replace description
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar modify-user -o 00000000-0000-0000-0000-000000000002 -p description -v "New description"

# Interactive shell execution
evCLIapp> modify-user -o 00000000-0000-0000-0000-000000000002 -p description -v "New description"
```