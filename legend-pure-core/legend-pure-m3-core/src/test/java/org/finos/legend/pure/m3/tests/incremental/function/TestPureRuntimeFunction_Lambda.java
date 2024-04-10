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

package org.finos.legend.pure.m3.tests.incremental.function;

import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class TestPureRuntimeFunction_Lambda extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("other.pure");
        runtime.delete("userId.pure");
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testPureRuntimeFunctionLambdaCollection() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{} function f(a:A[1]):String[1]{'ok1'}")
                        .createInMemorySource("userId.pure", "function go():Any[1]{^B(a=^A())->match([b:B[1]|f($b.a)+'ok2']);}\n")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}")
                        .compileWithExpectedCompileFailure("B has not been defined!", "userId.pure", 1, 23)
                        .updateSource("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{} function f(a:A[1]):String[1]{'ok'}")
                        .compile()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{} function f(a:A[1]):String[1]{'ok1'}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());

    }


    @Test
    public void testPureRuntimeFunctionLambdaCollectionError() throws Exception
    {
        //TODO
/*
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{}")
                        .createInMemorySource("userId.pure", "function go():Nil[0]{print(^B(a=^A())->match([b:B[1]|f($b.a)+'ok']));}\n")
                        .createInMemorySource("other.pure", " function f(a:A[1]):String[1]{'ok'}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("sourceId.pure")
                        .createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}")
                        .compileWithExpectedCompileFailure("(A|B) has not been defined!", "userId.pure", 1, 42)
                        .createInMemorySource("sourceId.pure", "Class A{version : Integer[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
*/

        runtime.createInMemorySource("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{}");
        runtime.createInMemorySource("userId.pure", "function go():Any[1]{^B(a=^A())->match([b:B[1]|fz($b.a)+'ok']);}\n");
        runtime.createInMemorySource("other.pure", " function fz(a:A[1]):String[1]{'ok'}");
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, Pattern.compile("(A|B) has not been defined!"), e);
            }

            try
            {
                runtime.modify("sourceId.pure", "Class Test{} Class C{} Class B{a:C[1];} Class A{}");
                runtime.compile();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "fz(_:C[1])\n" +
                        PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                        "\tfz(A[1]):String[1]\n" +
                        PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, "userId.pure", 1, 48, e);
            }
        }
        runtime.modify("sourceId.pure", "Class Test{} Class B{a:A[1];} Class A{}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }
}
