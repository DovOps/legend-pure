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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestLetCompiled extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        AbstractPureTestWithCoreCompiled.runtime.delete("fromString.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Test
    public void testLetCollectionWithAnyCastIssue()
    {
        try
        {
            AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", """
                    function testLetCollectionWithAnyCastIssue():Boolean[1]
                    {
                        let a = [A, B];
                        if($a->size() == 2, | true, | false);\
                    }
                    Class A\
                    {\
                    }
                    Class B\
                    {\
                    }\
                    """);
            MutableList<? extends Object> l = FastList.<Object>newListWith("");
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    public void testLetLarge()
    {
        String reallyLargeString = getLargeString();

        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", "function testLargeString():String[1]\n" +
                "{\n" +
                "    let a = \'" + reallyLargeString + "\';\n" +
                "    $a;" +
                "}\n");

    }

    @Test
    public void testLetLargeAlreadySplit()
    {
        String reallyLargeString = getLargeString();

        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", "function testLargeString():String[1]\n" +
                "{\n" +
                "    let a = \'" + reallyLargeString + "\' + \'" + reallyLargeString + "\';\n" +
                "    $a;" +
                "}\n");

    }

    private String getLargeString()
    {
        String reallyLargeString = "";

        for (int i = 0; i <= 6600; i++)
        {
            reallyLargeString += "aaaaaaaaaa";
        }

        Assertions.assertTrue(reallyLargeString.length() > 65535, "Large string not large enough:" + reallyLargeString.length());
        return reallyLargeString;
    }


}
