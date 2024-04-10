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

package org.finos.legend.pure.m3.tests.function;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGetFunctionType extends AbstractPureTestWithCoreCompiledPlatform
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
    }

    @Test
    public void testGetFunctionType()
    {
        CoreInstance property = Instance.getValueForMetaPropertyToManyResolved(runtime.getCoreInstance(M3Paths.Class), M3Properties.properties, processorSupport).detect(instance -> M3Properties.properties.equals(instance.getName()));
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        Assertions.assertEquals("""
                Anonymous_StripedId instance FunctionType
                    parameters(Property):
                        Anonymous_StripedId instance VariableExpression
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Class instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    [... >3]
                                            typeParameter(Property):
                                                Anonymous_StripedId instance TypeParameter
                                                    [... >3]
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            name(Property):
                                object instance String
                    returnMultiplicity(Property):
                        ZeroMany instance PackageableMultiplicity
                    returnType(Property):
                        Anonymous_StripedId instance GenericType
                            multiplicityArguments(Property):
                                ZeroMany instance PackageableMultiplicity
                            rawType(Property):
                                Property instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                Anonymous_StripedId instance GenericType
                                                    [... >3]
                                            propertyName(Property):
                                                typeArguments instance String
                                    typeParameter(Property):
                                        Anonymous_StripedId instance TypeParameter
                                            name(Property):
                                                T instance String
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Any instance Class\
                """, functionType.printWithoutDebug("", 3));
    }


    @Test
    public void testGetFunctionTypeForProperty()
    {
        runtime.createInMemorySource("fromString.pure", "Class Person {prop:String[1];}");
        runtime.compile();
        CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(runtime.getCoreInstance("Person"), "prop");
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        Assertions.assertEquals("""
                Anonymous_StripedId instance FunctionType
                    parameters(Property):
                        Anonymous_StripedId instance VariableExpression
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Person instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Person instance Class
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            name(Property):
                                object instance String
                    returnMultiplicity(Property):
                        PureOne instance PackageableMultiplicity
                    returnType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                String instance PrimitiveType\
                """, functionType.printWithoutDebug("", 10));
    }


    @Test
    public void testGetFunctionTypeForPropertiesUsingInheritance()
    {
        runtime.createInMemorySource("fromString.pure", """
                Class Person<T> {prop:T[*];}\
                Class Employee extends Person<String>{}\
                """);
        runtime.compile();
        CoreInstance property = processorSupport.class_findPropertyUsingGeneralization(runtime.getCoreInstance("Employee"), "prop");
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        Assertions.assertEquals("""
                Anonymous_StripedId instance FunctionType
                    parameters(Property):
                        Anonymous_StripedId instance VariableExpression
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Person instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Person instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    offset(Property):
                                                        0 instance Integer
                                                    owner(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    idOrPath(Property):
                                                                        Person instance String
                                                                    importGroup(Property):
                                                                        import_fromString_pure_1 instance ImportGroup
                                                                    resolvedNode(Property):
                                                                        Person instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    offset(Property):
                                                                        0 instance Integer
                                                                    owner(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                            multiplicityArguments(Property):
                                                                                ZeroMany instance PackageableMultiplicity
                                                                            rawType(Property):
                                                                                Property instance Class
                                                                            referenceUsages(Property):
                                                                                Anonymous_StripedId instance ReferenceUsage
                                                                                    offset(Property):
                                                                                        0 instance Integer
                                                                                    owner(Property):
                                                                                        prop instance Property
                                                                                            aggregation(Property):
                                                                                                None instance AggregationKind
                                                                                                    name(Property):
                                                                                                        None instance String
                                                                                            classifierGenericType(Property):
                                                                                                Anonymous_StripedId instance GenericType
                                                                                            genericType(Property):
                                                                                                Anonymous_StripedId instance GenericType
                                                                                                    typeParameter(Property):
                                                                                                        Anonymous_StripedId instance TypeParameter
                                                                                                            [... >10]
                                                                                            multiplicity(Property):
                                                                                                ZeroMany instance PackageableMultiplicity
                                                                                            name(Property):
                                                                                                prop instance String
                                                                                            owner(Property):
                                                                                                Person instance Class
                                                                                    propertyName(Property):
                                                                                        classifierGenericType instance String
                                                                            typeArguments(Property):
                                                                                Anonymous_StripedId instance GenericType
                                                                                Anonymous_StripedId instance GenericType
                                                                                    referenceUsages(Property):
                                                                                        Anonymous_StripedId instance ReferenceUsage
                                                                                            offset(Property):
                                                                                                1 instance Integer
                                                                                            owner(Property):
                                                                                                Anonymous_StripedId instance GenericType
                                                                                            propertyName(Property):
                                                                                                typeArguments instance String
                                                                                    typeParameter(Property):
                                                                                        Anonymous_StripedId instance TypeParameter
                                                                                            name(Property):
                                                                                                T instance String
                                                                    propertyName(Property):
                                                                        typeArguments instance String
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                    propertyName(Property):
                                                        typeArguments instance String
                                            typeParameter(Property):
                                                Anonymous_StripedId instance TypeParameter
                                                    name(Property):
                                                        T instance String
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            name(Property):
                                object instance String
                    returnMultiplicity(Property):
                        ZeroMany instance PackageableMultiplicity
                    returnType(Property):
                        Anonymous_StripedId instance GenericType
                            referenceUsages(Property):
                                Anonymous_StripedId instance ReferenceUsage
                                    offset(Property):
                                        1 instance Integer
                                    owner(Property):
                                        Anonymous_StripedId instance GenericType
                                            multiplicityArguments(Property):
                                                ZeroMany instance PackageableMultiplicity
                                            rawType(Property):
                                                Property instance Class
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    offset(Property):
                                                        0 instance Integer
                                                    owner(Property):
                                                        prop instance Property
                                                            aggregation(Property):
                                                                None instance AggregationKind
                                                                    name(Property):
                                                                        None instance String
                                                            classifierGenericType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                            genericType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    typeParameter(Property):
                                                                        Anonymous_StripedId instance TypeParameter
                                                                            name(Property):
                                                                                T instance String
                                                            multiplicity(Property):
                                                                ZeroMany instance PackageableMultiplicity
                                                            name(Property):
                                                                prop instance String
                                                            owner(Property):
                                                                Person instance Class
                                                    propertyName(Property):
                                                        classifierGenericType instance String
                                            typeArguments(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                Person instance String
                                                            importGroup(Property):
                                                                import_fromString_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                Person instance Class
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                            propertyName(Property):
                                                                typeArguments instance String
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    offset(Property):
                                                                        0 instance Integer
                                                                    owner(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                    propertyName(Property):
                                                                        typeArguments instance String
                                                            typeParameter(Property):
                                                                Anonymous_StripedId instance TypeParameter
                                                                    name(Property):
                                                                        T instance String
                                                Anonymous_StripedId instance GenericType
                                    propertyName(Property):
                                        typeArguments instance String
                            typeParameter(Property):
                                Anonymous_StripedId instance TypeParameter
                                    name(Property):
                                        T instance String\
                """, functionType.printWithoutDebug("", 10));
    }
}
