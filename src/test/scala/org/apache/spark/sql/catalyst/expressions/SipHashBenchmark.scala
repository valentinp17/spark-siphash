/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.expressions

import org.apache.spark.benchmark.Benchmark
import org.apache.spark.internal.config.MAX_RESULT_SIZE
import org.apache.spark.internal.config.UI.UI_ENABLED
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.SipHash.siphash
import org.apache.spark.sql.execution.benchmark.SqlBasedBenchmark
import org.apache.spark.sql.functions.{col, lit}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types.{LongType, StringType}

/**
 * Synthetic benchmark for SipHash UDF and Expression.
 * To run this benchmark:
 * {{{
 *   1. without sbt:
 *      bin/spark-submit --class <this class>
 *        --jars <spark core test jar>,<spark catalyst test jar> <sql core test jar>
 *   2. sbt "test:runMain org.apache.spark.sql.catalyst.expressions.SipHashBenchmark"
 *   3. generate result:
 *      SPARK_GENERATE_BENCHMARK_FILES=1 sbt "test:runMain <this class>"
 *      Results will be written to "benchmarks/SipHashBenchmark-results.txt".
 * }}}
 */
object SipHashBenchmark extends SqlBasedBenchmark {

  override def getSparkSession: SparkSession = {
    SparkSession.builder()
      .master("local[1]")
      .appName(this.getClass.getCanonicalName)
      .config(SQLConf.SHUFFLE_PARTITIONS.key, 1)
      .config(SQLConf.AUTO_BROADCASTJOIN_THRESHOLD.key, 1)
      .config(UI_ENABLED.key, false)
      .config(MAX_RESULT_SIZE.key, "3g")
      .config("spark.driver.bindAddress", "127.0.0.1")
      .getOrCreate()
  }

  val KEY: String = "0000000000000123"

  private def runBenchmarkExpression(cardinality: Int): Unit = {
    val idCol = col("id")
    val dataCol = col("data")

    spark.range(cardinality)
      .withColumn("data", idCol.cast(StringType))
      .select(siphash(lit(KEY), dataCol))
      .noop()
  }

  private def runBenchmarkUDF(cardinality: Int): Unit = {
    val idCol = col("id")
    val dataCol = col("data")

    spark.range(cardinality)
      .withColumn("data", idCol.cast(StringType))
      .selectExpr(s"siphashJava('$KEY', $dataCol)")
      .noop()
  }

  override def runBenchmarkSuite(mainArgs: Array[String]): Unit = {
    val cardinality = 10000000
    runBenchmark("SipHash") {

      codegenBenchmark("SipHash as Spark Expression", cardinality) {
        runBenchmarkExpression(cardinality)
      }

      codegenBenchmark("SipHash as Java Udf", cardinality) {
        spark.udf.register("siphashJava", new SipHasherUdf(), LongType)
        runBenchmarkUDF(cardinality)
      }

      val benchmark = new Benchmark("Expression and Java UDF", cardinality, output = output)


      benchmark.addCase("Java UDF", numIters = 15) { x =>
        spark.udf.register("siphashJava", new SipHasherUdf(), LongType)

        spark.range(cardinality)
          .withColumn("data", col("id").cast(StringType))
          .selectExpr(s"siphashJava('$KEY', data)")
          .noop()
      }

      benchmark.addCase("Expression", numIters = 15) { _ =>
        spark.range(cardinality)
          .withColumn("data", col("id").cast(StringType))
          .select(siphash(lit(KEY), col("data")))
          .noop()
      }

      benchmark.run()
    }
  }
}

