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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestNewClass extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("StandardCall.pure");
        runtime.compile();
    }

    @Test
    public void standardCall()
    {
        String source = """
                function go():Any[*]
                {
                    let newClass = 'meta::pure::functions::meta::newClass'->newClass();
                    assertEquals('newClass', $newClass.name);
                    assertEquals('meta', $newClass.package.name);
                    assertEquals('meta::pure::functions::meta::newClass', $newClass->elementToPath());
                }
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmptyPackage()
    {
        String source = """
                function go():Any[*]
                {
                    let newClass = 'MyNewClass'->newClass();
                    assertEquals('MyNewClass', $newClass.name);
                    assertEquals('MyNewClass', $newClass->elementToPath());
                }
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmpty()
    {
        String source = """
                function go():Any[*]
                {
                    let newClass = ''->newClass();
                    assertEquals('', $newClass.name);
                    assertEquals('', $newClass->elementToPath());
                }
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void newPackage()
    {
        String source = """
                function go():Any[*]
                {
                    let newClass = 'foo::bar::newClass'->newClass();
                    assertEquals('foo::bar::newClass', $newClass->elementToPath());
                }
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
