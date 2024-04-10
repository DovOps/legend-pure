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

package org.finos.legend.pure.m3.tests.generictype;

import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// WRITE IN MODELING A TEST WITH AN INSTANCE USING A PROPERTY NOT DEFINED IN THE CLASS
// "function func(emp:Employee[1]):HomeAddress[1]\n" + was compiling where it should be "function func(emp:Employee<Address>[1]):HomeAddress[1]\n"

public class TestIsGenericCompatibleWith extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        try
        {
            runtime.compile();
        }
        catch (PureCompilationException ignore)
        {
            setUp();
        }
    }

    @Test
    public void testIsGenericCompatibleWith_TypeParamInClass()
    {
        compileTestSource("fromString.pure", """
                Class Employee<T>
                {
                   address : T[1];
                }
                Class Address
                {
                }
                Class HomeAddress extends Address
                {
                }
                Class VacationAddress extends Address
                {
                }
                ^Employee<Address> i1 (address = ^VacationAddress())
                ^Employee<HomeAddress> i2 (address = ^HomeAddress())
                """);
        CoreInstance i1 = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i1"), processorSupport);
        CoreInstance i2 = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i2"), processorSupport);
        Assertions.assertFalse(GenericType.isGenericCompatibleWith(i1, i2, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(i2, i1, processorSupport));
    }

    @Test
    public void testIsGenericCompatibleWith_TypeParamInType()
    {
        compileTestSource("fromString.pure", """
                Class Person\
                {\
                }\
                Class Employee<Z> extends Person
                {
                   address : Z[1];
                }
                Class Address
                {
                }
                Class HomeAddress extends Address
                {
                }
                Class VacationAddress extends Address
                {
                }
                ^Employee<Address> i3 (address = ^VacationAddress())
                function func(emp:Employee<Address>[1]):HomeAddress[1]
                {
                   ^HomeAddress();
                }
                function func2(emp:Person[1]):HomeAddress[1]
                {
                   ^HomeAddress();
                }
                """);

        CoreInstance unresolvedPropertyGenericType = Instance.extractGenericTypeFromInstance(Instance.getValueForMetaPropertyToManyResolved(processorSupport.package_getByUserPath("Employee"), M3Properties.properties, processorSupport).getFirst(), processorSupport);
        CoreInstance resolvedPropertyGenericType = GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("i1"), processorSupport), unresolvedPropertyGenericType, processorSupport);

        CoreInstance functionGenericType = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("func_Employee_1__HomeAddress_1_"), processorSupport);

        CoreInstance function2GenericType = Instance.extractGenericTypeFromInstance(processorSupport.package_getByUserPath("func2_Person_1__HomeAddress_1_"), processorSupport);

        Assertions.assertFalse(GenericType.isGenericCompatibleWith(resolvedPropertyGenericType, functionGenericType, processorSupport));
        // The following one is false because FunctionDefinition is not a subtype of a Property
        Assertions.assertFalse(GenericType.isGenericCompatibleWith(functionGenericType, resolvedPropertyGenericType, processorSupport));
        Assertions.assertFalse(GenericType.genericTypesEqual(functionGenericType, function2GenericType, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(function2GenericType, functionGenericType, processorSupport));
        Assertions.assertFalse(GenericType.isGenericCompatibleWith(functionGenericType, function2GenericType, processorSupport));
    }

    @Test
    public void testGenericFunctionTypeCompatibleWithItself()
    {
        compileTestSource("fromString.pure", "Class TestClass { fn : Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>[1]; }");
        CoreInstance prop = processorSupport.class_findPropertyUsingGeneralization(runtime.getCoreInstance("TestClass"), "fn");
        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(prop), M3Properties.returnType, processorSupport);

        // Test that we got the right generic type
        Assertions.assertTrue(Instance.instanceOf(genericType, M3Paths.GenericType, processorSupport));
        Assertions.assertEquals("Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(genericType, processorSupport));

        // Test that it is compatible with itself
        Assertions.assertTrue(GenericType.genericTypesEqual(genericType, genericType, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(genericType, genericType, processorSupport));

        // Test that it is compatible with a copy of itself
        CoreInstance copy = GenericType.copyGenericType(genericType, processorSupport);
        Assertions.assertTrue(GenericType.genericTypesEqual(genericType, copy, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(genericType, copy, processorSupport));
    }

    @Test
    public void testGenericLambdaFunctionTypeCompatibleWithGenericFunctionType()
    {
        compileTestSource("fromString.pure", """
                Class TestClass
                {
                  fn : Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>[1];
                  lfn : LambdaFunction<{Integer[1]->LambdaFunction<{Float[1]->Boolean[1]}>[1]}>[1];
                }
                """);
        MapIterable<String, CoreInstance> props = processorSupport.class_getSimplePropertiesByName(runtime.getCoreInstance("TestClass"));
        CoreInstance fnProp = props.get("fn");
        CoreInstance lfnProp = props.get("lfn");

        CoreInstance fnGenericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(fnProp), M3Properties.returnType, processorSupport);
        Assertions.assertEquals("Function<{Integer[1]->Function<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(fnGenericType, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(fnGenericType, fnGenericType, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(fnGenericType, GenericType.copyGenericType(fnGenericType, processorSupport), processorSupport));

        CoreInstance lfnGenericType = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(lfnProp), M3Properties.returnType, processorSupport);
        Assertions.assertEquals("LambdaFunction<{Integer[1]->LambdaFunction<{Float[1]->Boolean[1]}>[1]}>", GenericType.print(lfnGenericType, processorSupport));
        Assertions.assertTrue(GenericType.isGenericCompatibleWith(lfnGenericType, GenericType.copyGenericType(lfnGenericType, processorSupport), processorSupport));

        Assertions.assertTrue(GenericType.isGenericCompatibleWith(lfnGenericType, fnGenericType, processorSupport));
    }

    // Make sure we can model a Set of Function having any time of parameters (the type check should work)

}
