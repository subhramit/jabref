package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSLFormatUtilsTest {
    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();
    private final BibEntry testEntry = TestEntry.getTestEntry();
    private final BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();

    @ParameterizedTest
    @MethodSource("rawHTMLProvider")
    void ooHTMLTransformFromRawHTMLTest(String input, String expected) {
        String actual = CSLFormatUtils.transformHTML(input);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> rawHTMLProvider() {
        return Stream.of(
                // First three are test cases for unescaping HTML entities

                // Ampersand (&amp entity)
                Arguments.of(
                        "Smith &amp; Jones",
                        "Smith & Jones"
                ),

                // Non-breaking space (&nbsp; entity)
                Arguments.of(
                        "Text with&nbsp;non-breaking&nbsp;spaces",
                        "Text with non-breaking spaces"
                ),

                // Bold formatting, less than, greater than symbols (&lt, &gt entities)
                Arguments.of(
                        "&lt;b&gt;Bold Text&lt;/b&gt;",
                        "<b>Bold Text</b>"
                ),

                // Handling margins
                Arguments.of(
                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">Citation text</div>",
                        "[1] Citation text"
                ),

                // Removing unsupported div tags
                Arguments.of(
                        "<div style=\"text-align:left;\">Aligned text</div>",
                        "Aligned text"
                ),

                // Removing unsupported links
                Arguments.of(
                        "Text with <a href=\"http://example.com\">link</a>",
                        "Text with link"
                ),

                // Replacing span tags with inline styles for bold
                Arguments.of(
                        "Text with <span style=\"font-weight:bold;\">bold</span>",
                        "Text with <b>bold</b>"
                ),

                // Replacing span tags with inline styles for italic
                Arguments.of(
                        "Text with <span style=\"font-style:italic;\">italic</span>",
                        "Text with <i>italic</i>"
                ),

                // Replacing span tags with inline styles for underline
                Arguments.of(
                        "Text with <span style=\"text-decoration:underline;\">underline</span>",
                        "Text with <u>underline</u>"
                ),

                // Replacing span tags with inline styles for small-caps
                Arguments.of(
                        "Text with <span style=\"font-variant:small-caps;\">small caps</span>",
                        "Text with <smallcaps>small caps</smallcaps>"
                ),

                // Test case for cleaning up remaining span tags
                Arguments.of(
                        "Text with <span>unnecessary span</span>",
                        "Text with unnecessary span"
                ),

                // Test case combining multiple transformations
                Arguments.of(
                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\"><span style=\"font-weight:bold;\">Author</span>, &quot;Title,&quot; <span style=\"font-style:italic;\">Journal</span>, vol. 1, no. 1, pp. 1-10, 2023.</div>",
                        "[1] <b>Author</b>, \"Title,\" <i>Journal</i>, vol. 1, no. 1, pp. 1-10, 2023."
                ),

                // Comprehensive test
                Arguments.of(
                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">" +
                                "<span style=\"font-weight:bold;\">Smith&nbsp;&amp;&nbsp;Jones</span>, " +
                                "&quot;<span style=\"font-style:italic;\">Analysis of &lt;code&gt; in HTML</span>,&quot; " +
                                "<span style=\"font-variant:small-caps;\">Journal of Web&nbsp;Development</span>, " +
                                "vol. 1, no. 1, pp. 1-10, 2023. " +
                                "<a href=\"https://doi.org/10.1000/example\">https://doi.org/10.1000/example</a></div>",

                        "[1] <b>Smith & Jones</b>, " +
                                "\"<i>Analysis of <code> in HTML</i>,\" " +
                                "<smallcaps>Journal of Web Development</smallcaps>, " +
                                "vol. 1, no. 1, pp. 1-10, 2023. " +
                                "https://doi.org/10.1000/example"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("rawCitationProvider")
    void ooHTMLTransformFromRawCitationTest(CitationStyle style, String expected) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), CitationStyleOutputFormat.HTML, context, bibEntryTypesManager).getFirst();
        String actual = CSLFormatUtils.transformHTML(citation);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> rawCitationProvider() {
        return Stream.of(
                // Type:"[1]"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    [1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.\n" +
                                "  \n"
                ),

                // Type:"<BOLD>1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    <b>1</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.\n" +
                                "  \n"
                ),

                // Type: "1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "(1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    (1) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).\n" +
                                "  \n"
                ),

                // Type: "<SUPERSCRIPT>1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().orElse(null),
                        "  <sup>1</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).\n"
                )
        );
    }

    /** Tests for American Psychological Association (APA) Style citations
     * Non-numeric style
     * Tests brackets "()", italics, slashes "/"
     */

    /** Tests for "Springer - Lecture Notes in Computer Science" Style citations
     * Numeric style
     * Tests brackets "()", italics, slashes "/"
     */

    // Tests for "De Montfort University - Harvard" Style citations

    // Tests for "Modern Language Association 7th edition (underline)" Style citations

    // Tests for "American Chemical Society" Style citations


    /**
     * Test for modifying the number (index) of a numeric citation.
     * The numeric index should change to the provided "current number".
     * The rest of the citation should stay as it is (other numbers in the body shouldn't be affected).
     * <p>
     * <b>Precondition 1:</b> This test assumes that Citation Style Generator works correctly.<br>
     * <b>Precondition 2:</b> This test assumes that transformHTML works correctly.<br>
     * <b>Precondition 3:</b> Only run this test on numeric Citation Styles.</p>
     *
     * @param style the numeric style to test updation on
     */
    @ParameterizedTest
    @MethodSource("numericCitationProvider")
    void updateSingleCitationTest(CitationStyle style, String expectedCitation) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), CitationStyleOutputFormat.HTML, context, bibEntryTypesManager).getFirst();
        String transformedCitation = CSLFormatUtils.transformHTML(citation);
        String actual = CSLFormatUtils.updateSingleCitation(transformedCitation, 3);
        assertEquals(expectedCitation, actual);
    }

    static Stream<Arguments> numericCitationProvider() {
        return Stream.of(
                // Type:"[1]"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    [3] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    3. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.\n" +
                                "  \n"
                ),

                // Type:"<BOLD>1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    <b>3</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.\n" +
                                "  \n"
                ),

                // Type: "1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    3 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "(1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    (3) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Type: "1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    3) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).\n" +
                                "  \n"
                ),

                // Type: "<SUPERSCRIPT>1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().orElse(null),
                        "  <sup>3</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).\n"
                )
        );
    }
}
