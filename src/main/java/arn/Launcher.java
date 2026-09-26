package arn;

import javafx.application.Application;

/**
 * Launches the packaged application without requiring a separate JavaFX installation.
 */
public final class Launcher {
    private static final String USAGE = "Usage: java -jar Arn.jar [--cli | --help | --version]";

    private Launcher() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Application.launch(Arn.class, args);
        } else if (args.length == 1 && "--cli".equals(args[0])) {
            Arn.main(new String[0]);
        } else if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println(USAGE);
            System.out.println("With no options, opens the desktop application. Use --cli for the console.");
        } else if (args.length == 1 && "--version".equals(args[0])) {
            String version = Launcher.class.getPackage().getImplementationVersion();
            System.out.println("Arn " + (version == null ? "development" : version));
        } else {
            if (args.length > 1) {
                System.err.println("Expected at most one launch option, but received " + args.length
                        + ". Use --cli, --help, or --version on its own, or no options for the desktop application.");
            } else {
                String option = args[0].replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
                System.err.println("Unknown launch option \"" + option
                        + "\". Use --cli for the console, --version for the version, or --help for usage.");
            }
            System.err.println(USAGE);
            System.exit(2);
        }
    }
}
