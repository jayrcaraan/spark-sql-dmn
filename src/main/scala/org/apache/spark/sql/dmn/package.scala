package org.apache.spark.sql

package object dmn {
  def registerAll(sqlContext: SQLContext): Unit = {
    registerAll(sqlContext.sparkSession)
  }

  def registerAll(sparkSession: SparkSession): Unit = {
    val functionRegistry = sparkSession.sessionState.functionRegistry
      FunctionRegistry.expressions.foreach { case (functionId, info, builder) =>
      functionRegistry.registerFunction(functionId, info, builder)
    }
  }

  def dropAll(sparkSession: SparkSession): Unit = {
    val functionRegistry = sparkSession.sessionState.functionRegistry
    FunctionRegistry.expressions.foreach { case (functionId, _, _) =>
      functionRegistry.dropFunction(functionId)
    }
  }
}
