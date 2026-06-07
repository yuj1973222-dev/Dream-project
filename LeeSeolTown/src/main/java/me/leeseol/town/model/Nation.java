package me.leeseol.town.model;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Nation {
    private final String id;
    private String name;
    private NationType type;
    private String capitalTownId;
    private final Set<String> townIds = new LinkedHashSet<>();
    private ClaimKey beaconClaim;
    private boolean pvpEnabled;
    private boolean buildProtectionEnabled = true;
    private int karma;
    private double treasury;
    private int surrenderWinStreak;
    private String debtCreditorNationId;
    private double debtAmount;
    private long debtDeadline;
    private boolean functionsSuspended;
    private long createdAt;

    public Nation(String id, String name, NationType type, String capitalTownId, long createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capitalTownId = capitalTownId;
        this.createdAt = createdAt;
        if (capitalTownId != null && !capitalTownId.isBlank()) {
            townIds.add(capitalTownId);
        }
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NationType type() {
        return type;
    }

    public void setType(NationType type) {
        this.type = type;
    }

    public String capitalTownId() {
        return capitalTownId;
    }

    public void setCapitalTownId(String capitalTownId) {
        this.capitalTownId = capitalTownId;
    }

    public Set<String> townIds() {
        return townIds;
    }

    public ClaimKey beaconClaim() {
        return beaconClaim;
    }

    public void setBeaconClaim(ClaimKey beaconClaim) {
        this.beaconClaim = beaconClaim;
    }

    public boolean pvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public boolean buildProtectionEnabled() {
        return buildProtectionEnabled;
    }

    public void setBuildProtectionEnabled(boolean buildProtectionEnabled) {
        this.buildProtectionEnabled = buildProtectionEnabled;
    }

    public int karma() {
        return karma;
    }

    public void setKarma(int karma) {
        this.karma = karma;
    }

    public void addKarma(int amount) {
        this.karma += amount;
    }

    public double treasury() {
        return treasury;
    }

    public void setTreasury(double treasury) {
        this.treasury = Math.max(0.0D, treasury);
    }

    public int surrenderWinStreak() {
        return surrenderWinStreak;
    }

    public void setSurrenderWinStreak(int surrenderWinStreak) {
        this.surrenderWinStreak = Math.max(0, surrenderWinStreak);
    }

    public String debtCreditorNationId() {
        return debtCreditorNationId;
    }

    public void setDebtCreditorNationId(String debtCreditorNationId) {
        this.debtCreditorNationId = debtCreditorNationId;
    }

    public double debtAmount() {
        return debtAmount;
    }

    public void setDebtAmount(double debtAmount) {
        this.debtAmount = Math.max(0.0D, debtAmount);
    }

    public long debtDeadline() {
        return debtDeadline;
    }

    public void setDebtDeadline(long debtDeadline) {
        this.debtDeadline = Math.max(0L, debtDeadline);
    }

    public boolean functionsSuspended() {
        return functionsSuspended;
    }

    public void setFunctionsSuspended(boolean functionsSuspended) {
        this.functionsSuspended = functionsSuspended;
    }

    public long createdAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
