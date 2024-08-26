package org.apache.spark.sql.dmn

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{BooleanType, DataType, DateType, DoubleType, FloatType,
  IntegerType, LongType, StringType, StructField, StructType, TimestampNTZType, TimestampType}
import org.camunda.bpm.dmn.engine.impl.{DmnDecisionTableImpl, DmnDecisionTableOutputImpl}
import org.camunda.bpm.dmn.engine.{DmnDecision, DmnEngine, DmnEngineConfiguration}
import org.camunda.bpm.engine.variable.{VariableMap, Variables}

import java.io.FileInputStream
import scala.collection.mutable

case class DecisionTable(decisionTable: DmnDecision, outputSchema: StructType)

object DmnService {
  @transient private lazy val instance: ThreadLocal[DmnService] = new ThreadLocal[DmnService] {
    override def initialValue(): DmnService = new DmnService()
  }

  def getInstance(): DmnService = instance.get()
}

class DmnService {
  private val dmnEngine: DmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration.buildEngine()
  private val decisionTableInstances = new mutable.HashMap[String, DecisionTable]()
  private val supportedDataTypes: Set[DataType] = Set(
    StringType,
    BooleanType,
    IntegerType,
    LongType,
    DoubleType,
    FloatType,
    DateType,
    TimestampType,
    TimestampNTZType
  )

  def evaluateDecisionTable(dmn: String, variables: VariableMap): GenericRowWithSchema = {
    val decisionTableInstance = this.getOrCreate(dmn)
    val result = this.dmnEngine.evaluateDecisionTable(decisionTableInstance.decisionTable, variables).getSingleResult

    if (result != null) {
      new GenericRowWithSchema(result.values().toArray.asInstanceOf[Array[Any]],
        decisionTableInstance.outputSchema)
    } else {
      null
    }
  }

  def structToDmnVariables(schema: StructType, data: InternalRow): VariableMap = {
    // Check for unsupported data types
    val unsupportedFields = schema.fields.filter(field => !supportedDataTypes.contains(field.dataType)).toList
    if (unsupportedFields.nonEmpty) {
      throw new Exception("Unsupported Data Types: " + unsupportedFields.mkString(","))
    }

    // Maps the input data into an object that is compatible with the DMN Engine
    val variables: VariableMap = Variables.createVariables()
    schema.zipWithIndex.foreach(field => {
      variables.put(field._1.name, data.get(field._2, field._1.dataType))
    })

    variables
  }

  def getOrCreate(dmn: String): DecisionTable = {
    if (!this.decisionTableInstances.contains(dmn)) {
      // Read the DMN file and build the Decision table object
      val inputStream: FileInputStream = new FileInputStream(dmn)
      val decisionTable = dmnEngine.parseDecisions(inputStream).get(0)
      inputStream.close()

      // Extract the decision table metadata to identify the output fields
      val dmnDecisionLogic: DmnDecisionTableImpl = decisionTable.getDecisionLogic.asInstanceOf[DmnDecisionTableImpl]
      val outputFields: Array[StructField] = dmnDecisionLogic.getOutputs.toArray().map(outputField => {
        val field: DmnDecisionTableOutputImpl = outputField.asInstanceOf[DmnDecisionTableOutputImpl]
        StructField(
          name = field.getName,
          dataType = DataType.fromDDL(field.getTypeDefinition.getTypeName)
        )
      })

      this.decisionTableInstances.put(dmn,
        DecisionTable(decisionTable, StructType(Array(outputFields: _*))))
    }

    decisionTableInstances(dmn)
  }
}