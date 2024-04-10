// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.string;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestSubstring extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testStart()
    {
           compileTestSource("substring.pure",
                    """
                    function testStart():Boolean[1]
                    {
                        let string = 'the quick brown fox jumps over the lazy dog';
                        assertEquals('the quick brown fox jumps over the lazy dog', substring($string, 0));
                        assertEquals('he quick brown fox jumps over the lazy dog', substring($string, 1));
                        assertEquals('e quick brown fox jumps over the lazy dog', substring($string, 2));
                        assertEquals(' quick brown fox jumps over the lazy dog', substring($string, 3));
                        assertEquals('quick brown fox jumps over the lazy dog', substring($string, 4));
                    }\
                    """);
            this.execute("testStart():Boolean[1]");
    }

    @Test
    public void testStartEnd()
    {
       compileTestSource("substring.pure",
                """
                function testStartEnd():Boolean[1]
                {
                    let string = 'the quick brown fox jumps over the lazy dog';
                    assertEquals('the quick brown fox jumps over the lazy dog', substring($string, 0, 43));
                    assertEquals('he quick brown fox jumps over the lazy do', substring($string, 1, 42));
                    assertEquals('e quick brown fox jumps over the lazy d', substring($string, 2, 41));
                    assertEquals(' quick brown fox jumps over the lazy ', substring($string, 3, 40));
                    assertEquals('quick brown fox jumps over the lazy', substring($string, 4, 39));
                }
                """);
        this.execute("testStartEnd():Boolean[1]");
    }

    @Test
    public void testStartWithReflection()
    {
        compileTestSource("substring.pure",
                """
                function testStart():Boolean[1]
                {
                    let string = 'the quick brown fox jumps over the lazy dog';
                    assertEquals('the quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 0));
                    assertEquals('he quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 1));
                    assertEquals('e quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 2));
                    assertEquals(' quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 3));
                    assertEquals('quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__String_1_->eval($string, 4));
                }\
                """);
        this.execute("testStart():Boolean[1]");
    }

    @Test
    public void testStartEndWithReflection()
    {
        compileTestSource("substring.pure",
                """
                function testStartEnd():Boolean[1]
                {
                    let string = 'the quick brown fox jumps over the lazy dog';
                    assertEquals('the quick brown fox jumps over the lazy dog', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 0, 43));
                    assertEquals('he quick brown fox jumps over the lazy do', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 1, 42));
                    assertEquals('e quick brown fox jumps over the lazy d', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 2, 41));
                    assertEquals(' quick brown fox jumps over the lazy ', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 3, 40));
                    assertEquals('quick brown fox jumps over the lazy', substring_String_1__Integer_1__Integer_1__String_1_->eval($string, 4, 39));
                }
                """);
        this.execute("testStartEnd():Boolean[1]");
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("substring.pure");
        runtime.compile();
    }
}
