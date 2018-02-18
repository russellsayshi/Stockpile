package com.russellsayshi.stockpile.server;

import com.russellsayshi.stockpile.inventory.Entry;
import java.util.*;

/**
 * Performs an update on a database from command strings
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class DatabaseUpdater {
	/**
	 * Parses a command string to update the
	 * database accordingly.
	 *
	 * The format of the command string is one of three:
	 * +entry
	 * -entry
	 * &gt;entrylen&gt;entry1entry2
	 *
	 * The first of which adds an entry to the database,
	 * the second of which removes said entry, and the
	 * third of which changes one entry into another.
	 *
	 * @param list The list of entries to update
	 * @param command The string to parse and update with
	 * @throws IllegalArgumentException if any of the arguments
	 *  are null, if command has 0 length, or if any entry
	 *  does not parse as it should.
	 */
	public static void updateWithString(List<Entry> list, String command) {
		if(command == null || list == null || command.length() == 0) {
			throw new IllegalArgumentException();
		}
		char instruction = command.charAt(0);
		String rest = command.substring(1);
		if(rest.length() == 0) {
			throw new IllegalArgumentException("Entry string empty.");
		}
		if(instruction == '+') {
			Entry toAdd = new Entry(rest); //parse entry from string
			list.add(toAdd);
		} else if(instruction == '-') {
			Entry toSub = new Entry(rest); //parse entry from string
			list.remove(toSub);
		} else if(instruction == '>') {
			int cutoff = rest.indexOf('>');
			if(cutoff == -1) throw new IllegalArgumentException("Invalid " +
					"move format string.");
			String entry1Length = rest.substring(0, cutoff);
			try {
				int entry1Len = Integer.parseInt(entry1Length);
				String entry1Str = rest.substring(cutoff+1, cutoff+1+entry1Len);
				String entry2Str = rest.substring(cutoff+1+entry1Len);
				Entry entry1 = new Entry(entry1Str);
				Entry entry2 = new Entry(entry2Str);
				for(int i = 0; i < list.size(); i++) {
					if(entry1.equals(list.get(i))) {
						list.set(i, entry2);
					}
				}
			} catch(NumberFormatException|IndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Corrupted move" +
						" format string.");
			}
		} else {
			throw new IllegalArgumentException(instruction + " is not a valid command.");
		}
	}
}
