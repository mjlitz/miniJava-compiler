package miniJava;

public class ErrorReporter {
	
	private boolean has_errors = false;

	public void reportError(String string) {
		System.out.println(string);
		has_errors = true;
	}
	
	public boolean hasErrors() {
		return has_errors;
	}
	
	public void reportContextError(String string) {
		System.out.println("*** Context Error: " + string);
		has_errors = true;
	}
	
	public void reportIDError(String string) {
		System.out.println("*** Identification Error: " + string);
		has_errors = true;
	}

	public void reportTypeError(String string) {
		System.out.println("*** Type Error: " + string);
		has_errors = true;
	}

	public void reportGenerationError(String string) {
		System.out.println("^^^ Generation Error: " + string);
		has_errors = true;
		
	}

}
