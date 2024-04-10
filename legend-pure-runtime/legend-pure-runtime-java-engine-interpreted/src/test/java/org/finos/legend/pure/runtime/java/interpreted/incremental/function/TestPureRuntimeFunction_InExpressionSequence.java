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

package org.finos.legend.pure.runtime.java.interpreted.incremental.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeFunction_InExpressionSequence extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
    }

    @Test
    public void testPureRuntimeFunctionBodyDependencies() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFunc',1);}");
        runtime.createInMemorySource("userId.pure", "function go():Nil[0]{sourceFunction();}");
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The system can't find a match for the function: sourceFunction()", "userId.pure", 1, 22, e);
            }

            runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFuncYeah!', 1);}");
            this.compileAndExecute("go():Nil[0]");
            Assertions.assertEquals("'theFuncYeah!'", functionExecution.getConsole().getLine(0));
        }
        runtime.delete("sourceId.pure");
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Nil[0]{print('theFunc', 1);}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }

    @Test
    public void testPureRuntimeFunctionBodyDependenciesError() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        runtime.createInMemorySource("userId.pure", """
                function test():String[1]{sourceFunction()}\
                function go():Nil[0]{print(test(),1);}\
                """);
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "The system can't find a match for the function: sourceFunction()", "userId.pure", 1, 27, e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "Return type error in function 'test'; found: Integer; expected: String", "userId.pure", 1, 27, e);
            }
        }
        runtime.delete("sourceId.pure");
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
