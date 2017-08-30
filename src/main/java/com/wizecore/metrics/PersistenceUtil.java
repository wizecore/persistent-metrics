package com.wizecore.metrics;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import org.redisson.Redisson;
import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides persistance by using Redis instance.
 * If no REDIS_ADDR or REDIS_CONF enviroment variables specifed, uses local default instance.
 * 
 * @see #redisAddr
 * @author Ruslan
 */
public class PersistenceUtil {
	private static Logger log = LoggerFactory.getLogger(PersistenceUtil.class);
	
	/**
	 * Single instance of Redis client
	 */
	private static RedissonClient redis;
	
	/**
	 * Optional config file name. Use REDIS_CONF environment variable to set.
	 * Takes precedence over REDIS_ADDR (@see {@link PersistenceUtil#redisAddr})
	 */
	private static String redisConfig = null;
	
	/**
	 * Optional redis address. Use REDIS_ADDR environment variable to set. Have no effect if REDIS_CONF is set.
	 */
	private static String redisAddr = null;
	
	/**
	 * Optional redis password. Have no effect if REDIS_CONF is set.
	 */
	private static String redisPassword = null;
	
	/**
	 * Common prefix for all values stored. By default <code>metrics.</code>
	 * Can be specified in environment variable METRICS_PREFIX. 
	 * If not ends with ".", dot will be appended to end of it.
	 */
	private static String metricPrefix = null;
	
	/**
	 * Makes lazy initialization of redis client.
	 */
	protected static void init() {
		if (redis == null) {
			if (redisConfig == null) {
				redisConfig = System.getenv("REDIS_CONF");
			}
			
			if (redisAddr == null) {
				redisAddr = System.getenv("REDIS_ADDR");
			}
			
			if (metricPrefix == null) {
				metricPrefix = System.getenv("METRIC_PREFIX");
			}
			
			if (metricPrefix == null) {
				metricPrefix = "metrics";
			}
			
			if (!metricPrefix.endsWith(".")) {
				metricPrefix = metricPrefix + ".";
			}
			
			Config redisConf = null;
			try {
				String src = "defaults";
				if (redisConfig != null) {
					redisConf = Config.fromJSON(new File(redisConfig));
					src = redisConfig;
				} else {
					String addr = "localhost:6379";
					if (redisAddr != null && !redisAddr.equals("")) {
						addr = redisAddr;
						src = redisAddr;
					}
					
					redisConf = new Config();
					SingleServerConfig ss = redisConf.useSingleServer();
					ss.setAddress(addr);
				
					if (redisPassword != null && !redisPassword.equals("")) {
						ss.setPassword(redisPassword);
					}
					
					// Reduce default usage of connections from 1 + 10 to 1
					ss.setConnectionMinimumIdleSize(0);
				}
				
				log.info("Initializing persistent metrics via Redis with " + src);
				redis = redisConf != null ? Redisson.create(redisConf) : Redisson.create();
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException("Redis connection failed with " + redisConf);
			}
		}
	}
	
	public static <T> SortedSet<T> createSortedSet(String name, Class<T> elements) {
		init();
		return redis.getSortedSet(name);
	}
	
	public static RAtomicLong createAtomicLong(String name, long defaultValue) {
		init();
		RAtomicLong v = redis.getAtomicLong(metricPrefix + name);
		if (!v.isExists()) {
			v.set(defaultValue);
		}
		return v;
	}
	
	public static RAtomicLong createAtomicLong(String name) {
		init();
		RAtomicLong v = redis.getAtomicLong(metricPrefix + name);
		if (!v.isExists()) {
			v.set(0);
		}
		return v;
	}

	public static LongAdderAdapter createLongAdderAdapter(String name) {
		final RAtomicLong v = createAtomicLong(name);
		return new LongAdderAdapter() {
			@Override
			public long sumThenReset() {
				long l = v.get();
				v.set(0);
				return l;
			}
			
			@Override
			public long sum() {
				return v.get();
			}
			
			@Override
			public void increment() {
				v.incrementAndGet();
			}
			
			@Override
			public void decrement() {
				v.decrementAndGet();
			}
			
			@Override
			public void add(long x) {
				v.addAndGet(x);
			}
		};
	}

	public static RAtomicDouble createAtomicDouble(String name) {
		init();
		RAtomicDouble v = redis.getAtomicDouble(metricPrefix + name);
		if (!v.isExists()) {
			v.set(0);
		}
		return v;
	}
	
	public static String getValue(String name) {
		init();
		RBucket<String> b = redis.getBucket(name);
		return b.isExists() ? b.get() : null;
	}
	
	public static void setValue(String name, String value) {
		init();
		RBucket<String> b = redis.getBucket(name);
		b.set(value);
	}
	

	public static RBucket<String> getBucket(String name) {
		init();
		return redis.getBucket(name);
	}

	public static String getRedisConfig() {
		return redisConfig;
	}

	public static void setRedisConfig(String redisConfig) {
		PersistenceUtil.redisConfig = redisConfig;
	}

	public static String getRedisAddr() {
		return redisAddr;
	}

	public static void setRedisAddr(String redisAddr) {
		PersistenceUtil.redisAddr = redisAddr;
	}

	public static String getMetricPrefix() {
		return metricPrefix;
	}

	public static void setMetricPrefix(String metricPrefix) {
		PersistenceUtil.metricPrefix = metricPrefix;
	}

	public static String getRedisPassword() {
		return redisPassword;
	}

	public static void setRedisPassword(String redisPassword) {
		PersistenceUtil.redisPassword = redisPassword;
	}
}
