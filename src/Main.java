//package analyzer;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    static final String UNKNOWN_FILE_TYPE = "Unknown file type";

    public static void main(String[] args) throws IOException {
        String fileName = args[0];
        byte[] pattern = args[1].getBytes();
        String fileExtension = args[2];
        boolean isPdf = false;

        try (InputStreamReader r = new FileReader(fileName)) {
            int b;
            int i = 0;
            while ((b = r.read()) != -1) {
                if (b == pattern[i]) {
                    i++;
                    if (i == pattern.length) {
                        isPdf = true;
                        break;
                    }
                } else {
                    i = 0;
                }
            }
        }
        if (isPdf) {
            System.out.println(fileExtension);
        } else {
            System.out.println(UNKNOWN_FILE_TYPE);
        }
    }
}
