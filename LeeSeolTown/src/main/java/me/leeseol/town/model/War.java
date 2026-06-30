package me.leeseol.town.model;

public final class War {
    private final String id;
    private final String attackerNationId;
    private final String defenderNationId;
    private final WarMode mode;
    private WarStatus status;
    private final long declaredAt;
    private long protectionUntil;
    private boolean defenderProtectionActive;

    public War(String id, String attackerNationId, String defenderNationId, WarStatus status, long declaredAt) {
        this(id, attackerNationId, defenderNationId, WarMode.INVASION, status, declaredAt);
    }

    public War(String id, String attackerNationId, String defenderNationId, WarMode mode, WarStatus status, long declaredAt) {
        this.id = id;
        this.attackerNationId = attackerNationId;
        this.defenderNationId = defenderNationId;
        this.mode = mode == null ? WarMode.INVASION : mode;
        this.status = status;
        this.declaredAt = declaredAt;
    }

    public String id() {
        return id;
    }

    public String attackerNationId() {
        return attackerNationId;
    }

    public String defenderNationId() {
        return defenderNationId;
    }

    public WarMode mode() {
        return mode;
    }

    public WarStatus status() {
        return status;
    }

    public void setStatus(WarStatus status) {
        this.status = status;
    }

    public long declaredAt() {
        return declaredAt;
    }

    public long protectionUntil() {
        return protectionUntil;
    }

    public void setProtectionUntil(long protectionUntil) {
        this.protectionUntil = protectionUntil;
    }

    public boolean defenderProtectionActive() {
        return defenderProtectionActive;
    }

    public void setDefenderProtectionActive(boolean defenderProtectionActive) {
        this.defenderProtectionActive = defenderProtectionActive;
    }

    public boolean involves(String nationId) {
        return attackerNationId.equals(nationId) || defenderNationId.equals(nationId);
    }

    public boolean between(String firstNationId, String secondNationId) {
        return involves(firstNationId) && involves(secondNationId) && !firstNationId.equals(secondNationId);
    }

    public String enemyOf(String nationId) {
        if (attackerNationId.equals(nationId)) {
            return defenderNationId;
        }
        if (defenderNationId.equals(nationId)) {
            return attackerNationId;
        }
        return null;
    }

    public static String id(String attackerNationId, String defenderNationId) {
        return attackerNationId + "__vs__" + defenderNationId;
    }
}
