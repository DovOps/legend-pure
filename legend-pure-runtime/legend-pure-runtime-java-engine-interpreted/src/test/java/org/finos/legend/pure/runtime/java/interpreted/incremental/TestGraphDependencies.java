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

package org.finos.legend.pure.runtime.java.interpreted.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class TestGraphDependencies extends AbstractPureTestWithCoreCompiled
{
//    @Test
//    public void testProperty()
//    {
//        compileTestSource("Class A\n" +
//                "{\n" +
//                "   prop:String[1];\n" +
//                "}\n" +
//                "\n" +
//                "function go():Nil[0]\n" +
//                "{\n" +
//                "   let b = ^A(prop = 'ok');\n" +
//                "   print($b.prop);\n" +
//                "   print(A.property('prop'));\n" +
//                "}");
//        this.execute("go():Nil[0]");
//        Assert.assertEquals("hello__String_1_ instance SimpleFunctionDefinition\n", this.functionExecution.getConsole().getLine(1));
//    }

    @Disabled
    @Test
    public void testFunctionApplicationsAndFunctionExpressionContext()
    {
        compileTestSource("""
                          Class A
                          {
                             b(){'3';hello();'1';}:String[1];
                          }
                          
                          function other(s:String[1]):String[1]
                          {
                             $s
                          }
                          
                          function hello():String[1]
                          {
                             'a';
                          }
                          
                          function omg():Nil[0]
                          {
                             2;
                             5;
                             other(hello());
                             hello();
                             [];
                          }
                          
                          function go():Boolean[1]
                          {
                              hello();
                              let r = hello__String_1_;
                              assertEq(4, $r.applications->size());
                              assertEq(1, $r.referenceUsages->size());
                              let set = $r.applications->evaluateAndDeactivate().usageContext->map(u|$u->match([
                                                                                                                   a:ExpressionSequenceValueSpecificationContext[1]|$a.functionDefinition.functionName->toOne()+'[expr]',
                                                                                                                   a:ParameterValueSpecificationContext[1]|$a.functionExpression.func.functionName->toOne()+'[param]'
                                                                                                                   ])
                                                                                                      );
                              assertEq('b[expr],go[expr],omg[expr],other[param]', $set->sort({a,b|$a->compare($b)})->makeString(','));
                          }\
                          """);
        this.execute("go():Boolean[1]");
    }

}