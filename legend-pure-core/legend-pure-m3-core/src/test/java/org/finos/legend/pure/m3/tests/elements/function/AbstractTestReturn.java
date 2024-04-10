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
import org.junit.jupiter.api.Test;

public abstract class AbstractTestReturn extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testReturn()
    {
        compileTestSource("fromString.pure","""
                function funcWithReturn():String[1]
                {
                   'Hello';
                }
                function test():Nil[0]
                {
                   assertEquals('Hello', funcWithReturn());\
                   [];
                }\
                """);
        this.execute("test():Nil[0]");
     }

    @Test
    public void testReturnWithInheritance()
    {
        compileTestSource("fromString.pure","""
                Class TypeA
                {
                   name : String[1];
                }
                Class TypeB extends TypeA
                {
                   moreName : String[1];
                }
                function funcWithReturn():TypeA[1]
                {
                   ^TypeB(moreName='xxx', name='aaa');
                }
                function test():Nil[0]
                {
                   assertEquals('aaa', funcWithReturn().name);\
                   [];
                }\
                """);
        this.execute("test():Nil[0]");
    }

    @Test
    public void testReturnWithMultiplicityMany()
    {
        compileTestSource("fromString.pure","""
                function process():String[*]
                {
                    ['a','b']
                }
                
                function test():Nil[0]
                {
                   assertEquals('a__b', process()->joinStrings('__'));\
                   [];
                }
                """);
        this.execute("test():Nil[0]");
    }
}
