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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestByteCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testByteUsageAsFunctionParameter()
    {
        compileTestSource("fromString.pure",
                """
                function myByteFunction(a:Byte[1], b:Byte[*]):String[1]
                {
                   'aa';
                }\
                """);
    }

    @Test
    public void testByteUsageAsClassProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                    """
                    Class myClassWithByteProperty
                    {
                       prop: Byte[1];
                    }\
                    """));
        Assertions.assertEquals("Compilation error at (resource:fromString.pure line:3 column:4), \"The property 'prop' has type of 'Byte'. 'Byte' type is not supported for property.\"", e.getMessage());
    }
}
