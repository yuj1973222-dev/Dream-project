package me.leeseol.cleanup.manager;

public final class CleanupCountdown {
    private CleanupCountdown() {
    }

    public static boolean shouldShow(long secondsUntilCleanup, int warningSeconds) {
        return secondsUntilCleanup > 0L && secondsUntilCleanup <= Math.max(1, warningSeconds);
    }

    public static boolean shouldPlayStartSound(long secondsUntilCleanup, int warningSeconds, boolean alreadyPlayed) {
        return !alreadyPlayed && secondsUntilCleanup == Math.max(1, warningSeconds);
    }

    public static String render(String template, long secondsUntilCleanup) {
        String value = template == null || template.isBlank()
                ? "&c곧 아이템이 삭제됩니다: &e%seconds%초"
                : template;
        return value.replace("%seconds%", Long.toString(Math.max(0L, secondsUntilCleanup)));
    }
}
