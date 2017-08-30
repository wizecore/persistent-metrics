package com.wizecore.metrics;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.thoughtworks.xstream.XStream;

/**
 * A metric which calculates the distribution of a value.
 *
 * @see <a href="http://www.johndcook.com/standard_deviation.html">Accurately computing running
 *      variance</a>
 */
public class PersistentHistogram extends Histogram implements Persistent {
    private Histogram value;
    private String key;
    private RAtomicLong count;
    private RBucket<String> snapshot;

    /**
     * Creates a new {@link Histogram} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public PersistentHistogram(String name, Reservoir reservoir) {
    	super(reservoir);
    	XStream x = new XStream();
    	key = name + ".xml";
		String xml = PersistenceUtil.getValue(key);
    	count = PersistenceUtil.createAtomicLong(name + ".count");
    	snapshot = PersistenceUtil.getBucket(name + ".snapshot");
    	if (xml != null) {
    		value = (Histogram) x.fromXML(xml);
    	} else {
    		value = new Histogram(reservoir);
        	save();
    	}
    }
    
    @Override
    public void save() {
    	XStream x = new XStream();
    	String xml = x.toXML(value);
    	PersistenceUtil.setValue(key, xml);
    	count.set(getCount());
    	snapshot.set(x.toXML(value.getSnapshot()));
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
        this.value.update(value);
        save();
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    @Override
    public long getCount() {
        return value.getCount();
    }

    @Override
    public Snapshot getSnapshot() {
        return value.getSnapshot();
    }
}
