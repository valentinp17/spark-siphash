# Spark SipHash
[![Test](https://github.com/valentinp17/spark-siphash/actions/workflows/build_test_scala.yml/badge.svg)](https://github.com/valentinp17/spark-siphash/actions/workflows/build_test_scala.yml)
---
Spark-SipHash is a wrapper around Java Function written by whitfin for better performance.

According to my benchmarks it's 2x faster than Java UDF.

See Java code [here](https://github.com/whitfin/siphash-java) 

Right now it's supports Spark versions from 3.2.x to 3.5.x 


### Function registration
To register functions add jar file to spark app:

`--jars spark-siphash_2.12-1.0.0.jar`

`--conf spark.jars=spark-siphash_2.12-1.0.0.jar`

### Usage
DataFrame API and SQL available

DataFrame Functions requires import.
```
import org.apache.spark.sql.catalyst.expressions.SipHash.siphash

df.select(
    siphash($"key", $"data"),  // c = 2, d = 4 by default
    siphash($"key", $"data", lit(3), lit(5))
)
```

The SQL functions will be registered automatically in your Spark session.
```
spark.sql("SELECT siphash('0000000000000123', 'ABC', 3, 5)")
```

### Testing
```sbt test``` \
or \
```sbt +test``` for both 2.12 and 2.13 scala versions

### Benchmark
To run benchmark:

```sbt "test:runMain org.apache.spark.sql.catalyst.expressions.SipHashBenchmark"```

[see more benchmark in source code ](src/test/scala/org/apache/spark/sql/catalyst/expressions/SipHashBenchmark.scala)

#### My results:

[SipHash Benchmark Java 8](benchmarks/SipHashBenchmark-results.txt)\
[SipHash Benchmark Java 11](benchmarks/SipHashBenchmark-jdk11-results.txt)\
[SipHash Benchmark Java 17](benchmarks/SipHashBenchmark-jdk17-results.txt)



### Unfinished and cons
- Supports only BinaryType and StringType  
- Can hash only 1 Column
- Python wrapper ([copy function from here](python/siphash.py))
