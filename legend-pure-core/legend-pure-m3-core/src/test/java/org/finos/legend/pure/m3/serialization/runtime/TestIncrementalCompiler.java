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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestIncrementalCompiler extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    /**
     * This is not supported or intended to be supported in PURE at the moment, but leaving this to document that.
     */
    @Disabled
    @Test
    public void testInstanceDefinedBeforeClassWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1", false, false, """
                ^my::Table instance
                (
                    name = 'Hello'
                )\
                """));
        sources.add(new Source("2", false, false, """
                Class my::Table
                {
                    name : String[1];
                }
                """));
        runtime.getIncrementalCompiler().compile(sources);

        Assertions.assertEquals("""
                instance instance Table
                    name(Property):
                        Hello instance String\
                """, runtime.getCoreInstance("instance").printWithoutDebug(""));
    }

    @Test
    public void testClassInstanceUsedBeforeClassWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1.pure", false, false, """
                function my::tableName():String[1]
                {
                    let t = ^my::Table(name = 'Hello');
                    $t.name;
                }
                """));
        sources.add(new Source("2.pure", false, false, """
                Class my::Table
                {
                    name : String[1];
                }
                """));
        runtime.getIncrementalCompiler().compile(sources);

        Assertions.assertEquals("""
                tableName__String_1_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    [... >1]
                    expressionSequence(Property):
                        Anonymous_StripedId instance SimpleFunctionExpression
                            func(Property):
                                letFunction_String_1__T_m__T_m_ instance NativeFunction
                            functionName(Property):
                                letFunction instance String
                            genericType(Property):
                                Anonymous_StripedId instance InferredGenericType
                                    [... >1]
                            importGroup(Property):
                                import_1_pure_1 instance ImportGroup
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance InstanceValue
                                    [... >1]
                                Anonymous_StripedId instance SimpleFunctionExpression
                                    [... >1]
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    [... >1]
                        Anonymous_StripedId instance SimpleFunctionExpression
                            func(Property):
                                name instance Property
                                    [... >1]
                            genericType(Property):
                                Anonymous_StripedId instance InferredGenericType
                                    [... >1]
                            importGroup(Property):
                                import_1_pure_1 instance ImportGroup
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance VariableExpression
                                    [... >1]
                            propertyName(Property):
                                my$tableName$1$system$imports$import_1_pure_1$0 instance InstanceValue
                                    [... >1]
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    [... >1]
                    functionName(Property):
                        tableName instance String
                    name(Property):
                        tableName__String_1_ instance String
                    package(Property):
                        my instance Package\
                """, runtime.getFunction("my::tableName():String[1]").printWithoutDebug("", 1));
    }

    @Test
    public void testInstanceWithPropertyReferencingEnumBeforeEnumDefinedWillCompile()
    {

        MutableList<Source> sources = Lists.mutable.empty();
        sources.add(new Source("1.pure", false, false, """
                Class my::myClass
                {
                    value : my::myEnum[1];
                }
                ^my::myClass instance
                (
                    value = my::myEnum.VAL1
                )\
                """));
        sources.add(new Source("2.pure", false, false, """
                Enum my::myEnum
                {
                    VAL1, VAL2
                }
                """));
        runtime.getIncrementalCompiler().compile(sources);

        Assertions.assertEquals("""
                instance instance myClass
                    value(Property):
                        Anonymous_StripedId instance EnumStub
                            enumName(Property):
                                VAL1 instance String
                            enumeration(Property):
                                Anonymous_StripedId instance ImportStub
                                    idOrPath(Property):
                                        my::myEnum instance String
                                    importGroup(Property):
                                        import_1_pure_1 instance ImportGroup\
                """, runtime.getCoreInstance("instance").printWithoutDebug("", 1));
    }
}
