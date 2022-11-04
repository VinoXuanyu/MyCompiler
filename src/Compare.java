import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compare {
    public static void Compare(String custom_input, String correct_input)
            throws IOException {

        try (BufferedReader custom = Files.newBufferedReader(Paths.get(custom_input));
             BufferedReader correct = Files.newBufferedReader(Paths.get(correct_input))) {
            long cur_line = 1;
            String custom_line = "", correct_line = "";
            while ((custom_line = custom.readLine()) != null && (correct_line = correct.readLine()) != null) {
                if (!custom_line.equals(correct_line)) {
                    System.out.println("Line: " + cur_line + " dont match");;
                    System.out.println("Correct: " + correct_line);
                    System.out.println("Custom: " + custom_line);
                    break;
                }
                cur_line++;
            }

            correct_line = correct.readLine();
            if (correct_line != null) {
                System.out.println("Num of Lines don't match, Custom stopped at: " + cur_line);
            }
            if (custom_line != null) {
                System.out.println("Custom has redundant lines, should be: " + cur_line);
            }

            System.out.println("Two files are identical");

        }

    }

    public static void main(String[] args) throws IOException {
        Compiler.main(new String[]{});
        Compare("output.txt", "correct.txt");
    }
}
