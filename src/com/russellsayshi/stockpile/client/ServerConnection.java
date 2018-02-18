package com.russellsayshi.stockpile.client;

import com.russellsayshi.stockpile.inventory.*;
import com.russellsayshi.stockpile.server.Server;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.io.*;
import java.net.*;

/**
 * This class does all of the backend work
 * for a connection to the server.
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class ServerConnection {
	/**
	 * Sent to state change listeners
	 * to let them know what has changed about
	 * the server.
	 *
	 * @author Russell Coleman
	 * @version 1.0.0
	 */
	public enum State {
		CONNECTED,
		DISCONNECTED,
		ERROR
	}

	private String hostname;
	private int port;
	private Thread serverListenerThread;
	private BufferedReader serverReader;
	private ReentrantLock serverReadLock = new ReentrantLock();
	private Socket socket;
	private PrintWriter serverWriter; //only to be accessed with below lock
	private ReentrantLock serverWriteLock = new ReentrantLock();
	private Vector<ServerUpdateListener> remoteUpdateListeners = new Vector<>();
	private Vector<ServerStateChangeListener> stateChangeListeners
		= new Vector<>();
	public static final int DEFAULT_PORT = Server.PORT;

	/**
	 * Constructs a connection instance
	 * without connecting.
	 *
	 * @param hostname The server hostname
	 * @param port The server port
	 */
	public ServerConnection(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	/**
	 * Constructs a connection instance,
	 * without connecting and with
	 * the default port.
	 *
	 * @param hostname The server hostname.
	 */
	public ServerConnection(String hostname) {
		this(hostname, DEFAULT_PORT);
	}

	/**
	 * Takes a consumer and notifies said consumer
	 * whenever the server changes state, by telling
	 * it the new state and maybe some additional info.
	 *
	 * @param callback The consumer function
	 */
	public void addStateChangeListener(ServerStateChangeListener callback) {
		stateChangeListeners.add(callback);
	}

	/**
	 * Notifies all state change listeners of
	 * a change in state.
	 *
	 * @param state New state
	 * @param info Optional additional info.
	 */
	private void notifyStateChangeListeners(State state, Optional<String> info) {
		for(ServerStateChangeListener stateChangeListener : stateChangeListeners) {
			stateChangeListener.stateChanged(state, info);
		}
	}

	/**
	 * Notifies all remote update listeners
	 * of a remote update.
	 *
	 * @param update The update from the server.
	 */
	private void notifyRemoteUpdateListeners(String update) {
		for(ServerUpdateListener serverUpdateListener : remoteUpdateListeners) {
			serverUpdateListener.update(update);
		}
	}

	/**
	 * Takes a consumer and notifies said consumer
	 * whenever the server sends data, by giving
	 * it said data.
	 *
	 * @param callback The consumer function
	 */
	public void addRemoteUpdateListener(ServerUpdateListener callback) {
		remoteUpdateListeners.add(callback);
	}

	/**
	 * Sends an update to the server.
	 *
	 * @param update The update to send
	 */
	public void update(String update) {
		if(update == null) {
		       throw new NullPointerException("Update to server cannot be null.");
		} else if(socket == null || socket.isClosed()) {
			throw new IllegalStateException("Cannot update an invalid socket.");
		}
		serverWriteLock.lock();
		try {
			serverWriter.println(update);
		} finally {
			serverWriteLock.unlock();
		}
	}

	/**
	 * Connects to the server
	 * and opens the appropriate thread.
	 *
	 * Returns the initial database from the
	 * server as a list of strings.
	 */
	public List<String> connectAndFetchDatabase() throws IOException {
		if(socket != null) throw new IllegalStateException("Cannot connect more than once.");
		socket = new Socket(hostname, port);
		serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		serverReadLock.lock();
		ArrayList<String> ret = new ArrayList<>();
		try {
			String read = serverReader.readLine();
			if(!read.equals("ACK_STOCKPILE_SERVER")) {
				//something is wrong. we're not connected
				//to a stockpile server.

				//close the socket if we can
				try {
					if(!socket.isClosed()) socket.close();
				} catch(IOException ioe) {
					//ignore it
				}

				throw new IOException("Handshake with server failed.");
			}
			
			//Read from the server its current database
			while(!((read = serverReader.readLine()).equals("BULK_DONE"))) {
				ret.add(read);
			}
		} finally {
			serverReadLock.unlock();
		}
		serverWriter = new PrintWriter(socket.getOutputStream(), true);
		serverListenerThread = new Thread(() -> {
			try {
				String read;
				while(true) {
					read = serverReader.readLine();
					//check if data is valid
					if(read == null) {
						//it's not valid, kill the server
						notifyStateChangeListeners(State.DISCONNECTED, Optional.empty());
						try {
							if(!socket.isClosed()) socket.close();
						} catch(IOException ioe) {
							ioe.printStackTrace();
						}
						break;
					}

					//it is valid! go for it!
					notifyRemoteUpdateListeners(read);
				}
			} catch(IOException ioe) {
				notifyStateChangeListeners(State.ERROR, Optional.of(ioe.getMessage()));
				ioe.printStackTrace();
			} finally {
				try {
					if(!socket.isClosed()) socket.close();
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
			}
		});
		serverListenerThread.start();
		return ret;
	}
}
