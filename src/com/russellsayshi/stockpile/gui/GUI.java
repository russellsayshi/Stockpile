package com.russellsayshi.stockpile.gui;

import com.russellsayshi.stockpile.inventory.*;
import javax.swing.*;
import java.awt.BorderLayout;
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
	 * Initializes the list with the stored items
	 * 
	 * @return was successful
	 */
	private boolean populateList() {
		try {
			Optional<File> listFile = FileFetcher.getFileHandle();
			if(!listFile.isPresent()) {
				//the user did not pick a file
				return false;
			}

			File file = listFile.get();
			try(Scanner scan = new Scanner(file)) {
				while(scan.hasNextLine()) {
					entryList.add(new Entry(scan.nextLine()));
				}
			} catch(FileNotFoundException fnfe) {
				fnfe.printStackTrace();
				return false;
			} finally {
				listModel.fireDataChanged();
				return true;
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
			return false;
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
		if(!populateList()) System.exit(1);
		panel.add(entryJList, BorderLayout.CENTER);

		//Show frame
		setFrameIcon();
		frame.setSize(600, 400);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
