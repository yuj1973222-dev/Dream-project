package me.leeseol.proxy.queue;

import java.util.Properties;

public record QueueSettings(
        String survivalServer,
        String lobbyServer,
        String limboWorld,
        String pluginMessageChannel,
        int maxSurvivalPlayers,
        int maxAdmissionAttempts,
        long limboRequestTimeoutSeconds,
        long actionbarIntervalSeconds,
        String bypassPermission,
        String nonPlayerMessage,
        String alreadySurvivalMessage,
        String survivalMissingMessage,
        String lobbyMissingMessage,
        String survivalMoveMessage,
        String survivalMovedMessage,
        String survivalMoveFailedMessage,
        String limboMoveMessage,
        String limboMoveFailedMessage,
        String queueAddedMessage,
        String queuePositionMessage,
        String queueLeftMessage,
        String lobbyMoveMessage,
        String actionbarMessage
) {
    public static QueueSettings defaults() {
        return new QueueSettings(
                "survival",
                "lobby",
                "limbo",
                "leeseol:queue",
                100,
                5,
                10L,
                3L,
                "leeseol.queue.bypass",
                "이 명령어는 플레이어만 사용할 수 있습니다.",
                "이미 survival 서버에 있습니다.",
                "survival 서버를 찾을 수 없습니다.",
                "lobby 서버를 찾을 수 없습니다.",
                "survival 서버로 이동합니다.",
                "survival 서버로 이동했습니다.",
                "survival 서버 이동에 실패했습니다.",
                "survival이 가득 차 limbo 대기실로 이동합니다.",
                "limbo 대기실로 이동할 수 없습니다.",
                "survival 대기열에 등록되었습니다. 현재 %position%번입니다.",
                "이미 survival 대기열에 있습니다. 현재 %position%번입니다.",
                "survival 대기열에서 나갔습니다.",
                "로비로 이동합니다.",
                "survival 대기열 %position%번 / 빈자리 대기 중"
        );
    }

    public static QueueSettings from(Properties properties) {
        QueueSettings defaults = defaults();
        return new QueueSettings(
                text(properties, "survival-server", defaults.survivalServer()),
                text(properties, "lobby-server", defaults.lobbyServer()),
                text(properties, "limbo-world", defaults.limboWorld()),
                text(properties, "plugin-message-channel", defaults.pluginMessageChannel()),
                integer(properties, "max-survival-players", defaults.maxSurvivalPlayers(), 1, 100000),
                integer(properties, "max-admission-attempts", defaults.maxAdmissionAttempts(), 1, 100),
                decimal(properties, "limbo-request-timeout-seconds", defaults.limboRequestTimeoutSeconds(), 1L, 120L),
                decimal(properties, "actionbar-interval-seconds", defaults.actionbarIntervalSeconds(), 1L, 60L),
                text(properties, "bypass-permission", defaults.bypassPermission()),
                text(properties, "message-non-player", defaults.nonPlayerMessage()),
                text(properties, "message-already-survival", defaults.alreadySurvivalMessage()),
                text(properties, "message-survival-missing", defaults.survivalMissingMessage()),
                text(properties, "message-lobby-missing", defaults.lobbyMissingMessage()),
                text(properties, "message-survival-move", defaults.survivalMoveMessage()),
                text(properties, "message-survival-moved", defaults.survivalMovedMessage()),
                text(properties, "message-survival-move-failed", defaults.survivalMoveFailedMessage()),
                text(properties, "message-limbo-move", defaults.limboMoveMessage()),
                text(properties, "message-limbo-move-failed", defaults.limboMoveFailedMessage()),
                text(properties, "message-queue-added", defaults.queueAddedMessage()),
                text(properties, "message-queue-position", defaults.queuePositionMessage()),
                text(properties, "message-queue-left", defaults.queueLeftMessage()),
                text(properties, "message-lobby-move", defaults.lobbyMoveMessage()),
                text(properties, "actionbar-message", defaults.actionbarMessage())
        );
    }

    public void writeDefaultsTo(Properties properties) {
        properties.setProperty("survival-server", survivalServer);
        properties.setProperty("lobby-server", lobbyServer);
        properties.setProperty("limbo-world", limboWorld);
        properties.setProperty("plugin-message-channel", pluginMessageChannel);
        properties.setProperty("max-survival-players", Integer.toString(maxSurvivalPlayers));
        properties.setProperty("max-admission-attempts", Integer.toString(maxAdmissionAttempts));
        properties.setProperty("limbo-request-timeout-seconds", Long.toString(limboRequestTimeoutSeconds));
        properties.setProperty("actionbar-interval-seconds", Long.toString(actionbarIntervalSeconds));
        properties.setProperty("bypass-permission", bypassPermission);
        properties.setProperty("message-non-player", nonPlayerMessage);
        properties.setProperty("message-already-survival", alreadySurvivalMessage);
        properties.setProperty("message-survival-missing", survivalMissingMessage);
        properties.setProperty("message-lobby-missing", lobbyMissingMessage);
        properties.setProperty("message-survival-move", survivalMoveMessage);
        properties.setProperty("message-survival-moved", survivalMovedMessage);
        properties.setProperty("message-survival-move-failed", survivalMoveFailedMessage);
        properties.setProperty("message-limbo-move", limboMoveMessage);
        properties.setProperty("message-limbo-move-failed", limboMoveFailedMessage);
        properties.setProperty("message-queue-added", queueAddedMessage);
        properties.setProperty("message-queue-position", queuePositionMessage);
        properties.setProperty("message-queue-left", queueLeftMessage);
        properties.setProperty("message-lobby-move", lobbyMoveMessage);
        properties.setProperty("actionbar-message", actionbarMessage);
    }

    public String queueAddedText(int position) {
        return renderPosition(queueAddedMessage, position);
    }

    public String queuePositionText(int position) {
        return renderPosition(queuePositionMessage, position);
    }

    public String actionbarText(int position) {
        return renderPosition(actionbarMessage, position);
    }

    private static String renderPosition(String template, int position) {
        return template.replace("%position%", Integer.toString(position));
    }

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key, fallback);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int integer(Properties properties, String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long decimal(Properties properties, String key, long fallback, long min, long max) {
        try {
            long value = Long.parseLong(properties.getProperty(key, Long.toString(fallback)).trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
