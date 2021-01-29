package crux.frontend.types;

import crux.frontend.Symbol;
import crux.frontend.ast.*;
import crux.frontend.ast.traversal.NullNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
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
            System.out.print("Name: " + name.getSymbol().getType());
            typeMap.put(name,new AddressType(name.getSymbol().getType()));
        }

        @Override
        public void visit(ArrayDeclaration arrayDeclaration) {
            System.out.print("Array");
            Class type = ((ArrayType)arrayDeclaration.getSymbol().getType()).getBase().getClass();
            if(type != IntType.class && type != BoolType.class){
                errors.add("ERROR: ARRAY DECLARED CAN NOT BE TYPE " + type + " LINE "  + arrayDeclaration.getPosition());
            }
        }

        @Override
        public void visit(Assignment assignment) {
            System.out.println("Assignment");
            //System.out.println("AssignValue" + assignment.getValue());
            //System.out.println("AssignLocation" + assignment.getLocation());
            typeMap.put(assignment,new VoidType());
            assignment.getValue().accept(this);
            assignment.getLocation().accept(this);



        }

        @Override
        public void visit(Call call) {
            System.out.print("call");
            typeMap.put(call,((FuncType)call.getCallee().getType()).getRet());
            for(int i = 0; i < call.getChildren().size(); i++) {
                call.getChildren().get(i).accept(this);
            }
        }

        @Override
        public void visit(DeclarationList declarationList){
            System.out.print("Declaration");

            for(int i = 0; i < declarationList.getChildren().size(); i++){
                declarationList.getChildren().get(i).accept(this);
            }
        }

        @Override
        public void visit(Dereference dereference) {
            System.out.print("deref");
            typeMap.put(dereference,((Name)dereference.getAddress()).getSymbol().getType());
            dereference.getAddress().accept(this);
        }

        @Override
        public void visit(FunctionDefinition functionDefinition) {
            System.out.print("FuncDef");
            for(int i = 0; i < functionDefinition.getStatements().getChildren().size(); i++){
                functionDefinition.getStatements().getChildren().get(i).accept(this);
            }

        }

        @Override
        public void visit(IfElseBranch ifElseBranch) {
            System.out.print("if");
            ifElseBranch.accept(this);
        }

        @Override
        public void visit(ArrayAccess access) {
            System.out.print("access");
            typeMap.put(access,access.getBase().getSymbol().getType());
            access.accept(this);
        }

        @Override
        public void visit(LiteralBool literalBool) {
            System.out.print("Bool");
            typeMap.put(literalBool,new BoolType());
        }

        @Override
        public void visit(LiteralInt literalInt) {
            System.out.print("Int");
            typeMap.put(literalInt,new IntType());
        }

        @Override
        public void visit(OpExpr op) {
            System.out.print("OP");

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

        @Override
        public void visit(Return ret) {
            ret.accept(this);
        }

        @Override
        public void visit(StatementList statementList) {
            statementList.accept(this);
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
            whileLoop.accept(this);
        }
    }
}
