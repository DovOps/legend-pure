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

package org.finos.legend.pure.m3.tests.elements.function;

import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestFunction extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testFunctionTypeWithWrongTypes()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    function myFunc(f:{String[1]->{String[1]->Booelean[1]}[1]}[*]):String[1]
                    {
                       'ee';
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Booelean has not been defined!", 1, 43, e);
        }
    }

    @Test
    public void testNewWithUnknownType()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    function myFunc():String[1]
                    {
                        ^XErrorType(name = 'ok');
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "XErrorType has not been defined!", 3, 6, e);
        }
    }

    @Test
    public void testCastWithUnknownType()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    function myFunc():String[1]
                    {
                        'a'->cast(@Error);
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error has not been defined!", 3, 16, e);
        }
    }


    @Test
    public void testCastWithUnknownGeneric()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A<E>{}
                    
                    function myFunc():String[1]
                    {
                        'a'->cast(@A<Error>);
                    }
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error has not been defined!", 5, 18, e);
        }
    }

    @Test
    public void testReturnTypeValidationWithTypeParameter()
    {
        // This should work because Nil is the bottom type
        compileTestSource("fromString.pure", """
                function test1<T>(t:T[1]):T[0..1]
                {
                    []
                }\
                """);
        try
        {
            compileTestSource("fromString2.pure", """
                    function test2<T>(t:T[1]):T[1]
                    {
                        5
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'test2'; found: Integer; expected: T", 3, 5, e);
        }

        try
        {
            compileTestSource("fromString3.pure", """
                    function test3<T,U>(t:T[1], u:U[1]):T[1]
                    {
                        $u
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return type error in function 'test3'; found: U; expected: T", 3, 6, e);
        }
    }

    @Test
    public void testReturnMultiplicityValidationWithMultiplicityParameter()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    function test1<|m>(a:Any[m]):Any[m]
                    {
                        1
                    }\
                    """);
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return multiplicity error in function 'test1'; found: [1]; expected: [m]", 3, 5, e);
        }

        try
        {
            compileTestSource("fromString2.pure", """
                    function test2<|m,n>(a:Any[m], b:Any[n]):Any[m]
                    {
                        $b
                    }\
                    """);
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Return multiplicity error in function 'test2'; found: [n]; expected: [m]", 3, 6, e);
        }
    }

    @Test
    public void testSimple()
    {
        runtime.createInMemorySource("fromString.pure",
                """
                Class a::A{val:String[1];}\
                function myFunction(func:a::A[1]):String[1]\
                {\
                    ^a::A(val='ok').val;\
                }\
                function test():Nil[0]
                {
                    print(myFunction_A_1__String_1_,1);\
                }\
                """);
        runtime.compile();

        CoreInstance func = runtime.getFunction("test():Nil[0]");
        Assertions.assertEquals("""
                test__Nil_0_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance FunctionType
                                            function(Property):
                                                [X] test__Nil_0_ instance ConcreteFunctionDefinition
                                            returnMultiplicity(Property):
                                                [X] PureZero instance PackageableMultiplicity
                                            returnType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                    expressionSequence(Property):
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
                                [X] import_fromString_pure_1 instance ImportGroup
                            multiplicity(Property):
                                [X] PureZero instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] ConcreteFunctionDefinition instance Class
                                            typeArguments(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                            functionExpression(Property):
                                                [>3] Anonymous_StripedId instance SimpleFunctionExpression
                                            offset(Property):
                                                [>3] 0 instance Integer
                                    values(Property):
                                        [~>] myFunction_A_1__String_1_ instance ConcreteFunctionDefinition
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
                                                [>3] Anonymous_StripedId instance SimpleFunctionExpression
                                            offset(Property):
                                                [>3] 1 instance Integer
                                    values(Property):
                                        1 instance Integer
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        [X] test__Nil_0_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        0 instance Integer
                    functionName(Property):
                        test instance String
                    name(Property):
                        test__Nil_0_ instance String
                    package(Property):
                        [X] Root instance Package\
                """, Printer.print(func, "", 3, runtime.getProcessorSupport()));

        Assertions.assertEquals("""
                test__Nil_0_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance FunctionType
                                            function(Property):
                                                [X] test__Nil_0_ instance ConcreteFunctionDefinition
                                            returnMultiplicity(Property):
                                                [X] PureZero instance PackageableMultiplicity
                                            returnType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        [~>] Nil instance Class
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                [_] Anonymous_StripedId instance FunctionType
                                                            propertyName(Property):
                                                                returnType instance String
                    expressionSequence(Property):
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
                                [X] import_fromString_pure_1 instance ImportGroup
                            multiplicity(Property):
                                [X] PureZero instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] ConcreteFunctionDefinition instance Class
                                            typeArguments(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance FunctionType
                                                            parameters(Property):
                                                                Anonymous_StripedId instance VariableExpression
                                                                    genericType(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                            rawType(Property):
                                                                                [~>] a::A instance Class
                                                                    multiplicity(Property):
                                                                        [X] PureOne instance PackageableMultiplicity
                                                                    name(Property):
                                                                        func instance String
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
                                                [_] Anonymous_StripedId instance SimpleFunctionExpression
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        [~>] myFunction_A_1__String_1_ instance ConcreteFunctionDefinition
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
                                        1 instance Integer
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        [X] test__Nil_0_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        0 instance Integer
                    functionName(Property):
                        test instance String
                    name(Property):
                        test__Nil_0_ instance String
                    package(Property):
                        [X] Root instance Package\
                """, Printer.print(func, "", runtime.getProcessorSupport()));
    }

    @Test
    public void testFunction()
    {
        compileTestSource("fromString.pure", """
                Class Employee {name:String[1];}\
                function getValue(source:Any[1], prop:String[1]):Any[*]
                {
                    Employee.all()->filter(t:Employee[1]|$t.name == 'cool');
                }\
                """);

        Assertions.assertEquals("""
                getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance FunctionType
                                            function(Property):
                                                getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition
                                            parameters(Property):
                                                Anonymous_StripedId instance VariableExpression
                                                    functionTypeOwner(Property):
                                                        Anonymous_StripedId instance FunctionType
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    idOrPath(Property):
                                                                        Any instance String
                                                                    importGroup(Property):
                                                                        import_fromString_pure_1 instance ImportGroup
                                                                    resolvedNode(Property):
                                                                        Any instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    offset(Property):
                                                                        0 instance Integer
                                                                    owner(Property):
                                                                        Anonymous_StripedId instance VariableExpression
                                                                    propertyName(Property):
                                                                        genericType instance String
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    name(Property):
                                                        source instance String
                                                Anonymous_StripedId instance VariableExpression
                                                    functionTypeOwner(Property):
                                                        Anonymous_StripedId instance FunctionType
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    offset(Property):
                                                                        0 instance Integer
                                                                    owner(Property):
                                                                        Anonymous_StripedId instance VariableExpression
                                                                    propertyName(Property):
                                                                        genericType instance String
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    name(Property):
                                                        prop instance String
                                            returnMultiplicity(Property):
                                                ZeroMany instance PackageableMultiplicity
                                            returnType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                Any instance String
                                                            importGroup(Property):
                                                                import_fromString_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                Any instance Class
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
                                filter_T_MANY__Function_1__T_MANY_ instance NativeFunction
                            functionName(Property):
                                filter instance String
                            genericType(Property):
                                Anonymous_StripedId instance InferredGenericType
                                    rawType(Property):
                                        Employee instance Class
                            importGroup(Property):
                                import_fromString_pure_1 instance ImportGroup
                            multiplicity(Property):
                                ZeroMany instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance SimpleFunctionExpression
                                    func(Property):
                                        getAll_Class_1__T_MANY_ instance NativeFunction
                                    functionName(Property):
                                        getAll instance String
                                    genericType(Property):
                                        Anonymous_StripedId instance InferredGenericType
                                            rawType(Property):
                                                Employee instance Class
                                    importGroup(Property):
                                        import_fromString_pure_1 instance ImportGroup
                                    multiplicity(Property):
                                        ZeroMany instance PackageableMultiplicity
                                    parametersValues(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Class instance Class
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Employee instance Class
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                    functionExpression(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                    offset(Property):
                                                        0 instance Integer
                                            values(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    idOrPath(Property):
                                                        Employee instance String
                                                    importGroup(Property):
                                                        import_fromString_pure_1 instance ImportGroup
                                                    resolvedNode(Property):
                                                        Employee instance Class
                                    usageContext(Property):
                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                            functionExpression(Property):
                                                Anonymous_StripedId instance SimpleFunctionExpression
                                            offset(Property):
                                                0 instance Integer
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                LambdaFunction instance Class
                                            typeArguments(Property):
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
                                                                                        Employee instance String
                                                                                    importGroup(Property):
                                                                                        import_fromString_pure_1 instance ImportGroup
                                                                                    resolvedNode(Property):
                                                                                        Employee instance Class
                                                                    multiplicity(Property):
                                                                        PureOne instance PackageableMultiplicity
                                                                    name(Property):
                                                                        t instance String
                                                            returnMultiplicity(Property):
                                                                PureOne instance PackageableMultiplicity
                                                            returnType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    rawType(Property):
                                                                        Boolean instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                            functionExpression(Property):
                                                Anonymous_StripedId instance SimpleFunctionExpression
                                            offset(Property):
                                                1 instance Integer
                                    values(Property):
                                        getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction
                                            classifierGenericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        LambdaFunction instance Class
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance FunctionType
                                                                    function(Property):
                                                                        getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction
                                                                    parameters(Property):
                                                                        Anonymous_StripedId instance VariableExpression
                                                                            functionTypeOwner(Property):
                                                                                Anonymous_StripedId instance FunctionType
                                                                            genericType(Property):
                                                                                Anonymous_StripedId instance GenericType
                                                                                    rawType(Property):
                                                                                        Anonymous_StripedId instance ImportStub
                                                                                            idOrPath(Property):
                                                                                                Employee instance String
                                                                                            importGroup(Property):
                                                                                                import_fromString_pure_1 instance ImportGroup
                                                                                            resolvedNode(Property):
                                                                                                Employee instance Class
                                                                                    referenceUsages(Property):
                                                                                        Anonymous_StripedId instance ReferenceUsage
                                                                                            offset(Property):
                                                                                                0 instance Integer
                                                                                            owner(Property):
                                                                                                Anonymous_StripedId instance VariableExpression
                                                                                            propertyName(Property):
                                                                                                genericType instance String
                                                                            multiplicity(Property):
                                                                                PureOne instance PackageableMultiplicity
                                                                            name(Property):
                                                                                t instance String
                                                                    returnMultiplicity(Property):
                                                                        PureOne instance PackageableMultiplicity
                                                                    returnType(Property):
                                                                        Anonymous_StripedId instance InferredGenericType
                                                                            rawType(Property):
                                                                                Boolean instance PrimitiveType
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
                                                        equal_Any_MANY__Any_MANY__Boolean_1_ instance NativeFunction
                                                    functionName(Property):
                                                        equal instance String
                                                    genericType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Boolean instance PrimitiveType
                                                    importGroup(Property):
                                                        import_fromString_pure_1 instance ImportGroup
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    parametersValues(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                            func(Property):
                                                                name instance Property
                                                                    aggregation(Property):
                                                                        None instance AggregationKind
                                                                            name(Property):
                                                                                None instance String
                                                                    applications(Property):
                                                                        Anonymous_StripedId instance SimpleFunctionExpression
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
                                                                                    propertyName(Property):
                                                                                        classifierGenericType instance String
                                                                            typeArguments(Property):
                                                                                Anonymous_StripedId instance GenericType
                                                                                    rawType(Property):
                                                                                        Anonymous_StripedId instance ImportStub
                                                                                            idOrPath(Property):
                                                                                                Employee instance String
                                                                                            importGroup(Property):
                                                                                                import_fromString_pure_1 instance ImportGroup
                                                                                            resolvedNode(Property):
                                                                                                Employee instance Class
                                                                                    referenceUsages(Property):
                                                                                        Anonymous_StripedId instance ReferenceUsage
                                                                                            offset(Property):
                                                                                                0 instance Integer
                                                                                            owner(Property):
                                                                                                Anonymous_StripedId instance GenericType
                                                                                            propertyName(Property):
                                                                                                typeArguments instance String
                                                                                Anonymous_StripedId instance GenericType
                                                                                    rawType(Property):
                                                                                        String instance PrimitiveType
                                                                                    referenceUsages(Property):
                                                                                        Anonymous_StripedId instance ReferenceUsage
                                                                                            offset(Property):
                                                                                                1 instance Integer
                                                                                            owner(Property):
                                                                                                Anonymous_StripedId instance GenericType
                                                                                            propertyName(Property):
                                                                                                typeArguments instance String
                                                                    genericType(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                            rawType(Property):
                                                                                String instance PrimitiveType
                                                                    multiplicity(Property):
                                                                        PureOne instance PackageableMultiplicity
                                                                    name(Property):
                                                                        name instance String
                                                                    owner(Property):
                                                                        Employee instance Class
                                                            genericType(Property):
                                                                Anonymous_StripedId instance InferredGenericType
                                                                    rawType(Property):
                                                                        String instance PrimitiveType
                                                            importGroup(Property):
                                                                import_fromString_pure_1 instance ImportGroup
                                                            multiplicity(Property):
                                                                PureOne instance PackageableMultiplicity
                                                            parametersValues(Property):
                                                                Anonymous_StripedId instance VariableExpression
                                                                    genericType(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                            rawType(Property):
                                                                                Anonymous_StripedId instance ImportStub
                                                                                    idOrPath(Property):
                                                                                        Employee instance String
                                                                                    importGroup(Property):
                                                                                        import_fromString_pure_1 instance ImportGroup
                                                                                    resolvedNode(Property):
                                                                                        Employee instance Class
                                                                    multiplicity(Property):
                                                                        PureOne instance PackageableMultiplicity
                                                                    name(Property):
                                                                        t instance String
                                                                    usageContext(Property):
                                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                                            functionExpression(Property):
                                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                            offset(Property):
                                                                                0 instance Integer
                                                            propertyName(Property):
                                                                getValue$1$system$imports$import_fromString_pure_1$0 instance InstanceValue
                                                                    genericType(Property):
                                                                        Anonymous_StripedId instance GenericType
                                                                            rawType(Property):
                                                                                String instance PrimitiveType
                                                                    multiplicity(Property):
                                                                        PureOne instance PackageableMultiplicity
                                                                    values(Property):
                                                                        name instance String
                                                            usageContext(Property):
                                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                                    functionExpression(Property):
                                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                                    offset(Property):
                                                                        0 instance Integer
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
                                                                cool instance String
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                                            functionDefinition(Property):
                                                                getValue$1$system$imports$import_fromString_pure_1$1 instance LambdaFunction
                                                            offset(Property):
                                                                0 instance Integer
                                            referenceUsages(Property):
                                                Anonymous_StripedId instance ReferenceUsage
                                                    offset(Property):
                                                        0 instance Integer
                                                    owner(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    propertyName(Property):
                                                        values instance String
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        getValue_Any_1__String_1__Any_MANY_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        0 instance Integer
                    functionName(Property):
                        getValue instance String
                    name(Property):
                        getValue_Any_1__String_1__Any_MANY_ instance String
                    package(Property):
                        Root instance Package\
                """, runtime.getCoreInstance("getValue_Any_1__String_1__Any_MANY_").printWithoutDebug("", 10));

    }

}
