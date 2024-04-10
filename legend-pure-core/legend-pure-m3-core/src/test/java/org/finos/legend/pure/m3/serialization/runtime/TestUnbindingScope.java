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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class TestUnbindingScope extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    // Class deletion / should unbind

    @Test
    public void testScopeOfFunctionUndbindingForClass_ReturnType()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn():A[*]
                {
                    []
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn():A[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ReturnTypeGeneric()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn():List<A>[*]
                {
                    []
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn():List[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ParameterType()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(a:A[1]):String[*]
                {
                    []
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(A[1]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_ParameterTypeGeneric()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(a:List<A>[1]):String[*]
                {
                    []
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(List[1]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_New()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(name:String[1]):Any[1]
                {
                    ^A(name=$name)
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_NewGeneric()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(name:String[1]):Any[1]
                {
                    ^List<A>()
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_Cast()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(a:Any[1]):String[1]
                {
                    $a->cast(@A).name
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(Any[1]):String[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Explicit_CastGeneric()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(a:Any[1]):String[*]
                {
                    $a->cast(@List<A>).values->map(v | $v.name)
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(Any[1]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Inferred()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::newA(name:String[1]):A[1]
                {
                    ^A(name=$name)
                }
                
                function test::getAName(a:A[1]):String[1]
                {
                    $a.name
                }
                
                function test::testFn(name:String[1]):String[1]
                {
                    $name->newA()->getAName()
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_FunctionBody_Inferred2()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::newA(name:String[1]):A[1]
                {
                    ^A(name=$name)
                }
                
                function test::testFn(name:String[1]):Any[1]
                {
                    $name->newA()
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):Any[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Explicit_New()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(names:String[*]):Any[*]
                {
                    $names->map(name | ^A(name=$name))
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[*]):Any[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Explicit_Cast()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(things:Any[*]):String[*]
                {
                    $things->map(thing | $thing->cast(@A).name)
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(Any[*]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Inferred()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::newA(name:String[1]):A[1]
                {
                    ^A(name=$name)
                }
                
                function test::getAName(a:A[1]):String[1]
                {
                    $a.name
                }
                
                function test::testFn(names:String[*]):String[*]
                {
                    $names->map(name | $name->newA()->getAName())
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[*]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Lambda_Inferred2()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::newA(name:String[1]):A[1]
                {
                    ^A(name=$name)
                }
                
                function test::testFn(names:String[*]):Any[*]
                {
                    $names->map(name | $name->newA())
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[*]):Any[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    // Class deletion / should NOT unbind

    @Test
    public void testScopeOfFunctionUndbindingForClass_Unrelated()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(string:String[1]):String[1]
                {
                    $string
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertTrue(testFn.hasBeenValidated());
    }

    @Test
    public void testScopeOfFunctionUndbindingForClass_Indirect()
    {
        compileTestSource("/test/source1.pure",
                """
                Class test::A
                {
                    name:String[1];
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::getName(name:String[1]):String[1]
                {
                    ^A(name=$name).name
                }
                
                function test::testFn(string:String[1]):String[1]
                {
                    $string->getName()
                }
                """);
        CoreInstance getName = runtime.getFunction("test::getName(String[1]):String[1]");
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[1]");
        Assertions.assertTrue(getName.hasBeenValidated());
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(getName.hasBeenProcessed());
        Assertions.assertTrue(testFn.hasBeenValidated());
    }

    // Function deletion / should unbind

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Direct()
    {
        compileTestSource("/test/source1.pure",
                """
                function test::testJoinStrings(strings:String[*]):String[1]
                {
                    $strings->joinStrings(' ')
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(string:String[1]):String[1]
                {
                    $string->split('\\t')->testJoinStrings()
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    @Test
    public void testScopeOfFunctionUnbindingForFunction_DirectInLambda()
    {
        compileTestSource("/test/source1.pure",
                """
                function test::testJoinStrings(strings:String[*]):String[1]
                {
                    $strings->joinStrings(' ')
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(strings:String[*]):String[*]
                {
                    $strings->map(string | $string->split('\\t')->testJoinStrings())
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[*]):String[*]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(testFn.hasBeenProcessed());
    }

    // Function deletion / should NOT unbind

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Unrelated()
    {
        compileTestSource("/test/source1.pure",
                """
                function test::testJoinStrings(strings:String[*]):String[1]
                {
                    $strings->joinStrings(' ')
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::testFn(string:String[1]):String[1]
                {
                    $string->replace('a', 'b')
                }
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[1]");
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertTrue(testFn.hasBeenValidated());
    }

    @Test
    public void testScopeOfFunctionUnbindingForFunction_Indirect()
    {
        compileTestSource("/test/source1.pure",
                """
                function test::testJoinStrings(strings:String[*]):String[1]
                {
                    $strings->joinStrings(' ')
                }
                """);
        compileTestSource("/test/source2.pure",
                """
                import test::*;
                function test::replaceTabsWithSpaces(strings:String[*]):String[*]
                {
                    $strings->map(s | $s->split('\\t')->testJoinStrings())
                }
                
                function test::testFn(string:String[1]):String[*]
                {
                    $string->split('\\n')->replaceTabsWithSpaces()
                }
                """);
        CoreInstance replaceTabsWithSpaces = runtime.getFunction("test::replaceTabsWithSpaces(String[*]):String[*]");
        CoreInstance testFn = runtime.getFunction("test::testFn(String[1]):String[*]");
        Assertions.assertTrue(replaceTabsWithSpaces.hasBeenValidated());
        Assertions.assertTrue(testFn.hasBeenValidated());
        runtime.delete("/test/source1.pure");
        runtime.getIncrementalCompiler().unload();
        Assertions.assertFalse(replaceTabsWithSpaces.hasBeenProcessed());
        Assertions.assertTrue(testFn.hasBeenValidated());
    }
}
