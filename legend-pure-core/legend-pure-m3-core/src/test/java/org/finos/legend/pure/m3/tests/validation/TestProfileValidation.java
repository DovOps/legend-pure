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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestProfileValidation extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final String SOURCE_ID = "/test/profileTest.pure";

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete(SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testStereotypeNameConflict()
    {
        String code = """
                Profile test::BadProfile
                {
                    stereotypes : [abc, def, abc, ghi, def];
                }
                """;
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(SOURCE_ID, code));
        Assertions.assertEquals(new SourceInformation(SOURCE_ID, 3, 30, 3, 30, 3, 32), e.getSourceInformation());
        Assertions.assertEquals("There is already a stereotype named 'abc' defined in test::BadProfile (at " + SOURCE_ID + " line:3 column:20)", e.getInfo());
    }

    @Test
    public void testTagNameConflict()
    {
        String code = """
                Profile test::BadProfile
                {
                    tags : [abc, xyz, def, xyz, abc];
                }
                """;
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(SOURCE_ID, code));
        Assertions.assertEquals(new SourceInformation(SOURCE_ID, 3, 28, 3, 28, 3, 30), e.getSourceInformation());
        Assertions.assertEquals("There is already a tag named 'xyz' defined in test::BadProfile (at " + SOURCE_ID + " line:3 column:18)", e.getInfo());
    }
}
