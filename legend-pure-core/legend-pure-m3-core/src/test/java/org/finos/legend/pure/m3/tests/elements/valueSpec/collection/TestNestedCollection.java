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

package org.finos.legend.pure.m3.tests.elements.valueSpec.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNestedCollection extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.compile();
    }

    @Test
    public void testCollectionWithNoNesting()
    {
        compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = ['a', 'b', 'c', 'd'];
                }
                """);
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithDirectNesting()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = ['e', 'f', ['g', 'h']];
                }
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 3, 24, e);
    }

    @Test
    public void testCollectionWithIllusoryNesting()
    {
        compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = ['i', ['j']];
                }
                """);
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithNonCollectionVariableExpression()
    {
        compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let x = 'k';\
                    let a = [$x, 'l'];
                }
                """);
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionVariableExpression()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let x = ['m', 'n'];
                    let a = [$x, 'o', 'p'];
                }
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 2", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
    }

    @Test
    public void testCollectionWithCollectionVariableExpressionFromFunction()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let x = 'm, n'->split(', ');
                    let a = [$x, 'o', 'p'];
                }
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 4, 15, 4, 15, 4, 15, e);
    }

    @Test
    public void testCollectionWithNonCollectionFunctionExpression()
    {
        compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = [trim('q'), 'r'];
                }
                """);
        // Expect no compilation exception
    }

    @Test
    public void testCollectionWithCollectionFunctionExpression()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = [split('s', ', '), 't', 'u'];
                }
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: *", "fromString.pure", 3, 14, e);
    }

    @Test
    public void testCollectionWithZeroOneExpression()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test(x:String[0..1]):String[*]
                {
                    ['a', $x, 'c'];
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 12, e);
    }

    @Test
    public void testCollectionWithZeroOneWrappedInCollection()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test():String[*]
                {
                    let a = [[['a', 'b']->first()], 't', 'u'];
                }
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 14, e);
    }


    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunction()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test(x:String[0..1]):String[*]
                {
                    ['a', doSomething(), 'c'];
                }\
                function doSomething():String[0..1]
                {
                    [];
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 11, e);
    }

    @Test
    public void testCollectionWithZeroOneExpressionReturnedFromFunctionWithLet()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function test(x:String[0..1]):String[*]
                {
                    let a = [doSomething()];
                    ['a', $a];
                }\
                function doSomething():String[0..1]
                {
                    [];
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 4, 12, e);
    }

    @Test
    public void testCollectionInClassConstraint()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                Class TestClass
                [
                  nonEmpty: length($this.name + $this.optionalName) > 0
                ]
                {
                    name : String[1];
                    optionalName : String[0..1];
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 39, e);
    }

    @Test
    public void testCollectionInFunctionPreConstraint()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function testFn(name:String[1], optionalName:String[0..1]):String[1]
                [
                  nonEmpty: length($name + $optionalName) > 0
                ]
                {
                    $name + if($optionalName->isEmpty(), |'', |' ' + $optionalName->toOne());
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 29, e);
    }

    @Test
    public void testCollectionInFunctionPostConstraint()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                function testFn(name:String[1], optionalName:String[0..1]):String[1]
                [
                  nonEmpty: size([$return, $optionalName]) > 0
                ]
                {
                    $name + if($optionalName->isEmpty(), |'', |' ' + $optionalName->toOne());
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 3, 29, e);
    }

    @Test
    public void testCollectionInConstraintMessageFn()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                Class TestClass
                [
                  nonEmpty
                  (
                      ~function : ($this.name->length() > 0) || (!$this.optionalName->isEmpty() && ($this.optionalName->toOne()->length() > 0))
                      ~message  : 'name (' + $this.name + ') or optionalName (' + $this.optionalName + ') must be non-empty'
                  )
                ]
                {
                    name : String[1];
                    optionalName : String[0..1];
                }\
                """));
        assertPureException(PureCompilationException.class, "Required multiplicity: 1, found: 0..1", "fromString.pure", 6, 73, e);
    }
}
