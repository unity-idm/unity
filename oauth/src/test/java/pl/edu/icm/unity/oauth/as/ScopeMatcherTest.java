package pl.edu.icm.unity.oauth.as;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScopeMatcherTest {
    static ActiveOAuthScopeDefinition nonWildcardScope(String name) {
        return ActiveOAuthScopeDefinition.builder().withName(name).withWildcard(false).build();
    }
    static ActiveOAuthScopeDefinition wildcardScope(String pattern) {
        return ActiveOAuthScopeDefinition.builder().withName(pattern).withWildcard(true).build();
    }

    @Test
    void shouldReturnTrueForExactMatchNonWildcard() {
        assertTrue(ScopeMatcher.match(nonWildcardScope("read"), "read", false));
    }

    @Test
    void shouldReturnFalseForNonMatchingNonWildcard() {
        assertFalse(ScopeMatcher.match(nonWildcardScope("read"), "write", false));
    }

    @Test
    void shouldUseRegexForWildcardAndNotAllowRequestingWildcard() {
        assertTrue(ScopeMatcher.match(wildcardScope("rea.*"), "read", false));
        assertFalse(ScopeMatcher.match(wildcardScope("rea.*"), "write", false));
    }

    @Test
    void shouldReturnFalseForInvalidRegex() {
        assertFalse(ScopeMatcher.match(wildcardScope("[invalid"), "read", false));
    }

    @Test
    void shouldUseIsSubsetOfWildcardScopeWhenAllowed() {
        // "rea.*" is a superset of "read"
        assertTrue(ScopeMatcher.match(wildcardScope("rea.*"), "read", true));
        // "read" is not a superset of "rea.*"
        assertFalse(ScopeMatcher.match(wildcardScope("read"), "rea.*", true));
    }

    @Test
    void isSubsetOfWildcardScope_shouldReturnTrueIfSubset() {
        assertTrue(ScopeMatcher.isSubsetOfWildcardScope("read", "rea.*"));
    }

    @Test
    void isSubsetOfWildcardScope_shouldReturnFalseIfNotSubset() {
        assertFalse(ScopeMatcher.isSubsetOfWildcardScope("write", "rea.*"));
    }

    @Test
    void match_shouldHandleEmptyStrings() {
        assertFalse(ScopeMatcher.match(nonWildcardScope(""), "read", false));
        assertTrue(ScopeMatcher.match(nonWildcardScope(""), "", false));
        assertTrue(ScopeMatcher.match(wildcardScope(".*"), "", false));
    }
}
