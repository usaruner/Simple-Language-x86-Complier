package crux.frontend;

import crux.frontend.ast.Position;
import crux.frontend.types.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SymbolTable {
    private final PrintStream err;
    private final List<Map<String, Symbol>> symbolScopes = new ArrayList<>();

    private boolean encounteredError = false;

    SymbolTable(PrintStream err) {
        this.err = err;
        Map<String, Symbol> glob = new HashMap<String, Symbol>();
        glob.put("readInt", new Symbol("readInt",new FuncType(new TypeList() ,new IntType())));
        glob.put("readChar", new Symbol("readChar",new FuncType(new TypeList() ,new IntType())));
        List<Type> Blist = new ArrayList<Type>();
        Blist.add(new BoolType());
        glob.put("printBool", new Symbol("printBool",new FuncType(new TypeList(Blist),new VoidType())));
        List<Type> Ilist = new ArrayList<Type>();
        Ilist.add(new IntType());
        glob.put("printInt", new Symbol("printInt",new FuncType(new TypeList(Ilist),new VoidType())));
        List<Type> Clist = new ArrayList<Type>();
        Clist.add(new IntType());
        glob.put("printChar", new Symbol("printChar",new FuncType(new TypeList(Clist),new VoidType())));
        glob.put("println", new Symbol("println",new FuncType(new TypeList(Clist),new VoidType())));
        symbolScopes.add(glob);
        // TODO
    }

    boolean hasEncounteredError() {
        return encounteredError;
    }

    void enter() {
        symbolScopes.add(new HashMap<String, Symbol>());
        // TODO
    }

    void exit() {
        symbolScopes.remove(symbolScopes.size() - 1);
        // TODO
    }

    Symbol add(Position pos, String name) {
        // TODO
        Symbol sym = new Symbol(name ,new ErrorType("No Type"));
        symbolScopes.get(symbolScopes.size() - 1).put(name, sym);
        return sym;
    }

    Symbol add(Position pos, String name, Type type) {
        // TODO
        Symbol sym = new Symbol(name ,type);
        symbolScopes.get(symbolScopes.size() - 1).put(name, sym);
        return sym;
    }

    Symbol lookup(Position pos, String name) {
        var symbol = find(name);
        if (symbol == null) {
            err.printf("ResolveSymbolError%s[Could not find %s.]%n", pos, name);
            encounteredError = true;
            return new Symbol(name, "ResolveSymbolError");
        } else {
            return symbol;
        }
    }

    private Symbol find(String name) {

        for(int i = symbolScopes.size() ; i > 0;i--)
        {
            //System.out.print("Key: " + symbolScopes.get(i-1).get(name).toString());
            if(symbolScopes.get(i-1).containsKey(name))
                return symbolScopes.get(i-1).get(name);
        }
        // TODO
        return null;
    }
}
