import java.io.IOException;

public class Compiler {
    public static String input = "testfile.txt";
    public static String output = "output.txt";

    public static void main(String[] args)  {
        try {
            FileHandler fileHandler = new FileHandler();

            LexicalAnalyser lexicalAnalyser = new LexicalAnalyser();
            lexicalAnalyser.analyse();

            SyntacticalAnalyser syntacticalAnalyser = new SyntacticalAnalyser(LexicalAnalyser.tokens);

            fileHandler.PrintLines(SyntacticalAnalyser.syntactic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
