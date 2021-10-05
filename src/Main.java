package analyzer;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    static final String UNKNOWN_FILE_TYPE = "Unknown file type";
    static final String KMP = "--KMP";

    interface SearchInFile {
        boolean search(String pattern, String fileName) throws IOException;
    }

    static class NaiveSearchInFile implements SearchInFile {
        final int BUFFER_SIZE = 128;
        @Override
        public boolean search(String pattern, String fileName) throws IOException {
            byte[] patternArr = pattern.getBytes();
            try (InputStreamReader r = new FileReader(fileName)) {
                char[] content = new char[BUFFER_SIZE];
                while (r.read(content) != -1) {
                    for (int i = 0; i < content.length - patternArr.length + 1; i++) {
                        boolean patternIsFound = true;

                        for (int j = 0; j < pattern.length(); j++) {
                            if (content[i + j] != patternArr[j]) {
                                patternIsFound = false;
                                break;
                            }
                        }

                        if (patternIsFound) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    static class KMPSearchInFile implements SearchInFile {
        final int BUFFER_SIZE = 128;
        @Override
        public boolean search(String pattern, String fileName) throws IOException {
            byte[] patternArr = pattern.getBytes();
            byte[] prefix = getPrefixFunction(patternArr);
            boolean isPdf = false;
            int j = 0;

            try (InputStreamReader r = new FileReader(fileName)) {
                char[] content = new char[BUFFER_SIZE];
                while (r.read(content) != -1) {
                    for (int i = 0; i < content.length; i++) {
                        while (j > 0 && content[i] != patternArr[j]) {
                            j = prefix[j - 1];
                        }
                        if (content[i] == patternArr[j]) {
                            j++;
                        }
                        if (j == patternArr.length) {
                            isPdf = true;
                            break;
                        }
                    }
                    if (isPdf) {
                        break;
                    }
                }
            }
            return isPdf;
        }

        private byte[] getPrefixFunction(byte[] pattern) {
            byte[] prefix = new byte[pattern.length];

            for (byte i = 1; i < pattern.length; i++) {
                byte j = prefix[i - 1];

                while (j > 0 && pattern[i] != pattern[j]) {
                    j = prefix[j - 1];
                }
                if (pattern[i] == pattern[j]) {
                    j++;
                }
                prefix[i] = j;
            }
            return prefix;
        }
    }

    static class SearchApplier {
        SearchInFile algorithm;

        public void setAlgorithm(SearchInFile algorithm) {
            this.algorithm = algorithm;
        }

        public boolean apply(String pattern, String fileName) throws IOException{
            return this.algorithm.search(pattern, fileName);
        }
    }

    public static void main(String[] args) throws IOException {
        String algorithm = args[0];
        String fileName = args[1];
        String pattern = args[2];
        String fileExtension = args[3];

        SearchInFile searchAlgo;
        if (KMP.equalsIgnoreCase(algorithm)) {
            searchAlgo = new KMPSearchInFile();
        } else {
            searchAlgo = new NaiveSearchInFile();
        }
        SearchApplier searchApplier = new SearchApplier();
        searchApplier.setAlgorithm(searchAlgo);
        long start = System.nanoTime();
        if (searchApplier.apply(pattern, fileName)) {
            System.out.println(fileExtension);
        } else {
            System.out.println(UNKNOWN_FILE_TYPE);
        }
        System.out.printf("It took %f.3 seconds", (System.nanoTime() - start) / Math.pow(10, 9));
    }
}
