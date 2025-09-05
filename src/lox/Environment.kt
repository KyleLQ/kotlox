package lox

class Environment {
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

    // walks distance amount up the chain of environments.
    // ASSUMES that the distance was correctly resolved.
    fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 ..< distance) {
            environment = environment.enclosing!!
        }

        return environment
    }

    // walks a set distance instead of dynamically walking up the chain
    // of environments
    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    // walks a set distance up the environment chain to assign
    // the variable
    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    fun get(name: Token): Any? {
        if (values.contains(name.lexeme)) {
            return values[name.lexeme]
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