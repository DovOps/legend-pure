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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestReprocessTypeParametersUsingGenericTypeOwnerContext extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testReprocessTypeParametersUsingGenericTypeOwnerContext()
    {
        CoreInstance property = Instance.getValueForMetaPropertyToManyResolved(runtime.getCoreInstance(M3Paths.Class), M3Properties.properties, processorSupport).detect(instance -> M3Properties.properties.equals(instance.getName()));
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        CoreInstance genericType = Instance.extractGenericTypeFromInstance(runtime.getCoreInstance(M3Paths.Multiplicity), processorSupport);
        Assertions.assertEquals("""
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                Anonymous_StripedId instance FunctionType
                                    parameters(Property):
                                        Anonymous_StripedId instance VariableExpression
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Class instance Class
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
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
                                                    rawType(Property):
                                                        Multiplicity instance Class
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Any instance Class\
                        """,
                GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(genericType, Type.wrapGenericType(functionType, processorSupport), processorSupport).printWithoutDebug("", 3));
    }
}
