package analyzer;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    static final String UNKNOWN_FILE_TYPE = "Unknown file type";
    static final String KMP = "--KMP";
    static final int POOL_CAPACITY = 10;
    static final String DELIMITER = ";";

    interface SearchInFile {
        boolean search(String pattern, String fileName) throws IOException;
    }

    static class NaiveSearchInFile implements SearchInFile {
        final int BUFFER_SIZE = 128;
        @Override
        public boolean search(String pattern, String fileName) throws IOException {
            byte[] patternArr = pattern.getBytes();
            boolean isPdf = false;

            try (InputStreamReader r = new FileReader(fileName)) {
                int b;
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

    public static class SearchApplier implements Callable<String>{
        SearchInFile algorithm;
        File file;
        File extensions;

        SearchApplier(SearchInFile algorithm, File file, File extensions) {
            this.algorithm = algorithm;
            this.file = file;
            this.extensions = extensions;
        }

        @Override
        public String call() {
            int extPriority = -1;
            String fileExtension = "";
            try {
                try (BufferedReader reader = new BufferedReader(new FileReader(extensions))) {
                    String line;
                    while (Objects.nonNull(line = reader.readLine())) {
                        String[] extParams = line.split(DELIMITER);
                        int curExtPriority = Integer.parseInt(extParams[0]);
                        String curPattern = extParams[1].replaceAll("\"", "");
                        String curExtension = extParams[2].replaceAll("\"", "");
                        if (algorithm.search(curPattern, file.getAbsolutePath())) {
                            if (extPriority < curExtPriority) {
                                extPriority = curExtPriority;
                                fileExtension = curExtension;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (extPriority == -1) {
                return file.getName() + ": " + UNKNOWN_FILE_TYPE;
            } else {
                return file.getName() + ": " + fileExtension;
            }
        }
    }
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String rootDirName = args[0];
        String patternsFileName = args[1];

        SearchInFile searchAlgo = new KMPSearchInFile();
        ExecutorService executor = Executors.newFixedThreadPool(POOL_CAPACITY);
        List<Callable<String>> threads = new ArrayList<>();
        File root = new File(rootDirName);
        File extensions = new File(patternsFileName);
        Deque<File> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
            File cur = deque.poll();
            File[] children = cur.listFiles();
            if (children == null) {
                threads.add(new SearchApplier(searchAlgo, cur, extensions));
            } else {
                for (File child : children) {
                    deque.add(child);
                }
            }
        }
        List<Future<String>> lst = executor.invokeAll(threads);
        for (Future<String> answer : lst) {
            System.out.println(answer.get());
        }
        executor.shutdown();
        executor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
    }
}
