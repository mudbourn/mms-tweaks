package info.mudbourn.mmstweaks.command.tier;

public enum Tier {
    PLAYER,
    TEMP_MOD,
    MODERATOR,
    ADMIN,
    OWNER;

    public boolean atLeast(Tier other) {
        return compareTo(other) >= 0;
    }

    public static Tier max(Tier a, Tier b) {
        return a.atLeast(b) ? a : b;
    }
}
