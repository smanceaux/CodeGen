package cap.manchou.codegen

import cap.manchou.codegen.connector.Tag
import cap.manchou.codegen.connector.TagGenerator
import java.io.File

fun main(vararg args: String) {
  generateDlImg()
}

fun generateConnectorTags() {
  print(
    TagGenerator("Connector Name", "2.505")
      .generateTags(
        Tag("ACCESSIBLE_FROM_VPN"),
        Tag("WIDE_INTERNET_EXPOSURE"),
        Tag("ACCESSIBLE_FROM_OTHER_VNETS"),
        Tag("ACCESSIBLE_FROM_OTHER_SUBSCRIPTIONS"),
        Tag("SENSITIVE_DATA"),
      )
  )
}

fun generateDlImg() {
  val generator = CodeGenerator.loadFromFile(File("src/main/resources/curl/dl_img.txt"))

  val domain = "https://www.example.com/";

  val code = "CODE123456"
  val idsNina = listOf(
    "idabcd1234", "idabcd1235", "id13568986"
  )

  idsNina.forEach { id ->
    println(generator.generate(mapOf("domain" to domain, "id" to id, "code" to code)))
  }
}

fun generateEnums() {
  val generator = CodeGenerator.loadFromFile(File("src/main/resources/enumValue.txt"))
  val values = listOf(
    "value_one",
    "value_two",
  ).sorted()

  values.forEach {
    print(
      generator.generate(
        it,
        mapOf(Pair("toto", "yolo")),
        DummyObject("dummy", mapOf(Pair("toto", "yolo")))
      )
    )
  }
}

data class DummyObject(private val name: String, private val map: Map<String, String>)
