package lox

class Scanner (private val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()

    private var start = 0 // the index of the first character of the lexeme being scanned
    private var current = 0 // the current character index being considered
    private var line = 1 // tracks line number of current character

    fun scanTokens() :List<Token> {
        while(!isAtEnd()) {
            // at beginning of next lexeme
            start = current
            scanToken()
        }

        // add EOF token at end
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when(c) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) {
                // skip entire line if comment
                while (peek() != '\n' && !isAtEnd()) advance()
            } else {
                addToken(TokenType.SLASH)
            }
            ' ', '\r', '\t' -> return
            '\n' -> line++
            '"' -> string()
            else -> {
                if (isDigit(c)) {
                    number()
                } else {
                    lox.error(line, "Unexpected character")
                }
            }
        }
    }

    // handle number literals
    private fun number() {
        while (isDigit(peek())) advance()

        // check if is fraction
        if (peek() =='.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) advance()
        }

        addToken(TokenType.NUMBER, source.substring(start,current).toDouble())
    }

    // handle string literals
    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            // allows multi-line comments
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            lox.error(line, "unterminated string.")
            return
        }

        // advance past the closing quotation mark
        advance()

        // trim surrounding quotes, when we use it later
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return 0.toChar() // null terminating character, even though kotlin doesn't use null terminated strings? todo?
        return source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return 0.toChar() // todo?
        return source[current + 1]
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }
}