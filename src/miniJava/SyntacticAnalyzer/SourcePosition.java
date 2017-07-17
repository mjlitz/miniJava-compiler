package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int line;
	public int character;
	
	public SourcePosition( int l, int c) {
		this.line = l;
		this.character = c;
	}
	
	public String toString() {
		return "posn : (" + line + "," + character + ")";
	}

	public SourcePosition copy() {
		return new SourcePosition(line,character);
	}
}
