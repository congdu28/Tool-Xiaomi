package app.cd.mitool.util;

/** Shared deduplication for battery transitions and explicit power broadcasts. */
public final class PowerConnectionPolicy {
    private boolean observed;
    private Boolean delivered;
    public PowerConnectionPolicy(boolean initial) { observed = initial; }
    public boolean observe(boolean connected, boolean explicitPowerEvent) {
        boolean changed = connected != observed;
        observed = connected;
        if (changed || (explicitPowerEvent && delivered == null)) {
            delivered = connected; return true;
        }
        return false;
    }
}
