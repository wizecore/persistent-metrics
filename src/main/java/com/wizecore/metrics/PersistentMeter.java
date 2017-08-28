package com.wizecore.metrics;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RAtomicLong;

import com.codahale.metrics.Clock;
import com.codahale.metrics.EWMA;
import com.codahale.metrics.Meter;

/**
 * A meter metric which measures mean throughput and one-, five-, and fifteen-minute
 * exponentially-weighted moving average throughputs.
 *
 * @see EWMA
 */
public class PersistentMeter extends Meter {
    private static final long TICK_INTERVAL = TimeUnit.SECONDS.toNanos(5);

    private final EWMA m1Rate;
    private final EWMA m5Rate;
    private final EWMA m15Rate;

    private final LongAdderAdapter count;
    private final RAtomicLong startTime;
    private final RAtomicLong lastTick;
    private final Clock clock;

    /**
     * Creates a new {@link PersistentMeter}.
     */
    public PersistentMeter(String name) {
        this(name, Clock.defaultClock());
    }

    /**
     * Creates a new {@link PersistentMeter}.
     *
     * @param clock      the clock to use for the meter ticks
     */
    public PersistentMeter(String name, Clock clock) {
    	this.m1Rate = PersistentEWMA.oneMinuteEWMA(name + ".m1");
    	this.m5Rate = PersistentEWMA.fiveMinuteEWMA(name + ".m5");
    	this.m15Rate = PersistentEWMA.fifteenMinuteEWMA(name + ".m15");
    	this.count = PersistenceUtil.createLongAdderAdapter(name + ".count");
        this.clock = clock;
        this.startTime = PersistenceUtil.createAtomicLong(name + ".startTime");
        if (this.startTime.get() == 0) {
        	this.startTime.set(this.clock.getTick());
        }
        this.lastTick = PersistenceUtil.createAtomicLong(name + ".lastTick");
    }

    /**
     * Mark the occurrence of an event.
     */
    public void mark() {
        mark(1);
    }

    /**
     * Mark the occurrence of a given number of events.
     *
     * @param n the number of events
     */
    public void mark(long n) {
        tickIfNecessary();
        count.add(n);
        m1Rate.update(n);
        m5Rate.update(n);
        m15Rate.update(n);
    }

    private void tickIfNecessary() {
        final long oldTick = lastTick.get();
        final long newTick = clock.getTick();
        final long age = newTick - oldTick;
        if (age > TICK_INTERVAL) {
            final long newIntervalStartTick = newTick - age % TICK_INTERVAL;
            if (lastTick.compareAndSet(oldTick, newIntervalStartTick)) {
                final long requiredTicks = age / TICK_INTERVAL;
                for (long i = 0; i < requiredTicks; i++) {
                    m1Rate.tick();
                    m5Rate.tick();
                    m15Rate.tick();
                }
            }
        }
    }

    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public double getFifteenMinuteRate() {
        tickIfNecessary();
        return m15Rate.getRate(TimeUnit.SECONDS);
    }

    @Override
    public double getFiveMinuteRate() {
        tickIfNecessary();
        return m5Rate.getRate(TimeUnit.SECONDS);
    }

    @Override
    public double getMeanRate() {
        if (getCount() == 0) {
            return 0.0;
        } else {
            final double elapsed = (clock.getTick() - startTime.get());
            return getCount() / elapsed * TimeUnit.SECONDS.toNanos(1);
        }
    }

    @Override
    public double getOneMinuteRate() {
        tickIfNecessary();
        return m1Rate.getRate(TimeUnit.SECONDS);
    }
}
