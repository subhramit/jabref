package org.jabref.logic.net;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.jabref.logic.util.URLUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class URLUtilTest {

    @Test
    void cleanGoogleSearchURL() {
        // empty text
        assertEquals("", URLUtil.cleanGoogleSearchURL(""));
        assertEquals(" ", URLUtil.cleanGoogleSearchURL(" "));
        // no URL
        assertEquals("this is no url!", URLUtil.cleanGoogleSearchURL("this is no url!"));
        // no Google search URL
        assertEquals("http://dl.acm.org/citation.cfm?id=321811", URLUtil.cleanGoogleSearchURL("http://dl.acm.org/citation.cfm?id=321811"));
        // malformed Google URL
        assertEquals("https://www.google.de/url♥", URLUtil.cleanGoogleSearchURL("https://www.google.de/url♥"));
        // no queries
        assertEquals("https://www.google.de/url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url"));
        assertEquals("https://www.google.de/url?", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?"));
        // no multiple queries
        assertEquals("https://www.google.de/url?key=value", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key=value"));
        // no key values
        assertEquals("https://www.google.de/url?key", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key"));
        assertEquals("https://www.google.de/url?url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url"));
        assertEquals("https://www.google.de/url?key=", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key="));
        // no url param
        assertEquals("https://www.google.de/url?key=value&key2=value2", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key=value&key2=value2"));
        // no url param value
        assertEquals("https://www.google.de/url?url=", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url="));
        // url param value no URL
        assertEquals("https://www.google.de/url?url=this+is+no+url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url=this+is+no+url"));
        // Http
        assertEquals(
                "http://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("http://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=http%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
        // Https
        assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
        // root domain
        assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
        // foreign domain
        assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
        // foreign domain co.uk
        assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.co.uk/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
        // accept ftp results
        assertEquals(
                "ftp://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=ftp%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w"));
    }

    @Test
    void isURLshouldAcceptValidURL() {
        assertTrue(URLUtil.isURL("http://www.google.com"));
        assertTrue(URLUtil.isURL("https://www.google.com"));
    }

    @Test
    void isURLshouldRejectInvalidURL() {
        assertFalse(URLUtil.isURL("www.google.com"));
        assertFalse(URLUtil.isURL("google.com"));
    }

    @Test
    void isURLshouldRejectEmbeddedURL() {
        assertFalse(URLUtil.isURL("dblp computer science bibliography, http://dblp.org"));
    }

    @Test
    void createUriShouldHandlePipeCharacter() {
        String input = "http://example.com/test|file";
        URI uri = URLUtil.createUri(input);
        assertEquals("http://example.com/test%7Cfile", uri.toString());
    }

    @Test
    void validUrl() throws MalformedURLException {
        String input = "http://example.com";

        URL result = URLUtil.create(input);
        assertNotNull(result);
        assertNotEquals("", result.toString().trim());
        assertEquals(input, result.toString());
    }

    @Test
    void nullUrl() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                URLUtil.create(null));
        assertTrue(exception.getMessage().contains("null or empty"));
    }

    @Test
    void emptyUrl() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                URLUtil.create("   "));
        assertTrue(exception.getMessage().contains("null or empty"));
    }

    @Test
    void uriMissingScheme() {
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("www.example.com"));
        assertTrue(exception.getMessage().contains("not absolute"));
    }

    @Test
    void uriMissingHost() {
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("mailto:someone@example.com"));
        assertTrue(exception.getMessage().contains("must include both scheme and host"));
    }

    @Test
    void malformedSyntax() {
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("http://[invalid-url]"));
        assertTrue(exception.getMessage().contains("Invalid URI"));
    }
}
