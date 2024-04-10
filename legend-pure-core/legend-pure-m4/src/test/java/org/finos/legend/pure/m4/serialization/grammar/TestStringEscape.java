// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m4.serialization.grammar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestStringEscape
{
    private static final String NO_ESCAPES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_+*/()[]{}<>-=#@$%^&!,.?~`:;|\"";

    @Test
    public void testEscape()
    {
        Assertions.assertEquals(NO_ESCAPES, StringEscape.escape(NO_ESCAPES));

        Assertions.assertEquals("\\'", StringEscape.escape("'"));
        Assertions.assertEquals("\\n", StringEscape.escape("\n"));
        Assertions.assertEquals("\\r", StringEscape.escape("\r"));
        Assertions.assertEquals("\\t", StringEscape.escape("\t"));
        Assertions.assertEquals("\\b", StringEscape.escape("\b"));
        Assertions.assertEquals("\\f", StringEscape.escape("\f"));
        Assertions.assertEquals("\\\\", StringEscape.escape("\\"));

        Assertions.assertEquals("\\\\\\n", StringEscape.escape("\\\n"));
        Assertions.assertEquals("\\\\\\'", StringEscape.escape("\\'"));

        Assertions.assertEquals("The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\"", StringEscape.escape("The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\""));
    }

    @Test
    public void testUnescape()
    {
        Assertions.assertEquals(NO_ESCAPES, StringEscape.unescape(NO_ESCAPES));

        Assertions.assertEquals("\n", StringEscape.unescape("\\n"));
        Assertions.assertEquals("\r", StringEscape.unescape("\\r"));
        Assertions.assertEquals("\t", StringEscape.unescape("\\t"));
        Assertions.assertEquals("\b", StringEscape.unescape("\\b"));
        Assertions.assertEquals("\f", StringEscape.unescape("\\f"));
        Assertions.assertEquals("\\", StringEscape.unescape("\\\\"));
        Assertions.assertEquals("'", StringEscape.unescape("\\'"));

        Assertions.assertEquals("\\\"", StringEscape.unescape("\\\\\""));
        Assertions.assertEquals("\\\n", StringEscape.unescape("\\\\\\n"));
        Assertions.assertEquals("\\n", StringEscape.unescape("\\\\n"));

        Assertions.assertEquals("a", StringEscape.unescape("\\a"));
        Assertions.assertEquals("\"", StringEscape.unescape("\\\""));

        Assertions.assertEquals("The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\"", StringEscape.unescape("The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\""));
    }

    @Test
    public void testRoundTrip()
    {
        Assertions.assertEquals(NO_ESCAPES, StringEscape.unescape(StringEscape.escape(NO_ESCAPES)));
        Assertions.assertEquals(NO_ESCAPES, StringEscape.escape(StringEscape.unescape(NO_ESCAPES)));

        String quickBrownFox = "The\tQuick\nBrown\rFox\bJumps\f'Over' The\\Lazy \"Dog\"";
        Assertions.assertEquals(quickBrownFox, StringEscape.unescape(StringEscape.escape(quickBrownFox)));

        String quickBrownFoxEscaped = "The\\tQuick\\nBrown\\rFox\\bJumps\\f\\'Over\\' The\\\\Lazy \"Dog\"";
        Assertions.assertEquals(quickBrownFoxEscaped, StringEscape.escape(StringEscape.unescape(quickBrownFoxEscaped)));

        String slashNewline = "\\\n";
        Assertions.assertEquals(slashNewline, StringEscape.unescape(StringEscape.escape(slashNewline)));

        String slashNewlineEscaped = "\\\\\\n";
        Assertions.assertEquals(slashNewlineEscaped, StringEscape.escape(StringEscape.unescape(slashNewlineEscaped)));

        String slashN = "\\n";
        Assertions.assertEquals(slashN, StringEscape.unescape(StringEscape.escape(slashN)));

        String slashNEscaped = "\\\\n";
        Assertions.assertEquals(slashNEscaped, StringEscape.escape(StringEscape.unescape(slashNEscaped)));

        Assertions.assertEquals("a", StringEscape.escape(StringEscape.unescape("\\a")));
    }
}
