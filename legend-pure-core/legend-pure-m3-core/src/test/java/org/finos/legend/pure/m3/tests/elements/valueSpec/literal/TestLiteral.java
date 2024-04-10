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

package org.finos.legend.pure.m3.tests.elements.valueSpec.literal;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestLiteral extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testLiteralList()
    {
        compileTestSource("fromString.pure",
                """
                function testMany():String[*]
                {
                    let a = ['z','k']
                }
                """);
        CoreInstance func = runtime.getFunction("testMany():String[*]");
        Assertions.assertEquals("""
                            testMany__String_MANY_ instance ConcreteFunctionDefinition
                                classifierGenericType(Property):
                                    Anonymous_StripedId instance GenericType
                                        rawType(Property):
                                            ConcreteFunctionDefinition instance Class
                                        typeArguments(Property):
                                            Anonymous_StripedId instance GenericType
                                                rawType(Property):
                                                    Anonymous_StripedId instance FunctionType
                                                        function(Property):
                                                            testMany__String_MANY_ instance ConcreteFunctionDefinition
                                                        returnMultiplicity(Property):
                                                            ZeroMany instance PackageableMultiplicity
                                                        returnType(Property):
                                                            Anonymous_StripedId instance GenericType
                                                                rawType(Property):
                                                                    String instance PrimitiveType
                                                                referenceUsages(Property):
                                                                    Anonymous_StripedId instance ReferenceUsage
                                                                        offset(Property):
                                                                            0 instance Integer
                                                                        owner(Property):
                                                                            Anonymous_StripedId instance FunctionType
                                                                        propertyName(Property):
                                                                            returnType instance String
                                expressionSequence(Property):
                                    Anonymous_StripedId instance SimpleFunctionExpression
                                        func(Property):
                                            letFunction_String_1__T_m__T_m_ instance NativeFunction
                                        functionName(Property):
                                            letFunction instance String
                                        genericType(Property):
                                            Anonymous_StripedId instance InferredGenericType
                                                rawType(Property):
                                                    String instance PrimitiveType
                                        importGroup(Property):
                                            import_fromString_pure_1 instance ImportGroup
                                        multiplicity(Property):
                                            Anonymous_StripedId instance Multiplicity
                                                lowerBound(Property):
                                                    Anonymous_StripedId instance MultiplicityValue
                                                        value(Property):
                                                            2 instance Integer
                                                upperBound(Property):
                                                    Anonymous_StripedId instance MultiplicityValue
                                                        value(Property):
                                                            2 instance Integer
                                        parametersValues(Property):
                                            Anonymous_StripedId instance InstanceValue
                                                genericType(Property):
                                                    Anonymous_StripedId instance GenericType
                                                        rawType(Property):
                                                            String instance PrimitiveType
                                                multiplicity(Property):
                                                    PureOne instance PackageableMultiplicity
                                                usageContext(Property):
                                                    Anonymous_StripedId instance ParameterValueSpecificationContext
                                                        functionExpression(Property):
                                                            Anonymous_StripedId instance SimpleFunctionExpression
                                                        offset(Property):
                                                            0 instance Integer
                                                values(Property):
                                                    a instance String
                                            Anonymous_StripedId instance InstanceValue
                                                genericType(Property):
                                                    Anonymous_StripedId instance GenericType
                                                        rawType(Property):
                                                            String instance PrimitiveType
                                                multiplicity(Property):
                                                    Anonymous_StripedId instance Multiplicity
                                                        lowerBound(Property):
                                                            Anonymous_StripedId instance MultiplicityValue
                                                                value(Property):
                                                                    2 instance Integer
                                                        upperBound(Property):
                                                            Anonymous_StripedId instance MultiplicityValue
                                                                value(Property):
                                                                    2 instance Integer
                                                usageContext(Property):
                                                    Anonymous_StripedId instance ParameterValueSpecificationContext
                                                        functionExpression(Property):
                                                            Anonymous_StripedId instance SimpleFunctionExpression
                                                        offset(Property):
                                                            1 instance Integer
                                                values(Property):
                                                    z instance String
                                                    k instance String
                                        usageContext(Property):
                                            Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                                functionDefinition(Property):
                                                    testMany__String_MANY_ instance ConcreteFunctionDefinition
                                                offset(Property):
                                                    0 instance Integer
                                functionName(Property):
                                    testMany instance String
                                name(Property):
                                    testMany__String_MANY_ instance String
                                package(Property):
                                    Root instance Package\
                            """, func.printWithoutDebug("", 10));
    }
}
