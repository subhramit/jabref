package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;

import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import de.undercouch.citeproc.output.Citation;
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
    private XTextRange mockTextRange;
    @Mock
    private CitationStyle mockStyle;
    @Mock
    private BibDatabaseContext mockBibDatabaseContext;
    @Mock
    private BibEntryTypesManager mockBibEntryTypesManager;
    @Mock
    private CSLReferenceMarkManager mockMarkManager;
    @Mock
    private CSLReferenceMark mockReferenceMark;

    private CSLCitationOOAdapter adapter;
    private BibEntry testEntry;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(mockDocument.getText()).thenReturn(mockText);
        when(mockCursor.getText()).thenReturn(mockText);
        when(mockCursor.getStart()).thenReturn(mockTextRange);
        when(mockText.createTextCursorByRange(any(XTextRange.class))).thenReturn(mockCursor);

        // Mock behavior for cursor.goLeft
        when(mockCursor.goLeft(anyShort(), anyBoolean())).thenReturn(true);
        when(mockCursor.getString()).thenReturn(" ");

        // Mock CSLReferenceMarkManager behavior
        when(mockMarkManager.createReferenceMark(any(BibEntry.class))).thenReturn(mockReferenceMark);

        adapter = new CSLCitationOOAdapter(mockDocument);

        // Use reflection to set the mockMarkManager
        java.lang.reflect.Field field = CSLCitationOOAdapter.class.getDeclaredField("markManager");
        field.setAccessible(true);
        field.set(adapter, mockMarkManager);

        testEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.YEAR, "2023");
    }

    @Test
    void insertCitation() throws Exception {
        // Arrange
        List<BibEntry> entries = List.of(testEntry);
        when(mockStyle.isNumericStyle()).thenReturn(false);
        when(mockStyle.getSource()).thenReturn("apa");

        Citation mockCitation = mock(Citation.class);
        when(mockCitation.getText()).thenReturn("(Smith, 2023)");

        try (MockedStatic<CitationStyleGenerator> mockedGenerator = mockStatic(CitationStyleGenerator.class)) {
            mockedGenerator.when(() -> CitationStyleGenerator.generateInText(eq(entries), anyString(), any(CitationStyleOutputFormat.class), eq(mockBibDatabaseContext), eq(mockBibEntryTypesManager)))
                           .thenReturn(mockCitation);

            // Act
            adapter.insertCitation(mockCursor, mockStyle, entries, mockBibDatabaseContext, mockBibEntryTypesManager);

            // Assert
            verify(mockCursor, times(2)).collapseToEnd(); // Expect collapseToEnd to be called twice
            verify(mockMarkManager).createReferenceMark(eq(testEntry));
            verify(mockReferenceMark).insertReferenceIntoOO(eq(mockDocument), eq(mockCursor), any(OOText.class), anyBoolean(), anyBoolean(), anyBoolean());
        }
    }
}
