package org.jabref.logic.openoffice.bst;

import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;

import org.jspecify.annotations.NullMarked;

/// Converts LaTeX fragments to XHTML using SnuggleTeX (pure Java, in-process).
///
/// TEMPORARY adapter to allow testing the BST → HTML → OOText pipeline without
/// requiring an external pandoc installation. Remove or gate behind a switch
/// before merge.
@NullMarked
public class SnuggleLatexConverter {

    private final SnuggleEngine engine = new SnuggleEngine();

    public boolean isAvailable() {
        // SnuggleTeX is on the classpath as a transitive dependency
        return true;
    }

    /// Converts a LaTeX fragment to XHTML. Returns SnuggleTeX's XML string.
    public String latexToHtml(String latex) {
        SnuggleSession session = engine.createSession();
        try {
            session.parseInput(new SnuggleInput(latex));
            return session.buildXMLString();
        } catch (Exception e) {
            // Soft-fail for testing: return the original text so downstream logic can proceed
            return latex;
        }
    }
}
