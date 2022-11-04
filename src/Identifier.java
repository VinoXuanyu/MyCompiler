public class Identifier {
    public String kind;
    public int intType;
    public String content;
    public int scope;

    public Identifier(String kind, int kindInt, Token token, int scope) {
        this.kind = kind;
        this.intType = kindInt;
        this.content = token.content;
        this.scope = scope;
    }
    
    @Override
    public String toString() {
        return content;
    }
}
