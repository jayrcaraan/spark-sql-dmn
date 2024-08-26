package org.apache.spark.sql.dmn

import org.apache.spark.sql.Column
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.unsafe.types.UTF8String

object functions {
  private def withExpr(expr: Expression): Column = new Column(expr)

  /**
   * Returns a struct containing the result of a DMN evaluation
   *
   * @since 1.0.0
   */
  def evaluate_decision_table(data: Column, dmn: Column): Column = {
    withExpr(EvaluateDecisionTable(data.expr, dmn.expr, DmnService.getInstance()
      .getOrCreate(dmn.expr.eval().asInstanceOf[UTF8String].toString).outputSchema))
  }

}
