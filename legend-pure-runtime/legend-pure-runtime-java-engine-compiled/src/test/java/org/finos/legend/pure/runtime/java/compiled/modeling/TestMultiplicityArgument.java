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

package org.finos.legend.pure.runtime.java.compiled.modeling;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMultiplicityArgument extends AbstractPureTestWithCoreCompiled
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
        runtime.delete("fromString2.pure");
        runtime.delete("fromString3.pure");
    }

    @Test
    public void testMulti()
    {
        compileTestSource("fromString.pure", """
                Class JSONResult<P|m> extends ServiceResult<P|m>
                {\
                \s
                }\
                function meta::pure::functions::meta::hasUpperBound(multiplicity:Multiplicity[1]):Boolean[1]
                {
                    let upperBound = $multiplicity.upperBound;
                    !$upperBound->isEmpty() && !$upperBound->toOne().value->isEmpty() && $upperBound->toOne().value != -1;
                }\
                function meta::pure::functions::meta::hasToOneUpperBound(multiplicity:Multiplicity[1]):Boolean[1]
                {
                    $multiplicity->hasUpperBound() && eq($multiplicity->getUpperBound(), 1)
                }\
                function meta::pure::functions::meta::getUpperBound(multiplicity:Multiplicity[1]):Integer[1]
                {
                    $multiplicity.upperBound->toOne().value->toOne()
                }
                function meta::pure::functions::meta::getLowerBound(multiplicity:Multiplicity[1]):Integer[1]
                {
                    $multiplicity.lowerBound->toOne().value->toOne()
                }\
                function meta::pure::functions::meta::isToOne(multiplicity:Multiplicity[1]):Boolean[1]
                {
                    hasToOneUpperBound($multiplicity) && eq($multiplicity->getLowerBound(), 1)
                }\
                function test(v:ServiceResult<Any|*>[1]):Boolean[1]
                {
                   $v->match(j:JSONResult<Any|*>[1]| $j.value->match([a:Any[*]|$j.classifierGenericType.multiplicityArguments->at(0)->isToOne()]))
                }\
                function test():Any[*]
                {\
                  assert(^JSONResult<String|1>(value='hello')->test(), |'');\
                  assert(!^JSONResult<String|0..1>(value='hello')->test(), |'');\
                  assert(!^JSONResult<String|*>(value='hello')->test(), |'');\
                  assert(!^JSONResult<String|1..2>(value='hello')->test(), |'');\
                  assert(!^JSONResult<String|0..*>(value='hello')->test(), |'');\
                }
                """);
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testFunctionWithReturnMultiplicityParameter()
    {
        String source = """
                function test::foo<T|m>(x:T[m], y:String[1]):T[m]{$x}
                function test::bar(s:String[1]):String[1] { $s + 'bar' }
                function test::testFn():Any[*] {test::foo('one string', 'two string')->test::bar()}
                """;
        this.compileTestSource("fromString.pure", source);
        this.compileAndExecute("test::testFn():Any[*]");
    }

    @Test
    public void testGenericTypeWithMultiplicityArgument()
    {
        compileTestSource("fromString.pure", """
                Class test::TestClass<|m>
                {
                  names : String[m];
                }
                """);

        compileTestSource("fromString3.pure", """
                import test::*;
                function test::testClass1():TestClass<|1>[1]
                {
                  ^TestClass<|1>(names='one name');
                }
                
                function test::testFn1():String[1]\
                {
                  let name = testClass1().names;
                  assert('one name' == $name, |'');
                  $name;\
                }
                """);

        compileTestSource("fromString2.pure", """
                import test::*;
                function test::testClass0_1():TestClass<|0..1>[1]
                {
                  ^TestClass<|0..1>(names=[]);
                }
                
                function test::testFn0_1():String[0..1]\
                {
                  let name = testClass0_1().names;
                  assert($name->isEmpty(), |'');
                  $name;\
                }
                """);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}