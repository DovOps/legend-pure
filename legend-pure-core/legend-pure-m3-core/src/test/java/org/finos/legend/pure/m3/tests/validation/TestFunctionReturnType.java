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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestFunctionReturnType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("testSource1.pure");
        runtime.delete("testSource2.pure");
        runtime.delete("testSource3.pure");
    }

    @Test
    public void testSimpleReturnError()
    {
        try
        {
            compileTestSource("testSource.pure",
                    """
                    Class A
                    {
                       name : String[1];
                    }
                    Class B extends A
                    {
                       moreName : String[1];
                    }
                    function funcWithReturn():B[1]
                    {
                       ^A(name='aaa');
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'funcWithReturn'; found: A; expected: B", "testSource.pure", 11, 4, e);
        }
    }

    @Test
    public void testSimpleReturnErrorSameClassNameInDifferentPackages()
    {
        try
        {
            compileTestSource("testSource.pure",
                    """
                    Class b::A
                    {
                       name : String[1];
                    }
                    Class c::A
                    {
                       moreName : String[1];
                    }
                    function funcWithReturn():c::A[1]
                    {
                       ^b::A(name='aaa');
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'funcWithReturn'; found: b::A; expected: c::A", "testSource.pure", 11, 4, e);
        }
    }

    @Test
    public void testReturnTypeWithGenerics()
    {
        try
        {
            compileTestSource("testSource.pure",
                    """
                    import test::*;
                    Class test::TestClass<T>
                    {
                       prop : T[1];
                    }
                    function funcWithReturn():TestClass<String>[1]
                    {
                       ^TestClass<Integer>(prop=5);
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'funcWithReturn'; found: test::TestClass<Integer>; expected: test::TestClass<String>", "testSource.pure", 8, 4, e);
        }
    }

    @Test
    public void testReturnTypeWithTypeParameter()
    {
        // This should compile
        compileTestSource("testSource1.pure",
                """
                function func1WithReturn<T>(t:T[*]):T[*]
                {
                  $t
                }
                """);

        // This should compile
        compileTestSource("testSource2.pure",
                """
                function func2WithReturn<T>(t:T[*]):T[*]
                {
                  []
                }
                """);

        try
        {
            // This should not compile
            compileTestSource("testSource3.pure",
                    """
                    function func3WithReturn<T>(t:T[*]):T[*]
                    {
                      5
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'func3WithReturn'; found: Integer; expected: T", "testSource3.pure", 3, 3, e);
        }
    }

    @Test
    public void testReturnTypeWithTypeParameterInGenerics()
    {
        // This should compile
        compileTestSource("testSource1.pure",
                """
                function func1WithReturn<T>(t:T[*]):List<T>[1]
                {
                  ^List<T>(values=$t)
                }
                """);

        // This should compile
        compileTestSource("testSource2.pure",
                """
                function func2WithReturn<T>(t:T[*]):List<T>[1]
                {
                  ^List<Nil>()
                }
                """);

        try
        {
            // This should not compile
            compileTestSource("testSource3.pure",
                    """
                    function func3WithReturn<T>(t:T[*]):List<T>[1]
                    {
                      ^List<Integer>(values=5)
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'func3WithReturn'; found: meta::pure::functions::collection::List<Integer>; expected: meta::pure::functions::collection::List<T>", "testSource3.pure", 3, 3, e);
        }

    }

    @Test
    public void testReturnTypeWithFunctionType()
    {
        try
        {
            compileTestSource("testSource.pure",
                    """
                    import test::*;
                    Class test::TestClass
                    {
                      prop : String[1];
                    }
                    function funcWithReturn():FunctionDefinition<{TestClass[1]->Integer[1]}>[1]
                    {
                       {tc:TestClass[1] | $tc.prop}
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'funcWithReturn'; found: meta::pure::metamodel::function::LambdaFunction<{test::TestClass[1]->String[1]}>; expected: meta::pure::metamodel::function::FunctionDefinition<{test::TestClass[1]->Integer[1]}>", "testSource.pure", 8, 5, e);
        }
    }

    @Test
    public void testReturnTypeWithFunctionTypeAndTypeParametersInReturnValueType()
    {
        // This should compile
        compileTestSourceM3("testSource1.pure",
                """
                import test::*;
                function test::func1(c:Class<Any>[0..1]):List<Any>[1]
                {
                    ^List<Any>()
                }
                function test::funcPairs1():Pair<String, Function<{Class<Any>[0..1]->List<Any>[1]}>>[*]
                {
                  [
                   pair('Key1', func1_Class_$0_1$__List_1_),
                   pair('Key2', func1_Class_$0_1$__List_1_)
                  ]
                }
                """);
        try
        {
            // This should not compile
            compileTestSourceM3("testSource2.pure",
                    """
                    import test::*;
                    function test::func2(c:Class<Type>[0..1]):List<Type>[1]
                    {
                        ^List<Type>()
                    }
                    function test::funcPairs2():Pair<String, Function<{Class<Any>[0..1]->List<Any>[1]}>>[*]
                    {
                      [
                       pair('Key1', func2_Class_$0_1$__List_1_),
                       pair('Key2', func2_Class_$0_1$__List_1_)
                      ]
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            assertPureException(PureCompilationException.class, "Return type error in function 'funcPairs2'; found: meta::pure::functions::collection::Pair<String, meta::pure::metamodel::function::ConcreteFunctionDefinition<{meta::pure::metamodel::type::Class<meta::pure::metamodel::type::Type>[0..1]->meta::pure::functions::collection::List<meta::pure::metamodel::type::Type>[1]}>>; expected: meta::pure::functions::collection::Pair<String, meta::pure::metamodel::function::Function<{meta::pure::metamodel::type::Class<meta::pure::metamodel::type::Any>[0..1]->meta::pure::functions::collection::List<meta::pure::metamodel::type::Any>[1]}>>", "testSource2.pure", 8, 3, e);
        }
    }
}
