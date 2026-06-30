package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownDisplayServiceContractTest {
    @Test
    public void displayMethodsMoveBehindFacade() throws IOException {
        String facade = source("TownService.java");
        String service = source("TownDisplayService.java");

        for (String method : List.of(
                "public void setChatMode(Player player, ChatMode mode)",
                "public void sendTownChat(Player player, Component message)",
                "public void sendNationChat(Player player, Component message)",
                "public void broadcastGlobalChat(Player player, Component message)",
                "public Component chatLine(String formatPath, Player player, Component message)",
                "public String affiliationPrefix(Player player)",
                "public String rankPrefix(Player player)",
                "public void updateIdentity(Player player)",
                "public void updateAllIdentities()",
                "public void sendSelfInfo(Player player)",
                "public String info(Town town)"
        )) {
            assertTrue("Display service missing method: " + method, service.contains(method));
            assertTrue("Facade missing method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "displayService.setChatMode(player, mode);",
                "displayService.sendTownChat(player, message);",
                "displayService.sendNationChat(player, message);",
                "displayService.broadcastGlobalChat(player, message);",
                "return displayService.chatLine(formatPath, player, message);",
                "return displayService.affiliationPrefix(player);",
                "return displayService.rankPrefix(player);",
                "displayService.updateIdentity(player);",
                "displayService.updateAllIdentities();",
                "displayService.sendSelfInfo(player);",
                "return displayService.info(town);"
        )) {
            assertTrue("Facade missing display delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void displayImplementationNoLongerLivesInFacade() throws IOException {
        String facade = source("TownService.java");

        for (String snippet : List.of(
                "PlaceholderAPI.setPlaceholders",
                "plugin.getConfig().getString(\"chat.",
                "prefix.nation",
                "rank-image."
        )) {
            assertFalse("Display implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    @Test
    public void selfInfoDisplayStringsStayReadableKorean() throws IOException {
        String service = source("TownDisplayService.java");

        for (String snippet : List.of(
                "&#BEEBFF[소속 정보]",
                "&7파티: &f없음",
                "&7국가: &f없음",
                "&7상태: &f파티를 생성하거나 초대를 받아 가입할 수 있습니다.",
                "? \"대표\" : \"구성원\"",
                "&7파티 인원: &f",
                "&7소유 청크: &f",
                "&7국가 인원: &f",
                "&7카르마: &f",
                "&7국고: &e",
                "&7일일 국가 유지비: &e",
                "&7유지비 체납: &c",
                "&7전쟁 체납: &c",
                "&c국가 기능 정지 상태",
                "&7채팅 모드: &f"
        )) {
            assertTrue("Unreadable or missing self-info display string: " + snippet,
                    service.contains(snippet));
        }

        for (String mojibake : List.of("\uF9D0", "\uF9CF", "\u7B4C", "\u91AB", "\u71C1", "\u8881")) {
            assertFalse("Self-info display contains mojibake: " + mojibake,
                    service.contains(mojibake));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
