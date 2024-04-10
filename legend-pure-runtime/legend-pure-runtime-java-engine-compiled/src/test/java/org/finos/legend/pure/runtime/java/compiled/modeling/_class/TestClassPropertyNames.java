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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestClassPropertyNames extends AbstractPureTestWithCoreCompiled
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
    public void testClassWithJavaKeywordProperty()
    {
        compileTestSource("fromString.pure", """
                Class test::TestClass
                {
                  case : String[1];
                }
                
                function test::testFn():Boolean[1]
                {
                  let t = ^test::TestClass(case='abc');
                  'abc' == $t.case;
                }
                """);
        CoreInstance testFn = this.runtime.getFunction("test::testFn():Boolean[1]");
        Assertions.assertNotNull(testFn);
        CoreInstance result = this.functionExecution.start(testFn, Lists.immutable.<CoreInstance>empty());
        Assertions.assertTrue(result instanceof InstanceValue);
        RichIterable<?> values = ((InstanceValue) result)._values();
        Verify.assertSize(1, values);
        Object value = values.getFirst();
        Assertions.assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void testClassWithJavaKeywordQualifiedProperty()
    {
        compileTestSource("fromString.pure", """
                Class test::TestClass
                {
                  case()
                  {
                    'abc'
                  }
                : String[1];
                }
                
                function test::testFn():Boolean[1]
                {
                  let t = ^test::TestClass();
                  'abc' == $t.case;
                }
                """);
        CoreInstance testFn = this.runtime.getFunction("test::testFn():Boolean[1]");
        Assertions.assertNotNull(testFn);
        CoreInstance result = this.functionExecution.start(testFn, Lists.immutable.<CoreInstance>empty());
        Assertions.assertTrue(result instanceof InstanceValue);
        RichIterable<?> values = ((InstanceValue) result)._values();
        Verify.assertSize(1, values);
        Object value = values.getFirst();
        Assertions.assertEquals(Boolean.TRUE, value);
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }


}
