package org.apache.spark.sql.dmn

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.codegen.CodegenFallback
import org.apache.spark.sql.catalyst.expressions.{BinaryExpression, Expression, ExpressionDescription, ImplicitCastInputTypes, NullIntolerant}
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String
import org.camunda.bpm.engine.variable.{VariableMap, Variables}

@ExpressionDescription(
  usage = "_FUNC_(data, dmn) - Returns a struct containing the result of a DMN evaluation.",
  examples = """
    Examples:
      > SELECT _FUNC_(named_struct('col1', 1), 'decision_table.dmn');
       {"result": true}
  """,
  since = "1.0.0")
case class EvaluateDecisionTable(left: Expression, right: Expression, dataType: DataType = null)
  extends BinaryExpression
    with CodegenFallback
    with ImplicitCastInputTypes
    with NullIntolerant {

  def this(left: Expression, right: Expression) = {
    this(left, right, DmnService.getInstance()
      .getOrCreate(right.eval().asInstanceOf[UTF8String].toString).outputSchema)
  }

  override def inputTypes: Seq[AbstractDataType] = Seq(StructType, StringType)

  override protected def nullSafeEval(leftAny: Any, rightAny: Any): Any = {
    val inputRow = leftAny.asInstanceOf[InternalRow]
    val variables: VariableMap = this.structToDmnVariables(left.dataType.asInstanceOf[StructType], inputRow)
    val result = DmnService.getInstance().evaluateDecisionTable(rightAny.asInstanceOf[UTF8String].toString, variables)

    //val outputRow = ArrayBuffer(inputRow.toSeq(left.dataType.asInstanceOf[StructType]): _*)
    //outputRow += UTF8String.fromString(result)
    //InternalRow(outputRow : _*)

    result
  }

  override protected def withNewChildrenInternal(newLeft: Expression, newRight: Expression): EvaluateDecisionTable =
    copy(left = newLeft, right = newRight)

  private def structToDmnVariables(schema: StructType, data: InternalRow): VariableMap = {
    val variables: VariableMap = Variables.createVariables()
    schema.zipWithIndex.foreach(field => {
      variables.put(field._1.name, data.get(field._2, field._1.dataType))
    })

    variables
  }

  override def prettyName: String = "evaluate_decision_table"
}