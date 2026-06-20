package com.evolveum.cli;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.cli.command.ConfigInitCommand;
import com.evolveum.cli.command.GetUserCommand;
import com.evolveum.cli.command.ModifyUserCommand;
import com.evolveum.cli.command.SearchUsersCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "evCLIapp", mixinStandardHelpOptions = true, version = "1.0",
        description = "Midpoint admin CLI APP",
        subcommands = { 
            CommandLine.HelpCommand.class,
            ConfigInitCommand.class,
            GetUserCommand.class,
            ModifyUserCommand.class,
            SearchUsersCommand.class
        })
public class App implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Application started with args: {}", (Object) args);
        if (args.length > 0) {
            int exitCode = new CommandLine(new App()).execute(args);
            System.exit(exitCode);
        } else {
            startInteractiveShell();
        }
    }

    private static void startInteractiveShell() {
        try {
            logger.info("Starting interactive shell mode");
            System.out.println("Starting interactive mode...");
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            CommandLine cmd = new CommandLine(new App());
            Parser parser = new DefaultParser();

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new picocli.shell.jline3.PicocliJLineCompleter(cmd.getCommandSpec()))
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50)
                    .build();

            System.out.println("Type 'help' to see available commands, 'exit' or 'quit' to close.");

            while (true) {
                String line;
                try {
                    line = reader.readLine("evCLIapp> ");
                } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                    // Handle Ctrl-C / Ctrl-D gracefully
                    break;
                }

                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                ParsedLine pl = parser.parse(line, 0);
                String[] arguments = pl.words().toArray(new String[0]);

                if (arguments.length > 0 && (arguments[0].equalsIgnoreCase("exit") || arguments[0].equalsIgnoreCase("quit"))) {
                    break;
                }

                new CommandLine(new App()).execute(arguments);
            }
        } catch (Exception e) {
            System.err.println("Failed to start the interactive shell: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Welcome to evolveumCLIapp! Type --help to see available commands.");
        return 0;
    }
}

