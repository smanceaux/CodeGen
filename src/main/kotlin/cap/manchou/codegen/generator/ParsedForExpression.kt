package cap.manchou.codegen.generator

data class ParsedForExpression(
  val indexName: String?,
  val intProgression: IntProgression,
  val toResolveExpression: String,
)
