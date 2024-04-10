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

package org.finos.legend.pure.runtime.java.compiled.modeling.function;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestLambdasWithTypeParameters extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("/test/testSource1.pure");
        runtime.compile();
    }

    @Test
    public void testLambdaWithNotFullyConcreteReturnType()
    {
        compileTestSource(
                "/test/testSource1.pure",
                """
                import test::*;
                
                Class test::Result<T|m>
                {
                  values:T[m];
                  other:Any[*];
                }
                
                function test::singleOther<T>(other:Any[1], object:T[0..1]):Result<T|0..1>[1]
                {
                  ^Result<T|0..1>(other=$other)
                }
                
                function test::singleValue<T>(value:T[1]):Result<T|0..1>[1]
                {
                  ^Result<T|0..1>(values=$value)
                }
                
                function test::expand<T>(result:Result<T|*>[1]):Result<T|0..1>[*]
                {
                  if($result.values->isEmpty(),
                     | $result.other->map(o | singleOther($o, @T)),
                     | $result.values->map(v | singleValue($v)))
                }
                
                function test::test():Any[*]
                {
                  expand(^Result<String|0>(other=[1, 2, 3, 4]))
                }
                """);
        CoreInstance test = runtime.getFunction("test::test():Any[*]");
        Assertions.assertNotNull(test);
        CoreInstance result = functionExecution.start(test, Lists.immutable.empty());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof InstanceValue);
        Assertions.assertEquals(4, ((InstanceValue) result)._values().size());
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
