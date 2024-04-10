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

package org.finos.legend.pure.m3.tests.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestIdem extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testClass() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "Class U{b:String[1];}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testFunction() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "function go():Nil[0]{[]}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testPropertyCollectUsage() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 1; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "function go():Any[*]{ConcreteFunctionDefinition.all().name}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testFunctionWithBody() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "function a():Integer[1]{1+1}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testProfile() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "Profile p {tags:[a,b];}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testAssociation() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", """
                    Class A{}
                    Class B{}
                    Association a {a:A[1];b:B[1];}\
                    """);
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testEnumeration() throws Exception
    {
        int size = repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            runtime.createInMemorySource("sourceId.pure", "Enum e {A}");
            runtime.compile();
            runtime.delete("sourceId.pure");
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }
}
