package org.apache.spark.sql.dmn

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{GenericRowWithSchema, Literal, StructsToJson}
import org.apache.spark.sql.types.{DataType, StructField, StructType}
import org.camunda.bpm.dmn.engine.impl.{DmnDecisionTableImpl, DmnDecisionTableOutputImpl}
import org.camunda.bpm.dmn.engine.{DmnDecision, DmnEngine, DmnEngineConfiguration}
import org.camunda.bpm.engine.variable.VariableMap

import java.io.FileInputStream
import java.util.TimeZone
import scala.collection.mutable

object DmnService {
  @transient private lazy val instance: ThreadLocal[DmnService] = new ThreadLocal[DmnService] {
    override def initialValue(): DmnService = new DmnService()
  }

  def getInstance(): DmnService = instance.get()
}

case class DecisionTable(decisionTable: DmnDecision, outputSchema: StructType)

class DmnService {
  private val dmnEngine: DmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration.buildEngine()
  private val decisionTableInstances = new mutable.HashMap[String, DecisionTable]()

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

  def getOrCreate(dmn: String): DecisionTable = {
    if(!this.decisionTableInstances.contains(dmn)) {
      // Read the DMN file and build the Decision table object
      val inputStream: FileInputStream = new FileInputStream(dmn)
      val decisionTable = dmnEngine.parseDecisions(inputStream).get(0)
      inputStream.close()

      // Extract the decision table metadata to identify the output fields
      val dmnDecisionLogic: DmnDecisionTableImpl = decisionTable.getDecisionLogic.asInstanceOf[DmnDecisionTableImpl]
      val outputFields: Array[StructField] = dmnDecisionLogic.getOutputs.toArray().map(outputField=> {
        val field: DmnDecisionTableOutputImpl = outputField.asInstanceOf[DmnDecisionTableOutputImpl]
        StructField(
          name = field.getName,
          dataType = DataType.fromDDL(field.getTypeDefinition.getTypeName)
        )
      })

      this.decisionTableInstances.put(dmn,
        DecisionTable(decisionTable, StructType(Array(outputFields : _*))))
    }

    decisionTableInstances(dmn)
  }
}