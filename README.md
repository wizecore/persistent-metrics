# Persistent metrics

Drop-in enhancement for [Dropwizard Metrics](http://metrics.dropwizard.io/) which provide metric persistence using Redis DB via [Redisson](https://github.com/redisson/redisson) library.

## Limitations

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

## Configuring Redis

By default it uses locally installed Redis (default port, i.e. 6379)
To configure redis use following environment variables:

  * REDIS_CONF - Redisson JSON [config](https://github.com/redisson/redisson/wiki/2.-Configuration#221-jsonyaml-file-based-configuration) file. Takes precedence.
  * REDIS_ADDR - host:port for single server.

## Configuring prefix

By default all metrics is put in redis using prefix "metrics.".
To configure different prefix use METRIC_PREFIX environment variable (dot at the end is added automatically).


