package com.russellsayshi.stockpile.client;

import java.util.Optional;

/**
 * Functional interface for when a server
 * changes state.
 */
public interface ServerStateChangeListener {
	void stateChanged(ServerConnection.State state,
			Optional<String> additionalInfo);
}
