package org.crisisconnect.model.enums;

/**
 * Types of user consent for GDPR compliance
 */
public enum ConsentType {
    /**
     * Consent for data processing (required)
     */
    DATA_PROCESSING,

    /**
     * Consent for communication/marketing
     */
    MARKETING,

    /**
     * Consent for data sharing with partners
     */
    DATA_SHARING,

    /**
     * Consent for analytics and tracking
     */
    ANALYTICS,

    /**
     * Consent for terms of service
     */
    TERMS_OF_SERVICE,

    /**
     * Consent for privacy policy
     */
    PRIVACY_POLICY
}
