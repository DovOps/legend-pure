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

package org.finos.legend.pure.m3.tests.incremental._package;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimePackage extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        setUp();
    }

    @Test
    public void testPackageRemovedWhenEmpty()
    {
        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        int size = repository.serialize().length;

        compileTestSource("source.pure", "Class test_package1::test_package2::TestClass {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));

        runtime.delete("source.pure");
        runtime.compile();
        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assertions.assertEquals(size, repository.serialize().length);
    }

    @Test
    public void testPackageNotRemovedWhenNotEmpty()
    {
        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass1 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        compileTestSource("source2.pure", "Class test_package1::test_package2::TestClass2 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        runtime.delete("source1.pure");
        runtime.compile();
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass2"));
    }

    @Test
    public void testMixedPackageRemovedAndNot()
    {
        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass1 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        compileTestSource("source2.pure", "Class test_package1::test_package3::TestClass2 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        runtime.delete("source1.pure");
        runtime.compile();
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3::TestClass2"));
    }

    @Test
    public void testPackageWithReferenceRemoved()
    {
        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass {}");
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        compileTestSource("source2.pure",
                """
                function test_package1::test_package3::testFn():Package[1]
                {
                   test_package1::test_package2
                }\
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        runtime.delete("source1.pure");
        try
        {
            runtime.compile();
            Assertions.fail("Expected compile exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "test_package1::test_package2 has not been defined!", "source2.pure", 3, 19, e);
        }
    }

    @Test
    public void testPackageDeleteWithPropertyWithSameNameAsPackage()
    {
        String sourceId = "source.pure";
        String sourceCode = """
                Class test_package1::test_name::TestClass1
                {
                   test_name : String[1];
                }\
                """;

        Assertions.assertNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_name"));
        Assertions.assertNull(runtime.getCoreInstance("test_package1::test_name::TestClass1"));
        int beforeSize = repository.serialize().length;

        compileTestSource(sourceId, sourceCode);
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_name"));
        Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_name::TestClass1"));
        int afterSize = repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete(sourceId);
            runtime.compile();
            Assertions.assertNull(runtime.getCoreInstance("test_package1"));
            Assertions.assertNull(runtime.getCoreInstance("test_package1::test_name"));
            Assertions.assertNull(runtime.getCoreInstance("test_package1::test_name::TestClass1"));
            Assertions.assertEquals(beforeSize, repository.serialize().length, "Failed on iteration #" + i);

            compileTestSource(sourceId, sourceCode);
            Assertions.assertNotNull(runtime.getCoreInstance("test_package1"));
            Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_name"));
            Assertions.assertNotNull(runtime.getCoreInstance("test_package1::test_name::TestClass1"));
            Assertions.assertEquals(afterSize, repository.serialize().length, "Failed on iteration #" + i);
        }
    }
}
