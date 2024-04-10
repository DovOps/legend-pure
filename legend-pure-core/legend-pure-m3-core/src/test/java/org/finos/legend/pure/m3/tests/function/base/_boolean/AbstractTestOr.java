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

package org.finos.legend.pure.m3.tests.function.base._boolean;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestOr extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testBasicParse()
    {
        compileTestSource("fromString.pure",
                """
                function test():Boolean[1]
                {
                   assert(true == or(true, true), |'');
                   assert(true == or(false, true), |'');
                   assert(true == or(true, false), |'');
                   assert(false == or(false, false), |'');
                }
                """);
        execute("test():Boolean[1]");
    }

    @Test
    public void testEvalParse()
    {
        compileTestSource("fromString.pure",
                """
                function test():Boolean[1]
                {
                   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(true, true), |'');
                   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(false, true), |'');
                   assert(true == or_Boolean_1__Boolean_1__Boolean_1_->eval(true, false), |'');
                   assert(false == or_Boolean_1__Boolean_1__Boolean_1_->eval(false, false), |'');
                }
                """);
        execute("test():Boolean[1]");
    }

    @Test
    public void testShortCircuit()
    {
        compileTestSource("fromString.pure",
                """
                Class A
                {
                    value:Any[1];
                }
                
                Class B
                {
                    name:String[1];
                }
                
                function test():Boolean[1]
                {
                   let a1 = ^A(value=^B(name='Claudius Ptolemy'));
                   let a2 = ^A(value=1);
                   assertFalse(!$a1.value->instanceOf(B) || ($a1.value->cast(@B).name != 'Claudius Ptolemy'));
                   assert(!$a2.value->instanceOf(B) || ($a2.value->cast(@B).name != 'Claudius Ptolemy'));
                }
                """);
        execute("test():Boolean[1]");
    }

    @Test
    public void testShortCircuitInDynamicEvaluation()
    {
        compileTestSource("fromString.pure",
                """
                Class A
                {
                    value:Any[1];
                }
                
                Class B
                {
                    name:String[1];
                }
                
                function test():Boolean[1]
                {
                   let fn1 = {|let a = ^A(value=^B(name='Claudius Ptolemy'));
                               !$a.value->instanceOf(B) || ($a.value->cast(@B).name != 'Claudius Ptolemy');};
                   let lambda1 = ^LambdaFunction<{->Boolean[1]}>(expressionSequence = $fn1.expressionSequence);
                   assertEquals(false, $lambda1->evaluate([]));
                   let fn2 = {|let a = ^A(value=1);
                               !$a.value->instanceOf(B) || ($a.value->cast(@B).name != 'Claudius Ptolemy');};
                   let lambda2 = ^LambdaFunction<{->Boolean[1]}>(expressionSequence = $fn2.expressionSequence);
                   assertEquals(true, $lambda2->evaluate([]));
                }
                """);
        execute("test():Boolean[1]");
    }
}
