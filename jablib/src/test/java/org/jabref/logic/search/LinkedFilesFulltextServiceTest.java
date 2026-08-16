package org.jabref.logic.search;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NullMarked
class LinkedFilesFulltextServiceTest {
    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();

    @TempDir
    private Path indexDir;

    private @Nullable LinkedFilesFulltextService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.closeAndWait();
        }
    }

    @Test
    void searchReturnsPdfMatchesWhenFulltextIsEnabled() throws IOException, URISyntaxException {
        FilePreferences filePreferences = FilePreferences.getDefault();
        filePreferences.setFulltextIndexLinkedFiles(true);
        BibDatabaseContext databaseContext = initializeDatabaseFromPath("test-library-with-attached-files.bib");

        service = new LinkedFilesFulltextService(databaseContext, filePreferences, TASK_EXECUTOR);

        Set<String> matchedEntries = service.search(new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT))).getMatchedEntries();

        assertEquals(Set.of(
                getEntryIdByCitationKey(databaseContext, "minimal-sentence-case"),
                getEntryIdByCitationKey(databaseContext, "minimal-all-upper-case"),
                getEntryIdByCitationKey(databaseContext, "minimal-mixed-case")), matchedEntries);
    }

    @Test
    void searchReturnsEmptyResultsWhenFulltextIsDisabled() throws IOException, URISyntaxException {
        FilePreferences filePreferences = FilePreferences.getDefault();
        filePreferences.setFulltextIndexLinkedFiles(false);
        BibDatabaseContext databaseContext = initializeDatabaseFromPath("test-library-with-attached-files.bib");

        service = new LinkedFilesFulltextService(databaseContext, filePreferences, TASK_EXECUTOR);

        Set<String> matchedEntries = service.search(new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT))).getMatchedEntries();

        assertEquals(Set.of(), matchedEntries);
    }

    @Test
    void fileFieldUpdateRefreshesIndex() throws IOException, URISyntaxException {
        FilePreferences filePreferences = FilePreferences.getDefault();
        filePreferences.setFulltextIndexLinkedFiles(true);
        BibDatabaseContext databaseContext = initializeDatabaseFromPath("test-library-with-attached-files.bib");

        service = new LinkedFilesFulltextService(databaseContext, filePreferences, TASK_EXECUTOR);

        BibEntry entry = getEntryByCitationKey(databaseContext, "minimal-sentence-case");
        String oldValue = entry.getField(StandardField.FILE).orElseThrow();
        entry.setFiles(List.of(new LinkedFile("", "minimal-note-sentence-case.pdf", StandardFileType.PDF.getName())));
        String newValue = entry.getField(StandardField.FILE).orElseThrow();

        service.updateEntry(new FieldChangedEvent(entry, StandardField.FILE, newValue, oldValue));

        Set<String> oldQueryMatches = service.search(new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT))).getMatchedEntries();
        Set<String> newQueryMatches = service.search(new SearchQuery("world", EnumSet.of(SearchFlags.FULLTEXT))).getMatchedEntries();

        assertFalse(oldQueryMatches.contains(entry.getId()));
        assertTrue(newQueryMatches.contains(entry.getId()));
        assertTrue(service.search(new SearchQuery("world", EnumSet.of(SearchFlags.FULLTEXT))).hasFulltextResults(entry));
    }

    private BibDatabaseContext initializeDatabaseFromPath(String testFile) throws IOException, URISyntaxException {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor())
                .importDatabase(Path.of(Objects.requireNonNull(LinkedFilesFulltextServiceTest.class.getResource("/org/jabref/logic/search/" + testFile)).toURI()));
        BibDatabaseContext databaseContext = spy(result.getDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);
        return databaseContext;
    }

    private BibEntry getEntryByCitationKey(BibDatabaseContext databaseContext, String citationKey) {
        return databaseContext.getEntries().stream()
                              .filter(entry -> entry.getCitationKey().map(citationKey::equals).orElse(false))
                              .findFirst()
                              .orElseThrow();
    }

    private String getEntryIdByCitationKey(BibDatabaseContext databaseContext, String citationKey) {
        return getEntryByCitationKey(databaseContext, citationKey).getId();
    }
}
