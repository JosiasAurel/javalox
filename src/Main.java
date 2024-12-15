import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


enum TokenType {
    // signle character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // one or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // literals
    IDENTIFIER, STRING, NUMBER,

    // keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
}

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private static final Map<String, TokenType> keywords;

    private int start = 0;
    private int current = 0;
    private int line = 1;

    static {
      keywords = new HashMap<>();
      keywords.put("and",    TokenType.AND);
      keywords.put("class",  TokenType.CLASS);
      keywords.put("else",   TokenType.ELSE);
      keywords.put("false",  TokenType.FALSE);
      keywords.put("for",    TokenType.FOR);
      keywords.put("fun",    TokenType.FUN);
      keywords.put("if",     TokenType.IF);
      keywords.put("nil",    TokenType.NIL);
      keywords.put("or",     TokenType.OR);
      keywords.put("print",  TokenType.PRINT);
      keywords.put("return", TokenType.RETURN);
      keywords.put("super",  TokenType.SUPER);
      keywords.put("this",   TokenType.THIS);
      keywords.put("true",   TokenType.TRUE);
      keywords.put("var",    TokenType.VAR);
      keywords.put("while",  TokenType.WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
      char c = advance();
      switch (c) {
        case '(': addToken(TokenType.LEFT_PAREN); break;
        case ')': addToken(TokenType.RIGHT_PAREN); break;
        case '{': addToken(TokenType.LEFT_BRACE); break;
        case '}': addToken(TokenType.RIGHT_BRACE); break;
        case ',': addToken(TokenType.COMMA); break;
        case '.': addToken(TokenType.DOT); break;
        case '-': addToken(TokenType.MINUS); break;
        case '+': addToken(TokenType.PLUS); break;
        case ';': addToken(TokenType.SEMICOLON); break;
        case '*': addToken(TokenType.STAR); break;
        case '!': 
                  addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                  break;
        case '=':
                  addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                  break;
        case '<':
                  addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                  break;
        case '>':
                  addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                  break;
        case '/':
                  if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                  } else if (match('*')) {
                    // handle support for c-style comments here
                    while (peek() != '*' && peekNext() != '/' && !isAtEnd()) advance();
                    // consume the remaining * and / tokens
                    advance(); advance();
                  } else {
                    addToken(TokenType.SLASH);
                  }
                  break;
        case ' ':
        case '\r':
        case '\t':
                  // ignore whitespace
                  break;
        case '\n':
                  line++;
                  break;
        case '"': string(); break;
        default:
          if (isDigit(c)) {
            number();
          } else if (isAlpha(c)) {
            identifier();
          } else {
            // Lox.error(line, "Unexpected character.");
          }
          break;
      }
    }

    private boolean isAtEnd() {
      return current >= source.length();
    }

    private void identifier() {
      while (isAlphaNumeric(peek())) advance();

      String text = source.substring(start, current);
      TokenType type = keywords.get(text);

      if (type == null) type = TokenType.IDENTIFIER;
      addToken(type);
    }

    private boolean isAlpha(char c) {
      return (c >= 'a' && c <= 'z') ||
              (c >= 'A' && c <= 'Z') ||
              c == '_';
    }

    private boolean isAlphaNumeric(char c) {
      return isAlpha(c) || isDigit(c);
    }
    
    private boolean isDigit(char c) {
      return c >= '0' && c <= '9';
    }

    private void number() {
      while (isDigit(peek())) advance();

      // Look for a fractional part
      if (peek() == '.' && isDigit(peekNext())) {
        advance(); // fractional part has at least one digit

        while (isDigit(peek())) advance();
      }

      addToken(TokenType.NUMBER,
          Double.parseDouble(source.substring(start, current))
          );
    }

    private void string() {
      while (peek() != '"' && !isAtEnd()) {
        // our string can span multiple lines
        if (peek() == '\n') line++;
        advance();
      }

      // if we're at the end of the file without having terminated our string
      // then we throw an unterminated string error
      if (isAtEnd()) {
        // Lox.error(line, "Unterminated string.");
        return; 
      }

      // move past the '"'
      advance();

      String value = source.substring(start + 1, current - 1);
      addToken(TokenType.STRING, value);
    }

    private char peek() {
      if (isAtEnd()) return '\0';
      return source.charAt(current);
    }

    private char peekNext() {
      if (current + 1 >= source.length()) return '\0';
      return source.charAt(current + 1);
    }

    private boolean match(char expected) {
      char next = peek();
      if (next != expected || next == '\0') return false;

      // if (isAtEnd()) return false;
      // if (source.charAt(current) != expected) return false;

      current++;
      return true;
    }

    private char advance() {
      return source.charAt(current++);
    }

    private void addToken(TokenType type) {
      addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
      String text = source.substring(start, current);
      tokens.add(new Token(type, text, literal, line));
    }
}

public class Main {
    static boolean hadError = false;
    public static void main(String[] args) throws IOException {
      /*
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(
            new Expr.Literal(45.67)));

    System.out.println(new AstPrinter().print(expression));
    */

        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // print all the tokens
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void report(int line, String where, String message) {
        System.err.println("[Line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
  }
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return paranthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return paranthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return paranthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  private String paranthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }
}
