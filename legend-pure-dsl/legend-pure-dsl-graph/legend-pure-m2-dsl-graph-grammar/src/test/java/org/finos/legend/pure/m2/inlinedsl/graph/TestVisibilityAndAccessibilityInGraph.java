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

package org.finos.legend.pure.m2.inlinedsl.graph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestVisibilityAndAccessibilityInGraph extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getCodeStorage());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("/datamart_datamt/testFile1.pure");
        runtime.delete("/datamart_datamt/testFile2.pure");
        runtime.delete("/datamart_datamt/testFile3.pure");
        runtime.delete("/datamart_dtm/testFile3.pure");
        runtime.delete("/model/testFile1.pure");
        runtime.delete("/model/testFile3.pure");
        runtime.compile();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.<CodeRepository>with(
                GenericCodeRepository.build("datamart_dtm", "((datamarts::dtm::(domain|mapping|store))|(apps::dtm))(::.*)?", "platform", "system", "model", "model_legacy"),
                GenericCodeRepository.build("datamart_datamt", "((datamarts::datamt::(domain|mapping|store))|(apps::datamt))(::.*)?", "platform", "system", "model", "model_legacy"),
                GenericCodeRepository.build("model", "(model::(domain|mapping|store|producers|consumers|external)||(apps::model))(::.*)?", "platform", "system"),
                GenericCodeRepository.build("model_candidate", "(model_candidate::(domain|mapping|store|producers|consumers|external)||(apps::model_candidate))(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("model_legacy", "(model_legacy::(domain|mapping|store|producers|consumers|external)||(apps::model_legacy))(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("model_validation", "(model::producers)(::.*)?", "platform", "system", "model"),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform")
        ).newWithAll(CodeRepositoryProviderHelper.findCodeRepositories());
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories()));
    }

    @Test
    public void testClassReferenceVisibilityInGraph()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("datamart_dtm");

        compileTestSource(
                "/datamart_datamt/testFile1.pure",
                """
                Class datamarts::datamt::domain::A
                {
                  name : String[1];
                }
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_dtm/testFile3.pure",
                """
                function datamarts::dtm::domain::testFn1():Any[*]
                {
                  #{datamarts::datamt::domain::A{name}}#
                }
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /datamart_dtm/testFile3.pure", "/datamart_dtm/testFile3.pure", 3, 5, 3, 32, 3, 32, e);
    }

    @Test
    public void testSubTypeClassReferenceVisibilityInGraph()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("model");

        compileTestSource(
                "/model/testFile1.pure",
                """
                Class model::domain::A
                {
                  name : String[1];
                  b : model::domain::B[1];
                }
                Class model::domain::B {}\
                """
        );
        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                "Class datamarts::datamt::domain::C extends model::domain::B {}"
        );
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model/testFile3.pure",
                """
                function model::domain::testFn1():Any[*]
                {
                  #{model::domain::A{name,b->subType(@datamarts::datamt::domain::C)}}#
                }
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::C is not visible in the file /model/testFile3.pure", "/model/testFile3.pure", 3, 39, 3, 39, 3, 66, e);
        runtime.delete("/model/testFile3.pure");

        compileTestSource(
                "/datamart_datamt/testFile3.pure",
                """
                function datamarts::datamt::domain::testFn1():Any[*]
                {
                  #{model::domain::A{name,b->subType(@datamarts::datamt::domain::C)}}#
                }
                """
        );
    }

    @Test
    public void testReferenceAccessibilityInGraph()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("datamart_dtm");

        compileTestSource(
                "/datamart_datamt/testFile1.pure",
                """
                Class <<access.private>> datamarts::datamt::domain::A
                {
                  name : String[1];
                }
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_dtm/testFile3.pure",
                """
                function datamarts::dtm::domain::testFn1():Any[*]
                {
                  #{datamarts::datamt::domain::A{name}}#
                }
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not accessible in datamarts::dtm::domain", "/datamart_dtm/testFile3.pure", 3, 5, 3, 32, 3, 32, e);
        runtime.delete("/datamart_dtm/testFile3.pure");

        compileTestSource(
                "/datamart_datamt/testFile3.pure",
                """
                function datamarts::datamt::domain::testFn1():Any[*]
                {
                  #{datamarts::datamt::domain::A{name}}#
                }
                """
        );
    }
}
