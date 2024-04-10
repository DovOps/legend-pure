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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestSyntheticNodeCut extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
    }

    @Test
    public void testEnsurePropertySourceTypeIsNotResolved()
    {
        compileTestSource("testSource.pure", """
                Class A{name:String[1];}
                function
                   {doc.doc = 'Get the property with the given name from the given class. Note that this searches only properties defined directly on the class, not those inherited from super-classes or those which come from associations.'}
                   meta::pure::functions::meta::classPropertyByName(class:Class<Any>[1], name:String[1]):Property<Nil,Any|*>[0..1]
                {
                    $class.properties->filter(p | $p.name == $name)->first()
                }
                function test():Nil[0]
                {
                    print(A->classPropertyByName('name'),10);
                }
                """);
        execute("test():Nil[0]");
        Assertions.assertEquals("""
                name instance Property
                    aggregation(Property):
                        None instance AggregationKind
                            name(Property):
                                None instance String
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            multiplicityArguments(Property):
                                [X] PureOne instance PackageableMultiplicity
                            rawType(Property):
                                [X] Property instance Class
                            referenceUsages(Property):
                                Anonymous_StripedId instance ReferenceUsage
                                    offset(Property):
                                        0 instance Integer
                                    owner(Property):
                                        [_] name instance Property
                                    propertyName(Property):
                                        classifierGenericType instance String
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [~>] A instance Class
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                [_] Anonymous_StripedId instance GenericType
                                            propertyName(Property):
                                                typeArguments instance String
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] String instance PrimitiveType
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                1 instance Integer
                                            owner(Property):
                                                [_] Anonymous_StripedId instance GenericType
                                            propertyName(Property):
                                                typeArguments instance String
                    genericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] String instance PrimitiveType
                    multiplicity(Property):
                        [X] PureOne instance PackageableMultiplicity
                    name(Property):
                        name instance String
                    owner(Property):
                        [X] A instance Class\
                """, functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testEnsureLambdaReturnIsNotResolved()
    {
        compileTestSource("testSource.pure",
                """
                Class A{}
                function testMany():FunctionDefinition<{->A[1]}>[1]
                {
                    |^A();
                }
                """);
        CoreInstance func = runtime.getFunction("testMany():FunctionDefinition[1]");
        Assertions.assertEquals("""
                        Anonymous_StripedId instance InstanceValue
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    idOrPath(Property):
                                                                        A instance String
                                                                    importGroup(Property):
                                                                        import_testSource_pure_1 instance ImportGroup
                                                                    resolvedNode(Property):
                                                                        A instance Class
                            multiplicity(Property):
                                PureOne instance PackageableMultiplicity
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        testMany__FunctionDefinition_1_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        0 instance Integer
                            values(Property):
                                testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction
                                    classifierGenericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                LambdaFunction instance Class
                                            typeArguments(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance FunctionType
                                                            function(Property):
                                                                testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction
                                                            returnMultiplicity(Property):
                                                                PureOne instance PackageableMultiplicity
                                                            returnType(Property):
                                                                Anonymous_StripedId instance InferredGenericType
                                                                    rawType(Property):
                                                                        Anonymous_StripedId instance ImportStub
                                                                            idOrPath(Property):
                                                                                A instance String
                                                                            importGroup(Property):
                                                                                import_testSource_pure_1 instance ImportGroup
                                                                            resolvedNode(Property):
                                                                                A instance Class
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
                                                new_Class_1__String_1__T_1_ instance NativeFunction
                                            functionName(Property):
                                                new instance String
                                            genericType(Property):
                                                Anonymous_StripedId instance InferredGenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                A instance String
                                                            importGroup(Property):
                                                                import_testSource_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                A instance Class
                                            importGroup(Property):
                                                import_testSource_pure_1 instance ImportGroup
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            parametersValues(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Class instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    offset(Property):
                                                                        0 instance Integer
                                                                    owner(Property):
                                                                        Anonymous_StripedId instance InstanceValue
                                                                    propertyName(Property):
                                                                        genericType instance String
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    rawType(Property):
                                                                        Anonymous_StripedId instance ImportStub
                                                                            idOrPath(Property):
                                                                                A instance String
                                                                            importGroup(Property):
                                                                                import_testSource_pure_1 instance ImportGroup
                                                                            resolvedNode(Property):
                                                                                A instance Class
                                                                    referenceUsages(Property):
                                                                        Anonymous_StripedId instance ReferenceUsage
                                                                            offset(Property):
                                                                                0 instance Integer
                                                                            owner(Property):
                                                                                Anonymous_StripedId instance GenericType
                                                                            propertyName(Property):
                                                                                typeArguments instance String
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                            offset(Property):
                                                                0 instance Integer
                                                    values(Property):
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
                                                                1 instance Integer
                                                    values(Property):
                                                         instance String
                                            usageContext(Property):
                                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                                    functionDefinition(Property):
                                                        testMany$1$system$imports$import_testSource_pure_1$0 instance LambdaFunction
                                                    offset(Property):
                                                        0 instance Integer
                                    referenceUsages(Property):
                                        Anonymous_StripedId instance ReferenceUsage
                                            offset(Property):
                                                0 instance Integer
                                            owner(Property):
                                                Anonymous_StripedId instance InstanceValue
                                            propertyName(Property):
                                                values instance String\
                        """,
                func.getValueForMetaPropertyToOne(M3Properties.expressionSequence).printWithoutDebug("", 10));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
