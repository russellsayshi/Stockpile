package com.russellsayshi.stockpile.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * The purpose of this class is to fetch the file
 * handler for all of the list data. If it does not exist,
 * it will prompt the user to either create a file
 * or choose another file.
 * @author Russell Coleman
 * @version 1.0.0
 */
public class FileFetcher {
	private static final String DEFAULT_FILENAME = "entries.db";

	/**
	 * Gets the file handle
	 *
	 * @throws IOException If an I/O error occured
	 */
	public static Optional<File> getFileHandle() throws IOException {
		File defaultFile = new File(DEFAULT_FILENAME);
		if(defaultFile.exists() && defaultFile.isFile())
			return Optional.of(defaultFile);

		//if we're at this point it means the default
		//file does not exist and it's time to prompt
		//the user
		JFileChooser jfc = new JFileChooser(".");
		jfc.setSelectedFile(defaultFile);
		jfc.setDialogTitle("Pick an Entry db file or create a new one.");
		int result = jfc.showSaveDialog(null);
		if(result == JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
			if(!file.exists()) {
				file.createNewFile();
			}
			return Optional.of(file);
		} else {
			return Optional.empty();
		}
	}
}
