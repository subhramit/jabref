package org.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.support.DisabledOnCIServerWindows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Need to run on JavaFX thread since we are parsing FXML files
@ExtendWith(ApplicationExtension.class)
@DisabledOnCIServerWindows("Needs DISPLAY variable to be set")
class LocalizationConsistencyTest {

    @Test
    void allFilesMustBeInLanguages() throws IOException {
        String bundle = "JabRef";
        // e.g., "<bundle>_en.properties", where <bundle> is [JabRef, Menu]
        Pattern propertiesFile = Pattern.compile("%s_.{2,}.properties".formatted(bundle));
        Set<String> localizationFiles = new HashSet<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of("src/main/resources/l10n"))) {
            for (Path fullPath : directoryStream) {
                String fileName = fullPath.getFileName().toString();
                if (propertiesFile.matcher(fileName).matches()) {
                    localizationFiles.add(fileName.substring(bundle.length() + 1, fileName.length() - ".properties".length()));
                }
            }
        }

        Set<String> knownLanguages = Stream.of(Language.values())
                                           .map(Language::getId)
                                           .collect(Collectors.toSet());
        assertEquals(knownLanguages, localizationFiles, "There are some localization files that are not present in org.jabref.logic.l10n.Language or vice versa!");
    }

    @Test
    void ensureNoDuplicates() {
        String bundle = "JabRef";
        for (Language lang : Language.values()) {
            String propertyFilePath = "/l10n/%s_%s.properties".formatted(bundle, lang.getId());

            // read in
            DuplicationDetectionProperties properties = new DuplicationDetectionProperties();
            try (InputStream is = LocalizationConsistencyTest.class.getResourceAsStream(propertyFilePath)) {
                assert is != null;
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String> duplicates = properties.getDuplicates();

            assertEquals(List.of(), duplicates, "Duplicate keys inside bundle " + bundle + "_" + lang.getId());
        }
    }

    @Test
    void keyValueShouldBeEqualForEnglishPropertiesMessages() {
        Properties englishKeys = LocalizationParser.getProperties("/l10n/%s_%s.properties".formatted("JabRef", "en"));
        for (Map.Entry<Object, Object> entry : englishKeys.entrySet()) {
            String expectedKeyEqualsKey = "%s=%s".formatted(entry.getKey(), entry.getKey().toString().replace("\n", "\\n"));
            String actualKeyEqualsValue = "%s=%s".formatted(entry.getKey(), entry.getValue().toString().replace("\n", "\\n"));
            assertEquals(expectedKeyEqualsKey, actualKeyEqualsValue);
        }
    }

    @Test
    void languageKeysShouldNotContainUnderscoresForSpaces() throws IOException {
        final List<LocalizationEntry> quotedEntries = LocalizationParser
                .findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG)
                .stream()
                .filter(key -> key.getKey().contains("\\_"))
                .collect(Collectors.toList());
        assertEquals(List.of(), quotedEntries,
                "Language keys must not use underscores for spaces! Use \"This is a message\" instead of \"This_is_a_message\".\n" +
                        "Please correct the following entries:\n" +
                        quotedEntries
                                .stream()
                                .map(key -> "\n%s (%s)\n".formatted(key.getKey(), key.getPath()))
                                .toList());
    }

    @Test
    void languageKeysShouldNotContainHtmlBrAndHtmlP() throws IOException {
        final List<LocalizationEntry> entriesWithHtml = LocalizationParser
                .findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG)
                .stream()
                .filter(key -> key.getKey().contains("<br>") || key.getKey().contains("<p>"))
                .collect(Collectors.toList());
        assertEquals(List.of(), entriesWithHtml,
                "Language keys must not contain HTML <br> or <p>. Use \\n for a line break.\n" +
                        "Please correct the following entries:\n" +
                        entriesWithHtml
                                .stream()
                                .map(key -> "\n%s (%s)\n".formatted(key.getKey(), key.getPath()))
                                .toList());
    }

    @Test
    void findMissingLocalizationKeys() throws IOException {
        List<LocalizationEntry> missingKeys = new ArrayList<>(LocalizationParser.findMissingKeys(LocalizationBundleForTest.LANG));
        assertEquals(List.of(), missingKeys,
                missingKeys.stream()
                           .map(key -> LocalizationKey.fromKey(key.getKey()))
                           .map(key -> "%s=%s".formatted(
                                   key.getEscapedPropertiesKey(),
                                   key.getValueForEnglishPropertiesFile()))
                           .collect(Collectors.joining("\n",
                                   """

                                           DETECTED LANGUAGE KEYS WHICH ARE NOT IN THE ENGLISH LANGUAGE FILE.
                                           PASTE THESE INTO THE ENGLISH LANGUAGE FILE "JabRef_en.properties".
                                           Search for a proper place; typically related keys are grouped together.
                                           If a similar key is already present, please adapt your language instead of adding load to translators by adding a new key.

                                           """,
                                   "\n\n")));
    }

    @Test
    void findObsoleteLocalizationKeys() throws IOException {
        Set<String> obsoleteKeys = LocalizationParser.findObsolete(LocalizationBundleForTest.LANG);
        assertEquals(Set.of(), obsoleteKeys,
                obsoleteKeys.stream().collect(Collectors.joining("\n",
                        "Obsolete keys found in language properties file: \n\n",
                        """

                                1. CHECK IF THE KEY IS REALLY NOT USED ANYMORE.
                                2. REMOVE THESE FROM THE ENGLISH LANGUAGE FILE "JabRef_en.properties".

                                """))
        );
    }

    @Test
    void localizationParameterMustIncludeAString() throws IOException {
        // Must start with "
        // - Localization.lang("test")
        // - Localization.lang("test %1", var)
        // - Localization.lang("Problem downloading from %1", address)
        // - Localization.lang("test %1 %2", var1, var2)
        Set<LocalizationEntry> keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.LANG);
        for (LocalizationEntry e : keys) {
            // TODO: Forbidden Localization.lang("test" + var2) not covered by the test
            //       In case this kind of code is found, an error should be reported
            //       JabRef's infrastructure only supports Localization.lang("Some Message"); and not something else.
            assertTrue(e.getKey().startsWith("\""), "Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey());
        }

        keys = LocalizationParser.findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest.MENU);
        for (LocalizationEntry e : keys) {
            assertTrue(e.getKey().startsWith("\""), "Illegal localization parameter found. Must include a String with potential concatenation or replacement parameters. Illegal parameter: Localization.lang(" + e.getKey());
        }
    }

    private static Language[] installedLanguages() {
        return Language.values();
    }

    @ParameterizedTest
    @MethodSource("installedLanguages")
    void resourceBundleExists(Language language) {
        Path messagesPropertyFile = Path.of("src/main/resources").resolve(Localization.RESOURCE_PREFIX + "_" + language.getId() + ".properties");
        assertTrue(Files.exists(messagesPropertyFile));
    }

    @ParameterizedTest
    @MethodSource("installedLanguages")
    void languageCanBeLoaded(Language language) {
        Locale oldLocale = Locale.getDefault();
        try {
            Locale locale = Language.convertToSupportedLocale(language).get();
            Locale.setDefault(locale);
            ResourceBundle messages = ResourceBundle.getBundle(Localization.RESOURCE_PREFIX, locale);
            assertNotNull(messages);
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

    private static class DuplicationDetectionProperties extends Properties {

        @Serial private static final long serialVersionUID = 1L;

        private final List<String> duplicates = new ArrayList<>();

        DuplicationDetectionProperties() {
            super();
        }

        /**
         * Overriding the HashTable put() so we can check for duplicates
         */
        @Override
        public synchronized Object put(Object key, Object value) {
            // Have we seen this key before?
            if (containsKey(key)) {
                duplicates.add(String.valueOf(key));
            }

            return super.put(key, value);
        }

        List<String> getDuplicates() {
            return duplicates;
        }
    }
}
