package org.jabref.logic.search.sqlbased;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.mockito.Answers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("embeddedPostgres")
class IndexManagerTest {

    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();

    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
    private BibDatabaseContext databaseContext;
    private PostgresServer postgresServer;

    @TempDir
    private Path indexDir;

    @BeforeEach
    void setUp() {
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(false);
        when(filePreferences.fulltextIndexLinkedFilesProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        databaseContext = spy(new BibDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);

        postgresServer = new PostgresServer();
    }

    @AfterEach
    void tearDown() {
        postgresServer.close();
    }

    @Test
    void closeAndWaitCancelsScheduledThrottledUpdateAndShutsDownThrottler() throws Exception {
        IndexManager indexManager = new IndexManager(databaseContext, TASK_EXECUTOR, preferences, postgresServer);

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "old");
        databaseContext.getDatabase().insertEntry(entry);
        indexManager.updateEntry(new FieldChangedEvent(entry, StandardField.TITLE, "new", "old"));

        DelayTaskThrottler throttler = getPrivateField(indexManager, "indexUpdateThrottler", DelayTaskThrottler.class);
        ScheduledFuture<?> scheduledTaskBeforeClose = getPrivateField(throttler, "scheduledTask", ScheduledFuture.class);
        assertNotNull(scheduledTaskBeforeClose);
        assertFalse(scheduledTaskBeforeClose.isCancelled());

        indexManager.closeAndWait();

        ScheduledThreadPoolExecutor executor = getPrivateField(throttler, "executor", ScheduledThreadPoolExecutor.class);
        ScheduledFuture<?> scheduledTaskAfterClose = getPrivateField(throttler, "scheduledTask", ScheduledFuture.class);
        assertTrue(executor.isShutdown());
        assertTrue(scheduledTaskAfterClose == null || scheduledTaskAfterClose.isCancelled());
    }

    @Test
    void searchReturnsLinkedFileMatchesWhenFulltextEnabled() throws Exception {
        FilePreferences enabledFilePreferences = FilePreferences.getDefault();
        enabledFilePreferences.setFulltextIndexLinkedFiles(true);
        when(preferences.getFilePreferences()).thenReturn(enabledFilePreferences);

        BibDatabaseContext fulltextDatabaseContext = initializeDatabaseFromPath("test-library-with-attached-files.bib");
        IndexManager indexManager = new IndexManager(fulltextDatabaseContext, TASK_EXECUTOR, preferences, postgresServer);

        try {
            SearchQuery searchQuery = new SearchQuery("comma", EnumSet.of(SearchFlags.FULLTEXT));
            Set<String> matchedEntries = indexManager.search(searchQuery).getMatchedEntries();

            Set<String> expectedEntries = Set.of(
                    getEntryIdByCitationKey(fulltextDatabaseContext, "minimal-sentence-case"),
                    getEntryIdByCitationKey(fulltextDatabaseContext, "minimal-all-upper-case"),
                    getEntryIdByCitationKey(fulltextDatabaseContext, "minimal-mixed-case"));
            assertEquals(expectedEntries, matchedEntries);
        } finally {
            indexManager.closeAndWait();
        }
    }

    private BibDatabaseContext initializeDatabaseFromPath(String testFile) throws URISyntaxException, java.io.IOException {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor())
                .importDatabase(Path.of(Objects.requireNonNull(IndexManagerTest.class.getResource("/org/jabref/logic/search/" + testFile)).toURI()));
        BibDatabaseContext fulltextDatabaseContext = spy(result.getDatabaseContext());
        when(fulltextDatabaseContext.getFulltextIndexPath()).thenReturn(indexDir);
        return fulltextDatabaseContext;
    }

    private String getEntryIdByCitationKey(BibDatabaseContext fulltextDatabaseContext, String citationKey) {
        return fulltextDatabaseContext.getEntries().stream()
                                      .filter(entry -> entry.getCitationKey().map(citationKey::equals).orElse(false))
                                      .findFirst()
                                      .orElseThrow()
                                      .getId();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName, Class<T> expectedType) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) expectedType.cast(field.get(object));
    }
}
