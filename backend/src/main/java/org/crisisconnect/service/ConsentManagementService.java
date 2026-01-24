package org.crisisconnect.service;

import org.crisisconnect.model.entity.UserConsent;
import org.crisisconnect.model.enums.ConsentType;
import org.crisisconnect.repository.UserConsentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consent Management Service
 * Implements GDPR Article 7 (Conditions for consent)
 */
@Service
public class ConsentManagementService {

    @Autowired
    private UserConsentRepository userConsentRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Grant consent for a specific type.
     *
     * @param userId User ID
     * @param consentType Type of consent
     * @param consentText Full text of consent shown to user
     * @param consentVersion Version of consent document
     * @param ipAddress IP address where consent was granted
     */
    @Transactional
    public UserConsent grantConsent(UUID userId, ConsentType consentType,
                                   String consentText, String consentVersion, String ipAddress) {
        // Check if consent already exists
        UserConsent consent = userConsentRepository
                .findByUserIdAndConsentType(userId, consentType)
                .orElse(new UserConsent());

        consent.setUserId(userId);
        consent.setConsentType(consentType);
        consent.setGranted(true);
        consent.setConsentText(consentText);
        consent.setConsentVersion(consentVersion);
        consent.setGrantedAt(LocalDateTime.now());
        consent.setRevokedAt(null);
        consent.setIpAddress(ipAddress);

        UserConsent saved = userConsentRepository.save(consent);

        // Audit log
        auditService.logAction(
            userId,
            "CONSENT_GRANTED",
            "CONSENT",
            saved.getId(),
            "Consent granted: " + consentType.name(),
            ipAddress
        );

        return saved;
    }

    /**
     * Revoke consent for a specific type.
     *
     * @param userId User ID
     * @param consentType Type of consent to revoke
     * @param ipAddress IP address where consent was revoked
     */
    @Transactional
    public UserConsent revokeConsent(UUID userId, ConsentType consentType, String ipAddress) {
        UserConsent consent = userConsentRepository
                .findByUserIdAndConsentType(userId, consentType)
                .orElseThrow(() -> new RuntimeException("Consent not found"));

        consent.setGranted(false);
        consent.setRevokedAt(LocalDateTime.now());

        UserConsent saved = userConsentRepository.save(consent);

        // Audit log
        auditService.logAction(
            userId,
            "CONSENT_REVOKED",
            "CONSENT",
            saved.getId(),
            "Consent revoked: " + consentType.name(),
            ipAddress
        );

        return saved;
    }

    /**
     * Get all consents for a user.
     *
     * @param userId User ID
     * @return List of all consents
     */
    public List<UserConsent> getUserConsents(UUID userId) {
        return userConsentRepository.findByUserId(userId);
    }

    /**
     * Get active (granted) consents for a user.
     *
     * @param userId User ID
     * @return List of active consents
     */
    public List<UserConsent> getActiveConsents(UUID userId) {
        return userConsentRepository.findByUserIdAndGrantedTrue(userId);
    }

    /**
     * Check if user has granted specific consent.
     *
     * @param userId User ID
     * @param consentType Type of consent
     * @return true if consent is granted, false otherwise
     */
    public boolean hasConsent(UUID userId, ConsentType consentType) {
        return userConsentRepository.existsByUserIdAndConsentTypeAndGrantedTrue(userId, consentType);
    }

    /**
     * Update consent text/version (when terms change).
     * If user has existing consent, mark it as requiring re-consent.
     *
     * @param consentType Type of consent
     * @param newConsentText New consent text
     * @param newVersion New version number
     */
    @Transactional
    public void updateConsentTerms(ConsentType consentType, String newConsentText, String newVersion) {
        // This would typically trigger a notification to all users
        // to review and re-consent to the new terms

        // For now, we just log it
        auditService.logAction(
            null,
            "CONSENT_TERMS_UPDATED",
            "CONSENT_TYPE",
            (UUID) null,
            "Consent terms updated: " + consentType.name() + " v" + newVersion,
            null
        );
    }

    /**
     * Grant all required consents for a new user.
     *
     * @param userId User ID
     * @param ipAddress IP address
     */
    @Transactional
    public void grantDefaultConsents(UUID userId, String ipAddress) {
        // Grant essential consents for new users
        grantConsent(
            userId,
            ConsentType.DATA_PROCESSING,
            "I consent to the processing of my personal data for the purpose of using this service.",
            "1.0",
            ipAddress
        );

        grantConsent(
            userId,
            ConsentType.TERMS_OF_SERVICE,
            "I agree to the Terms of Service.",
            "1.0",
            ipAddress
        );
    }

    /**
     * Check if user needs to re-consent (e.g., terms have been updated).
     *
     * @param userId User ID
     * @param consentType Type of consent
     * @param currentVersion Current version of consent document
     * @return true if re-consent is needed
     */
    public boolean needsReconsent(UUID userId, ConsentType consentType, String currentVersion) {
        UserConsent consent = userConsentRepository
                .findByUserIdAndConsentType(userId, consentType)
                .orElse(null);

        if (consent == null || !consent.getGranted()) {
            return true;
        }

        return !currentVersion.equals(consent.getConsentVersion());
    }

    /**
     * Withdraw all consents for a user (e.g., before account deletion).
     *
     * @param userId User ID
     * @param ipAddress IP address
     */
    @Transactional
    public void withdrawAllConsents(UUID userId, String ipAddress) {
        List<UserConsent> consents = userConsentRepository.findByUserId(userId);
        for (UserConsent consent : consents) {
            if (consent.getGranted()) {
                consent.setGranted(false);
                consent.setRevokedAt(LocalDateTime.now());
                userConsentRepository.save(consent);
            }
        }

        auditService.logAction(
            userId,
            "ALL_CONSENTS_WITHDRAWN",
            "USER",
            userId,
            "All consents withdrawn",
            ipAddress
        );
    }
}
