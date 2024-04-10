// Copyright 2023 Goldman Sachs
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestNewEnumeration extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testNewEnumeration()
    {

        compileTestSource("/test/test.pure",
                """
                function test::testFn():Any[*]
                {
                 let  testEnum =  newEnumeration('test::testEnum',['value1','value2']);\
                assert($testEnum->instanceOf(Enumeration), |'');\
                assert($testEnum->subTypeOf(Enum), |'');\
                $testEnum->enumValues()->map(e|assert($e->instanceOf(Enum), |'')); \
                $testEnum->enumValues()->map(e|$e->id())->print(1);
                }
                """);
        execute("test::testFn():Any[*]");
        Assertions.assertEquals("""
                [
                   'value1'
                   'value2'
                ]\
                """, functionExecution.getConsole().getLine(0));
    }
}
