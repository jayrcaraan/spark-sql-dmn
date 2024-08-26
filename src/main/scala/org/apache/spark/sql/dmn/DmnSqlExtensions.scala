package org.apache.spark.sql.dmn

import org.apache.spark.sql.{SparkSessionExtensions, SparkSessionExtensionsProvider}

class DmnSqlExtensions extends SparkSessionExtensionsProvider {
  override def apply(sse: SparkSessionExtensions): Unit = {
    FunctionRegistry.expressions.foreach(sse.injectFunction)
  }
}
