package com.memedaddy.ntmcombat.handler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class SRPMobAdaptReflect {

    private static final String SRP_MALLEABLE_CLASS = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityPMalleable";
    private static final String SRP_CONFIG_CLASS = "com.dhanantry.scapeandrunparasites.util.config.SRPConfig";
    private static final String SRP_SOUNDS_CLASS = "com.dhanantry.scapeandrunparasites.init.SRPSounds";

    private static boolean srpPresent = false;
    private static Class<?> clazzPMalleable;
    private static Field resistanceS;
    private static Field resistanceI;
    private static Field firemultyplier;
    private static DataParameter<Byte> HIT;
    private static Field pointCapField;
    private static Field pointReductionField;
    private static Field adaptationCapField;
    private static Field damagetypecapField;
    private static Field chanceLearnFireField;
    private static SoundEvent adaptationPSound;
    private static SoundEvent adaptationFSound;

    private static final Map<String, Float> LEARN_CHANCE_MAP = new HashMap<>();
    private static final Map<String, Float> FIRE_FAIL_MAP = new HashMap<>();

    private static final int LEARN_COOLDOWN_TICKS = 10;

    private static final WeakHashMap<EntityLivingBase, Integer> lastLearnTick = new WeakHashMap<>();
    private static final WeakHashMap<EntityLivingBase, Integer> lastCallTick = new WeakHashMap<>();

    private static final Map<UUID, Map<String, Integer>> ADAPTATION_COUNTS = new HashMap<>();

    public static boolean skipNextDamageEntityCall = false;

    static {
        LEARN_CHANCE_MAP.put("physical", 1.0f);
        LEARN_CHANCE_MAP.put("explosive", 0.8f);
        LEARN_CHANCE_MAP.put("laser", 0.6f);
        LEARN_CHANCE_MAP.put("plasma", 0.5f);
        LEARN_CHANCE_MAP.put("microwave", 0.1f);
        LEARN_CHANCE_MAP.put("electric", 0.1f);
        LEARN_CHANCE_MAP.put("subatomic", 0.0f);
        LEARN_CHANCE_MAP.put("nuclearblast", 0.0f);
        LEARN_CHANCE_MAP.put("artillery", 0.0f);
        LEARN_CHANCE_MAP.put("himars", 0.0f);

        FIRE_FAIL_MAP.put("physical", 0.0f);
        FIRE_FAIL_MAP.put("explosive", 0.0f);
        FIRE_FAIL_MAP.put("laser", 0.0f);
        FIRE_FAIL_MAP.put("plasma", 0.0f);
        FIRE_FAIL_MAP.put("microwave", 0.0f);
        FIRE_FAIL_MAP.put("electric", 0.0f);
        FIRE_FAIL_MAP.put("subatomic", 0.0f);
        FIRE_FAIL_MAP.put("nuclearblast", 0.0f);
        FIRE_FAIL_MAP.put("artillery", 0.0f);
        FIRE_FAIL_MAP.put("himars", 0.0f);

        try {
            clazzPMalleable = Class.forName(SRP_MALLEABLE_CLASS);
            resistanceS = clazzPMalleable.getDeclaredField("resistanceS");
            resistanceS.setAccessible(true);
            resistanceI = clazzPMalleable.getDeclaredField("resistanceI");
            resistanceI.setAccessible(true);

            Class<?> clazzConfig = Class.forName(SRP_CONFIG_CLASS);
            firemultyplier = clazzConfig.getDeclaredField("firemultyplier");
            firemultyplier.setAccessible(true);

            Field hitField = clazzPMalleable.getDeclaredField("HIT");
            hitField.setAccessible(true);
            HIT = (DataParameter<Byte>) hitField.get(null);

            pointCapField = clazzPMalleable.getDeclaredField("pointCap");
            pointCapField.setAccessible(true);
            pointReductionField = clazzPMalleable.getDeclaredField("pointReduction");
            pointReductionField.setAccessible(true);
            adaptationCapField = clazzPMalleable.getDeclaredField("adaptationCap");
            adaptationCapField.setAccessible(true);
            damagetypecapField = clazzPMalleable.getDeclaredField("DamageTypeCap");
            damagetypecapField.setAccessible(true);
            chanceLearnFireField = clazzPMalleable.getDeclaredField("chanceLearnFire");
            chanceLearnFireField.setAccessible(true);

            Class<?> clazzSounds = Class.forName(SRP_SOUNDS_CLASS);
            adaptationPSound = (SoundEvent) clazzSounds.getDeclaredField("ADAPTATION_P").get(null);
            adaptationFSound = (SoundEvent) clazzSounds.getDeclaredField("ADAPTATION_F").get(null);

            srpPresent = true;
        } catch (Exception e) {
            srpPresent = false;
            e.printStackTrace();
        }
    }

    public static boolean isActive() {
        return srpPresent;
    }

    public static boolean isPMalleable(EntityLivingBase entity) {
        if (!srpPresent) return false;
        return clazzPMalleable.isInstance(entity);
    }

    private static String normalizeType(String type) {
        if ("artillery".equals(type) || "himars".equals(type) || "nuclearblast".equals(type)) {
            return "explosive";
        }
        if ("electric".equals(type) || "laser".equals(type) || "plasma".equals(type)
                || "microwave".equals(type) || "subatomic".equals(type)) {
            return "energy";
        }
        return type;
    }

    private static String getAdaptKey(DamageSource source) {
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            net.minecraft.item.ItemStack held = player.getHeldItemMainhand();
            if (!held.isEmpty()) {
                net.minecraft.util.ResourceLocation rl = held.getItem().getRegistryName();
                if (rl != null) return rl.toString();
            }
            return "player_empty_hand";
        }
        if (source.getTrueSource() instanceof EntityLivingBase) {
            return source.getTrueSource().getClass().getName();
        }
        return source.getDamageType().toLowerCase(Locale.US);
    }

    private static void ensureLists(EntityLivingBase living) {
        try {
            List<String> sList = (List<String>) resistanceS.get(living);
            if (sList == null) {
                sList = new ArrayList<>();
                resistanceS.set(living, sList);
            }
            List<Integer> iList = (List<Integer>) resistanceI.get(living);
            if (iList == null) {
                iList = new ArrayList<>();
                resistanceI.set(living, iList);
            }
        } catch (Exception ignored) {
        }
    }

    private static int getResistanceCount(EntityLivingBase living, String key) {
        try {
            UUID uuid = living.getUniqueID();
            Map<String, Integer> entityAdapt = ADAPTATION_COUNTS.get(uuid);
            if (entityAdapt == null) {
                entityAdapt = new HashMap<>();
                if (srpPresent) {
                    try {
                        ensureLists(living);
                        List<String> sList = (List<String>) resistanceS.get(living);
                        List<Integer> iList = (List<Integer>) resistanceI.get(living);
                        if (sList != null && iList != null) {
                            for (int i = 0; i < sList.size() && i < iList.size(); i++) {
                                entityAdapt.put(sList.get(i), iList.get(i));
                            }
                        }
                    } catch (Exception ignored) {}
                }
                ADAPTATION_COUNTS.put(uuid, entityAdapt);
            }
            Integer val = entityAdapt.get(key);
            return (val != null) ? val : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void setResistanceCount(EntityLivingBase living, String key, int count) {
        try {
            UUID uuid = living.getUniqueID();
            Map<String, Integer> entityAdapt = ADAPTATION_COUNTS.get(uuid);
            if (entityAdapt == null) {
                entityAdapt = new HashMap<>();
                ADAPTATION_COUNTS.put(uuid, entityAdapt);
            }
            entityAdapt.put(key, count);
        } catch (Exception ignored) {
        }
    }

    public static void setHIT(EntityLivingBase living, byte value) {
        try {
            living.getDataManager().set(HIT, value);
        } catch (Exception ignored) {
        }
    }

    public static void refreshHIT(EntityLivingBase living, DamageSource source) {
        if (!srpPresent || !clazzPMalleable.isInstance(living)) {
            setHIT(living, (byte) 0);
            return;
        }
        String type = source.getDamageType().toLowerCase(Locale.US);
        Float learnChance = LEARN_CHANCE_MAP.get(type);
        if (learnChance == null || learnChance <= 0) {
            setHIT(living, (byte) 0);
            return;
        }
        String adaptKey = getAdaptKey(source);
        int count = getResistanceCount(living, adaptKey);
        if (count <= 0) {
            setHIT(living, (byte) 0);
            return;
        }
        int pointCap = getPointCap(living);
        if (count <= pointCap) {
            setHIT(living, (byte) 1);
        } else {
            setHIT(living, (byte) 2);
        }
    }

    private static int getPointCap(EntityLivingBase living) {
        try {
            return pointCapField.getInt(living);
        } catch (Exception e) {
            return 19;
        }
    }

    private static float getPointReduction(EntityLivingBase living) {
        try {
            return pointReductionField.getFloat(living);
        } catch (Exception e) {
            return 0.1f;
        }
    }

    private static float getAdaptationCap(EntityLivingBase living) {
        try {
            return adaptationCapField.getFloat(living);
        } catch (Exception e) {
            return 1.0f;
        }
    }

    private static int getDamageTypeCap(EntityLivingBase living) {
        try {
            return damagetypecapField.getInt(living);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private static float getChanceLearnFire(EntityLivingBase living) {
        try {
            return chanceLearnFireField.getFloat(living);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private static void playAdaptationSound(EntityLivingBase living, SoundEvent sound) {
        try {
            float pitch = 1.0F + (living.getRNG().nextFloat() - living.getRNG().nextFloat()) * 0.2F;
            living.world.playSound(null, living.posX, living.posY, living.posZ, sound, SoundCategory.HOSTILE, 1.0F, pitch);
        } catch (Exception ignored) {
        }
    }

    public static float applyAdaptation(EntityLivingBase living, DamageSource source, float amount) {
        if (!srpPresent || !clazzPMalleable.isInstance(living) || amount <= 0) {
            return amount;
        }

        Integer lastCall = lastCallTick.get(living);
        if (lastCall != null && lastCall == living.ticksExisted) {
            return amount;
        }
        lastCallTick.put(living, living.ticksExisted);

        String type = source.getDamageType().toLowerCase(Locale.US);
        if (type.isEmpty()) {
            return amount;
        }

        if (type.equals("fire")) {
            try {
                return amount * firemultyplier.getFloat(null);
            } catch (Exception e) {
                return amount;
            }
        }

        Float learnChance = LEARN_CHANCE_MAP.get(type);
        if (learnChance == null || learnChance <= 0) {
            return amount;
        }

        String normalizedType = normalizeType(type);
        String adaptKey = getAdaptKey(source);

        int pointCap = getPointCap(living);
        float pReduction = getPointReduction(living);
        float aCap = getAdaptationCap(living);
        int dTypeCap = getDamageTypeCap(living);
        float cLearnFire = getChanceLearnFire(living);

        int count = getResistanceCount(living, adaptKey);
        int reductionCount = count;

        if (living.isBurning()) {
            float fireFail = cLearnFire;
            Float mapFail = FIRE_FAIL_MAP.get(normalizedType);
            if (mapFail != null && mapFail > fireFail) fireFail = mapFail;
            learnChance *= (1.0f - fireFail);
        }

        boolean learned = false;
        if (learnChance > 0) {
            Integer lastTick = lastLearnTick.get(living);
            int ticksSinceLast = lastTick != null ? living.ticksExisted - lastTick : LEARN_COOLDOWN_TICKS;
            float rngRoll = living.getRNG().nextFloat();
            learned = ticksSinceLast >= LEARN_COOLDOWN_TICKS && rngRoll < learnChance;

            if (learned) {
                if (count > 0) {
                    count++;
                    setResistanceCount(living, adaptKey, count);
                } else {
                    List<String> sList;
                    try {
                        sList = (List<String>) resistanceS.get(living);
                    } catch (Exception e) {
                        sList = null;
                    }
                    if (sList == null || sList.size() < dTypeCap) {
                        count = 1;
                        setResistanceCount(living, adaptKey, count);
                    } else {
                        count = 0;
                    }
                }
                if (count > 0) {
                    lastLearnTick.put(living, living.ticksExisted);
                }
            }
        }

        if (learned && count > 0) {
            if (count <= pointCap) {
                setHIT(living, (byte) 1);
                playAdaptationSound(living, adaptationPSound);
            } else {
                setHIT(living, (byte) 2);
                playAdaptationSound(living, adaptationFSound);
            }
        } else {
            setHIT(living, (byte) 0);
        }

        if (reductionCount <= 0) {
            return amount;
        }

        float clamped = Math.min(reductionCount, pointCap);
        float reduction = clamped * pReduction;
        if (reduction > aCap) reduction = aCap;
        if (reduction > 1.0f) reduction = 1.0f;

        return Math.max(amount - reduction * amount, 0);
    }
}
