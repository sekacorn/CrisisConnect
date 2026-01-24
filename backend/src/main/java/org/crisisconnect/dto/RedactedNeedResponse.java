package org.crisisconnect.dto;

import lombok.Data;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.UrgencyLevel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redacted need response - default view for unauthorized users.
 * No PII, region-level location only, generalized vulnerability flags.
 */
@Data
public class RedactedNeedResponse {
    private UUID id;
    private NeedStatus status;
    private NeedCategory category;
    private String country;
    private String regionOrState;
    private UrgencyLevel urgencyLevel;
    private String generalizedVulnerabilityFlags;
    private LocalDateTime createdAt;
}
