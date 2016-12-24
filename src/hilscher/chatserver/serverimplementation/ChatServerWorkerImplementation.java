package hilscher.chatserver.serverimplementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public final class ChatServerWorkerImplementation extends Thread {
	private static final String COMMAND_PREFIX = ":";
	private static final String CLIENT_IDENTIFICATION_COMMAND = ":meet";
	private static final String GET_ALL_CLIENTS_COMMAND = ":who";
	private static final String QUIT_COMMAND = ":quit";
	private static final String WHISPER_COMMAND = ":whisper";
	private static final String DEFAULT_THREAD_NAME = " ";

	private WorkerThreadCallback listener;
	private int id;
	private Socket clientSocket;
	private boolean isConnected;
	private String welcomeMessage;
	private boolean hasClientSetInitialName;

	private PrintWriter out;
	private BufferedReader in;

	public ChatServerWorkerImplementation(int id, Socket clientSocket, String welcomeMessage,
			WorkerThreadCallback listener) {
		this.id = id;
		this.clientSocket = clientSocket;
		this.welcomeMessage = welcomeMessage;
		this.listener = listener;
		this.isConnected = false;
		this.hasClientSetInitialName = false;
		// Change the build in name of thread to default name.
		this.setName(DEFAULT_THREAD_NAME);
	}

	/**
	 * Use this method instead this.getId() to get the id of the thread.
	 * 
	 * @return id of the thread provided when on creation
	 */
	public int getThreadId() {
		return id;
	}

	public void run() {
		printMessageOnConsole("A new client : " + id + " has connected to the Server");

		try {
			// Open send and receive streams to client.
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			isConnected = true;

			sendMessageToClient(welcomeMessage);

			// Wait client to set valid name before allowing him to chat
			readClientInputUntilValidNameIsSet();

			String msgFromClient;
			try {
				while (true) {

					if ((msgFromClient = in.readLine()) != null)
						// Check if the message is command or simple chat
						// message
						if (isMessageFromClientCommant(msgFromClient)) {
						handleCommandFromClient(msgFromClient);
						} else {
						// Tell others that client said smth and tell them who it was
						listener.sayToEveryoneExcludingMyself(id, msgFromClient);
						}
				}
			} catch (IOException e) {
					// Suppress exception if  trying to readfrom closed stream
			}

		} catch (IOException e) {
			printMessageOnConsole("Error opening send and receive streams to client!");
			e.printStackTrace();
		} finally {
			listener.disconnectClient(id); // Tell the server to remove this connection from list
			out.close();
			try {
				in.close();
			} catch (IOException e) {
				printMessageOnConsole("Error closing input stream to client.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method reads client messages until a valid name is set for the given
	 * client/
	 * 
	 * @throws IOException
	 */
	private void readClientInputUntilValidNameIsSet() throws IOException {
		while (!hasClientSetInitialName) {
			String identificationCommand;
			if ((identificationCommand = in.readLine()) != null) {

				if (identificationCommand.startsWith(CLIENT_IDENTIFICATION_COMMAND)) {
					handleClientIdentificationCommand(identificationCommand);
				} else {
					sendMessageToClient("Invalid name or Name is already taken. Try with another name.");
					// printMessageOnConsole("Received command: " +
					// identificationCommand);
				}
			}
		}
	}

	private void handleCommandFromClient(String command) throws IOException {

		if (command.startsWith(COMMAND_PREFIX)) {

			// Check what type of command it is
			if (command.startsWith(CLIENT_IDENTIFICATION_COMMAND)) {

				// Client wants to change name so set the flag to false;
				handleClientIdentificationCommand(command);
			} else if (command.startsWith(GET_ALL_CLIENTS_COMMAND)) {

				handleGetAllClientsCommand();
			} else if (command.startsWith(QUIT_COMMAND)) {

				handleQuitCommand();
			} else if (command.startsWith(WHISPER_COMMAND)) {

				handleWhisperCommand(command);
			}
		} else {
			// Tell the client that command is not supported
			sendMessageToClient("Unknown command!");
			printMessageOnConsole("Server: Unknown command. ");
		}
	}

	/**
	 * Tries to set name of client. If success - changes hasClientSetName flag
	 * to true.
	 * 
	 * @param command
	 *            - message from client
	 */
	private void handleClientIdentificationCommand(String command) {

		// Get the client name from the command.
		String clientName = command.substring(CLIENT_IDENTIFICATION_COMMAND.length());
		// Check is client name is not already taken.
		boolean isNameTaken = listener.isClientNameTaken(clientName);
		if (isNameTaken) {
			// Inform the user
			sendMessageToClient("Name is already taken. Try with another name.");
		} else {

			// Save name
			this.setName(clientName);
			hasClientSetInitialName = true;
			//printMessageOnConsole("Test: Server: " + id + " saved name : " + clientName);
		}
	}

	/*
	 * Get all active client names
	 */
	private void handleGetAllClientsCommand() {

		ArrayList<String> clientNamesList = listener.getAllClientNames();
		// Append all names to response message
		StringBuilder builder = new StringBuilder();
		builder.append("The names are: ");

		for (int i = 0; i < clientNamesList.size(); i++) {
			// If it is the last name do not add separator
			if (i == clientNamesList.size()) {
				builder.append(clientNamesList.get(i));
			}
			builder.append(clientNamesList.get(i) + "  ---  ");
		}

		String resposeMessage = builder.toString();

		// Send to client
		sendMessageToClient(resposeMessage);
	}

	/*
	 * Closes socket and tells the server to remove him
	 */
	private void handleQuitCommand() {

		listener.disconnectClient(id);
		try {
			clientSocket.close();
		} catch (IOException e) {
			printMessageOnConsole("Error closing client socket with id: " + id);
			e.printStackTrace();
		}
		isConnected = false;

	}

	/*
	 * Send message only to client with given name
	 */
	private void handleWhisperCommand(String command) {

		String clientListenerNameAndMessage = command.substring(WHISPER_COMMAND.length());

		// Split by the first ocurence of ','
		String[] messageArray = clientListenerNameAndMessage.split(",{1,}+");

		int arraySize = 2; // First is name , second - message
		if (messageArray.length == arraySize && messageArray[0] != null) {
			String listenerName = messageArray[0];
			String message = messageArray[1];
			// printMessageOnConsole("Name : " + listenerName + " message: " +
			// message);
			listener.whisper(id, listenerName, message);
		} else {
			// Tell the client that the command was not valid
			sendMessageToClient("Invalid command!");
		}
	}

	private boolean isMessageFromClientCommant(String message) {
		if (message.startsWith(COMMAND_PREFIX)) {
			return true;
		}
		return false;
	}

	public void printMessageOnConsole(String message) {
		System.out.println(message);
	}

	public void sendMessageToClient(String message) {
		if (isConnected) {
			out.println(message);
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	/*
	 * Callback interface
	 */
	public interface WorkerThreadCallback {

		boolean isClientNameTaken(String name);

		/*
		 * Return a list with all names currently in use by other clients
		 */
		ArrayList<String> getAllClientNames();

		/*
		 * Send message to every client who is connected
		 */
		void sayToEveryoneExcludingMyself(int id, String message);

		/*
		 * Send message to specific client with given name
		 */
		void whisper(int speakerId, String listenerName, String message);

		/*
		 * Remove client
		 */
		void disconnectClient(int id);

	}

}
