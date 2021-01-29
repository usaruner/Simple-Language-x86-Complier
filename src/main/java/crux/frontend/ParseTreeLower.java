package crux.frontend;

import crux.frontend.ast.*;
import crux.frontend.ast.OpExpr.Operation;
import crux.frontend.ast.traversal.NodeVisitor;
import crux.frontend.pt.CruxBaseVisitor;
import crux.frontend.pt.CruxParser;
import crux.frontend.types.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In this class, you're going to implement functionality that transform a input ParseTree
 * into an AST tree.
 *
 * The lowering process would start with {@link #lower(CruxParser.ProgramContext)}. Which take top-level
 * parse tree as input and process its children, function definitions and array declarations for example,
 * using other utilities functions or classes, like {@link #lower(CruxParser.StatementListContext)} or {@link DeclarationVisitor},
 * recursively.
 * */

public final class ParseTreeLower {
    private final DeclarationVisitor declarationVisitor = new DeclarationVisitor();
    private final StatementVisitor statementVisitor = new StatementVisitor();
    private final ExpressionVisitor expressionVisitor = new ExpressionVisitor(true);
    private final ExpressionVisitor locationVisitor = new ExpressionVisitor(false);

    private final SymbolTable symTab;

    public ParseTreeLower(PrintStream err) {
        symTab = new SymbolTable(err);
    }

    private static Position makePosition(ParserRuleContext ctx) {
        var start = ctx.start;
        return new Position(start.getLine());
    }
    public Type StringToType(String t){
        switch (t){
            case "bool":
                return new BoolType();
            case "int":
                return new IntType();
            case "void":
                return new VoidType();
        }
        return null;
    }

    /**
     * Should returns true if we have encountered an error.
     */

    public boolean hasEncounteredError() {
        return false;
    }

    /**
     * Lower top-level parse tree to AST
     * @return a {@link DeclarationList} object representing the top-level AST.
     * */

    public DeclarationList lower(CruxParser.ProgramContext program) {

        List<CruxParser.DeclarationContext> DList = program.declarationList().declaration();
        List<Declaration> declarations = new ArrayList<Declaration>();
//        System.out.print("Declaration");

        for(int i = 0; i < DList.size(); i++)
        {
//            System.out.print(program.declarationList().declaration().get(i).getText());
            declarations.add(DList.get(i).accept(declarationVisitor));
            //declarations.add(declarationVisitor.visitVariableDeclaration(DList.get(i).variableDeclaration()));
            //declarations.add(declarationVisitor.visitArrayDeclaration(DList.get(i).arrayDeclaration()));
            //declarations.add(declarationVisitor.visitFunctionDefinition(DList.get(i).functionDefinition()));
        }
        new DeclarationList(makePosition(program), declarations);

        return new DeclarationList(makePosition(program), declarations);
    }

    /**
     * Lower statement list by lower individual statement into AST.
     * @return a {@link StatementList} AST object.
     * */


    private StatementList lower(CruxParser.StatementListContext statementList) {
        List<CruxParser.StatementContext> SList = statementList.statement();
        List<Statement> statements = new ArrayList<Statement>();
//        System.out.print("Statement List");
        for(int i = 0; i < SList.size(); i++)
        {
            statements.add(SList.get(i).accept(statementVisitor));
//            statements.add(statementVisitor.visitVariableDeclaration(SList.get(i).variableDeclaration()));
//            statements.add(statementVisitor.visitAssignmentStatement(SList.get(i).assignmentStatement()));
//            statements.add(statementVisitor.visitCallStatement(SList.get(i).callStatement()));
//            statements.add(statementVisitor.visitIfStatement(SList.get(i).ifStatement()));
//            statements.add(statementVisitor.visitWhileStatement(SList.get(i).whileStatement()));
//            statements.add(statementVisitor.visitReturnStatement(SList.get(i).returnStatement()));
        }
//        System.out.print("Done" );
        return new StatementList(makePosition(statementList), statements);
    }


    /**
     * Similar to {@link #lower(CruxParser.StatementListContext)}, but handling symbol table
     * as well.
     * @return a {@link StatementList} AST object.
     * */


    private StatementList lower(CruxParser.StatementBlockContext statementBlock) {
//        System.out.print("Statement Block");
        List<CruxParser.StatementContext> SList = statementBlock.statementList().statement();
        List<Statement> statements = new ArrayList<Statement>();

        for(int i = 0; i < SList.size() ; i++)
        {
            statements.add(SList.get(i).accept(statementVisitor));
        }

        return new StatementList(makePosition(statementBlock), statements);
    }


    /**
     * A parse tree visitor to create AST nodes derived from {@link Declaration}
     * */
    private final class DeclarationVisitor extends CruxBaseVisitor<Declaration> {
        /**
         * Visit a parse tree variable declaration and create an AST {@link VariableDeclaration}
         * @return an AST {@link VariableDeclaration}
         * */


        @Override
        public VariableDeclaration visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
//            System.out.print("Declare Var");
            //System.out.print(ctx.type().getText());
            return new VariableDeclaration(makePosition(ctx), symTab.add( makePosition(ctx), ctx.IDENTIFIER().getText(), StringToType(ctx.type().getText())));
        }


        /**
         * Visit a parse tree array declaration and create an AST {@link ArrayDeclaration}
         * @return an AST {@link ArrayDeclaration}
         * */


        @Override
        public Declaration visitArrayDeclaration(CruxParser.ArrayDeclarationContext ctx) {
            //System.out.print(ctx.type().getText());
            System.out.println(ctx.ARRAY().getText());
            return new ArrayDeclaration(makePosition(ctx), symTab.add( makePosition(ctx), ctx.IDENTIFIER().getText(), new ArrayType(Integer.parseInt(ctx.INTEGER().getText()),StringToType(ctx.type().getText()))));
        }


        /**
         * Visit a parse tree function definition and create an AST {@link FunctionDefinition}
         * @return an AST {@link FunctionDefinition}
         * */

        @Override
        public Declaration visitFunctionDefinition(CruxParser.FunctionDefinitionContext ctx) {
//           System.out.print("Func");
            //System.out.print(ctx.type().getText());
            List<CruxParser.ParameterContext> FList = ctx.parameterList().parameter();
            ArrayList<Type> types = new ArrayList<Type>();
            ArrayList<Symbol> symbols = new ArrayList<Symbol>();
            symTab.enter();
            for(int i = 0; i < FList.size(); i++)
            {
                //System.out.println(FList.get(i).IDENTIFIER().getText());
                types.add(StringToType(FList.get(i).type().getText()));
                symbols.add(symTab.add(makePosition(ctx),FList.get(i).IDENTIFIER().getText(),StringToType(FList.get(i).type().getText())));
            }
            TypeList TList = new TypeList(types);
            //System.out.println(StringToType(ctx.type().getText()));
            StatementList Slist = lower(ctx.statementBlock().statementList());
            symTab.exit();
            return new FunctionDefinition(makePosition(ctx), symTab.add(makePosition(ctx),ctx.IDENTIFIER().getText(),new FuncType(TList, StringToType(ctx.type().getText()))),symbols,Slist);

        }


    }

    /**
     * A parse tree visitor to create AST nodes derived from {@link Statement}
     * */
    private final class StatementVisitor extends CruxBaseVisitor<Statement> {
        /**
         * Visit a parse tree variable declaration and create an AST {@link VariableDeclaration}.
         * Since {@link VariableDeclaration} is both {@link Declaration} and {@link Statement},
         * we simply delegate this to {@link DeclarationVisitor#visitArrayDeclaration(CruxParser.ArrayDeclarationContext)}
         * which we implement earlier.
         * @return an AST {@link VariableDeclaration}
         * */


        @Override
        public Statement visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
//            System.out.print("Visit Variable");
            return new VariableDeclaration(makePosition(ctx), symTab.add( makePosition(ctx), ctx.IDENTIFIER().getText(), StringToType(ctx.type().getText())));
        }


        /**
         * Visit a parse tree assignment statement and create an AST {@link Assignment}
         * @return an AST {@link Assignment}
         * */


        @Override
        public Statement visitAssignmentStatement(CruxParser.AssignmentStatementContext ctx) {
//            System.out.print("AssignStatement");
            //System.out.print(ctx.designator().expression0(0).getText());
            return new Assignment(makePosition(ctx), ctx.designator().accept(expressionVisitor), ctx.expression0().accept(expressionVisitor));
        }


        /**
         * Visit a parse tree call statement and create an AST {@link Call}.
         * Since {@link Call} is both {@link Expression} and {@link Statement},
         * we simply delegate this to {@link ExpressionVisitor#visitCallExpression(CruxParser.CallExpressionContext)}
         * that we will implement later.
         * @return an AST {@link Call}
         * */

        @Override
        public Statement visitCallStatement(CruxParser.CallStatementContext ctx) {

//            List<CruxParser.Expression0Context> EList = ctx.callExpression().expressionList().expression0();
//            List<Expression> exp = new ArrayList<Expression>();
//            System.out.print("callStatement");
//            for(int i = 0; i < EList.size(); i++)
//            {
//                exp.add(EList.get(i).accept(expressionVisitor));
//                //System.out.print(EList.get(i).getText());
//            }
            return (Call)ctx.callExpression().accept(expressionVisitor);
            //return new Call(makePosition(ctx), symTab.lookup(makePosition(ctx),ctx.callExpression().IDENTIFIER().getText()) ,exp);
        }


        /**
         * Visit a parse tree if-else branch and create an AST {@link IfElseBranch}.
         * The template code shows partial implementations that visit the then block and else block
         * recursively before using those returned AST nodes to construct {@link IfElseBranch} object.
         * @return an AST {@link IfElseBranch}
         * */

        @Override
        public Statement visitIfStatement(CruxParser.IfStatementContext ctx) {
            symTab.enter();
//            System.out.print("IF:");
            List<CruxParser.StatementContext> SList = ctx.statementBlock().get(0).statementList().statement();

            List<Statement> thenState = new ArrayList<Statement>();
            List<Statement> elseState = new ArrayList<Statement>();

            for(int i = 0; i < SList.size() ; i++)
            {
                thenState.add(SList.get(i).accept(statementVisitor));
            }
            if(ctx.statementBlock().size() > 1) {
                List<CruxParser.StatementContext> SList2 = ctx.statementBlock().get(1).statementList().statement();

                for (int i = 0; i < SList2.size(); i++) {
                    elseState.add(SList2.get(i).accept(statementVisitor));
                }
                symTab.exit();
                //return new IfElseBranch(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx),thenState), new StatementList(makePosition(ctx),elseState));
                return new IfElseBranch(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx.statementBlock(0).statementList()),thenState), new StatementList(makePosition(ctx.statementBlock(1).statementList()),elseState));
            }
            symTab.exit();
            return new IfElseBranch(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx.statementBlock(0).statementList()),thenState), new StatementList(makePosition(ctx),elseState));
            //return new IfElseBranch(makePosition(ctx), ctx.expression0().accept(expressionVisitor),lower(ctx.statementBlock(0)), null);
        }


        /**
         * Visit a parse tree while loop and create an AST {@link WhileLoop}.
         * You'll going to use a similar techniques as {@link #visitIfStatement(CruxParser.IfStatementContext)}
         * to decompose this construction.
         * @return an AST {@link WhileLoop}
         * */

        @Override
        public Statement visitWhileStatement(CruxParser.WhileStatementContext ctx) {
            symTab.enter();
            List<CruxParser.StatementContext> SList =  ctx.statementBlock().statementList().statement();
            List<Statement> State = new ArrayList<Statement>();
            for(int i = 0; i < SList.size(); i++)
            {
                State.add(SList.get(i).accept(statementVisitor));
            }
            ctx.statementBlock().statementList();
            symTab.exit();
            return new WhileLoop(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx.statementBlock().statementList()),State));
        }


        /**
         * Visit a parse tree return statement and create an AST {@link Return}.
         * Here we show a simple example of how to lower a simple parse tree construction.
         * @return an AST {@link Return}
         * */

        @Override
        public Statement visitReturnStatement(CruxParser.ReturnStatementContext ctx) {
            return new Return(makePosition(ctx), ctx.expression0().accept(expressionVisitor));
        }

    }

    private final class ExpressionVisitor extends CruxBaseVisitor<Expression> {
        private final boolean dereferenceDesignator;

        private ExpressionVisitor(boolean dereferenceDesignator) {
            this.dereferenceDesignator = dereferenceDesignator;
        }


        @Override
        public Expression visitExpression0(CruxParser.Expression0Context ctx) {
//            System.out.print("ex0");
            if (ctx.op0(0) != null){
                String op = "";
//                System.out.print("op" +  ctx.op0(0).getText());
//                System.out.print( "value" + ctx.expression1(0).getText());
                if (ctx.op0(0).getText().equals(">="))
                    op = "GE";
                if (ctx.op0(0).getText().equals("<="))
                    op = "LE";
                if (ctx.op0(0).getText().equals("!="))
                    op = "NE";
                if (ctx.op0(0).getText().equals("=="))
                    op = "EQ";
                if (ctx.op0(0).getText().equals(">"))
                    op = "GT";
                if (ctx.op0(0).getText().equals("<"))
                    op = "LT";
                return new OpExpr(makePosition(ctx), OpExpr.Operation.valueOf(op), ctx.expression1(0).accept(expressionVisitor), ctx.expression1(1).accept(expressionVisitor));
            }

            return ctx.expression1(0).accept(expressionVisitor);

        }



        @Override
        public Expression visitExpression1(CruxParser.Expression1Context ctx) {
            //System.out.print("ex1");
            if (ctx.op1(0) != null) {
                String op = "";
                if (ctx.op1(0).getText().equals("+"))
                    op = "ADD";
                if (ctx.op1(0).getText().equals("-"))
                    op = "SUB";
                if (ctx.op1(0).getText().equals("or"))
                    op = "LOGIC_OR";
                OpExpr prev = new OpExpr(makePosition(ctx),OpExpr.Operation.valueOf(op), ctx.expression2( 0).accept(expressionVisitor), ctx.expression2( 1).accept(expressionVisitor));
                for (int i = 1; i < ctx.op1().size(); i++) {
                    if (ctx.op1(i).getText().equals("+"))
                        op = "ADD";
                    if (ctx.op1(i).getText().equals("-"))
                        op = "SUB";
                    if (ctx.op1(i).getText().equals("or"))
                        op = "LOGIC_OR";
                    prev = new OpExpr(makePosition(ctx),OpExpr.Operation.valueOf(op), prev, ctx.expression2(i + 1).accept(expressionVisitor));
                }
                return prev;
            }
            return ctx.expression2(0).accept(expressionVisitor);
        }



        @Override
        public Expression visitExpression2(CruxParser.Expression2Context ctx) {
            //System.out.println("ex2");

                if (ctx.op2(0) != null) {
                    String op = "";
                    if (ctx.op2(0).getText().equals("*"))
                        op = "MULT";
                    if (ctx.op2(0).getText().equals("/"))
                        op = "DIV";
                    if (ctx.op2(0).getText().equals("and"))
                        op = "LOGIC_AND";
                    OpExpr prev = new OpExpr(makePosition(ctx),OpExpr.Operation.valueOf(op), ctx.expression3( 0).accept(expressionVisitor), ctx.expression3( 1).accept(expressionVisitor));
                    for(int i = 1; i < ctx.op2().size(); i++) {

                        //System.out.println("op" + ctx.op2(i).getText());
                        if (ctx.op2(i).getText().equals("*"))
                            op = "MULT";
                        if (ctx.op2(i).getText().equals("/"))
                            op = "DIV";
                        if (ctx.op2(i).getText().equals("and"))
                            op = "LOGIC_AND";
                        prev = new OpExpr(makePosition(ctx),OpExpr.Operation.valueOf(op), prev, ctx.expression3(i + 1).accept(expressionVisitor));
                    }
                    return prev;
                }
            return ctx.expression3(0).accept(expressionVisitor);
        }



        @Override
        public Expression visitExpression3(CruxParser.Expression3Context ctx) {
            //System.out.print("ex3");
            if(ctx.NOT() !=  null) {
                return new OpExpr(makePosition(ctx), Operation.valueOf("!"), ctx.expression3().accept(expressionVisitor), null);
            }
            if(ctx.expression0() != null){
                return ctx.expression0().accept(expressionVisitor);
            }
            if(ctx.designator() != null){
                return ctx.designator().accept(locationVisitor);
            }
            if(ctx.callExpression() != null){
                return ctx.callExpression().accept(expressionVisitor);
            }
            return ctx.literal().accept(expressionVisitor);
        }



        @Override
        public Call visitCallExpression(CruxParser.CallExpressionContext ctx) {
//            System.out.print("Expression Statement");
            symTab.enter();
            List<CruxParser.Expression0Context> EList = ctx.expressionList().expression0();
            List<Expression> exp = new ArrayList<Expression>();
            for(int i = 0; i < EList.size(); i++) {
                exp.add(EList.get(i).accept(expressionVisitor));
            }
            symTab.exit();
            return new Call(makePosition(ctx), symTab.lookup(makePosition(ctx),ctx.IDENTIFIER().getText()), exp);
        }



        @Override
        public Expression visitDesignator(CruxParser.DesignatorContext ctx) {
            //System.out.print("Designator");
            //System.out.print(ctx.expression0(0).getText());
            if(ctx.expression0(0) != null) {
                if (!dereferenceDesignator) {
                    return new Dereference(makePosition(ctx), new ArrayAccess(makePosition(ctx), new Name(makePosition(ctx), symTab.lookup(makePosition(ctx), ctx.IDENTIFIER().getText())), ctx.expression0(0).accept(expressionVisitor)));
                }
                //new Dereference(makePosition(ctx),
                return new ArrayAccess(makePosition(ctx), new Name(makePosition(ctx), symTab.lookup(makePosition(ctx), ctx.IDENTIFIER().getText())), ctx.expression0(0).accept(expressionVisitor));
                // TODO
            }
            if (!dereferenceDesignator) {
                return new Dereference(makePosition(ctx), new Name(makePosition(ctx), symTab.lookup(makePosition(ctx), ctx.IDENTIFIER().getText())));
            }
            return new Name(makePosition(ctx), symTab.lookup(makePosition(ctx), ctx.IDENTIFIER().getText()));
        }



        @Override
        public Expression visitLiteral(CruxParser.LiteralContext ctx) {
            // TODO
            //System.out.print("Lit");
            var position = makePosition(ctx);
            if(ctx.INTEGER() != null) {
                return new LiteralInt(position, Integer.parseInt(ctx.INTEGER().toString()));
            }
            if(ctx.TRUE() != null){
                return new LiteralBool(position, Boolean.valueOf(ctx.TRUE().toString()));
            }
            return new LiteralBool(position, Boolean.valueOf(ctx.FALSE().toString()));
        }

    }
}