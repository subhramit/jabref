package org.jabref.logic.citationstyle;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationStyleTest {

    @Test
    void getDefault() throws Exception {
        assertNotNull(CitationStyle.getDefault());
    }

    @Test
    void defaultCitation() {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
        context.setMode(BibDatabaseMode.BIBLATEX);
        String citation = CitationStyleGenerator.generateCitation(List.of(TestEntry.getTestEntry()), CitationStyle.getDefault().getSource(), CitationStyleOutputFormat.HTML, context, new BibEntryTypesManager()).getFirst();

        // if the default citation style changes this has to be modified
        String expected = """
                  <div class="csl-entry">
                    <div class="csl-left-margin">[1]</div><div class="csl-right-inline">B. Smith, B. Jones, and J. Williams, &ldquo;Title of the test entry,&rdquo; <span style="font-style: italic">BibTeX Journal</span>, vol. 34, no. 3, pp. 45&ndash;67, Jul. 2016, doi: 10.1001/bla.blubb.</div>
                  </div>
                """;

        assertEquals(expected, citation);
    }

    @Test
    void discoverCitationStylesNotNull() throws Exception {
        List<CitationStyle> styleList = CitationStyle.discoverCitationStyles();
        assertNotNull(styleList);
    }

    @ParameterizedTest
    @MethodSource("citationStyleProvider")
    void testParseStyleInfo(String filename, String expectedTitle, boolean expectedNumeric) throws Exception {
        Optional<CitationStyle.StyleInfo> styleInfo = parseStyleInfoFromFile(filename);

        assertTrue(styleInfo.isPresent(), "Style info should be present for " + filename);
        assertEquals(expectedTitle, styleInfo.get().title(), "Title should match for " + filename);
        assertEquals(expectedNumeric, styleInfo.get().isNumericStyle(), "Numeric style should match for " + filename);
    }

    private static Stream<Arguments> citationStyleProvider() {
        return Stream.of(
                Arguments.of("/ieee.csl", "IEEE", true),
                Arguments.of("/apa.csl", "American Psychological Association 7th edition", false),
                Arguments.of("/harvard1.csl", "Harvard Reference format 1 (author-date)", false),
                Arguments.of("/vancouver.csl", "Vancouver", true),
                Arguments.of("/chicago-author-date.csl", "Chicago Manual of Style 17th edition (author-date)", false),
                Arguments.of("/nature.csl", "Nature", true)
        );
    }

    private Optional<CitationStyle.StyleInfo> parseStyleInfoFromFile(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(filename)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + filename);
            }
            String content = new String(is.readAllBytes());
            return CitationStyle.parseStyleInfo(filename, content);
        }
    }
}
