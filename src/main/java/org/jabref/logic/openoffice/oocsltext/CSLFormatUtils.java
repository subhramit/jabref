package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Contains utility constants and methods for processing of CSL citations as generated by methods of <a href="https://github.com/michel-kraemer/citeproc-javaciteproc-java">citeproc-java</a> ({@link org.jabref.logic.citationstyle.CitationStyleGenerator}).
 * <p>These methods are used in {@link CSLCitationOOAdapter} which inserts CSL citation text into an OO document.</p>
 */
public class CSLFormatUtils {

    public static final Pattern YEAR_IN_CITATION_PATTERN = Pattern.compile("(.).*?, (\\d{4}.*)");
    public static final String[] PREFIXES = {"JABREF_", "CSL_"};

    // TODO: These are static final fields right now, should add the functionality to let user select these and store them in preferences.
    public static final String DEFAULT_BIBLIOGRAPHY_TITLE = "References";
    public static final String DEFAULT_BIBLIOGRAPHY_HEADER_PARAGRAPH_FORMAT = "Heading 2";
    public static final CitationStyleOutputFormat OUTPUT_FORMAT = CitationStyleOutputFormat.HTML;
    private static final int MAX_ALPHA_AUTHORS = 4;

    /**
     * Transforms provided HTML into a format that can be fully parsed by OOTextIntoOO.write(...)
     * The transformed HTML can be used for inserting into a LibreOffice document
     * Context: The HTML produced by CitationStyleGenerator.generateCitation(...) is not directly (completely) parsable by OOTextIntoOO.write(...)
     * For more details, read the documentation of the write(...) method in the {@link OOTextIntoOO} class.
     * <a href="https://devdocs.jabref.org/code-howtos/openoffice/code-reorganization.html">Additional Information</a>.
     *
     * @param html The HTML string to be transformed into OO-write ready HTML.
     * @return The formatted html string
     */
    public static String transformHTML(String html) {
        // Initial clean up of escaped characters
        html = StringEscapeUtils.unescapeHtml4(html);

        // Handle margins (spaces between citation number and text)
        html = html.replaceAll("<div class=\"csl-left-margin\">(.*?)</div><div class=\"csl-right-inline\">(.*?)</div>", "$1 $2");

        // Remove unsupported tags
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replace("</div>", "");

        // Remove unsupported links
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replace("</a>", "");

        // Replace span tags with inline styles for bold
        html = html.replaceAll("<span style=\"font-weight: ?bold;?\">(.*?)</span>", "<b>$1</b>");

        // Replace span tags with inline styles for italic
        html = html.replaceAll("<span style=\"font-style: ?italic;?\">(.*?)</span>", "<i>$1</i>");

        // Replace span tags with inline styles for underline
        html = html.replaceAll("<span style=\"text-decoration: ?underline;?\">(.*?)</span>", "<u>$1</u>");

        html = html.replaceAll("<span style=\"font-variant: ?small-caps;?\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Clean up any remaining span tags
        html = html.replaceAll("</?span[^>]*>", "");

        return html;
    }

    /**
     * Method to update citation number of a bibliographic entry.
     * By default, citeproc-java's generateCitation always starts the numbering of a list of citations with "1".
     * If a citation doesn't correspond to the first cited entry, the number should be changed to the relevant current citation number.
     * If an entries has been cited before, the current number should be old number.
     * The number can be enclosed in different formats, such as "1", "1.", "1)", "(1)" or "[1]".
     * Precondition: Use ONLY with numeric citation styles.
     *
     * @param citation the numeric citation with an unresolved number.
     * @param currentNumber the correct number to update the citation with.
     * @return the bibliographic citation with resolved number.
     */
    public static String updateSingleCitation(String citation, int currentNumber) {
        Pattern pattern = Pattern.compile("(\\[|\\()?(\\d+)(\\]|\\))?(\\.)?\\s*");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        boolean numberReplaced = false;

        while (matcher.find()) {
            if (!numberReplaced) {
                String prefix = matcher.group(1) != null ? matcher.group(1) : "";
                String suffix = matcher.group(3) != null ? matcher.group(3) : "";
                String dot = matcher.group(4) != null ? "." : "";
                String space = matcher.group().endsWith(" ") ? " " : "";

                String replacement = prefix + currentNumber + suffix + dot + space;

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                numberReplaced = true;
            } else {
                matcher.appendReplacement(sb, matcher.group());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Extracts year from a citation having single or multiple entries, for the purpose of using in in-text citations.
     *
     * @param formattedCitation - the citation cleaned up and formatted using transformHTML
     */
    public static String extractYear(String formattedCitation) {
        Matcher matcher = YEAR_IN_CITATION_PATTERN.matcher(formattedCitation);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group(2);
        }
        return formattedCitation;
    }

    public static String generateAlphanumericCitation(List<BibEntry> entries, BibDatabaseContext bibDatabaseContext) {
        StringBuilder citation = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            BibEntry entry = entries.get(i);
            Optional<String> author = entry.getResolvedFieldOrAlias(StandardField.AUTHOR, bibDatabaseContext.getDatabase());
            Optional<String> year = entry.getResolvedFieldOrAlias(StandardField.YEAR, bibDatabaseContext.getDatabase());

            if (author.isPresent() && year.isPresent()) {
                AuthorList authorList = AuthorList.parse(author.get());
                String alphaKey = authorsAlpha(authorList);

                // Extract last two digits of the year
                String shortYear = year.get().length() >= 2 ?
                        year.get().substring(year.get().length() - 2) :
                        year.get();

                citation.append(alphaKey).append(shortYear);
            } else {
                citation.append(entry.getCitationKey().orElse(""));
            }

            if (i < entries.size() - 1) {
                citation.append("; ");
            }
        }
        citation.append("]");
        return citation.toString();
    }

    public static String authorsAlpha(AuthorList authorList) {
        StringBuilder alphaStyle = new StringBuilder();
        int maxAuthors;
        final boolean maxAuthorsExceeded;
        if (authorList.getNumberOfAuthors() <= MAX_ALPHA_AUTHORS) {
            maxAuthors = authorList.getNumberOfAuthors();
            maxAuthorsExceeded = false;
        } else {
            maxAuthors = MAX_ALPHA_AUTHORS;
            maxAuthorsExceeded = true;
        }

        if (authorList.getNumberOfAuthors() == 1) {
            String[] firstAuthor = authorList.getAuthor(0).getNamePrefixAndFamilyName()
                                             .replaceAll("\\s+", " ").trim().split(" ");
            // take first letter of any "prefixes" (e.g. van der Aalst -> vd)
            for (int j = 0; j < (firstAuthor.length - 1); j++) {
                alphaStyle.append(firstAuthor[j], 0, 1);
            }
            // append last part of last name completely
            alphaStyle.append(firstAuthor[firstAuthor.length - 1], 0,
                    Math.min(4, firstAuthor[firstAuthor.length - 1].length()));
        } else {
            boolean andOthersPresent = authorList.getAuthor(maxAuthors - 1).equals(Author.OTHERS);
            if (andOthersPresent) {
                maxAuthors--;
            }
            List<String> vonAndLastNames = authorList.getAuthors().stream()
                                                     .limit(maxAuthors)
                                                     .map(Author::getNamePrefixAndFamilyName)
                                                     .toList();
            for (String vonAndLast : vonAndLastNames) {
                // replace all whitespaces by " "
                // split the lastname at " "
                String[] nameParts = vonAndLast.replaceAll("\\s+", " ").trim().split(" ");
                for (String part : nameParts) {
                    // use first character of each part of lastname
                    alphaStyle.append(part, 0, 1);
                }
            }
        }
        return alphaStyle.toString();
    }
}