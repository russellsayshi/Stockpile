package com.russellsayshi.stockpile.client;

import java.util.Optional;

/**
 * Functional interface for when a server
 * sends out an update.
 */
public interface ServerUpdateListener {
	void update(String message);
}
