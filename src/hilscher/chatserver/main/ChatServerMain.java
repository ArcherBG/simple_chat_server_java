package hilscher.chatserver.main;

import hilscher.chatserver.serverimplementation.ChatServerImplementation;

public class ChatServerMain {

	private static String portNumStr;
	private static String maxConectionsStr;
	private static String welcomeMessage;

	private static int portNum;
	private static int maxConnections;

	public static void main(String[] args) {
		getArguments(args);

		// Trim parameters.
		if (portNumStr != null && portNumStr.length() != 0 && !portNumStr.equals("")) {
			portNum = Integer.parseInt(portNumStr.trim());
		}
		if (maxConectionsStr != null && maxConectionsStr.length() != 0 && !maxConectionsStr.equals("")) {
			maxConnections = Integer.parseInt(maxConectionsStr.trim());
		}
		if (welcomeMessage != null) {
			welcomeMessage = welcomeMessage.trim();
		}

		ChatServerImplementation chatServer = new ChatServerImplementation(portNum, maxConnections, welcomeMessage);
		chatServer.startServer();

	}

	private static void getArguments(String[] args) {
		final String portNumPrefix = "-p";
		final String maxConnectionsPrefix = "-n";
		final String welcomeMessagePrefix = "-m";

		boolean portNumArgFound = false;
		boolean maxConnectionsArgFound = false;
		boolean welcomeMessageArgFound = false;

		for (int i = 0; i < args.length; i++) {

			// Check if current arg is port argument.
			if (args[i].startsWith(portNumPrefix)) {

				// Check if the argument does not contain twice.
				if (!portNumArgFound) {

					portNumStr = args[i].substring(portNumPrefix.length());
					portNumArgFound = true;
					continue;
				} else {
					System.out.println("Error port argument is set twice");
					System.exit(1);
				}

			}

			// Check if current arg is maxConnections argument.
			if (args[i].startsWith(maxConnectionsPrefix)) {

				// Check if the argument does not contain twice.
				if (!maxConnectionsArgFound) {
					
					maxConectionsStr = args[i].substring(maxConnectionsPrefix.length());
					maxConnectionsArgFound = true;
					continue;
				} else {
					System.out.println("Error max connection argument is set twice");
					System.exit(1);
				}
			}

			// Check if current arg is welcomeMessage argument.
			if (args[i].startsWith(welcomeMessagePrefix)) {

				// Check if the argument does not contain twice.
				if (!welcomeMessageArgFound) {
				
					welcomeMessage = args[i].substring(welcomeMessagePrefix.length());
					welcomeMessageArgFound = true;
				} else {
					System.out.println("Error welcome message argument is set twice");
					System.exit(1);
				}
			}
		}
	}

}
