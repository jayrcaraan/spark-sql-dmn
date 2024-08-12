package org.apache.spark.sql.dmn

import org.apache.spark.sql.SparkSessionExtensions
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry.FunctionBuilder
import org.apache.spark.sql.catalyst.expressions.ExpressionInfo

class DmnSqlExtensions extends (SparkSessionExtensions => Unit) {
  def apply(sse: SparkSessionExtensions): Unit = {
    FunctionRegistry.expressions.foreach(sse.injectFunction)
  }
}
