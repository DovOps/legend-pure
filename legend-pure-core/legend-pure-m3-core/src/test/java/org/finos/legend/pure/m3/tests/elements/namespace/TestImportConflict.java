// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements.namespace;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestImportConflict extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testImportTypeConflict() throws Exception
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    import a::*;
                    import b::*;
                    Class a::Employee
                    {
                       a:String[1];
                    }
                    Class b::Employee
                    {
                        b: String[1];
                    }
                    function test():Nil[0]
                    {
                        print(Employee);
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Employee has been found more than one time in the imports: [a::Employee, b::Employee]", "fromString.pure", 13, 11, 13, 11, 13, 18, e);
        }
    }

    @Test
    public void testImportTypeNonConflict() throws Exception
    {
        runtime.createInMemorySource("fromString.pure",
                """
                import a::*;
                import a::*;
                Class a::Employee
                {
                   a:String[1];
                }
                function test():Nil[0]
                {
                    print(Employee,1);
                }
                """);
        runtime.compile();
    }
}
