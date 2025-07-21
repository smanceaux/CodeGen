package cap.manchou.codegen.generator.resolver

import cap.manchou.codegen.generator.error.InvalidExpressionException

enum class Comparator(val expression: String) {
  LTE("<="),
  GTE(">="),
  LT("<"),
  GT(">"),
  EQ("=="),
  NEQ("!=");

  fun resolve(left: Any?, right: Any?): Boolean {

    if (left is Number && right is Number) {
      return compare(left.toDouble(), right.toDouble())
    }
    if (left is String && right is String) {
      return compare(left, right)
    }

    if (this == EQ) {
      return left == right
    }
    if (this == NEQ) {
      return left != right
    }
    throw InvalidExpressionException("Unsupported comparison $expression")
  }

  fun <U> compare(left: Comparable<U>, right: U): Boolean {
    return when (this) {
      LTE -> left <= right
      GTE -> left >= right
      LT -> left < right
      GT -> left > right
      EQ -> left == right
      NEQ -> left != right
    }
  }
}
