package com.kainanresto.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
public record AccountRow(long id, String fullName, String username, String email,
                         AccountRole role, AccountStatus status,
                         LocalDateTime lastLogin, LocalDate createdDate) {}
