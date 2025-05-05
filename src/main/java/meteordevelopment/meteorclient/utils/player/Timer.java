package meteordevelopment.meteorclient.utils.player;

public class Timer {

    public Timer() {
        reset();
    }

    private long time = -1L;

    public Timer reset() {
        time = System.nanoTime();
        return this;
    }

    public boolean tick(int tick) {
        return passedMs(tick * 50L);
    }

    public boolean passedMs(long ms) {
        return passedNS(convertToNS(ms));
    }

    public boolean passedNS(long ns) {
        return System.nanoTime() - time >= ns;
    }

    public long convertToNS(long time) {
        return time * 1000000L;
    }

}
