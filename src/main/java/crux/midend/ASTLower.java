package crux.midend;

import crux.frontend.Symbol;
import crux.frontend.ast.*;
import crux.frontend.ast.traversal.NodeVisitor;
import crux.frontend.types.*;
import crux.midend.ir.core.*;
import crux.midend.ir.core.insts.*;

import java.util.*;

/**
 * Lower from AST to IR
 * */
public final class ASTLower implements NodeVisitor {
    private Program mCurrentProgram = new Program();
    private Function mCurrentFunction = null;

    private Map<Symbol, AddressVar> mCurrentGlobalSymMap = new HashMap<>();
    private Map<Symbol, Variable> mCurrentLocalVarMap = new HashMap<>();
    private Map<String, AddressVar> mBuiltInFuncMap = new HashMap<>();
    private TypeChecker mTypeChecker;
    private Instruction currInstruction;
    private LocalVar currentVar;

    public ASTLower(TypeChecker checker) {
        mTypeChecker = checker;
    }
  
    public Program lower(DeclarationList ast) {
        initBuiltInFunctions();
        visit(ast);
        return mCurrentProgram;
    }

    /**
     * The top level Program
     * */
    private void initBuiltInFunctions() {
        // TODO: Add built-in function symbols
        System.out.println("HIS");
        mBuiltInFuncMap = new HashMap<>();
        List<Type> Blist = new ArrayList<Type>();
        List<Type> Ilist = new ArrayList<Type>();
        List<Type> Clist = new ArrayList<Type>();
        Blist.add(new BoolType());
        Ilist.add(new IntType());
        Clist.add(new IntType());
        mBuiltInFuncMap.put("readInt", new AddressVar(new FuncType(new TypeList() ,new IntType()), "readInt"));
        mBuiltInFuncMap.put("readChar", new AddressVar(new FuncType(new TypeList() ,new IntType()), "readChar"));
        mBuiltInFuncMap.put("printBool", new AddressVar(new FuncType(new TypeList(Blist),new VoidType()), "printBool"));
        mBuiltInFuncMap.put("printInt", new AddressVar(new FuncType(new TypeList(Ilist),new VoidType()), "printInt"));
        mBuiltInFuncMap.put("printChar", new AddressVar(new FuncType(new TypeList(Clist),new VoidType()), "printChar"));
        mBuiltInFuncMap.put("println", new AddressVar(new FuncType(new TypeList(),new VoidType()), "println"));
    }

    public Instruction getLast(){
        Instruction currI = mCurrentFunction.getStart();
        while(currI.getNext(0) != null){
            currI = currI.getNext(0);
        }
        return currI;
    }

    @Override
    public void visit(DeclarationList declarationList) {
        System.out.println("DelcareL");
        Declaration dec;
        AddressVar adv;
        Map<Symbol, AddressVar> globMap = new HashMap<>();
        for(int i = 0; i < declarationList.getChildren().size(); i++) {
            dec = (Declaration) declarationList.getChildren().get(i);

            if (dec.getClass() == FunctionDefinition.class) {
                System.out.println("DelcareL => func");
                adv = new AddressVar(((FunctionDefinition)dec).getSymbol().getType(), ((FunctionDefinition)dec).getSymbol().getName());
                globMap.put(((FunctionDefinition)dec).getSymbol(), adv);
                mCurrentGlobalSymMap = globMap;
                visit((FunctionDefinition) dec);

            } else if (dec.getClass() == ArrayDeclaration.class) {
                adv = new AddressVar(((ArrayDeclaration)dec).getSymbol().getType(), ((ArrayDeclaration)dec).getSymbol().getName());
                globMap.put(((ArrayDeclaration) dec).getSymbol(), adv);
                mCurrentGlobalSymMap = globMap;
                visit((ArrayDeclaration) dec);

            } else if (dec.getClass() == VariableDeclaration.class) {
                adv = new AddressVar(((FunctionDefinition)dec).getSymbol().getType(), ((VariableDeclaration)dec).getSymbol().getName());
                globMap.put(((VariableDeclaration)dec).getSymbol(), adv);
                mCurrentGlobalSymMap = globMap;
                visit((VariableDeclaration) dec);
            }

        }
    }

    /**
     * Function
     * */
    @Override
    public void visit(FunctionDefinition functionDefinition) {
        System.out.println("Func");
        Map<Symbol, Variable> temploc = mCurrentLocalVarMap;
        ArrayList<LocalVar> loc = new ArrayList<>();
        Map<Symbol, Variable> LocalMap = new HashMap<>();
        mCurrentLocalVarMap = LocalMap;
        LocalVar tempVar;
        for(int i = 0; i < functionDefinition.getParameters().size(); i++){
            tempVar = new LocalVar(functionDefinition.getParameters().get(i).getType(),functionDefinition.getParameters().get(i).getName());
            mCurrentLocalVarMap.put(functionDefinition.getParameters().get(i), tempVar);
            loc.add(tempVar);
//            LocalMap.put(functionDefinition.getParameters().get(i),tempVar);
        }
        Function func = new Function(functionDefinition.getSymbol().getName(),loc,(FuncType)functionDefinition.getSymbol().getType());
        mCurrentFunction = func;
        func.setStart(new NopInst());
        currInstruction = func.getStart();
        //System.out.println("NAME: " + func.getName());
        mCurrentProgram.addFunction(func);
        //System.out.println("NAME: " + mCurrentProgram.getFunctions().next().getName());
        //System.out.println("Func => StatementList");
        //System.out.print(mCurrentProgram.getFunctions().next().getStart());
        visit(functionDefinition.getStatements());
        mCurrentLocalVarMap = temploc;
    }

    @Override
    public void visit(StatementList statementList) {
        System.out.println("StamementList");
            Statement stat = null;
        for(int i = 0; i < statementList.getChildren().size(); i++) {
            System.out.println("StamementList" + i);
            stat = (Statement) statementList.getChildren().get(i);
            if(stat.getClass() == VariableDeclaration.class) {
                visit((VariableDeclaration)stat);
            }else if(stat.getClass() == ArrayDeclaration.class){
                visit((ArrayDeclaration)stat);
            }else if(stat.getClass() == Assignment.class){
                visit((Assignment) stat);
            }else if(stat.getClass() == Call.class){
                visit((Call) stat);
            }else if(stat.getClass() == Return.class){
                visit((Return) stat);
            }else if(stat.getClass() == IfElseBranch.class){
                visit((IfElseBranch) stat);
            }else if(stat.getClass() == WhileLoop.class){
                visit((WhileLoop) stat);
            }
        }
    }

    /**
     * Declarations
     * */
    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        mCurrentLocalVarMap.put(variableDeclaration.getSymbol(),new LocalVar(variableDeclaration.getSymbol().getType(),variableDeclaration.getSymbol().getName()));
    }
  
    @Override
    public void visit(ArrayDeclaration arrayDeclaration) {
        mCurrentLocalVarMap.put(arrayDeclaration.getSymbol(),new AddressVar(arrayDeclaration.getSymbol().getType(),arrayDeclaration.getSymbol().getName()));
    }

    @Override
    public void visit(Name name) {

    }

    @Override
    public void visit(Assignment assignment) {
        LocalVar loc = new LocalVar(mTypeChecker.getType(assignment.getValue()),assignment.getValue().toString());
        AddressVar adv = new AddressVar(mTypeChecker.getType(assignment.getLocation()),assignment.getLocation().toString());
        StoreInst store = new StoreInst(loc,adv);
        mCurrentFunction.setStart(store);
    }

    @Override
    public void visit(Call call) {
        System.out.println("CALL");
        //LocalVar loc = null;
        Map<Symbol, Variable> temploc = mCurrentLocalVarMap;
        AddressVar adv =  null;
        ArrayList<LocalVar> args = new ArrayList<LocalVar>();
        Map<Symbol, Variable> locVar = new HashMap<>();
        mCurrentLocalVarMap = locVar;
        LocalVar temp;
        System.out.println("CALLEE: " + currInstruction.toString());

        if(mBuiltInFuncMap.containsKey(call.getCallee().getName())){
            System.out.println("BIN");
            //loc = (LocalVar)mBuiltInFuncMap.get(call.getCallee());
            adv = mBuiltInFuncMap.get(call.getCallee().getName());
        }else if(mCurrentLocalVarMap.containsKey(call.getCallee())){
            System.out.println("LOC");
             //loc = (LocalVar) mCurrentLocalVarMap.get(call.getCallee());
             adv = mCurrentGlobalSymMap.get(call.getCallee().getName());
        }else{
            System.out.println("FUNCTION NOT FOUND: " + call.getCallee().getName());
        }

        for(int i = 0;  i < call.getArguments().size(); i++){
            visit(call.getArguments().get(i));
            args.add((LocalVar)currentVar);
        }
        System.out.println("INST: " + currInstruction);
        currInstruction.setNext(0,new CallInst(adv,args));
        currInstruction = currInstruction.getNext(0);
        System.out.println("INST: " + currInstruction);
        mCurrentLocalVarMap = temploc;
    }

    @Override
    public void visit(OpExpr operation) {
        System.out.println("OP");
        if( mTypeChecker.getType(operation).getClass() == IntType.class){
            visit((LiteralInt) operation.getLeft());
            LocalVar opl = currentVar;
            visit((LiteralInt) operation.getRight());
            LocalVar opr = currentVar;
            System.out.println("L: " + opl);
            System.out.println("R: " + opr);
            LocalVar temp1 = mCurrentFunction.getTempVar(new IntType());
            mCurrentLocalVarMap.put(new Symbol(temp1.getName(),new IntType()),temp1);
            if(operation.getOp() == OpExpr.Operation.ADD) {
                currInstruction.setNext(0, new BinaryOperator(BinaryOperator.Op.Add, temp1, opl,opr));
            }else if(operation.getOp() == OpExpr.Operation.SUB){
                currInstruction.setNext(0, new BinaryOperator(BinaryOperator.Op.Sub, temp1, opl,opr));
            }else if(operation.getOp() == OpExpr.Operation.MULT){
                currInstruction.setNext(0, new BinaryOperator(BinaryOperator.Op.Mul, temp1, opl,opr));
            }else if(operation.getOp() == OpExpr.Operation.DIV){
                currInstruction.setNext(0, new BinaryOperator(BinaryOperator.Op.Div, temp1, opl,opr));
            }
            currentVar = temp1;
            mCurrentLocalVarMap.put(new Symbol(currentVar.getName(),currentVar.getType()),currentVar);
            currInstruction = currInstruction.getNext(0);

        }else if( mTypeChecker.getType(operation).getClass() == BoolType.class){
            visit((LiteralBool) operation.getLeft());
            LocalVar opl = currentVar;
            visit((LiteralBool) operation.getRight());
            LocalVar opr = currentVar;
            System.out.println("L: " + opl);
            System.out.println("R: " + opr);
            LocalVar temp1 = mCurrentFunction.getTempVar(new IntType());
            LocalVar temp2 = mCurrentFunction.getTempVar(new IntType());
            LocalVar temp3 = mCurrentFunction.getTempVar(new IntType());
            currInstruction.setNext(0, new BinaryOperator(Enum.valueOf(BinaryOperator.Op.class,operation.getOp().name()), temp1, temp2, temp3));
            mCurrentLocalVarMap.put(new Symbol(temp1.getName(),temp1.getType()),temp1);
        }
    }

    @Override
    public void visit(Dereference dereference) {
    }

    private void visit(Expression expression) {
        expression.accept(this);
    }

    @Override
    public void visit(ArrayAccess access) {
    }

    @Override
    public void visit(LiteralBool literalBool) {
        currentVar = mCurrentFunction.getTempVar(new BoolType());
        mCurrentLocalVarMap.put(new Symbol(currentVar.getName(),currentVar.getType()),currentVar);
        currInstruction.setNext(0,new CopyInst(currentVar, BooleanConstant.get(mCurrentProgram,(literalBool).getValue())));
        currInstruction = currInstruction.getNext(0);
    }

    @Override
    public void visit(LiteralInt literalInt) {
        System.out.println("INT");
        currentVar = mCurrentFunction.getTempVar(new IntType());
        currInstruction.setNext(0,new CopyInst(currentVar, IntegerConstant.get(mCurrentProgram,(literalInt).getValue())));
        currInstruction = currInstruction.getNext(0);
        mCurrentLocalVarMap.put(new Symbol(currentVar.getName(),currentVar.getType()),currentVar);
    }

    @Override
    public void visit(Return ret) {
    }

    /**
     * Control Structures
     * */
    @Override
    public void visit(IfElseBranch ifElseBranch) {
    }

    @Override
    public void visit(WhileLoop whileLoop) {
    }
}
