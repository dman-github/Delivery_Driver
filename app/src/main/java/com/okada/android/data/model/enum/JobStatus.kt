package com.okada.android.data.model.enum

enum class JobStatus {
    NEW,
    DECLINED,
    CANCELLED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED;

    // Helper method to check if the status is greater than a given threshold
    fun isActiveJob(): Boolean {
        return this.ordinal > CANCELLED.ordinal && this.ordinal < COMPLETED.ordinal // Threshold is CANCELLED (ordinal 2)
    }
}