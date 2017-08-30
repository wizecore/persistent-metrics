package com.wizecore.metrics;

import org.redisson.api.RAtomicLong;

import com.codahale.metrics.Counter;
import com.thoughtworks.xstream.XStream;

/**
 * An incrementing and decrementing counter metric.
 */
public class PersistentCounter extends Counter implements Persistent {
    private Counter value;
    private RAtomicLong counter;
    private String key;

    public PersistentCounter(String name) {
    	XStream x = new XStream();
    	key = name + ".xml";
		String xml = PersistenceUtil.getValue(key);
		counter = PersistenceUtil.createAtomicLong(name);
    	if (xml != null) {
    		value = (Counter) x.fromXML(xml);
    	} else {
    		value = new Counter();
        	save();
    	}
    }
    
    public void save() {
    	XStream x = new XStream();
    	String xml = x.toXML(value);
    	PersistenceUtil.setValue(key, xml);
    	counter.set(getCount());
    }

    /**
     * Increment the counter by one.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Increment the counter by {@code n}.
     *
     * @param n the amount by which the counter will be increased
     */
    public void inc(long n) {
        value.inc(n);
        save();
    }

    /**
     * Decrement the counter by one.
     */
    public void dec() {
        dec(1);
        save();
    }

    /**
     * Decrement the counter by {@code n}.
     *
     * @param n the amount by which the counter will be decreased
     */
    public void dec(long n) {
        value.dec(n);
        save();
    }

    /**
     * Returns the counter's current value.
     *
     * @return the counter's current value
     */
    @Override
    public long getCount() {
        return value.getCount();
    }
}
