package com.yo1000.winchester.analyzer;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File f = file.toFile();

                if (extensions.stream().anyMatch(ext -> f.getName().toLowerCase().endsWith(ext))) {
                    try (FileInputStream stream = new FileInputStream(f)) {
                        dependencies.put(file.toString(), CyclomaticComplexity.this.analyze(stream));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                return super.visitFile(file, attrs);
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
