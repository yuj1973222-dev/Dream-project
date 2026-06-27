package me.leeseol.core.content;

import java.util.Collection;

public final class ContentRegionSynchronizer {
    private ContentRegionSynchronizer() {
    }

    public static int syncLoadedRegions(Collection<ContentEntry> entries, ContentRegionService regionService) {
        if (entries == null || regionService == null || !regionService.available()) {
            return 0;
        }

        int synced = 0;
        for (ContentEntry entry : entries) {
            if (regionService.upsert(entry)) {
                synced++;
            }
        }
        return synced;
    }
}
