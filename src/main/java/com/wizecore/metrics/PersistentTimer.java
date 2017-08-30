package com.wizecore.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.thoughtworks.xstream.XStream;

/**
 * A timer metric which aggregates timing durations and provides duration statistics, plus
 * throughput statistics via {@link Meter}.
 */
public class PersistentTimer extends Timer implements Persistent {
	private Timer value;
	private String key;
	private RAtomicLong count;
	private RAtomicDouble meanRate;
	private RAtomicDouble m1Rate;
	private RAtomicDouble m5Rate;
	private RAtomicDouble m15Rate;

	public PersistentTimer(String name) {
		this(name, new ExponentiallyDecayingReservoir());
	}
	
	public PersistentTimer(String name, Reservoir reservoir) {
        this(name, reservoir, Clock.defaultClock());
    }

    public PersistentTimer(String name, Reservoir reservoir, Clock clock) {
    	super(reservoir, clock);
    	XStream x = new XStream();
    	key = name + ".xml";
		String xml = PersistenceUtil.getValue(key);
		count = PersistenceUtil.createAtomicLong(name + ".count");
		meanRate = PersistenceUtil.createAtomicDouble(name + ".meanRate");
		m1Rate = PersistenceUtil.createAtomicDouble(name + ".m1Rate");
		m5Rate = PersistenceUtil.createAtomicDouble(name + ".m5Rate");
		m15Rate = PersistenceUtil.createAtomicDouble(name + ".m15Rate");
    	if (xml != null) {
    		value = (Timer) x.fromXML(xml);
    	} else {
    		value = new Timer(reservoir, clock);
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
	public void update(long duration, TimeUnit unit) {
		value.update(duration, unit);
		save();
	}

	@Override
	public <T> T time(Callable<T> event) throws Exception {
		T v = value.time(event);
		save();
		return v;
	}

	@Override
	public void time(Runnable event) {
		value.time(event);
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

	@Override
	public Snapshot getSnapshot() {
		return value.getSnapshot();
	}
}
