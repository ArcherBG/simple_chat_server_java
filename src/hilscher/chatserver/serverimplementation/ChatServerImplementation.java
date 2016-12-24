package hilscher.chatserver.serverimplementation;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import hilscher.chatserver.interfaces.ChatServer;
import hilscher.chatserver.serverimplementation.ChatServerWorkerImplementation.WorkerThreadCallback;

public final class ChatServerImplementation implements ChatServer, WorkerThreadCallback {
	private static final String DEFAULT_THREAD_NAME = " ";
	private static final int DEFAULT_PORT_NUMBER = 3456;
	private static final String GUEST_NAME = "Guest";

	private int portNum;
	private int maxConnections;
	private String welcomeMessage;

	private AtomicInteger connectionsCount;
	private List<ChatServerWorkerImplementation> workerThreadsArray;

	public ChatServerImplementation(int portNum, int maxConnections, String welcomeMessage) {

		// Check if provided port num is between the min and max port range
		if (portNum > 0 && portNum < 65545) {
			this.portNum = portNum;
		} else {
			this.portNum = DEFAULT_PORT_NUMBER;
		}

		if (maxConnections > 0 && maxConnections < Integer.MAX_VALUE) {
			this.maxConnections = maxConnections;
		} else {
			printMessageOnConsole("Error. Max connections must be between 0 and ~ 2 000 000 000");
			System.exit(1);
		}

		if (welcomeMessage != null && !welcomeMessage.equals("")) {
			this.welcomeMessage = welcomeMessage;
		} else {
			printMessageOnConsole("Error.  You must set a default welcome message!");
			System.exit(1);
		}

		connectionsCount = new AtomicInteger(0);
		workerThreadsArray = Collections.synchronizedList(new ArrayList<>());
	}

	public void startServer() {
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(portNum);
			printMessageOnConsole("Server is listening on port: " + portNum);

			boolean listening = true;
			while (listening) {
				if (connectionsCount.get() <= maxConnections) {
					int id = generateId();
					ChatServerWorkerImplementation workerThread = new ChatServerWorkerImplementation(id,
							serverSocket.accept(), welcomeMessage, this);
					workerThread.start();
					workerThreadsArray.add(workerThread);
					connectionsCount.incrementAndGet();
				} else {
					printMessageOnConsole("Max number of connections reached:  " + maxConnections);
					// TODO handle max number of connections reached case.
				}
			}

		} catch (IOException e) {
			System.out.println("Error could not listen on port : " + portNum);
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("Error closing serverSocket");
				e.printStackTrace();
			}
		}
	}

	// generates unique id for that server
	private int generateId() {
		int id = 0;
		for (int i = 0; i < workerThreadsArray.size(); i++) {
			if (workerThreadsArray.get(i).getThreadId() == id) {
				id++;
				// Reset and start searching from the begging of the list
				i = 0;
			}
		}
		return id;

	}

	private void printMessageOnConsole(String message) {
		System.out.println(message);
	}

	/*
	 * Get the name of the Thread with given id from workerThreadsArray
	 */
	private String getClientName(int id) {
		// Get name of client if it has one. If not use "Someone".
		String clientName = "Someone ";
		for (int i = 0; i < workerThreadsArray.size(); i++) {

			// check for client match
			if (workerThreadsArray.get(i).getThreadId() == id) {

				// Check is it has been set with name
				if ((workerThreadsArray.get(i).getName() != null)
						&& !(workerThreadsArray.get(i).getName().equals(DEFAULT_THREAD_NAME))) {

					clientName = workerThreadsArray.get(i).getName();
				} else {
					clientName = clientName + id;
				}
			}
		}
		return clientName;
	}

	/*
	 * Callback methods are implemented below.
	 */
	@Override
	public synchronized boolean isClientNameTaken(String name) {

		for (ChatServerWorkerImplementation workerThread : workerThreadsArray) {
			if (workerThread.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void sayToEveryoneExcludingMyself(int id, String message) {
		// Get Client name
		String clientName = getClientName(id);

		// Send message to everyone except myself
		for (int i = 0; i < workerThreadsArray.size(); i++) {
			if (workerThreadsArray.get(i).getThreadId() == id) {
				continue;
			}
			ChatServerWorkerImplementation workerThread = workerThreadsArray.get(i);
			workerThread.sendMessageToClient(
					clientName + " said: "  + message);
		}

	}

	@Override
	public synchronized void disconnectClient(int id) {
		// Removes client from list of active clients
		for (int i = 0; i < workerThreadsArray.size(); i++) {
			if (workerThreadsArray.get(i).getThreadId() == id) {
				printMessageOnConsole("Client " + workerThreadsArray.get(i).getName() + " has disconnected.");
				workerThreadsArray.remove(i);
				break;
			}
		}

		// Decrease the number of connections by one
		connectionsCount.decrementAndGet();
	}

	@Override
	public synchronized ArrayList<String> getAllClientNames() {
		List<String> namesList = new ArrayList<>();
		for (ChatServerWorkerImplementation workerThread : workerThreadsArray) {
			String name = workerThread.getName();
			// If Client has connected but has not set name yet show him as
			// "Guest"
			if (name.equals(DEFAULT_THREAD_NAME)) {
				name = GUEST_NAME;
			}

			namesList.add(name);
		}
		ArrayList<String> namesListCopy = new ArrayList<>();
		namesListCopy.addAll(namesList);
		return namesListCopy;
	}

	@Override
	public synchronized void whisper(int speakerId, String listenerName, String message) {
		// Get client name from id.
		String clientName = getClientName(speakerId);

		// Find the thread we want to send message by his id
		for (int i = 0; i < workerThreadsArray.size(); i++) {
			// If client is found, send the message
			if (workerThreadsArray.get(i).getName().equals(listenerName)) {
				// printMessageOnConsole("Test Server: Whisped client : " +
				// speakerId + " to id: "+
				// workerThreadsArray.get(i).getThreadId() + " :" + message);
				ChatServerWorkerImplementation workerThread = workerThreadsArray.get(i);
				workerThread.sendMessageToClient(clientName + " said: " + message);
				break;
			}
		}
	}

}
