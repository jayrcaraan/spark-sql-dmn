package org.apache.spark.sql.dmn

import org.apache.log4j.{Level, LogManager}
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec

class SparkSqlExtensionsTest extends AnyFlatSpec {
  // Turning off all the logger object as Spark could be noisy sometimes
  LogManager.getRootLogger.setLevel(Level.OFF)

  "The DmnSqlExtensions" should "be able to register the function into the SparkSession" in {
    val spark = SparkSession.builder
      .appName("spark-sql-dmn")
      .config("spark.sql.extensions", "org.apache.spark.sql.dmn.DmnSqlExtensions")
      .master("local")
      .getOrCreate()

    val count = spark.catalog.listFunctions.filter(_.name == "evaluate_decision_table").count()
    assert(count == 1)
    spark.stop()
  }

  it should "enable management of the functions through the package methods" in {
    val spark = SparkSession.builder
      .appName("spark-sql-dmn")
      .master("local")
      .getOrCreate()

    var count = spark.catalog.listFunctions.filter(_.name == "evaluate_decision_table").count()
    assert(count == 0)

    // Manually trigger the function registration
    org.apache.spark.sql.dmn.registerAll(spark)
    count = spark.catalog.listFunctions.filter(_.name == "evaluate_decision_table").count()
    assert(count == 1)

    // Manually drop the functions from the spark session
    org.apache.spark.sql.dmn.dropAll(spark)
    count = spark.catalog.listFunctions.filter(_.name == "evaluate_decision_table").count()
    assert(count == 0)

    spark.stop()
  }

}
