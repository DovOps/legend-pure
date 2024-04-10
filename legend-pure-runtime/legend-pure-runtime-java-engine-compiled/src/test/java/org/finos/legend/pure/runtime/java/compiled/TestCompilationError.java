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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tools.ThrowableTools;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class TestCompilationError extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Disabled
    @Test
    public void testCompileError()
    {
        try
        {
            compileTestSource("testSource.pure",
                    """
                    Class MyClass<T|m>
                    {
                        values:T[m];
                    }
                    
                    function test():Integer[1]
                    {
                        myClass(1).values;
                    }
                    
                    function myClass<T|m>(values:T[m]):MyClass<T|m>[1]
                    {
                       ^MyClass<T|m>(values=$values);
                    }\
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            Throwable root = ThrowableTools.findRootThrowable(e);
            Assertions.assertTrue(root instanceof PureJavaCompileException);
            Assertions.assertTrue(e.getMessage().startsWith("org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException: 1 error compiling /" + JavaPackageAndImportBuilder.rootPackage().replace(".", "/") + "/testSource.java"));
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
