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

package org.finos.legend.pure.runtime.java.compiled.modeling.valueSpec;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNilTypeCompiled extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testNilCastToIntegerReturnValueMany() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testFn1():Integer[*]
                {
                    []
                }
                """);

        this.assertNilType(this.execute("test::testFn1():Integer[*]"));
    }

    @Test
    public void testNilCastToIntegerReturnValueZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testFn2():Integer[0..1]
                {
                    []
                }
                """);

        this.assertNilType(this.execute("test::testFn2():Integer[0..1]"));
    }

    @Test
    public void testNilVariableCastToStringReturnValueMany() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testFn3():String[*]
                {
                    let x = [];
                    $x;
                }
                """);

        this.assertNilType(this.execute("test::testFn3():String[*]"));
    }

    @Test
    public void testNilVariableCastToStringReturnValueZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testFn4():String[0..1]
                {
                    let x = [];
                    $x;
                }
                """);

        this.assertNilType(this.execute("test::testFn4():String[0..1]"));
    }

    @Test
    public void testNilAsStringParamMany() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper1(strings:String[*]):String[1]
                {
                    joinStrings($strings, '')
                }
                
                function test::testFn5():String[1]
                {
                    test::testHelper1([])
                }
                """);

        this.assertValue("", this.execute("test::testFn5():String[1]"));
    }

    @Test
    public void testNilVariableAsIntegerParamMany() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper2(nums:Integer[*]):Integer[1]
                {
                    plus($nums)
                }
                
                function test::testFn6():Integer[1]
                {
                    let x = [];
                    test::testHelper2($x);
                }
                """);

        this.assertValue(0L, this.execute("test::testFn6():Integer[1]"));
    }

    @Test
    public void testNilAsPairParamZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper3(y:Pair<Integer,Any>[0..1]):Integer[1]
                {
                    if($y->isEmpty(), |7, |$y->toOne().first)
                }
                
                function test::testFn7():Integer[1]
                {
                    test::testHelper3([])
                }
                """);

        this.assertValue(7L, this.execute("test::testFn7():Integer[1]"));
    }

    @Test
    public void testNilVariableAsPairParamZeroOne() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper3(y:Pair<Integer,Any>[0..1]):Integer[1]
                {
                    if($y->isEmpty(), |7, |$y->toOne().first)
                }
                
                function test::testFn8():Integer[1]
                {
                    let x = [];
                    test::testHelper3($x);
                }
                """);

        this.assertValue(7L, this.execute("test::testFn8():Integer[1]"));
    }

    @Test
    public void testNilVariableAsParam() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper1(strings:String[*]):String[1]
                {
                    joinStrings($strings, '')
                }
                
                function test::testHelper2(nums:Integer[*]):Integer[1]
                {
                    plus($nums)
                }
                function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1]
                {
                   ^Pair<U,V>(first=$first, second=$second);
                }
                function test::testFn9():Pair<Number,String>[1]
                {
                    let x = [];
                    let y = test::testHelper2($x);
                    let z = test::testHelper1($x);
                    pair($y, $z);
                }
                """);
        Pair resultPair = (Pair) Iterate.getFirst(((InstanceValue) this.execute("test::testFn9():Pair[1]"))._values());
        Assertions.assertEquals(0L, resultPair._first());
        Assertions.assertEquals("", resultPair._second());
    }

    @Test
    public void testNilTypeAsReturnValue() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testHelper4(strings:String[*]): Nil[0]
                {
                   [];
                }
                
                function test::testFn10():String[0..1]
                {
                   let x = [];
                   test::testHelper4($x);
                }
                """);

        this.assertNilType(this.execute("test::testFn10():String[0..1]"));
    }

    @Test
    public void testNilWithAddInMap() throws Exception
    {
        compileTestSource("fromString.pure", """
                function test::testFn():String[1]
                {
                  let dummy = [];
                  ['a', 'b', 'c', 'd']->map(s | add($dummy, $s))->joinStrings(', ');
                }
                """);
        assertValue("a, b, c, d", this.execute("test::testFn():String[1]"));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    private void assertNilType(CoreInstance instance)
    {
        Assertions.assertTrue(this.processorSupport.valueSpecification_instanceOf(instance, M3Paths.Nil));
    }

    private void assertValue(Object expected, CoreInstance instance)
    {
        Assertions.assertEquals(expected, Iterate.getFirst(((InstanceValue) instance)._values()));
    }
}
