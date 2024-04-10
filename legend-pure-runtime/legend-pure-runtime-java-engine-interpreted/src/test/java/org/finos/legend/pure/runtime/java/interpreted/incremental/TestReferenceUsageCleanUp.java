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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestReferenceUsageCleanUp extends AbstractPureTestWithCoreCompiled
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
        runtime.delete("otherId.pure");
    }

    @Test
    public void testPureRuntimePackageAsPointer() throws Exception
    {
        runtime.createInMemorySource("otherId.pure", "Class okForTestPackage::A{}");
        runtime.createInMemorySource("sourceId.pure", """
                function test():Boolean[1]
                {  \s
                   assert(1 == okForTestPackage.referenceUsages->size(), |'');
                }\
                """);

        this.compileAndExecute("test():Boolean[1]");

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("test():Boolean[1]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("The function 'test():Boolean[1]' can't be found", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", """
                    function test():Boolean[1]
                    {  \s
                       assert(1 == okForTestPackage.referenceUsages->size(), |'');
                    }\
                    """);
            this.compileAndExecute("test():Boolean[1]");
        }
    }


    @Test
    public void testPureRuntimeFunctionAsPointer() throws Exception
    {
        runtime.createInMemorySource("otherId.pure", "function f():Nil[0]{[];}");
        runtime.createInMemorySource("sourceId.pure", """
                Class okForTestPackage::A{}\
                function test():Boolean[1]
                {  \s
                   assert(1 == f__Nil_0_.referenceUsages->size(), |'');
                }\
                """);

        this.compileAndExecute("test():Boolean[1]");

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("test():Boolean[1]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("The function 'test():Boolean[1]' can't be found", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", """
                    Class okForTestPackage::A{}\
                    function test():Boolean[1]
                    {  \s
                       assert(1 == okForTestPackage.referenceUsages->size(), |'');
                    }\
                    """);
            this.compileAndExecute("test():Boolean[1]");
        }
    }


    @Test
    public void testPureRuntimeClassAFunctionReturn() throws Exception
    {
        runtime.createInMemorySource("otherId.pure", "Class A{}");
        runtime.createInMemorySource("sourceId.pure", """
                function test():A[0]
                {\
                   [];
                }\
                """);
        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            runtime.createInMemorySource("sourceId.pure", """
                    function test():A[0]
                    {  \s
                       assert(2 == A.referenceUsages->size(), |'');\
                       [];
                    }\
                    """);
            this.compileAndExecute("test():A[0]");
        }
    }

    @Test
    public void testPureRuntimeClassAFunctionParameter() throws Exception
    {
        runtime.createInMemorySource("otherId.pure", "Class A{}");
        runtime.createInMemorySource("sourceId.pure", """
                function test(a:A[1]):Nil[0]
                {\
                   [];
                }\
                """);
        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            runtime.createInMemorySource("sourceId.pure", """
                    function test(a:A[0]):Boolean[1]
                    {\
                       assert(2 == A.referenceUsages->size(), |'');
                    }\
                    """);
            this.compileAndExecute("test(A[0]):Boolean[1]", ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport));
        }
    }


    @Test
    public void testPureRuntimeFunctionWithLambdaUsedInExpression() throws Exception
    {
        runtime.createInMemorySource("otherId.pure", "Class ABCD{ok:String[1];}");
        runtime.createInMemorySource("sourceId.pure", """
                function test():Nil[0]
                {
                   ABCD.all()->filter(a|$a.ok=='eee');
                   [];
                }
                
                function go():Boolean[1]
                {  \s
                   assert(4 == ABCD.referenceUsages->size(), |'');
                }\
                """);
        this.compileAndExecute("go():Boolean[1]");

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            runtime.createInMemorySource("sourceId.pure", """
                    function test():Nil[0]
                    {
                       ABCD.all()->filter(a|$a.ok=='eee');
                       [];
                    }
                    
                    function go():Boolean[1]
                    {  \s
                       assert(4 == ABCD.referenceUsages->size(), |'');
                    }\
                    """);
            this.compileAndExecute("go():Boolean[1]");
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
