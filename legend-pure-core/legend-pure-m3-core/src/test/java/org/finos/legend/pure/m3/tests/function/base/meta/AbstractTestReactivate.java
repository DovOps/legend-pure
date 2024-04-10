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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestReactivate extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    protected void compileAndExecuteVariableScopeFailure()
    {
        compileTestSource("testSource.pure",
                """
                function go():Any[*]
                {
                   let a = 7; \s
                   let t = {|{| $a + 3};};
                  \s
                   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);
                  \s
                   assert('a' == $t->openVariableValues()->keys()->first(), |'');
                   assert(0 == $l->openVariableValues()->keys()->size(), |'');
                   let z = r($l);
                   print($z, 1);
                   false;
                }
                
                function r(l:LambdaFunction<Any>[1]):Any[*]
                {
                   $l.expressionSequence->at(0)->reactivate(^Map<String, List<Any>>());
                }\
                """
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeSuccess()
    {
        compileTestSource("testSource.pure",
                """
                Class meta::pure::executionPlan::PlanVarPlaceHolder
                {
                    name : String[1];
                }\
                function go():Any[*]
                {
                   let a = 7; \s
                   let t = {|{| $a + 3};};
                  \s
                   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);
                  \s
                   assert('a' == $t->openVariableValues()->keys()->first(), |'');
                   assert(0 == $l->openVariableValues()->keys()->size(), |'');
                   let z = r($l, $t->openVariableValues());
                   assert(10 == $z, |'');
                   false;
                }
                
                function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]
                {
                   $l.expressionSequence->at(0)->reactivate($vars);
                }\
                """
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeSuccessWithList()
    {
        compileTestSource("testSource.pure",
                """
                Class meta::pure::executionPlan::PlanVarPlaceHolder
                {
                    name : String[1];
                }
                function meta::pure::functions::math::sum(numbers:Integer[*]):Integer[1]
                {
                    $numbers->plus();
                }
                function go():Any[*]
                {
                   let a = [7,3]; \s
                   let t = {|{| $a->sum() + 3};};
                  \s
                   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);
                  \s
                   assert('a' == $t->openVariableValues()->keys()->first(), |'');
                   assert(0 == $l->openVariableValues()->keys()->size(), |'');
                   let z = r($l, $t->openVariableValues());
                   assert(13 == $z, |'');
                   false;
                }
                
                function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]
                {
                   $l.expressionSequence->at(0)->reactivate($vars);
                }\
                """
        );
        execute("go():Any[*]");
    }


    @Test
    public void testVariableScopeWithEmpty()
    {
        compileTestSource("testSource.pure",
                """
                function go():Any[*]
                {
                   let a = []->cast(@Integer); \s
                   let t = {|{| $a->sum() + 3};};
                  \s
                   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);
                  \s
                   assert('a' == $t->openVariableValues()->keys()->first(), |'');
                   assert(0 == $l->openVariableValues()->keys()->size(), |'');
                   let z = r($l, $t->openVariableValues());
                   assert(3 == $z, |'');
                   false;
                }
                
                function meta::pure::functions::math::sum(numbers:Integer[*]):Integer[1]
                {
                    $numbers->plus();
                }
                function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]
                {
                   $l.expressionSequence->at(0)->reactivate($vars);
                }\
                """
        );
        execute("go():Any[*]");
    }

    @Test
    public void testVariableScopeWithEmptyNested()
    {
        compileTestSource("testSource.pure",
                """
                function go():Any[*]
                {
                   let a = []->cast(@Integer); \s
                   let t = {|{| [1,2,3]->filter(b|true && $a->contains(2))}};
                  \s
                   let l = $t.expressionSequence->at(0)->cast(@InstanceValue)->evaluateAndDeactivate().values->at(0)->cast(@LambdaFunction<Any>);
                  \s
                   assert('a' == $t->openVariableValues()->keys()->first(), |'');
                   assert(0 == $l->openVariableValues()->keys()->size(), |'');
                   let z = r($l, $t->openVariableValues());
                   assert([] == $z, |'');
                   false;
                }
                
                function r(l:LambdaFunction<Any>[1],vars:Map<String, List<Any>>[1]):Any[*]
                {
                   $l.expressionSequence->at(0)->reactivate($vars);
                }\
                """
        );
        execute("go():Any[*]");
    }

    @Test
    public void testReactivateFunctionExpressionWithPackageArg()
    {
        compileTestSource("testSource.pure",
                """
                function test::pkg1::test():Any[1]
                {
                  test::pkg1->map(p | $p)->deactivate()->reactivate(^Map<String, List<Any>>())->toOne()
                }
                """);
        Assertions.assertEquals(runtime.getCoreInstance("test::pkg1"), ((InstanceValue) execute("test::pkg1::test():Any[1]"))._values().getOnly());
    }
}
