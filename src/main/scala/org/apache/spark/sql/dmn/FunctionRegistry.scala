package org.apache.spark.sql.dmn

import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry.{FUNC_ALIAS, FunctionBuilder}
import org.apache.spark.sql.catalyst.analysis.FunctionRegistryBase
import org.apache.spark.sql.catalyst.expressions.{Expression, ExpressionInfo, NamedArgumentExpression}
import org.apache.spark.sql.errors.QueryCompilationErrors

import scala.reflect.ClassTag

object FunctionRegistry {
  val expressions: Seq[(FunctionIdentifier, ExpressionInfo, FunctionBuilder)] = Seq(
    expression[EvaluateDecisionTable]("evaluate_decision_table")
  )

  /**
   * Forked from org.apache.spark.sql.catalyst.analysis
   *
   * Create a SQL function builder and corresponding `ExpressionInfo`.
   * @param name The function name.
   * @param setAlias The alias name used in SQL representation string.
   * @param since The Spark version since the function is added.
   * @tparam T The actual expression class.
   * @return (function name, (expression information, function builder))
   */
  private def expression[T <: Expression : ClassTag](
                                                      name: String,
                                                      setAlias: Boolean = false,
                                                      since: Option[String] = None): (FunctionIdentifier, ExpressionInfo, FunctionBuilder) = {
    val (expressionInfo, builder) = FunctionRegistryBase.build[T](name, since)
    val newBuilder = (expressions: Seq[Expression]) => {
      if (expressions.exists(_.isInstanceOf[NamedArgumentExpression])) {
        throw QueryCompilationErrors.namedArgumentsNotSupported(name)
      }
      val expr = builder(expressions)
      if (setAlias) expr.setTagValue(FUNC_ALIAS, name)
      expr
    }
    (FunctionIdentifier(name), expressionInfo, newBuilder)
  }
}
