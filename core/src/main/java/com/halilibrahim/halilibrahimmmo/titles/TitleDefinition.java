package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/** Immutable YAML-authored title definition. */
public record TitleDefinition(String id, String displayName, Material icon, NamespacedKey model,
                             UnlockCondition condition, TitleChatStyle chatStyle) {
}
