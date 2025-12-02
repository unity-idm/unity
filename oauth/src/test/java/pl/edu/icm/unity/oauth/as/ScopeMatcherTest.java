package pl.edu.icm.unity.oauth.as;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScopeMatcherTest {
    static ActiveOAuthScopeDefinition nonPatternScope(String name) {
        return ActiveOAuthScopeDefinition.builder().withName(name).withPattern(false).build();
    }

    static ActiveOAuthScopeDefinition patternScope(String pattern) {
        return ActiveOAuthScopeDefinition.builder().withName(pattern).withPattern(true).build();
    }

    @Test
    void shouldReturnTrueForExactMatchNonPattern() {
        assertTrue(ScopeMatcher.match(nonPatternScope("read"), "read", false));
    }

    @Test
    void shouldReturnFalseForNonMatchingNonPattern() {
        assertFalse(ScopeMatcher.match(nonPatternScope("read"), "write", false));
    }

    @Test
    void shouldUseRegexForPatternScopesAndNotAllowRequestingPatternScope() {
        assertTrue(ScopeMatcher.match(patternScope("rea.*"), "read", false));
        assertFalse(ScopeMatcher.match(patternScope("rea.*"), "write", false));
    }

    @Test
    void shouldReturnFalseForInvalidRegexPattern() {
        assertFalse(ScopeMatcher.match(patternScope("[invalid"), "read", false));
    }

    @Test
    void shouldUseIsSubsetOfPatternScopeWhenAllowed() {
        // "rea.*" is a superset of "read"
        assertTrue(ScopeMatcher.match(patternScope("rea.*"), "read", true));
        // "read" is not a superset of "rea.*"
        assertFalse(ScopeMatcher.match(patternScope("read"), "rea.*", true));
    }

    @Test
    void shouldReturnTrueIfSubset() {
        assertTrue(ScopeMatcher.isSubsetOfPatternScope("read", "rea.*"));
    }

    @Test
    void shouldReturnFalseIfNotSubset() {
        assertFalse(ScopeMatcher.isSubsetOfPatternScope("write", "rea.*"));
    }

    @Test
    void shouldHandleEmptyStrings() {
        assertFalse(ScopeMatcher.match(nonPatternScope(""), "read", false));
        assertTrue(ScopeMatcher.match(nonPatternScope(""), "", false));
        assertTrue(ScopeMatcher.match(patternScope(".*"), "", false));
    }
}
