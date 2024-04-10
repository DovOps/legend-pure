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

package org.finos.legend.pure.runtime.java.interpreted.function.base.lang;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestNewAtRuntime;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNew extends AbstractTestNewAtRuntime
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage());
    }

    @Test
    public void testNewWithMultiplicityParameter()
    {
        compileTestSource("fromString.pure",
                """
                Class MyClass<|m>
                {
                  value:String[m];
                }
                
                function testFn():Any[*]
                {
                  ^MyClass<|1>(value='hello');
                }\
                """);
        execute("testFn():Any[*]");
        // TODO add asserts
    }

    @Test
    public void testNewWithMissingOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                """
                function test(): Any[*]
                {
                   ^test::Owner(firstName='John', lastName='Roe')
                }
                
                Class
                test::Car
                {
                   name : String[1];
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'car' requires 1 value, got 0", "fromString.pure", 3, 4, e);
    }

    @Test
    public void testNewWithMissingOneToManyProperty()
    {
        compileTestSource("fromString.pure",
                """
                function test(): Any[*]
                {
                   ^test::Owner(firstName='John', lastName='Roe')
                }
                
                Class
                test::Car
                {
                   name : String[1];
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'cars' requires 1..* values, got 0", "fromString.pure", 3, 4, e);
    }

    @Test
    public void testNewWithChildWithMismatchedReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                """
                function test(): Any[*]
                {
                   ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', car=^test::Car(name='Audi')))
                }
                
                Class
                test::Car
                {
                   name : String[1];
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating the type 'Owner'. The property 'car' has a multiplicity range of [1] when the given list has a cardinality equal to 2", "fromString.pure", 3, 4, e);
    }

    @Override
    @Test
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure",
                """
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'car' requires 1 value, got 0", "fromString.pure", 3, 14, e);
    }

    @Override
    @Test
    public void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure",
                """
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Error instantiating class 'Owner'.  The following properties have multiplicity violations: 'cars' requires 1..* values, got 0", "fromString.pure", 3, 14, e);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
