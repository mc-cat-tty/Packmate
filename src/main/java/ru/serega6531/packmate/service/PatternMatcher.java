package ru.serega6531.packmate.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.codec.Hex;
import ru.serega6531.packmate.model.FoundPattern;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.enums.PatternSearchType;
import ru.serega6531.packmate.utils.Bytes;

import java.util.*;
import java.util.regex.Matcher;

public class PatternMatcher {

    private static final Map<String, java.util.regex.Pattern> compiledPatterns = new HashMap<>();

    private final byte[] contentBytes;
    private final String content;
    private final List<Pattern> patterns;

    private final Set<FoundPattern> result = new HashSet<>();

    public PatternMatcher(byte[] contentBytes, List<Pattern> patterns) {
        this.contentBytes = contentBytes;
        this.content = new String(contentBytes);
        this.patterns = patterns;
    }

    public Set<FoundPattern> findMatches() {
        patterns.forEach(this::match);
        return result;
    }

    private void match(Pattern pattern) {
        if (pattern.getSearchType() == PatternSearchType.REGEX) {
            final var regex = compilePattern(pattern);
            final Matcher matcher = regex.matcher(content);
            int startPos = 0;

            while (matcher.find(startPos)) {
                addIfPossible(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(matcher.start())
                        .endPosition(matcher.end() - 1)
                        .build());
                startPos = matcher.end();
            }
        } else if (pattern.getSearchType() == PatternSearchType.SUBSTRING) {
            int startSearch = 0;
            final String value = pattern.getValue();

            while (true) {
                int start = StringUtils.indexOfIgnoreCase(content, value, startSearch);

                if (start == -1) {
                    return;
                }

                int end = start + value.length() - 1;
                addIfPossible(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(start)
                        .endPosition(end)
                        .build());

                startSearch = end + 1;
            }
        } else {  // SUBBYTES
            int startSearch = 0;
            final byte[] value = Hex.decode(pattern.getValue());

            while (true) {
                int start = Bytes.indexOf(contentBytes, value, startSearch, contentBytes.length);

                if (start == -1) {
                    return;
                }

                int end = start + value.length - 1;
                addIfPossible(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(start)
                        .endPosition(end)
                        .build());

                startSearch = end + 1;
            }
        }
    }

    private void addIfPossible(FoundPattern found) {
        if (result.stream().noneMatch(match -> between(match.getStartPosition(), match.getEndPosition(), found.getStartPosition()) ||
                between(match.getStartPosition(), match.getEndPosition(), found.getEndPosition()))) {
            result.add(found);
        }
    }

    private boolean between(int a, int b, int x) {
        return a <= x && x <= b;
    }

    static java.util.regex.Pattern compilePattern(Pattern pattern) {
        return compiledPatterns.computeIfAbsent(pattern.getValue(), java.util.regex.Pattern::compile);
    }

}
