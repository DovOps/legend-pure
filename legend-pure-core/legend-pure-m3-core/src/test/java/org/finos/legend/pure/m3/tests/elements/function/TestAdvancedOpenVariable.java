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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class TestAdvancedOpenVariable extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testNestedOpenVariables()
    {
        compileTestSource("fromString.pure","""
                function func(a:String[1]):Function<{String[1]->String[1]}>[*]
                {
                     [{z:String[1]|$z+$a},{z:String[1]|$z+$a+'2'}];
                }
                
                function go():Any[*]
                {
                     assert(['ede:)', 'ede:)2'] == func(':)')->map(ok|$ok->eval('ede')),|'');
                }\
                """);
        this.execute("go():Any[*]");

    }

    @Test
    public void testOpenVariablesForPropagatedBusinessDates()
    {
        compileTestSource("fromString.pure","""
                Class <<temporal.businesstemporal>> MyClass
                {
                  account : Account[1];\s
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*], ids:String[*]):Any[*]\
                {\
                   'bla';\
                }
                
                Class <<temporal.businesstemporal>> Account
                {
                  \s
                }
                function do():Any[*]
                {
                   let bd = %2010-10-10;\
                   assert('bd' == {|MyClass.all($bd)->project(c|if(true,|$c.account,|'ee'),'e')}.expressionSequence->evaluateAndDeactivate()->cast(@FunctionExpression).parametersValues->at(1)->cast(@InstanceValue).values->cast(@LambdaFunction<Any>).openVariables, |'');\
                }\
                """);
        this.execute("do():Any[*]");

    }
}
