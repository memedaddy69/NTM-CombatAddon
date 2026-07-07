package com.memedaddy.ntmcombat.api;

import com.hbm.items.weapon.sedna.BulletConfig;

public interface IBulletConfigAccessor {

    boolean isBypassVanillaArmor();

    BulletConfig setBypassVanillaArmor(boolean bypass);
}
