package crux.frontend.types;

import crux.frontend.Symbol;
import crux.frontend.ast.*;
import crux.frontend.ast.traversal.NullNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TypeChecker {
    private final HashMap<Node, Type> typeMap = new HashMap<>();
    private final ArrayList<String> errors = new ArrayList<>();

    public ArrayList<String> getErrors() {
        return errors;
    }

    public void check(DeclarationList ast) {
        var inferenceVisitor = new TypeInferenceVisitor();
        inferenceVisitor.visit(ast);
        //System.out.println(typeMap.toString());
    }

    private void addTypeError(Node n, String message) {
        errors.add(String.format("TypeError%s[%s]", n.getPosition(), message));
    }

    private void setNodeType(Node n, Type ty) {
        typeMap.put(n, ty);
        if (ty.getClass() == ErrorType.class) {
            var error = (ErrorType) ty;
            addTypeError(n, error.getMessage());
        }
    }

    /** 
      *  Returns type of given AST Node.
      */
    public Type getType(Node n) {
        return typeMap.get(n);
    }

    private final class TypeInferenceVisitor extends NullNodeVisitor {
        private Symbol currentFunctionSymbol;
        private Type currentFunctionReturnType;

        private boolean lastStatementReturns;

        @Override
        public void visit(Name name) {
            //System.out.print("Name: " + name.getSymbol().getType());
            typeMap.put(name,new AddressType(name.getSymbol().getType()));
        }

        @Override
        public void visit(ArrayDeclaration arrayDeclaration) {
            //System.out.print("Array");
            Class type = ((ArrayType)arrayDeclaration.getSymbol().getType()).getBase().getClass();
            if(type != IntType.class && type != BoolType.class){
                errors.add("ERROR: ARRAY DECLARED CAN NOT BE TYPE " + type + " LINE "  + arrayDeclaration.getPosition());
            }
        }

        @Override
        public void visit(Assignment assignment) {
            //System.out.println("Assignment");
            //System.out.println("AssignValue" + retHelper(assignment.getValue()).getClass());
            //System.out.println("AssignLocation" + retHelper(assignment.getLocation()).getClass() );
            if(retHelper(assignment.getValue()).getClass() == retHelper(assignment.getLocation()).getClass() ) {
                typeMap.put(assignment, new VoidType());
                assignment.getValue().accept(this);
                assignment.getLocation().accept(this);
            }else{
                errors.add("ERROR CANT ASSIGN " + retHelper(assignment.getLocation()).getClass() +" to " + retHelper(assignment.getValue()).getClass());
            }

        }

        @Override
        public void visit(Call call) {
            //System.out.println("call" + call.getCallee().getName() + ((FuncType)call.getCallee().getType()));
            Symbol oldFunctionSymbol = currentFunctionSymbol;
            Type oldFunctionReturnType = currentFunctionReturnType;
            currentFunctionSymbol = call.getCallee();
            currentFunctionReturnType = ((FuncType) call.getCallee().getType()).getRet();
            //System.out.println("Call" + ((FuncType)call.getCallee().getType()).getRet());
            Iterator<Type> it = ((FuncType) call.getCallee().getType()).getArgs().iterator();
            int len = 0;
            while(it.hasNext()){
                it.next();
                len++;
            }
            //System.out.print("ARGS: " + call.getCallee().getName());
            //System.out.print("ARGS: " + call.getArguments().toString());
           // System.out.println("LEN: " + len + " vs " + call.getArguments().size());
            if (len == call.getArguments().size()) {
                it = ((FuncType) call.getCallee().getType()).getArgs().iterator();
                for(int i = 0; i < call.getArguments().size();i++) {
                    if (it.hasNext() && it.next().getClass() != retHelper(call.getArguments().get(i)).getClass()) {
                        errors.add("ERROR: ARGS " + i + " DOES NOT MATCH. EXPECT: " + it.next().getClass() + " BUT WAS " + retHelper(call.getArguments().get(i)).getClass());
                    }
                }
                for (int i = 0; i < call.getChildren().size(); i++) {

                        call.getChildren().get(i).accept(this);

                }
                if (((FuncType) call.getCallee().getType()) != null) {
                    //System.out.println("HI");
                    typeMap.put(call, ((FuncType) call.getCallee().getType()).getRet());
                }
            }else{
                errors.add("ERROR: ARGS LENGTH EXPECTED TO BE " + len + " BUT WAS " + call.getArguments().size());
            }
            currentFunctionSymbol = oldFunctionSymbol;
            currentFunctionReturnType = oldFunctionReturnType;
        }

        @Override
        public void visit(DeclarationList declarationList){
            //System.out.print("Declaration");
            boolean hasMain = false;
            for(int i = 0; i < declarationList.getChildren().size(); i++){
                declarationList.getChildren().get(i).accept(this);
                if(declarationList.getChildren().get(i).getClass() == FunctionDefinition.class) {
                    if (((FunctionDefinition) declarationList.getChildren().get(i)).getSymbol().toString().contains("main:func(TypeList()):void)")) {
                        hasMain = true;
                    }
                }
            }
            if(hasMain == false)
                errors.add("NO MAIN");
        }

        @Override
        public void visit(Dereference dereference) {
            //System.out.print("deref");
            //System.out.print("REF: " + ListNode.class);
            if(dereference.getAddress().getClass() == Name.class){
                typeMap.put(dereference, ((Name) dereference.getAddress()).getSymbol().getType());
            }else{
                if (((ArrayAccess)dereference.getAddress()).getBase().getSymbol().getType().toString().contains("int")) {
                    typeMap.put(dereference, new IntType());
                }else if(((ArrayAccess)dereference.getAddress()).getBase().getSymbol().getType().toString().contains("bool")) {
                    typeMap.put(dereference, new BoolType());
                }
            }
            dereference.getAddress().accept(this);
        }

        @Override
        public void visit(FunctionDefinition functionDefinition) {
            //System.out.println("FuncDef " + ((FuncType)functionDefinition.getSymbol().getType()).getRet());
            currentFunctionSymbol = functionDefinition.getSymbol();
            currentFunctionReturnType = ((FuncType)functionDefinition.getSymbol().getType()).getRet();
            for(int i = 0; i < functionDefinition.getParameters().size(); i++){
                //System.out.println(functionDefinition.getParameters().get(i).toString());
                typeMap.put(new Name(functionDefinition.getPosition(),functionDefinition.getParameters().get(i)),functionDefinition.getParameters().get(i).getType());
            }

            for(int i = 0; i < functionDefinition.getStatements().getChildren().size(); i++){
                functionDefinition.getStatements().getChildren().get(i).accept(this);
            }

            /*
            boolean ret = false;
            if (((FuncType)functionDefinition.getSymbol().getType()).getRet().getClass() == IntType.class || ((FuncType)functionDefinition.getSymbol().getType()).getRet().getClass() == BoolType.class)
            {
                for (int i = 0; i < functionDefinition.getParameters().size(); i++) {
                    //System.out.println(functionDefinition.getParameters().get(i).toString());
                    typeMap.put(new Name(functionDefinition.getPosition(), functionDefinition.getParameters().get(i)), functionDefinition.getParameters().get(i).getType());
                }
                for (int i = 0; i < functionDefinition.getStatements().getChildren().size(); i++) {
                    System.out.print("Child:" + functionDefinition.getStatements().getChildren().get(i).getClass());
                    functionDefinition.getStatements().getChildren().get(i).accept(this);
                    if(functionDefinition.getStatements().getChildren().get(i).getClass() == Return.class){
                        ret = true;
                        if(((FuncType)functionDefinition.getSymbol().getType()).getRet().getClass()  != ((Return)functionDefinition.getStatements().getChildren().get(i)).getValue().getClass())
                        {
                            errors.add("THE RETURN TYPE DO NOT MATCH: " + ((FuncType) functionDefinition.getSymbol().getType()).getRet().getClass() + " to " + ((Return)functionDefinition.getStatements().getChildren().get(i)).getValue().getClass());
                        }
                    }
                }
                if(ret == false){
                    errors.add("NO RETURN TYPE EXPECTED: " + ((FuncType)functionDefinition.getSymbol().getType()).getRet().getClass());
                }
            }
            if (((FuncType)functionDefinition.getSymbol().getType()).getRet().getClass() == VoidType.class) {
                for (int i = 0; i < functionDefinition.getParameters().size(); i++) {
                    //System.out.println(functionDefinition.getParameters().get(i).toString());
                    typeMap.put(new Name(functionDefinition.getPosition(), functionDefinition.getParameters().get(i)), functionDefinition.getParameters().get(i).getType());
                }
                for (int i = 0; i < functionDefinition.getStatements().getChildren().size(); i++) {
                    System.out.print("Child:" + functionDefinition.getStatements().getChildren().get(i).getClass());
                    functionDefinition.getStatements().getChildren().get(i).accept(this);
                    System.out.println("Return False");
                }
            }
            */
        }

        @Override
        public void visit(IfElseBranch ifElseBranch) {
            //System.out.print("if");
            StatementList sList1 = ifElseBranch.getElseBlock();
            StatementList sList2 = ifElseBranch.getThenBlock();
            if(retHelper(ifElseBranch.getCondition()).getClass() == BoolType.class) {
                ifElseBranch.getCondition().accept(this);
                for (int i = 0; i < sList2.getChildren().size(); i++) {
                    //System.out.println(sList2.getChildren().toString());
                    sList2.getChildren().get(i).accept(this);
                }
                for (int i = 0; i < sList1.getChildren().size(); i++) {
                    //System.out.println(sList1.getChildren().toString());
                    sList1.getChildren().get(i).accept(this);
                }
            }else{
                errors.add("ERROR: CONDITION IS NOT A BOOL");
            }

        }

        @Override
        public void visit(ArrayAccess access) {
            //System.out.print("access");
            //System.out.println("ac" + access.getBase().getSymbol().getType());
            if (access.getBase().getSymbol().getType().toString().contains("int")) {
                typeMap.put(access, new AddressType(new IntType()));
            } else if (access.getBase().getSymbol().getType().toString().contains("bool")) {
                typeMap.put(access, new AddressType(new BoolType()));
                // ( access.getBase()).accept(this);
            } else{
                errors.add("ERROR: ARRAY CAN NOT ACCESS TYPE " + access.getBase().getSymbol().getType() + " LINE " + access.getPosition());
            }
            access.getBase().accept(this);
            access.getOffset().accept(this);
        }

        @Override
        public void visit(LiteralBool literalBool) {
            //System.out.print("Bool");
            typeMap.put(literalBool,new BoolType());
        }

        @Override
        public void visit(LiteralInt literalInt) {
            //System.out.print("Int");
            typeMap.put(literalInt,new IntType());
        }

        @Override
        public void visit(OpExpr op) {
            //System.out.print("OP");

            if(op.getOp() == OpExpr.Operation.ADD || op.getOp() == OpExpr.Operation.SUB || op.getOp() == OpExpr.Operation.MULT || op.getOp() == OpExpr.Operation.DIV) {
                op.getRight().accept(this);
                op.getLeft().accept(this);
                typeMap.put(op,new IntType());
            }else if (op.getOp() == OpExpr.Operation.EQ || op.getOp() == OpExpr.Operation.GE || op.getOp() == OpExpr.Operation.GT || op.getOp() == OpExpr.Operation.LE || op.getOp() == OpExpr.Operation.LT || op.getOp() == OpExpr.Operation.NE || op.getOp() == OpExpr.Operation.LOGIC_OR || op.getOp() == OpExpr.Operation.LOGIC_NOT || op.getOp() == OpExpr.Operation.LOGIC_AND) {
                op.getRight().accept(this);
                op.getLeft().accept(this);
                typeMap.put(op,new BoolType());
            }

        }
        public Type retHelper(Expression ret){
            Type temp = null;
            //System.out.println( ret.getClass() + " vs " + currentFunctionReturnType.getClass());
            if (ret.getClass() == Call.class){
                temp = ((FuncType)((Call) ret).getCallee().getType()).getRet();
                //System.out.println(((FuncType)((Call) ret).getCallee().getType()).getRet().getClass()  + " vs " + currentFunctionReturnType.getClass());
            } else if ( ret.getClass() == LiteralInt.class ) {
                temp = new IntType();
            } else if ( ret.getClass() == LiteralBool.class ){
                temp = new BoolType();
            } else if ( ret.getClass() == OpExpr.class ) {
                //System.out.println("OP");
                //System.out.println((((OpExpr)ret).getLeft()).getClass());
                OpExpr op = ((OpExpr)ret);
                if(retHelper(((OpExpr)ret).getLeft()).getClass() == retHelper(((OpExpr)ret).getLeft()).getClass() ){
                    if (op.getOp() == OpExpr.Operation.EQ || op.getOp() == OpExpr.Operation.GE || op.getOp() == OpExpr.Operation.GT || op.getOp() == OpExpr.Operation.LE || op.getOp() == OpExpr.Operation.LT || op.getOp() == OpExpr.Operation.NE || op.getOp() == OpExpr.Operation.LOGIC_OR || op.getOp() == OpExpr.Operation.LOGIC_NOT || op.getOp() == OpExpr.Operation.LOGIC_AND)
                        temp = new BoolType();
                    else if(op.getOp() == OpExpr.Operation.ADD || op.getOp() == OpExpr.Operation.SUB || op.getOp() == OpExpr.Operation.MULT || op.getOp() == OpExpr.Operation.DIV)
                        temp = new IntType();
                }else{
                        temp =  null;
                }

            }else if ( ret.getClass() == Dereference.class ) {
                temp = retHelper(((Dereference)ret).getAddress());
            }else if(ret.getClass() == Name.class ) {
                temp = ((Name)ret).getSymbol().getType();
            }else if(ret.getClass() == ArrayAccess.class) {
                if (((ArrayAccess)ret).getBase().getSymbol().getType().toString().contains("int")) {
                    temp = new IntType();
                } else if (((ArrayAccess)ret).getBase().getSymbol().getType().toString().contains("bool")) {
                    temp = new BoolType();
                    // ( access.getBase()).accept(this);
                } else{
                    errors.add("ERROR: ARRAY CAN NOT ACCESS TYPE " + ((ArrayAccess)ret).getBase().getSymbol().getType() + " LINE " + ((ArrayAccess)ret).getPosition());
                }
            }
            return temp;
        }
        @Override
        public void visit(Return ret) {
            //System.out.println(retHelper(ret.getValue())  + " vs " + currentFunctionReturnType.getClass());
            if(retHelper(ret.getValue()).getClass() == currentFunctionReturnType.getClass()) {
                ret.getValue().accept(this);
            }else{
                //System.out.println("hello: " + ret.getValue());
                errors.add("RETURN TYPE DOES NOT MATCH");
            }
        }

        @Override
        public void visit(StatementList statementList) {
            for(int i = 0; i < statementList.getChildren().size(); i++) {
                statementList.getChildren().get(i).accept(this);
            }
        }

        @Override
        public void visit(VariableDeclaration variableDeclaration) {
            Class type = variableDeclaration.getSymbol().getType().getClass();
            if(type != BoolType.class && type != IntType.class){
                errors.add("ERROR: VARIABLE DECLARED CAN NOT BE TYPE " + type + " LINE "  + variableDeclaration.getPosition());
            }
        }

        @Override
        public void visit(WhileLoop whileLoop) {
            if (retHelper(whileLoop.getCondition()).getClass() == BoolType.class)
            {
                whileLoop.getCondition().accept(this);
                StatementList sList1 = whileLoop.getBody();
                for (int i = 0; i < sList1.getChildren().size(); i++) {
                    //System.out.println(sList1.getChildren().toString());
                    sList1.getChildren().get(i).accept(this);
                }
            }else{
                errors.add("ERROR: CONDITION IS NOT A BOOL");
            }
        }
    }
}
