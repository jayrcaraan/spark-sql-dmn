package org.apache.spark.sql.dmn

import org.apache.log4j.{Level, LogManager}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.functions.expr
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.camunda.bpm.dmn.engine.impl.transform.DmnTransformException
import org.scalatest.flatspec.AnyFlatSpec

class DmnServiceTest extends AnyFlatSpec {

  // Turning off all the logger object as Spark could be noisy sometimes
  LogManager.getRootLogger.setLevel(Level.OFF)

  lazy private val fixture = new {
    val spark: SparkSession = SparkSession.builder
      .appName("spark-test")
      .master("local")
      .getOrCreate()

    val sampleData: DataFrame = spark.sql(
      """
        | SELECT
        |   100 AS customer_id,
        |   CAST('0.9' AS float) AS credit_score,
        |   10000 as loan_amount
        |""".stripMargin)
  }

  "The DmnService" should "enable a creation of a decision table object through a provided DMN XML file path" in {
    val dmnFile = "src/test/resources/sample_dmn.xml"
    assert(DmnService.getInstance().getOrCreate(dmnFile).isInstanceOf[DecisionTable])
  }

  it should "reuse an existing instance of a decision table if the same filepath has been provided" in {
    val dmnFile = "src/test/resources/sample_dmn.xml"
    val decisionTable0 = DmnService.getInstance().getOrCreate(dmnFile)
    val decisionTable1 = DmnService.getInstance().getOrCreate(dmnFile)

    assert(decisionTable0.hashCode() == decisionTable1.hashCode())

    val duplicateDmnFile = "src/test/resources/sample_dmn_duplicate.xml"
    val decisionTable2 = DmnService.getInstance().getOrCreate(duplicateDmnFile)

    assert(decisionTable0.hashCode() != decisionTable2.hashCode())
  }


  it should "throw an exception if the provided DMN XML file is invalid" in {
    val dmnFile = "src/test/resources/sample_broken_dmn.xml"
    assertThrows[DmnTransformException] {
      DmnService.getInstance().getOrCreate(dmnFile)
    }
  }

  it should "enable struct fields to be transformed into a DMN Engine compatible input" in {
    val f = fixture
    val dfWithStruct = f.sampleData.selectExpr("credit_score", "loan_amount")
    val variableMap = DmnService.getInstance().structToDmnVariables(dfWithStruct.schema, InternalRow(dfWithStruct.first().toSeq: _*))
    assert(variableMap.containsKey("credit_score"))
    assert(variableMap.containsKey("loan_amount"))
  }

  it should "throw an exception if a non-supported data type is used as an input" in {
    val f = fixture
    val dfWithStruct = f.sampleData.selectExpr("CAST(credit_score AS DECIMAL(1,1)) AS credit_score", "loan_amount")
    assertThrows[Exception] {
      DmnService.getInstance().structToDmnVariables(dfWithStruct.schema, InternalRow(dfWithStruct.first().toSeq: _*))
    }
  }

  it should "be able to run decision evaluation based on the provided input" in {
    val f = fixture
    val dmnFile = "src/test/resources/sample_dmn.xml"
    /*
     The conditions on the sample DMN are:
       1. If the credit_score is > 0.8 and the loan_amount > 5000 then loan_status = 'approved'
       2. If the credit_score is < 0.8 and the loan_amount > 5000 then loan_status = 'declined'
     */

    var dfWithStruct = f.sampleData.selectExpr("credit_score", "loan_amount")
    var variableMap = DmnService.getInstance().structToDmnVariables(dfWithStruct.schema, InternalRow(dfWithStruct.first().toSeq: _*))
    var evaluation = DmnService.getInstance().evaluateDecisionTable(dmnFile, variableMap)
    assert(evaluation.values(0) == "approved")

    dfWithStruct = dfWithStruct.withColumn("credit_score", expr("CAST(0.7 AS FLOAT)"))
    variableMap = DmnService.getInstance().structToDmnVariables(dfWithStruct.schema, InternalRow(dfWithStruct.first().toSeq: _*))
    evaluation = DmnService.getInstance().evaluateDecisionTable(dmnFile, variableMap)
    assert(evaluation.values(0) == "declined")
  }
}
