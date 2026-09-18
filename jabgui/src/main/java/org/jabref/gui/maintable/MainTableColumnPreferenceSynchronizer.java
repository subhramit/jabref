package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.maintable.columns.MainTableColumn;

import org.jspecify.annotations.NullMarked;

/// Applies changes of the column preferences (e.g., made in the preferences dialog) to a live main table.
///
/// The table also writes its own state back into the same preferences (see [PersistenceVisualStateTable]).
/// That echo is harmless: the table is changed with a single `setAll`, so the write-back carries exactly the
/// configured columns, which then compare equal and are ignored.
@NullMarked
class MainTableColumnPreferenceSynchronizer {

    private final TableView<BibEntryTableViewModel> table;
    private final MainTableColumnFactory columnFactory;
    private final MainTablePreferences mainTablePreferences;
    private final ListChangeListener<MainTableColumnModel> columnsListener = _ -> applyConfiguredColumns();
    private final ChangeListener<Boolean> resizeColumnsListener = (_, _, resizeColumnsToFit) -> updateColumnResizePolicy(resizeColumnsToFit);

    MainTableColumnPreferenceSynchronizer(TableView<BibEntryTableViewModel> table,
                                          MainTableColumnFactory columnFactory,
                                          MainTablePreferences mainTablePreferences) {
        this.table = table;
        this.columnFactory = columnFactory;
        this.mainTablePreferences = mainTablePreferences;
    }

    void addListeners() {
        mainTablePreferences.getColumnPreferences().getColumns().addListener(columnsListener);
        mainTablePreferences.resizeColumnsToFitProperty().addListener(resizeColumnsListener);
    }

    /// The preferences outlive the table, so the listeners have to be removed when the library tab closes.
    void dispose() {
        mainTablePreferences.getColumnPreferences().getColumns().removeListener(columnsListener);
        mainTablePreferences.resizeColumnsToFitProperty().removeListener(resizeColumnsListener);
    }

    private void applyConfiguredColumns() {
        List<MainTableColumnModel> configuredColumns = mainTablePreferences.getColumnPreferences().getColumns().stream()
                                                                           .filter(MainTableColumnModel::isConfigurable)
                                                                           .toList();
        // The first column is the hidden match category column
        List<TableColumn<BibEntryTableViewModel, ?>> shownColumns = table.getColumns().subList(1, table.getColumns().size());
        if (configuredColumns.equals(shownColumns.stream().map(MainTableColumnPreferenceSynchronizer::getModel).toList())) {
            return;
        }

        List<TableColumn<BibEntryTableViewModel, ?>> updatedColumns = new ArrayList<>();
        // Keeping the instance keeps it in the sort order. A new instance would briefly empty the sort order.
        updatedColumns.add(table.getColumns().getFirst());
        configuredColumns.stream()
                         .flatMap(model -> findColumn(shownColumns, model)
                                 .or(() -> Optional.ofNullable(columnFactory.createColumn(model)))
                                 .stream())
                         .forEach(updatedColumns::add);
        table.getColumns().setAll(updatedColumns);
    }

    private static Optional<TableColumn<BibEntryTableViewModel, ?>> findColumn(List<TableColumn<BibEntryTableViewModel, ?>> columns, MainTableColumnModel model) {
        return columns.stream()
                      .filter(column -> model.equals(getModel(column)))
                      .findFirst();
    }

    private static MainTableColumnModel getModel(TableColumn<BibEntryTableViewModel, ?> column) {
        return ((MainTableColumn<?>) column).getModel();
    }

    private void updateColumnResizePolicy(boolean resizeColumnsToFit) {
        if (resizeColumnsToFit) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        } else {
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }
    }
}
