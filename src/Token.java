public class Token {
    public String content;
    public Kind kind;
    public int line;

    public Token(String identifier, int lineNum) {
        this.kind = MapToken2Kind.kind(identifier);
        this.content = identifier;
        this.line = lineNum;
    }

    public Token(char identifier, int lineNum) {
        this.kind = MapToken2Kind.kind(String.valueOf(identifier));
        this.content = String.valueOf(identifier);
        this.line = lineNum;
    }

    public Token(Kind kind, String content, int lineNum) {
        this.kind = kind;
        this.content = content;
        this.line = lineNum;
    }

    @Override
    public String toString() {
        return kind + " " + content;
    }

    public boolean kindMatch(Kind kind) {
        return this.kind == kind ;
    }

    public boolean kindStmt() {
        return this.kind == Kind.IDENFR
                || kind == Kind.LBRACE
                || kind == Kind.IFTK
                || kind == Kind.ELSETK
                || kind == Kind.WHILETK
                || kind == Kind.BREAKTK
                || kind == Kind.CONTINUETK
                || kind == Kind.RETURNTK
                || kind == Kind.PRINTFTK
                || kind == Kind.SEMICN
                || kindExp();
    }

    public boolean kindExp() {
        return kind == Kind.LPARENT
                || kind == Kind.IDENFR
                || kind == Kind.INTCON
                || kind == Kind.NOT
                || kind == Kind.PLUS
                || kind == Kind.MINU;
    }

    public boolean kindUnary() {
        return kind == Kind.PLUS
                || kind == Kind.MINU
                || kind == Kind.NOT;
    }
}