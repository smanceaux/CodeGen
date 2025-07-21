package cap.manchou.codegen.generator

import cap.manchou.codegen.generator.error.InvalidExpressionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class CodeGeneratorTest {

  @Test
  fun `should do nothing`() {
    val template = " This template \$contains no {expression}"
    val generator = CodeGenerator(template)

    val actual = generator.generate()

    val expected = template

    assertEquals(expected, actual)
  }

  @Test
  fun `should resolve expression`() {
    val template = " This template contains an \${expression}"
    val generator = CodeGenerator(template)

    val actual = generator.generate(mapOf("expression" to "resolved expression"))

    val expected = " This template contains an resolved expression"

    assertEquals(expected, actual)
  }

  @Test
  fun `should resolve native function`() {
    val template = " This template contains an \${upperCase(expression)}"
    val generator = CodeGenerator(template)

    val actual = generator.generate(mapOf("expression" to "resolved expression"))

    val expected = " This template contains an RESOLVED_EXPRESSION"

    assertEquals(expected, actual)
  }

  @Test
  fun `should resolve a newly register function`() {
    val template = " This template contains an \${quote(expression)}"
    val generator = CodeGenerator(template)
    generator.registerFunction("quote") { s -> "\"$s\"" }

    val actual = generator.generate(mapOf("expression" to "resolved expression"))

    val expected = " This template contains an \"resolved expression\""

    assertEquals(expected, actual)
  }

  @Nested
  inner class IfExpression {
    @Test
    fun `should resolve if true`() {
      val template = " This template contains an \$if{true} then {expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression"
        )
      )

      val expected = " This template contains an resolved expression"

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve if true condition`() {
      val template = " This template contains an \$if{nb == 1} then{expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression",
          "nb" to 1
        )
      )

      val expected = " This template contains an resolved expression"

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve if false condition`() {
      val template = " This template contains an \$if{nb == 1}then {expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression",
          "nb" to 2
        )
      )

      val expected = " This template contains an "

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve if else`() {
      val template =
        " This template contains an \$if{nb == 1} then {expression} else { \"else branch\" }"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression",
          "nb" to 2
        )
      )

      val expected = " This template contains an else branch"

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve if isEmpty function`() {
      val template =
        "\$if{isEmpty(arg0)} then {\"yes\"} else { \"no\" }"
      val generator = CodeGenerator(template)

      assertEquals("yes", generator.generate(""))
      assertEquals("no", generator.generate("a"))
      assertEquals("no", generator.generate("null"))
      assertEquals("no", generator.generate("null"))
      assertEquals("yes", generator.generate(emptyList<Any>()))
      assertEquals("no", generator.generate(listOf("a")))
      assertEquals("no", generator.generate(listOf(null)))
      assertEquals("yes", generator.generate(arrayOf<Any>()))
      assertEquals("no", generator.generate(arrayOf("a")))

      val template1 =
        "\$if{isEmpty(arg0[1])} then {\"yes\"} else { \"no\" }"
      val generator1 = CodeGenerator(template1)
      assertEquals("yes", generator1.generate(listOf(null)))
    }

    @Test
    fun `should resolve if else with no then clause`() {
      val template =
        " This template contains an \$if{nb == 1} else { \"else branch\" }"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression",
          "nb" to 2
        )
      )

      val expected = " This template contains an else branch"

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve empty if no then or else clause`() {
      val template =
        " This template contains an \$if{nb == 1}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression",
          "nb" to 2
        )
      )

      val expected = " This template contains an "

      assertEquals(expected, actual)
    }

    @Test
    fun `should throw exception if unparseable condition`() {
      val template = " This template contains an \$if{nb equals 1} then {expression}"
      val generator = CodeGenerator(template)

      assertThrows<InvalidExpressionException> {
        generator.generate(
          mapOf(
            "expression" to "resolved expression",
            "nb" to 1
          )
        )
      }
    }
  }

  @Nested
  inner class ForExpression {

    @Test
    fun `should resolve for`() {
      val template = " This template contains an \$for(1..nb) {expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression ",
          "nb" to 5
        )
      )

      val expected =
        " This template contains an resolved expression resolved expression resolved expression resolved expression resolved expression "

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve for with 0 step`() {
      val template = " This template contains an \$for(1..list.length) {expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression ",
          "list" to emptyList<String>()
        )
      )

      val expected =
        " This template contains an "

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve for with no step defined by a string variable`() {
      val template = " This template contains an \$for(1..nb) {expression}"
      val generator = CodeGenerator(template)

      val actual = generator.generate(
        mapOf(
          "expression" to "resolved expression ",
          "nb" to "3"
        )
      )

      val expected =
        " This template contains an resolved expression resolved expression resolved expression "

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve for with index`() {
      val template = " This template contains an \$for(i = 1..nb) {quote(i)}"
      val generator = CodeGenerator(template)
      generator.registerFunction("quote") { s -> "\"$s\"" }

      val actual = generator.generate(
        mapOf(
          "nb" to 5
        )
      )

      val expected = " This template contains an \"1\"\"2\"\"3\"\"4\"\"5\""

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve for with positive step`() {
      val template = " This template contains an \$for(i = 3..nb step 2) {quote(i)}"
      val generator = CodeGenerator(template)
      generator.registerFunction("quote") { s -> "\"$s\"" }

      val actual = generator.generate(
        mapOf(
          "nb" to 9
        )
      )

      val expected =
        " This template contains an \"3\"\"5\"\"7\"\"9\""

      assertEquals(expected, actual)
    }

    @Test
    fun `should resolve for with negative step`() {
      val template = " This template contains an \$for(i = 3..nb step -1) {quote(i)}"
      val generator = CodeGenerator(template)
      generator.registerFunction("quote") { s -> "\"$s\"" }

      val actual = generator.generate(
        mapOf(
          "nb" to -2
        )
      )

      val expected =
        " This template contains an \"3\"\"2\"\"1\"\"0\"\"-1\"\"-2\""

      assertEquals(expected, actual)
    }

    @Test
    fun `should throw exception if unparseable range`() {
      val template = " This template contains an \$for(1 to nb) {expression}"
      val generator = CodeGenerator(template)

      assertThrows<InvalidExpressionException> {
        generator.generate(
          mapOf(
            "expression" to "resolved expression ",
            "nb" to 5
          )
        )
      }
    }

    @Test
    fun `should throw exception if range variable is not a number`() {
      val template = " This template contains an \$for(1..nb) {expression}"
      val generator = CodeGenerator(template)

      assertThrows<InvalidExpressionException> {
        generator.generate(
          mapOf(
            "expression" to "resolved expression ",
            "nb" to "Five"
          )
        )
      }
    }
  }

  @Test
  fun `should generate from pattern with for and nested pattern`() {
    val generator = CodeGenerator.loadFromFile(File("src/test/resources/for_pattern.txt"))

    val actual = generator.generate(
      mapOf(
        "items" to listOf(Item("table"), Item("chair"), Item("stool"))
      )
    )

    val expected = """List:
  1. Table,
  2. Chair,
  3. Stool
End."""

    assertEquals(expected, actual)
  }

  private data class Item(val name: String)

  @Test
  fun `should generate with params`() {
    val generator = CodeGenerator.loadFromFile(File("src/test/resources/pattern.txt"))
    generator.registerFunction("quote") { s -> "\"$s\"" }

    val actual = generator.generate("CaMel&_kd-fdéÀàf aa1 Ds.totoAAA")

    val expected = """
camel&_kd-fdéààf aa1 ds.totoaaa CaMel&_kd-fdéÀàf aa1 Ds.totoAAA CaMel&_kd-fdéÀàf aa1 Ds.totoAAA
CAMEL&_KD_FDÉÀÀF_AA1_DS_TOTOAAA CA_MEL&_KD_FDÉ_ÀÀF_AA1_DS_TOTOAAA CA_MEL&_KD_FDÉ_ÀÀF_AA1_DS_TOTOAAA
camel& kd fdéààf aa1 ds totoaaa ca mel& kd fdé ààf aa1 ds totoaaa ca mel& kd fdé ààf aa1 ds totoaaa
camel&_kd-fdéààf aa1 ds.totoaaa camel&_kd-fdéààf aa1 ds.totoaaa camel&_kd-fdéààf aa1 ds.totoaaa
Camel& kd fdéààf aa1 ds totoaaa Ca mel& kd fdé ààf aa1 ds totoaaa Ca mel& kd fdé ààf aa1 ds totoaaa
camel&KdFdéààfAa1DsTotoaaa caMel&KdFdéÀàfAa1DsTotoaaa caMel&KdFdéÀàfAa1DsTotoaaa
camel&-kd-fdéààf-aa1-ds-totoaaa ca-mel&-kd-fdé-ààf-aa1-ds-totoaaa ca-mel&-kd-fdé-ààf-aa1-ds-totoaaa
Camel&KdFdéààfAa1DsTotoaaa CaMel&KdFdéÀàfAa1DsTotoaaa CaMel&KdFdéÀàfAa1DsTotoaaa
camel&_kd_fdéààf_aa1_ds_totoaaa ca_mel&_kd_fdé_ààf_aa1_ds_totoaaa ca_mel&_kd_fdé_ààf_aa1_ds_totoaaa
camel&.kd.fdéààf.aa1.ds.totoaaa ca.mel&.kd.fdé.ààf.aa1.ds.totoaaa ca.mel&.kd.fdé.ààf.aa1.ds.totoaaa
Camel& Kd Fdéààf Aa1 Ds Totoaaa Ca Mel& Kd Fdé Ààf Aa1 Ds Totoaaa Ca Mel& Kd Fdé Ààf Aa1 Ds Totoaaa
"camel&_kd-fdéààf aa1 ds.totoaaa" "CaMel&_kd-fdéÀàf aa1 Ds.totoAAA" "CaMel&_kd-fdéÀàf aa1 Ds.totoAAA"
        """.trim()
    assertEquals(expected, actual)
  }
}
