package org.jabref.gui.maintable;

import java.util.List;

import javafx.beans.InvalidationListener;
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

@NullMarked
@ExtendWith(JavaFxExtension.class)
class PersistenceVisualStateTableTest {

    private TableView<BibEntryTableViewModel> table;
    private ColumnPreferences preferences;
    private MainTableColumnModel titleColumn;
    private MainTableColumnModel relevanceColumn;
    private int columnPreferenceChanges;

    @BeforeEach
    void setUp() {
        titleColumn = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, StandardField.TITLE.getName());
        relevanceColumn = new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, SpecialField.RANKING.getName());

        table = new TableView<>();
        table.getColumns().setAll(List.of(
                new MainTableColumn<>(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)),
                new MainTableColumn<>(titleColumn),
                new MainTableColumn<>(relevanceColumn)));

        preferences = new ColumnPreferences(List.of(titleColumn, relevanceColumn), List.of(titleColumn));
        new PersistenceVisualStateTable(table, preferences).addListeners();
        preferences.getColumns().addListener((InvalidationListener) _ -> columnPreferenceChanges++);
    }

    @Test
    void removingColumnUpdatesPreferences() {
        table.getColumns().remove(2);

        assertEquals(List.of(titleColumn), preferences.getColumns());
    }

    @Test
    void columnWidthChangeUpdatesPreferences() {
        titleColumn.widthProperty().set(250);

        assertEquals(1, columnPreferenceChanges);
    }

    @Test
    void removedColumnWidthChangeDoesNotUpdatePreferences() {
        table.getColumns().remove(2);
        columnPreferenceChanges = 0;

        relevanceColumn.widthProperty().set(250);

        assertEquals(0, columnPreferenceChanges);
    }
}
