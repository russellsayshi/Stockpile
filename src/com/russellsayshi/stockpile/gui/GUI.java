package com.russellsayshi.stockpile.gui;

import com.russellsayshi.stockpile.inventory.*;
import com.russellsayshi.stockpile.client.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.*;
import java.io.*;

/**
 * Stockpile class
 * @author Russell Coleman
 * @version 1.0.0
 */
public class GUI {
	private static final String ICON_PATH = "icon.png";
	private JFrame frame;
	private JTextField searchBox;
	private JList<Entry> entryJList = new JList<>();
	private List<Entry> entryList = new ArrayList<>();
	private EntryListModel listModel = new EntryListModel(entryList);
	private ServerConnection connectionToServer = new ServerConnection("localhost");
	private JLabel serverStatus;

	/**
	 * Entry point of the application. Creates a GUI.
	 *
	 * @param args Command-line arguments to the program
	 */
	public static void main(String[] args) {
		new GUI();
	}

	/**
	 * Attempts to set the icon of frame.
	 * If not, no harm done. Should not throw
	 * unless extreme circumstance occurs.
	 *
	 * @return Whether or not setting the icon
	 *  was successful.
	 */
	private boolean setFrameIcon() {
		if(frame == null) return false;
		File icon = new File(ICON_PATH);
		if(!icon.exists()) return false;

		ImageIcon iicon = new ImageIcon(ICON_PATH);
		frame.setIconImage(iicon.getImage());
		return true;
	}

	/**
	 * Initializes the list with the stored items.
	 * Only call it once.
	 */
	private void connectToServerAndPopulateList() {
		if(entryList.size() > 0) throw new IllegalStateException("Cannot populate list again.");
		try {
			List<String> serverDatabase = connectionToServer.connectAndFetchDatabase();
			for(String s : serverDatabase) {
				entryList.add(new Entry(s));
			}
			listModel.fireDataChanged();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Initializes server change/update handlers.
	 */
	private void setupServerHandlers() {
		connectionToServer.addServerStateChangeListener((a, b) -> handleServerStateChange(a, b));
	}

	/**
	 * Handles state changes from the server.
	 * Just lets the user know and then ignores it.
	 *
	 * @param state The new state of the server
	 * @param info Optional additional info about the state change
	 */
	private void handleServerStateChange(ServerConnection.State state,
			Optional<String> info) {
		String additionalInfoString = "";
		if(info.isPresent()) additionalInfoString = ": " + info.get();
		switch(state) {
			case CONNECTED:
				serverStatus.setForeground(Color.GREEN);
				serverStatus.setText("Connected!" + additionalInfoString);
				break;
			case DISCONNECTED:
				serverStatus.setForeground(Color.BLACK);
				serverStatus.setText("Disconnected!" + additionalInfoString);
				break;
			case ERROR:
				serverStatus.setForeground(Color.RED);
				serverStatus.setText("Error with server" + additionalInfoString);
				break;
		}
	}

	/**
	 * Constructs a frame to view the data
	 */
	public GUI() {
		//Build frame
		frame = new JFrame("Stockpile");

		//Add components to frame
		JPanel panel = new JPanel(new BorderLayout());
		frame.setContentPane(panel);
		panel.add((searchBox = new JTextField("")), BorderLayout.NORTH);
		panel.add(entryJList, BorderLayout.CENTER);
		panel.add((serverStatus = new JLabel("No connection.")), BorderLayout.SOUTH);

		//Show frame
		setFrameIcon();
		frame.setSize(600, 400);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		//Cool cool. Do stuff with server now
		setupServerHandlers();
		connectToServerAndPopulateList();
	}
}
