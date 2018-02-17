package com.russellsayshi.stockpile.inventory;

import java.io.*;

/**
 * Contains a single stockpile entry
 *
 * @author Russell Coleman
 * @version 1.0.0
 */
public class Entry implements Comparable<Entry> {
	//Both nameLower and locationLower serve to
	//cache lowercase versions of the name and location
	//for easier searching
	private String name;
	private transient String nameLower;
	private String location;
	private transient String locationLower;
	private int flags;

	/**
	 * Constructs a new entry with a name, location, and flags.
	 *
	 * @param name The name
	 * @param location The location of the item
	 * @param flags The flags associated with the item
	 */
	public Entry(String name, String location, int flags) {
		this.name = name;
		this.nameLower = name.toLowerCase();
		this.location = location;
		this.locationLower = location.toLowerCase();
		this.flags = flags;
	}

	/**
	 * Copy constructor for this Entry
	 *
	 * @param Entry The entry to copy from
	 */
	public Entry(Entry other) {
		this.name = other.name;
		this.nameLower = other.nameLower;
		this.location = other.location;
		this.flags = other.flags;
	}

	/**
	 * Checks the flags to see if the item is missing
	 *
	 * @return Whether or not it is missing
	 */
	public boolean isMissing() {
		return (flags & 1) != 0;
	}

	/**
	 * Sets whether or not an entry is missing
	 *
	 * @param missing Whether or not the entry should
	 *  be marked missing
	 */
	public void setMissing(boolean missing) {
		this.flags = flags & (~1) | (missing ? 1 : 0);
	}

	/**
	 * Sets the name of the object
	 *
	 * @param name The new name
	 */
	public void setName(String name) {
		this.name = name;
		this.nameLower = name.toLowerCase();
	}

	/**
	 * Sets the location of this object.
	 *
	 * @param location The new location
	 */
	public void setLocation(String location) {
		this.location = location;
		this.locationLower = location.toLowerCase();
	}

	/**
	 * Getter method for the name of the item
	 *
	 * @return The name of the item
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getter method for the location of the item
	 *
	 * @return The location of the item
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Gets the cached lower case location.
	 *
	 * @return The location in lower case
	 */
	public String getLocationLower() {
		return locationLower;
	}

	/**
	 * Gets the cached lower case name.
	 *
	 * @return The name in lower case
	 */
	public String getNameLower() {
		return nameLower;
	}

	/**
	 * <code>toString()</code> implementation that yields
	 * name and location
	 *
	 * @return The string representation
	 */
	@Override
	public String toString() {
		return "Entry[name='" + name + "' location='" + location + "']";
	}

	/**
	 * Sum of the hashes of name, location, and flags
	 *
	 * @return The hashcode
	 */
	@Override
	public int hashCode() {
		return name.hashCode() + location.hashCode() + flags;
	}

	/**
	 * Compares two Entries for equality
	 *
	 * @return Whether or not two objects are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Entry)) return false;
		Entry other = (Entry)obj;
		return name.equals(other.name)
		    && location.equals(other.location)
		    && flags == other.flags;
	}

	/**
	 * Compares two Entries
	 *
	 * @return Integer difference between two entries
	 */
	@Override
	public int compareTo(Entry other) {
		//See if the names are the same
		int namediff = name.compareTo(other.name);
		if(namediff != 0) return namediff;

		//If the names have no difference, check location
		int locationdiff = location.compareTo(other.location);
		if(locationdiff != 0) return locationdiff;

		//Return the difference between the flags otherwise
		return flags - other.flags;
	}

	/**
	 * Returns a String that we can construct another
	 * Entry from.
	 * Of the form <code>nameLength|flags|namelocation</code>
	 *
	 * @return The string representation of Entry
	 */
	public String getAbsoluteRepresentation() {
		StringBuilder ret = new StringBuilder();
		int nameLen = name.length();
		ret.append(Integer.toHexString(nameLen));
		ret.append("|");
		ret.append(Integer.toHexString(flags));
		ret.append("|");
		ret.append(name);
		ret.append(location);
		return ret.toString();
	}

	/**
	 * Constructs an entry from an absolute string representation
	 * yielded by the method <code>getAbsoluteRepresentation()</code>.
	 * I prefer this over serializing.
	 *
	 * @see Entry#getAbsoluteRepresentation()
	 */
	public Entry(String absoluteRepresentation) {
		String[] parts = absoluteRepresentation.split("\\|");
		if(parts.length != 3) {
			throw new IllegalArgumentException("Invalid representation string for Entry");
		}
		try {
			int nameLen = Integer.parseInt(parts[0], 16);
			int flags = Integer.parseInt(parts[1], 16);

			String namelocation = parts[2];
			String name = namelocation.substring(0, nameLen);
			String location = namelocation.substring(nameLen);

			//WE GOT IT ALL. The rest of this should be nonthrowing.
			this.name = name;
			this.location = location;
			this.flags = flags;
			this.nameLower = name.toLowerCase();
			this.locationLower = location.toLowerCase();
			//Done constructing class!
		} catch(NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid number in absolute representation.");
		} catch(IndexOutOfBoundsException ioobe) {
			throw new IllegalArgumentException("Invalid name length in string representation");
		}
	}
}
