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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPrint extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFunctionPrint()
    {
        runtime.createInMemoryAndCompile(Tuples.pair("testSource.pure",
                        """
                        function testFunction():String[1]
                        {
                           'Test'
                        }
                        function testFunction2():String[1]
                        {
                           testFunction()
                        }
                        """),
                Tuples.pair(
                        "testSource2.pure",
                        """
                        function go():Nil[0]
                        {
                           print(testFunction__String_1_,0);
                        }\
                        """
                ));
        this.execute("go():Nil[0]");
        Assertions.assertEquals("""
                testFunction__String_1_ instance ConcreteFunctionDefinition
                    applications(Property):
                        [>0] Anonymous_StripedId instance SimpleFunctionExpression
                    classifierGenericType(Property):
                        [>0] Anonymous_StripedId instance GenericType
                    expressionSequence(Property):
                        [>0] Anonymous_StripedId instance InstanceValue
                    functionName(Property):
                        [>0] testFunction instance String
                    name(Property):
                        [>0] testFunction__String_1_ instance String
                    package(Property):
                        [X] Root instance Package
                    referenceUsages(Property):
                        [>0] Anonymous_StripedId instance ReferenceUsage\
                """, functionExecution.getConsole().getLine(0));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
