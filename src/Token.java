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

    public int formatParamCount() {
        int count = 0;
        for (int i = 0; i < content.length() - 1; i++) {
            if (content.charAt(i) == '%' && content.charAt(i + 1) == 'd') {
                count += 1;
            }
        }

        return count;
    }

    private boolean isLegalChar(char ch) {
        return ch == 32 ||
                ch == 33 ||
                (ch >= 40 && ch <= 126);
    }

    public boolean isIllegalFormatString() {
        for (int i = 1; i < content.length() - 1; i++) {
            char ch = content.charAt(i);
            if (!isLegalChar(ch)) {
                if (ch == '%' && content.charAt(i + 1) == 'd') {
                    continue;
                }
                return true;
            } else {
                if (ch == '\\' && content.charAt(i + 1) != 'n') {
                    return true;
                }
            }
        }
        return false;
    }
}
