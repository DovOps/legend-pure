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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), JavaModelFactoryRegistryLoader.loader(), getOptions(), getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("/test/testSource.pure");
        runtime.compile();
    }

    @Test
    public void testInheritedQualifiedProperty()
    {
        compileTestSource("/test/testSource.pure",
                """
                import test::*;
                Class test::TestClass1
                {
                  name : String[1];
                  getNameFunction()
                  {
                     $this.name + 'x'
                  }:String[1];
                }
                
                Class test::TestClass2 extends TestClass1
                {
                }
                
                function test::testFn():Any[*]
                {
                  assertEquals('Danielx', ^TestClass1(name='Daniel').getNameFunction());
                  assertEquals('Benedictx', ^TestClass2(name='Benedict').getNameFunction());
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @ToFix
    @Disabled
    public void testInheritedQualifiedPropertyWithTighterMultiplicity()
    {
        compileTestSource("/test/testSource.pure",
                """
                import test::*;
                Class test::TestClass1
                {
                  name : String[1];
                  getNames()
                  {
                    $this.name->split(' ')
                  }:String[*];
                }
                
                Class test::TestClass2 extends TestClass1
                {
                  getNames()
                  {
                    let x = $this.name->split(' ');\
                    $x->at($x->size()-1);
                  }:String[0..1];
                }
                
                function test::testFn():Any[*]
                {
                  ^TestClass1(name='Daniel Benedict').getNames()->joinStrings(', ') +
                    '\\n' +
                    ^TestClass2(name='Daniel Benedict').getNames()->joinStrings(', ')
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        CoreInstance result = functionExecution.start(func, Lists.immutable.empty());
        Assertions.assertEquals("Daniel, Benedict\nBenedict", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository system = GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform");
        CodeRepository test = GenericCodeRepository.build("test", "test(::.*)?", "platform", "system");
        repositories.add(system);
        repositories.add(test);
        return repositories;
    }
}
