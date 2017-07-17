package miniJava.ContextualAnalyzer;

import java.util.HashMap;

import miniJava.ContextError;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;

public class ScopedTable {
	
	ScopedTable previous;
	ScopedTable next;
	int scope;
	final HashMap<String, Declaration> idtable = new HashMap<String, Declaration>();
	
	public ScopedTable (ScopedTable previous) {
		scope = previous.scope; scope++;
		previous.next = this;
		this.previous = previous;
	}
	
	//called for a predefined scope
	public ScopedTable () {
		scope = 0;
	}
	
	public void addDecl (Declaration decl) throws ContextError {
		if (scope >= 3 && searchID(decl.name,3) != null) {
			throw new ContextError(decl);
		}
		if (idtable.get(decl.name) == null) {
			idtable.put(decl.name,decl);
			if (ContextualAnalyzer.trace) {
				for (int i = 0 ; i < scope ; i++)
					System.out.print(". ");
				System.out.println("Decl \""+decl.name+"\" added at scope " + scope);
			}
		} else {
			//duplicate, return error
			throw new ContextError(decl);
		}
	}
	
	public Declaration searchID (Identifier id, String arg) {
		int curScope = scope;
		String key = id.spelling;
		ScopedTable s = this;
			while (s != null) {
			HashMap<String,Declaration> idtable = s.idtable;
			Declaration result = idtable.get(key);
			//found match at this scope
			if (result != null) {
				if (arg != null) {
					int scope = arg.charAt(0);
					if (scope >= 48 && scope <= 57)
						curScope = scope-48;
					else
						key = arg;
				}
				if (ContextualAnalyzer.trace) {
					for (int i = 0 ; i < curScope ; i++)
						System.out.print(". ");
					System.out.println("ID \""+key+"\" "+id.posn+" at scope " + curScope + " linked to Decl \""+result.name+"\" at scope " + s.scope);
				}
				id.decl = result;
				return result;
			}  
			//no match, check previous scopes
			if (s.scope >= 0) {
				s = s.previous;
			} else {
				break;
			}
		}
		return null;
	}
	
	public Declaration searchID (String key, int scope) {
		ScopedTable s = this;
		while (previous != null && s.scope >= scope) {
			HashMap<String,Declaration> stable = s.idtable;
			Declaration result = stable.get(key);
			//found match at this scope
			if (result != null) {
				for (int i = 0 ; i < scope ; i++)
					System.out.print(". ");
				System.out.println("ID \""+key+"\" found at scope " + scope);
				return result;
			} 
			//no match, check previous scopes
			s = s.previous;
		}
		return null;
	}

}
