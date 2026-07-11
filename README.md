# NTM-CE PvE Rebalance & SRP Compatibility Addon

A compatibility and gameplay rebalance addon for **HBM's Nuclear Tech Mod CE** focused on improving **PvE combat** while restoring mechanics from **classic NTM**.

The goal of this project is to make combat feel closer to NTM Extended, improve interactions with other mods, and provide proper compatibility with **Scape and Run: Parasites (SRP)** without breaking overall balance.

---

# Features

## PvE Combat Rebalance

This addon restores some of the combat mechanics that were removed or changed from older versions of HBM's NTM.

### Restored Legacy Armor Mechanics

For **PvE and non-NTM combat**, legacy armor mechanics have been restored with balance adjustments.

- General Damage Modifier (GDM)
- Damage Threshold (Now GDT)
- Hard Damage Cap (HDC)

These mechanics only apply against non-NTM damage sources and are designed to preserve PvP and NTM weapon balance.

---

### Modernized Damage Calculations

NTM weapons now use Minecraft **1.12.2 vanilla armor calculations** together with base:

- Damage Reduction (DR)
- Damage Threshold (DT)

instead of the old **1.7.10 flat armor reduction** system.

As a result:

- Heavy armor behaves more fairly.
- High-powered NTM weapons are significantly more effective against armored mobs.
- Damage scaling now adheres to modern Minecraft combat.

---

## Internal Damage Handler

Most Nuclear Tech weapons now use the addon's internal damage handler.

Benefits include:

- Consistent damage calculations
- Better compatibility with other mods
- Proper armor interaction
- Support for custom damage mechanics

Most weapons and turret types are supported.

*A few rare weapons and damage sources are still waiting to be migrated.*

---

## Multipart Entity Support

NTM weapons can now correctly damage multipart entities and piercing weapons do not stack damage if multiple hitboxes of the same mob are damaged.

This fixes compatibility with bosses and mobs that use multiple hitboxes.

---

## Weapon Fixes/Changes

### Flamethrower/Mister Topaz/Fritz Turret

Now properly bypasses invulnerability frames and ignore vanilla armor.

### Tau Cannon

Now bypasses vanilla armor.

---

# Scape and Run: Parasites Compatibility

A major goal of this addon is making SRP significantly more enjoyable and playable when played alongside NTM.

## Adaptation Support

NTM damage now correctly interacts with the Parasite Adaptation System.

Previously, many Nuclear Tech weapons did not interact with adaptation correctly.

---

## Custom Adaptation Learning Rates

Different damage types now have individual learning modifiers.

Examples include:

| Damage Type | Adaptation Rate |
|--------------|----------------:|
| Physical | 100% |
| Laser | 60% |
| *(additional values planned/documented in code)* | |

Certain advanced damage types are completely **unadaptable**, including:

- Tau Cannon blasts
- Nuclear Blasts
- Artillery

---

## Fire Damage Improvements

Parasites now properly receive increased fire damage from NTM weapons.

Affected weapons include:

- Flamethrower
- White Phosphorus Rockets
- Low-Frequency Capacitors
- Other incendiary ammunition

These attacks now deal the intended **4× damage** against Parasites.

---

## Hard Damage Cap Support

The player Hard Damage Cap now overrides SRP's minimum damage mechanic.

This restores meaningful armor progression and prevents unavoidable damage from bypassing high-end armor sets.

Current implementation:

- HDC represents the maximum damage a player can receive from a single mob attack in one tick.

---

# Planned Features

- Better implementation of SRP Damage Cap mechanics
- Configurable limits for player Hard Damage Cap
- Solinium Grenades
- Solinium Shells
- Solinium Rockets for Greg and Henry
- High-Frequency Sword
- Ultrahard Steel Sword
- Solinium explosions cleansing Parasite biomes
- Removal and neutralization of Parasite blocks after Solinium detonations

---

# Project Goals

This addon aims to:

- Restore the feel of Nuclear Tech Extended combat
- Improve PvE balance without affecting core NTM gameplay
- Make Scape and Run: Parasites progression fair and enjoyable
- Provide a framework for better cross-mod compatibility
- Preserve challenge while rewarding equipment progression

---

# Compatibility

Designed for:

- Minecraft **1.12.2**
- HBM's Nuclear Tech Mod CE
- Scape and Run: Parasites (optional but recommended)

---

# Status

More compatibility improvements and restored NTM Extended mechanics are planned for future releases.
🚧 **Active Development**

## Disclaimer

This addon includes limited use of Java Reflection to interact with internal fields and methods of **Scape and Run: Parasites (SRP)** for interoperability purposes.

Reflection is used **exclusively for runtime compatibility**, allowing this addon to read or invoke specific values that are not exposed through SRP's public API. This is necessary because SRP is a closed-source mod and does not provide an official compatibility interface for the features implemented here.

The reflected members are used solely to:
- Read adaptation-related values and configuration needed for compatibility.
- Access multipart entity information required for proper hit detection.
- Trigger existing SRP functionality where appropriate.

No SRP source code, algorithms, or implementation logic has been copied, reproduced, decompiled, or redistributed as part of this project. All gameplay logic introduced by this addon is independently implemented and merely interfaces with SRP at runtime using reflection where no public API exists.

This project is intended to improve cross-mod compatibility while fully respecting the ownership and licensing of SRP.
