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

import org.apache.spark.sql.{Column, SparkSession}
import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.expressions.codegen._
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

// scalastyle:off line.size.limit
@ExpressionDescription(
  usage = """
    _FUNC_(key, data) - Hashes a data input for a given key with c = 2 and d = 4
    _FUNC_(key, data, c, d) - Hashes a data input for a given key, using the provided rounds of compression
  """,
  examples = """
    > SELECT _FUNC_('0101010101010101', 'Spark');
     -2905799387013516250
    > SELECT _FUNC_(CAST('0101010101010101' AS BINARY), 'Spark', 3, 5);
     7716273459115555297
  """,
  since = "3.2.0")
// scalastyle:on line.size.limit
case class SipHash(key: Expression, data: Expression, c: Expression, d: Expression)
  extends QuaternaryExpression with ImplicitCastInputTypes with NullIntolerant {
  def this(key: Expression, data: Expression) =
    this(key, data, Literal(2, IntegerType), Literal(4, IntegerType))

  override def prettyName: String = "siphash"

  override def inputTypes: scala.Seq[AbstractDataType] = Seq(
    TypeCollection(BinaryType, StringType),
    TypeCollection(BinaryType, StringType),
    IntegerType, IntegerType)
  override def dataType: DataType = LongType

  override protected def nullSafeEval(key: Any, data: Any, c: Any, d: Any): Any = {
    def toBytes(value: Any, dataType: DataType): Array[Byte] = dataType match {
      case BinaryType => value.asInstanceOf[Array[Byte]]
      case StringType => value.asInstanceOf[UTF8String].getBytes
    }

    val keyValue: Array[Byte] = toBytes(key, first.dataType)
    val dataValue: Array[Byte] = toBytes(data, second.dataType)

    SipHasher.hash(keyValue, dataValue, c.asInstanceOf[Int], d.asInstanceOf[Int])
  }

  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    def typeHelper(ev: String, dataType: DataType): String = {
      dataType match {
        case BinaryType => s"$ev"
        case StringType => s"""$ev.getBytes()"""
      }
    }

    val sipHasher = classOf[SipHasher].getName
    defineCodeGen(ctx, ev, (eval1, eval2, eval3, eval4) =>
      s"$sipHasher.hash(${typeHelper(eval1, first.dataType)}, " +
        s"${typeHelper(eval2, second.dataType)}, $eval3, $eval4);")
  }

  override def first: Expression = key
  override def second: Expression = data
  override def third: Expression = c
  override def fourth: Expression = d

  override protected def withNewChildrenInternal(
      first: Expression, second: Expression, third: Expression, fourth: Expression): SipHash =
    copy(key = first, data = second, c = third, d = fourth)
}

object SipHash {
  /**
   * Hashes a data input for a given key, using the provided rounds
   * of compression.
   *
   * @param key the key to seed the hash with.
   * @param data the input data to hash.
   * @param c the number of C rounds of compression.
   * @param d the number of D rounds of compression.
   * @since 3.2.0
   */
  def siphash(key: Column, data: Column, c: Column, d: Column): Column = Column {
    SipHash(key.expr, data.expr, c.expr, d.expr)
  }

  /**
   * Hashes a data input for a given key.
   * This will used the default values for C = 2 and D = 2 rounds.
   *
   * @param key the key to seed the hash with.
   * @param data the input data to hash.
   * @since 3.2.0
   */
  def siphash(key: Column, data: Column): Column = Column {
    SipHash(key.expr, data.expr, Literal(2, IntegerType), Literal(4, IntegerType))
  }

  val functionsDescription: (FunctionIdentifier, ExpressionInfo, Seq[Expression] => SipHash) = (
    new FunctionIdentifier("siphash"),
    new ExpressionInfo("org.apache.spark.sql.catalyst.expressions.SipHash", "siphash"),
    (children: Seq[Expression]) => children.size match {
      case 2 => SipHash(children.head, children.last,
        Literal(2, IntegerType), Literal(4, IntegerType))
      case 4 => SipHash(children.head, children(1), children(2), children(3))
    }
  )

  def registerFunction(spark: SparkSession): Unit = {
    spark.sessionState.functionRegistry.registerFunction(
      functionsDescription._1,
      functionsDescription._2,
      functionsDescription._3
    )
  }
}

