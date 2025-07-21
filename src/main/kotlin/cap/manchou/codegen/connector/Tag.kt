package cap.manchou.codegen.connector

data class Tag(
  val name: String,
  val description: String = name,
  val type: TagType = TagType.ASSET,
)
