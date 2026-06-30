package me.leeseol.town.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownPlaceholderContractTest {
    @Test
    public void keepsPlaceholderApiExpansionIdentity() {
        TownPlaceholderExpansion expansion = new TownPlaceholderExpansion(null);

        assertEquals("leeseoltown", expansion.getIdentifier());
        assertEquals("lee_seol", expansion.getAuthor());
        assertTrue(expansion.persist());
    }

    @Test
    public void keepsPublishedPlaceholderParameters() throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "me", "leeseol", "town", "hook", "TownPlaceholderExpansion.java"
        ));

        for (String parameter : List.of(
                "affiliation",
                "rank",
                "has_party",
                "has_town",
                "town",
                "party",
                "nation",
                "nation_color",
                "nation_color_hex",
                "nation_type"
        )) {
            assertTrue("Missing placeholder parameter: " + parameter, source.contains('"' + parameter + '"'));
        }
    }
}
