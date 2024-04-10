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

package org.finos.legend.pure.runtime.java.compiled.modeling._class;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestClassWithParamSuperType extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testPropertiesWithTypeParamsAndExtends()
    {
        compileTestSource("fromString.pure",
                // class with concrete field that has type arguments - i.e Pair
                """
                Class A<K,V | m>
                {
                    test : Pair<K, V>[1];
                    withMult: String[m];
                }
                
                Class B extends A<Integer, String | 0..1>\
                {
                }
                
                Class C<X|k> extends A<String, X | k>\
                {
                }
                
                Class D extends C<String | 1>\
                {
                }
                function meta::pure::functions::collection::pair<U,V>(first:U[1], second:V[1]):Pair<U,V>[1]
                {
                   ^Pair<U,V>(first=$first, second=$second);
                }
                function test():Any[*]
                {
                  ^D(test = pair('eeee', 'ffff'), withMult = 'aaaa');
                  ^C<Float|*>(test = pair('eeee', 1.2), withMult = ['aaaa', 'aaaa']);
                  ^B(test = pair(2, 'eeee'),  withMult = []);
                  ^A<Integer, String|1>(test = pair(2, 'eeee'), withMult = 'aaaa');
                }
                """);
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testTypeParamsAndExtends()
    {
        compileTestSource("fromString.pure", """
                Class A<P>
                {
                    test : P[1];
                }\
                Class B<K> extends A<K>\
                {\
                }
                
                function test():Any[*]
                {\
                  ^B<String>(test = 'eeee');\
                  ^B<Integer>(test = 2);\
                }
                """);
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testTypeParamsMulParamsAndExtends()
    {
        compileTestSource("fromString.pure", """
                Class A<P|m>
                {
                    test : P[m];
                }\
                Class B<K|z> extends A<K|z>\
                {\
                }
                
                function test():Any[*]
                {\
                  ^B<String|1>(test = 'eeee');\
                  ^B<Integer|*>(test = [2,3]);\
                }
                """);
        this.compileAndExecute("test():Any[*]");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
