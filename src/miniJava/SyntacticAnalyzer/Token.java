package miniJava.SyntacticAnalyzer;

public class Token {

	public TokenType type;
	public String spelling;
	public SourcePosition posn;
	
	public Token (TokenType type, String spelling, SourcePosition posn) {
		this.type = type;
		this.spelling = spelling;
		this.posn = posn;
	}
	
	public Token copy() {
		return new Token(type,spelling,posn);
	}
	
	public void print() {
		System.out.println("type = " + type + ", spelling = " + spelling + ".");
	}
	
}
