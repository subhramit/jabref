package org.jabref.logic.importer.fileformat;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Articles in the medline format can be downloaded from http://www.ncbi.nlm.nih.gov/pubmed/.
 * <ol>
 *   <li>Search for a term and make sure you have selected the <strong>PubMed</strong> database.</li>
 *   <li>Select the results you want to export by checking their checkboxes.</li>
 *   <li>Press on the <strong>'Send to'</strong> drop down menu on top of the search results.</li>
 *   <li>Select <strong>'File'</strong> as Destination and <strong>'XML'</strong> as Format.</li>
 *   <li>Press <strong>'Create File'</strong> to download your search results in a medline xml file.</li>
 * </ol>
 */
class MedlineImporterTest {

    private MedlineImporter importer;

    @BeforeEach
    void setUp() {
        this.importer = new MedlineImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Medline/PubMed", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("medline", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.MEDLINE, importer.getFileType());
    }
}
