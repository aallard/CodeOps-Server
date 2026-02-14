package com.codeops.dto.request;

import com.codeops.entity.enums.TeamRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull TeamRole role
) {}
