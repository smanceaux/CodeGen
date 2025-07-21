package cap.manchou.codegen.generator

import cap.manchou.codegen.generator.resolver.ExpressionResolver

data class ParsedIfExpression(
  val conditionExpression: String,
  val thenExpression: String?,
  val elseExpression: String?,
) {
  fun resolve(resolver: ExpressionResolver): String {
    val conditionVal = resolver.resolveExpression(conditionExpression)
    if (conditionVal == "true" &&
      thenExpression != null
    ) {
      return resolver.resolveExpression(thenExpression)
    }
    if (conditionVal == "false" && elseExpression != null) {
      return resolver.resolveExpression(elseExpression)
    }
    return ""
  }
}
