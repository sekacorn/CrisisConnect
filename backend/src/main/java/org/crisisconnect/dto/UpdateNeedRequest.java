package org.crisisconnect.dto;

import lombok.Data;
import org.crisisconnect.model.enums.NeedStatus;

@Data
public class UpdateNeedRequest {
    private NeedStatus status;
    private String comment;
}
