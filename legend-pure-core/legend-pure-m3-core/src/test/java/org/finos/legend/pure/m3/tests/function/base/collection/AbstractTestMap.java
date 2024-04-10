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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestMap extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("classes.pure");
        runtime.compile();
    }

    @Test
    public void testMapWithMultiplicityInferencePropertyOwner()
    {
        compileTestSource(
                "fromString.pure",
                """
                Class Employee<|m>
                {
                    prop:String[m];
                }
                
                function test():Nil[0]
                {
                    let f = [^Employee<|*>(prop=['a','b']), ^Employee<|1>(prop='b')];
                    print($f->map(e|$e.prop), 1);
                }
                """);
        execute("test():Nil[0]");
        Assertions.assertEquals(
                """
                [
                   'a'
                   'b'
                   'b'
                ]\
                """,
                functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithMultiplicityInferenceFunctionWhichIsNotAProperty()
    {
        compileTestSource(
                "fromString.pure",
                """
                function f<|m>(s:String[m]):String[m]
                {
                    $s
                }
                
                function test():Nil[0]
                {
                    print([^List<String>(values='a'), ^List<String>(values=['b','c']), ^List<String>(values='c')]->map(i|f($i.values)), 1);
                }
                """);
        execute("test():Nil[0]");
        Assertions.assertEquals(
                """
                [
                   'a'
                   'b'
                   'c'
                   'c'
                ]\
                """,
                functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testMapWithVariableThisAsParameter()
    {
        compileTestSource(
                "fromString.pure",
                """
                Class A
                {
                   func(valueFunc:Function<{A[1]->Float[1]}>[1])
                   {
                       if(true, |$this->map($valueFunc), |1.0);
                   }:Float[1];
                }
                
                function test():Nil[0]
                {
                    print(^A().func(a | 2.0), 1);
                }
                """);
        execute("test():Nil[0]");
        Assertions.assertEquals("2.0", functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testAutoMapWithZeroToOne()
    {
        compileTestSource(
                "classes.pure",
                """
                Class A
                {
                    b: B[0..1];
                }
                
                Class B
                {
                    name: String[1];
                }
                """);
        compileTestSource(
                "fromString.pure",
                """
                function test(a:A[1]):Any[*]
                {
                    $a.b.name;
                }
                """);
        CoreInstance autoMap = Automap.getAutoMapExpressionSequence(Instance.getValueForMetaPropertyToManyResolved(runtime.getCoreInstance("test_A_1__Any_MANY_"), M3Properties.expressionSequence, processorSupport).getFirst());
        Assertions.assertNotNull(autoMap);
    }

    @Test
    public void testAutoMapWithZeroToOneInEvaluate()
    {
        compileTestSource(
                "fromString.pure",
                """
                Class A
                {
                    b:B[0..1];
                }
                
                Class B
                {
                    name:String[1];
                }
                
                function test():Any[*]
                {
                    assertEquals('Akbar the Great', ^A(b=^B(name='Akbar the Great')).b.name);
                    assertEmpty(^A().b.name);
                    let fn1 = {|^A(b=^B(name='Akbar the Great')).b.name};
                    let lambda1 = ^LambdaFunction<{->String[0..1]}>(expressionSequence = $fn1.expressionSequence);
                    assertEquals('Akbar the Great', $lambda1->evaluate([]));
                    let fn2 = {|^A().b.name};
                    let lambda2 = ^LambdaFunction<{->String[0..1]}>(expressionSequence = $fn2.expressionSequence);
                    assertEmpty($lambda2->evaluate([]));
                }
                """);
        execute("test():Any[*]");
    }
}
