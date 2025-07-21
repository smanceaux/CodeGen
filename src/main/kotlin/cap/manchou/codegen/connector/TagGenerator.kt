package cap.manchou.codegen.connector

import cap.manchou.codegen.generator.CodeGenerator
import java.io.File

class TagGenerator(val connector: String, val sinceVersion: String = "") {

  private val templatePath = "src/main/resources/connector/connector_tags.txt"

  fun generateTags(vararg tags: Tag): String {
    val generator = CodeGenerator.loadFromFile(File(templatePath))

    val params = mapOf(
      Pair("tags", tags.sortedBy(Tag::name)),
      Pair("connector", connector),
      Pair("sinceVersion", sinceVersion),
    )

    return generator.generate(params)
  }
}
