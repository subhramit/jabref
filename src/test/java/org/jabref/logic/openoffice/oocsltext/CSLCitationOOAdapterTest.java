package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.uno.CreationException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import de.undercouch.citeproc.output.Citation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyShort;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CSLCitationOOAdapterTest {

    @Mock
    private XTextDocument mockDocument;
    @Mock
    private XTextCursor mockCursor;
    @Mock
    private XText mockText;
    @Mock
    private CitationStyle mockStyle;
    @Mock
    private BibDatabaseContext mockBibDatabaseContext;
    @Mock
    private BibEntryTypesManager mockBibEntryTypesManager;

    private CSLCitationOOAdapter adapter;
    private BibEntry testEntry;
    private MockedStatic<CitationStyleGenerator> mockedCitationStyleGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new CSLCitationOOAdapter(mockDocument);
        testEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.YEAR, "2023");

        // Mock XTextCursor.getText() to return a non-null XText
        when(mockCursor.getText()).thenReturn(mockText);
        when(mockText.createTextCursorByRange(any())).thenReturn(mockCursor);

        // Initialize the mocked static CitationStyleGenerator
        mockedCitationStyleGenerator = mockStatic(CitationStyleGenerator.class);
    }

    @AfterEach
    void tearDown() {
        // Close the mocked static CitationStyleGenerator
        mockedCitationStyleGenerator.close();
    }

    @Test
    void insertBibliography() throws WrappedTargetException, CreationException {
        // Arrange
        List<BibEntry> entries = List.of(testEntry);
        when(mockStyle.isNumericStyle()).thenReturn(true);
        when(mockStyle.getSource()).thenReturn("ieee");

        mockedCitationStyleGenerator.when(() -> CitationStyleGenerator.generateCitation(eq(entries), anyString(), any(CitationStyleOutputFormat.class), eq(mockBibDatabaseContext), eq(mockBibEntryTypesManager)))
                                    .thenReturn(List.of("[1] J. Smith, \"Test Title,\" 2023."));

        // Act
        adapter.insertBibliography(mockCursor, mockStyle, entries, mockBibDatabaseContext, mockBibEntryTypesManager);

        // Assert
        verify(mockCursor, times(3)).goLeft(anyShort(), anyBoolean());
        verify(mockCursor, times(2)).setString(anyString());
    }

    @Test
    void insertCitation() throws Exception {
        // Arrange
        List<BibEntry> entries = List.of(testEntry);
        when(mockStyle.isNumericStyle()).thenReturn(false);
        when(mockStyle.getSource()).thenReturn("apa");

        Citation mockCitation = mock(Citation.class);
        when(mockCitation.getText()).thenReturn("(Smith, 2023)");
        mockedCitationStyleGenerator.when(() -> CitationStyleGenerator.generateInText(eq(entries), anyString(), any(CitationStyleOutputFormat.class), eq(mockBibDatabaseContext), eq(mockBibEntryTypesManager)))
                                    .thenReturn(mockCitation);

        // Act
        adapter.insertCitation(mockCursor, mockStyle, entries, mockBibDatabaseContext, mockBibEntryTypesManager);

        // Assert
        verify(mockCursor).collapseToEnd();
    }

    @Test
    void insertInTextCitation() throws Exception {
        // Arrange
        List<BibEntry> entries = List.of(testEntry);
        when(mockStyle.isNumericStyle()).thenReturn(false);
        when(mockStyle.getSource()).thenReturn("apa");

        Citation mockCitation = mock(Citation.class);
        when(mockCitation.getText()).thenReturn("Smith (2023)");
        mockedCitationStyleGenerator.when(() -> CitationStyleGenerator.generateInText(eq(entries), anyString(), any(CitationStyleOutputFormat.class), eq(mockBibDatabaseContext), eq(mockBibEntryTypesManager)))
                                    .thenReturn(mockCitation);

        // Act
        adapter.insertInTextCitation(mockCursor, mockStyle, entries, mockBibDatabaseContext, mockBibEntryTypesManager);

        // Assert
        verify(mockCursor).collapseToEnd();
    }
}
