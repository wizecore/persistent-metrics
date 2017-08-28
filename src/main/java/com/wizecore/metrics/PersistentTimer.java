package com.wizecore.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * A timer metric which aggregates timing durations and provides duration statistics, plus
 * throughput statistics via {@link Meter}.
 */
public class PersistentTimer extends Timer {
	private final Meter meter;
    private final Histogram histogram;
    private final Clock clock;

    /**
     * Creates a new {@link PersistentTimer} using an {@link ExponentiallyDecayingReservoir} and the default
     * {@link Clock}.
     */
    public PersistentTimer(String name) {
        this(name, new PersistentExponentiallyDecayingReservoir(name + ".reservoir"));
    }

    /**
     * Creates a new {@link PersistentTimer} that uses the given {@link Reservoir}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should use
     */
    public PersistentTimer(String name, Reservoir reservoir) {
        this(name, reservoir, Clock.defaultClock());
    }

    /**
     * Creates a new {@link PersistentTimer} that uses the given {@link Reservoir} and {@link Clock}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should use
     * @param clock  the {@link Clock} implementation the timer should use
     */
    public PersistentTimer(String name, Reservoir reservoir, Clock clock) {
        this.meter = new PersistentMeter(name + ".meter", clock);
        this.clock = clock;
        this.histogram = new PersistentHistogram(name + ".histogram", reservoir);
    }

    /**
     * Adds a recorded duration.
     *
     * @param duration the length of the duration
     * @param unit     the scale unit of {@code duration}
     */
    public void update(long duration, TimeUnit unit) {
        update(unit.toNanos(duration));
    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Callable} whose {@link Callable#call()} method implements a process
     *              whose duration should be timed
     * @param <T>   the type of the value returned by {@code event}
     * @return the value returned by {@code event}
     * @throws Exception if {@code event} throws an {@link Exception}
     */
    public <T> T time(Callable<T> event) throws Exception {
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(clock.getTick() - startTime);
        }
    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Runnable} whose {@link Runnable#run()} method implements a process
     *              whose duration should be timed
     */
    public void time(Runnable event) {
        final long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(clock.getTick() - startTime);
        }
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

    private void update(long duration) {
        if (duration >= 0) {
            histogram.update(duration);
            meter.mark();
        }
    }
}
