package org.jabref.logic.search.inmemory;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.LinkedFilesFulltextService;
import org.jabref.logic.search.SearchBackend;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;

/// Hybrid in-memory backend: metadata search stays in-memory while linked-file
/// fulltext is delegated to [LinkedFilesFulltextService].
@NullMarked
public class InMemoryLuceneSearchBackend implements SearchBackend {

    private final InMemorySearchBackend inMemorySearchBackend;
    private final LinkedFilesFulltextService linkedFilesFulltextService;

    public InMemoryLuceneSearchBackend(BibDatabaseContext databaseContext,
                                       BibEntryPreferences bibEntryPreferences,
                                       FilePreferences filePreferences,
                                       TaskExecutor taskExecutor) {
        this.inMemorySearchBackend = new InMemorySearchBackend(databaseContext, bibEntryPreferences);
        this.linkedFilesFulltextService = new LinkedFilesFulltextService(databaseContext, filePreferences, taskExecutor);
    }

    @Override
    public SearchResults search(SearchQuery query) {
        SearchResults results = inMemorySearchBackend.search(query);
        results.mergeSearchResults(linkedFilesFulltextService.search(query));
        query.setSearchResults(results);
        return results;
    }

    @Override
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return inMemorySearchBackend.isEntryMatched(entry, query);
    }

    @Override
    public void addToIndex(List<BibEntry> entries) {
        linkedFilesFulltextService.addToIndex(entries);
    }

    @Override
    public void removeFromIndex(List<BibEntry> entries) {
        linkedFilesFulltextService.removeFromIndex(entries);
    }

    @Override
    public void updateEntry(FieldChangedEvent event) {
        linkedFilesFulltextService.updateEntry(event);
    }

    @Override
    public void rebuildFullTextIndex() {
        linkedFilesFulltextService.rebuildFullTextIndex();
    }

    @Override
    public void close() {
        linkedFilesFulltextService.closeAndWait();
    }
}
