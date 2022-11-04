import CodeGen.VM;

import java.util.Scanner;

public class Compiler {
    public static String input = "testfile.txt";
    public static String output = "output.txt";
//    public static String output = "pcoderesult.txt";

    public static void main(String[] args)  {
        try {
            FileHandler fileHandler = new FileHandler();
            Scanner scanner = new Scanner(System.in);
            LexicalAnalyser lexicalAnalyser = new LexicalAnalyser();
            lexicalAnalyser.analyse();

            SyntacticalAnalyser syntacticalAnalyser = new SyntacticalAnalyser(LexicalAnalyser.tokens);

            fileHandler.PrintLines(SyntacticalAnalyser.syntactic);

            VM vm = new VM(syntacticalAnalyser.PCodes, FileHandler.writer, scanner);
            vm.run();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
