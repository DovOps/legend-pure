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

package org.finos.legend.pure.m3.tests.function.base.math;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestRound extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testBasic()
    {
        compileTestSource("fromString.pure",
                """
                function test():Boolean[1]
                {
                   assert(2 == 1.9->round(), |'');
                }
                """);
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testBasicScale()
    {
        compileTestSource("fromString.pure",
                """
                function test():Boolean[1]
                {
                   assert(1.9 == 1.923->round(1), |'');
                   assert(1.92 == 1.923->round(2), |'');
                   assert(1.93 == 1.925->round(2), |'');
                }
                """);
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testEval()
    {
        compileTestSource("fromString.pure",
                """
                function test():Boolean[1]
                {
                   assert(2 == round_Number_1__Integer_1_->eval(1.9), |'');
                }
                """);
        this.execute("test():Boolean[1]");
    }
}
