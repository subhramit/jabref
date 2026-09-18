package org.jabref.gui.maintable;

import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class MainTableColumnPreferenceSynchronizerTest {

    private TableView<BibEntryTableViewModel> table;
    private MainTablePreferences mainTablePreferences;
    private MainTableColumnPreferenceSynchronizer synchronizer;
    private MainTableColumn<?> matchCategoryColumn;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel relevanceColumn;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        ColumnPreferences columnPreferences = new ColumnPreferences(List.of(titleColumn), List.of(titleColumn));
        mainTablePreferences = new MainTablePreferences(columnPreferences, false, false);

        matchCategoryColumn = new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY));
        table = new TableView<>();
        table.getColumns().setAll(List.of(matchCategoryColumn, new MainTableColumn<>(titleColumn)));
        table.getSortOrder().setAll(List.of(matchCategoryColumn, table.getColumns().get(1)));

        MainTableColumnFactory columnFactory = mock(MainTableColumnFactory.class);
        when(columnFactory.createColumn(any(MainTableColumnModel.class))).thenAnswer(invocation -> new MainTableColumn<>((MainTableColumnModel) invocation.getArgument(0)));

        // Installed as in MainTable, so that the write-back of the table state is part of every test
        new PersistenceVisualStateTable(table, columnPreferences).addListeners();
        synchronizer = new MainTableColumnPreferenceSynchronizer(table, columnFactory, mainTablePreferences);
        synchronizer.addListeners();
    }

    @Test
    void updatesDisplayedColumnsWhenColumnPreferencesChange() {
        mainTablePreferences.getColumnPreferences().setColumns(List.of(relevanceColumn, titleColumn));

        assertEquals(List.of(relevanceColumn, titleColumn), visibleColumns());
    }

    @Test
    void keepsPreferencesWhenTableWritesItsStateBack() {
        mainTablePreferences.getColumnPreferences().setColumns(List.of(relevanceColumn, titleColumn));

        assertEquals(List.of(relevanceColumn, titleColumn), mainTablePreferences.getColumnPreferences().getColumns());
    }

    @Test
    void keepsExistingColumnsWhenAddingAnotherConfiguredColumn() {
        TableColumn<BibEntryTableViewModel, ?> originalTitleColumn = table.getColumns().get(1);

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));

        assertSame(originalTitleColumn, table.getColumns().get(1));
    }

    @Test
    void keepsMatchCategoryColumnInSortOrderWhenColumnsChange() {
        mainTablePreferences.getColumnPreferences().setColumns(List.of(relevanceColumn));

        assertEquals(List.of(matchCategoryColumn), table.getSortOrder());
    }

    @Test
    void ignoresReservedColumnsInPreferences() {
        MainTableColumnModel reservedColumn = new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY);

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, reservedColumn, relevanceColumn));

        assertEquals(List.of(titleColumn, relevanceColumn), visibleColumns());
    }

    @Test
    void disposedSynchronizerStopsReactingToPreferenceChanges() {
        synchronizer.dispose();

        mainTablePreferences.getColumnPreferences().setColumns(List.of(titleColumn, relevanceColumn));

        assertEquals(List.of(titleColumn), visibleColumns());
    }

    @Test
    void updatesResizePolicyWhenPreferenceChanges() {
        mainTablePreferences.setResizeColumnsToFit(true);

        assertEquals(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS, table.getColumnResizePolicy());
    }

    private List<MainTableColumnModel> visibleColumns() {
        return table.getColumns().stream()
                    .map(column -> ((MainTableColumn<?>) column).getModel())
                    .filter(model -> model.getType() != MainTableColumnModel.Type.MATCH_CATEGORY)
                    .toList();
    }
}
