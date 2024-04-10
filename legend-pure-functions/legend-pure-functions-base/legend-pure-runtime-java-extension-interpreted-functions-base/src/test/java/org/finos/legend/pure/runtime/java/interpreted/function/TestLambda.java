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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.*;

public class TestLambda extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());

        //set observer
//        System.setProperty("pure.typeinference.test", "true");
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("inferenceTest.pure");
    }

    @AfterAll
    public static void unsetObserver()
    {
        System.clearProperty("pure.typeinference.test");
    }

    @Test
    public void testLambdaParametersInferenceWithLet()
    {
        try
        {
            compileTestSource("inferenceTest.pure", """
                    function myFunc(func:Function<{String[1],Boolean[1]->String[1]}>[1], b: Boolean[1]):String[1]
                    {
                        $func->eval('ok', $b);
                    }
                    
                    function testMany():Nil[0]
                    {
                        let l = {a,b|$a+if($b,|'eee',|'rrrr')};
                        print($l->myFunc(true)+$l->myFunc(false));
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Can't infer the parameters' types for the lambda. Please specify it in the signature.", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(8, sourceInfo.getLine());
            Assertions.assertEquals(14, sourceInfo.getColumn());
        }
    }

    @Test
    public void testLambdaParametersInferenceWithFunctionAnyAsTemplate()
    {
        try
        {
            compileTestSource("inferenceTest.pure", """
                    function myFunc(func:Function<Any>[1]):String[1]
                    {
                        'ok'
                    }
                    function testMany():String[1]
                    {
                        myFunc(a|$a+'eee');
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Can't infer the parameters' types for the lambda. Please specify it in the signature.", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(7, sourceInfo.getLine());
            Assertions.assertEquals(12, sourceInfo.getColumn());
        }
    }

    @Test
    public void testLambdaWithUnknownTypeAsParameter()
    {
        try
        {
            compileTestSource("inferenceTest.pure", """
                    function test():Nil[0]
                    {
                        print({a:Employee[1], b:Integer[1]|$b});
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Employee has not been defined!", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(3, sourceInfo.getLine());
            Assertions.assertEquals(14, sourceInfo.getColumn());
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
