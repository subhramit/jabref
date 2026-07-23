package org.jabref.logic.openoffice.bst;

import org.jspecify.annotations.NullMarked;

/// Converts HTML-like output into a string that [org.jabref.model.openoffice.ootext.OOTextIntoOO]
/// can write into a LibreOffice document.
///
/// Supports both Pandoc HTML (primary target) and a best-effort subset of SnuggleTeX XHTML.
/// For Pandoc: unwraps outer <p>, maps <em>/<strong>/smallcaps span, then delegates to
/// [BstStyleUtils.transformHTML]. For SnuggleTeX: strips XML declaration, outer html/body wrappers,
/// xmlns attributes, maps common tags to OOText, then delegates to [BstStyleUtils].
@NullMarked
public final class BstHtmlToOOText {

    private BstHtmlToOOText() {
    }

    /// Converts an HTML fragment to an OOText-compatible string.
    public static String convert(String html) {
        String s = html.trim();
        boolean likelySnuggle = s.startsWith("<?xml") || s.contains("xmlns=") || s.contains("<html") || s.contains("<body");

        if (likelySnuggle) {
            // SnuggleTeX XHTML hygiene: strip XML declaration, doctype, and outer html/head/body wrappers
            s = s.replaceFirst("^\\s*<\\?xml[^>]*>\\s*", "");
            s = s.replaceFirst("^\\s*<!DOCTYPE[^>]*>\\s*", "");
            s = s.replaceAll("</?html[^>]*>", "");
            s = s.replaceAll("</?head[^>]*>.*?</head>", "");
            s = s.replaceAll("</?body[^>]*>", "");

            // Snuggle pretty-prints: single newlines are just formatting.
            // Treat blank lines as paragraph separators and collapse other newlines to spaces.
            s = s.replaceAll("\\R{2,}", "<p></p>");
            s = s.replaceAll("\\R", " ");
            s = s.replaceAll(" {2,}", " ");
        }

        // Unwrap outer <p>…</p>; internal paragraph boundaries become <p></p>.
        s = s.replaceAll("(?s)</p>\\s*<p>", "<p></p>");
        s = s.replaceAll("(?s)^<p>", "");
        s = s.replaceAll("(?s)</p>$", "");

        // Map semantic tags to OOText inline tag set (both Pandoc and Snuggle forms, attributes tolerated)
        s = s.replaceAll("(?s)<em[^>]*>(.*?)</em>", "<i>$1</i>");
        s = s.replaceAll("(?s)<strong[^>]*>(.*?)</strong>", "<b>$1</b>");
        // Small caps via class or inline style
        s = s.replaceAll("(?s)<span[^>]*class=\"[^\"]*smallcaps[^\"]*\"[^>]*>(.*?)</span>", "<smallcaps>$1</smallcaps>");
        s = s.replaceAll("(?s)<span[^>]*style=\"[^\"]*small-caps[^\"]*\"[^>]*>(.*?)</span>", "<smallcaps>$1</smallcaps>");

        // Delegate remaining cleanup to shared BST HTML transforms (entity decoding, <div>/<a>/<span> removal, newline handling)
        return BstStyleUtils.transformHTML(s);
    }
}
