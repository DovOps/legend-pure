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

package org.finos.legend.pure.m3.tests.function.base.io;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestPrint extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testPrint()
    {
        compileTestSource("fromString.pure","""
                function testPrint():Nil[0]
                {
                    print('Hello World', 1);
                }
                """);
        this.execute("testPrint():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testArrowWithAFunctionWithNoParameters()
    {
        compileTestSource("fromString.pure","""
                function testArrowWithFunctionNoParameters():Nil[0]
                {
                    'a'->print(1);
                }
                """);
        this.execute("testArrowWithFunctionNoParameters():Nil[0]");
        Assertions.assertEquals("'a'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintInteger()
    {
        compileTestSource("fromString.pure","""
                function testPrintInteger():Nil[0]
                {
                    print(123, 1);
                }
                """);
        this.execute("testPrintInteger():Nil[0]");
        Assertions.assertEquals("123", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintFloat()
    {
        compileTestSource("fromString.pure","""
                function testPrintFloat():Nil[0]
                {
                    print(123.456, 1);
                }
                """);
        this.execute("testPrintFloat():Nil[0]");
        Assertions.assertEquals("123.456", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintDate()
    {
        compileTestSource("fromString.pure","""
                function testPrintDate():Nil[0]
                {
                    print(%2016-07-08, 1);
                }
                """);
        this.execute("testPrintDate():Nil[0]");
        Assertions.assertEquals("2016-07-08", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintBoolean()
    {
        compileTestSource("fromString.pure","""
                function testPrintBoolean():Nil[0]
                {
                    print(true, 1);
                }
                """);
        this.execute("testPrintBoolean():Nil[0]");
        Assertions.assertEquals("true", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintIntegerCollection()
    {
        compileTestSource("fromString.pure","""
                function testPrintIntegerCollection():Nil[0]
                {
                    print([1, 2, 3], 1);
                }
                """);
        this.execute("testPrintIntegerCollection():Nil[0]");
        Assertions.assertEquals("""
                [
                   1
                   2
                   3
                ]\
                """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintFloatCollection()
    {
        compileTestSource("fromString.pure","""
                function testPrintFloatCollection():Nil[0]
                {
                    print([1.0, 2.5, 3.0], 1);
                }
                """);
        this.execute("testPrintFloatCollection():Nil[0]");
        Assertions.assertEquals("""
                [
                   1.0
                   2.5
                   3.0
                ]\
                """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintDateCollection()
    {
        compileTestSource("fromString.pure","""
                function testPrintDateCollection():Nil[0]
                {
                    print([%1973-11-13T23:09:11, %2016-07-08], 1);
                }
                """);
        this.execute("testPrintDateCollection():Nil[0]");
        Assertions.assertEquals("""
                [
                   1973-11-13T23:09:11+0000
                   2016-07-08
                ]\
                """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintStringCollection()
    {
        compileTestSource("fromString.pure","""
                function testPrintStringCollection():Nil[0]
                {
                    print(['testString', '2.5', '%2016-07-08'], 1);
                }
                """);
        this.execute("testPrintStringCollection():Nil[0]");
        Assertions.assertEquals("""
                [
                   'testString'
                   '2.5'
                   '%2016-07-08'
                ]\
                """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintMixedCollection()
    {
        compileTestSource("fromString.pure","""
                function testPrintMixedCollection():Nil[0]
                {
                    print([1, 2.5, 'testString', %2016-07-08], 1);
                }
                """);
        this.execute("testPrintMixedCollection():Nil[0]");
        Assertions.assertEquals("""
                            [
                               1
                               2.5
                               'testString'
                               2016-07-08
                            ]\
                            """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testFunction()
    {
        compileTestSource("fromString.pure",
                          """
                          function tst():String[1]{let c = 'a'+'b'; $c+'c';}
                          
                          function test():Nil[0]
                          {
                             print(tst__String_1_,2);
                          }
                          """);
        this.compileAndExecute("test():Nil[0]");
        Assertions.assertEquals("""
                tst__String_1_ instance ConcreteFunctionDefinition
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] ConcreteFunctionDefinition instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [>2] Anonymous_StripedId instance FunctionType
                    expressionSequence(Property):
                        Anonymous_StripedId instance SimpleFunctionExpression
                            func(Property):
                                [X] letFunction_String_1__T_m__T_m_ instance NativeFunction
                            functionName(Property):
                                letFunction instance String
                            genericType(Property):
                                Anonymous_StripedId instance InferredGenericType
                                    rawType(Property):
                                        [X] String instance PrimitiveType
                            importGroup(Property):
                                [X] import_fromString_pure_1 instance ImportGroup
                            multiplicity(Property):
                                [X] PureOne instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        [>2] Anonymous_StripedId instance GenericType
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext
                                    values(Property):
                                        [>2] c instance String
                                Anonymous_StripedId instance SimpleFunctionExpression
                                    func(Property):
                                        [X] plus_String_MANY__String_1_ instance ConcreteFunctionDefinition
                                    functionName(Property):
                                        [>2] plus instance String
                                    genericType(Property):
                                        [>2] Anonymous_StripedId instance InferredGenericType
                                    importGroup(Property):
                                        [X] import_fromString_pure_1 instance ImportGroup
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    parametersValues(Property):
                                        [>2] Anonymous_StripedId instance InstanceValue
                                    usageContext(Property):
                                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        [X] tst__String_1_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        [>2] 0 instance Integer
                        Anonymous_StripedId instance SimpleFunctionExpression
                            func(Property):
                                [X] plus_String_MANY__String_1_ instance ConcreteFunctionDefinition
                            functionName(Property):
                                plus instance String
                            genericType(Property):
                                Anonymous_StripedId instance InferredGenericType
                                    rawType(Property):
                                        [X] String instance PrimitiveType
                            importGroup(Property):
                                [X] import_fromString_pure_1 instance ImportGroup
                            multiplicity(Property):
                                [X] PureOne instance PackageableMultiplicity
                            parametersValues(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        [>2] Anonymous_StripedId instance GenericType
                                    multiplicity(Property):
                                        [>2] Anonymous_StripedId instance Multiplicity
                                    usageContext(Property):
                                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext
                                    values(Property):
                                        [>2] Anonymous_StripedId instance VariableExpression
                                        [>2] Anonymous_StripedId instance InstanceValue
                            usageContext(Property):
                                Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                    functionDefinition(Property):
                                        [X] tst__String_1_ instance ConcreteFunctionDefinition
                                    offset(Property):
                                        [>2] 1 instance Integer
                    functionName(Property):
                        tst instance String
                    name(Property):
                        tst__String_1_ instance String
                    package(Property):
                        [X] Root instance Package
                    referenceUsages(Property):
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        [>2] Anonymous_StripedId instance GenericType
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext
                                    values(Property):
                                        [~>] tst__String_1_ instance ConcreteFunctionDefinition
                            propertyName(Property):
                                values instance String\
                """, this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testPrintObj()
    {
        compileTestSource("fromString.pure","""
                       Class A
                       {
                           test : String[1];
                           test2 : String[1];
                       }
                       
                       function test():Nil[0]
                       {
                          print(A,0);
                       }
                       """);
        this.compileAndExecute("test():Nil[0]");
        Assertions.assertEquals("""
                A instance Class
                    classifierGenericType(Property):
                        [>0] Anonymous_StripedId instance GenericType
                    generalizations(Property):
                        [>0] Anonymous_StripedId instance Generalization
                    name(Property):
                        [>0] A instance String
                    package(Property):
                        [X] Root instance Package
                    properties(Property):
                        [>0] test instance Property
                        [>0] test2 instance Property
                    referenceUsages(Property):
                        [>0] Anonymous_StripedId instance ReferenceUsage
                        [>0] Anonymous_StripedId instance ReferenceUsage
                        [>0] Anonymous_StripedId instance ReferenceUsage\
                """, this.functionExecution.getConsole().getLine(0));
    }
}
