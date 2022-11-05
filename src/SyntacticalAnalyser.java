import CodeGen.PCode;
import CodeGen.PCodeKind;

import java.util.ArrayList;
import java.util.HashMap;
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

    public int scope = -1;
    public int scopeID = -1;
    public static ArrayList<PCode> PCodes = new ArrayList<>();
    public HashMap<Integer, IdentifierMap> mapIdentifierGroup = new HashMap<>();
    public HashMap<String, Function> mapFunction = new HashMap<>();

    public void addScope() {
        scopeID += 1;
        scope += 1;
        mapIdentifierGroup.put(scope, new IdentifierMap());
    }

    public void removeScope() {
        mapIdentifierGroup.remove(scope);
        scope -= 1;
    }

    public void addIdentifierToScope(Token token, String kind, int kindInt, int scopeID) {
        mapIdentifierGroup.get(scope).put(kind, kindInt, token, scopeID);
    }

    public Identifier getIdentifier(Token token) {
        Identifier identifier = null;
        for (IdentifierMap identifierMap : mapIdentifierGroup.values()) {
            if (identifierMap.has(token)) {
                identifier = identifierMap.get(token);
            }
        }
        return identifier;
    }

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

        addScope();

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

        removeScope();

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

        // CodeGen
        Token identifier = curToken;
        int kindInt = 0;
        PCodes.add(new PCode(PCodeKind.VAR, scopeID + "-" + curToken.content));
        // CodeGen

        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            kindInt += 1;
            // CodeGen

            moveForward();

            ConstExpHandle();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                //CodeGen//
                kindInt += 1;
                //CodeGen//

                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();
            }
        }

        // CodeGen
        if (kindInt > 0) {
            PCodes.add(new PCode(PCodeKind.DIMVAR, scopeID + "-" + identifier.content, kindInt));
        }
        addIdentifierToScope(identifier, "const", kindInt, scopeID);
        // CodeGen


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

        // CodeGen
        Token identifier = curToken;
        PCodes.add(new PCode(PCodeKind.VAR, scopeID + "-" + identifier.content));

        int kindInt = 0;
        // CodeGen

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            kindInt += 1;
            // CodeGen

            moveForward();
            ConstExpHandle();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                // CodeGen
                kindInt += 1;
                // CodeGen

                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();

            }
        }

        // CodeGen
        if (kindInt > 0) {
            PCodes.add(new PCode(PCodeKind.DIMVAR, scopeID + "-" + identifier.content, kindInt));
        }
        addIdentifierToScope(identifier, "var", kindInt, scopeID);
        // CodeGen

        if (curToken.kind == Kind.ASSIGN) {
            moveForward();
            InitValHandle();
        } else {
            // CodeGen
            PCodes.add(new PCode(PCodeKind.PLACEHOLDER, scopeID + "-" + identifier.content, kindInt));
            // Codegen
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

        // CodeGen
        Function function = new Function("main", "int", new ArrayList<>());
        mapFunction.put("main", function);

        PCodes.add(new PCode(PCodeKind.MAIN, "main"));
        // CodeGen

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

        BlockHandle(false);

        wrapNonterminal(Nonterminal.MainFuncDef);

        // CodeGen
        PCodes.add(new PCode(PCodeKind.EXIT));
        // CodeGen
    }

    public void FuncDefHandle() {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block

        // CodeGen
        addScope();

        String rType = FuncTypeHandle();
        String funcName = curToken.content;

        PCode code = new PCode(PCodeKind.FUNC, funcName);
        Function function = new Function(funcName, rType);
        PCodes.add(code);

        // CodeGen

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

            // CodeGen
            function.params = new ArrayList<>();
            // CodeGen

        } else {

            // CodeGen
            function.params = FuncFParamsHandle();
            // CodeGen

            if (curToken.kind != Kind.RPARENT) {
                handleError();
            }
            moveForward();
        }
        BlockHandle(true);

        wrapNonterminal(Nonterminal.FuncDef);

        // CodeGen
        mapFunction.put(function.name, function);
        code.val2 = function.params.size();
        PCodes.add(new PCode(PCodeKind.RET, 0));
        PCodes.add(new PCode(PCodeKind.ENDFUNC));

        removeScope();
        // CodeGen

    }

    public String FuncTypeHandle() {
        // FuncType -> 'void' | 'int'
        String rType;
        if (!(curToken.kind == Kind.VOIDTK || curToken.kind == Kind.INTTK)) {
            rType = "";
            handleError();
        } else if (curToken.kind == Kind.VOIDTK) {
            rType = "void";
        } else {
            rType = "int";
        }

        moveForward();

        wrapNonterminal(Nonterminal.FuncType);

        return rType;
    }

    public int FuncFParamHandle() {
        // FuncFParam -> BType Ident ['['']' {'[' ConstExp ']' }]

        BTypeHandle();

        // CodeGen
        int paramType = 0;
        Token identifier = curToken;
        // CodeGen

        if (curToken.kind != Kind.IDENFR) {
            handleError();
        }
        moveForward();

        if (curToken.kind == Kind.LBRACK) {
            // CodeGen
            paramType += 1;
            // CodeGen

            moveForward();

            if (curToken.kind != Kind.RBRACK) {
                handleError();
            }
            moveForward();

            if (curToken.kind == Kind.LBRACK) {
                // CodeGen
                paramType += 1;
                // CodeGen

                moveForward();
                ConstExpHandle();

                if (curToken.kind != Kind.RBRACK) {
                    handleError();
                }
                moveForward();
            }
        }

        wrapNonterminal(Nonterminal.FuncFParam);

        // CodeGen
        PCodes.add(new PCode(PCodeKind.PARA, scopeID + "-" + identifier.content, paramType));
        addIdentifierToScope(identifier, "para", paramType, scopeID);
        return paramType;
    }

    public ArrayList<Integer> FuncFParamsHandle() {
        // FuncFParams -> FuncFParam {',' FuncFParam}

        // CodeGen
        ArrayList<Integer> params = new ArrayList<>();
        params.add(FuncFParamHandle());
        // CodeGen

        while (curToken.kind == Kind.COMMA) {
            moveForward();

            // CodeGen
            params.add(FuncFParamHandle());
            // CodeGen
        }

        wrapNonterminal(Nonterminal.FuncFParams);

        return params;
    }

    public void FuncRParamsHandle() {
        // FuncRParams → Exp { ',' Exp }

        // CodeGen
        int kind = ExpHandle();
        PCodes.add(new PCode(PCodeKind.RPARA, kind));
        // CodeGen

        while (curToken.kind == Kind.COMMA) {
            moveForward();

            // CodeGen
            kind = ExpHandle();
            PCodes.add(new PCode(PCodeKind.RPARA, kind));
            // CodeGen
        }

        wrapNonterminal(Nonterminal.FuncRParams);
    }
    public void BlockHandle(boolean inFunc) {
        // '{' { BlockItem } '}'

        // CodeGen
        if (!inFunc) {
            addScope();
        }
        //Code Gen //

        if (curToken.kind != Kind.LBRACE) {
            handleError();
        }
        moveForward();

        while (curToken.kind != Kind.RBRACE) {
            BlockItemHandle();
        }
        moveForward();

        wrapNonterminal(Nonterminal.Block);

        // CodeGen
        if (!inFunc) {
            removeScope();
        }
        //Code Gen //
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

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.RET, 0));
                    // CodeGen
                } else {
                    ExpHandle();

                    if (curToken.kind != Kind.SEMICN) {
                        handleError();
                    }
                    moveForward();

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.RET, 1));
                    // CodeGen
                }
                break;

            case PRINTFTK:
                int paramCount = 0;
                moveForward();

                if (curToken.kind != Kind.LPARENT) {
                    handleError();
                }
                moveForward();

                // CodeGen
                Token str = curToken;
                // Codegen

                if (curToken.kind != Kind.STRCON) {
                    handleError();
                }
                moveForward();


                while (curToken.kind == Kind.COMMA) {
                    moveForward();
                    ExpHandle();
                    paramCount += 1;
                }

                if (curToken.kind != Kind.RPARENT) {
                    handleError();
                }
                moveForward();

                if (curToken.kind != Kind.SEMICN) {
                    handleError();
                }
                moveForward();

                // CodeGen
                PCodes.add(new PCode(PCodeKind.PRINT, str.content, paramCount));
                // CodeGen

                break;

            case LBRACE:
                BlockHandle(false);
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
                    // CodeGen
                    Token identifier = curToken;

                    int kindInt = LValHandle();
                    PCodes.add(new PCode(PCodeKind.ADDRESS, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
                    // CodeGen

                    if (curToken.kind != Kind.ASSIGN) {
                        handleError();
                    }
                    moveForward();

                    if (curToken.kind == Kind.GETINTTK) {
                        moveForward();

                        // CodeGen
                        PCodes.add(new PCode(PCodeKind.GETINT));
                        // CodeGen

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

                    // CodeGen
                    PCodes.add(new PCode(PCodeKind.POP, getIdentifier(identifier).scope + "-" + identifier.content));
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

    public int ExpHandle() {
        // Exp → AddExp

        int kind = AddExpHandle();

        wrapNonterminal(Nonterminal.Exp);

        return kind;
    }

    public int AddExpHandle() {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        int kindInt = 0;

        kindInt = MulExpHandle();
        while (curToken.kind == Kind.PLUS || curToken.kind == Kind.MINU) {
            Kind kind = curToken.kind;

            wrapNonterminal(Nonterminal.AddExp);
            moveForward();
            kindInt = MulExpHandle();

            // CodeGen
            if (kind == Kind.PLUS) {
                PCodes.add(new PCode(PCodeKind.ADD));
            } else {
                PCodes.add(new PCode(PCodeKind.SUB));
            }
            // CodeGen
        }

        wrapNonterminal(Nonterminal.AddExp);

        return kindInt;
    }

    public int MulExpHandle() {
        // UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        int kindInt = 0;

        kindInt = UnaryExpHandle();
        while (curToken.kind == Kind.MULT || curToken.kind == Kind.DIV || curToken.kind == Kind.MOD) {
            Kind kind = curToken.kind;
            wrapNonterminal(Nonterminal.MulExp);
            moveForward();
            kindInt = UnaryExpHandle();

            // CodeGen
            if (kind == Kind.MULT) {
                PCodes.add(new PCode(PCodeKind.MUL));
            } else if (kind == Kind.DIV) {
                PCodes.add(new PCode(PCodeKind.DIV));
            } else {
                PCodes.add(new PCode(PCodeKind.MOD));
            }
            //CodeGen
        }

        wrapNonterminal(Nonterminal.MulExp);

        return kindInt;
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

    public int UnaryExpHandle() {
        // UnaryExp → PrimaryExp
        //          | Ident '(' [FuncRParams] ')'
        //          | UnaryOp UnaryExp

        int kindInt = 0;

        if (curToken.kind == Kind.IDENFR) {
            Token identifier = curToken;
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

                // CodeGen
                PCodes.add(new PCode(PCodeKind.CALL, identifier.content));
                if (mapFunction.containsKey(identifier.content) && mapFunction.get(identifier.content).rType.equals("void")) {
                    kindInt = -1;
                }
                // CodeGen

            } else {
                kindInt = PrimaryExpHandle();
            }

        } else if (curToken.kind == Kind.LPARENT || curToken.kind == Kind.INTCON) {
            kindInt = PrimaryExpHandle();
        } else {
            Kind kind = curToken.kind;
            UnaryOpHandle();
            UnaryExpHandle();

            // CodeGen
            if (kind == Kind.PLUS) {
                PCodes.add(new PCode(PCodeKind.POS));
            } else if (kind == Kind.MINU) {
                PCodes.add(new PCode(PCodeKind.NEG));
            } else {
                PCodes.add(new PCode(PCodeKind.NOT));
            }
            // CodeGen

        }

        wrapNonterminal(Nonterminal.UnaryExp);

        return kindInt;
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

    public int PrimaryExpHandle() {
        // PrimaryExp -> '(' Exp ')'
        //              | LVal
        //              | Number
        int kindInt = 0;
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

            // CodeGen
            Token identifier = curToken;
            kindInt = LValHandle();
            if (kindInt == 0) {
                PCodes.add(new PCode(PCodeKind.VALUE, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
            } else {
                PCodes.add(new PCode(PCodeKind.ADDRESS, getIdentifier(identifier).scope + "-" + identifier.content, kindInt));
            }
            // CodeGen

        } else {
            handleError();
        }

        wrapNonterminal(Nonterminal.PrimaryExp);

        return kindInt;
    }

    public void NumberHandle() {
        // CodeGen
        PCodes.add(new PCode(PCodeKind.PUSH, Integer.parseInt(curToken.content)));
        // CodeGen

        if (curToken.kind != Kind.INTCON) {
            handleError();
        }
        moveForward();

        wrapNonterminal(Nonterminal.Number);
    }

    public int LValHandle() {
        // LVal -> Ident {'[' Exp ']'}

        // CodeGen
        int kindInt = 0;
        Token identifier = curToken;
        PCodes.add(new PCode(PCodeKind.PUSH, getIdentifier(identifier).scope + "-" + identifier.content));
        // CodeGen

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
        return 0;
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
