import CodeGen.PCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class SyntacticalAnalyser {
    public ArrayList<Token> tokens;
    public int p;

    public Token curToken;
    
    public static final int OneStep = 1;
    public static final int TwoStep = 2;
    public boolean isCond = false;
    public static ArrayList<String> syntactic = new ArrayList<>();
    public static ArrayList<Token> forDebug = new ArrayList<>();
    public static ArrayList<PCode> codes = new ArrayList<>();

    public static void wrapNonterminal(Nonterminal nonterminal) {
        syntactic.add("<" + nonterminal.toString() + ">");
    }

    public SyntacticalAnalyser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        p = 0;
        curToken = tokens.get(0);

        CompUnitStart();
    }
    
    public void handleError() {
        RuntimeException e = new RuntimeException("handle err");
        Stream.of(e.getStackTrace()).forEach(System.out::println);
        System.exit(-1);
    }

    public Kind lookAhead(int n) {
        if (p + n >= tokens.size()) {
            return Kind.VOID;
        }
        return tokens.get(p + n).kind;
    }

    public void moveForward() {
        if (p >= tokens.size()) {
            handleError();
        }

        forDebug.add(curToken);
        syntactic.add(curToken.toString());
        p += 1;

        if (p < tokens.size()){
            curToken = tokens.get(p);
        }
    }


    public void CompUnitStart() {
        //CompUnit → {Decl} {FuncDef} MainFuncDef

        while (p < tokens.size()) {
            if (curToken.kind == Kind.CONSTTK) {
                DeclHandle();
            } else if (curToken.kind == Kind.INTTK) {
                if (lookAhead(OneStep) == Kind.MAINTK) {
                    MainFuncDefHandle();
                } else {
                    if (lookAhead(TwoStep) == Kind.LPARENT) {
                        FuncDefHandle();
                    } else {
                        DeclHandle();
                    }
                }
            } else if (curToken.kind == Kind.VOIDTK) {
                FuncDefHandle();
            } else {
                handleError();
            }
        }

        wrapNonterminal(Nonterminal.CompUnit);
    }

    public void DeclHandle() {
        //Decl → ConstDecl | VarDecl

        if (curToken.kind == Kind.CONSTTK) {
            ConstDeclHandle();
        } else if (curToken.kind == Kind.INTTK) {
            VarDeclHandle();
        } else {
            handleError();
        }
    }

    public void ConstDeclHandle() {
        // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'

        if (curToken.kind != Kind.CONSTTK) {
            handleError();
        }
        moveForward();

        BTypeHandle();
        ConstDefHandle();
        while (curToken.kind == Kind.COMMA) {
            moveForward();
            ConstDefHandle();
        }

        if (curToken.kind != Kind.SEMICN) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.ConstDecl);
    }

    public void VarDeclHandle() {
        // VarDecl → BType VarDef { ',' VarDef } ';'

        BTypeHandle();
        VarDefHandle();

        while (curToken.kind == Kind.COMMA) {
            moveForward();
            VarDefHandle();
        }

        if (curToken.kind != Kind.SEMICN) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.VarDecl);
    }

    public void BTypeHandle() {
        // BType → 'int'

        if (curToken.kind != Kind.INTTK) {
            handleError();
        }
        moveForward();

    }

    public void ConstDefHandle() {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            moveForward();

            ConstExpHandle();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();
            }
        }
        if (curToken.kind == Kind.ASSIGN) {
            moveForward();
            ConstInitValHandle();
        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.ConstDef);
    }

    public void VarDefHandle() {
        // VarDef → Ident { '[' ConstExp ']' }
        //          | Ident { '[' ConstExp ']' } '=' InitVal

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            moveForward();
            ConstExpHandle();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();

            }
        }

        if (curToken.kind == Kind.ASSIGN) {
            moveForward();
            InitValHandle();
        }

        wrapNonterminal(Nonterminal.VarDef);
    }


    public void InitValHandle() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'

        if (curToken.kind == Kind.LBRACE) {
            moveForward();

            if (curToken.kind == Kind.RBRACE) {
                moveForward();
            } else {
                InitValHandle();

                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    InitValHandle();
                }

                if (curToken.kind != Kind.RBRACE) {
                    handleError();
                }
                moveForward();
            }
        } else {
            ExpHandle();
        }

        wrapNonterminal(Nonterminal.InitVal);
    }
    public void MainFuncDefHandle() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block

        if (curToken.kind != Kind.INTTK) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.MAINTK) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.LPARENT) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.RPARENT) {
            handleError();
        }
        moveForward();

        BlockHandle();

        wrapNonterminal(Nonterminal.MainFuncDef);
    }

    public void FuncDefHandle() {


        FuncTypeHandle();

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind != Kind.LPARENT) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.RPARENT) {
            moveForward();
            BlockHandle();
        } else {
            FuncFParamsHandle();

            if (curToken.kind != Kind.RPARENT) {
                handleError();
            }
            moveForward();

            BlockHandle();
        }

        wrapNonterminal(Nonterminal.FuncDef);
    }

    public void FuncTypeHandle() {
        // FuncType -> 'void' | 'int'

        if (!(curToken.kind == Kind.VOIDTK || curToken.kind == Kind.INTTK)) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.FuncType);
    }

    public void FuncFParamHandle() {
        // FuncFParam -> BType Ident ['['']' {'[' ConstExp ']' }]

        BTypeHandle();

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            moveForward();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();
            }
        }

        wrapNonterminal(Nonterminal.FuncFParam);
    }

    public void FuncFParamsHandle() {
        // FuncFParams -> FuncFParam {',' FuncFParam}

        FuncFParamHandle();
        while (curToken.kind == Kind.COMMA) {
            moveForward();
            FuncFParamHandle();
        }

        wrapNonterminal(Nonterminal.FuncFParams);
    }

    public void FuncRParamsHandle() {
        // FuncRParams → Exp { ',' Exp }

        ExpHandle();
        while (curToken.kind == Kind.COMMA) {
            moveForward();
            ExpHandle();
        }

        wrapNonterminal(Nonterminal.FuncRParams);
    }
    public void BlockHandle() {
        // '{' { BlockItem } '}'

        if (curToken.kind != Kind.LBRACE) {
            handleError();
        }
        moveForward();

        while (curToken.kind != Kind.RBRACE) {
            BlockItemHandle();
        }

        moveForward();

        wrapNonterminal(Nonterminal.Block);
    }

    public void BlockItemHandle() {
        // BlockItem → Decl | Stmt

        if (curToken.kind == Kind.CONSTTK || curToken.kind == Kind.INTTK) {
            DeclHandle();
        } else {
            StmtHandle();
        }
    }

    public void StmtHandle() {
        // Stmt → LVal '=' Exp ';'
        //      | [Exp] ';' //有无Exp两种情况
        //      | Block
        //      | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
        //      | 'while' '(' Cond ')' Stmt
        //      | 'break' ';'
        //      | 'continue' ';'
        //      | 'return' [Exp] ';' // 1.有Exp 2.无Exp
        //      | LVal '=' 'getint''('')'';'
        //      | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp

        switch (curToken.kind) {
            case IFTK:
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                CondHandle();

                if (curToken.kind != Kind.RPARENT) {
                    handleError();
                }
                moveForward();

                StmtHandle();

                if (curToken.kind == Kind.ELSETK) {
                    moveForward();
                    StmtHandle();
                }

                break;

            case WHILETK:
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                CondHandle();

                if (curToken.kind != Kind.RPARENT) {
                    handleError();
                }
                moveForward();

                StmtHandle();
                break;

            case BREAKTK:
            case CONTINUETK:
                moveForward();

                if (curToken.kind != Kind.SEMICN) {
                    handleError();
                }
                moveForward();
                break;

            case RETURNTK:
                moveForward();

                if (curToken.kind == Kind.SEMICN) {
                    moveForward();
                } else {
                    ExpHandle();

                    if (curToken.kind != Kind.SEMICN) {
                        handleError();
                    }
                    moveForward();
                }
                break;

            case PRINTFTK:
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                if (curToken.kind != Kind.STRCON) {
                    handleError();
                }
                moveForward();

                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    ExpHandle();

                }

                if (curToken.kind != Kind.RPARENT) {
                    handleError();
                }
                moveForward();

                if (curToken.kind != Kind.SEMICN) {
                    handleError();
                }
                moveForward();
                break;

            case LBRACE:
                BlockHandle();
                break;

            case IDENFR:
                boolean isLVal = false;
                for (int i = 1; lookAhead(i) != Kind.SEMICN; i++) {
                    if (lookAhead(i) == Kind.ASSIGN) {
                        isLVal = true;
                        break;
                    }
                }

                if (!isLVal) {
                    ExpHandle();

                    if (curToken.kind != Kind.SEMICN) {
                        handleError();
                    }
                    moveForward();
                } else {
                    LValHandle();

                    if (curToken.kind != Kind.ASSIGN) {
                        handleError();
                    }
                    moveForward();

                    if (curToken.kind == Kind.GETINTTK) {
                        moveForward();

                        if (curToken.kind != Kind.LPARENT) {
                            handleError();
                        }
                        moveForward();

                        if (curToken.kind != Kind.RPARENT) {
                            handleError();
                        }
                        moveForward();

                        if (curToken.kind != Kind.SEMICN) {
                            handleError();
                        }
                        moveForward();
                    } else {
                        ExpHandle();

                        if (curToken.kind != Kind.SEMICN) {
                            handleError();
                        }
                        moveForward();
                    }
                }

                break;

            default:
                if (curToken.kind == Kind.SEMICN) {
                    moveForward();
                } else {
                    ExpHandle();
                    if (curToken.kind != Kind.SEMICN) {
                        handleError();
                    }
                    moveForward();

                }
        }

        wrapNonterminal(Nonterminal.Stmt);

    }

    public void ExpHandle() {
        // Exp → AddExp

        AddExpHandle();

        wrapNonterminal(Nonterminal.Exp);
    }

    public void AddExpHandle() {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp

        MulExpHandle();
        while (curToken.kind == Kind.PLUS || curToken.kind == Kind.MINU) {
            wrapNonterminal(Nonterminal.AddExp);
            moveForward();
            MulExpHandle();
        }

        wrapNonterminal(Nonterminal.AddExp);
    }

    public void MulExpHandle() {
        // UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

        UnaryExpHandle();
        while (curToken.kind == Kind.MULT || curToken.kind == Kind.DIV || curToken.kind == Kind.MOD) {
            wrapNonterminal(Nonterminal.MulExp);
            moveForward();
            UnaryExpHandle();
        }

        wrapNonterminal(Nonterminal.MulExp);
    }

    public void RelExpHandle() {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp

        AddExpHandle();
        while (curToken.kind == Kind.LSS || curToken.kind == Kind.LEQ || curToken.kind == Kind.GRE || curToken.kind == Kind.GEQ) {
            wrapNonterminal(Nonterminal.RelExp);
            moveForward();
            AddExpHandle();
        }

        wrapNonterminal(Nonterminal.RelExp);
    }

    public void EqExpHandle() {
        // RelExp | EqExp ('==' | '!=') RelExp

        RelExpHandle();
        while (curToken.kind == Kind.EQL || curToken.kind == Kind.NEQ) {
            wrapNonterminal(Nonterminal.EqExp);
            moveForward();
            RelExpHandle();
        }

        wrapNonterminal(Nonterminal.EqExp);
    }

    public void LAndExpHandle() {
        // EqExp | LAndExp '&&' EqExp

        EqExpHandle();
        while (curToken.kind == Kind.AND) {
            wrapNonterminal(Nonterminal.LAndExp);
            moveForward();
            EqExpHandle();
        }

        wrapNonterminal(Nonterminal.LAndExp);
    }

    public void LOrExpHandle() {
        // LOrExp → LAndExp | LOrExp '||' LAndExp

        LAndExpHandle();
        while (curToken.kind == Kind.OR) {
            wrapNonterminal(Nonterminal.LOrExp);
            moveForward();
            LAndExpHandle();
        }

        wrapNonterminal(Nonterminal.LOrExp);
    }

    public void CondHandle() {
        isCond = true;
        LOrExpHandle();
        isCond = false;

        wrapNonterminal(Nonterminal.Cond);
    }

    public void UnaryExpHandle() {
        // UnaryExp → PrimaryExp
        //          | Ident '(' [FuncRParams] ')'
        //          | UnaryOp UnaryExp

        if (curToken.kind == Kind.IDENFR) {
            if (lookAhead(OneStep) == Kind.LPARENT) {
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                if (curToken.kind == Kind.RPARENT) {
                    moveForward();
                } else {
                    FuncRParamsHandle();
                    if (curToken.kind != Kind.RPARENT) {
                        handleError();
                    }
                    moveForward();
                }
            } else {
                PrimaryExpHandle();
            }

        } else if (curToken.kind == Kind.LPARENT || curToken.kind == Kind.INTCON) {
            PrimaryExpHandle();
        } else {
            UnaryOpHandle();
            UnaryExpHandle();
        }

        wrapNonterminal(Nonterminal.UnaryExp);
    }

    public void UnaryOpHandle() {
        // UnaryOp → '+' | '−' | '!'
        // '!' Only in CondExp

        if (curToken.kind == Kind.PLUS || curToken.kind == Kind.MINU) {
            moveForward();
        } else if (curToken.kind == Kind.NOT && isCond){
            moveForward();
        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.UnaryOp);
    }

    public void PrimaryExpHandle() {
        // PrimaryExp -> '(' Exp ')'
        //              | LVal
        //              | Number

        if (curToken.kind == Kind.LPARENT) {
            moveForward();
            ExpHandle();

            if (curToken.kind != Kind.RPARENT) {
                handleError();
            }
            moveForward();
        } else if (curToken.kind == Kind.INTCON) {
            NumberHandle();
        } else if (curToken.kind == Kind.IDENFR) {
            LValHandle();
        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.PrimaryExp);
    }

    public void NumberHandle() {
        if (curToken.kind != Kind.INTCON) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.Number);
    }

    public void LValHandle() {
        // LVal -> Ident {'[' Exp ']'}

        if (curToken.kind == Kind.IDENFR) {
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                moveForward();
                ExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();
                if (curToken.kind == Kind.LBRACK) {
                    moveForward();
                    ExpHandle();

                    if (curToken.kind != Kind.RBRACK) {
                        handleError();
                    }
                    moveForward();
                }
            }
        }

        wrapNonterminal(Nonterminal.LVal);
    }

    public void ConstExpHandle() {
        // ConstExp → AddExp

        AddExpHandle();

        wrapNonterminal(Nonterminal.ConstExp);
    }

    public void ConstInitValHandle() {
        // ConstInitVal → ConstExp
        //             | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'

        if (curToken.kind == Kind.LBRACE) {
            moveForward();

            if (curToken.kind == Kind.RBRACE) {
                moveForward();
            } else {
                ConstInitValHandle();
                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    ConstInitValHandle();
                }

                if (curToken.kind != Kind.RBRACE) {
                    handleError();
                }
                moveForward();
            }
        } else {
            ConstExpHandle();
        }

        wrapNonterminal(Nonterminal.ConstInitVal);
    }
}
