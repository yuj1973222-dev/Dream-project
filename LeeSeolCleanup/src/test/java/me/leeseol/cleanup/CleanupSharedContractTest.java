package me.leeseol.cleanup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import me.leeseol.cleanup.hook.CleanupPlaceholderExpansion;
import me.leeseol.cleanup.manager.CleanupCountdown;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class CleanupSharedContractTest {
    @Test
    public void pluginDescriptorKeepsPlaceholderApiAndCleanupAliases() {
        YamlConfiguration descriptor = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(descriptor.getStringList("softdepend").contains("PlaceholderAPI"));
        assertEquals("me.leeseol.cleanup.LeeSeolCleanupPlugin", descriptor.getString("main"));
        assertTrue(descriptor.isConfigurationSection("commands.leeseolcleanup"));
        assertTrue(descriptor.getStringList("commands.leeseolcleanup.aliases").contains("cleanup"));
        assertTrue(descriptor.getStringList("commands.leeseolcleanup.aliases").contains("itemcleanup"));
    }

    @Test
    public void placeholderExpansionKeepsPublishedIdentity() {
        CleanupPlaceholderExpansion expansion = new CleanupPlaceholderExpansion(null);

        assertEquals("leeseolcleanup", expansion.getIdentifier());
        assertEquals("lee_seol", expansion.getAuthor());
        assertTrue(expansion.persist());
    }

    @Test
    public void defaultActionbarTemplateKeepsSecondsToken() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        String template = config.getString("cleanup.warning.actionbar");

        assertTrue(template.contains("%seconds%"));
        String rendered = CleanupCountdown.render(template, 7L);
        assertTrue(rendered.contains("7"));
        assertFalse(rendered.contains("%seconds%"));
    }
}
