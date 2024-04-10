// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDatabaseInclude extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(GenericCodeRepository.build("test", "((test)|(meta))(::.*)?", "platform"));
        return repositories;
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("/test/testDB.pure");
        runtime.delete("/test/testDB1.pure");
        runtime.delete("/test/testDB2.pure");
        runtime.compile();
    }

    @Test
    public void testDoubleInclude()
    {
        compileTestSource("/test/testDB1.pure",
                """
                ###Relational
                Database test::TestDB1 ()\
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB2.pure",
                """
                ###Relational
                Database test::TestDB2
                (
                    include test::TestDB1
                    include test::TestDB1
                )
                """));
        assertPureException(PureCompilationException.class, "test::TestDB1 is included multiple times in test::TestDB2", "/test/testDB2.pure", 2, 1, 2, 16, 6, 1, e);
    }

    @Test
    public void testSelfInclude()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB.pure",
                    """
                    ###Relational
                    Database test::TestDB
                    (
                        include test::TestDB
                    )
                    """));
        assertPureException(PureCompilationException.class, "Circular include in test::TestDB: test::TestDB -> test::TestDB", "/test/testDB.pure", 2, 1, 2, 16, 5, 1, e);
    }

    @Test
    public void testIncludeLoop()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testDB.pure",
                    """
                    ###Relational
                    Database test::TestDB1
                    (
                        include test::TestDB2
                    )
                    
                    ###Relational
                    Database test::TestDB2
                    (
                        include test::TestDB3
                    )
                    
                    ###Relational
                    Database test::TestDB3
                    (
                        include test::TestDB1
                    )
                    """));
        switch (e.getInfo())
        {
            case "Circular include in test::TestDB1: test::TestDB1 -> test::TestDB2 -> test::TestDB3 -> test::TestDB1":
            {
                assertSourceInformation("/test/testDB.pure", 2, 1, 2, 16, 5, 1, e.getSourceInformation());
                break;
            }
            case "Circular include in test::TestDB2: test::TestDB2 -> test::TestDB3 -> test::TestDB1 -> test::TestDB2":
            {
                assertSourceInformation("/test/testDB.pure", 8, 1, 8, 16, 11, 1, e.getSourceInformation());
                break;
            }
            case "Circular include in test::TestDB3: test::TestDB3 -> test::TestDB1 -> test::TestDB2 -> test::TestDB3":
            {
                assertSourceInformation("/test/testDB.pure", 14, 1, 14, 16, 17, 1, e.getSourceInformation());
                break;
            }
            default:
            {
                Assertions.assertEquals("Circular include in test::TestDB1: test::TestDB1 -> test::TestDB2 -> test::TestDB3 -> test::TestDB1", e.getInfo());
            }
        }
    }
}
