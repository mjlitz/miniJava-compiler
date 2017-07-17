package miniJava;

import miniJava.AbstractSyntaxTrees.Declaration;

public class ContextError extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public Declaration d;
	public String message;
	
	public ContextError(Declaration d) {
		this.d = d;
	}
	
	public ContextError(String message) {
		this.message = "*** "+message;
	}

	public ContextError() {}

}
