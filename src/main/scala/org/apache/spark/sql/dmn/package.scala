package org.apache.spark.sql

/***
 * Backup package methods in case a user would prefer to manually register / deregister the function
 */
package object dmn {
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
