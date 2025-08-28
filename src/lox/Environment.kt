package lox

class Environment {
    // value for variable if it has not been initialized or assigned
    object UninitValue

    private val enclosing: Environment?
    private val values = mutableMapOf<String, Any?>()

    constructor() {
        enclosing = null
    }

    constructor(enclosing: Environment) {
        this.enclosing = enclosing
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.contains(name.lexeme)) {
            val value = values[name.lexeme]
            if (value == UninitValue) {
                throw RuntimeError(name, "Variable ${name.lexeme} has not been initialized or assigned!")
            }
            return value
        }

        if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.contains(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
    }
}