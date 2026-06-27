package me.leeseol.core.content;

public interface ContentRegionService {
    boolean available();

    boolean upsert(ContentEntry entry);

    boolean remove(ContentEntry entry);
}
