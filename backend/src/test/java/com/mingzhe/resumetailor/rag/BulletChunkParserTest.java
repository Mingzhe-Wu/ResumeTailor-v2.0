package com.mingzhe.resumetailor.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulletChunkParserTest {

    private final BulletChunkParser parser = new BulletChunkParser();

    @Test
    void returnsNoChunksForNullOrBlankInput() {
        assertEquals(List.of(), parser.parseBullets(null));
        assertEquals(List.of(), parser.parseBullets(""));
        assertEquals(List.of(), parser.parseBullets(" \t\r\n  \n"));
    }

    @Test
    void trimsLeadingAndTrailingWhitespaceBeforeSplitting() {
        assertEquals(
                List.of("Built REST APIs", "Fixed production defects"),
                parser.parseBullets(
                        """

                          * Built REST APIs
                          * Fixed production defects

                        """
                )
        );
    }

    @Test
    void splitsStarSeparatedInput() {
        assertEquals(
                List.of("Built REST APIs", "Fixed production defects"),
                parser.parseBullets("* Built REST APIs * Fixed production defects")
        );
    }

    @Test
    void splitsNewlineSeparatedInput() {
        assertEquals(
                List.of("Built REST APIs", "Fixed production defects"),
                parser.parseBullets(
                        """
                        Built REST APIs
                        Fixed production defects
                        """
                )
        );
    }

    @Test
    void splitsWindowsLineBreaks() {
        assertEquals(
                List.of("Built REST APIs", "Fixed production defects"),
                parser.parseBullets("Built REST APIs\r\nFixed production defects")
        );
    }

    @Test
    void removesChunksCreatedByRepeatedSeparators() {
        assertEquals(
                List.of("Built APIs", "Fixed defects"),
                parser.parseBullets("*** Built APIs ** Fixed defects *")
        );
        assertEquals(
                List.of("Built APIs", "Fixed defects"),
                parser.parseBullets("Built APIs\n\n\nFixed defects")
        );
    }

    @Test
    void keepsSingleContinuousParagraphAsOneChunk() {
        String paragraph =
                "I worked on REST APIs, database queries, validation, and production debugging.";

        assertEquals(List.of(paragraph), parser.parseBullets(paragraph));
    }

    @Test
    void givesStarSeparatorsPriorityOverLineBreaks() {
        assertEquals(
                List.of(
                        "Context line",
                        "Built REST APIs\nwith request validation",
                        "Fixed production defects"
                ),
                parser.parseBullets(
                        """
                        Context line
                        * Built REST APIs
                        with request validation
                        * Fixed production defects
                        """
                )
        );
    }
}
