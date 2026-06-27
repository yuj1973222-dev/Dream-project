package me.leeseol.core.content;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class ContentRegionSynchronizerTest {
    @Test
    public void upsertsLoadedContentRegions() {
        ContentEntry neutral = ContentEntry.create(
                ContentType.NEUTRAL,
                "central",
                "central",
                new ContentArea("world", 0, 60, 0, 10, 80, 10),
                new ContentSpawn("world", 5.5D, 65.0D, 5.5D, 0.0F, 0.0F)
        );
        ContentEntry casino = ContentEntry.create(
                ContentType.CASINO,
                "main_casino",
                "main_casino",
                new ContentArea("world", 20, 60, 20, 30, 80, 30),
                new ContentSpawn("world", 25.5D, 65.0D, 25.5D, 0.0F, 0.0F)
        );
        RecordingRegionService regionService = new RecordingRegionService();

        int synced = ContentRegionSynchronizer.syncLoadedRegions(List.of(neutral, casino), regionService);

        assertEquals(2, synced);
        assertEquals(List.of("leeseol_content_neutral_central", "leeseol_content_casino_main_casino"), regionService.upsertedRegionIds);
    }

    private static final class RecordingRegionService implements ContentRegionService {
        private final List<String> upsertedRegionIds = new ArrayList<>();

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public boolean upsert(ContentEntry entry) {
            upsertedRegionIds.add(entry.regionId());
            return true;
        }

        @Override
        public boolean remove(ContentEntry entry) {
            return true;
        }
    }
}
