package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.HashMap;

import miniJava.ErrorReporter;

public class Scanner {
	private InputStream stream;
	private ErrorReporter reporter;
	
	private char currentChar;
	private String currentSpelling;
	private SourcePosition posn;
	private SourcePosition startposn;
	private boolean eot = false;
	
	private static final HashMap<String, TokenType> hash = new HashMap<String, TokenType>();
	static {
		/*Keywords: INT,BOOL,VOID,CLASS,PUBLIC,PRIVATE,STATIC,
		 *IF,ELSE,WHILE,TRUE,FALSE,THIS,NEW,RETURN,*/
		hash.put("int", TokenType.INT);
		hash.put("boolean", TokenType.BOOL);
		hash.put("void", TokenType.VOID);
		hash.put("class", TokenType.CLASS);
		hash.put("public", TokenType.PUBLIC);
		hash.put("private", TokenType.PRIVATE);
		hash.put("static", TokenType.STATIC);
		hash.put("if", TokenType.IF);
		hash.put("else", TokenType.ELSE);
		hash.put("while", TokenType.WHILE);
		hash.put("null", TokenType.NULL);
		hash.put("true", TokenType.TRUE);
		hash.put("false", TokenType.FALSE);
		hash.put("this", TokenType.THIS);
		hash.put("new", TokenType.NEW);
		hash.put("return", TokenType.RETURN);
	}
	
	public Scanner (InputStream stream, ErrorReporter reporter) {
		this.stream = stream;
		this.reporter = reporter;
		posn = new SourcePosition(1, 0);
		startposn = new SourcePosition(1, 0);
		nextChar();
	}
	
	public Token scan() {
		while (!eot) {
			startposn.line = posn.line;
			startposn.character = posn.character;
			currentSpelling = "";
			while (isNewline())
				nextChar();
			if (eot)
				return new Token(TokenType.EOT, "", startposn);
			start:
			switch(currentChar) {
			case '/':
				acceptIt();
				switch (currentChar) {
				case '/': case '*':
					skipComment();
					currentSpelling = "";
					break start;
				default:
					return new Token(TokenType.DIV, currentSpelling, startposn);
				}
			case ' ': case '\t': 
				nextChar();
				break start;
			default:
				TokenType type = scanToken();
				return new Token(type, currentSpelling, startposn);
			}
		}
			return new Token(TokenType.EOT, "", startposn);
	}
	
	private void skipComment() {
		while (!eot) {
			switch(currentChar) {
				case '/':
					acceptIt();
					while(!isNewline()){
						nextChar();
					}
					return;
				case '*':
					acceptIt();
					while (!eot) {
						while (currentChar != '*' && !eot) {
							isNewline();
							nextChar();
						}
						if (currentChar == '*') {
							acceptIt();
							if (currentChar == '/') {
								nextChar();
								return;
							}
						}
					}
					reporter.reportError("Scan error: Unexpected end of comment");
			}
		}
	}
	
	private boolean isNewline() {
		switch (currentChar) {
			//Linux
			case '\n':
				posn.line++;
				posn.character = 0;
				return true;
			//Windows
			case '\r':
				nextChar();
				if (currentChar == '\n') {
					posn.line++;
					posn.character = 1;
					return true;
				} else {
					eot = true;
					return false;
				}
			default:
				return false;
					
		}
	}
	
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
	
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	public TokenType scanToken() {
		if (eot) {
			System.out.println("Found EOT");
			return TokenType.EOT;
		}
		switch (currentChar) {
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				while (isDigit(currentChar)) {
					acceptIt();
				}
				return TokenType.INTLIT;
			case 'a': case 'b': case 'c': case 'd': case 'e':
			case 'f': case 'g': case 'h': case 'i': case 'j':
			case 'k': case 'l': case 'm': case 'n': case 'o':
			case 'p': case 'q': case 'r': case 's': case 't':
			case 'u': case 'v': case 'w': case 'x': case 'y':
			case 'z':
				
			case 'A': case 'B': case 'C': case 'D': case 'E':
			case 'F': case 'G': case 'H': case 'I': case 'J':
			case 'K': case 'L': case 'M': case 'N': case 'O':
			case 'P': case 'Q': case 'R': case 'S': case 'T':
			case 'U': case 'V': case 'W': case 'X': case 'Y':
			case 'Z':
				acceptIt();
				while (isAlpha(currentChar) || isDigit(currentChar) || currentChar == '_') {
					acceptIt();
				}
				return keywordLookup();
			case '/':
				acceptIt();
				return TokenType.DIV;
			case '+':
				acceptIt();
				return TokenType.PLUS;
			case '*':
				acceptIt();
				return TokenType.MULT;
			case '-':
				acceptIt();
				if (currentChar == '-'){
					return TokenType.ERROR;//Decrement not allowed
				} else {
					return TokenType.SUBT;
				}				
			case '&':
				acceptChar('&');
				acceptChar('&');
				return TokenType.AND;
			case '|':
				acceptChar('|');
				acceptChar('|');
				return TokenType.OR;
			case '!':
				acceptChar('!');
				if (currentChar == '=') {
					acceptChar('=');
					return TokenType.NOTEQ;
				} else {
					return TokenType.NOT;
				}
			case '>':
				acceptChar('>');
				if (currentChar == '=') {
					acceptIt();
					return TokenType.GTE;
				} else {
					return TokenType.GT;
				}
			case '<':
				acceptChar('<');
				if (currentChar == '=') {
					acceptIt();
					return TokenType.LTE;
				} else {
					return TokenType.LT;
				}
			case '=':
				acceptChar('=');
				if (currentChar == '=') {
					acceptIt();
					return TokenType.EQUAL;
				} else {
					return TokenType.ASSIGN;
				}
			case '(':
				acceptIt();
				return TokenType.LPAREN;
			case ')':
				acceptIt();
				return TokenType.RPAREN;
			case '[':
				acceptIt();
				return TokenType.LBRACK;
			case ']':
				acceptIt();
				return TokenType.RBRACK;
			case '{':
				acceptIt();
				return TokenType.LBRACE;
			case '}':
				acceptIt();
				return TokenType.RBRACE;
			case '.':
				acceptIt();
				return TokenType.DOT;
			case ',':
				acceptIt();
				return TokenType.COMMA;
			case ';':
				acceptIt();
				return TokenType.SEMICOL;
			default:
				return TokenType.ERROR;
		}
	}
	
	private TokenType keywordLookup() {
		TokenType value = null;
		value = hash.get(currentSpelling);
		if (value != null) {
			return value;
		} else {
			return TokenType.ID;
		}
	}
	
	private void acceptChar(char c) {
		if (currentChar == c) {
			currentSpelling += c;
			nextChar();
		} else {
			reporter.reportError("Scan error: Expected character '"+c+"', but received '"+currentChar+"'.");
		}
	}
	
	private void acceptIt() {
		currentSpelling += currentChar;
		nextChar();
	}

	private void nextChar() {
		if (eot) {
			return;
		} else {
			try {
				int ch = stream.read();
				currentChar = (char) ch;
				eot = (ch == -1)? true : false;
				posn.character++;
			}
			catch (IOException e) {
				reporter.reportError("Scan Error: I/O Exception");
				eot = true;
			}
		}
	}
}
