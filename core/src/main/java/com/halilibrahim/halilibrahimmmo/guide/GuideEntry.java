package com.halilibrahim.halilibrahimmmo.guide;

import org.bukkit.Material;

import java.util.List;

/**
 * One thing the guide explains: a heading, an icon and the lines under it.
 *
 * @param lines already-Turkish, MiniMessage-formatted body lines, rendered one
 *              per lore row exactly as written. Kept as a list rather than one
 *              paragraph because a lore row does not wrap - where the line
 *              breaks is a layout decision, and the person writing the text is
 *              the one who should be making it.
 */
public record GuideEntry(String title, Material icon, List<String> lines) {

    public GuideEntry {
        lines = List.copyOf(lines);
    }
}
