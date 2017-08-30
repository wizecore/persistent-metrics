package com.wizecore.metrics;

import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;

import com.codahale.metrics.Clock;
import com.codahale.metrics.EWMA;
import com.codahale.metrics.Meter;
import com.thoughtworks.xstream.XStream;

/**
 * A meter metric which measures mean throughput and one-, five-, and fifteen-minute
 * exponentially-weighted moving average throughputs.
 *
 * @see EWMA
 */
public class PersistentMeter extends Meter implements Persistent {
	private Meter value;
	private String key;
	private RAtomicLong count;
	private RAtomicDouble meanRate;
	private RAtomicDouble m1Rate;
	private RAtomicDouble m5Rate;
	private RAtomicDouble m15Rate;
	
	public PersistentMeter(String name) {
		 this(name, Clock.defaultClock());
	}

	public PersistentMeter(String name, Clock clock) {
		super(clock);
		XStream x = new XStream();
    	key = name + ".xml";
		String xml = PersistenceUtil.getValue(key);
		count = PersistenceUtil.createAtomicLong(name + ".count");
		meanRate = PersistenceUtil.createAtomicDouble(name + ".meanRate");
		m1Rate = PersistenceUtil.createAtomicDouble(name + ".m1Rate");
		m5Rate = PersistenceUtil.createAtomicDouble(name + ".m5Rate");
		m15Rate = PersistenceUtil.createAtomicDouble(name + ".m15Rate");
    	if (xml != null) {
    		value = (Meter) x.fromXML(xml);
    	} else {
    		value = new Meter(clock);
        	save();
    	}
	}
	 
    @Override
    public void save() {
    	XStream x = new XStream();
    	String xml = x.toXML(value);
    	PersistenceUtil.setValue(key, xml);
    	count.set(getCount());
    	meanRate.set(value.getMeanRate());
    	m1Rate.set(value.getOneMinuteRate());
    	m5Rate.set(value.getFiveMinuteRate());
    	m15Rate.set(value.getFifteenMinuteRate());
    }

	@Override
	public void mark() {
		mark(1);
	}

	@Override
	public void mark(long n) {
		value.mark(n);
		save();
	}

	@Override
	public long getCount() {
		return value.getCount();
	}

	@Override
	public double getFifteenMinuteRate() {
		return value.getFifteenMinuteRate();
	}

	@Override
	public double getFiveMinuteRate() {
		return value.getFiveMinuteRate();
	}

	@Override
	public double getMeanRate() {
		return value.getMeanRate();
	}

	@Override
	public double getOneMinuteRate() {
		return value.getOneMinuteRate();
	}
}
