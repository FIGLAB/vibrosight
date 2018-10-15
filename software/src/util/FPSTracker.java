package util;

/*
 * FPS tracker to help calculate FPS
 */
public class FPSTracker {
    private double alpha;
    private double prev;
    private double delta;

    public FPSTracker(double alpha) {
        this.alpha = alpha;
        reset();
    }

    public FPSTracker() {
        this(0.01);
    }

    // Reset the FPS tracker
    public void reset() {
        prev = Double.NaN;
        delta = Double.NaN;
    }

    // Tick the FPS tracker. 
    public void tick() {
        if (Double.isNaN(this.prev))
            return;
        if (Double.isNaN(this.delta))
            return;

        double now = System.currentTimeMillis() / 1000.0;
        if (now - this.prev < delta) {
            return;
        }
        double newdelta = now - this.prev;
        this.delta += this.alpha * (newdelta - this.delta);
    }

    // Update the FPS tracker
    public void update() {
        double now = System.currentTimeMillis() / 1000.0;

        if (Double.isNaN(this.prev)) {
            this.prev = now;
            return;
        }

        if (Double.isNaN(this.delta)) {
            this.delta = now - this.prev;
            this.prev = now;
            return;
        }

        double newdelta = now - this.prev;
        this.delta += this.alpha * (newdelta - this.delta);
        this.prev = now;
    }

    // Get the current FPS 
    public double fps() {
        return 1.0 / this.delta;
    }
}