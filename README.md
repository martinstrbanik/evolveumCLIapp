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
# Execute with options
java -jar target/evolveumCLIapp-1.0-SNAPSHOT.jar -n "Martin"

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
evCLIapp> -n Martin
evCLIapp> exit
```