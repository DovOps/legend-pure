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

package org.finos.legend.pure.m3.tests.lineinfo;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

public class TestNavigateFromCoordinates extends AbstractPureTestWithCoreCompiledPlatform
{
    // Not working yet
    //      properties used in a new
    //      profile in a stereotype (routes to the value)
    //      profile in a tagged value (routes to the tag)
    //      value of an enum (routes to extractEnumValue)

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
    public void testNavigation1() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", new Scanner(TestNavigateFromCoordinates.class.getResourceAsStream("/org/finos/legend/pure/m3/tests/lineinfo/file1.pure")).useDelimiter("\\Z").next());
        runtime.compile();
        Assertions.assertEquals(this.fromPackage("A"), this.get(28, 6));
        Assertions.assertEquals(this.fromPackage("B"), this.get(30, 16));
        Assertions.assertEquals(this.fromPackage("String"), this.get(17, 17));
        Assertions.assertEquals(this.fromPackage("meta::pure::metamodel::type::Any"), this.get(18, 20));
        Assertions.assertEquals(this.fromPackage("String"), this.get(35, 24));
        Assertions.assertEquals(this.fromPackage("Integer"), this.get(35, 35));
        Assertions.assertEquals(this.fromPackage("testFunc_String_1__Integer_1_"), this.get(32, 10));
    }

    @Test
    public void testNavigation2() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure", new Scanner(TestNavigateFromCoordinates.class.getResourceAsStream("/org/finos/legend/pure/m3/tests/lineinfo/file2.pure")).useDelimiter("\\Z").next());
        runtime.compile();
        Assertions.assertEquals("deprecated", this.get(17, 10).getName());
        Assertions.assertEquals("deprecated", this.get(17, 20).getName());
        Assertions.assertEquals("doc", this.get(17, 28).getName());
        Assertions.assertEquals("doc", this.get(17, 32).getName());
        Assertions.assertEquals(this.fromPackage("myEnum"), this.get(29, 13));
    }

    private CoreInstance fromPackage(String element)
    {
        return processorSupport.package_getByUserPath(element);
    }

    private CoreInstance get(int x, int y)
    {
        return runtime.getSourceById("sourceId.pure").navigate(x, y, runtime.getProcessorSupport());
    }

}
