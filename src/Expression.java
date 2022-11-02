import java.util.ArrayList;

public class Expression {
    public ArrayList<ArrayList<Token>> tokens;
    public ArrayList<Token> symbols;

    public Expression(ArrayList<ArrayList<Token>> tokens, ArrayList<Token> symbols) {
        this.tokens = tokens;
        this.symbols = symbols;
    }

    public ArrayList<ArrayList<Token>> getTokens() {
        return tokens;
    }

    public ArrayList<Token> getSymbols() {
        return symbols;
    }

}
