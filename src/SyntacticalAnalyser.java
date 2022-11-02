import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class SyntacticalAnalyser {
    public ArrayList<Token> tokens;
    public int p;
    public Token curToken;
    public static ArrayList<String> syntactic = new ArrayList<>();

    public static String wrap(Nonterminal nonterminal) {
        return "<" + nonterminal.toString() + ">";
    }

    public SyntacticalAnalyser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        p = 0;

        handleCompUnit();
    }

    public void moveForward() {
        curToken = tokens.get(p);
        syntactic.add(curToken.toString());

        p++;
    }

    public void getTokenWithoutAddToGrammar() {
        curToken = tokens.get(p);

        p++;
    }

    public Token lookAhead() {
        return tokens.get(p);
    }

    public Token lookAheadTwo() {
        return tokens.get(p + 1);
    }

    public Token lookAheadThree() {
        return tokens.get(p + 2);
    }

    public void handleCompUnit() {
        //CompUnit → {Decl} {FuncDef} MainFuncDef

        Token token = lookAhead();

        while (token.kindMatch(Kind.CONSTTK) || (
                token.kindMatch(Kind.INTTK) && lookAheadTwo().kindMatch(Kind.IDENFR) && !lookAheadThree().kindMatch(Kind.LPARENT))) {

            handleDecl();
            token = lookAhead();
        }

        while (token.kindMatch(Kind.VOIDTK) || (
                (token.kindMatch(Kind.INTTK) && !lookAheadTwo().kindMatch(Kind.MAINTK)))) {
            handleFuncDef();
            token = lookAhead();
        }

        if (token.kindMatch(Kind.INTTK) && lookAheadTwo().kindMatch(Kind.MAINTK)) {
            handleMainFuncDef();
        } else {
            handleError();
        }

        syntactic.add(wrap(Nonterminal.CompUnit));
    }

    public void handleDecl() {
        //Decl → ConstDecl | VarDecl

        Token token = lookAhead();
        if (token.kindMatch(Kind.CONSTTK)) {
            handleConstDecl();
        } else if (token.kindMatch(Kind.INTTK)) {
            handleVarDecl();
        } else {
            handleError();
        }
    }

    public void handleConstDef() {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal

        moveForward(); // Ident
        Token token = lookAhead();

        while (token.kindMatch(Kind.LBRACK)) {
            moveForward(); // [
            handleConstExp(getExp());
            moveForward(); // ]

            if (!curToken.kindMatch(Kind.RBRACK)) {
                handleError();
            }

            token = lookAhead();
        }
        moveForward(); // =
        handleConstInitVal();

        syntactic.add(wrap(Nonterminal.ConstDef));
    }

    public void handleFuncDef() {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block

        handleFuncType();

        moveForward(); // Ident

        if (curToken.kindMatch(Kind.IDENFR)) {
            moveForward(); // (

            if (!curToken.kindMatch(Kind.LPARENT)) {
                handleError();
            }

            Token token = lookAhead();

            if (!token.kindMatch(Kind.RPARENT)) {
                handleFuncFParams();
            }
            moveForward(); // )

        } else {
            handleError();
        }

        handleBlock();

        syntactic.add(wrap(Nonterminal.FuncDef));
    }

    public void handleMainFuncDef() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block

        moveForward(); // int
        moveForward(); // main
        moveForward(); // (
        moveForward(); // )

        handleBlock();
        syntactic.add(wrap(Nonterminal.MainFuncDef));

    }

    public void handleBlock() {
        // '{' { BlockItem } '}'

        moveForward(); // {

        Token token = lookAhead();
        while (!token.kindMatch(Kind.RBRACE)) {
            handleBlockItem();
            token = lookAhead();
        }
        moveForward(); // }

        syntactic.add(wrap(Nonterminal.Block));
    }

    public void handleBlockItem() {
        // BlockItem → Decl | Stmt

        Token token = lookAhead();
        if (token.kindMatch(Kind.CONSTTK) || token.kindMatch(Kind.INTTK)) {
            handleDecl();
        } else {
            handleStmt();
        }
    }

    public void handleStmt() {
        // Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖 | [Exp] ';' //有无Exp两种情况
        //      | Block
        //      | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
        //      | 'while' '(' Cond ')' Stmt
        //      | 'break' ';'
        //      | 'continue' ';'
        //      | 'return' [Exp] ';' // 1.有Exp 2.无Exp
        //      | LVal '=' 'getint''('')'';'
        //      | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp

        Token token = lookAhead();
        if (token.kindMatch(Kind.IDENFR)) {
            ArrayList<Token> exp = getExp();
            if (!lookAhead().kindMatch(Kind.SEMICN)) {
                handleLVal(exp);
                moveForward(); // =
                if (lookAhead().kindMatch(Kind.GETINTTK)) {
                    moveForward(); // getint
                    moveForward(); // (
                    moveForward(); // )
                    moveForward(); // ;
                } else {
                    handleExp(getExp());//
                    moveForward(); //;
                }
            } else {
                handleExp(exp);
                moveForward(); // ;
            }
        } else if (token.kindExp()) {
            handleExp(getExp());
            moveForward(); // ;
        } else if (token.kindMatch(Kind.LBRACE)) {
            handleBlock();
        } else if (token.kindMatch(Kind.IFTK)) {
            moveForward(); // if
            moveForward(); // (
            handleCond();
            moveForward(); // )
            handleStmt();
            token = lookAhead();
            if (token.kindMatch(Kind.ELSETK)) {
                moveForward(); // else
                handleStmt();
            }
        } else if (token.kindMatch(Kind.WHILETK)) {
            moveForward(); // while
            moveForward(); // (
            handleCond();
            moveForward(); // )
            handleStmt();
        } else if (token.kindMatch(Kind.BREAKTK)) {
            moveForward(); // break
            moveForward(); //;
        } else if (token.kindMatch(Kind.CONTINUETK)) {
            moveForward(); // continue
            moveForward(); //;
        } else if (token.kindMatch(Kind.RETURNTK)) {
            moveForward(); // return
            token = lookAhead();
            if (token.kindExp()) {
                handleExp(getExp());
            }
            moveForward(); // ;
        } else if (token.kindMatch(Kind.PRINTFTK)) {
            moveForward(); // printf
            moveForward(); // (
            moveForward(); // STRCON
            token = lookAhead();
            while (token.kindMatch(Kind.COMMA)) {
                moveForward(); // ,
                handleExp(getExp());
                token = lookAhead();
            }
            moveForward(); // )
            moveForward(); // ;
        } else if (token.kindMatch(Kind.SEMICN)) {
            moveForward(); // ;
        }

        syntactic.add(wrap(Nonterminal.Stmt));
    }

    public void handleFuncFParams() {
        // FuncFParams -> FuncFParam {',' FuncFParam}

        handleFuncFParam();
        Token token = lookAhead();
        while (token.kindMatch(Kind.COMMA)) {
            moveForward();//,
            handleFuncFParam();
            token = lookAhead();
        }

        syntactic.add(wrap(Nonterminal.FuncFParams));
    }

    public void handleFuncFParam() {
        // FuncFParam -> BType Ident ['['']' {'[' ConstExp ']' }]

        moveForward(); // void | int
        moveForward(); // Ident
        Token token = lookAhead();
        if (token.kindMatch(Kind.LBRACK)) {
            moveForward(); // [
            moveForward(); // ]
            token = lookAhead();
            while (token.kindMatch(Kind.LBRACK)) {
                moveForward(); // [
                handleConstExp(getExp());
                moveForward(); // ]
                token = lookAhead();
            }
        }

        syntactic.add(wrap(Nonterminal.FuncFParam));
    }

    public void handleFuncType() {
        // FuncType -> 'void' | 'int'

        moveForward(); // void | int

        syntactic.add(wrap(Nonterminal.FuncType));
    }

    public void handleConstInitVal() {
        Token token = lookAhead();
        if (token.kindMatch(Kind.LBRACE)) {
            moveForward();//{
            token = lookAhead();
            if (!token.kindMatch(Kind.RBRACE)) {
                handleConstInitVal();
                Token token1 = lookAhead();
                while (token1.kindMatch(Kind.COMMA)) {
                    moveForward();//,
                    handleConstInitVal();
                    token1 = lookAhead();
                }
            }
            moveForward();//}
        } else {
            handleConstExp(getExp());
        }

        syntactic.add(wrap(Nonterminal.ConstInitVal));
    }

    public void handleBType() {
        //
    }

    public void handleConstDecl() {
        // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'

        moveForward(); // const
        moveForward(); // int
        if (curToken.kindMatch(Kind.INTTK)) {
            handleBType();
        } else {
            handleError();
        }
        handleConstDef();
        Token token = lookAhead();
        while (token.kindMatch(Kind.COMMA)) {
            moveForward(); // ,
            handleConstDef();
            token = lookAhead();
        }
        moveForward(); // ;

        syntactic.add(wrap(Nonterminal.ConstDecl));
    }

    public void handleVarDecl() {
        // VarDecl → BType VarDef { ',' VarDef } ';'

        moveForward(); // int
        handleVarDef();
        Token token = lookAhead();
        while (token.kindMatch(Kind.COMMA)) {
            moveForward(); // ,
            handleVarDef();
            token = lookAhead();
        }
        moveForward(); // ;

        syntactic.add(wrap(Nonterminal.VarDecl));
    }

    public void handleVarDef() {
        // VarDef → Ident { '[' ConstExp ']' }
        //          | Ident { '[' ConstExp ']' } '=' InitVal

        moveForward(); // Ident
        Token token = lookAhead();
        while (token.kindMatch(Kind.LBRACK)) {
            moveForward(); // [
            handleConstExp(getExp());
            moveForward(); // ]
            token = lookAhead();
        }
        if (token.kindMatch(Kind.ASSIGN)) {
            moveForward(); // =
            handleInitVal();
        }

        syntactic.add(wrap(Nonterminal.VarDef));
    }

    public void handleInitVal() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'

        Token token = lookAhead();
        if (token.kindMatch(Kind.LBRACE)) {
            moveForward(); // {
            token = lookAhead();
            if (!token.kindMatch(Kind.RBRACK)) {
                handleInitVal();
                Token tok = lookAhead();
                while (tok.kindMatch(Kind.COMMA)) {
                    moveForward(); // ,
                    handleInitVal();
                    tok = lookAhead();
                }
            }
            moveForward(); // }
        } else {
            handleExp(getExp());
        }

        syntactic.add(wrap(Nonterminal.InitVal));
    }

    public void handleExp(ArrayList<Token> exp) {
        // Exp → AddExp

        handleAddExp(exp);

        syntactic.add(wrap(Nonterminal.Exp));
    }

    public void handleCond() {
        // Exp -> OrExp

        handleLOrExp(getExp());

        syntactic.add(wrap(Nonterminal.Cond));
    }

    public void handleFuncRParams(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.COMMA)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleExp(exp1);
            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }

        syntactic.add(wrap(Nonterminal.FuncRParams));
    }

    public void handleRelExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.LSS, Kind.LEQ, Kind.GRE, Kind.GEQ)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleAddExp(exp1);

            syntactic.add(wrap(Nonterminal.RelExp));

            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }

    public void handleEqExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.EQL, Kind.NEQ)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleRelExp(exp1);

            syntactic.add(wrap(Nonterminal.EqExp));

            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }

    public void handleLAndExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.AND)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleEqExp(exp1);

            syntactic.add(wrap(Nonterminal.LAndExp));

            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }

    public void handleLOrExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.OR)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleLAndExp(exp1);

            syntactic.add(wrap(Nonterminal.LOrExp));

            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }

    public void handleLVal(ArrayList<Token> exp) {
        syntactic.add(exp.get(0).toString()); // Ident
        if (exp.size() > 1) {
            ArrayList<Token> exp1 = new ArrayList<>();
            int flag = 0;
            for (int i = 1; i < exp.size(); i++) {
                Token token = exp.get(i);
                if (token.kindMatch(Kind.LBRACK)) {
                    flag++;
                    if (flag == 1) {
                        syntactic.add(token.toString());
                        exp1 = new ArrayList<>();
                    } else {
                        exp1.add(token);
                    }
                } else if (token.kindMatch(Kind.RBRACK)) {
                    flag--;
                    if (flag == 0) {
                        handleExp(exp1);
                        syntactic.add(token.toString());
                    } else {
                        exp1.add(token);
                    }
                } else {
                    exp1.add(token);
                }
            }
        }

        syntactic.add(wrap(Nonterminal.LVal));
    }

    public void handleNumber(Token token) {
        syntactic.add(token.toString());

        syntactic.add(wrap(Nonterminal.Number));
    }

    public void handlePrimaryExp(ArrayList<Token> exp) {
        Token token = exp.get(0);
        if (token.kindMatch(Kind.LPARENT)) {
            //remove ( )
            syntactic.add(exp.get(0).toString());
            handleExp(new ArrayList<>(exp.subList(1, exp.size() - 1)));
            syntactic.add(exp.get(exp.size() - 1).toString());
        } else if (token.kindMatch(Kind.IDENFR)) {
            handleLVal(exp);
        } else if (token.kindMatch(Kind.INTCON)) {
            handleNumber(exp.get(0));
        } else {
            handleError();
        }

        syntactic.add(wrap(Nonterminal.PrimaryExp));
    }

    public void handleUnaryExp(ArrayList<Token> exp) {
        Token token = exp.get(0);
        if (token.kindMatch(Kind.PLUS) || token.kindMatch(Kind.MINU) || token.kindMatch(Kind.NOT)) {
            //remove UnaryOp
            handleUnaryOp(exp.get(0));
            handleUnaryExp(new ArrayList<>(exp.subList(1, exp.size())));
        } else if (exp.size() == 1) {
            handlePrimaryExp(exp);
        } else {
            if (exp.get(0).kindMatch(Kind.IDENFR) && exp.get(1).kindMatch(Kind.LPARENT)) {
                //remove Ident ( )
                syntactic.add(exp.get(0).toString());
                syntactic.add(exp.get(1).toString());
                if (exp.size() > 3) {
                    handleFuncRParams(new ArrayList<>(exp.subList(2, exp.size() - 1)));
                }
                syntactic.add(exp.get(exp.size() - 1).toString());
            } else {
                handlePrimaryExp(exp);
            }
        }
        syntactic.add(wrap(Nonterminal.UnaryExp));

    }

    public void handleUnaryOp(Token token) {
        syntactic.add(token.toString());

        syntactic.add(wrap(Nonterminal.UnaryOp));

    }

    public Expression divideExp(ArrayList<Token> exp, ArrayList<Kind> symbol) {
        ArrayList<ArrayList<Token>> exps = new ArrayList<>();
        ArrayList<Token> exp1 = new ArrayList<>();
        ArrayList<Token> symbols = new ArrayList<>();
        boolean unaryFlag = false;
        int flag1 = 0;
        int flag2 = 0;
        for (int i = 0; i < exp.size(); i++) {
            Token token = exp.get(i);
            if (token.kindMatch(Kind.LPARENT)) {
                flag1++;
            }
            if (token.kindMatch(Kind.RPARENT)) {
                flag1--;
            }
            if (token.kindMatch(Kind.LBRACK)) {
                flag2++;
            }
            if (token.kindMatch(Kind.RBRACK)) {
                flag2--;
            }
            if (symbol.contains(token.kind) && flag1 == 0 && flag2 == 0) {
                //UnaryOp
                if (token.kindUnary()) {
                    if (!unaryFlag) {
                        exp1.add(token);
                        continue;
                    }
                }
                exps.add(exp1);
                symbols.add(token);
                exp1 = new ArrayList<>();
            } else {
                exp1.add(token);
            }
            unaryFlag = token.kindMatch(Kind.IDENFR) || token.kindMatch(Kind.RPARENT) || token.kindMatch(Kind.INTCON) || token.kindMatch(Kind.RBRACK);
        }
        exps.add(exp1);
        return new Expression(exps, symbols);
    }

    public void handleMulExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.MULT, Kind.DIV, Kind.MOD)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleUnaryExp(exp1);

            syntactic.add(wrap(Nonterminal.MulExp));
            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }

    public void handleAddExp(ArrayList<Token> exp) {
        Expression expression = divideExp(exp, new ArrayList<>(Arrays.asList(Kind.PLUS, Kind.MINU)));
        int j = 0;
        for (ArrayList<Token> exp1 : expression.getTokens()) {
            handleMulExp(exp1);

            syntactic.add(wrap(Nonterminal.AddExp));
            if (j < expression.getSymbols().size()) {
                syntactic.add(expression.getSymbols().get(j++).toString());
            }
        }
    }


    public void handleConstExp(ArrayList<Token> exp) {
        handleAddExp(exp);

        syntactic.add(wrap(Nonterminal.ConstExp));
    }


    public ArrayList<Token> getExp() {
        ArrayList<Token> exp = new ArrayList<>();
        boolean inFunc = false;
        int funcFlag = 0;
        int flag1 = 0;
        int flag2 = 0;
        Token token = lookAhead();
        while (true) {
            if (token.kindMatch(Kind.SEMICN) || token.kindMatch(Kind.ASSIGN) || token.kindMatch(Kind.RBRACE)) {
                break;
            }
            if (token.kindMatch(Kind.COMMA) && !inFunc) {
                break;
            }
            if (token.kindMatch(Kind.IDENFR)) {
                if (lookAheadTwo().kindMatch(Kind.LPARENT)) {
                    inFunc = true;
                }
            }
            if (token.kindMatch(Kind.LPARENT)) {
                flag1++;
                if (inFunc) {
                    funcFlag++;
                }
            }
            if (token.kindMatch(Kind.RPARENT)) {
                flag1--;
                if (inFunc) {
                    funcFlag--;
                    if (funcFlag == 0) {
                        inFunc = false;
                    }
                }
            }
            if (token.kindMatch(Kind.LBRACK)) {
                flag2++;
            }
            if (token.kindMatch(Kind.RBRACK)) {
                flag2--;
            }
            if (flag1 < 0) {
                break;
            }
            if (flag2 < 0) {
                break;
            }
            getTokenWithoutAddToGrammar();
            exp.add(curToken);
            token = lookAhead();
        }
        return exp;
    }


    public void handleError() {
        RuntimeException e = new RuntimeException("handle err");
        Stream.of(e.getStackTrace()).forEach(System.out::println);
        System.exit(-1);
    }
}
