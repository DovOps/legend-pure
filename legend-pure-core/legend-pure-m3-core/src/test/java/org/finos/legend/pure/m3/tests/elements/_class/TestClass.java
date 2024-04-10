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

package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestClass extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testClass()
    {
        compileTestSource("fromString.pure", """
                Class Table
                {
                    name : String[1];
                    columns : Column[*];
                }
                Class Column
                {
                    name : String[1];
                }
                """);

        Assertions.assertEquals("""
                Table instance Class
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                Class instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Table instance Class
                    generalizations(Property):
                        Anonymous_StripedId instance Generalization
                            general(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Any instance Class
                            specific(Property):
                                Table instance Class
                    name(Property):
                        Table instance String
                    package(Property):
                        Root instance Package
                    properties(Property):
                        name instance Property
                            aggregation(Property):
                                None instance AggregationKind
                                    name(Property):
                                        None instance String
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    multiplicityArguments(Property):
                                        PureOne instance PackageableMultiplicity
                                    rawType(Property):
                                        Property instance Class
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                name instance Property
                                                    [... >3]
                                            propertyName(Property):
                                                classifierGenericType instance String
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    [... >3]
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    [... >3]
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                String instance PrimitiveType
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    [... >3]
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        String instance PrimitiveType
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            name(Property):
                                name instance String
                            owner(Property):
                                Table instance Class
                        columns instance Property
                            aggregation(Property):
                                None instance AggregationKind
                                    name(Property):
                                        None instance String
                            classifierGenericType(Property):
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
                                                columns instance Property
                                                    [... >3]
                                            propertyName(Property):
                                                classifierGenericType instance String
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    [... >3]
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    [... >3]
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    [... >3]
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    [... >3]
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Column instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Column instance Class
                            multiplicity(Property):
                                ZeroMany instance PackageableMultiplicity
                            name(Property):
                                columns instance String
                            owner(Property):
                                Table instance Class
                    referenceUsages(Property):
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Table instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Table instance Class
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                Anonymous_StripedId instance GenericType
                                                    [... >3]
                                            propertyName(Property):
                                                typeArguments instance String
                            propertyName(Property):
                                rawType instance String
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Table instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Table instance Class
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                Anonymous_StripedId instance GenericType
                                                    [... >3]
                                            propertyName(Property):
                                                typeArguments instance String
                            propertyName(Property):
                                rawType instance String\
                """, this.runtime.getCoreInstance("Table").printWithoutDebug("", 3));

        Assertions.assertEquals("""
                Column instance Class
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                Class instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Column instance Class
                    generalizations(Property):
                        Anonymous_StripedId instance Generalization
                            general(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Any instance Class
                            specific(Property):
                                Column instance Class
                    name(Property):
                        Column instance String
                    package(Property):
                        Root instance Package
                    properties(Property):
                        name instance Property
                            aggregation(Property):
                                None instance AggregationKind
                                    name(Property):
                                        None instance String
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    multiplicityArguments(Property):
                                        PureOne instance PackageableMultiplicity
                                    rawType(Property):
                                        Property instance Class
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            [... >2]
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            [... >2]
                                        Anonymous_StripedId instance GenericType
                                            [... >2]
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        String instance PrimitiveType
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            name(Property):
                                name instance String
                            owner(Property):
                                Column instance Class
                    referenceUsages(Property):
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            [... >2]
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            [... >2]
                            propertyName(Property):
                                rawType instance String
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            [... >2]
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            [... >2]
                            propertyName(Property):
                                rawType instance String\
                """, this.runtime.getCoreInstance("Column").printWithoutDebug("", 2));
    }

    @Test
    public void testInstance()
    {
        compileTestSource("fromString.pure", """
                Class Table
                {
                    name : String[1];
                    columns : Column[*];
                }
                
                
                Class Column
                {
                    name : String[1];
                }
                ^Table instance
                (
                    name = 'Hello',
                    columns = [
                        ^Column
                            (
                                name='Test'
                            ),
                        ^Column
                            (
                
                            )
                        ]
                )\
                """);
        Assertions.assertEquals("""
                instance instance Table
                    columns(Property):
                        Anonymous_StripedId instance Column
                            name(Property):
                                Test instance String
                        Anonymous_StripedId instance Column
                    name(Property):
                        Hello instance String\
                """, this.runtime.getCoreInstance("instance").printWithoutDebug(""));
    }

    @Test
    public void testGetProperties()
    {
        compileTestSource("test.pure",
                """
                Class test::A extends test::C {
                  propA: String[1];
                  qpropA(){''}: String[1];
                }
                
                Class test::B {
                  propB: String[1];
                  qpropB(){''}: String[1];
                }
                
                Class test::C {
                  propC: String[1];
                  qpropC(){''}: String[1];
                }
                
                Class test::D {
                  propD: String[1];
                  qpropD(){''}: String[1];
                }
                
                Association test::AB
                {
                  propAB_A: test::A[1];
                  propAB_B: test::B[1];
                }
                
                Association test::CD
                {
                  propCD_C: test::C[1];
                  propCD_D: test::D[1];
                }\
                """);

        Assertions.assertEquals(_Class.getQualifiedProperties(runtime.getCoreInstance("test::A"), runtime.getProcessorSupport()).size(), 2);
        Assertions.assertEquals(_Class.getSimpleProperties(runtime.getCoreInstance("test::A"), runtime.getProcessorSupport()).size(), 6);
    }
}
