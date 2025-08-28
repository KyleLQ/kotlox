package lox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


val lox = Lox()
fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: kotlox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        lox.runFile(args[0])
    } else {
        lox.runPrompt()
    }
}

class Lox {
    // TODO!!! DO I NEED THESE TO BE STATIC!?!
    private val interpreter = Interpreter()
    var hadError: Boolean = false
    var hadRuntimeError: Boolean = false
    var isRepl = false

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)
        isRepl = true

        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        // stop if there was a syntax error
        if (hadError) return

        interpreter.interpret(statements)
    }

    // todo more detailed error reporting
    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        println("[line ${line}] Error ${where}: $message")
        hadError = true
    }

    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, "at \'${token.lexeme}\'", message)
        }
    }

    fun runtimeError(error: RuntimeError) {
        println("${error.message} \n[line ${error.token.line}]")
        hadRuntimeError = true
    }
}