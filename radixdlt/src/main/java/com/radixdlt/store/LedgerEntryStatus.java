package com.radixdlt.store;

/**
 * Status of an {@link LedgerEntry} in Tempo consensus
 */
public enum LedgerEntryStatus {
	/**
	 * The atom is unavailable.
	 */
	UNAVAILABLE,

	/**
	 * The atom is available, pending but not yet committed.
	 */
	PENDING,

	/**
	 * The atom is available and irreversibly committed.
	 */
	COMMITTED
}