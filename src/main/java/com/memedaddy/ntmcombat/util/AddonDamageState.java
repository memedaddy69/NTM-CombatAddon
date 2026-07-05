package com.memedaddy.ntmcombat.util;

public class AddonDamageState {
    // Defaults to false. ThreadLocal ensures absolute safety during calculation chains.
    public static final ThreadLocal<Boolean> isNTDamage = ThreadLocal.withInitial(() -> false);
    // Your custom flag to bypass vanilla armor math
    public static final ThreadLocal<Boolean> bypassVanillaArmorThisDamage = ThreadLocal.withInitial(() -> false);
}
