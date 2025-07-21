package cap.manchou.codegen

import org.apache.commons.lang3.StringUtils.isNotBlank


private fun splitCamelCase(str: String): List<String> {
  if (str.isEmpty()) {
    return emptyList()
  }
  val c = str.toCharArray()
  val list: MutableList<String> = ArrayList()
  var tokenStart = 0
  var currentType = Character.getType(c[tokenStart]).toByte()
  for (pos in tokenStart + 1 until c.size) {
    val type = Character.getType(c[pos]).toByte()
    if (type == currentType) {
      continue
    }
    if (type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
      val newTokenStart = pos - 1
      if (newTokenStart != tokenStart) {
        list.add(String(c, tokenStart, newTokenStart - tokenStart))
        tokenStart = newTokenStart
      }
    }
    currentType = type
  }
  list.add(String(c, tokenStart, c.size - tokenStart))
  return list.toList()
}

/**
 * Splits the input string into words on CamelCase and on the following separators:
 * space ' ', underscore '_', dash '-', dot '.'. Each word is trimmed.
 *
 * @param str the input string of whatever format
 * @return the list of words
 */
fun superSplit(str: String): List<String> {
  return splitCamelCase(str.trim())
    .filter { isNotBlank(it) }
    .stream()
    .flatMap { it.split(' ', '-', '.', '_').toList().stream() }
    .filter { isNotBlank(it) }
    .map { it.trim() }
    .toList()
}

fun label(s: String) = superSplit(s)
  .joinToString(" ").lowercase()

fun lowerCase(s: String) = s.lowercase()

fun capitalize(s: String) = s.replaceFirstChar(Char::uppercase)

fun capitalizedLabel(s: String) = capitalize(label(s))

fun camelCase(s: String) = superSplit(s)
  .mapIndexed { index, it -> if (index > 0) capitalize(it.lowercase()) else it.lowercase() }
  .joinToString("")

fun pascalCase(s: String) = superSplit(s)
  .joinToString("") { capitalize(it.lowercase()) }

fun snakeCase(s: String) = superSplit(s)
  .joinToString("_").lowercase()

fun kebabCase(s: String) = superSplit(s)
  .joinToString("-").lowercase()

fun upperCase(s: String) = superSplit(s)
  .joinToString("_").uppercase()

fun dotCase(s: String) = superSplit(s)
  .joinToString(".").lowercase()

fun titleCase(s: String): String = superSplit(s)
  .joinToString(" ") { capitalize(it.lowercase()) }
