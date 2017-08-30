# Persistent metrics

Drop-in enhancement for [Dropwizard Metrics](http://metrics.dropwizard.io/) which provide metric persistence using Redis DB via [Redisson](https://github.com/redisson/redisson) library.

Uses [XStream](http://x-stream.github.io/) library for serialization.

## Limitations

__ALPHA QUALITY__ Use only if you intend to help improve it.

  1. Gauge implemented as passthrough metric. Only saved if you call Gauge.getValue() method, either by yourself or by reporter.
  2. Obvious latency issues. Use only for important selected metrics.
  3. Values use custom serialization

## Usage

Replace your usual
```java 
new MetricRegistry();
```

with improved

```java
new PersistentMetricRegistry();
```

## Maven repository

Maven repository is created using [jitpack.io](https://jitpack.io/) [![](https://jitpack.io/v/com.wizecore/persistent-metrics.svg)](https://jitpack.io/#com.wizecore/persistent-metrics). Configure maven using following steps.

### Step 1. Add repository
```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

### Step 2. Add the dependency

```xml
<dependency>
	<groupId>com.wizecore</groupId>
	<artifactId>persistent-metrics</artifactId>
	<version>0.8</version>
</dependency>
```

## Configuring 

By default it uses locally installed Redis (default port, i.e. 6379)
To configure use following environment variables:

  * REDIS_CONF - Redisson JSON [config](https://github.com/redisson/redisson/wiki/2.-Configuration#221-jsonyaml-file-based-configuration) file. Takes precedence.
  * REDIS_ADDR - host:port for single server. Have no effect if REDIS_CONF is defined.
  * METRIC_PREFIX - Prefix for all values stored. Default is "metrics.". Dot at the end is added automatically.
  * REDIS_PASSWORD - Password for single server. Have no effect if REDIS_CONF is defined.

## License

Copyright (c) 2010-2017 Coda Hale, Yammer.com, Wizecore

Published under Apache Software License 2.0, see LICENSE
