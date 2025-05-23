package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IEEEQueryTransformerTest extends InfixTransformerTest<IEEEQueryTransformer> {

    @Override
    public IEEEQueryTransformer getTransformer() {
        return new IEEEQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "author:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "publication_title:";
    }

    @Override
    public String getTitlePrefix() {
        return "article_title:";
    }

    @Override
    @Test
    public void convertJournalFieldPrefix() throws QueryNodeParseException {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(Optional.of("Nature"), transformer.getJournal());
    }

    @Override
    @Test
    public void convertYearField() throws QueryNodeParseException {
        // IEEE does not support year range
        // Thus, a generic test does not work

        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year:2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(Optional.of(2021), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }

    @Override
    @Test
    public void convertYearRangeField() throws QueryNodeParseException {
        IEEEQueryTransformer transformer = getTransformer();

        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        transformer.transformLuceneQuery(luceneQuery);

        assertEquals(Optional.of(2018), transformer.getStartYear());
        assertEquals(Optional.of(2021), transformer.getEndYear());
    }

    private static Stream<Arguments> getTitleTestData() {
        return Stream.of(
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers AND Portal AND Newcomers", "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers"),
                Arguments.of("Overcoming AND Open AND Source AND Project AND Entry AND Barriers", "Overcoming Open Source Project Entry Barriers"),
                Arguments.of(null, "and")
        );
    }

    @ParameterizedTest
    @MethodSource("getTitleTestData")
    void stopWordRemoval(String expected, String queryString) throws QueryNodeParseException {
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> result = getTransformer().transformLuceneQuery(luceneQuery);
        assertEquals(Optional.ofNullable(expected), result);
    }
}
