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

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestNewAtRuntime;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNewCompiled extends AbstractTestNewAtRuntime
{
    @BeforeAll
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), AbstractTestNewAtRuntime.getCodeStorage(), JavaModelFactoryRegistryLoader.loader());
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected void assertNewNilException(Exception e)
    {
        assertOriginatingPureException(PureExecutionException.class, "Cannot instantiate meta::pure::metamodel::type::Nil", e);
    }

    @Override
    @Test
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.car->size()->toString(), 1);
                   $car;\
                }
                
                Class
                test::Car
                {
                   name : String[1];
                }
                
                Class
                test::FastCar extends test::Car
                {
                   owner : test::Owner[1];
                }
                Class
                test::Owner
                {
                   firstName: String[1];
                   lastName: String[1];
                }
                
                Association test::Car_Owner
                {
                   owner : test::Owner[1];
                   car  : test::Car[1];
                }\
                """);
        try
        {
            execute("test():Any[*]");
            String result = AbstractPureTestWithCoreCompiled.functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'0'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Override
    @Test
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::FastCar(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.cars->size()->toString(), 1);
                   $car;\
                }
                
                Class
                test::Car
                {
                   name : String[1];
                }
                
                Class
                test::FastCar extends test::Car
                {
                   owner : test::Owner[1];
                }
                Class
                test::Owner
                {
                   firstName: String[1];
                   lastName: String[1];
                }
                
                Association test::Car_Owner
                {
                   owner : test::Owner[1];
                   cars  : test::Car[1..*];
                }\
                """);
        try
        {
            execute("test():Any[*]");
            String result = AbstractPureTestWithCoreCompiled.functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'0'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }
}
