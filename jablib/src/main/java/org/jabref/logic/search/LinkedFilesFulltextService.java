package org.jabref.logic.search;

import java.io.IOException;
import java.util.List;

import javafx.beans.value.ChangeListener;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.sqlbased.LuceneIndexer;
import org.jabref.logic.search.sqlbased.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Shared linked-file fulltext indexing + search lifecycle.
@NullMarked
public final class LinkedFilesFulltextService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFilesFulltextService.class);

    private final TaskExecutor taskExecutor;
    private final FilePreferences filePreferences;
    private final LuceneIndexer linkedFilesIndexer;
    private final LinkedFilesSearcher linkedFilesSearcher;
    private final ChangeListener<Boolean> preferenceListener;

    public LinkedFilesFulltextService(BibDatabaseContext databaseContext,
                                      FilePreferences filePreferences,
                                      TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        this.filePreferences = filePreferences;

        LuceneIndexer indexer;
        try {
            indexer = new DefaultLinkedFilesIndexer(databaseContext, filePreferences);
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index, using read-only index", e);
            indexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
        this.linkedFilesIndexer = indexer;
        this.linkedFilesSearcher = new LinkedFilesSearcher(databaseContext, linkedFilesIndexer, filePreferences);

        this.preferenceListener = (_, _, newValue) -> {
            if (newValue) {
                updateOnStart();
            } else {
                linkedFilesIndexer.removeAllFromIndex();
            }
        };
        this.filePreferences.fulltextIndexLinkedFilesProperty().addListener(preferenceListener);

        if (filePreferences.shouldFulltextIndexLinkedFiles()) {
            updateOnStart();
        }
    }

    public SearchResults search(SearchQuery query) {
        return linkedFilesSearcher.search(query);
    }

    public void addToIndex(List<BibEntry> entries) {
        if (!filePreferences.shouldFulltextIndexLinkedFiles()) {
            return;
        }

        new BackgroundTask<>() {
            @Override
            public Void call() {
                linkedFilesIndexer.addToIndex(entries, this);
                return null;
            }
        }.executeWith(taskExecutor);
    }

    public void removeFromIndex(List<BibEntry> entries) {
        if (!filePreferences.shouldFulltextIndexLinkedFiles()) {
            return;
        }

        new BackgroundTask<>() {
            @Override
            public Void call() {
                linkedFilesIndexer.removeFromIndex(entries, this);
                return null;
            }
        }.executeWith(taskExecutor);
    }

    public void updateEntry(FieldChangedEvent event) {
        if (!filePreferences.shouldFulltextIndexLinkedFiles() || StandardField.FILE != event.getField()) {
            return;
        }

        new BackgroundTask<>() {
            @Override
            public Void call() {
                linkedFilesIndexer.updateEntry(event.getBibEntry(), event.getOldValue(), event.getNewValue(), this);
                return null;
            }
        }.executeWith(taskExecutor);
    }

    public void rebuildFullTextIndex() {
        if (!filePreferences.shouldFulltextIndexLinkedFiles()) {
            return;
        }

        new BackgroundTask<>() {
            @Override
            public Void call() {
                linkedFilesIndexer.rebuildIndex(this);
                return null;
            }
        }.executeWith(taskExecutor);
    }

    public void close() {
        filePreferences.fulltextIndexLinkedFilesProperty().removeListener(preferenceListener);
        linkedFilesIndexer.close();
    }

    public void closeAndWait() {
        filePreferences.fulltextIndexLinkedFilesProperty().removeListener(preferenceListener);
        linkedFilesIndexer.closeAndWait();
    }

    private void updateOnStart() {
        new BackgroundTask<>() {
            @Override
            public Void call() {
                linkedFilesIndexer.updateOnStart(this);
                return null;
            }
        }.executeWith(taskExecutor);
    }
}
