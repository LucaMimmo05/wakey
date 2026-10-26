package it.luca.sveglia.util;

import android.os.Build;

import java.util.Locale;

/** Riconosce i produttori che limitano le app in background più di Android standard. */
public final class DeviceInfo {

    private DeviceInfo() {
    }

    /** Xiaomi, Redmi e POCO (MIUI / HyperOS). */
    public static boolean isXiaomi() {
        String maker = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.ROOT);
        return maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco");
    }

    /** Samsung (One UI), che mette in "sospensione" le app usate poco. */
    public static boolean isSamsung() {
        return Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }
}
