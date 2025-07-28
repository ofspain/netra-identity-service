package com.netra.authrex.dtos;

import com.netra.commons.requests.DatedSearchParams;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IdentitySearchParam extends DatedSearchParams {

    private Boolean locked;
    private Boolean disabled;
    private String domainCode;
}
