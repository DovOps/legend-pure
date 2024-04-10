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

package org.finos.legend.pure.m3.tests.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.compiler.visibility.Visibility;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestVisibility extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getCodeStorage());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("testFile.pure");
        runtime.delete("testFile2.pure");
        runtime.delete("/system/testFile.pure");
        runtime.delete("/system/testFile2.pure");
        runtime.delete("/system/testFile3.pure");
        runtime.delete("/system/testFile4.pure");
        runtime.delete("/model_legacy/testFile.pure");
        runtime.delete("/model_legacy/testFile2.pure");
        runtime.delete("/model_validation/testFile2.pure");
        runtime.delete("/datamart_datamt/testFile.pure");
        runtime.delete("/datamart_datamt/testFile2.pure");
        runtime.delete("/datamart_datamt/testFile3.pure");
        runtime.delete("/datamart_dtm/testFile.pure");
        runtime.delete("/datamart_dtm/testFile2.pure");
        runtime.delete("/datamart_dtm/testFile3.pure");
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

    // No repo visibility (not visible in any repo)
    @Test
    public void testVisibilityOfNoRepoInNoRepo()
    {
        compileTestSource(
                "testFile.pure",
                "Class pkg::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("pkg::A"));
        compileTestSource(
                "testFile2.pure",
                """
                import pkg::*;
                function meta::test::func():pkg::A[1]
                {
                    ^pkg::A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("meta::test::func():A[1]"));
    }

    @Test
    public void testVisibilityOfNoRepoInSystemRepo()
    {
        assertRepoExists("system");
        compileTestSource(
                "testFile.pure",
                "Class pkg::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("pkg::A"));

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile2.pure",
                """
                import pkg::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg::A is not visible in the file /system/testFile2.pure", "/system/testFile2.pure", 2, 29, 2, 29, 2, 29, e);
    }

    @Test
    public void testVisibilityOfNoRepoInModelRepo()
    {
        assertRepoExists("system");
        assertRepoExists("model_legacy");
        compileTestSource(
                "testFile.pure",
                "Class pkg::ABC {}");
        Assertions.assertNotNull(runtime.getCoreInstance("pkg::ABC"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model_legacy/testFile2.pure",
                """
                import pkg::*;
                function model_legacy::domain::func():ABC[1]
                {
                    ^ABC();
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg::ABC is not visible in the file /model_legacy/testFile2.pure", "/model_legacy/testFile2.pure", 2, 39, 2, 39, 2, 41, e);
    }

    @Test
    public void testVisibilityOfNoRepoInDatamartRepo()
    {
        assertRepoExists("system");
        assertRepoExists("datamart_datamt");
        compileTestSource(
                "testFile.pure",
                "Class pkg::Maxwell {}");
        Assertions.assertNotNull(runtime.getCoreInstance("pkg::Maxwell"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_datamt/testFile2.pure",
                """
                import pkg::*;
                function apps::datamt::func():Maxwell[1]
                {
                    ^Maxwell();
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg::Maxwell is not visible in the file /datamart_datamt/testFile2.pure", "/datamart_datamt/testFile2.pure", 2, 31, 2, 31, 2, 37, e);
    }

    // System repo visibility (visible everywhere)

    @Test
    public void testVisibilityOfSystemRepoInNoRepo()
    {
        assertRepoExists("system");
        compileTestSource(
                "/system/testFile.pure",
                "Class meta::test::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("meta::test::A"));
        compileTestSource(
                "testFile2.pure",
                """
                import meta::test::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("meta::test::func():A[1]"));
    }

    @Test
    public void testVisibilityOfSystemRepoInSystemRepo()
    {
        assertRepoExists("system");
        compileTestSource(
                "/system/testFile.pure",
                "Class meta::test::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("meta::test::A"));
        compileTestSource(
                "/system/testFile2.pure",
                """
                import meta::test::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("meta::test::func():A[1]"));
    }

    @Test
    public void testVisibilityOfSystemRepoInModelRepo()
    {
        assertRepoExists("system");
        assertRepoExists("model_legacy");
        compileTestSource(
                "/system/testFile.pure",
                "Class meta::test::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("meta::test::A"));
        compileTestSource(
                "/model_legacy/testFile2.pure",
                """
                import meta::test::*;
                function model_legacy::domain::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("model_legacy::domain::func():A[1]"));
    }

    @Test
    public void testVisibilityOfSystemRepoInDatamartRepo()
    {
        assertRepoExists("system");
        assertRepoExists("datamart_datamt");
        compileTestSource(
                "/system/testFile.pure",
                "Class meta::test::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("meta::test::A"));
        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                """
                import meta::test::*;
                function apps::datamt::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("apps::datamt::func():A[1]"));
    }

    // Model repo visibility (visible in datamart repos)

    @Test
    public void testVisibilityOfModelRepoInNoRepo()
    {
        assertRepoExists("model_legacy");
        compileTestSource(
                "/model_legacy/testFile.pure",
                "Class model_legacy::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("model_legacy::domain::A"));
        compileTestSource(
                "testFile2.pure",
                """
                import model_legacy::domain::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("meta::test::func():A[1]"));
    }

    @Test
    public void testVisibilityOfModelRepoInSystemRepo()
    {
        assertRepoExists("model_legacy");
        assertRepoExists("system");

        compileTestSource(
                "/model_legacy/testFile.pure",
                "Class model_legacy::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("model_legacy::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile2.pure",
                """
                import model_legacy::domain::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """));
        assertPureException(PureCompilationException.class, "model_legacy::domain::A is not visible in the file /system/testFile2.pure", "/system/testFile2.pure", 2, 29, 2, 29, 2, 29, e);
    }

    @Test
    public void testVisibilityOfModelRepoInModelRepo()
    {
        assertRepoExists("model_legacy");
        assertRepoExists("model_candidate");
        Assertions.assertFalse(getRepositoryByName("model_legacy").isVisible(getRepositoryByName("model_candidate")));

        compileTestSource(
                "/model_candidate/testFile.pure",
                "Class model_candidate::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("model_candidate::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model_legacy/testFile2.pure",
                """
                import model_candidate::domain::*;
                Class model_legacy::domain::B extends A
                {
                }\
                """));
        assertPureException(PureCompilationException.class, "model_candidate::domain::A is not visible in the file /model_legacy/testFile2.pure", "/model_legacy/testFile2.pure", 2, 39, 2, 39, 2, 39, e);
    }

    @Test
    public void testVisibilityOfModelRepoInModelRepoWithExplicitVisibility()
    {
        assertRepoExists("model_legacy");
        assertRepoExists("model");
        Assertions.assertTrue(getRepositoryByName("model_legacy").isVisible(getRepositoryByName("model")));

        compileTestSource(
                "/model/testFile.pure",
                "Class model::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("model::domain::A"));
        compileTestSource(
                "/model_legacy/testFile2.pure",
                """
                import model::domain::*;
                Class model_legacy::domain::B extends A
                {
                }\
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("model_legacy::domain::B"));
    }

    @Test
    public void testVisibilityOfModelRepoInDatamartRepo()
    {
        assertRepoExists("model_legacy");
        assertRepoExists("datamart_datamt");

        compileTestSource(
                "/model_legacy/testFile.pure",
                "Class model_legacy::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("model_legacy::domain::A"));

        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                """
                import model_legacy::domain::*;
                function apps::datamt::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("apps::datamt::func():A[1]"));
    }

    // Datamart repo visibility (visible only to themselves)

    @Test
    public void testVisibilityOfDatamartRepoInNoRepo()
    {
        assertRepoExists("datamart_datamt");
        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Class datamarts::datamt::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        compileTestSource(
                "testFile2.pure",
                """
                import datamarts::datamt::domain::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("meta::test::func():A[1]"));
    }

    @Test
    public void testVisibilityOfDatamartRepoInSystemRepo()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("system");

        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Class datamarts::datamt::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile2.pure",
                """
                import datamarts::datamt::domain::*;
                function meta::test::func():A[1]
                {
                    ^A();
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /system/testFile2.pure", "/system/testFile2.pure", 2, 29, 2, 29, 2, 29, e);
    }

    @Test
    public void testVisibilityOfDatamartRepoInModelRepo()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("model_legacy");

        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Class datamarts::datamt::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model_legacy/testFile2.pure",
                """
                import datamarts::datamt::domain::*;
                function model_legacy::domain::test::func():A[1]
                {
                    ^A();
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /model_legacy/testFile2.pure", "/model_legacy/testFile2.pure", 2, 45, 2, 45, 2, 45, e);
    }

    @Test
    public void testVisibilityOfDatamartRepoInDatamartRepo()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("datamart_dtm");

        compileTestSource(
                "/datamart_datamt/testFile.pure",
                """
                Class datamarts::datamt::domain::A {}
                Profile datamarts::datamt::domain::TestProfile
                {
                  stereotypes: [st1];
                }\
                """);
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_dtm/testFile2.pure",
                """
                import datamarts::datamt::domain::*;
                Class datamarts::dtm::domain::B extends A
                {
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::A is not visible in the file /datamart_dtm/testFile2.pure", "/datamart_dtm/testFile2.pure", 2, 41, 2, 41, 2, 41, e1);

        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/datamart_dtm/testFile3.pure",
                """
                import datamarts::datamt::domain::*;
                Class datamarts::dtm::domain::C
                {
                  <<TestProfile.st1>> prop1 : String[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestProfile is not visible in the file /datamart_dtm/testFile3.pure", "/datamart_dtm/testFile3.pure", 4, 23, 4, 23, 4, 40, e2);
    }

    // Direct package references

    @Test
    public void testDirectPackageReference()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("system");

        compileTestSource(
                "/datamart_datamt/testFile.pure",
                "Class datamarts::datamt::domain::A {}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::A"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/system/testFile.pure",
                """
                function meta::pure::testFn():Package[1]
                {
                  datamarts::datamt
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt is not visible in the file /system/testFile.pure", "/system/testFile.pure", 3, 14, 3, 14, 3, 19, e);
    }

    // Associations

    @Test
    public void testAssociations()
    {
        assertRepoExists("datamart_datamt");
        assertRepoExists("system");

        compileTestSource(
                "/system/testFile.pure",
                "Class meta::pure::TestClass1 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("meta::pure::TestClass1"));
        compileTestSource(
                "/datamart_datamt/testFile2.pure",
                "Class datamarts::datamt::domain::TestClass2 {}\nClass datamarts::datamt::domain::TestClass3 {}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::TestClass2"));

        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                    "/system/testFile3.pure",
                    """
                    Association meta::system::TestAssoc1
                    {
                      toTestClass1:datamarts::datamt::domain::TestClass2[0..1];
                      toTestClass2:datamarts::datamt::domain::TestClass3[0..1];
                    }\
                    """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::TestClass2 is not visible in the file /system/testFile3.pure", "/system/testFile3.pure", 3, 43, 3, 43, 3, 52, e1);

        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                    "/system/testFile4.pure",
                    """
                    Association meta::system::TestAssoc1
                    {
                      toTestClass1:meta::pure::TestClass1[0..1];
                      toTestClass2:datamarts::datamt::domain::TestClass2[0..1];
                    }\
                    """));
        assertPureException(PureCompilationException.class, "Associations are not permitted between classes in different repositories, datamarts::datamt::domain::TestClass2 is in the \"datamart_datamt\" repository and meta::pure::TestClass1 is in the \"system\" repository. This can be solved by first creating a subclass located in the same repository and creating an Association to the subclass.", "/system/testFile4.pure", 4, 3, 4, 3, 4, 59, e2);

        compileTestSource(
                "/datamart_datamt/testFile3.pure",
                """
                Association datamarts::datamt::domain::TestAssoc2
                {
                  toTestClass2:datamarts::datamt::domain::TestClass2[0..1];
                  toTestClass3:datamarts::datamt::domain::TestClass3[0..1];
                }\
                """);
        CoreInstance testAssoc2 = runtime.getCoreInstance("datamarts::datamt::domain::TestAssoc2");
        Assertions.assertNotNull(testAssoc2);

        ListIterable<? extends CoreInstance> testAssoc2Props = Instance.getValueForMetaPropertyToManyResolved(testAssoc2, M3Properties.properties, processorSupport);
        Assertions.assertEquals(2, testAssoc2Props.size());
        CoreInstance toTestClass2 = testAssoc2.getValueInValueForMetaPropertyToMany(M3Properties.properties, "toTestClass2");
        Assertions.assertNotNull(toTestClass2);
        CoreInstance toTestClass3 = testAssoc2.getValueInValueForMetaPropertyToMany(M3Properties.properties, "toTestClass3");
        Assertions.assertNotNull(toTestClass3);

        CoreInstance testClass2 = runtime.getCoreInstance("datamarts::datamt::domain::TestClass2");
        ListIterable<? extends CoreInstance> testClass1PropsFromAssocs = Instance.getValueForMetaPropertyToManyResolved(testClass2, M3Properties.propertiesFromAssociations, processorSupport);
        Assertions.assertEquals(1, testClass1PropsFromAssocs.size());
        Assertions.assertSame(toTestClass3, testClass1PropsFromAssocs.getFirst());

        CoreInstance testClass3 = runtime.getCoreInstance("datamarts::datamt::domain::TestClass3");
        ListIterable<? extends CoreInstance> testClass2PropsFromAssocs = Instance.getValueForMetaPropertyToManyResolved(testClass3, M3Properties.propertiesFromAssociations, processorSupport);
        Assertions.assertEquals(1, testClass2PropsFromAssocs.size());
        Assertions.assertSame(toTestClass2, testClass2PropsFromAssocs.getFirst());
    }

    @Test
    public void testVisibilityOfModelInModelValidationRepo()
    {

        assertRepoExists("model");
        assertRepoExists("model_validation");
        Assertions.assertTrue(getRepositoryByName("model_validation").isVisible(getRepositoryByName("model")));
        Assertions.assertTrue(getRepositoryByName("model_validation").isVisible(getRepositoryByName("system")));
        Assertions.assertFalse(getRepositoryByName("model_validation").isVisible(getRepositoryByName("sec_div")));
        Assertions.assertFalse(getRepositoryByName("model_validation").isVisible(getRepositoryByName("contracts")));
        compileTestSource(
                "/model/testFile1.pure",
                "Class model::producers::bu::A { name:String[1];}");
        Assertions.assertNotNull(runtime.getCoreInstance("model::producers::bu::A"));
        compileTestSource(
                "/model_validation/testFile2.pure",
                """
                import model::producers::bu::*;
                function model::producers::bu::validationFunc():A[1]
                {
                    ^A(name='a');
                }\
                """);
        Assertions.assertNotNull(runtime.getFunction("model::producers::bu::validationFunc():A[1]"));
    }

    @Test
    public void testDatamartsVisibilityOfModelInModelValidationRepo()
    {
        assertRepoExists("model_validation");
        assertRepoExists("datamart_datamt");
        compileTestSource(
                "/datamart_datamt/testFile1.pure",
                "Class datamarts::datamt::domain::Test { name:String[1];}");
        Assertions.assertNotNull(runtime.getCoreInstance("datamarts::datamt::domain::Test"));
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/model_validation/testFile2.pure",
                """
                import datamarts::datamt::domain::*;
                function model::producers::bu::validationFunc():Test[1]
                {
                    ^Test(name='lala');
                }\
                """));
        assertPureException(PureCompilationException.class, "datamarts::datamt::domain::Test is not visible in the file /model_validation/testFile2.pure", "/model_validation/testFile2.pure", 2, 49, 2, 49, 2, 52, e);
        Assertions.assertNull(runtime.getFunction("model::producers::bu::validationFunc():Test[1]"));
    }

    @Test
    public void testIsVisibleInPackage()
    {
        compileTestSource("testFile.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function <<access.public>> pkg1::publicFunc1(s:String[1]):String[1]
                {
                    pkg1::privateFunc($s, ' from public')
                }
                
                function pkg1::sub::publicFunc2(s:String[1]):String[1]
                {
                    pkg1::protectedFunc($s, ' from public')
                }
                
                function pkg2::publicFunc3(s:String[1]):String[1]
                {
                    $s
                }
                function <<access.externalizable>> pkg1::extFunc1(s:String[1]):String[1]
                {
                    pkg1::privateFunc($s, ' from public')
                }
                
                function <<access.externalizable>> pkg1::sub::extFunc2(s:String[1]):String[1]
                {
                    pkg1::protectedFunc($s, ' from public')
                }
                
                function <<access.externalizable>> pkg2::extFunc3(s:String[1]):String[1]
                {
                    $s
                }\
                """);

        PackageableFunction<?> privateFunc = (PackageableFunction<?>) runtime.getFunction("pkg1::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);
        Assertions.assertSame(AccessLevel.PRIVATE, AccessLevel.getAccessLevel(privateFunc, context, processorSupport));

        PackageableFunction<?> protectedFunc = (PackageableFunction<?>) runtime.getFunction("pkg1::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);
        Assertions.assertSame(AccessLevel.PROTECTED, AccessLevel.getAccessLevel(protectedFunc, context, processorSupport));

        PackageableFunction<?> publicFunc1 = (PackageableFunction<?>) runtime.getFunction("pkg1::publicFunc1(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc1);
        Assertions.assertSame(AccessLevel.PUBLIC, AccessLevel.getAccessLevel(publicFunc1, context, processorSupport));

        PackageableFunction<?> publicFunc2 = (PackageableFunction<?>) runtime.getFunction("pkg1::sub::publicFunc2(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc2);
        Assertions.assertSame(AccessLevel.PUBLIC, AccessLevel.getAccessLevel(publicFunc2, context, processorSupport));

        PackageableFunction<?> publicFunc3 = (PackageableFunction<?>) runtime.getFunction("pkg2::publicFunc3(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc3);
        Assertions.assertSame(AccessLevel.PUBLIC, AccessLevel.getAccessLevel(publicFunc3, context, processorSupport));

        PackageableFunction<?> extFunc1 = (PackageableFunction<?>) runtime.getFunction("pkg1::extFunc1(String[1]):String[1]");
        Assertions.assertNotNull(extFunc1);
        Assertions.assertSame(AccessLevel.EXTERNALIZABLE, AccessLevel.getAccessLevel(extFunc1, context, processorSupport));

        PackageableFunction<?> extFunc2 = (PackageableFunction<?>) runtime.getFunction("pkg1::sub::extFunc2(String[1]):String[1]");
        Assertions.assertNotNull(extFunc2);
        Assertions.assertSame(AccessLevel.EXTERNALIZABLE, AccessLevel.getAccessLevel(extFunc2, context, processorSupport));

        PackageableFunction<?> extFunc3 = (PackageableFunction<?>) runtime.getFunction("pkg2::extFunc3(String[1]):String[1]");
        Assertions.assertNotNull(extFunc3);
        Assertions.assertSame(AccessLevel.EXTERNALIZABLE, AccessLevel.getAccessLevel(extFunc3, context, processorSupport));

        CoreInstance pkg1 = runtime.getCoreInstance("pkg1");
        Assertions.assertNotNull(pkg1);

        CoreInstance pkg1Sub = runtime.getCoreInstance("pkg1::sub");
        Assertions.assertNotNull(pkg1Sub);

        CoreInstance pkg2 = runtime.getCoreInstance("pkg2");
        Assertions.assertNotNull(pkg2);

        Assertions.assertTrue(Visibility.isVisibleInPackage(privateFunc, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(protectedFunc, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc1, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc2, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc3, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc1, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc2, pkg1, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc3, pkg1, context, processorSupport));

        Assertions.assertFalse(Visibility.isVisibleInPackage(privateFunc, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(protectedFunc, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc1, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc2, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc3, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc1, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc2, pkg1Sub, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc3, pkg1Sub, context, processorSupport));

        Assertions.assertFalse(Visibility.isVisibleInPackage(privateFunc, pkg2, context, processorSupport));
        Assertions.assertFalse(Visibility.isVisibleInPackage(protectedFunc, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc1, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc2, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(publicFunc3, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc1, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc2, pkg2, context, processorSupport));
        Assertions.assertTrue(Visibility.isVisibleInPackage(extFunc3, pkg2, context, processorSupport));
    }
}
