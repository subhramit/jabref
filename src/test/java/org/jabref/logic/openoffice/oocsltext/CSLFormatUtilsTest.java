package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.openoffice.ootext.OOText;

import de.undercouch.citeproc.output.Citation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSLFormatUtilsTest {
    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();
    private static final CitationStyleOutputFormat OUTPUT_FORMAT = CitationStyleOutputFormat.HTML;
    private final BibEntry testEntry = TestEntry.getTestEntry();
    private final BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();

    /**
     * Test to check transformation of raw, unsupported HTML into OO-ready HTML.
     *
     * @param input the raw HTML
     */
    @ParameterizedTest
    @MethodSource("rawHTMLProvider")
    void ooHTMLTransformFromRawHTML(String input, String expected) {
        String actual = CSLFormatUtils.transformHTML(input);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> rawHTMLProvider() {
        return Stream.of(
                // First three are general test cases for unescaping HTML entities

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
    @MethodSource("rawHTMLProvider")
    void ooTextTest(String input, String expected) {
        String actual = CSLFormatUtils.transformHTML(input);
        OOText ooText = OOText.fromString(actual);
        assertEquals(OOText.fromString(expected), ooText);
        assertEquals(OOText.fromString(expected).toString(), ooText.toString());
        assertEquals(expected, ooText.toString());
    }

    /**
     * Test to check correct transformation of raw CSL bibliography generated by citeproc-java methods into OO-ready text.
     * <p>
     * <b>Precondition:</b> This test assumes that Citation Style Generator works properly.
     * </p>
     *
     * @param style the CSL style to test transformation on
     */
    @ParameterizedTest
    @MethodSource("rawCitationProvider")
    void ooHTMLTransformFromRawCitation(CitationStyle style, String expected) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), OUTPUT_FORMAT, context, bibEntryTypesManager).getFirst();
        String actual = CSLFormatUtils.transformHTML(citation);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> rawCitationProvider() {
        return Stream.of(

                // Non-numeric, parentheses, commas, full stops, slashes, hyphens, colons, italics
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().orElse(null),
                        "  Smith, B., Jones, B., & Williams, J. (2016). Title of the test entry. <i>BibTeX Journal</i>, <i>34</i>(3), 45–67. https://doi.org/10.1001/bla.blubb\n"
                ),

                // Numeric type "[1]", brackets, newlines
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    [1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Numeric type "1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith, B., Jones, B., Williams, J.: Title of the test entry. BibTeX Journal. 34, 45–67 (2016). https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().orElse(null),
                        "  Smith, Bill, Bob Jones, and Jeff Williams. 2016. “Title of the Test Entry.” Edited by Phil Taylor. <i>BibTeX Journal</i> 34 (3): 45–67. https://doi.org/10.1001/bla.blubb.\n"
                ),

                // Semicolons
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, editor. BibTeX Journal [Internet]. 2016 Jul;34(3):45–67. Available from: https://github.com/JabRef\n" +
                                "  \n"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith, B., Jones, B. & Williams, J. Title of the test entry. <i>BibTeX Journal</i> <b>34</b>, 45–67 (2016).\n" +
                                "  \n"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, ed. <i>BibTeX Journal</i>. 2016;34(3):45-67. doi:10.1001/bla.blubb\n" +
                                "  \n"
                ),

                // Small-caps
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().orElse(null),
                        "  <smallcaps>Smith</smallcaps>, <smallcaps>B.</smallcaps>, <smallcaps>Jones</smallcaps>, <smallcaps>B.</smallcaps>, <smallcaps>Williams</smallcaps>, <smallcaps>J.</smallcaps> (2016) Title of the test entry <smallcaps>Taylor</smallcaps>, <smallcaps>P.</smallcaps> (ed.). <i>BibTeX Journal</i>, 34(3), pp. 45–67.\n"
                ),

                // Underlines
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().orElse(null),
                        "  Smith, Bill, Bob Jones, and Jeff Williams. “Title of the test entry.” Ed. Phil Taylor. <u>BibTeX Journal</u> 34.3 (2016): 45–67. <https://github.com/JabRef>.\n"
                ),

                // Non-breaking spaces
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().orElse(null),
                        "  Smith, Bill, Bob Jones, & Jeff Williams, “Title of the test entry,” <i>BibTeX Journal</i>, 2016, vol. 34, no. 3, pp. 45–67.\n"
                ),

                // Numeric with a full stop - "1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.\n" +
                                "  \n"
                ),

                // Bold text, bold numeric with a full stop - "<BOLD>1."
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    <b>1</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.\n" +
                                "  \n"
                ),

                // Naked numeric - "1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Numeric in parentheses - "(1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    (1) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n"
                ),

                // Numeric with right parenthesis - "1)"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().orElse(null),
                        "  \n" +
                                "    1) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).\n" +
                                "  \n"
                ),

                // Numeric in superscript - "<SUPERSCRIPT>1"
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().orElse(null),
                        "  <sup>1</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).\n"
                )
        );
    }

    static Stream<Arguments> rawInTextCitationProvider() {
        return Stream.of(

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().orElse(null),
                        "(Smith et al., 2016)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().orElse(null),
                        "[1]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().orElse(null),
                        "[1]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().orElse(null),
                        "(Smith, Jones, and Williams 2016)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().orElse(null),
                        "(1)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().orElse(null),
                        "(Smith, Jones, Williams, 2016)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().orElse(null),
                        "(Smith, Jones, & Williams)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().orElse(null),
                        "Smith, B., B. Jones, and J. Williams, 2016."
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().orElse(null),
                        "[1]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().orElse(null),
                        "(<i>1</i>)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>"
                ),

                // Note: not sure if the right parenthesis outside the superscript is correct, but that's how citeproc-java generates it in raw form as well.
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1</sup>"
                )

                // Currently not supported by citeproc-java
//                Arguments.of(
//                        STYLE_LIST.stream().filter(e -> "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690".equals(e.getTitle())).findAny().orElse(null),
//                        "[Smit2016]"
//                )
        );
    }

    @ParameterizedTest
    @MethodSource("rawInTextCitationProvider")
    void ooRoundTrip(CitationStyle style, String expected) throws IOException {
        Citation citation = CitationStyleGenerator.generateInText(List.of(testEntry), style.getSource(), OUTPUT_FORMAT, context, bibEntryTypesManager);
        String inTextCitationText = citation.getText();
        String actual = CSLFormatUtils.transformHTML(inTextCitationText);
        OOText ooText = OOText.fromString(actual);
        assertEquals(OOText.fromString(expected), ooText);
    }

    @ParameterizedTest
    @MethodSource("rawInTextCitationProviderMultipleEntries")
    void ooHTMLTransformFromRawInTextCitationWithMultipleEntries(CitationStyle style, String expected) throws IOException {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria and Lee, David")
                .withField(StandardField.JOURNAL, "International Review of Physics")
                .withField(StandardField.NUMBER, "6")
                .withField(StandardField.PAGES, "789--810")
                .withField(StandardField.TITLE, "Quantum Entanglement in Superconductors")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.ISSUE, "3")
                .withField(StandardField.YEAR, "2021")
                .withCitationKey("Garcia_2021");

        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "101--120")
                .withField(StandardField.TITLE, "A Study on Machine Learning Algorithms")
                .withField(StandardField.VOLUME, "15")
                .withField(StandardField.ISSUE, "2")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Smith_2020");

        List<BibEntry> entries = List.of(entry1, entry2);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        context.setMode(BibDatabaseMode.BIBLATEX);
        Citation citation = CitationStyleGenerator.generateInText(entries, style.getSource(), OUTPUT_FORMAT, context, bibEntryTypesManager);
        String inTextCitationText = citation.getText();
        String actual = CSLFormatUtils.transformHTML(inTextCitationText);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> rawInTextCitationProviderMultipleEntries() {
        return Stream.of(
                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().orElse(null),
                        "(Garcia & Lee, 2021; Smith & Johnson, 2020)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().orElse(null),
                        "[1], [2]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().orElse(null),
                        "[1, 2]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().orElse(null),
                        "(Garcia and Lee 2021; Smith and Johnson 2020)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().orElse(null),
                        "(1,2)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().orElse(null),
                        "(Garcia, Lee, 2021; Smith, Johnson, 2020)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().orElse(null),
                        "(Garcia & Lee; Smith & Johnson)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().orElse(null),
                        "Garcia, M. and D. Lee, 2021 ; Smith, J. and E. Johnson, 2020."
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().orElse(null),
                        "[1, 2]"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().orElse(null),
                        "(<i>1,2</i>)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>)"
                ),

                Arguments.of(
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().orElse(null),
                        "<sup>1,2</sup>"
                )
        );
    }

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
    @MethodSource("rawNumericCitationProvider")
    void updateSingleNumericCitation(CitationStyle style, String expectedCitation) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), OUTPUT_FORMAT, context, bibEntryTypesManager).getFirst();
        String transformedCitation = CSLFormatUtils.transformHTML(citation);
        String actual = CSLFormatUtils.updateSingleCitation(transformedCitation, 3);
        assertEquals(expectedCitation, actual);
    }

    static Stream<Arguments> rawNumericCitationProvider() {
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

    // Tests for extractYear()xtractY

    @Test
    public void testExtractYearWithValidCitation() {
        String citation = "Smith, 2023. The Art of Testing";
        String result = CSLFormatUtils.extractYear(citation);
        assertEquals("S2023. The Art of Testing", result);
    }

    @Test
    public void testExtractYearWithNoYear() {
        String citation = "Smith. The Art of Testing";
        String result = CSLFormatUtils.extractYear(citation);
        assertEquals(citation, result);
    }

    @Test
    public void testExtractYearWithYearOnly() {
        String citation = "2023";
        String result = CSLFormatUtils.extractYear(citation);
        assertEquals(citation, result);
    }

    @Test
    public void testExtractYearWithMultipleYears() {
        String citation = "Johnson, 2022, 2023. Multiple Years Study";
        String result = CSLFormatUtils.extractYear(citation);
        assertEquals("J2022, 2023. Multiple Years Study", result);
    }

    @Test
    public void testExtractYearWithSpecialCharacters() {
        String citation = "O'Brien, 2021. Special Characters' Study";
        String result = CSLFormatUtils.extractYear(citation);
        assertEquals("O2021. Special Characters' Study", result);
    }

    /**
     * <b>Precondition:</b> This test assumes that the method authorsAlpha works correctly.
     *
     * @param entries
     * @param expectedCitation
     */
    @ParameterizedTest
    @MethodSource("provideBibEntries")
    public void testGenerateAlphanumericCitationA(List<BibEntry> entries, String expectedCitation) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        String actualCitation = CSLFormatUtils.generateAlphanumericCitation(entries, context);
        assertEquals(expectedCitation, actualCitation);
    }

    private static Stream<Arguments> provideBibEntries() {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria")
                .withField(StandardField.TITLE, "Quantum Entanglement in Superconductors")
                .withField(StandardField.JOURNAL, "International Review of Physics")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.NUMBER, "6")
                .withField(StandardField.PAGES, "789--810")
                .withField(StandardField.YEAR, "2021")
                .withCitationKey("Garcia_2021");

        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                .withField(StandardField.TITLE, "A Study on Machine Learning Algorithms")
                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                .withField(StandardField.VOLUME, "15")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "101--120")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Smith_2020");

        BibEntry entry3 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Johnson, Emily; Williams, Jessica; Lee, David")
                .withField(StandardField.TITLE, "Trends in Artificial Intelligence")
                .withField(StandardField.JOURNAL, "AI Magazine")
                .withField(StandardField.VOLUME, "41")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.PAGES, "45--60")
                .withField(StandardField.YEAR, "2019")
                .withCitationKey("Johnson_2019");

        BibEntry entry4 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John; Johnson, Emily; Lee, David; Williams, Jessica")
                .withField(StandardField.TITLE, "Big Data Analytics in Healthcare")
                .withField(StandardField.JOURNAL, "Journal of Medical Informatics")
                .withField(StandardField.VOLUME, "23")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "11--25")
                .withField(StandardField.YEAR, "2018")
                .withCitationKey("Smith_2018");

        BibEntry entry5 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria; Smith, John; Johnson, Emily; Lee, David; Williams, Jessica")
                .withField(StandardField.TITLE, "Advances in Renewable Energy Technologies")
                .withField(StandardField.JOURNAL, "Energy Policy")
                .withField(StandardField.VOLUME, "52")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "120--135")
                .withField(StandardField.YEAR, "2017")
                .withCitationKey("Garcia_2017");

        return Stream.of(
                // Entry with single author
                Arguments.of(List.of(entry1), "[Garc21]"),

                // Entry with two authors
                Arguments.of(List.of(entry2), "[SJ20]"),

                // Entry with three authors
                Arguments.of(List.of(entry3), "[JWL19]"),

                // Entry with four authors
                Arguments.of(List.of(entry4), "[SJLW18]"),

                // Entry with five authors
                Arguments.of(List.of(entry5), "[GSJL17]"),

                // Multiple entries with varying number of authors
                Arguments.of(List.of(entry1, entry2, entry3, entry4, entry5), "[Garc21; SJ20; JWL19; SJLW18; GSJL17]")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAuthorsAndExpectedOutput")
    void testAuthorsAlpha(String authors, String expected) {
        AuthorList authorList = AuthorList.parse(authors);
        assertEquals(expected, CSLFormatUtils.authorsAlpha(authorList));
    }

    private static Stream<Arguments> provideAuthorsAndExpectedOutput() {
        return Stream.of(

                // Single author
                Arguments.of("John Doe", "Doe"),
                Arguments.of("van der Aalst", "vdAals"),

                // Two authors
                Arguments.of("John Doe and Jane Smith", "DS"),
                Arguments.of("van der Aalst and Smith", "vdAS"),

                // Three authors
                Arguments.of("John Doe and Jane Smith and Bob Johnson", "DSJ"),
                Arguments.of("van der Aalst and Smith and Johnson", "vdASJ"),

                // Four authors
                Arguments.of("John Doe and Jane Smith and Bob Johnson and Alice Brown", "DSJB"),
                Arguments.of("van der Aalst and Smith and Johnson and Brown", "vdASJB"),

                // Five authors (MAX_ALPHA_AUTHORS)
                Arguments.of("A and B and C and D and E", "ABCD"),
                Arguments.of("van der Aalst and Smith and Johnson and Brown and Davis", "vdASJB"),

                // More than MAX_ALPHA_AUTHORS
                Arguments.of("A and B and C and D and E and F", "ABCD"),
                Arguments.of("van der Aalst and Smith and Johnson and Brown and Davis and Evans", "vdASJB"),

                // With "and others"
                Arguments.of("A and B and C and D and E and others", "ABCD"),
                Arguments.of("van der Aalst and Smith and Johnson and Brown and others", "vdASJB"),

                // Long last names
                Arguments.of("John Doe-Smith", "Doe-"),
                Arguments.of("John van der Aalst-Smith", "vdAals"),

                // Non-ASCII characters
                Arguments.of("Jörg Müller", "Müll"),
                Arguments.of("Élodie Dupont and François Truffaut", "DT")

        );
    }
}