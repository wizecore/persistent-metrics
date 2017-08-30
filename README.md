# Persistent metrics

Drop-in enhancement for [Dropwizard Metrics](http://metrics.dropwizard.io/) which provide metric persistence using Redis DB via [Redisson](https://github.com/redisson/redisson) library.

Uses [XStream](http://x-stream.github.io/) library for serialization.

## Limitations

__ALPHA QUALITY__ Use only if you intend to help improve it.

  1. Gauge is not supported as it implemented as instant Java method call measurement.
  2. Obvious latency issues. Use only for important selected metrics.

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
	<version>0.6</version>
</dependency>
```

## Configuring Redis

By default it uses locally installed Redis (default port, i.e. 6379)
To configure redis use following environment variables:

  * REDIS_CONF - Redisson JSON [config](https://github.com/redisson/redisson/wiki/2.-Configuration#221-jsonyaml-file-based-configuration) file. Takes precedence.
  * REDIS_ADDR - host:port for single server.

## Configuring prefix

By default all metrics is put in redis using prefix "metrics.".
To configure different prefix use METRIC_PREFIX environment variable (dot at the end is added automatically).

## License

Copyright (c) 2010-2017 Coda Hale, Yammer.com, Wizecore

Published under Apache Software License 2.0, see LICENSE
