package miniJava.CodeGeneration;

import java.util.HashMap;

import mJAM.Disassembler;
import mJAM.Machine;
import mJAM.ObjectFile;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class CodeGenerator implements Visitor<String,Object>{

	ErrorReporter reporter;
	String file;
	int offset = 3;
	int codeAddr_main;
	MethodDecl main;
	boolean sys_out_println;
	int i = 0;
	
	//if call happens before method, we will record the location of call to call_offsets, then patch in postamble
	final HashMap<MethodDecl,Integer> call_offsets = new HashMap<MethodDecl,Integer>();
	
	public CodeGenerator(ErrorReporter reporter, String file) {
		this.reporter = reporter;
		file = file.substring(0,file.length()-5);//take off .java
		this.file = file;
	}
	
	public void generateCode(AST ast) {
		//Preamble
		Machine.initCodeGen();
		//generate call to main
		Machine.emit(Op.LOADL,0);            // array length 0
		Machine.emit(Prim.newarr);           // empty String array argument
		int patchAddr_Call_main = Machine.nextInstrAddr();  // record instr addr where
		                                                    // "main" is called
		Machine.emit(Op.CALL,Reg.CB,-1);     // static call main (address to be patched)
		Machine.emit(Op.HALT,0,0,0);         // end execution
		
		/*codeAddr_main = Machine.nextInstrAddr();
		Machine.emit(Op.LOADL,3);
		Machine.emit(Op.STORE,Reg.LB,4);*/
		
		ast.visit(this,null);
				
		/*
		 *  Postamble
		 *    patch jumps and calls to unknown code addresses
		 *  
		 */
			Machine.patch(patchAddr_Call_main, codeAddr_main);
			                                     // supply correct address of "main" to
			                                     // generated call in preamble
			
		
		/*
		 * save object code and generate corresponding assembler instructions
		 */

			/* write code to object code file */
			String objectCodeFileName = file+".mJAM";
			ObjectFile objF = new ObjectFile(objectCodeFileName);
			System.out.print("Writing object code file " + objectCodeFileName + " ... ");
			if (objF.write()) {
				System.out.println("FAILED!");
				return;
			}
			else
				System.out.println("SUCCEEDED");	
			
			/* create asm file using disassembler */
			String asmCodeFileName = file+".asm";
			System.out.print("Writing assembly file ... ");
			Disassembler d = new Disassembler(objectCodeFileName);
			if (d.disassemble()) {
				System.out.println("FAILED!");
				return;
			}
			else
				System.out.println("SUCCEEDED");
			
		/* 
		 * run code using debugger
		 * 
		 */
			/*System.out.println("Running code ... ");
			Interpreter.debug(objectCodeFileName, asmCodeFileName);

			System.out.println("*** mJAM execution completed");*/
	}
	
	public Object visitPackage(Package prog, String arg) {
		for (ClassDecl c: prog.classDeclList)
			c.visit(this, null);
		if (main == null) {
			reporter.reportGenerationError("No public static void main method found");
			System.exit(4);
		}
		main.visit(this, null);
		return null;
	}

	@SuppressWarnings("unused")
	public Object visitClassDecl(ClassDecl cd, String arg) {
		int nonstaticfields = 0;
		int staticfields = 0;
		int fieldnum = 1;
		for (FieldDecl f: cd.fieldDeclList) {
        	f.visit(this, fieldnum+"");
        	fieldnum++;
        	if (f.isStatic)
        		staticfields++;
        	else
        		nonstaticfields++;
		}
        for (MethodDecl m: cd.methodDeclList) 
        	findMain(m, null);
        cd.entity = new UnknownAddress(nonstaticfields,0);
        return null;
	}

	public Object findMemberDecls(ClassDecl cd, String arg) {
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, String arg) {
		//if static store and initialize in global segment
		if (fd.isStatic)
			;
		//non-static
		else
			;
		fd.entity = new UnknownAddress(1,Integer.parseInt(arg));
		return null;
	}
	
	public Object findMain(MethodDecl md, String arg) {
		ParameterDeclList pdl = md.parameterDeclList;
		ParameterDecl args = null;
		for (ParameterDecl pd: pdl) {
			pd.visit(this,null);
			args = pd;
		}
		if (args != null && args.type instanceof ArrayType) {
			ArrayType t = (ArrayType) args.type;
			if (t.eltType instanceof ClassType) {
				ClassType ct = (ClassType) t.eltType;
				if (md.isStatic && !md.isPrivate && md.type.typeKind == TypeKind.VOID && md.name.equals("main") 
					&& pdl.size() == 1 && ct.className.spelling.equals("String")) {
					//main method found, double check if already been found
					if (main != null) {
						reporter.reportGenerationError("Duplicate main method, main method already found at "+codeAddr_main);
						System.exit(4);
					}
					else {
						main = md;
						codeAddr_main = Machine.nextInstrAddr();
					}
				}
			}
		}
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, String arg) {
		int beginAddr = Machine.nextInstrAddr();
		for (Statement s: md.statementList) {
			i++;
        	s.visit(this, null);
		}
		int endAddr = Machine.nextInstrAddr();
		md.entity = new KnownAddress(endAddr-beginAddr,beginAddr);
		/*int callAddr = call_offsets.get(md);
		if (callAddr != 0)
			Machine.patch(callAddr, beginAddr);*/
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, String arg) {
		return null;
	}

	public Object visitVarDecl(VarDecl decl, String arg) {
		decl.entity = new KnownAddress(1,offset);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, String arg) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object visitClassType(ClassType type, String arg) {
		//newobjarr
		/*if (!type.className.decl.name.equals("_PrintStream")) {
			int s = type.className.decl.entity.size;
			Machine.emit(Op.LOADL,-1);
			Machine.emit(Op.LOADL,s);
			Machine.emit(Prim.newobj);
			System.out.println("Visiting Class Type in statement "+i);
		}*/
			
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, String arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object visitBlockStmt(BlockStmt stmt, String arg) {
		for (Statement s : stmt.sl)
			s.visit(this, null);
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, String arg) {
		Machine.emit(Op.PUSH,1);
		stmt.initExp.visit(this, null);
		stmt.varDecl.visit(this, null);
		Machine.emit(Op.STORE,Reg.LB,offset);
		offset++;
		
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, String arg) {
		if (stmt.ref.decl instanceof VarDecl) {
			stmt.ref.visit(this, null);
			stmt.val.visit(this,null);
			QualifiedRef qf = null;
			if (stmt.ref instanceof QualifiedRef) {
				/*qf = (QualifiedRef) stmt.ref;
				while (qf.ref instanceof QualifiedRef)
					qf = (QualifiedRef) qf.ref;
				UnknownAddress ua = (UnknownAddress) qf.ref.decl.entity;*/
				Machine.emit(Prim.fieldupd);
				//Machine.emit(Prim.fieldupd);
			}
			else {
				KnownAddress ka = (KnownAddress) stmt.ref.decl.entity;
				Machine.emit(Op.STORE,Reg.LB,ka.address);
			}
		}
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, String arg) {
		// TODO Auto-generated method stub
		return null;
	}

	//if we are calling a method, look if it is in methods_offset.
	//if so, patch to it.
	//if not, record current Addr to call_offsets 
	public Object visitCallStmt(CallStmt stmt, String arg) {
		stmt.methodRef.visit(this, null);
		if (sys_out_println) {
			for (Expression e : stmt.argList) {
				e.visit(this, null);
			}
			Machine.emit(Prim.putintnl);
		}
		//MethodDecl md = (MethodDecl) stmt.methodRef.decl;
			
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, String arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, String arg) {
		stmt.cond.visit(this, null);
		int jumpElseAddr = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,jumpElseAddr);
		stmt.thenStmt.visit(this, null);
		int jumpEndAddr = Machine.nextInstrAddr();
		if (stmt.elseStmt != null) {
			Machine.emit(Op.JUMP,Reg.CB,jumpEndAddr);
			Machine.patch(jumpElseAddr, Machine.nextInstrAddr());
			stmt.elseStmt.visit(this, null);
		} else
			Machine.patch(jumpElseAddr, Machine.nextInstrAddr());
		Machine.patch(jumpEndAddr, Machine.nextInstrAddr());
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, String arg) {
		stmt.cond.visit(this, null);
		int endAddr = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF,0,Reg.CB,endAddr);
		int bodyAddr = Machine.nextInstrAddr();
		stmt.body.visit(this, null);
		stmt.cond.visit(this, null);
		Machine.emit(Op.JUMPIF,1,Reg.CB,bodyAddr);
		Machine.patch(endAddr,Machine.nextInstrAddr());
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr expr, String arg) {
		expr.expr.visit(this, null);
		expr.operator.visit(this, null);
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, String arg) {
		expr.left.visit(this,null);
		expr.right.visit(this,null);
		expr.operator.visit(this,null);
		return null;
	}

	public Object visitRefExpr(RefExpr expr, String arg) {
		expr.ref.visit(this,null);
		if (expr.ref instanceof QualifiedRef) {
			QualifiedRef qf = (QualifiedRef) expr.ref;
			if (qf.ref instanceof IdRef) {
				IdRef ir = (IdRef) qf.ref;
				if (expr.ref.decl.type instanceof ArrayType && ir.id.spelling.equals("length"))
				;
				else
					Machine.emit(Prim.fieldref);
			} else
				Machine.emit(Prim.fieldref);
		}
		/*else if (expr.ref.decl instanceof VarDecl && !(expr.ref.decl.type instanceof ClassType)) {
			KnownAddress ka = (KnownAddress) expr.ref.decl.entity;
			Machine.emit(Op.LOAD,Reg.LB,ka.address);
		}*/
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, String arg) {
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, String arg) {
		expr.lit.visit(this, null);
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {
		Machine.emit(Op.LOADL,-1);
		ClassDecl cd = (ClassDecl) expr.classtype.className.decl;
		int s = cd.entity.size;
		Machine.emit(Op.LOADL,s);
		Machine.emit(Prim.newobj);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
		expr.sizeExpr.visit(this,null);//emits size from intliteral
		Machine.emit(Prim.newarr);
		return null;
	}

	public Object visitQualifiedRef(QualifiedRef ref, String arg) {
		UnknownAddress field = null;
		KnownAddress known_addr = null;
		UnknownAddress unknown_addr = null;
		//print statement
		if (ref.id.spelling.equals("System") && ref.ref instanceof QualifiedRef) {
			QualifiedRef qf = (QualifiedRef) ref.ref;
			if (qf.id.spelling.equals("out") && qf.ref instanceof IdRef) {
				IdRef ir = (IdRef) qf.ref;
				if (ir.id.spelling.equals("println")) {
					sys_out_println = true;
					return null;
				}
			}
		}
		//non-static
		if (ref.decl instanceof VarDecl) {
			if (ref.ref instanceof IdRef) {
				IdRef ir = (IdRef) ref.ref;
				if (ref.decl.type instanceof ArrayType && ir.id.spelling.equals("length")) {
					RuntimeEntity qfe = ref.decl.entity;
					if (qfe instanceof KnownAddress) {
						known_addr = (KnownAddress) ref.decl.entity;
						Machine.emit(Op.LOAD,Reg.LB,known_addr.address);
					}
					Machine.emit(Prim.arraylen);
					return null;
				}
			}
			//first time calling qualifiedRef
			if (arg == null) {
				arg = ref.decl.name+".";
				QualifiedRef qf = ref;
				RuntimeEntity qfe = qf.decl.entity;
				field = (UnknownAddress) qf.ref.decl.entity;
				if (qfe instanceof KnownAddress) {
					known_addr = (KnownAddress) qf.decl.entity;
					Machine.emit(Op.LOAD,Reg.LB,known_addr.address);
				}
				else {
					unknown_addr = (UnknownAddress) qf.decl.entity;
					Machine.emit(Op.LOAD,Reg.LB,unknown_addr.address);
				}
			} else {
				Machine.emit(Prim.fieldref);
			}
			Machine.emit(Op.LOADL,field.address-1);
			ref.ref.visit(this, arg);
		} else if (ref.decl instanceof FieldDecl) {
			ref.decl.type.visit(this, null);
		}
		//static
		else
			;

		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, String arg) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object visitIdRef(IdRef ref, String arg) {
		KnownAddress known_addr = null;
		RuntimeEntity qfe = ref.decl.entity;
		if (qfe instanceof KnownAddress) {
			known_addr = (KnownAddress) ref.decl.entity;
			Machine.emit(Op.LOAD,Reg.LB,known_addr.address);
		}
		/*else {
			unknown_addr = (UnknownAddress) qf.decl.entity;
			Machine.emit(Op.LOAD,Reg.LB,unknown_addr.address);
		}*/
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, String arg) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object visitIdentifier(Identifier id, String arg) {
		RuntimeEntity e = id.decl.entity;
		return e;
	}

	public Object visitOperator(Operator op, String arg) {
		switch (op.type) {
		//binop
		case PLUS:
			Machine.emit(Prim.add);
			break;
		case MULT:
			Machine.emit(Prim.mult);
			break;
		case SUBT:
			Machine.emit(Prim.sub);
			break;
		case DIV:
			Machine.emit(Prim.div);
			break;
		case AND:
			Machine.emit(Prim.and);
			break;
		case OR:
			Machine.emit(Prim.or);
			break;
		case GT:
			Machine.emit(Prim.gt);
			break;
		case LT:
			Machine.emit(Prim.lt);
			break;
		case GTE:
			Machine.emit(Prim.ge);
			break;
		case LTE:
			Machine.emit(Prim.le);
			break;
		case EQUAL:
			Machine.emit(Prim.eq);
			break;
		case NOTEQ:
			Machine.emit(Prim.ne);
			break;
		//unop
		case NEG:
			Machine.emit(Prim.neg);
			break;
		case NOT:
			Machine.emit(Prim.not);
			break;
		default:
			reporter.reportGenerationError("Couldn't call operation of type "+op.type);
		}
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, String arg) {
		int result = Integer.parseInt(num.spelling);
		Machine.emit(Op.LOADL,result);
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, String arg) {
		if (bool.spelling.equals("true"))
			Machine.emit(Op.LOADL,1);
		else
			Machine.emit(Op.LOADL,0);
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral n, String arg) {
		Machine.emit(Op.LOADL,Machine.nullRep);
		return null;
	}

}
