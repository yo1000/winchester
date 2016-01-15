package com.yo1000.winchester.analyzer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by yoichi.kikuchi on 2016/01/14.
 */
public class CyclomaticComplexity implements Analyzer {
    protected static final List<String> COMPLEXITY_TOKENS = Arrays.asList(
            "if", "else", "switch", "case", "default", "for", "while","do", "continue", "break",
            "catch", "finally", "throw", "throws", "return", "&&", "||", "?", ":"
    );

    @Override
    public Map<String, Integer> analyze(Path directory, List<String> extensions) throws IOException {
        Map<String, Integer> dependencies = new TreeMap<>();

        Files.list(directory).forEach(path -> {
            File file = path.toFile();

            if (file.isFile() && extensions.stream().anyMatch(ext -> file.getName().toLowerCase().endsWith(ext))) {
                try {
                    dependencies.put(path.toString(), this.analyze(new FileInputStream(path.toFile())));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }

            if (file.isDirectory()) {
                try {
                    dependencies.putAll(this.analyze(path, extensions));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }
        });

        return dependencies;
    }

    protected int analyze(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines()
                    .map(line -> Collections.list(new StringTokenizer(line)).stream()
                        .filter(token -> COMPLEXITY_TOKENS.contains(token))
                        .count())
                    .reduce((totalTokens, lineTokens) -> totalTokens + lineTokens)
                    .get().intValue();
        }
    }
}
