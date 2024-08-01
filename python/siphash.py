from pyspark.sql.column import Column, _to_java_column


def siphash(key, data) -> Column:
    sc = SparkContext._active_spark_context
    key = _to_java_column(key)
    data = _to_java_column(data)
    jc = sc._jvm.org.apache.spark.sql.catalyst.expressions.SipHash.siphash(key, data)
    return Column(jc)


# usage example
spark.range(10)\
    .withColumn("data", F.col("id").cast(StringType()))\
    .select(siphash(F.lit("0000000000000123"), F.col("data")))\
    .write.format("noop").mode("overwrite").save()

