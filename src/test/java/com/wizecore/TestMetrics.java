package com.wizecore;

import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.wizecore.metrics.PersistentMetricRegistry;

public class TestMetrics {

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void test() throws IOException, InterruptedException {
		PersistentMetricRegistry reg = new PersistentMetricRegistry();
		Counter cnt = reg.counter("test");
		cnt.inc();
		
		Meter m = reg.meter("testmeter");
		m.mark();
		m.mark(1000);
		
		Histogram hh = reg.histogram("testhist");
		hh.update(111);
		
		Timer tt = reg.timer("testtimer");
		Context ttc = tt.time();
		Thread.sleep(new Random().nextInt(100));
		ttc.close();
	}
}
