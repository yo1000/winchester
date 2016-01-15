package com.yo1000.winchester.analyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by yoichi.kikuchi on 2016/01/15.
 */
public interface Analyzer {
    Map<String, Integer> analyze(Path directory, List<String> extensions) throws IOException;
}
