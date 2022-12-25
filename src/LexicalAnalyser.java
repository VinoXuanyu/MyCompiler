import java.io.IOException;
import java.util.ArrayList;

public class LexicalAnalyser {
    public String codes = FileHandler.codes;
    public int line = 1;
    public int p = 0;
    public static ArrayList<Token> tokens = new ArrayList<>();

    public LexicalAnalyser() throws IOException {
        codes = FileHandler.codes;
        analyse();
    }

    public Character moveForward() {
        if (p < codes.length()) {
            char c = codes.charAt(p);
            if (c == '\n') {
                line++;
            }
            p++;
            return c;
        } else {
            return null;
        }
    }

    public void moveBackward() {
        p--;
        char c = codes.charAt(p);
        if (c == '\n') {
            line--;
        }
    }

    public void analyse() throws IOException {
        Character ch = null;
        while ((ch = moveForward()) != null) {
            if (ch == ' ' || ch == '\r' || ch == '\t') {
                continue;
            } else if (ch == '+' || ch == '-' || ch == '*' || ch == '%') {
                tokens.add(new Token(ch, line));
            } else if (ch == '/') {
                parseSlash();
            } else if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '{' || ch == '}') {
                tokens.add(new Token(ch, line));
            } else if (ch == '>' || ch == '<' || ch == '=' || ch == '!') {
                parseRelation(ch);
            } else if (ch == ',' || ch == ';') {
                tokens.add(new Token(ch, line));
            } else if (ch == '"') {
                parseQuote();
            } else if (ch == '&' || ch == '|') {
                parseLogic(ch);
            } else if (Character.isDigit(ch)) {
                parseDigit(ch);
            } else if (Character.isLetter(ch) || ch == '_') {
                parseLetter(ch);
            }
        }
    }

    public void parseSlash() {
        Character c = moveForward();
        if (c == '/') {
            do {
                c = moveForward();
                if (c == null || c == '\n') {
                    return;
                }
            } while (true);
        } else if (c == '*') {
            do {
                c = moveForward();
                if (c == null) {
                    return;
                }
                if (c == '*') {
                    c = moveForward();
                    if (c == '/') {
                        return;
                    } else {
                        moveBackward();
                    }
                }
            } while (true);
        } else {
            tokens.add(new Token("/", line));
            moveBackward();
        }
    }

    public void parseRelation(char c) {
        if (c == '=') {
            c = moveForward();
            if (c == '=') {
                tokens.add(new Token("==", line));
            } else {
                moveBackward();
                tokens.add(new Token("=", line));
                return;
            }
        } else if (c == '<') {
            c = moveForward();
            if (c == '=') {
                tokens.add(new Token("<=", line));
            } else {
                moveBackward();
                tokens.add(new Token("<", line));
            }
        } else if (c == '>') {
            c = moveForward();
            if (c == '=') {
                tokens.add(new Token(">=", line));
            } else {
                moveBackward();
                tokens.add(new Token(">", line));
            }
        } else {
            c = moveForward();
            if (c == '=') {
                tokens.add(new Token("!=", line));
            } else {
                moveBackward();
                tokens.add(new Token("!", line));
            }
        }
    }

    public void parseQuote() {
        Character c = null;
        StringBuilder builder = new StringBuilder("");
        boolean metSlash = false;
        while ((c = moveForward()) != null) {
            if (c == '"') {
                tokens.add(new Token(Kind.STRCON, "\"" + builder + "\"", line));
                return;
            } else {
                if (c == '\\') {
                    metSlash = true;
                } else {
                    if (metSlash && c == 'n') {
                        builder.append("\n");
                    } else {
                        builder.append(c);
                    }
                    metSlash = false;
                }
            }
        }

    }

    public void parseLogic(char pre) {
        Character c = null;
        if ((c = moveForward()) != null) {
            if (pre == '&') {
                if (c == '&') {
                    tokens.add(new Token("&&", line));
                } else {
                    moveBackward();
                    tokens.add(new Token("&", line));
                }
            } else {
                if (c == '|') {
                    tokens.add(new Token("||", line));
                } else {
                    moveBackward();
                    tokens.add(new Token("|", line));
                }
            }
        }
    }

    public void parseDigit(char pre) {
        StringBuilder builder = new StringBuilder("" + pre);
        Character c = null;
        while ((c = moveForward()) != null) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else {
                moveBackward();
                tokens.add(new Token(Kind.INTCON, builder.toString(), line));
                return;
            }
        }
    }

    public void parseLetter(char pre) {
        StringBuilder builder = new StringBuilder("" + pre);
        Character c = null;
        while ((c = moveForward()) != null) {
            if (Character.isLetter(c) || c == '_' || Character.isDigit(c)) {
                builder.append(c);
            } else {
                moveBackward();
                if (MapToken2Kind.map.containsKey(builder.toString())) {
                    tokens.add(new Token(builder.toString(), line));
                } else {
                    tokens.add(new Token(Kind.IDENFR, builder.toString(), line));
                }
                return;
            }
        }
    }
}
