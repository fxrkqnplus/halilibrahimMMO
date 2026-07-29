package com.halilibrahim.halilibrahimmmo.guide;

import org.bukkit.Material;

import java.util.List;

/** One tab of the guide - a heading on the hub screen and the entries behind it. */
public record GuideCategory(String id, String title, Material icon, String summary, List<GuideEntry> entries) {

    public GuideCategory {
        entries = List.copyOf(entries);
    }
}
