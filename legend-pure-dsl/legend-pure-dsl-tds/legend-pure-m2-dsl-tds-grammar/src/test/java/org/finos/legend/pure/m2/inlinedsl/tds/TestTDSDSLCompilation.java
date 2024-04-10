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

package org.finos.legend.pure.m2.inlinedsl.tds;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestTDSDSLCompilation extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }

    @Test
    public void testGrammarBaselineTest()
    {
        try
        {
            runtime.createInMemorySource("file.pure",
                    """
                    function test():Any[*]
                    {
                        print(#EEW#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Parser error at (resource:file.pure line:3 column:15), expected: one of {'as', '{', '<'} found: '<EOF>'", e.getMessage());
        }
    }

    @Test
    public void testSimpleDeclarationAndSubtypeAny()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                function test():Any[*]
                {
                   print(\
                       #TDS
                         value, other, name
                         1, 3, A
                         2, 4, B
                       #\
                , 2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testSimpleDeclarationApplyFunction()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];\
                function test():Any[*]
                {
                   print(\
                       #TDS
                         value, other, name
                         1, 3, A
                         2, 4, B
                       #->rows()\
                , 2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testSimpleDeclarationUseColumnsInLambda()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                native function <<functionType.SideEffectFunction>> rows<T>(type:TDS<T>[1]):T[*];\
                function test():Any[*]
                {
                    print(\
                   #TDS
                       value, other, name
                       1, 3, A
                       2, 4, B
                   #\
                   ->rows()->map(x|$x.value->toOne() + $x.other->toOne())\
                , 2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testSimpleDeclarationUseColumnsInLambdaAndMatchTDS()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                native function meta::pure::functions::relation::filter<T>(rel:Relation<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1];
                
                function meta::pure::functions::relation::filter<T>(rel:TDS<T>[1], f:Function<{T[1]->Boolean[1]}>[1]):Relation<T>[1]
                {
                    $rel->cast(@meta::pure::metamodel::relation::Relation<T>)->meta::pure::functions::relation::filter($f);
                }\
                function test():Any[*]
                {
                   print(\
                       #TDS
                         value, other, name
                         1, 3, A
                         2, 4, B
                       #->filter(x|$x.value > 1)\
                , 2);
                }
                """);
        this.runtime.compile();

        this.runtime.modify("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                native function <<functionType.SideEffectFunction>> rows<T>(type:Relation<T>[1]):T[*];\
                function test():Any[*]
                {
                    print(\
                   #TDS
                       value, other, name
                       1, 3, A
                       2, 4, B
                   #\
                   ->rows()->map(x|$x.value->toOne() + $x.other->toOne())\
                , 2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testFunctionMatchingDeepColumn()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                import meta::pure::metamodel::relation::*;\
                function x(t:Relation<(vce:String)>[1]):Boolean[1]
                {
                  true;
                }\
                function test():Boolean[1]\
                {\
                    #TDS
                      id, name, vce
                      1, Pierre, a
                      2, Ram, e
                      3, Neema, e#
                    ->x();\
                    true;\
                }\
                """);
        this.runtime.compile();
    }
}
