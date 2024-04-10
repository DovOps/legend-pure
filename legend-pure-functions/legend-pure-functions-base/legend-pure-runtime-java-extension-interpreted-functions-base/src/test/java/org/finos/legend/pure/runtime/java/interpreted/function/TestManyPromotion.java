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

package org.finos.legend.pure.runtime.java.interpreted.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestManyPromotion extends AbstractPureTestWithCoreCompiled
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
        runtime.compile();
    }

    @Test
    public void testString()
    {
        compileTestSource("fromString.pure", """
                function func():String[*]
                {
                    'ok';
                }
                function test():Nil[0]
                {
                    print(func(),1);\
                }
                """);
        this.execute("test():Nil[0]");
        Assertions.assertEquals("'ok'", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testStringError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    function func():String[1]
                    {
                        ['ok','ok2'];
                    }
                    function test():Nil[0]
                    {
                        print(func());\
                    }
                    """);
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return multiplicity error in function 'func'; found: [2]; expected: [1]", "fromString.pure", 3, 5, e);
        }
    }


    @Test
    public void testFunctionMatchingMultiplicity()
    {
        compileTestSource("fromString.pure", """
                function func(a:Any[1]):Any[*]
                {
                    $a;
                }
                function test():Nil[0]
                {
                    print(func(test__Nil_0_)->size(), 1);\
                    print(test__Nil_0_->map(c|func($c))->size(), 1);\
                }
                """);
        this.execute("test():Nil[0]");
        Assertions.assertEquals("1", functionExecution.getConsole().getLine(0));
        Assertions.assertEquals("1", functionExecution.getConsole().getLine(1));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
