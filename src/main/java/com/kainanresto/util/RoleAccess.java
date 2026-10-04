package com.kainanresto.util;

import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.User;

public final class RoleAccess {
    private RoleAccess() {}

    public static boolean canUseBoth(User user) {
        return user != null && (user.getRole() == AccountRole.MANAGER
                || user.getRole() == AccountRole.SUPERVISOR);
    }
}