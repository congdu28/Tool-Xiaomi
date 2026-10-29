package app.cd.mitool.util;

public final class AudioModePolicy {
    private AudioModePolicy() {}
    public static int combine(boolean ring, boolean master, boolean voice) {
        return (ring ? 1 : 0) | (master && voice ? 2 : 0);
    }
}
