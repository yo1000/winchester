package com.yo1000.winchester;

import com.yo1000.winchester.analyzer.Analyzer;
import com.yo1000.winchester.analyzer.CyclomaticComplexity;
import com.yo1000.winchester.analyzer.SyntheticComplexity;
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
    protected static final String SYNTHETIC_COMPLEXITY = "synthetic-complexity";

    public static void main(String[] args) throws IOException {
        final String analyzer = args.length >= 1 ? args[0] : null;
        final String directoryPath = args.length >= 2 ? args[1] : null;
        final String packagePattern = args.length >= 3 ? args[2] : null;

        final Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>() {
            {
                this.put(CYCLOMATIC_COMPLEXITY, new CyclomaticComplexity());
                this.put(REFERENCED_DEPENDENCY, new ReferencedDependency());
                this.put(SYNTHETIC_COMPLEXITY, new SyntheticComplexity(packagePattern != null ? packagePattern : ".*"));
            }
        };

        final Map<String, List<String>> extensionsMap = new HashMap<String, List<String>>() {
            {
                this.put(CYCLOMATIC_COMPLEXITY, Arrays.asList(".java", ".groovy"));
                this.put(REFERENCED_DEPENDENCY, Arrays.asList(".class"));
                this.put(SYNTHETIC_COMPLEXITY, Arrays.asList(".java"));
            }
        };

        analyzerMap.get(analyzer).analyze(Paths.get(directoryPath), extensionsMap.get(analyzer))
                .entrySet().stream()
                .sorted((x, y) -> (y.getValue() - x.getValue()))
                .forEach(entry -> System.out.printf("%s %d\n", entry.getKey(), entry.getValue()));
    }
}
