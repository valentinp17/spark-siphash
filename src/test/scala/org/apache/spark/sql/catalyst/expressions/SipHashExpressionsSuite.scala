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

import org.scalacheck.Gen

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.types.{BinaryType, DataType, IntegerType, StringType}

class SipHashExpressionsSuite extends SparkFunSuite with ExpressionEvalHelper {

  test("sipHash with String input") {
    val KEY: String = "0000000000000123"
    val KEY_LIT: Literal = Literal(KEY)

    checkEvaluation(SipHash(KEY_LIT, Literal("ABC"), Literal(2), Literal(4)), -7940908013966841855L)
    checkEvaluation(new SipHash(KEY_LIT, Literal("ABC")), -7940908013966841855L)
    checkEvaluation(new SipHash(KEY_LIT, Literal("88005553535")), -3384829560635057694L)
    checkEvaluation(new SipHash(KEY_LIT, Literal("79999999999")), -8494705321812884486L)
    checkEvaluation(new SipHash(KEY_LIT, Literal.create(null, StringType)), null)
    checkConsistencySipHash(SipHash.apply, StringType, StringType)
  }

  test("sipHash with BinaryType input") {
    checkEvaluation(SipHash(Literal("0101010101010101"), Literal("Spark".getBytes),
      Literal(2), Literal(4)), -2905799387013516250L)

    checkEvaluation(SipHash(Literal("0101010101010101".getBytes), Literal("Spark"),
      Literal(3), Literal(5)), 7716273459115555297L)

    checkConsistencySipHash(SipHash.apply, BinaryType, BinaryType)
  }

  test("sipHash result from same input as String and as binary should be equal") {
    checkEvaluation(SipHash(Literal("0101010101010101"), Literal("Spark"),
        Literal(2), Literal(4)), -2905799387013516250L)
    checkEvaluation(SipHash(Literal("0101010101010101".getBytes), Literal("Spark".getBytes),
        Literal(2), Literal(4)), -2905799387013516250L)
  }

  test("sipHash throws exception if key size equals 16") {
    val error1 = intercept[IllegalArgumentException] {
      SipHash(Literal("101010101010101".getBytes), Literal("Spark"), Literal(3), Literal(5)).eval()
    }.getMessage
    assert(error1.contains("Key must be exactly 16 bytes!"))

    val error2 = intercept[IllegalArgumentException] {
      SipHash(Literal("10101010101010101".getBytes), Literal("Spark"),
        Literal(3), Literal(5)).eval()
    }.getMessage
    assert(error2.contains("Key must be exactly 16 bytes!"))
  }

  /**
   * Altered version of checkConsistency to test evaluation results between
   * Interpreted mode and Codegen mode,
   * making sure we have consistent result regardless of the evaluation method we use.
   */
  def checkConsistencySipHash(c: (Expression, Expression, Expression, Expression) => Expression,
                              dataType1: DataType,
                              dataType2: DataType): Unit = {
    lazy val sizedHexGen: (Int, DataType) => Gen[Literal] =
      (n: Int, dataType: DataType) => for {str <- Gen.listOfN(n, Gen.hexChar).map(_.mkString)}
        yield Literal.create(if (dataType == StringType) str else str.getBytes, dataType)

    lazy val siphashKeyGen = sizedHexGen(16, dataType1)
    lazy val hashRounds = Gen.chooseNum(2, 10).map(Literal.create(_, IntegerType))

    forAll(
      siphashKeyGen,
      LiteralGenerator.randomGen(dataType2),
      hashRounds,
      hashRounds
    ) { (l1: Literal, l2: Literal, l3: Literal, l4: Literal) =>
      cmpInterpretWithCodegen(EmptyRow, c(l1, l2, l3, l4))
    }
  }
}
