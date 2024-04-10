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

package org.finos.legend.pure.m3.tests.elements.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestProperty extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testNewWithProperty()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A{name : String[1];}
                    function myFunc():A[1]
                    {
                        ^A(nameError = 'ok');
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The property 'nameError' can't be found in the type 'A' or in its hierarchy.", 4, 8, e);
        }
    }

    @Test
    public void testNewWithPropertySpaceError()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A{'first name' : String[1];}
                    function myFunc():A[1]
                    {
                        ^A('firstname' = 'ok');
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertPureException(PureCompilationException.class, "The property 'firstname' can't be found in the type 'A' or in its hierarchy.", 4, 8, e);
        }
    }

    @Test
    public void testNewWithPropertySpaceOk()
    {
        compileTestSource("fromString.pure", """
                Class A{'first name' : String[1];}
                function myFunc():A[1]
                {
                    ^A('first name' = 'ok');
                }
                """);

    }

    @Test
    public void testPropertyAccessOk()
    {
        compileTestSource("fromString.pure", """
                Class A{'first name' : String[1];}
                function myFunc():A[1]
                {
                    A.all()->filter(a|$a.'first name' == 'ok')->toOne();
                }
                """);

    }

    @Test
    public void testPropertyAccessError()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A{'first name' : String[1];}
                    function myFunc():A[1]
                    {
                        A.all()->filter(a|$a.'first _name' == 'ok')->toOne();
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Can't find the property 'first _name' in the class A", 4, 26, e);
        }
    }

}
