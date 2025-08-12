package tool

import java.io.PrintWriter
import kotlin.system.exitProcess

/**
 * Used to generate AST related classes instead of manually typing them out
 * Output dir = src/lox
 */

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(outputDir, "Expr", listOf(
        "Binary   ; val left: Expr, val operator: Token, val right: Expr",
        "Grouping ; val expression: Expr",
        "Literal  ; val value: Any?",
        "Unary    ; val operator: Token, val right: Expr"
    ))
}

// baseName = name of the base class that the classes in types inherit from
// types = each entry  provides the name of the subclass, it's fields, and the types of those fields
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")
    writer.println("package lox")
    writer.println("")
    defineVisitor(writer, baseName, types)
    writer.println("")
    writer.println("abstract class $baseName {")
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")
    writer.println("}")
    writer.println("")

    // AST subclasses
    for (type in types) {
        val className = type.split(";")[0].trim()
        val fields = type.split(";")[1].trim()
        defineType(writer, baseName, className, fields)
    }
    writer.close()
}

private fun defineVisitor(
    writer: PrintWriter,
    baseName: String,
    types: List<String>
) {
    writer.println("interface Visitor<R> {")

    for (type in types) {
        val typeName = type.split(";")[0].trim()
        writer.println("    fun visit${typeName}${baseName}(${baseName.lowercase()}: ${typeName}): R")
    }

    writer.println("}")
}

private fun defineType(
    writer: PrintWriter,
    baseName: String,
    className: String,
    fieldList: String
) {
    writer.println("class $className($fieldList): $baseName() {")
    writer.println("    override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("        return visitor.visit${className}${baseName}(this)")
    writer.println("    }")
    writer.println("}")
    writer.println("")
}
