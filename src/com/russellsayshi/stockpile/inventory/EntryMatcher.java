package com.russellsayshi.stockpile.inventory;

/**
 * Determines if entries match a search
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class EntryMatcher {
	/**
	 * Checks to see if an entry matches a string search
	 * query anywhere. Checks name, location, flags, etc.
	 * Not scientific, could change throughout time.
	 *
	 * @param entry The entry to search for
	 * @param searchQuery A string (MUST BE LOWERCASE) to search for.
	 * @return Whether or not it matches
	 */
	public static boolean matchesLowerCaseQuery(Entry entry, String searchQuery) {
		if(entry.getNameLower().contains(searchQuery)) return true;
		if(entry.getLocationLower().contains(searchQuery)) return true;
		if(searchQuery.contains("missing") && entry.isMissing()) return true;
		return false;
	}
}
