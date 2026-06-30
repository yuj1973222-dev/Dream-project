package me.leeseol.economy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import me.leeseol.economy.ledger.LedgerSnapshot;
import me.leeseol.economy.market.MarketSaleResult;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class EconomySharedContractTest {
    @Test
    public void pluginDescriptorKeepsVaultPayAndMarketSurface() {
        YamlConfiguration descriptor = YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));

        assertTrue(descriptor.getStringList("depend").contains("Vault"));
        assertEquals("me.leeseol.economy.LeeSeolEconomyPlugin", descriptor.getString("main"));
        assertTrue(descriptor.isConfigurationSection("commands.won"));
        assertTrue(descriptor.isConfigurationSection("commands.pay"));
        assertEquals("leeseoleconomy.pay", descriptor.getString("commands.pay.permission"));
        assertTrue(descriptor.isConfigurationSection("commands.market"));
        assertTrue(descriptor.isConfigurationSection("permissions.leeseoleconomy.market"));
        assertTrue(descriptor.isConfigurationSection("commands.servermenu"));
        assertTrue(descriptor.isConfigurationSection("commands.leeseolmenu"));
    }

    @Test
    public void defaultConfigKeepsMarketLedgerAndSharedStorage() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));

        assertTrue(config.getBoolean("features.vault-provider"));
        assertTrue(config.getBoolean("features.market"));
        assertTrue(config.getBoolean("features.ledger"));
        assertEquals("/opt/minecraft/shared/economy/balances.properties", config.getString("storage.balances-file"));
        assertEquals("/opt/minecraft/shared/economy/market.yml", config.getString("storage.market-file"));
        assertEquals("/opt/minecraft/shared/economy/ledger.yml", config.getString("storage.ledger-file"));
        assertTrue(config.getStringList("market.allowed-local-servers").contains("survival"));
    }

    @Test
    public void bungeeBridgeChannelKeepsVelocityCompatibleName() {
        assertEquals("BungeeCord", LeeSeolEconomyPlugin.BUNGEE_CHANNEL);
    }

    @Test
    public void ledgerSnapshotReportsNetIssuedMoney() {
        LedgerSnapshot snapshot = new LedgerSnapshot(
                "2026-06-27",
                Map.of("admin_give", 10_000L),
                Map.of("shop_buy", 2_500L),
                Map.of("player_pay", 1_000L),
                10_000L,
                2_500L,
                1_000L
        );

        assertEquals(7_500L, snapshot.netIssued());
    }

    @Test
    public void marketSaleResultSucceedsOnlyWhenAcceptedAndPaid() {
        assertTrue(new MarketSaleResult(null, 64, 32, 1_000L, 10L, null).success());
        assertFalse(new MarketSaleResult(null, 64, 0, 1_000L, 10L, "not accepted").success());
        assertFalse(new MarketSaleResult(null, 64, 32, 0L, 10L, "no payout").success());
    }
}
