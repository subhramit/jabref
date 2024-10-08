package org.jabref.gui.libraryproperties.keypattern;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.libraryproperties.PropertiesTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class KeyPatternPropertiesView extends AbstractPropertiesTabView<KeyPatternPropertiesViewModel> implements PropertiesTab {

    @FXML private Button keyPatternHelp;
    @FXML private CitationKeyPatternsPanel bibtexKeyPatternTable;

    @Inject private GuiPreferences preferences;
    @Inject private BibEntryTypesManager bibEntryTypesManager;

    public KeyPatternPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Citation key patterns");
    }

    public void initialize() {
        this.viewModel = new KeyPatternPropertiesViewModel(databaseContext, preferences);

        bibtexKeyPatternTable.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        bibtexKeyPatternTable.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP_KEY_PATTERNS, new HelpAction(HelpFile.CITATION_KEY_PATTERN, dialogService, preferences.getExternalApplicationsPreferences()), keyPatternHelp);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        bibtexKeyPatternTable.setValues(
                bibEntryTypesManager.getAllTypes(databaseContext.getMetaData().getMode()
                                                                .orElse(preferences.getLibraryPreferences()
                                                                                   .getDefaultBibDatabaseMode())),
                databaseContext.getMetaData().getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences().getKeyPatterns()));
    }

    @FXML
    public void resetAllKeyPatterns() {
        bibtexKeyPatternTable.resetAll();
    }
}
