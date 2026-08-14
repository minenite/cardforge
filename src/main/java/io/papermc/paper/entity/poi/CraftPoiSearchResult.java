package io.papermc.paper.entity.poi;

import org.bukkit.Location;

/**
 * A point of interest found by a world search, paired with where it sits.
 */
public record CraftPoiSearchResult(PoiType poiType, Location location) implements PoiSearchResult {
}
