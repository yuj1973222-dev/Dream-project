package me.leeseol.town.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Town {
    private final String id;
    private String name;
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> invites = new LinkedHashSet<>();
    private final Set<String> inviteNames = new LinkedHashSet<>();
    private final Set<ClaimKey> claims = new LinkedHashSet<>();
    private String nationId;
    private long createdAt;

    public Town(String id, String name, UUID leader, long createdAt) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.createdAt = createdAt;
        this.members.add(leader);
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

    public UUID leader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public Set<UUID> members() {
        return members;
    }

    public Set<UUID> invites() {
        return invites;
    }

    public Set<String> inviteNames() {
        return inviteNames;
    }

    public Set<ClaimKey> claims() {
        return claims;
    }

    public String nationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public long createdAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLeader(UUID uuid) {
        return leader != null && leader.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isRecognized(int minMembers) {
        return members.size() >= minMembers;
    }
}
