package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import de.undercouch.citeproc.output.Citation;
import org.jbibtex.TokenMgrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade to unify the access to the citation style engine. Use these methods if you need rendered BibTeX item(s) in a
 * given journal style. This class uses {@link CSLAdapter} to create output.
 */
public class CitationStyleGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleGenerator.class);
    private static final CSLAdapter CSL_ADAPTER = new CSLAdapter();

    private CitationStyleGenerator() {
    }

    /**
     * Generates a Citation based on the given entry and style with a default {@link BibDatabaseContext}
     *
     * @implNote the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     */
    protected static String generateCitation(BibEntry entry, CitationStyle style, BibEntryTypesManager entryTypesManager) {
        return generateCitation(entry, style.getSource(), entryTypesManager);
    }

    /**
     * Generates a Citation based on the given entry and style with a default {@link BibDatabaseContext}
     *
     * @implNote the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     */
    protected static String generateCitation(BibEntry entry, String style, BibEntryTypesManager entryTypesManager) {
        return generateCitation(entry, style, CitationStyleOutputFormat.HTML, new BibDatabaseContext(), entryTypesManager);
    }

    /**
     * Generates a Citation based on the given entry, style, and output format
     *
     * @implNote the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     */
    public static String generateCitation(BibEntry entry, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) {
        return generateCitations(Collections.singletonList(entry), style, outputFormat, databaseContext, entryTypesManager).stream().findFirst().orElse("");
    }

    public static Citation generateInText(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) throws IOException {
        return CSL_ADAPTER.makeInText(bibEntries, style, outputFormat, databaseContext, entryTypesManager);
    }

    /**
     * Generates the citation for multiple entries at once.
     *
     * @implNote The citations are generated using JavaScript which may take some time, better call it from outside the main thread.
     */
    public static List<String> generateCitations(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) {
        try {
            return CSL_ADAPTER.makeBibliography(bibEntries, style, outputFormat, databaseContext, entryTypesManager);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not generate BibEntry citation. The CSL engine could not create a preview for your item.", e);
            return Collections.singletonList(Localization.lang("Cannot generate preview based on selected citation style."));
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Could not generate BibEntry citation", e);
            return Collections.singletonList(Localization.lang("Cannot generate preview based on selected citation style."));
        } catch (TokenMgrException e) {
            LOGGER.error("Bad character inside BibEntry", e);
            // sadly one cannot easily retrieve the bad char from the TokenMgrError
            return Collections.singletonList(Localization.lang("Cannot generate preview based on selected citation style.") +
                    outputFormat.getLineSeparator() +
                    Localization.lang("Bad character inside entry") +
                    outputFormat.getLineSeparator() +
                    e.getLocalizedMessage());
        }
    }
}
