package lox

class ReturnException(val value: Any?): RuntimeException(null, null, false, false)