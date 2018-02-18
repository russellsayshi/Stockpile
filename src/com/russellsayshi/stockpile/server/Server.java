package com.russellsayshi.stockpile.server;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import com.russellsayshi.stockpile.inventory.*;

/**
 * TCP Server that handles all incoming connections
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class Server implements Runnable {
	public static final int PORT = 2377;
	private ConcurrentLinkedQueue<ClientConnection> clients = new ConcurrentLinkedQueue<>();
	private List<Entry> database = new ArrayList<Entry>();
	private ReentrantLock databaseLock = new ReentrantLock();
	private static final String DATABASE_FILENAME = "entries.db";
	private volatile boolean databaseChangedSinceOnDisk = false;
	private Thread diskWritingThread;

	/**
	 * Mark database data as changed since put on disk so that
	 * it will get written to the disc at a future time.
	 */
	private void markDatabaseDirty() {
		databaseChangedSinceOnDisk = true;
	}

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
	private void handleClient(Socket socket) throws IOException {
		ClientConnection connection = new ClientConnection(
			new BufferedReader(new InputStreamReader(
					socket.getInputStream())),
			new PrintWriter(socket.getOutputStream(), true),
			socket
		);
		try {
			//We are the only ones that should ever hold the read
			//lock. Keep it for the life cycle of the client.
			connection.readLock.lock();
			clients.add(connection);
			String clientStringRepr = connection.socket.getRemoteSocketAddress().toString();
			try {
				connection.writeLock.lock();
				try {
					connection.writer.println("ACK_STOCKPILE_SERVER"); //let 'em know we're here

					//send 'em over the current database
					databaseLock.lock();
					try {
						for(Entry s : database) {
							connection.writer.println(s.getAbsoluteRepresentation());
						}
					} finally {
						databaseLock.unlock();
					}
					connection.writer.println("BULK_DONE"); //tell 'em that's all from the database
				} finally {
					connection.writeLock.unlock();
				}
				while(true) {
					String read = connection.reader.readLine();
					if(read == null) {
						log("Unable to read from client " + clientStringRepr + ". Breaking connection.");
						break;
					}
					//try and use this string to update our
					//database
					boolean isProperString = false;
					try {
						DatabaseUpdater.updateWithString(database, read);
						markDatabaseDirty();
						isProperString = true;
					} catch(IllegalArgumentException iae) {
						log("Client " + clientStringRepr + " gave an invalid database update string.");
						iae.printStackTrace();
					}
					if(isProperString) {
						databaseLock.lock();
						try {
							for(ClientConnection client : clients) {
								if(client.equals(connection)) continue;
								client.writeLock.lock();
								try {
									client.writer.println(read);
								} finally {
									client.writeLock.unlock();
								}
							}
						} finally {
							databaseLock.unlock();
						}
					}
				}
			} finally {
				connection.readLock.unlock();
			}
		} finally {
			clients.remove(connection);
			if(socket != null && !socket.isClosed()) socket.close();
		}
	}

	/**
	 * Populates the database with data.
	 */
	private void populateDatabase() throws IOException {
		if(database.size() != 0) return;
		File file = new File(DATABASE_FILENAME);
		if(!file.exists()) {
			file.createNewFile();
		}

		databaseLock.lock();
		try(Scanner scan = new Scanner(file)) {
			int line = 0;
			while(scan.hasNextLine()) {
				line++;
				try {
					database.add(new Entry(scan.nextLine()));
				} catch(IllegalArgumentException iae) {
					log("Invalid database entry on line " + line + ". Continuing...");
				}
			}
		} finally {
			databaseLock.unlock();
		}
	}

	/**
	 * Updates the disk every 15 minutes, if necessary.
	 * Meant to be called as a separate thread
	 * from within the run() method.
	 */
	private void updateDiskPeriodically() {
		try {
			while(true) {
				if(databaseChangedSinceOnDisk) {
					log("About to update database...");
					try(PrintWriter writer = new PrintWriter(
						DATABASE_FILENAME
					)) {
						databaseLock.lock();
						try {
							for(Entry e : database) {
								writer.println(
									e.getAbsoluteRepresentation());
							}
						} finally {
							databaseLock.unlock();
						}
					}
					log("Updated database on disk.");
					databaseChangedSinceOnDisk = false;
				}
				Thread.sleep(/*15 * 60 **/ 1000); //15 minutes
			}
		} catch(InterruptedException|IOException ie) {
			ie.printStackTrace();
		}
	}

	/**
	 * Begins server thread to wait for clients.
	 */
	public void run() {
		try {
			populateDatabase();
		} catch(IOException ioe) {
			System.err.println("Unable to communicate with database. Exiting...");
			ioe.printStackTrace();
			return;
		}
		if(diskWritingThread == null || !diskWritingThread.isAlive()) {
			diskWritingThread = new Thread(() -> updateDiskPeriodically());
			diskWritingThread.start();
		}
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT);
			log("Socket open on port " + PORT + ".");
			while(true) {
				try {
					Socket clientSocket = serverSocket.accept();
					log("Client found at " + clientSocket.getRemoteSocketAddress().toString());
					new Thread(() -> {
						try {
							handleClient(clientSocket);
						} catch(IOException ioe) {
							//something went wrong
							ioe.printStackTrace();
						}
					}).start();
				} catch(IOException ioe) {
					log("Unable to communicate with client.");
					ioe.printStackTrace();
				}
			}
		} catch(IOException ioe) {
			log("Error with server.");
			ioe.printStackTrace();
		} finally {
			try {
				if(serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
			for(ClientConnection client : clients) {
				if(client.socket != null && !client.socket.isClosed()) {
					try {
						client.socket.close();
					} catch(IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Entry point for server application
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		new Server().run();
	}
}
