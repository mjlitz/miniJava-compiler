package miniJava.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.List;

import miniJava.ContextError;
import miniJava.ErrorReporter;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class ContextualAnalyzer implements Visitor<String,Object> {

	AST ast;
	ErrorReporter reporter;
	ScopedTable s0;
	ScopedTable s1;
	ScopedTable s2;
	ScopedTable s3;
	ClassDecl currentClass;
	MethodDecl currentMethod;
	ScopedTable currentScope;
	static boolean trace = true;
	
	public ContextualAnalyzer(ErrorReporter reporter) {
		this.reporter = reporter;
	}
	
	public void analyzeTree (AST ast) {
		addPredefined();
		ast.visit(this,"");
		if (trace)
			System.out.println("=============================================");
	}
	
	public void addPredefined() {
		SourcePosition p = new SourcePosition(0,0);
		if (trace)
			System.out.println("\n==============Predefined Classes=============");
		s0 = new ScopedTable();
		currentScope = s0;
		
		/****************
		 * _PrintStream *
		 ****************/
		
		FieldDeclList flist2 = new FieldDeclList();
		MethodDeclList mlist2 = new MethodDeclList();
			
		BaseType voidtype = new BaseType(TypeKind.VOID, p);
		FieldDecl f2 = new FieldDecl(false,false,voidtype,"println",p);
		ParameterDeclList pl = new ParameterDeclList();
			BaseType inttype = new BaseType(TypeKind.INT,p);
			ParameterDecl pd = new ParameterDecl(inttype,"n",p);
			pl.add(pd);
		StatementList sl = new StatementList();
		MethodDecl m = new MethodDecl(f2,pl,sl,p);
		mlist2.add(m);
		
		ClassDecl prstr = new ClassDecl("_PrintStream",flist2,mlist2,p);
		try {
			s0.addDecl(prstr);
			prstr.findDecls(this, null);
		} catch (ContextError e) {
			reporter.reportContextError("Error defining predefined class '_PrintStream'");
		}
		
		/**********
		 * System *
		 **********/
		FieldDeclList flist1 = new FieldDeclList();
		MethodDeclList mlist1 = new MethodDeclList();
		Identifier printstream = new Identifier(new Token(TokenType.ID,"_PrintStream",p));
		printstream.decl = prstr;
		ClassType typ1 = new ClassType(printstream,p);
		
		//public static _PrintStream out;
		FieldDecl f = new FieldDecl(false, true, typ1,"out",p);
		flist1.add(f);
		
		ClassDecl c1 = new ClassDecl("System",flist1,mlist1,p);
		try {
			s0.addDecl(c1);
			c1.findDecls(this, null);
		} catch (ContextError e1) {
			reporter.reportContextError("Error defining predefined class 'System'");
		}
		
		/**********
		 * String *
		 **********/
		
		FieldDeclList flist3 = new FieldDeclList();
		MethodDeclList mlist3 = new MethodDeclList();
		
		ClassDecl str = new ClassDecl("String",flist3,mlist3,p);
		try {
			s0.addDecl(str);
			str.findDecls(this, null);
		} catch (ContextError e) {
			reporter.reportContextError("Error defining predefined class 'String'");
		}
	}
	
	public Type compareBinopTypes (Type t1, Type t2, Operator op) {
		TypeKind k1 = t1.typeKind;
		TypeKind k2 = t2.typeKind;
		if (t1 instanceof ArrayType)
			k1 = ((ArrayType) t1).eltType.typeKind;
		if (t2 instanceof ArrayType)
			k2 = ((ArrayType) t2).eltType.typeKind;
		if (k1 == TypeKind.ERROR)
			return t2;
		if (k2 == TypeKind.ERROR)
			return t1;
		Type e = new BaseType(TypeKind.ERROR,t1.posn);
		Type bool = new BaseType(TypeKind.BOOLEAN,t1.posn);
		switch (op.type) {
		case PLUS: case SUBT: case MULT: case DIV:
			//INT x INT -> INT
			if (k1 == TypeKind.INT && k2 == TypeKind.INT)
				return t1;
			reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type(s) "+k1+", "+k2);
			return e;
		 case GTE: case LTE: case GT: case LT:
			//INT x INT -> BOOL 
			if (k1 == TypeKind.INT && k2 == TypeKind.INT)
				return bool;
			reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type(s) "+k1+", "+k2);
			return e;
		case AND: case OR:
			//BOOL x BOOL -> BOOL
			if (k1 == TypeKind.BOOLEAN && k2 == TypeKind.BOOLEAN)
				return t1;
			reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type(s) "+k1+", "+k2);
			return e;
		 case EQUAL:case NOTEQ:
			 //BOOL x BOOL -> BOOL
			 if (k1 == TypeKind.BOOLEAN && k2 == TypeKind.BOOLEAN)
				 return bool;
			 //INT x INT -> BOOL
			 else if (k1 == TypeKind.INT && k2 == TypeKind.INT)
				 return bool;
			 //CLASS x NULL -> BOOL
			 else if (k1 == TypeKind.CLASS && k2 == TypeKind.NULL)
				 return bool;
			 //NULL x CLASS -> BOOL
			 else if (k2 == TypeKind.CLASS && k1 == TypeKind.NULL)
				 return bool;
		default:
			reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type(s) "+k1+", "+k2);
			return e;
		}
	}
	
	public Type compareUnopTypes (Type t1, Operator op) {
		TypeKind k1 = t1.typeKind;
		if (t1 instanceof ArrayType)
			k1 = ((ArrayType) t1).eltType.typeKind;
		Type e = new BaseType(TypeKind.ERROR,t1.posn);
		switch (op.type) {
		case NEG:
			//INT -> INT
			if (k1 == TypeKind.INT)
				return t1;
			else {
				reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type "+k1);
				return e;
			}
		case NOT:
			//BOOL -> BOOL
			if (k1 == TypeKind.BOOLEAN)
				return t1;
			else {
				reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type "+k1);
				return e;
			}
		default:
			reporter.reportTypeError("The operator "+op.spelling+" is undefined for the argument type "+k1);
			return e;			
		}
	}
	
	public Object compareClassTypes (Type t1, Type t2) throws ContextError {
		ClassType ct1 = (ClassType) t1;
		ClassType ct2 = (ClassType) t2;
		String s1 = ct1.className.spelling;
		String s2 = ct2.className.spelling;
		if (s1.equals(s2))
				return t1;
		else {
			throw new ContextError("The classtypes '"+s1+"' and '"+s2+"' cannot be correctly type resolved");
		}
	}
	
	public Object compareAssignTypes (Type decl, Type exp) {
		//if they're the same return that type
		//if different, return the error type
		TypeKind k1 = decl.typeKind;
		TypeKind k2 = exp.typeKind;
		TypeKind e = TypeKind.ERROR;
		//ClassType
		if (k1 == TypeKind.CLASS && k2 == TypeKind.CLASS)
			try {
				return (Type) compareClassTypes((ClassType)decl,(ClassType)exp);
			} catch (ContextError e1) {
				reporter.reportTypeError("The classtypes '"+s1+"' and '"+s2+"' cannot be correctly type resolved");
				return new BaseType(TypeKind.ERROR,decl.posn);
			}
		if (decl instanceof ArrayType)
			k1 = ((ArrayType) decl).eltType.typeKind;
		if (exp instanceof ArrayType)
			k2 = ((ArrayType) exp).eltType.typeKind;
		//Error Cases
		if (k1 == TypeKind.UNSUPPORTED) {
			reporter.reportTypeError("Type String at "+exp.posn+" is unsupported");
			return decl;
		} else if (k2 == TypeKind.UNSUPPORTED) {
			reporter.reportTypeError("Type String at "+decl.posn+" is unsupported");
			return exp;
		} else if (k1 == TypeKind.ERROR)
			return exp;
		else if (k2 == TypeKind.ERROR)
			return decl;
		
		
		//BaseType
		switch (k2) {
		case INT:
			if (k1 == TypeKind.INT) 
				return decl;
			else {
				reporter.reportTypeError("The operator ASSIGN is undefined for the argument types "+k1+", "+k2);
				return e;
			}
		case BOOLEAN:
			if (k1 == TypeKind.BOOLEAN) 
				return decl;
			else {
				reporter.reportTypeError("The operator ASSIGN is undefined for the argument types "+k1+", "+k2);
				return e;
			}
		default:
			return e;
		}
		//ClassType
	}

	public Object visitPackage(Package prog, String arg) {
		s1 = new ScopedTable(s0);
		if (trace)
			System.out.println("\n=========Class and Member Declarations=======");
		for (ClassDecl c: prog.classDeclList){
			try {
				currentScope = s1;
				s1.addDecl(c);
				c.type = new ClassType(new Identifier(new Token(TokenType.ID,c.name,c.posn)),c.posn);
			} catch (ContextError e) {
				reporter.reportIDError("Duplicate Declaration at " +c.posn);
			}
			//this finds all class and member decls before going into methods.
            c.findDecls(this, null);
         }
		if (trace)
			System.out.println("\n==============Local Declarations==============");
		for (ClassDecl c: prog.classDeclList)
            c.visit(this, null);
        return null;
	}

	public Object findMemberDecls (ClassDecl cd, String arg) {
		s2 = new ScopedTable(currentScope);
		cd.next = s2;
		currentClass = cd;
		//we're just finding decls, so no visiting methods
		 for (FieldDecl f: cd.fieldDeclList) {
	        	try {s2.addDecl(f);}
	        	catch (ContextError e) {reporter.reportIDError("Duplicate of Declaration "+e.d+" at " + f.posn);}
	        }
	        for (MethodDecl m: cd.methodDeclList) {
	        	try {s2.addDecl(m);}
	        	catch (ContextError e) {reporter.reportIDError("Duplicate Declaration "+e.d+" at " + m.posn);}
	        }
	        return null;
	}
	
	public Object visitClassDecl(ClassDecl cd, String arg) {
		s2 = cd.next;
		currentClass = cd;
        for (FieldDecl f: cd.fieldDeclList)
        	f.visit(this, null);
        for (MethodDecl m: cd.methodDeclList) 
        	m.visit(this, null);
        return null;
	}

	public Object visitFieldDecl(FieldDecl fd, String arg) {
		currentScope = s2;
		fd.type.visit(this, null);
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, String arg) {
		s3 = new ScopedTable(s2);
		currentScope = s3;
		currentMethod = md;
    	Type expectedType = (Type) md.type.visit(this, null);
        ParameterDeclList pdl = md.parameterDeclList;
		for (ParameterDecl pd: pdl) {
			try {s3.addDecl(pd);}
			catch (ContextError e) {reporter.reportIDError("Duplicate Declaration "+e.d+" at " + pd.posn);}
            pd.visit(this, null);
        }
		//boolean staticAccess = md.isStatic;
        StatementList sl = md.statementList;
        Type returnType = null;
        int i = 0;
        for (Statement s: sl) {
        	i++;
        	if (trace)
        		System.out.println("------Statement "+i+"------");
        	ScopedTable temp = currentScope;
        	returnType = (Type) s.visit(this, null);
        	currentScope = temp;
        }
        if (expectedType.typeKind != TypeKind.VOID && returnType == null) 
        	reporter.reportTypeError("Expecting a return statement of type "+expectedType.typeKind);
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, String arg) {
		return pd.type.visit(this, null);
	}

	public Object visitVarDecl(VarDecl decl, String arg) {
		try {currentScope.addDecl(decl);}
		catch (ContextError e) {reporter.reportIDError("Duplicate local variable \""+e.d.name+"\" at " + decl.posn);}
		return decl.type.visit(this, null);
	}

	public Object visitBaseType(BaseType type, String arg) {
		return type;
	}

	public Object visitClassType(ClassType type, String arg) {
		if (type.className.spelling.equals("String"))
			return new BaseType(TypeKind.UNSUPPORTED,type.posn);
		Identifier className = type.className;
		if (s1.searchID(className, currentScope.scope+"") != null)
			return type;
		else {
			reporter.reportIDError("ClassType "+className+" not found.");
			return null;
		}
	}

	@Override
	public Object visitArrayType(ArrayType type, String arg) {
		return type;
	}

	public Object visitBlockStmt(BlockStmt stmt, String arg) {
		//open scope
        currentScope = new ScopedTable(currentScope);
		StatementList sl = stmt.sl;
        for (Statement s: sl) {
        	if (s instanceof ReturnStmt)
        		return s.visit(this, null);
        	s.visit(this, null);
        }
        currentScope = currentScope.previous;
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, String arg) {
		Type exp = (Type) stmt.initExp.visit(this, null);
		Type decl = (Type) stmt.varDecl.visit(this, null);	
		compareAssignTypes(decl,exp);
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, String arg) {
		Type val = (Type) stmt.val.visit(this, null);
		stmt.ref.visit(this, null);

		QualifiedRef qf = null;
		if (stmt.ref instanceof QualifiedRef) {
			qf = (QualifiedRef) stmt.ref;
			while (qf.ref instanceof QualifiedRef)
				qf = (QualifiedRef) qf.ref;
			compareAssignTypes(qf.ref.type,val);
		}
		else {
			Type ref = (Type) stmt.ref.type.visit(this, null);
			compareAssignTypes(ref,val);
		}
		
		return null;
	}

	public Object visitIxAssignStmt(IxAssignStmt stmt, String arg) {
		Type ref = (Type) stmt.ixRef.visit(this, null);
		Type val = (Type) stmt.val.visit(this, null);
		compareAssignTypes(ref,val);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, String arg) {
		//System.out.println("Visited call stmt");
		stmt.methodRef.visit(this, null);
        ExprList al = stmt.argList;
        for (Expression e: al) {
            e.visit(this, null);
        }
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, String arg) {
        //expect matching type in methodDecl
		Type returnType = null;
		if (stmt.returnExpr != null) {
			returnType = (Type) stmt.returnExpr.visit(this, null);
			Type expectedType = currentMethod.type;
			TypeKind k1 = returnType.typeKind;
			TypeKind k2 = expectedType.typeKind;
			String s1 = ""; String s2 = "";
			if (k2 == TypeKind.ARRAY && k2 == TypeKind.ARRAY) {
				ArrayType temp1 = (ArrayType) returnType;
				Type eltType1 = temp1.eltType;
				k1 = eltType1.typeKind;
				temp1 = (ArrayType) expectedType;
				Type eltType2 = temp1.eltType;
				k2 = eltType2.typeKind;
				if (k1 == TypeKind.CLASS && k2 == TypeKind.CLASS) {
					ClassType ct1 = (ClassType) eltType1;
					ClassType ct2 = (ClassType) eltType2;
					s1 = ct1.className.spelling;
					s2 = ct2.className.spelling;
					if (!(s1.equals(s2)))
						reporter.reportTypeError("Expecting return value of type "+s2+"[] but found "+s1+"[]");
				}
				if (k1 != k2)
					reporter.reportTypeError("Expecting return value of type "+k2+"[] but found "+k1+"[]");
			} else if (k1 == TypeKind.CLASS && k2 == TypeKind.CLASS) {
				//Compare spellings of return and expected types
				try {
					compareClassTypes(returnType,expectedType);
				} catch (ContextError e) {
				}
			} else { 
				if (returnType.typeKind != expectedType.typeKind)
					reporter.reportTypeError("Expecting return value of type "+expectedType.typeKind+" but found "+returnType.typeKind);
			}
		}
        //returnExpr == null, expecting VOID
        else if (currentMethod.type.typeKind != TypeKind.VOID)
        	reporter.reportTypeError("Expecting return value of type "+currentMethod.type.typeKind+", received a VOID");       		
		return returnType;
	}

	public Object visitIfStmt(IfStmt stmt, String arg) {
		Type t = (Type) stmt.cond.visit(this, null);
		//if a return type happens in an if statement, this argument passes along to MethodDecl
        if (t.typeKind != TypeKind.BOOLEAN) {
        	reporter.reportTypeError("Expected BOOLEAN expression for condition, received "+t.typeKind);
        	return null;
        }
        if (stmt.thenStmt instanceof VarDeclStmt)
        	reporter.reportIDError("VarDecls in IF statements not allowed");
        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt instanceof VarDeclStmt)
        	reporter.reportIDError("VarDecls in ELSE statements not allowed");
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, null);
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, String arg) {
		Type t = (Type) stmt.cond.visit(this, null);
        if (t.typeKind != TypeKind.BOOLEAN) {
        	reporter.reportTypeError("Expected BOOLEAN expression for condition, received "+t.typeKind);
        	return null;
        }
        if (stmt.body instanceof VarDeclStmt)
        	reporter.reportIDError("VarDecls in WHILE loops not allowed");
        stmt.body.visit(this, null);
        return null;
	}
	
	/***************
	 * EXPRESSIONS *
	 ***************/

	public Object visitUnaryExpr(UnaryExpr expr, String arg) {
		Operator op = (Operator) expr.operator.visit(this, null);
		Type t1 = (Type) expr.expr.visit(this, null);
		return compareUnopTypes(t1,op);
	}

	public Object visitBinaryExpr(BinaryExpr expr, String arg) {
		Operator op = (Operator) expr.operator.visit(this, null);
        Type t1 = (Type) expr.left.visit(this, null);
        Type t2 = (Type) expr.right.visit(this, null);
		return compareBinopTypes(t1,t2,op);
	}

	public Object visitRefExpr(RefExpr expr, String arg) {
		//check that type returned wasn't a method
		Type result = (Type) expr.ref.visit(this, null);
		return result;
	}

	public Object visitCallExpr(CallExpr expr, String arg) {
		//type check parameters
        Type returnType = (Type) expr.functionRef.visit(this, null);
        QualifiedRef qref = null;
        Reference ref = expr.functionRef;
        while (ref instanceof QualifiedRef) {
        	qref = (QualifiedRef) ref;
        	ref = qref.ref;
        }
        IdRef idref = (IdRef) ref;
        if (!(idref.id.decl instanceof MethodDecl))
        	reporter.reportError("*** Identification Error: Method "+idref.id.spelling+"() could not be resolved");
        MethodDecl md = (MethodDecl) idref.id.decl;
        ParameterDeclList pdl = md.parameterDeclList;
        List<Type> typeList = new ArrayList<Type>();
		for (ParameterDecl pd: pdl) {
			Type t = (Type) pd.visit(this, null);
            typeList.add(t);
        }
        returnType.visit(this, null);
        ExprList al = expr.argList;
        int i = 0;
        for (Expression e: al) {
            Type t1 = (Type) e.visit(this, null);
            Type t2 = typeList.get(i);
            TypeKind k1 = t1.typeKind;
            TypeKind k2 = t2.typeKind;
            if (k1 != k2)
            	reporter.reportTypeError("Expecting arg["+i+"] in "+currentClass.name+"."+currentMethod.name+"() call to be "+k1+" but found "+k2);
            else if (k1 == TypeKind.CLASS && k2 == TypeKind.CLASS){
            	ClassType c1 = (ClassType) t1;
            	ClassType c2 = (ClassType) t2;
            	String s1 = c1.className.spelling;
            	String s2 = c2.className.spelling;
            	if (!(s1.equals(s2)))
            		reporter.reportTypeError("Expecting arg["+i+"] in "+currentClass.name+"."+currentMethod.name+"() call to be \""+s1+"\" but found \""+s2+"\"");
            }
            i++;
        }
		//return return type of method
		return returnType;
	}

	public Object visitLiteralExpr(LiteralExpr expr, String arg) {
		return expr.lit.visit(this, null);
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {
		expr.classtype.className.visit(this, null);
		return expr.classtype;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
		return new ArrayType(expr.eltType,expr.posn);
	}
	
	/**************
	 * REFERENCES *
	 **************/

	public Object visitQualifiedRef(QualifiedRef ref, String arg) {
		ScopedTable temp = currentScope;
		boolean staticAccess = false;
		boolean isStatic = false;
		Type t = null;
		Declaration decl = null;
		if (arg == null)
			arg = "";
		if (ref.ref instanceof ThisRef) {
			ref.ref.visit(this,null);
			currentScope = currentClass.next;
			t = (Type) ref.id.visit(this, null);
		} else {
			ref.id.visit(this, null);
			decl = ref.id.decl;
			ref.decl = decl;
			//static reference
			if (decl instanceof ClassDecl) {
				currentScope = decl.next;
				staticAccess = true;
				decl.type = new ClassType(ref.id,ref.posn);
				t = (Type) ref.ref.visit(this,arg+ref.id.spelling+".");
			} 
			//non-static reference
			else { 
				if (decl.type instanceof ClassType) {
					ClassType type = (ClassType) decl.type;
					staticAccess = false;
					if (type.className.decl == null) {
						type.visit(this, null);
					}
					currentScope = type.className.decl.next;
					t = (Type) ref.ref.visit(this,arg+ref.id.spelling+".");
				} else {
					if (ref.ref instanceof IdRef) {
						IdRef ir = (IdRef) ref.ref;
						if (ref.decl.type instanceof ArrayType && ir.id.spelling.equals("length"))
							return new BaseType(TypeKind.INT,ref.decl.type.posn);
						else {
							reporter.reportIDError("Declaration \""+decl.name+"\" of type "+decl.type.typeKind+" cannot be dereferenced.");
							System.exit(4);
						}
					} else {
						reporter.reportIDError("Declaration \""+decl.name+"\" of type "+decl.type.typeKind+" cannot be dereferenced.");
						System.exit(4);
					}
				}
			}
		}
    	MemberDecl md = null;
    	if (ref.ref instanceof QualifiedRef) {
    		QualifiedRef id = (QualifiedRef) ref.ref;
    		Type t1 = ref.id.decl.type;
    		if (t1 instanceof ClassType) {
    			ClassType c = (ClassType) t1;
        		md = (MemberDecl) id.id.decl;
    			//calling method or field outside class
    			if (!c.className.decl.equals(currentClass) && md.isPrivate)
    				reporter.reportIDError("Cannot access private member "+md.name+".");
    		}
    		else {
    			System.out.println("t1 not of type ClassType");
    		}
    		isStatic = md.isStatic;
    		if (isStatic && !staticAccess) 
    			reporter.reportContextError("Cannot make a non-static reference to the static field \""+decl.name+"."+arg+id.id.spelling+"\"");
    		if (!isStatic && staticAccess)
    			reporter.reportContextError("Cannot make a static reference to the non-static field \""+decl.name+"."+arg+id.id.spelling+"\"");
    	} else if (ref.ref instanceof IdRef) {
    		IdRef id = (IdRef) ref.ref;
    		Type t1 = ref.id.decl.type;
    		t1.visit(this,null);
    		if (t1 instanceof ClassType) {
    			ClassType c = (ClassType) t1;
        		md = (MemberDecl) id.id.decl;
    			//calling method or field outside class
    			if (!c.className.decl.equals(currentClass) && md.isPrivate)
    				reporter.reportIDError("Cannot access private member "+md.name+".");
    		}
    		else {
    			System.out.println("t1 not of type ClassType");
    		}
    		isStatic = md.isStatic;
    		if (isStatic && !staticAccess) 
    			reporter.reportContextError("Cannot make a non-static reference to the static field \""+decl.name+"."+arg+id.id.spelling+"\"");
    		if (!isStatic && staticAccess)
    			reporter.reportContextError("Cannot make a static reference to the non-static field \""+decl.name+"."+arg+id.id.spelling+"\"");
    	} else if (ref.ref instanceof ThisRef) {
    		currentScope = currentClass.next;
    		md = (MemberDecl) ref.id.decl;
    		isStatic = md.isStatic;
    		if (isStatic) 
    			reporter.reportContextError("Cannot make a non-static reference to the static field \""+"this."+arg+ref.id.spelling+"\"");
    	}
    	//return scope to previous local scope
		currentScope = temp;
		return t;
	}

	public Object visitIndexedRef(IndexedRef ref, String arg) {
		Type t1 = (Type) ref.indexExpr.visit(this, null);
		Type t2 = null;
		if (t1.typeKind != TypeKind.INT) {
			reporter.reportTypeError(t1.typeKind+" is an undefined argument for IndexedRef ");
			return new BaseType(TypeKind.ERROR,t1.posn);
		} else {
			t2 = (Type) ref.idRef.visit(this, null);
		}
		return t2;
	}

	public Object visitIdRef(IdRef ref, String arg) {
		ref.type = (Type) ref.id.visit(this, null);
		ref.decl = ref.id.decl;
		if (ref.id.decl instanceof MemberDecl) {
			MemberDecl d = (MemberDecl) ref.id.decl;
			boolean isStatic = d.isStatic;
			boolean staticAccess = false;;
			if (!currentMethod.name.equals("main"))
				staticAccess = currentMethod.isStatic;
			if (arg == null)
				arg = "";
			if (isStatic && !staticAccess) 
				reporter.reportContextError("Cannot make a non-static reference to the static member \""+arg+ref.id.spelling+"\"");
			if (!isStatic && staticAccess)
				reporter.reportContextError("Cannot make a static reference to the non-static member \""+arg+ref.id.spelling+"\"");
		}
    	
		return ref.type;
	}

	public Object visitThisRef(ThisRef ref, String arg) {
		//Changes scope to match level of appropriate class
		Identifier id = new Identifier(new Token(TokenType.ID,currentClass.name,ref.posn));
		visitIdentifier(id,"this");
		ref.id = id;
		ref.decl = id.decl;
		return new ClassType(id,ref.posn);
	}

	public Object visitIdentifier(Identifier id, String arg) {
		Declaration decl = currentScope.searchID(id,arg);
		if (decl instanceof ClassDecl && arg != null && !arg.equals("this"))
			reporter.reportIDError("Cannot use class name \""+decl.name+"\" as an id.");;
		if (decl == null) {
			reporter.reportIDError("ID \"" + id.spelling + "\" not found.");
			//Exit, so we don't cause any null pointer exceptions
			System.exit(4);
			//unreachable
			return new BaseType(TypeKind.ERROR,id.posn);
		} else
			return id.decl.type;
	}

	public Object visitOperator(Operator op, String arg) {
		return op;
	}

	public Object visitIntLiteral(IntLiteral num, String arg) {
		return new BaseType(TypeKind.INT, num.posn);
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, String arg) {
		return new BaseType(TypeKind.BOOLEAN,bool.posn);
	}

	public Object visitNullLiteral(NullLiteral n, String arg) {
		return new BaseType(TypeKind.NULL,n.posn);
	}
}