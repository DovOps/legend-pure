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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestIfCompiled extends AbstractPureTestWithCoreCompiled
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
        runtime.compile();
    }

    @Test
    public void testUnAssignedIfInFuncExpression()
    {
        compileTestSource(
                "fromString.pure",
                """
                function meta::pure::functions::lang::tests::if::testUnAssignedIfInFuncExpression():String[1]
                {
                   let ifVar =  if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););
                   if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););
                   if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););
                   let iff = if(true, | let b = 'true', | if(true, | let b = 'true', | 'false'););
                   if(true, | let b = 'true'; if(true, | let b = 'true', | 'false'); let bb = 'bb';, | let c = 'see');
                   let a = 'be';
                   if(true, | let b = 'true', | 'false');
                }\
                """);
    }

    @Test
    public void testIfWithDifferentMultiplicities()
    {
        compileTestSource(
                "fromString.pure",
                """
                Class A
                {
                  id : Integer[1];
                }
                
                function testFn(ids:Integer[*]):A[*]
                {
                  if ($ids->isEmpty(),
                      | let id = -1;
                        ^A(id=$id);,
                      | let newIds = $ids->tail();
                        $ids->map(id | $newIds->testFn());)
                }\
                """);
    }

    @Test
    public void testIfWithFloatVersusInteger()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", """
                import test::*;
                function test::testFn(test:Boolean[1]):Number[1]
                {
                    let result = if($test, |1.0, |3);
                    $result;
                }
                
                function test::testTrue():Any[*]
                {
                  let result = testFn(true);
                  assert(1.0 == $result, |'');
                  $result;
                }
                
                function test::testFalse():Any[*]
                {
                  let result = testFn(false);
                  assert(3 == $result, |'');
                  $result;
                }
                """);

        CoreInstance testTrue = runtime.getFunction("test::testTrue():Any[*]");
        Assertions.assertNotNull(testTrue);
        CoreInstance resultTrue = functionExecution.start(testTrue, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultTrue);
        InstanceValue trueInstanceValue = (InstanceValue) resultTrue;
        Verify.assertSize(1, trueInstanceValue._values());
        Object trueValue = trueInstanceValue._values().getFirst();
        Verify.assertInstanceOf(Double.class, trueValue);
        Assertions.assertEquals(1.0d, trueValue);

        CoreInstance testFalse = runtime.getFunction("test::testFalse():Any[*]");
        Assertions.assertNotNull(testFalse);
        CoreInstance resultFalse = functionExecution.start(testFalse, Lists.immutable.empty());
        Verify.assertInstanceOf(InstanceValue.class, resultFalse);
        InstanceValue falseInstanceValue = (InstanceValue) resultFalse;
        Verify.assertSize(1, falseInstanceValue._values());
        Object falseValue = falseInstanceValue._values().getFirst();
        Verify.assertInstanceOf(Long.class, falseValue);
        Assertions.assertEquals(3L, falseValue);
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
