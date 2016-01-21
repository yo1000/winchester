package com.yo1000.winchester;

import com.yo1000.winchester.analyzer.Analyzer;
import com.yo1000.winchester.analyzer.CyclomaticComplexity;
import com.yo1000.winchester.analyzer.ReferencedDependency;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yoichi.kikuchi on 2016/01/15.
 */
public class Application {
    protected static final String CYCLOMATIC_COMPLEXITY = "complexity";
    protected static final String REFERENCED_DEPENDENCY = "dependency";

    protected static final Map<String, Analyzer> ANALYZER_MAP = new HashMap<String, Analyzer>() {
        {
            this.put(CYCLOMATIC_COMPLEXITY, new CyclomaticComplexity());
            this.put(REFERENCED_DEPENDENCY, new ReferencedDependency());
        }
    };

    protected static final Map<String, List<String>> EXTENSIONS_MAP = new HashMap<String, List<String>>() {
        {
            this.put(CYCLOMATIC_COMPLEXITY, Arrays.asList(".java", ".groovy"));
            this.put(REFERENCED_DEPENDENCY, Arrays.asList(".class"));
        }
    };

    public static void main(String[] args) throws IOException {
        String analyzer = args.length >= 1 ? args[0] : null;
        String directoryPath = args.length >= 2 ? args[1] : null;

        ANALYZER_MAP.get(analyzer).analyze(Paths.get(directoryPath), EXTENSIONS_MAP.get(analyzer))
                .entrySet().stream()
                .sorted((x, y) -> (int) (y.getValue() - x.getValue()))
                .forEach(entry -> System.out.printf("%s %d\n", entry.getKey(), entry.getValue()));
    }
}
