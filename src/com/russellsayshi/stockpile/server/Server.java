package com.russellsayshi.stockpile.server;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * TCP Server that handles all incoming connections
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class Server implements Runnable {
	private static final int PORT = 2377;
	private LinkedBlockingQueue<String> requests = new LinkedBlockingQueue<>();
	private ConcurrentLinkedQueue<ClientConnection> clients = new ConcurrentLinkedQueue<>();
	private List<String> database = new ArrayList<String>();
	private ReentrantLock databaseLock = new ReentrantLock();
	private static final String DATABASE_FILENAME = "entries.db";

	/**
	 * Holds a socket,
	 * input/output streams,
	 * and lock object to client.
	 */
	private class ClientConnection {
		BufferedReader reader;
		PrintWriter writer;
		Socket socket;
		//Not using ReadWriteLock because we should
		//only have one reader at a time.
		ReentrantLock writeLock = new ReentrantLock();
		ReentrantLock readLock = new ReentrantLock();

		/**
		 * Basic constructor
		 *
		 * @param reader The reaer
		 * @param writer The writer
		 * @param socket The socket
		 */
		public ClientConnection(BufferedReader reader,
				PrintWriter writer,
				Socket socket) {
			this.reader = reader;
			this.writer = writer;
			this.socket = socket;
		}
	}

	/**
	 * Logs server data out to terminal.
	 *
	 * @param log What to log.
	 */
	private <T> void log(T toLog) {
		System.out.print("[SERVER] ");
		System.out.println(toLog);
	}

	/**
	 * Writes some data to a ClientConnection
	 * 
	 * @param client The client to write to
	 * @param data The string to write
	 */
	private void writeToClient(ClientConnection client, String data) {
		client.writeLock.lock();
		try {
			client.writer.println(data);
		} finally {
			client.writeLock.unlock();
		}
	}

	/**
	 * Takes a socket and does what it needs to do.
	 * e.g. handling requests, sending data.
	 * Not run on the main thread.
	 *
	 * @param socket The socket
	 */
	private void handleClient(Socket socket) {
		ClientConnection connection = new ClientConnection(
			new BufferedReader(new InputStreamReader(
					socket.getInputStream())),
			new PrintWriter(socket.getOutputStream(), true),
			socket
		);
		try {
			clients.add(connection);
			//We are the only ones that should ever hold the read
			//lock. Keep it for the life cycle of the client.
			connection.readLock.lock();
			try {
				connection.writeLock.lock();
				try {
					databaseLock.lock();
					try {
						for(String s : database) {
							connection.writer.println("+" + s);
						}
					} finally {
						databaseLock.unlock();
					}
				} finally {
					connection.writeLock.unlock();
				}
				while(true) {
					String read = connection.reader.readLine();
					//validate input
					char start = read.charAt(0);
					
					for(ClientConnection client : clients) {
						client.writeLock.lock();
						try {
							client.writer.println(read);
						} finally {
							client.writeLock.unlock();
						}
					}
					if(read.charAt(0) == "+") {
						
					}
				}
			} finally {
				connection.readLock.unlock();
			}
		} finally {
			if(socket != null && !socket.isClosed()) socket.close();
			clients.remove(connection);
		}
	}

	/**
	 * Populates the database with data.
	 */
	private void populateDatabase() {
		if(!database.empty()) return;
		File file = new File(DATABASE_FILENAME);
		if(!file.exists()) {
			file.createNewFile();
		}

		databaseLock.lock();
		try(Scanner scan = new Scanner(file)) {
			while(scan.hasNextLine()) {
				database.add(scan.nextLine());
			}
		} finally {
			databaseLock.unlock();
		}
	}

	/**
	 * Begins server thread to wait for clients.
	 */
	public void run() {
		populateDatabase();
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(PORT);
			log("Socket open on port " + PORT + ".");
			while(true) {
				Socket clientSocket = serverSocket.accept();
				log("Client found at " + clientSocket.getRemoteSocketAddress().toString());
				new Thread(() -> {
					handleClient(clientSocket);
				}).start();
			}
		} finally {
			if(serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
			for(ClientConnection client : clients) {
				if(client.socket != null && !client.socket.isClosed()) {
					client.socket.close();
				}
			}
		}
	}
}
