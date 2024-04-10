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

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestResolveClassTypeParameter extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testResolveClassTypeParameter()
    {
        compileTestSource("fromString.pure", """
                Class A<T>{}\
                Class D<T> extends A<T>{}\
                Class B extends D<C>{}\
                Class C{}\
                """);
        CoreInstance genericType = GenericType.resolveClassTypeParameterUsingInheritance(Type.wrapGenericType(runtime.getCoreInstance("B"), processorSupport), Type.wrapGenericType(runtime.getCoreInstance("A"), processorSupport), processorSupport).getArgumentsByParameterName().get("T");
        Assertions.assertEquals("""
                Anonymous_StripedId instance GenericType
                    rawType(Property):
                        Anonymous_StripedId instance ImportStub
                            idOrPath(Property):
                                C instance String
                            importGroup(Property):
                                import_fromString_pure_1 instance ImportGroup
                            resolvedNode(Property):
                                C instance Class\
                """, genericType.printWithoutDebug(""));
    }

    @Test
    public void testResolveClassStartingFromANonResolvedGenericType()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A<T>{}\
                    Class D<T> extends A<T>{}\
                    Class B<Z> extends D<C>{}\
                    Class C{}\
                    """);
            GenericType.resolveClassTypeParameterUsingInheritance(Type.wrapGenericType(runtime.getCoreInstance("B"), processorSupport), Type.wrapGenericType(runtime.getCoreInstance("A"), processorSupport), processorSupport).getArgumentsByParameterName().get("T");
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Type argument mismatch for B<Z>; got: B", e.getMessage());
        }
    }

    @Test
    public void testResolveClassTypeWithFunctionTypes()
    {
        compileTestSource("fromString.pure", """
                Class A<T>{}\
                Class D<T,X> extends A<{T[1]->{X[1]->C[1]}[1]}>{}\
                Class B<X> extends D<X,C>{}\
                Class C{}\
                Class E{}\
                ^B<E> k()\
                """);
        CoreInstance genericType = GenericType.resolveClassTypeParameterUsingInheritance(Instance.getValueForMetaPropertyToOneResolved(runtime.getCoreInstance("k"), M3Properties.classifierGenericType, processorSupport), Type.wrapGenericType(runtime.getCoreInstance("A"), processorSupport), processorSupport).getArgumentsByParameterName().get("T");
        Assertions.assertEquals("""
                Anonymous_StripedId instance GenericType
                    rawType(Property):
                        Anonymous_StripedId instance FunctionType
                            parameters(Property):
                                Anonymous_StripedId instance VariableExpression
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    idOrPath(Property):
                                                        E instance String
                                                    importGroup(Property):
                                                        import_fromString_pure_1 instance ImportGroup
                                                    resolvedNode(Property):
                                                        E instance Class
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    name(Property):
                                         instance String
                            returnMultiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            returnType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance FunctionType
                                            parameters(Property):
                                                Anonymous_StripedId instance VariableExpression
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    idOrPath(Property):
                                                                        C instance String
                                                                    importGroup(Property):
                                                                        import_fromString_pure_1 instance ImportGroup
                                                                    resolvedNode(Property):
                                                                        C instance Class
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    name(Property):
                                                         instance String
                                            returnMultiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            returnType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                C instance String
                                                            importGroup(Property):
                                                                import_fromString_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                C instance Class\
                """, genericType.printWithoutDebug("", 10));
    }
}
