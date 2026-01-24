package org.crisisconnect.model.enums;

/**
 * User roles with escalating privileges.
 * RBAC enforcement happens at service layer and controller level.
 */
public enum UserRole {
    /**
     * Beneficiary - highly restricted, can only view their own case
     */
    BENEFICIARY,

    /**
     * Field worker - can create needs, view their own submissions
     */
    FIELD_WORKER,

    /**
     * NGO staff - can claim and manage needs in their service area
     */
    NGO_STAFF,

    /**
     * Administrator - full system access
     */
    ADMIN
}
