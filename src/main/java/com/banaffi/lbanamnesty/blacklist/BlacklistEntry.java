package com.banaffi.lbanamnesty.blacklist;

import java.util.UUID;

public record BlacklistEntry(
        UUID uuid,
        String name,
        String reason,
        String addedBy,
        long addedAt
) {}

