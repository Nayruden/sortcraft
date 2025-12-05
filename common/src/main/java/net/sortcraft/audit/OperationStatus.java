package net.sortcraft.audit;

/**
 * Status of a completed sorting operation.
 */
public enum OperationStatus {
    /**
     * All items were successfully sorted.
     */
    SUCCESS,

    /**
     * Some items were sorted, but some could not be (unknown items or overflow).
     */
    PARTIAL_SUCCESS,

    /**
     * The operation failed due to an error.
     */
    FAILED
}

