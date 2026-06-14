package com.smartinventory.model;

/**
 * Application user roles. ADMIN has full access; STAFF has restricted access
 * (e.g. cannot delete products or manage users).
 */
public enum Role {
    ADMIN,
    STAFF
}
