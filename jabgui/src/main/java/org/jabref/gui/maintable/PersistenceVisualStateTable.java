package org.jabref.gui.maintable;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Keep track of changes made to the columns (reordering, resorting).
///
/// Resizing and changing the sort type need no listener here: the column models are the very instances held by
/// [ColumnPreferences], which reports changes of their properties itself.
public class PersistenceVisualStateTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceVisualStateTable.class);

    protected final TableView<BibEntryTableViewModel> table;
    protected final ColumnPreferences preferences;

    public PersistenceVisualStateTable(TableView<BibEntryTableViewModel> table, ColumnPreferences preferences) {
        this.table = table;
        this.preferences = preferences;
    }

    public void addListeners() {
        table.getColumns().addListener((InvalidationListener) _ -> updateColumns());
        table.getSortOrder().addListener((ListChangeListener<? super TableColumn<BibEntryTableViewModel, ?>>) _ -> updateSortOrder());
    }

    /// Stores shown columns, their width and their [TableColumn.SortType] in preferences.
    /// The conversion to the "real" string in the preferences is made at
    /// [org.jabref.logic.preferences.JabRefCliPreferences#getColumnSortTypesAsStringList(ColumnPreferences)]
    private void updateColumns() {
        List<MainTableColumnModel> list = toList(table.getColumns());
        LOGGER.debug("Updating columns to {}", list);
        preferences.setColumns(list);
    }

    /// Stores the SortOrder of the Table in the preferences. This includes [TableColumn.SortType].
    ///
    /// Cannot be combined with updateColumns, because JavaFX would provide just an empty list for the sort order
    /// on other changes.
    private void updateSortOrder() {
        LOGGER.debug("Updating sort order");
        preferences.setColumnSortOrder(toList(table.getSortOrder()));
    }

    private List<MainTableColumnModel> toList(List<TableColumn<BibEntryTableViewModel, ?>> columns) {
        return columns.stream()
                      .filter(col -> col instanceof MainTableColumn<?>)
                      .map(column -> ((MainTableColumn<?>) column).getModel())
                      .filter(MainTableColumnModel::isConfigurable)
                      .collect(Collectors.toList());
    }
}
