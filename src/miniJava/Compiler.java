package miniJava;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.ContextualAnalyzer.ContextualAnalyzer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

/**
 * Recognize whether input is an arithmetic expression as defined by
 * a simple context free grammar for expressions and a scanner grammar.
 * 
 */
public class Compiler {


	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {
		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				File file = new File(args[0]);
				System.out.println(file.getAbsolutePath());
				inputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}
		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);

		System.out.println("Syntactic analysis ... ");
		AST ast = parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		ContextualAnalyzer analyzer = new ContextualAnalyzer(reporter);
		CodeGenerator cg = new CodeGenerator(reporter, args[0]);
		if (reporter.hasErrors()) {
			System.out.println("INVALID expression");
			System.exit(4);
		} else {
			System.out.println("valid program\n");
			ASTDisplay a = new ASTDisplay();
			a.showTree(ast);
			analyzer.analyzeTree(ast);
			if (reporter.hasErrors()) {
				System.out.println("\nContextual errors exist. Code generation cannot proceed.");
				System.exit(4);
			} else {
				cg.generateCode(ast);
				if (reporter.hasErrors()) {
					System.out.println("\nCode Generation errors exist.");
					System.exit(4);
				}
				System.exit(0);
			}
		}
	}
}



