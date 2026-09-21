package com.archguard.api.persistence;

/** Identifies whether a scan analyzed a remote clone or an explicitly enabled local folder. */
public enum ScanSourceType {
    REMOTE,
    LOCAL
}
