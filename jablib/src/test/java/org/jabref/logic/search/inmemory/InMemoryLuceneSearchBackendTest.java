package org.jabref.logic.search.inmemory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NullMarked
class InMemoryLuceneSearchBackendTest {

    @TempDir
    private Path indexDir;

    private @Nullable InMemoryLuceneSearchBackend searchBackend;

    @AfterEach
    void tearDown() {
        if (searchBackend != null) {
            searchBackend.close();
        }
    }

    @Test
    void fulltextQueriesMergeMetadataAndLinkedFileMatches() throws URISyntaxException {
        BibDatabaseContext databaseContext = spy(new BibDatabaseContext());
        Path resourceDirectory = Path.of(Objects.requireNonNull(InMemoryLuceneSearchBackendTest.class.getResource("/org/jabref/logic/search/test-library-with-attached-files.bib")).toURI()).getParent();
        doReturn(List.of(resourceDirectory)).when(databaseContext).getFileDirectories(any());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);

        BibEntry metadataMatch = new BibEntry(StandardEntryType.Article)
                .withCitationKey("metadata")
                .withField(StandardField.TITLE, "comma");
        BibEntry fulltextMatch = new BibEntry(StandardEntryType.Article)
                .withCitationKey("fulltext")
                .withField(StandardField.TITLE, "other")
                .withFiles(List.of(new LinkedFile("", "minimal-sentence-case.pdf", StandardFileType.PDF.getName())));
        databaseContext.getDatabase().insertEntries(List.of(metadataMatch, fulltextMatch));

        FilePreferences filePreferences = FilePreferences.getDefault();
        filePreferences.setFulltextIndexLinkedFiles(true);
        searchBackend = new InMemoryLuceneSearchBackend(databaseContext, new BibEntryPreferences(','), filePreferences, new CurrentThreadTaskExecutor());

        SearchResults results = searchBackend.search(new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT)));

        assertEquals(Set.of(metadataMatch.getId(), fulltextMatch.getId()), results.getMatchedEntries());
        assertFalse(results.hasFulltextResults(metadataMatch));
        assertTrue(results.hasFulltextResults(fulltextMatch));
    }
}
