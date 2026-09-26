package arn;

import javafx.application.Application;

/**
 * Launches the packaged application without requiring a separate JavaFX installation.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Application.launch(Arn.class, args);
        } else if (args.length == 1 && "--cli".equals(args[0])) {
            Arn.main(new String[0]);
        } else if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Usage: java -jar Arn.jar [--cli | --help | --version]");
            System.out.println("With no options, opens the desktop application. Use --cli for the console.");
        } else if (args.length == 1 && "--version".equals(args[0])) {
            String version = Launcher.class.getPackage().getImplementationVersion();
            System.out.println("Arn " + (version == null ? "development" : version));
        } else {
            System.err.println("Unknown option. Use --help to see available options.");
            System.exit(2);
        }
    }
}

