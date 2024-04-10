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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class TestVisibility extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getCodeStorage());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("/datamart_datamt/testFile.pure");
        runtime.delete("/datamart_datamt/testFile2.pure");
        runtime.delete("/system/testFile.pure");
        runtime.delete("/system/testFile1.pure");
        runtime.delete("/system/testFile2.pure");
        runtime.compile();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        RichIterable<? extends CodeRepository> repositories = AbstractPureTestWithCoreCompiled.getCodeRepositories();
        CodeRepository system = new GenericCodeRepository("system", null, "platform", "platform_dsl_mapping");
        CodeRepository model = new GenericCodeRepository("model", null, "platform", "system");
        CodeRepository other = new GenericCodeRepository("datamart_datamt", null, "platform", "platform_dsl_mapping", "system", "model");
        MutableList<CodeRepository> r = Lists.mutable.withAll(repositories);
        r.add(system);
        r.add(model);
        r.add(other);
        return r;
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testClassMapping()
    {
        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                "Class datamarts::datamt::domain::TestClass2 {}\n");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestClass2"));

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile.pure",
                """
                function meta::pure::a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping system::myMap(
                   datamarts::datamt::domain::TestClass2[ppp]: Operation
                           {
                               meta::pure::a__SetImplementation_MANY_()
                           }
                )
                """));

        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestClass2 is not visible in the file /system/testFile.pure", "/system/testFile.pure", 4, 31, e);
    }

    @Test
    public void testEnumMapping()
    {
        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Enum datamarts::datamt::domain::TestEnum1{ VAL }\n");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestEnum1"));

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile2.pure",
                """
                ###Mapping
                Mapping meta::pure::TestMapping1
                (
                
                    datamarts::datamt::domain::TestEnum1: EnumerationMapping Foo
                    {
                        VAL:  'a'
                    }
                )
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestEnum1 is not visible in the file /system/testFile2.pure", "/system/testFile2.pure", 5, 32, e);
    }

    @Test
    public void testMappingIncludes()
    {
        compileTestSource(
                "/datamart_datamt/testFile.pure",
                """
                Class datamarts::datamt::domain::TestClass2 {}
                function datamarts::datamt::mapping::a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping datamarts::datamt::mapping::myMap1(
                   datamarts::datamt::domain::TestClass2[ppp]: Operation
                           {
                               datamarts::datamt::mapping::a__SetImplementation_MANY_()
                           }
                )
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestClass2"));

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile1.pure",
                """
                ###Mapping
                Mapping system::myMap(
                  include datamarts::datamt::mapping::myMap1
                )
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::mapping::myMap1 is not visible in the file /system/testFile1.pure", "/system/testFile1.pure", 2, 17, e);
    }
}
