package com.wizecore.metrics;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;

import com.codahale.metrics.EWMA;

import static java.lang.Math.exp;

/**
 * An exponentially-weighted moving average.
 *
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg1.pdf">UNIX Load Average Part 1: How
 *      It Works</a>
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg2.pdf">UNIX Load Average Part 2: Not
 *      Your Average Average</a>
 * @see <a href="http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average">EMA</a>
 */
public class PersistentEWMA extends EWMA {
    private static final int INTERVAL = 5;
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final int ONE_MINUTE = 1;
    private static final int FIVE_MINUTES = 5;
    private static final int FIFTEEN_MINUTES = 15;
    private static final double M1_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / ONE_MINUTE);
    private static final double M5_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIVE_MINUTES);
    private static final double M15_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIFTEEN_MINUTES);

    private volatile RAtomicLong initialized;
    private volatile RAtomicDouble rate;

    private final LongAdderAdapter uncounted;
    private final double alpha, interval;

    /**
     * Creates a new EWMA which is equivalent to the UNIX one minute load average and which expects
     * to be ticked every 5 seconds.
     *
     * @return a one-minute EWMA
     */
    public static PersistentEWMA oneMinuteEWMA(String name) {
        return new PersistentEWMA(name, M1_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX five minute load average and which expects
     * to be ticked every 5 seconds.
     *
     * @return a five-minute EWMA
     */
    public static PersistentEWMA fiveMinuteEWMA(String name) {
        return new PersistentEWMA(name, M5_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX fifteen minute load average and which
     * expects to be ticked every 5 seconds.
     *
     * @return a fifteen-minute EWMA
     */
    public static PersistentEWMA fifteenMinuteEWMA(String name) {
        return new PersistentEWMA(name, M15_ALPHA, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Create a new EWMA with a specific smoothing constant.
     *
     * @param alpha        the smoothing constant
     * @param interval     the expected tick interval
     * @param intervalUnit the time unit of the tick interval
     */
    public PersistentEWMA(String name, double alpha, long interval, TimeUnit intervalUnit) {
    	super(alpha, interval, intervalUnit);
        this.interval = intervalUnit.toNanos(interval);
        this.alpha = alpha;
        this.uncounted = PersistenceUtil.createLongAdderAdapter(name + ".uncounted");
        this.initialized = PersistenceUtil.createAtomicLong(name + ".initialized");
        this.rate = PersistenceUtil.createAtomicDouble(name + ".rate");
    }

    /**
     * Update the moving average with a new value.
     *
     * @param n the new value
     */
    public void update(long n) {
        uncounted.add(n);
    }

    /**
     * Mark the passage of time and decay the current rate accordingly.
     */
    public void tick() {
        final long count = uncounted.sumThenReset();
        final double instantRate = count / interval;
        if (initialized.get() == 0) {
            rate.addAndGet(alpha * (instantRate - rate.get()));
        } else {
            rate.set(instantRate);
            initialized.set(1);
        }
    }

    /**
     * Returns the rate in the given units of time.
     *
     * @param rateUnit the unit of time
     * @return the rate
     */
    public double getRate(TimeUnit rateUnit) {
        return rate.get() * (double) rateUnit.toNanos(1);
    }
}
