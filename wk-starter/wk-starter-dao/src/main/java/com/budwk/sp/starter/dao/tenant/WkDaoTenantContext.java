package com.budwk.sp.starter.dao.tenant;

import java.util.function.Supplier;

public final class WkDaoTenantContext {
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    private WkDaoTenantContext() {
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    public static void setIgnoreTenant(boolean ignoreTenant) {
        IGNORE.set(ignoreTenant);
    }

    public static void clear() {
        IGNORE.remove();
    }

    public static void withoutTenant(Runnable runnable) {
        boolean previous = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            runnable.run();
        } finally {
            if (previous) {
                setIgnoreTenant(true);
            } else {
                clear();
            }
        }
    }

    public static <T> T withoutTenant(Supplier<T> supplier) {
        boolean previous = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            return supplier.get();
        } finally {
            if (previous) {
                setIgnoreTenant(true);
            } else {
                clear();
            }
        }
    }
}
