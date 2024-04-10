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

package org.finos.legend.pure.runtime.java.interpreted.incremental.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeFunction_AsPointer extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("other.pure");
    }

    @Test
    public void testPureRuntimeFunctionPointer() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(sourceFunction__String_1_, 10)}\n");
        this.compileAndExecute("go():Nil[0]");
        int size = runtime.getModelRepository().serialize().length;
        Assertions.assertEquals("""
                sourceFunction__String_1_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance FunctionType
                                            function(Property):
                                                [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                                            returnMultiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            returnType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        [X] String instance PrimitiveType
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                [_] Anonymous_StripedId instance FunctionType
                                                            propertyName(Property):
                                                                returnType instance String
                    expressionSequence(Property):
                        Anonymous_StripedId instance InstanceValue
                            genericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] String instance PrimitiveType
                            multiplicity(Property):
                                [X] PureOne instance PackageableMultiplicity
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        0 instance Integer
                            values(Property):
                                theFunc instance String
                    functionName(Property):
                        sourceFunction instance String
                    name(Property):
                        sourceFunction__String_1_ instance String
                    package(Property):
                        [X] Root instance Package
                    referenceUsages(Property):
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] ConcreteFunctionDefinition instance Class
                                            typeArguments(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance FunctionType
                                                            returnMultiplicity(Property):
                                                                [X] PureOne instance PackageableMultiplicity
                                                            returnType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    rawType(Property):
                                                                        [X] String instance PrimitiveType
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                            functionExpression(Property):
                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                    func(Property):
                                                        [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction
                                                    functionName(Property):
                                                        print instance String
                                                    genericType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                [~>] Nil instance Class
                                                    importGroup(Property):
                                                        [X] import_userId_pure_1 instance ImportGroup
                                                    multiplicity(Property):
                                                        [X] PureZero instance PackageableMultiplicity
                                                    parametersValues(Property):
                                                        [_] Anonymous_StripedId instance InstanceValue
                                                        Anonymous_StripedId instance InstanceValue
                                                            genericType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    rawType(Property):
                                                                        [X] Integer instance PrimitiveType
                                                            multiplicity(Property):
                                                                [X] PureOne instance PackageableMultiplicity
                                                            usageContext(Property):
                                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                                    functionExpression(Property):
                                                                        [_] Anonymous_StripedId instance SimpleFunctionExpression
                                                                    offset(Property):
                                                                        1 instance Integer
                                                            values(Property):
                                                                10 instance Integer
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                                            functionDefinition(Property):
                                                                [X] go__Nil_0_ instance ConcreteFunctionDefinition
                                                            offset(Property):
                                                                0 instance Integer
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        [~>] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                            propertyName(Property):
                                values instance String\
                """, functionExecution.getConsole().getLine(0));

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 28, e);
            }

            runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'beuh!'}");
            this.compileAndExecute("go():Nil[0]");
            Assertions.assertEquals("""
                    sourceFunction__String_1_ instance ConcreteFunctionDefinition
                        classifierGenericType(Property):
                            Anonymous_StripedId instance GenericType
                                rawType(Property):
                                    [X] ConcreteFunctionDefinition instance Class
                                typeArguments(Property):
                                    Anonymous_StripedId instance GenericType
                                        rawType(Property):
                                            Anonymous_StripedId instance FunctionType
                                                function(Property):
                                                    [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                                                returnMultiplicity(Property):
                                                    [X] PureOne instance PackageableMultiplicity
                                                returnType(Property):
                                                    Anonymous_StripedId instance GenericType
                                                        rawType(Property):
                                                            [X] String instance PrimitiveType
                                                        referenceUsages(Property):
                                                            Anonymous_StripedId instance ReferenceUsage
                                                                offset(Property):
                                                                    0 instance Integer
                                                                owner(Property):
                                                                    [_] Anonymous_StripedId instance FunctionType
                                                                propertyName(Property):
                                                                    returnType instance String
                        expressionSequence(Property):
                            Anonymous_StripedId instance InstanceValue
                                genericType(Property):
                                    Anonymous_StripedId instance GenericType
                                        rawType(Property):
                                            [X] String instance PrimitiveType
                                multiplicity(Property):
                                    [X] PureOne instance PackageableMultiplicity
                                usageContext(Property):
                                    Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                        functionDefinition(Property):
                                            [X] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                                        offset(Property):
                                            0 instance Integer
                                values(Property):
                                    beuh! instance String
                        functionName(Property):
                            sourceFunction instance String
                        name(Property):
                            sourceFunction__String_1_ instance String
                        package(Property):
                            [X] Root instance Package
                        referenceUsages(Property):
                            Anonymous_StripedId instance ReferenceUsage
                                offset(Property):
                                    0 instance Integer
                                owner(Property):
                                    Anonymous_StripedId instance InstanceValue
                                        genericType(Property):
                                            Anonymous_StripedId instance GenericType
                                                rawType(Property):
                                                    [X] ConcreteFunctionDefinition instance Class
                                                typeArguments(Property):
                                                    Anonymous_StripedId instance GenericType
                                                        rawType(Property):
                                                            Anonymous_StripedId instance FunctionType
                                                                returnMultiplicity(Property):
                                                                    [X] PureOne instance PackageableMultiplicity
                                                                returnType(Property):
                                                                    Anonymous_StripedId instance GenericType
                                                                        rawType(Property):
                                                                            [X] String instance PrimitiveType
                                        multiplicity(Property):
                                            [X] PureOne instance PackageableMultiplicity
                                        usageContext(Property):
                                            Anonymous_StripedId instance ParameterValueSpecificationContext
                                                functionExpression(Property):
                                                    Anonymous_StripedId instance SimpleFunctionExpression
                                                        func(Property):
                                                            [X] print_Any_MANY__Integer_1__Nil_0_ instance NativeFunction
                                                        functionName(Property):
                                                            print instance String
                                                        genericType(Property):
                                                            Anonymous_StripedId instance InferredGenericType
                                                                rawType(Property):
                                                                    [~>] Nil instance Class
                                                        importGroup(Property):
                                                            [X] import_userId_pure_1 instance ImportGroup
                                                        multiplicity(Property):
                                                            [X] PureZero instance PackageableMultiplicity
                                                        parametersValues(Property):
                                                            [_] Anonymous_StripedId instance InstanceValue
                                                            Anonymous_StripedId instance InstanceValue
                                                                genericType(Property):
                                                                    Anonymous_StripedId instance GenericType
                                                                        rawType(Property):
                                                                            [X] Integer instance PrimitiveType
                                                                multiplicity(Property):
                                                                    [X] PureOne instance PackageableMultiplicity
                                                                usageContext(Property):
                                                                    Anonymous_StripedId instance ParameterValueSpecificationContext
                                                                        functionExpression(Property):
                                                                            [_] Anonymous_StripedId instance SimpleFunctionExpression
                                                                        offset(Property):
                                                                            1 instance Integer
                                                                values(Property):
                                                                    10 instance Integer
                                                        usageContext(Property):
                                                            Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                                                functionDefinition(Property):
                                                                    [X] go__Nil_0_ instance ConcreteFunctionDefinition
                                                                offset(Property):
                                                                    0 instance Integer
                                                offset(Property):
                                                    0 instance Integer
                                        values(Property):
                                            [~>] sourceFunction__String_1_ instance ConcreteFunctionDefinition
                                propertyName(Property):
                                    values instance String\
                    """, functionExecution.getConsole().getLine(0));
        }

        runtime.delete("sourceId.pure");
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }

    @Test
    public void testPureRuntimeFunctionPointerError() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.createInMemorySource("userId.pure", "function go():Nil[0]{print(sourceFunction__String_1_,1)}\n");
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.createInMemorySource("sourceId.pure", "function sourceFunction():Integer[1]{1}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "sourceFunction__String_1_ has not been defined!", "userId.pure", 1, 28, e);
            }
        }
        runtime.modify("sourceId.pure", "function sourceFunction():String[1]{'theFunc'}");
        runtime.compile();
        Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
    }


    @Test
    public void testPureRuntimeFunctionPointerAsParamOfAFunction() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "Class A{}");
        runtime.createInMemorySource("userId.pure", "function other(a:FunctionDefinition<{->A[1]}>[1]):Nil[0]{[]} function go():Nil[0]{other(sourceFunction__A_1_)}\n");
        runtime.createInMemorySource("other.pure", " function sourceFunction():A[1]{^A()}");
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", e);
            }

            runtime.createInMemorySource("sourceId.pure", "Class A{}");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
        }
    }


    @Test
    public void testPureRuntimeFunctionPointerAsParamOfAFunctionError() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", "Class A{}");
        runtime.createInMemorySource("userId.pure", "function other(a:FunctionDefinition<{->A[1]}>[1]):Nil[0]{[]} function go():Nil[0]{other(sourceFunction__A_1_)}\n");
        runtime.createInMemorySource("other.pure", " function sourceFunction():A[1]{^A()}");
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "A has not been defined!", e);
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", "Class B{}");
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertTrue("Compilation error at (resource:other.pure line:1 column:28), \"A has not been defined!\"".equals(e.getMessage()) || "Compilation error at (resource:userId.pure line:1 column:40), \"A has not been defined!\"".equals(e.getMessage()));
            }

            runtime.modify("sourceId.pure", "Class A{}");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch");
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
