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

package org.finos.legend.pure.m3.tests.validation;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNewInstance extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("testModel.pure");
        runtime.delete("testFunc.pure");
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleGeneralizationUnknownProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class A
                {
                   propA:String[1];
                }
                Class B extends A
                {
                   propB:String[1];
                }
                function simpleTest():B[1]
                {
                   ^B(propA='iA',propB='iB',random='oll');
                }\
                """));
        assertPureException(PureCompilationException.class, "The property 'random' can't be found in the type 'B' or in its hierarchy.", 11, 29, e);
    }

    @Test
    public void testIncompatiblePrimitiveTypes()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:String[1];
                }\
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=1)
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: Integer not a subtype of String", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleClasses()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:B[1];
                }
                
                Class B
                {
                }
                
                Class C
                {
                }\
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=^C())
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: C not a subtype of B", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleMixedTypes1()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:B[1];
                }
                
                Class B
                {
                }\
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop='the quick brown fox')
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: String not a subtype of B", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleMixedTypes2()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:String[1];
                }
                
                Class B
                {
                }\
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=^B())
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: B not a subtype of String", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleInstanceValueMultiplicity()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:String[1];
                }
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=['one string', 'two string', 'red string', 'blue string'])
                }\
                """));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [4] is not compatible with [1]", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testIncompatibleExpressionMultiplicity()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:String[1];
                }
                
                function someStrings():String[*]
                {
                    ['one string', 'two string', 'red string', 'blue string'];
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=someStrings())
                }\
                """));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testMissingRequiredProperty()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    optionalProp:Integer[*];
                    requiredProp:String[1];
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(optionalProp=[1, 2, 3])
                }\
                """));
        assertPureException(PureCompilationException.class, "Missing value(s) for required property 'requiredProp' which has a multiplicity of [1] for type A", "testFunc.pure", 3, 5, 3, 5, 3, 30, e);
    }

    @Test
    public void testChainedProperties()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:String[1];
                }
                
                Class B
                {
                    propToA:A[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():B[1]
                {
                    ^B(propToA.prop='string')
                }\
                """));
        assertPureException(PureCompilationException.class, "Chained properties are not allowed in new expressions", "testFunc.pure", 3, 8, 3, 8, 3, 16, e);
    }

    @Test
    public void testNewBoolean()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^Boolean()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Boolean", "testSource.pure", 3, 5, 3, 5, 3, 14, e);
    }

    @Test
    public void testNewDate()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^Date()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Date", "testSource.pure", 3, 5, 3, 5, 3, 11, e);
    }

    @Test
    public void testNewFloat()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^Float()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Float", "testSource.pure", 3, 5, 3, 5, 3, 12, e);
    }

    @Test
    public void testNewInteger()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^Integer()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Integer", "testSource.pure", 3, 5, 3, 5, 3, 14, e);
    }

    @Test
    public void testNewNumber()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^Number()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: Number", "testSource.pure", 3, 5, 3, 5, 3, 13, e);
    }

    @Test
    public void testNewEnumeration()
    {
        compileTestSource("fromString.pure", "Enum TestEnum {VAL1, VAL2}");

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                function test():Any[*]
                {
                    ^TestEnum()
                }\
                """));
        assertPureException(PureCompilationException.class, "Cannot instantiate a non-Class: TestEnum", "testSource.pure", 3, 5, 3, 5, 3, 15, e);
    }

    @Test
    public void testTimeWithStrictDateType()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:StrictDate[1];
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=%2014-02-07T07:03:01)
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: DateTime not a subtype of StrictDate", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testTimeWithWrongStrictTimeType()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:StrictTime[1];
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=%2014-02-07T07:03:01)
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: DateTime not a subtype of StrictTime", "testFunc.pure", 3, 12, 3, 12, 3, 12, e);
    }

    @Test
    public void testTimeWithStrictTimeType()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    prop:StrictTime[1];
                }\
                """);

        compileTestSource(
                "testFunc.pure",
                """
                function testFunc():A[1]
                {
                    ^A(prop=%07:03:01)
                }\
                """);

    }

    @Test
    public void testFunctionSignature()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                }\
                Class B{}\
                Class C{}\
                Class meta::pure::router::extension::RouterExtension
                {
                   analytics_getStoreFromSetImpl : LambdaFunction<{A[1] -> LambdaFunction<{Nil[1]->C[1]}>[1]}>[0..1];
                }\
                function a():Any[0..1]\
                {\
                 ^meta::pure::router::extension::RouterExtension(\
                   analytics_getStoreFromSetImpl = a:A[1]|{b:B[1]|^C()}\
                 )\
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFunc.pure",
                """
                Class meta::pure::router::extension::RouterExtension2
                {
                   analytics_getStoreFromSetImpl : LambdaFunction<{A[1] -> LambdaFunction<{Any[1]->C[1]}>[1]}>[0..1];
                }
                
                function za():Any[0..1]
                {
                 ^meta::pure::router::extension::RouterExtension2(
                   analytics_getStoreFromSetImpl = a:A[1]|{b:B[1]|^C()}
                 )
                }
                """));
        assertPureException(PureCompilationException.class, "Type Error: LambdaFunction<{A[1]->LambdaFunction<{B[1]->C[1]}>[1]}> not a subtype of LambdaFunction<{A[1]->LambdaFunction<{Any[1]->C[1]}>[1]}>", "testFunc.pure", 9, 34, 9, 34, 9, 34, e);
    }

    @Test
    public void testNewSourceInformation()
    {
        String text = """
                Class a::b::c::FirmX
                {
                    prop:String[1];
                }
                function testFunc():a::b::c::FirmX[1]
                {
                    ^a::b::c::FirmX(prop='one string')
                }
                """;
        compileTestSource("testModel.pure",
                text);
        CoreInstance a = runtime.getCoreInstance("a::b::c::FirmX");
        ListIterable<? extends CoreInstance> referenceUsages = a.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        Verify.assertSize(3, referenceUsages);

        ListIterable<SourceInformation> sourceInformations = referenceUsages.collect(coreInstance -> coreInstance.getValueForMetaPropertyToOne(M3Properties.owner).getValueForMetaPropertyToOne(M3Properties.rawType).getSourceInformation());
        String[] lines = text.split("\n");
        Verify.assertSize(3, sourceInformations);

        for (SourceInformation sourceInformation : sourceInformations)
        {
            Assertions.assertEquals("FirmX", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getEndColumn()));
        }
    }

    @Test
    public void testNewInstanceWithNonHomogenousTypeParameters()
    {
        compileTestSource("testModel.pure",
                """
                import a::b::c::*;
                Class a::b::c::ClassWithTypeParameter<T>
                {
                    value:T[1];
                }
                
                Class a::b::c::SubclassWithoutTypeParameter extends ClassWithTypeParameter<String>
                {
                }
                
                Class a::b::c::ClassWrapper
                {
                    classWithTypeParameter:Class<ClassWithTypeParameter<Any>>[1];
                }
                
                function a::b::c::testFn():Any[*]
                {
                    ^ClassWrapper(classWithTypeParameter=a::b::c::SubclassWithoutTypeParameter);
                }
                """);
    }
}
