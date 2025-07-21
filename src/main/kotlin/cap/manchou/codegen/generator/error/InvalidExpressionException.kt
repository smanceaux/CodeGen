package cap.manchou.codegen.generator.error

class InvalidExpressionException : Exception {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
}
