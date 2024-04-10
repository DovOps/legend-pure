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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestCopyAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testCopyWithReverseZeroToOneProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));
                   print($newCar.owner.car->size()->toString(), 1);
                   print($newCar.owner->size()->toString(), 1);
                   print($newCar.name, 1);
                   $car;
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
                   car  : test::Car[0..1];
                }\
                """);
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse properties for a zero-to-one association.");
        }
    }

    @Test
    public void testCopyIncrementIntegerProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', accidents=0);
                   let newCar = ^$car(accidents = $car.accidents + 1);
                   print($newCar.accidents, 1);
                   $car;
                }
                
                Class
                test::Car
                {
                   name : String[1];
                   accidents: Integer[1];
                }
                
                """);
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("1", functionExecution.getConsole().getLine(0).substring(0,1));
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            Assertions.fail("Assert.failed to increment the Integer property on Copy.");
        }
    }

    @Test
    public void testCopyWithReverseZeroToManyProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));
                   print($newCar.owner.cars->size()->toString(), 1);
                   print($newCar.owner->size()->toString(), 1);
                   print($newCar.name, 1);
                   $car;
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
                   cars  : test::Car[0..*];
                }\
                """);
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse properties for a zero-to-many association.");
        }
    }

    @Test
    public void testCopyWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));
                   print($newCar.owner.car->size()->toString(), 1);
                   print($newCar.owner->size()->toString(), 1);
                   print($newCar.name, 1);
                   $car;
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
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Test
    public void testCopyWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   let newCar = ^$car(name='Veyron', owner=^test::Owner(firstName='John', lastName='Roe'));
                   print($newCar.owner.cars->size()->toString(), 1);
                   print($newCar.owner->size()->toString(), 1);
                   print($newCar.name, 1);
                   $car;
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
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Veyron'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse properties for a one-to-many association.");
        }
    }

    @Test
    public void testCopyWithChildWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', cars=[^test::Car(name='Audi')]));
                   let newCar = ^$car(name='Veyron');
                   print($newCar.owner.cars->size()->toString(), 1);
                   print($newCar.owner.cars->sortBy(c|$c.name)->at(0).name, 1);
                   print($newCar.owner.cars->sortBy(c|$c.name)->at(1).name, 1);
                   print($newCar.owner.cars->sortBy(c|$c.name)->at(2).name, 1);
                   $car;\
                }
                function meta::pure::functions::collection::sortBy<T,U|m>(col:T[m], key:Function<{T[1]->U[1]}>[0..1]):T[m]
                {
                    sort($col, $key, [])
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
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'3'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'Audi'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Bugatti'", functionExecution.getConsole().getLine(2));
            Assertions.assertEquals("'Veyron'", functionExecution.getConsole().getLine(3));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse property of a child for a one-to-many association.");
        }
    }

    @Test
    public void testCopyWithRedefinedManyToManyAssociation()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let john = ^test::Owner(firstName='John', lastName='Roe');
                   let pierre = ^$john(firstName='Pierre', lastName='Doe');
                  \s
                   let audi = ^test::Car(name='Audi', owners=[$john]);
                   let bugatti = ^$audi(name='Bugatti', owners=[$pierre]);
                  \s
                   print($john.cars->size()->toString(), 1);
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
                   owners : test::Owner[0..*];
                   cars  : test::Car[0..*];
                }\
                """);
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("'1'", functionExecution.getConsole().getLine(0));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse property of a child for a many-to-many association.");
        }
    }

    @Disabled
    @Test
    public void testCopyWithRedefinedOneToOneAssociation()
    {
        compileTestSource("fromString.pure","""
                function test(): Any[*]
                {
                   let audi = ^test::Car(name='Audi');
                   let bugatti = ^$audi(name='Bugatti');
                  \s
                   let john = ^test::Owner(firstName='John', lastName='Roe', car=$audi);
                   let pierre = ^$john(firstName='Pierre', lastName='Doe', car=$bugatti);
                
                   print($audi.owner->isEmpty()->toString());
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
                   owner : test::Owner[0..1];
                   car  : test::Car[0..1];
                }\
                """);
        try
        {
            this.execute("test():Any[*]");
            Assertions.assertEquals("false", functionExecution.getConsole().getLine(0));
        }
        catch (Exception e)
        {
            Assertions.fail("Assert.failed to set the reverse property of a child for a many-to-many association.");
        }
    }

    @Test
    public void testCopyParametrizedClassWithEmptyPropertiesSet()
    {
        String source =
                """
                Class A<T1, T2>\s
                { prop1:T1[*];
                 prop2:T2[*]; }
                Class B<T>\s
                { prop1:String[*];
                 prop2:T[*]; }
                function test::testFn():Any[*] { let a = ^A<String, Integer>(prop1='string', prop2=[1,2]); let a1 = ^$a(prop2=[]); ^B<Integer>(prop1='Hello', prop2=[]);}
                """;
        compileTestSource("fromString.pure",source);
        this.compileAndExecute("test::testFn():Any[*]");
    }

    @Test
    public void testSourceInformationCopy()
    {
        String source =
                        """
                        function test::testFn():Any[*] {\
                           let x0 = meta::pure::functions::collection::removeDuplicates_T_MANY__T_MANY_->evaluateAndDeactivate();
                           let x1 = ^$x0();
                           let x2 = ^$x0(expressionSequence = $x0.expressionSequence);
                        \s
                           assert($x0->sourceInformation().source == $x1->sourceInformation().source, |'');
                           assert($x0->sourceInformation().source != $x2->sourceInformation().source, |'');\
                        }
                        """;
        compileTestSource("fromString.pure",source);
        this.compileAndExecute("test::testFn():Any[*]");
    }
}
