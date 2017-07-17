package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {

	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = false;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}

	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;	
	}
	
	class ContextError extends Error {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * start parse
	 */
	public AST parse() {
		try {
			AST ast = parsePackage();
			ast.posn = new SourcePosition(1,1);
			return ast;
		}
		catch (SyntaxError e) {}
		return null;
	}
	
	private Package parsePackage() {
		ClassDeclList clist = parseProgram();
		return new Package(clist,token.posn.copy());
	}

	private ClassDeclList parseProgram() {
		try {
			ClassDeclList clist = new ClassDeclList();
			token = scanner.scan();
			while (token.type == TokenType.CLASS) {
				clist.add(parseClass());
			}
			accept(TokenType.EOT);
			return clist;
		} catch (SyntaxError e) {}
		return null;
	}
	
	private ClassDecl parseClass() {
		SourcePosition posn = token.posn.copy();
		FieldDeclList flist = new FieldDeclList();
		MethodDeclList mlist = new MethodDeclList();
		accept(TokenType.CLASS);
		String id = token.spelling;
		accept(TokenType.ID);
		accept(TokenType.LBRACE);
		while (token.type != TokenType.RBRACE) {
			SourcePosition p = token.posn.copy();
			boolean isPrivate = false;
			boolean isStatic = false;
			boolean doneMethod = false;
			//Left-factored: Visibility, Accessibility, Type
			//Visibility
			if (token.type == TokenType.PUBLIC)
				acceptIt();
			else if (token.type == TokenType.PRIVATE) {
				isPrivate = true;
				acceptIt();
			}
			//Accessibility
			if (token.type == TokenType.STATIC) {
				isStatic = true;
				acceptIt();
			}
			//Type || void
			Type t = new BaseType(TypeKind.VOID,token.posn);
			switch(token.type) {
				case BOOL: case INT: case ID:
					t = parseType(token);
					break;
				case VOID:
					acceptIt();
					String name = token.spelling;
					accept(TokenType.ID);
					accept(TokenType.LPAREN);
					ParameterDeclList plist = parseParameters();
					accept(TokenType.RPAREN);
					accept(TokenType.LBRACE);
					StatementList slist = parseStatements();
					accept(TokenType.RBRACE);
					doneMethod = true;
					mlist.add(new MethodDecl(new FieldDecl(isPrivate,isStatic,t,name,p),plist,slist,p));
					break;
				default:
					parseError("expecting VOID, BOOL, INT[], INT, ID, ID[], or RBRACE, but found '" + token.type + "'");
					return null;
			}
			if (!doneMethod){
			String name = token.spelling;
			accept(TokenType.ID);
				if (token.type == TokenType.LPAREN) {
					accept(TokenType.LPAREN);
					ParameterDeclList plist = parseParameters();
					accept(TokenType.RPAREN);
					accept(TokenType.LBRACE);
					StatementList slist = parseStatements();
					accept(TokenType.RBRACE);
					mlist.add(new MethodDecl(new FieldDecl(isPrivate,isStatic,t,name,p),plist,slist,p));
				} else if (token.type == TokenType.SEMICOL){
					accept(TokenType.SEMICOL);
					flist.add(new FieldDecl(isPrivate,isStatic,t,name,p));
				}
			}
		}
		accept(TokenType.RBRACE);
		return new ClassDecl(id,flist,mlist,posn);
	}
	
	private Type parseType(Token t) {
		SourcePosition p = token.posn.copy();
		switch(token.type) {
			case BOOL:
				acceptIt();
				return new BaseType(TypeKind.BOOLEAN, p);
			case INT:
				BaseType i = new BaseType(TypeKind.INT, p);
				acceptIt();
				if (token.type == TokenType.LBRACK){
					accept(TokenType.LBRACK);
					accept(TokenType.RBRACK);
					return new ArrayType(i,p);
				} else {
					return i;
				}
			case ID:
				ClassType c = new ClassType(new Identifier(t), token.posn.copy());
				acceptIt();
				if (token.type == TokenType.LBRACK){
					accept(TokenType.LBRACK);
					accept(TokenType.RBRACK);
					return new ArrayType(c,token.posn);
				} else {
					return c;
				}
			default:
				parseError("expecting VOID, BOOL, INT[], INT, ID, ID[], or RBRACE, but found '" + token.type + "'");
				return new BaseType(TypeKind.UNSUPPORTED, token.posn.copy());
		}
	}
	
	private ParameterDeclList parseParameters() {
		ParameterDeclList plist = new ParameterDeclList();
		if (token.type != TokenType.RPAREN) {
			Type t = parseType(token);
			Token tok = token.copy();
			accept(TokenType.ID);
			plist.add(new ParameterDecl(t,tok.spelling,tok.posn.copy()));
		}
		while (token.type != TokenType.RPAREN) {
			accept(TokenType.COMMA);
			Type t = parseType(token);
			Token tok = token;
			accept(TokenType.ID);
			plist.add(new ParameterDecl(t,tok.spelling,tok.posn.copy()));
		}
		return plist;
	}
	
	private ExprList parseArguments() {
		ExprList elist = new ExprList();
		if (token.type != TokenType.RPAREN)
			elist.add(parseExpression());
		while (token.type != TokenType.RPAREN) {
			accept(TokenType.COMMA);
			elist.add(parseExpression());
		}
		return elist;
	}
	
	//first this | id already parsed because of left-factoring required for Statement and Expression
	//Previous token t is passed in as argument
	private Reference parseReference(Token t) {
		SourcePosition posn = t.posn.copy();
		if (token.type == TokenType.DOT) {
			accept(TokenType.DOT);
			switch (token.type){
			case ID:
				Token id = token;
				acceptIt();
				Reference r = parseReference(id);
				if (t.type == TokenType.THIS) {
					return new QualifiedRef(new ThisRef(t.posn.copy()),new Identifier(id),id.posn.copy());
				} else {
					return new QualifiedRef(r,new Identifier(t), id.posn.copy());
				}
			default:
				parseError("Expecting ID, but found " + token.type + ".");
				return null;
			}
		} else {
			switch (t.type) {
			case THIS:
				return new ThisRef(posn);
			case ID:
				return new IdRef(new Identifier(t), posn);
			default:
				parseError("Expecting ID or THIS, but found " + token.type + ".");
			}
		}
		parseError("Expecting DOT, ID, or THIS, but found " + token.type + ".");
		return null;
	}
	
	//first this | id already parsed because of left-factoring required for Statement and Expression

	
	private StatementList parseStatements() {
		StatementList slist = new StatementList();
		while (token.type != TokenType.RBRACE) {
			slist.add(parseStatement());
		}
		return slist;
	}
	
	private VarDeclStmt parseVarDeclStmt(Type t, String s, SourcePosition p) {
		VarDecl var = new VarDecl(t, s, p);
		accept(TokenType.ASSIGN);
		Expression e = parseExpression();
		accept(TokenType.SEMICOL);
		return new VarDeclStmt(var, e, p);
	}
	
	private void subtChangeType() {
		if (token.type == TokenType.SUBT) {
			token = new Token(TokenType.NEG,token.spelling,token.posn.copy());
		}
	}
	
	private Statement parseStatement() {
		SourcePosition posn = token.posn.copy();
		switch(token.type){
			case LBRACE:
				//{Statement*}
				accept(TokenType.LBRACE);
				StatementList slist = parseStatements();
				accept(TokenType.RBRACE);
				return new BlockStmt(slist, posn);
			case INT: case BOOL:
				//Type id = Expression;
				SourcePosition p = token.posn.copy();
				Type t = parseType(token);
				String s = token.spelling;
				accept(TokenType.ID);
				return parseVarDeclStmt(t,s,p);
			case ID:
				Token tok = token.copy();
				acceptIt();
				//if a reference, Type t will go unused
				//Token ID accepted within parseType()
				
				switch(token.type) {
				case ID:
					//Type id = Expression;
					Token tok2 = token.copy();
					t = parseType(tok);
					return parseVarDeclStmt(t,tok2.spelling,posn);
				case DOT:
					//Reference (= Expression | (ArgumentList?));
					Reference r = parseReference(tok);
					switch(token.type) {
					case LPAREN:
						//Reference (ArgumentList?));
						accept(TokenType.LPAREN);
						ExprList elist = parseArguments();
						accept(TokenType.RPAREN);
						accept(TokenType.SEMICOL);
						return new CallStmt(r, elist, posn);
					case ASSIGN:
						//Reference = Expression;
						acceptIt();
						Expression e = parseExpression();
						accept(TokenType.SEMICOL);
						return new AssignStmt(r,e,posn);
					default:
						parseError("Expecting ASSIGN or LPAREN, but found " + token.type + ".");
						return null;
					}
				case LBRACK:
					//ArrayReference = Expression;
					acceptIt();
					/*empty vs. non-empty expression determines construction possibilities
					 *This requires arrayReference construction
					 * 
					 * Empty expression can be arrayReference or Type id = expression;
					 */
					
					//Type id = expression;
					if (token.type == TokenType.RBRACK) {
						acceptIt();
						VarDecl var = new VarDecl(new ArrayType(new ClassType(new Identifier(tok),tok.posn.copy()),tok.posn.copy()),token.spelling,tok.posn.copy());
						accept(TokenType.ID);
						accept(TokenType.ASSIGN);
						Expression e = parseExpression();
						accept(TokenType.SEMICOL);
						return new VarDeclStmt(var, e, posn);
					} else {
						//ArrayReference = expression;
						Expression e1 = parseExpression();
						accept(TokenType.RBRACK);
						IndexedRef idref = new IndexedRef(new IdRef(new Identifier(tok), tok.posn.copy()), e1, posn);
						accept(TokenType.ASSIGN);
						Expression e2 = parseExpression();
						accept(TokenType.SEMICOL);
						return new IxAssignStmt(idref, e2, posn);
					}
				case ASSIGN:
					//Reference = Expression
					r = new IdRef(new Identifier(tok),tok.posn);
					acceptIt();
					Expression e = parseExpression();
					accept(TokenType.SEMICOL);
					return new AssignStmt(r,e,posn);
				case LPAREN:
					//Reference(argumentList?);
					r = new IdRef(new Identifier(tok),tok.posn);
					accept(TokenType.LPAREN);
					ExprList elist = parseArguments();
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOL);
					return new CallStmt(r, elist, posn);
				default:
					parseError("Expecting ID, DOT, LPAREN, or LBRACK but found " + token.type + ".");
				}
				
			//must include valid followers of THIS. THIS is a possible Reference
			case THIS:
				tok = token;
				acceptIt();
				Reference r = parseReference(tok);
				switch(token.type) {
				case LPAREN:
					//Reference (ArgumentList?));
					accept(TokenType.LPAREN);
					ExprList elist = parseArguments();
					accept(TokenType.RPAREN);
					accept(TokenType.SEMICOL);
					return new CallStmt(r, elist, posn);
				case ASSIGN:
					//Reference = Expression;
					acceptIt();
					Expression e = parseExpression();
					accept(TokenType.SEMICOL);
					return new AssignStmt(r,e,posn);
				default:
					parseError("Expecting ASSIGN or LPAREN, but found " + token.type + ".");
					return null;
				}
			case RETURN:
				//return Expression? ;
				acceptIt();
				Expression body = null;
				if (token.type != TokenType.SEMICOL)
					body = parseExpression();
				accept(TokenType.SEMICOL);
				return new ReturnStmt(body, posn);
			case IF:
				//if(Expression) Statement (else Statement)?
				acceptIt();
				accept(TokenType.LPAREN);
				Expression cond = parseExpression();
				accept(TokenType.RPAREN);
				Statement then = parseStatement();
				Statement els = null;
				if (token.type == TokenType.ELSE){
					accept(TokenType.ELSE);
					els = parseStatement();
				}
				return new IfStmt(cond, then, els, posn);
			case WHILE:
				//while(Expression) Statement
				acceptIt();
				accept(TokenType.LPAREN);
				cond = parseExpression();
				accept(TokenType.RPAREN);
				Statement st = parseStatement();
				return new WhileStmt(cond, st, posn);
			default:
				parseError("Expecting LBRACE, INT, BOOL, ID, THIS, RETURN, IF, or WHILE"
				+ ", but found " + token.type + ".");
				return null;
		}
	}
	
	private Expression parseExpression() {
		Token tok = token;
		switch(token.type) {
			case SUBT:
				subtChangeType();
				return parseOpA();
			case NOT:
				return parseOpA();
			case NEW:
				acceptIt();
				Identifier id = new Identifier(token);
				switch(token.type) {
				case ID:
					acceptIt();
					switch(token.type) {
					case LPAREN:
						accept(TokenType.LPAREN);
						accept(TokenType.RPAREN);
						return new NewObjectExpr(new ClassType(id, id.posn), tok.posn.copy());
					case LBRACK:
						accept(TokenType.LBRACK);
						Expression e = parseExpression();
						accept(TokenType.RBRACK);
						return new NewArrayExpr(new ClassType(id, id.posn), e, tok.posn.copy());
					default:
						parseError("Expecting LBRACK or LPAREN, but found " + token.type + ".");
						return null;
					}
				case INT:
					acceptIt();
					accept(TokenType.LBRACK);
					Expression e = parseExpression();
					accept(TokenType.RBRACK);
					return new NewArrayExpr(new BaseType(TypeKind.INT,id.posn),e,tok.posn.copy());
				default:
					parseError("Expecting ID or INT, but found " + token.type + ".");
					return null;
				}
			case INTLIT: case ID: case THIS: case LPAREN: case TRUE: case FALSE: case NULL:
				return parseOpA();
			default:
				parseError("Expecting INTLIT, TRUE, FALSE, LPAREN, NOT, SUBT, or NEW but found " + token.type + ".");
				return null;
		}
		/**
		 * Checks followers of Expression, if a binop, 
		 * then we have used Expression ::= Expression binop Expression
		 */
	}
	
	private boolean isValidExpressionFollower() {
		switch(token.type) {
			//binop
			case EQUAL: case LTE:  case GTE: case LT:  case GT:  
			case NOTEQ: case PLUS: case AND: case OR:  case DIV:  
			case SUBT:  case MULT: 
			//others
			case RPAREN: case RBRACK: case SEMICOL: case COMMA:
				return true;
			default:
				return false;
		}
	}
	
	//A ::= B (|| B)*
	private Expression parseOpA() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpB();
		while (token.type == TokenType.OR) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpB();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//B ::= C (&& C)*
	private Expression parseOpB() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpC();
		while (token.type == TokenType.AND) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpC();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//C ::= D ((== | !=) D)*
	private Expression parseOpC() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpD();
		while (token.type == TokenType.EQUAL || token.type == TokenType.NOTEQ) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpD();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//D ::= E ((< | >)(e | =) E)*
	private Expression parseOpD() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpE();
		while (token.type == TokenType.LTE || token.type == TokenType.GTE ||
				token.type == TokenType.LT || token.type == TokenType.GT) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpE();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//E ::= F ((+ | -) F)*
	private Expression parseOpE() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpF();
		while (token.type == TokenType.PLUS || token.type == TokenType.SUBT) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpF();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//F ::= G ((* | /) G)*
	private Expression parseOpF() {
		SourcePosition posn = token.posn.copy();
		Expression e = parseOpG();
		while (token.type == TokenType.MULT || token.type == TokenType.DIV) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseOpG();
			e = new BinaryExpr(op,e,e2,posn);
		}
		return e;
	}
	
	//G ::=  ((-|!) G)* H
	private Expression parseOpG() {
		SourcePosition posn = token.posn.copy();
		//subtraction operations stopped at parseOpE()
		subtChangeType();
		Expression e = null;
		while (token.type == TokenType.NEG || token.type == TokenType.NOT) {
			Operator op = new Operator(token);
			acceptIt();
			e = parseOpG();
			return new UnaryExpr(op,e,posn);
		}
		e = parseOpH();
		return e;
	}
	
	//H ::= id | num | (expression)
	private Expression parseOpH() {
		Token tok = token;
		Expression e;
		switch (token.type) {
		case INTLIT:
			e = new LiteralExpr(new IntLiteral(token), token.posn.copy());
			acceptIt();
			return e;
		case TRUE: case FALSE:
			acceptIt();
			return new LiteralExpr(new BooleanLiteral(tok), tok.posn.copy());
		case NULL:
			acceptIt();
			return new LiteralExpr(new NullLiteral(tok), tok.posn.copy());
		case ID:
			Identifier id = new Identifier(tok);
			acceptIt();
			if (isValidExpressionFollower()) {
				//this will be considered a reference. No other way to check without accepting
				return new RefExpr(new IdRef(id,tok.posn.copy()),tok.posn.copy());
			}
			switch(token.type) {
			case DOT:
				//Reference (e | (ArgumentList?));
				Reference r = parseReference(tok);
				if (token.type == TokenType.LPAREN) {
					//Reference (ArgumentList?));
					accept(TokenType.LPAREN);
					ExprList elist = parseArguments();
					accept(TokenType.RPAREN);
					return new CallExpr(r, elist, tok.posn.copy());
				}
				else if (isValidExpressionFollower()){
					return new RefExpr(r,tok.posn.copy());
				} else {
					parseError("Expecting LPAREN or follower of EXPRESSION, but found " + token.type + ".");
					return null;
				}
			case LBRACK:
				//ArrayReference
				acceptIt();
				Expression e1 = parseExpression();
				accept(TokenType.RBRACK);
				r = new IndexedRef(new IdRef(id,tok.posn.copy()),e1,tok.posn.copy());
				return new RefExpr(r,tok.posn.copy());
			case LPAREN:
				//Reference(ArgList?)
				acceptIt();
				ExprList elist = parseArguments();
				accept(TokenType.RPAREN);
				r = parseReference(tok);
				return new CallExpr(r, elist, tok.posn.copy());
			default:
				parseError("Expecting ID, DOT, or LBRACK but found " + token.type + ".");
			}
		case THIS:
			acceptIt();
			Reference r = parseReference(tok);
			if (token.type == TokenType.LPAREN) {
				//Reference (ArgumentList?));
				accept(TokenType.LPAREN);
				ExprList elist = parseArguments();
				accept(TokenType.RPAREN);
				return new CallExpr(r, elist, tok.posn.copy());
			} else if (isValidExpressionFollower()){
				return new RefExpr(r,tok.posn.copy());
			} else {
				parseError("Expecting LPAREN or follower of EXPRESSION, but found " + token.type + ".");
				return null;
			}
		case LPAREN:
			acceptIt();
			e = parseExpression();
			accept(TokenType.RPAREN);
			return e;

		default:
			//parseError("Expecting number or left parenthesis but found " + token.type + "."); 
			return null;
		}
	}

	/**
	 * accept current token and advance to next token
	 */
	private void acceptIt() throws SyntaxError {
		accept(token.type);
	}

	/**
	 * verify that current token in input matches expected token and advance to next token
	 * @param expectedToken
	 * @throws SyntaxError  if match fails
	 */
	
	private void accept(TokenType expectedTokenType) throws SyntaxError {
		if (token.type == expectedTokenType) {
			if (trace)
				pTrace();
			token = scanner.scan();
		}
		else
			parseError("expecting '" + expectedTokenType +
					"' but found '" + token.type + "'");
	}

	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}

	// show parse stack whenever terminal is  accepted
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + token.type + " (\"" + token.spelling + "\") " + token.posn.toString());
		System.out.println();
	}

}