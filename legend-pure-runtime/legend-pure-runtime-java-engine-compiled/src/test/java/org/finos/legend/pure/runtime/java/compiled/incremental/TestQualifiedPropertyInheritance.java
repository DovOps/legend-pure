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

package org.finos.legend.pure.runtime.java.compiled.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestQualifiedPropertyInheritance extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("file1.pure");
        runtime.delete("file2.pure");
        runtime.delete("file3.pure");
    }

    @Test
    public void testQualifiedPropertyInheritance_OneSource()
    {
        String sourceId = "file1.pure";
        String sourceCode = """
                Class test::A
                {
                  value : Float[1];
                }
                
                Class test::B
                {
                  prop1 : test::A[*];
                  prop2()
                  {
                    $this.prop1->map(x | $x.value)
                  }:Float[*];
                }
                Class test::C extends test::B
                {
                }\
                """;
        new RuntimeTestScriptBuilder()
                .createInMemorySource(sourceId, sourceCode)
                .compile()
                .run(this.runtime, this.functionExecution);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(), new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.immutable.with(sourceId))
                        .compile()
                        .createInMemorySource(sourceId, sourceCode)
                        .compile(),
                this.runtime, this.functionExecution, Lists.immutable.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testQualifiedPropertyInheritance_TwoSources()
    {
        String sourceId1 = "file1.pure";
        String sourceCode1 = """
                Class test::A
                {
                  value : Float[1];
                }
                
                Class test::B
                {
                  prop1 : test::A[*];
                  prop2()
                  {
                    $this.prop1->map(x | $x.value)
                  }:Float[*];
                }\
                """;
        String sourceId2 = "file2.pure";
        String sourceCode2 = """
                Class test::C extends test::B
                {
                }\
                """;
        new RuntimeTestScriptBuilder()
                .createInMemorySource(sourceId1, sourceCode1)
                .createInMemorySource(sourceId2, sourceCode2)
                .compile()
                .run(this.runtime, this.functionExecution);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(), new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.immutable.with(sourceId2))
                        .compile()
                        .createInMemorySource(sourceId2, sourceCode2)
                        .compile(),
                this.runtime, this.functionExecution, Lists.immutable.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testQualifiedPropertyInheritance_TwoSourcesAutomapLambdaInQualifier()
    {
        String sourceId1 = "file1.pure";
        String sourceCode1 = """
                Class test::A
                {
                  value : Float[1];
                }
                Class test::C
                {
                  prop3 : test::A[*];
                  prop4()
                  {
                    $this.prop3.value
                  }:Float[*];
                }
                
                Class test::B
                {
                  prop1 : test::A[*];
                  prop2()
                  {
                    $this.prop1.value
                  }:Float[*];
                }\
                """;
        String sourceId2 = "file2.pure";
        String sourceCode2 = """
                Class test::D extends test::B
                {
                }\
                """;
        new RuntimeTestScriptBuilder()
                .createInMemorySource(sourceId1, sourceCode1)
                .createInMemorySource(sourceId2, sourceCode2)
                .compile()
                .run(this.runtime, this.functionExecution);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(), new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.immutable.with(sourceId2))
                        .compile()
                        .createInMemorySource(sourceId2, sourceCode2)
                        .compile(),
                this.runtime, this.functionExecution, Lists.immutable.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testQualifiedPropertyInheritance_ThreeSources()
    {
        String sourceId1 = "file1.pure";
        String sourceCode1 = """
                Class test::A
                {
                  value1 : Float[1];
                  value2 : Integer[1];
                }
                
                Class test::B
                {
                  prop1 : test::A[*];
                  prop2()
                  {
                    $this.prop1->map(x | $x.value1)
                  }:Float[*];
                }
                
                Class test::C
                {
                  prop1 : test::A[*];
                  prop2()
                  {
                    $this.prop1->map(y | $y.value2)
                  }:Integer[*];
                }\
                """;
        String sourceId2 = "file2.pure";
        String sourceCode2 = """
                Class test::D extends test::B
                {
                }\
                """;
        String sourceId3 = "file3.pure";
        String sourceCode3 = """
                Class test::E extends test::C
                {
                }\
                """;
        new RuntimeTestScriptBuilder()
                .createInMemorySource(sourceId1, sourceCode1)
                .createInMemorySource(sourceId2, sourceCode2)
                .createInMemorySource(sourceId3, sourceCode3)
                .compile()
                .run(this.runtime, this.functionExecution);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder(), new RuntimeTestScriptBuilder()
                        .deleteSources(Lists.immutable.with(sourceId2, sourceId3))
                        .compile()
                        .createInMemorySource(sourceId2, sourceCode2)
                        .createInMemorySource(sourceId3, sourceCode3)
                        .compile(),
                this.runtime, this.functionExecution, Lists.immutable.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    public static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
