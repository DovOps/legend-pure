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

package org.finos.legend.pure.runtime.java.compiled;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.PureCompiledExecutionException;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestGenerationWithPureStacktraceIncluded extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), getCodeStorage(), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testExceptionStacktrace()
    {
        String genericFunc = """
                function generic():Any[1]
                {
                   55;
                }
                """;
        compileTestSource("genericFunc.pure", genericFunc);

        String icFunc = """
                function invalidCast():String[1]
                {
                   generic()->cast(@String);\
                }
                """;
        compileTestSource("invalidCast.pure", icFunc);
        String noOPFunc = """
                function noOP():Any[*]
                {
                   invalidCast();\
                }
                """;
        compileTestSource("noOP.pure", noOPFunc);
        compileTestSource("pureStacktaceTest.pure",
                """
                function test():Any[*]
                {
                   'whatever';noOP();
                }
                """);
        PureCompiledExecutionException e = Assertions.assertThrows(PureCompiledExecutionException.class, () -> execute("test():Any[*]"));
        StringBuilder sb = new StringBuilder();
        e.printPureStackTrace(sb);
        Assertions.assertEquals("""
                resource:invalidCast.pure line:3 column:15
                resource:noOP.pure line:3 column:4
                resource:pureStacktaceTest.pure line:3 column:15
                """, sb.toString());
    }

    @Test
    public void testCompileWithThisReference()
    {
        String code =
                """
                Class A { qp(){ func55($this); }:Number[1]; }\s
                function func55(a:A[1]):Number[1] { 55; }\
                """;
        compileTestSource("fromString.pure", code);
    }

    @Test
    public void testToOneCompilation()
    {
        String code = """
                Class JsiProperty{ name:String[1]; }\s
                
                function jsiProperty(prop: AbstractProperty<Any>[*]):JsiProperty[1] { ^JsiProperty(name='ta da'); }
                
                function getJsiProperties(class: Class<Any>[1], pathToRoot:AbstractProperty<Any>[*]): JsiProperty[*]
                {  \s
                   let current = \s
                      $class.properties->map(pt | $pt.genericType.rawType->toOne()->match([
                         {pr:PrimitiveType[1] | jsiProperty($pathToRoot->add($pt)->toOneMany()) },                       \s
                         {et:Enumeration<Any>[1] | jsiProperty($pathToRoot->add($pt)->toOneMany()) },
                         {a:Class<Any>[1] |\s
                          let newPath = $pathToRoot->add($pt);
                          let keyProps = $a.properties;\s
                          let props = if(true, | jsiProperty($pt), | if($a.properties->size() > 1,\s
                                                                        | if($keyProps->isEmpty(), | $a->getJsiProperties($newPath),\s
                                  | $keyProps->toOneMany()->map(aapt | $aapt->jsiProperty())),
                                  | $a.properties->cast(@AbstractProperty<Any>)->toOne()->jsiProperty()));
                                 \s
                          $props;
                         }         \s
                         ]));     \s
                     \s
                   $current;           \s
                }\
                """;
        compileTestSource("fromString.pure", code);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().shouldIncludePureStackTrace().build();
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
    }
}
