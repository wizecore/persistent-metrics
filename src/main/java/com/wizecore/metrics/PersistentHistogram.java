package com.wizecore.metrics;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;

/**
 * A metric which calculates the distribution of a value.
 *
 * @see <a href="http://www.johndcook.com/standard_deviation.html">Accurately computing running
 *      variance</a>
 */
public class PersistentHistogram extends Histogram {
    private final Reservoir reservoir;
    private final LongAdderAdapter count;

    /**
     * Creates a new {@link Histogram} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public PersistentHistogram(String name, Reservoir reservoir) {
    	super(reservoir);
        this.reservoir = reservoir;
        this.count = PersistenceUtil.createLongAdderAdapter(name + ".count");
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(int value) {
        update((long) value);
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(long value) {
        count.increment();
        reservoir.update(value);
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public Snapshot getSnapshot() {
        return reservoir.getSnapshot();
    }
}
