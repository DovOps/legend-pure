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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCollectionMixedTypes extends AbstractPureTestWithCoreCompiled
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
    public void testSimple()
    {
        compileTestSource("fromString.pure", """
                Class Employee{lastName:String[1];}
                function test():GenericType[1]
                {
                    let a = [^Employee(lastName='William'),'a string',123,false];
                    $a->genericType();
                }
                """);
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assertions.assertEquals("Any", GenericType.print(genericType, processorSupport));
    }

    @Test
    public void testWithTypeArguments()
    {
        compileTestSource("fromString.pure", """
                Class MyType<T>{}
                Class A{}
                Class B extends A{}
                Class C{}
                function test():GenericType[2]
                {
                    let a = [^MyType<A>(), ^MyType<B>()];
                    let b = [^MyType<A>(), ^MyType<B>(), ^MyType<C>()];
                    [$a->genericType(), $b->genericType()];
                }
                """);
        CoreInstance result = execute("test():GenericType[2]");
        ListIterable<? extends CoreInstance> genericTypes = result.getValueForMetaPropertyToMany(M3Properties.values);
        Assertions.assertEquals("MyType<A>", GenericType.print(genericTypes.get(0), processorSupport));
        Assertions.assertEquals("MyType<Any>", GenericType.print(genericTypes.get(1), processorSupport));
    }

    @Test
    public void testWithTypeMultiplicities()
    {
        compileTestSource("fromString.pure", """
                Class MyType<|m>{}
                function test():GenericType[1]
                {
                    let argpa = [^MyType<|1..2>(), ^MyType<|3..5>()];
                    $argpa->genericType();
                }
                """);
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assertions.assertEquals("MyType<|1..5>", GenericType.print(genericType, processorSupport));
    }

    @Test
    public void testWithTypeMultiplicitiesWithMany()
    {
        compileTestSource("fromString.pure", """
                Class MyType<|m>{}
                function test():GenericType[1]
                {
                    let argpa = [^MyType<|1..2>(), ^MyType<|3..5>(), ^MyType<|*>()];
                    $argpa->genericType();
                }
                """);
        CoreInstance result = execute("test():GenericType[1]");
        CoreInstance genericType = result.getValueForMetaPropertyToOne(M3Properties.values);
        Assertions.assertEquals("MyType<|*>", GenericType.print(genericType, processorSupport));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
