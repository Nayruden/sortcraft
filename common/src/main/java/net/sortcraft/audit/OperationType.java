package net.sortcraft.audit;

/**
 * Type of sorting operation being audited.
 */
public enum OperationType {
    /**
     * An actual sort operation that moves items.
     */
    SORT,

    /**
     * A preview operation that simulates sorting without moving items.
     */
    PREVIEW
}

