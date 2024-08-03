package kami.lib.kamiblocky.party;

public enum PartyRole {
    OWNER(256, 1),
    MANAGER(128, 3),
    MEMBER(1, 25);

    private final int level;
    private final int limit;

    PartyRole(int level, int limit) {
        this.level = level;
        this.limit = limit;
    }

    public int getLevel() {
        return level;
    }

    public int getLimit() {
        return limit;
    }

    public static PartyRole fromLevel(int level) {
        for (PartyRole role : PartyRole.values()) {
            if (role.getLevel() == level) {
                return role;
            }
        }
        return null;
    }
}
