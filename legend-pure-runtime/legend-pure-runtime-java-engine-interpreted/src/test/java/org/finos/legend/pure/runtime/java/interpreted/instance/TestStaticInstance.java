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

package org.finos.legend.pure.runtime.java.interpreted.instance;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestStaticInstance extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testStaticInstance()
    {
        compileTestSource("fromString.pure", """
                Class Person
                {
                   lastName:String[1];
                }
                ^Person p (lastName='last')
                function testGet():Nil[0]
                {
                    print(p, 1);
                }
                """);
        this.execute("testGet():Nil[0]");
        Assertions.assertEquals("""
                p instance Person
                    lastName(Property):
                        last instance String\
                """, functionExecution.getConsole().getLine(0));
    }


    @Test
    public void testGetterFromStaticInstance()
    {
        compileTestSource("fromString.pure", """
                Class Person
                {
                   lastName:String[1];
                }
                ^Person a (lastName='last')
                function testGet():Nil[0]
                {
                    print(a.lastName, 1);
                }
                """);
        this.execute("testGet():Nil[0]");
        Assertions.assertEquals("'last'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testGetterFromStaticInstanceWithWrongProperty()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class test::Person
                    {
                       lastName:String[1];
                    }
                    ^test::Person p (lastName='last')
                    function testGet():Nil[0]
                    {
                        print(p.wrongProperty);
                    }\
                    """);
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Can't find the property 'wrongProperty' in the class test::Person", 8, 13, e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}