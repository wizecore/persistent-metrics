package com.wizecore.metrics;

import static java.lang.Math.exp;
import static java.lang.Math.min;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.redisson.api.RAtomicLong;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Snapshot;
import com.wizecore.metrics.WeightedSnapshot.WeightedSample;

/**
 * An exponentially-decaying random reservoir of {@code long}s. Uses Cormode et al's
 * forward-decaying priority reservoir sampling method to produce a statistically representative
 * sampling reservoir, exponentially biased towards newer entries.
 *
 * @see <a href="http://dimacs.rutgers.edu/~graham/pubs/papers/fwddecay.pdf">
 * Cormode et al. Forward Decay: A Practical Time Decay Model for Streaming Systems. ICDE '09:
 *      Proceedings of the 2009 IEEE International Conference on Data Engineering (2009)</a>
 */
public class PersistentExponentiallyDecayingReservoir extends ExponentiallyDecayingReservoir {
	
    private static final int DEFAULT_SIZE = 1028;
    private static final double DEFAULT_ALPHA = 0.015;
    private static final long RESCALE_THRESHOLD = TimeUnit.HOURS.toNanos(1);
    
    public static class ValueEntry implements Comparable<ValueEntry>, Serializable {
		private static final long serialVersionUID = 1L;
		private final Double key;
		private final WeightedSample value;
		
		public ValueEntry() {
			key = null;
			value = null;
		}
    	
    	public ValueEntry(Double key, WeightedSample value) {
			this.key = key;
			this.value = value;
		}
    	
    	@Override
    	public int hashCode() {
    		return key.hashCode();
    	}
    	
    	@Override
    	public boolean equals(Object obj) {
    		return ((ValueEntry) obj).key.equals(key);
    	}
    	
    	@Override
    	public int compareTo(ValueEntry o) {
    		return key.compareTo(o.key);
    	}
    }

    private final Set<ValueEntry> values;
    private final ReentrantReadWriteLock lock;
    private final double alpha;
    private final int size;
    private final RAtomicLong count;
    private volatile RAtomicLong startTime;
    private final RAtomicLong nextScaleTime;
    private final Clock clock;

    /**
     * Creates a new {@link PersistentExponentiallyDecayingReservoir} of 1028 elements, which offers a 99.9%
     * confidence level with a 5% margin of error assuming a normal distribution, and an alpha
     * factor of 0.015, which heavily biases the reservoir to the past 5 minutes of measurements.
     */
    public PersistentExponentiallyDecayingReservoir(String name) {
        this(name, DEFAULT_SIZE, DEFAULT_ALPHA);
    }

    /**
     * Creates a new {@link PersistentExponentiallyDecayingReservoir}.
     *
     * @param size  the number of samples to keep in the sampling reservoir
     * @param alpha the exponential decay factor; the higher this is, the more biased the reservoir
     *              will be towards newer values
     */
    public PersistentExponentiallyDecayingReservoir(String name, int size, double alpha) {
        this(name, size, alpha, Clock.defaultClock());
    }

    /**
     * Creates a new {@link PersistentExponentiallyDecayingReservoir}.
     *
     * @param size  the number of samples to keep in the sampling reservoir
     * @param alpha the exponential decay factor; the higher this is, the more biased the reservoir
     *              will be towards newer values
     * @param clock the clock used to timestamp samples and track rescaling
     */
    public PersistentExponentiallyDecayingReservoir(String name, int size, double alpha, Clock clock) {
        this.values = PersistenceUtil.createSortedSet(name + ".values", ValueEntry.class);
        this.lock = new ReentrantReadWriteLock();
        this.alpha = alpha;
        this.size = size;
        this.clock = clock;
        this.count = PersistenceUtil.createAtomicLong(name + ".count");
        this.startTime = PersistenceUtil.createAtomicLong(name + ".count", currentTimeInSeconds());
        this.nextScaleTime = PersistenceUtil.createAtomicLong(name + ".nextScaleTime", (clock.getTick() + RESCALE_THRESHOLD));
    }

    @Override
    public int size() {
        return (int) min(size, count.get());
    }

    @Override
    public void update(long value) {
        update(value, currentTimeInSeconds());
    }

    /**
     * Adds an old value with a fixed timestamp to the reservoir.
     *
     * @param value     the value to be added
     * @param timestamp the epoch timestamp of {@code value} in seconds
     */
    public void update(long value, long timestamp) {
        rescaleIfNeeded();
        lockForRegularUsage();
        try {
            final double itemWeight = weight(timestamp - startTime.get());
            final WeightedSample sample = new WeightedSample(value, itemWeight);
            final double priority = itemWeight / ThreadLocalRandomProxy.current().nextDouble();
            
            final long newCount = count.incrementAndGet();
            ValueEntry e = new ValueEntry(priority, sample);
			if (newCount <= size) {
            	values.remove(e);
                values.add(e);
            } else {
            	Double first = values.iterator().next().key;
                if (first < priority && !values.add(e)) {
                    // ensure we always remove an item
                    while (!values.remove(first)) {
                        first = values.iterator().next().key;
                    }
                }
            }
        } finally {
            unlockForRegularUsage();
        }
    }

    private void rescaleIfNeeded() {
        final long now = clock.getTick();
        final long next = nextScaleTime.get();
        if (now >= next) {
            rescale(now, next);
        }
    }
    
    private Collection<WeightedSample> values() {
    	ArrayList<WeightedSample> l = new ArrayList<>();
    	for (ValueEntry e: values) {
			l.add(e.value);
		}
    	return l;
    }

    @Override
    public Snapshot getSnapshot() {
        lockForRegularUsage();
        try {
            return new WeightedSnapshot(values());
        } finally {
            unlockForRegularUsage();
        }
    }

    private long currentTimeInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(clock.getTime());
    }

    private double weight(long t) {
        return exp(alpha * t);
    }

    /* "A common feature of the above techniques—indeed, the key technique that
     * allows us to track the decayed weights efficiently—is that they maintain
     * counts and other quantities based on g(ti − L), and only scale by g(t − L)
     * at query time. But while g(ti −L)/g(t−L) is guaranteed to lie between zero
     * and one, the intermediate values of g(ti − L) could become very large. For
     * polynomial functions, these values should not grow too large, and should be
     * effectively represented in practice by floating point values without loss of
     * precision. For exponential functions, these values could grow quite large as
     * new values of (ti − L) become large, and potentially exceed the capacity of
     * common floating point types. However, since the values stored by the
     * algorithms are linear combinations of g values (scaled sums), they can be
     * rescaled relative to a new landmark. That is, by the analysis of exponential
     * decay in Section III-A, the choice of L does not affect the final result. We
     * can therefore multiply each value based on L by a factor of exp(−α(L′ − L)),
     * and obtain the correct value as if we had instead computed relative to a new
     * landmark L′ (and then use this new L′ at query time). This can be done with
     * a linear pass over whatever data structure is being used."
     */
    private void rescale(long now, long next) {
        lockForRescale();
        try {
            if (nextScaleTime.compareAndSet(next, now + RESCALE_THRESHOLD)) {
                final long oldStartTime = startTime.get();
                this.startTime.set(currentTimeInSeconds());
                final double scalingFactor = exp(-alpha * (startTime.get() - oldStartTime));

                for (ValueEntry e : values) {
                    final WeightedSample sample = e.value;
                    final WeightedSample newSample = new WeightedSample(sample.value, sample.weight * scalingFactor);
                    ValueEntry ne = new ValueEntry(e.key * scalingFactor, newSample);
                    values.remove(e);
					values.add(e);
                }

                // make sure the counter is in sync with the number of stored samples.
                count.set(values.size());
            }
        } finally {
            unlockForRescale();
        }
    }

    private void unlockForRescale() {
        lock.writeLock().unlock();
    }

    private void lockForRescale() {
        lock.writeLock().lock();
    }

    private void lockForRegularUsage() {
        lock.readLock().lock();
    }

    private void unlockForRegularUsage() {
        lock.readLock().unlock();
    }
}
