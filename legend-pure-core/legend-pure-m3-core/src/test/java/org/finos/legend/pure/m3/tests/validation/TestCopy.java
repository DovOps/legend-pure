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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCopy extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testModel.pure");
        runtime.delete("testFunc.pure");
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
                    let a = ^A(prop='the quick brown fox');
                    ^$a(prop=1);\
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: Integer not a subtype of String", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
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
                    let a = ^A(prop=^B());
                    ^$a(prop=^C());
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: C not a subtype of B", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
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
                    let a = ^A(prop=^B());
                    ^$a(prop='the quick brown fox');
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: String not a subtype of B", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
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
                    let a = ^A(prop='the quick brown fox');
                    ^$a(prop=^B());
                }\
                """));
        assertPureException(PureCompilationException.class, "Type Error: B not a subtype of String", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
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
                    let a = ^A(prop='one string');
                    ^$a(prop=['one string', 'two string', 'red string', 'blue string']);
                }\
                """));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [4] is not compatible with [1]", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
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
                    let a = ^A(prop='one string');
                    ^$a(prop=someStrings());
                }\
                """));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 4, 13, 4, 13, 4, 13, e);
    }

    @Test
    public void testIncompatibleExpressionMultiplicity_Deep()
    {
        compileTestSource("testModel.pure",
                """
                Class A
                {
                    toB:B[1];
                }
                
                Class B
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
                    let a = ^A(toB=^B(prop='one string'));
                    ^$a(toB.prop=someStrings());
                }\
                """));
        assertPureException(PureCompilationException.class, "Multiplicity Error: [*] is not compatible with [1]", "testFunc.pure", 4, 17, 4, 17, 4, 17, e);
    }
}
