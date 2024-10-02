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

import org.apache.spark.sql.{QueryTest, Row, SparkSession}
import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.expressions.SipHash.siphash
import org.apache.spark.sql.functions.lit

/**
 * Test suite for functions in [[org.apache.spark.sql.catalyst.expressions.SipHash]].
 */
class SipHashFunctionsSuite extends QueryTest {
  var spark: SparkSession = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder()
      .master("local[*]")
      .appName("testSipHashFunctions")
      .getOrCreate()
  }

  test("siphash available in functionRegistry") {
    assert(
      spark.sessionState.functionRegistry.lookupFunction(FunctionIdentifier("siphash")).isDefined
    )
  }

  test("hash siphash function") {
    val s = spark
    import s.implicits._

    val df = spark.createDataFrame(Seq(("0101010101010101".getBytes, "Spark"))).toDF("key", "data")

    checkAnswer(
      df.select(siphash($"key", $"data"),
        siphash($"key", $"data", lit(3), lit(5))),
      Row(-2905799387013516250L, 7716273459115555297L))

    checkAnswer(
      df.selectExpr("siphash(key, data)", "siphash(key, data, 3, 5)"),
      Row(-2905799387013516250L, 7716273459115555297L))
  }

  test("siphash available as SQL functions") {
    checkAnswer(spark.sql("SELECT siphash('0000000000000123', 'ABC', 2, 4)"),
      Row(-7940908013966841855L))

    checkAnswer(spark.sql("SELECT siphash('0101010101010101', 'Spark')"),
      Row(-2905799387013516250L))
  }

  test("test expression docs") {
    spark.sql("DESCRIBE FUNCTION siphash;").show(100, false)

    spark.sql("DESCRIBE FUNCTION EXTENDED siphash;").show(100, false)

    spark.sql("DESCRIBE FUNCTION EXTENDED md5;").show(100, false)
  }
}
