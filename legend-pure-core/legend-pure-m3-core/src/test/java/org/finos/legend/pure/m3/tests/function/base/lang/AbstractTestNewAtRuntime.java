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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestNewAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("/test/testModel.pure");
        runtime.compile();
    }

    @Test
    public void testGetterFromDynamicInstanceWithWrongProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class test::Person
                {
                   lastName:String[1];
                }
                function testGet():Nil[0]
                {
                   let p = ^test::Person(lastName='last');
                   print($p.wrongProperty);
                }\
                """));
        assertPureException(PureCompilationException.class, "Can't find the property 'wrongProperty' in the class test::Person", 8, 13, e);
    }

    @Test
    public void testNewWithInvalidProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Person
                {
                   lastName:String[1];
                }
                function testNew():Person[1]
                {
                   ^Person(lastName='last', wrongProperty='wrong');
                }\
                """));
        assertPureException(PureCompilationException.class, "The property 'wrongProperty' can't be found in the type 'Person' or in its hierarchy.", 7, 29, e);
    }

    @Test
    public void testNewNil() throws Exception
    {
        compileTestSource("fromString.pure", """
                function testNewNil():Nil[1]
                {
                    ^Nil();
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNewNil():Nil[1]"));
        assertNewNilException(e);
    }

    protected void assertNewNilException(Exception e)
    {
        assertPureException(PureExecutionException.class, "Cannot instantiate meta::pure::metamodel::type::Nil", 3, 5, e);
    }

    @Test
    public void testNewWithReverseZeroToOneProperty()
    {
        compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.car->size()->toString(), 1);
                   $car;\
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
            execute("test():Any[*]");
            String result = functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a zero-to-one association.");
        }
    }

    @Test
    public void testNewWithReverseZeroToManyProperty()
    {
        compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.cars->size()->toString(), 1);
                   $car;\
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
            execute("test():Any[*]");
            String result = functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a zero-to-many association.");
        }
    }

    @Test
    public void testNewWithReverseOneToOneProperty()
    {
        compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.car->size()->toString(), 1);
                   $car;\
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
            execute("test():Any[*]");
            String result = functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a one-to-one association.");
        }
    }

    @Test
    public void testNewWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe'));
                   print($car.owner.cars->size()->toString(), 1);
                   $car;\
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
            execute("test():Any[*]");
            String result = functionExecution.getConsole().getLine(0);
            Assertions.assertEquals("'1'", result);
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse properties for a one-to-many association.");
        }
    }

    @Test
    public void testNewWithChildWithReverseOneToManyProperty()
    {
        compileTestSource("fromString.pure", """
                function test(): Any[*]
                {
                   let car = ^test::Car(name='Bugatti', owner= ^test::Owner(firstName='John', lastName='Roe', cars=[^test::Car(name='Audi')]));
                   print($car.owner.cars->size()->toString(), 1);
                   print($car.owner.cars->sortBy(c|$c.name)->at(0).name, 1);
                   print($car.owner.cars->sortBy(c|$c.name)->at(1).name, 1);
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
            execute("test():Any[*]");
            Assertions.assertEquals("'2'", functionExecution.getConsole().getLine(0));
            Assertions.assertEquals("'Audi'", functionExecution.getConsole().getLine(1));
            Assertions.assertEquals("'Bugatti'", functionExecution.getConsole().getLine(2));
        }
        catch (Exception e)
        {
            Assertions.fail("Failed to set the reverse property of a child for a one-to-many association.");
        }
    }

    @Test
    public void testNewWithZeroToOneAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                """
                import test::*;
                Class test::TestClassA
                {
                  name : String[1];
                }
                
                Class test::TestClassB
                {
                }
                
                Association test::TestAssocAB
                {
                  toB : TestClassB[0..1];
                  toA : TestClassA[1];
                }
                
                function test::testFn():Any[*]
                {
                  let a = ^TestClassA(name='A', toB=[]);
                  assert('A' == $a.name, |'');
                  assert($a.toB->isEmpty(), |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testNewWithZeroToManyAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                """
                import test::*;
                Class test::TestClassA
                {
                  name : String[1];
                }
                
                Class test::TestClassB
                {
                }
                
                Association test::TestAssocAB
                {
                  toB : TestClassB[*];
                  toA : TestClassA[1];
                }
                
                function test::testFn():Any[*]
                {
                  let a = ^TestClassA(name='A', toB=[]);
                  assert('A' == $a.name, |'');
                  assert($a.toB->isEmpty(), |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testNewParametrizedClassWithEmptyPropertiesSet()
    {
        String source =
                """
                Class A<T1, T2>\s
                { prop1:T1[*];
                 prop2:T2[*]; }
                Class B<T>\s
                { prop1:String[*];
                 prop2:T[*]; }
                function test::testFn():Any[*] { ^A<String, Integer>(prop1=[], prop2=[]); ^B<Integer>(prop1='Hello', prop2=[]);}
                function test::testGenericFn<R, T>():Any[*] { ^A<R, T>(prop1=[], prop2=[]); }
                """;
        compileTestSource("fromString.pure", source);
        compileAndExecute("test::testFn():Any[*]");
        // TODO should this be allowed?
//        this.compileAndExecute("test::testGenericFn():Any[*]");
    }

    @Test
    public void testNewWithoutKeyExpressions()
    {
        String source =
                """
                Class A
                {\
                   prop1:String[*];
                }
                function test::testFn():Any[*] \
                {\
                   let lambda = {| ^A()};
                   $lambda.expressionSequence->toOne()->meta::pure::functions::meta::reactivate(meta::pure::functions::collection::newMap([])->cast(@Map<String, List<Any>>));\
                }
                """;
        compileTestSource("fromString.pure", source);
        compileAndExecute("test::testFn():Any[*]");
    }

    @Test
    public abstract void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToOneProperty();

    @Test
    public abstract void testNewWithInheritenceAndOverriddenAssociationEndWithReverseOneToManyProperty();


    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository test = new GenericCodeRepository("test", null, "platform", "platform_functions");
        repositories.add(test);
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }
}
