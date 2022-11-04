import java.util.HashMap;

public class IdentifierMap {
    public HashMap<String, Identifier> map;

    public IdentifierMap() {
        map = new HashMap<>();
    }

    public void put(String kind, int kindInt, Token token, int scope) {
        map.put(token.content, new Identifier(kind, kindInt, token, scope));
    }

    public boolean has(Token token) {
        return map.containsKey(token.content);
    }

    public Identifier get(Token token) {
        return map.get(token.content);
    }

    public boolean isConst(Token token) {
        return map.get(token.content).kind.equals("const");
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
