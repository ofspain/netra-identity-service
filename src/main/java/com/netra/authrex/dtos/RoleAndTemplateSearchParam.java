package com.netra.authrex.dtos;

import com.netra.commons.requests.PagingSearchParams;
import lombok.Data;

@Data
public class RoleAndTemplateSearchParam extends PagingSearchParams {
    private Long id;
    private String name;
}
