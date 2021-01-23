package crux.frontend;

import crux.frontend.ast.*;
import crux.frontend.ast.OpExpr.Operation;
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
        System.out.print("Declaration");
        for(int i = 0; i < DList.size(); i++)
        {
            declarations.add(DList.get(i).accept(declarationVisitor));
            //declarations.add(declarationVisitor.visitVariableDeclaration(DList.get(i).variableDeclaration()));
            //declarations.add(declarationVisitor.visitArrayDeclaration(DList.get(i).arrayDeclaration()));
            //declarations.add(declarationVisitor.visitFunctionDefinition(DList.get(i).functionDefinition()));
        }

        return new DeclarationList(makePosition(program), declarations);
    }

    /**
     * Lower statement list by lower individual statement into AST.
     * @return a {@link StatementList} AST object.
     * */


    private StatementList lower(CruxParser.StatementListContext statementList) {
        List<CruxParser.StatementContext> SList = statementList.statement();
        List<Statement> statements = new ArrayList<Statement>();

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
        return new StatementList(makePosition(statementList), statements);
    }


    /**
     * Similar to {@link #lower(CruxParser.StatementListContext)}, but handling symbol table
     * as well.
     * @return a {@link StatementList} AST object.
     * */


    private StatementList lower(CruxParser.StatementBlockContext statementBlock) {
        List<CruxParser.StatementContext> SList = statementBlock.statementList().statement();
        List<Statement> statements = new ArrayList<Statement>();

        for(int i = 0; i < SList.size(); i++)
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
            return new VariableDeclaration(makePosition(ctx), symTab.add( makePosition(ctx), ctx.IDENTIFIER().getText()));
        }


        /**
         * Visit a parse tree array declaration and create an AST {@link ArrayDeclaration}
         * @return an AST {@link ArrayDeclaration}
         * */


        @Override
        public Declaration visitArrayDeclaration(CruxParser.ArrayDeclarationContext ctx) {
            return new ArrayDeclaration(makePosition(ctx), symTab.add( makePosition(ctx), ctx.IDENTIFIER().getText()));
        }


        /**
         * Visit a parse tree function definition and create an AST {@link FunctionDefinition}
         * @return an AST {@link FunctionDefinition}
         * */

        @Override
        public Declaration visitFunctionDefinition(CruxParser.FunctionDefinitionContext ctx) {
            System.out.print(ctx.IDENTIFIER().getText());
            List<CruxParser.ParameterContext> FList = ctx.parameterList().parameter();
            ArrayList<Type> types = new ArrayList<Type>();
            ArrayList<Symbol> symbols = new ArrayList<Symbol>();
            for(int i = 0; i < FList.size(); i++)
            {
                symbols.add(symTab.add(makePosition(ctx),FList.get(i).IDENTIFIER().getText()));
            }
            System.out.print(ctx.IDENTIFIER().getText());
            return new FunctionDefinition(makePosition(ctx), symTab.add(makePosition(ctx),ctx.IDENTIFIER().getText()),symbols, lower(ctx.statementBlock().statementList()));

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

//
//        @Override
//        public Statement visitVariableDeclaration(CruxParser.VariableDeclarationContext ctx) {
//            return new VariableDeclaration(makePosition(ctx), symTab.add(makePosition(ctx),ctx.IDENTIFIER().getText()));
//        }
//
//
//        /**
//         * Visit a parse tree assignment statement and create an AST {@link Assignment}
//         * @return an AST {@link Assignment}
//         * */
//
//
//        @Override
//        public Statement visitAssignmentStatement(CruxParser.AssignmentStatementContext ctx) {
//            return new Assignment(makePosition(ctx), ctx.accept(locationVisitor), ctx.accept(expressionVisitor));
//        }
//
//
//        /**
//         * Visit a parse tree call statement and create an AST {@link Call}.
//         * Since {@link Call} is both {@link Expression} and {@link Statement},
//         * we simply delegate this to {@link ExpressionVisitor#visitCallExpression(CruxParser.CallExpressionContext)}
//         * that we will implement later.
//         * @return an AST {@link Call}
//         * */
//
//        @Override
//        public Statement visitCallStatement(CruxParser.CallStatementContext ctx) {
//            List<CruxParser.Expression0Context> EList = ctx.callExpression().expressionList().expression0();
//            List<Expression> exp = new ArrayList<Expression>();
//            for(int i = 0; i < EList.size(); i++)
//            {
//                exp.add(EList.get(i).accept(expressionVisitor));
//            }
//            return new Call(makePosition(ctx), symTab.lookup(makePosition(ctx),ctx.getText()) ,exp);
//        }
//
//
//        /**
//         * Visit a parse tree if-else branch and create an AST {@link IfElseBranch}.
//         * The template code shows partial implementations that visit the then block and else block
//         * recursively before using those returned AST nodes to construct {@link IfElseBranch} object.
//         * @return an AST {@link IfElseBranch}
//         * */
//
//        @Override
//        public Statement visitIfStatement(CruxParser.IfStatementContext ctx) {
//            List<CruxParser.StatementContext> SList = ctx.statementBlock().get(0).statementList().statement();
//            List<CruxParser.StatementContext> SList2 = ctx.statementBlock().get(1).statementList().statement();;
//            List<Statement> thenState = new ArrayList<Statement>();
//            List<Statement> elseState = new ArrayList<Statement>();
//            for(int i = 0; i < SList.size(); i++)
//            {
//                thenState.add(SList.get(i).accept(statementVisitor));
//            }
//            for(int i = 0; i < SList2.size(); i++)
//            {
//                elseState.add(SList.get(i).accept(statementVisitor));
//            }
//            return new IfElseBranch(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx),thenState), new StatementList(makePosition(ctx),elseState));
//        }
//
//
//        /**
//         * Visit a parse tree while loop and create an AST {@link WhileLoop}.
//         * You'll going to use a similar techniques as {@link #visitIfStatement(CruxParser.IfStatementContext)}
//         * to decompose this construction.
//         * @return an AST {@link WhileLoop}
//         * */
//
//        @Override
//        public Statement visitWhileStatement(CruxParser.WhileStatementContext ctx) {
//            List<CruxParser.StatementContext> SList =  ctx.statementBlock().statementList().statement();
//            List<Statement> State = new ArrayList<Statement>();
//            for(int i = 0; i < SList.size(); i++)
//            {
//                State.add(SList.get(i).accept(statementVisitor));
//            }
//            ctx.statementBlock().statementList();
//            return new WhileLoop(makePosition(ctx), ctx.expression0().accept(expressionVisitor),new StatementList(makePosition(ctx),State));
//        }
//
//
//        /**
//         * Visit a parse tree return statement and create an AST {@link Return}.
//         * Here we show a simple example of how to lower a simple parse tree construction.
//         * @return an AST {@link Return}
//         * */
//
//        @Override
//        public Statement visitReturnStatement(CruxParser.ReturnStatementContext ctx) {
//            return new Return(makePosition(ctx), ctx.expression0().accept(expressionVisitor));
//        }

    }

    private final class ExpressionVisitor extends CruxBaseVisitor<Expression> {
        private final boolean dereferenceDesignator;

        private ExpressionVisitor(boolean dereferenceDesignator) {
            this.dereferenceDesignator = dereferenceDesignator;
        }

        /*
        @Override
        public Expression visitExpression0(CruxParser.Expression0Context ctx) {

            return new OpExpr(makePosition(ctx), Operation.valueOf(ctx.op0(0).getText()) , ctx.expression1(0).accept(expressionVisitor),ctx.expression1(1).accept(expressionVisitor));

        }



        @Override
        public Expression visitExpression1(CruxParser.Expression1Context ctx) {
            return new OpExpr(makePosition(ctx), Operation.valueOf(ctx.op1(0).getText()) , ctx.expression2(0).accept(expressionVisitor),ctx.expression2(1).accept(expressionVisitor));
        }



        @Override
        public Expression visitExpression2(CruxParser.Expression2Context ctx) {
            return new OpExpr(makePosition(ctx), Operation.valueOf(ctx.op2(0).getText()) , ctx.expression3(0).accept(expressionVisitor),ctx.expression3(1).accept(expressionVisitor));
        }



        @Override
        public Expression visitExpression3(CruxParser.Expression3Context ctx) {
            if(ctx.NOT() !=  null) {
                return new OpExpr(makePosition(ctx), Operation.valueOf("!"), ctx.expression3().accept(expressionVisitor), null);
            }
            if(ctx.expression0() != null){
                return ctx.expression0().accept(expressionVisitor);
            }
            if(ctx.designator() != null){
                return ctx.designator().accept(expressionVisitor);
            }
            if(ctx.callExpression() != null){
                return ctx.callExpression().accept(expressionVisitor);
            }
            return ctx.literal().accept(expressionVisitor);
        }



        @Override
        public Call visitCallExpression(CruxParser.CallExpressionContext ctx) {
            List<CruxParser.Expression0Context> EList = ctx.expressionList().expression0();
            List<Expression> exp = new ArrayList<Expression>();
            for(int i = 0; i < EList.size(); i++)
            {
                exp.add(EList.get(i).accept(expressionVisitor));
            }
            return new Call(makePosition(ctx), symTab.lookup(makePosition(ctx),ctx.getText()), exp);
        }



        @Override
        public Expression visitDesignator(CruxParser.DesignatorContext ctx) {
            return new Dereference(makePosition(ctx), ctx.expression0(0).accept(expressionVisitor));
            // TODO
        }



        @Override
        public Expression visitLiteral(CruxParser.LiteralContext ctx) {
            // TODO
            var position = makePosition(ctx);
            if(ctx.INTEGER().getChildCount() != 0) {
                return new LiteralInt(position, ctx.INTEGER().hashCode());
            }
            if(ctx.TRUE().getChildCount() != 0){
                return new LiteralInt(position, ctx.TRUE().hashCode());
            }
            return new LiteralInt(position, ctx.FALSE().hashCode());
        }
        */
    }
}