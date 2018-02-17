package com.russellsayshi.stockpile.gui;

import javax.swing.*;
import java.util.*;
import com.russellsayshi.stockpile.inventory.*;

/**
 * A ListModel for use by the GUI class
 * to display in the JList
 * 
 * @author Russell Coleman
 * @version 1.0.0
 */
public class EntryListModel extends AbstractListModel<Entry> {
	protected List<Entry> list;

	/**
	 * Constructs the model with an underlying list
	 *
	 * @param list The underlying list
	 */
	public EntryListModel(List<Entry> list) {
		this.list = list;
	}

	/**
	 * Adds an element to the underlying list
	 * and updates the contents
	 *
	 * @param element The Entry to add to the list
	 */
	public void addElement(Entry element) {
		list.add(element);
		int addedLoc = list.size();
		fireContentsChanged(element, addedLoc, addedLoc);
	}

	/**
	 * Refreshes the entire contents of the list
	 */
	public void fireDataChanged() {
		int listSize = list.size();
		fireContentsChanged(this, listSize, listSize);
	}

	/**
	 * Returns the size of the underlying list
	 *
	 * @return The list size
	 */
	public int getSize() {
		return list.size();
	}

	/**
	 * Gets a certain entry from the list
	 *
	 * @param index The entry to grab the item from
	 * @return The entry at that index.
	 */
	@Override
	public Entry getElementAt(int index) {
		return list.get(index);
	}
}
