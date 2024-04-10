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

import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeFunction_AsFunctionExpressionParam extends AbstractPureTestWithCoreCompiled
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
    public void testPureRuntimeFunctionParamDependencies() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
        runtime.createInMemorySource("userId.pure", """
                function fix(s:String[1]):String[1]{$s}
                function go():Nil[0]{print(fix(sourceFunction()),1);}\
                """);
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 2, 32, e);
            }

            runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFuncYeah!';}");
            this.compileAndExecute("go():Nil[0]");
            Assertions.assertEquals("'theFuncYeah!'", functionExecution.getConsole().getLine(0));

            runtime.delete("sourceId.pure");
            runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc';}");
            this.compileAndExecute("go():Nil[0]");
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
        }
    }

    @Test
    public void testPureRuntimeFunctionParamDependenciesError() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", """
                function fix(s:String[1]):String[1]{$s}
                function go():Nil[0]{print(fix(sourceFunction()),1);}\
                """);
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 2, 32, e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "fix(_:Integer[1])\n" +
                        PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                        "\tfix(String[1]):String[1]\n" +
                        PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 2, 28, e);
            }
        }

        runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }

    @Test
    public void testPureRuntimeFunctionParamDependenciesTypeInference() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", """
                function fix(s:String[1]):String[1]{$s}
                function myFunction<T>(p:T[1]):T[1]{$p}
                function go():Nil[0]{print(fix(myFunction(sourceFunction())),1)}
                """);
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("'theFunc'", functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("go():Nil[0]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "sourceFunction()", "userId.pure", 3, 43, e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "fix(_:Integer[1])\n" +
                        PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                        "\tfix(String[1]):String[1]\n" +
                        PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 3, 28, e);
            }
        }
        runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
