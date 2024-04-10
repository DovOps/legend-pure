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

package org.finos.legend.pure.m3.tests.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m3.tools.SearchTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class TestSearchTools extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("SourceId.pure");
    }

    @Test
    public void testFindInAllPackages()
    {
        compileTestSource("SourceId.pure", """
                Class test::TestClass {}
                Class test::searchTools::TestClass {}
                Class test::TestClass1{}
                Class test::TestClass2 {}
                Class test::searchTools::TestClass2 {}
                Profile test::TestProfile {}
                Profile test::searchTools::TestProfile {}
                function test::testFunction1():Any[*] {true;}
                function test::searchTools::testFunction1():Any[*] {true;}
                Enum test::TestEnum {A}
                Enum test::searchTools::TestEnum {A}
                Association test::TestAssociation1 {testClass2:test::TestClass2[1];testClass1:test::TestClass1[1];}
                Association test::searchTools::TestAssociation1 {testClass2:test::searchTools::TestClass2[1];testClass:test::searchTools::TestClass[1];}\
                """);
        //find one class
        CoreInstance test_TestClass1 = runtime.getCoreInstance("test::TestClass1");
        Verify.assertSetsEqual(Sets.mutable.with(test_TestClass1), SearchTools.findInAllPackages("TestClass1", runtime.getModelRepository()).toSet());

        //find all classes
        CoreInstance test_TestClass = runtime.getCoreInstance("test::TestClass");
        CoreInstance test_searchTools_TestClass = runtime.getCoreInstance("test::searchTools::TestClass");
        CoreInstance test_letFn_TestClass = runtime.getCoreInstance("meta::pure::functions::lang::tests::letFn::TestClass");
        Verify.assertSetsEqual(Sets.mutable.with(test_TestClass, test_searchTools_TestClass, test_letFn_TestClass), SearchTools.findInAllPackages("TestClass", runtime.getModelRepository()).toSet());

        //find all functions
        CoreInstance test_testFunction1 = runtime.getCoreInstance("test::testFunction1__Any_MANY_");
        CoreInstance test_searchTools_testFunction1 = runtime.getCoreInstance("test::searchTools::testFunction1__Any_MANY_");
        Verify.assertSetsEqual(Sets.mutable.with(test_testFunction1, test_searchTools_testFunction1), SearchTools.findInAllPackages("testFunction1__Any_MANY_", runtime.getModelRepository()).toSet());

        //find class that doesn't exist
        Verify.assertEmpty(SearchTools.findInAllPackages("testClass", runtime.getModelRepository()));

        //find all Enums
        CoreInstance test_TestEnum = runtime.getCoreInstance("test::TestEnum");
        CoreInstance test_searchTools_TestEnum = runtime.getCoreInstance("test::searchTools::TestEnum");
        Verify.assertSetsEqual(Sets.mutable.with(test_TestEnum, test_searchTools_TestEnum), SearchTools.findInAllPackages("TestEnum", runtime.getModelRepository()).toSet());

        //find all associations
        CoreInstance test_TestAssociation1 = runtime.getCoreInstance("test::TestAssociation1");
        CoreInstance test_searchTools_TestAssociation1 = runtime.getCoreInstance("test::searchTools::TestAssociation1");
        Verify.assertSetsEqual(Sets.mutable.with(test_TestAssociation1, test_searchTools_TestAssociation1), SearchTools.findInAllPackages("TestAssociation1", runtime.getModelRepository()).toSet());

        //find with pattern
        CoreInstance test_TestClass2 = runtime.getCoreInstance("test::TestClass2");
        CoreInstance test_searchTools_TestClass2 = runtime.getCoreInstance("test::searchTools::TestClass2");
        CoreInstance test_Enum1 = runtime.getCoreInstance("meta::pure::functions::boolean::tests::equalitymodel::TestEnum1");
        CoreInstance test_Enum2 = runtime.getCoreInstance("meta::pure::functions::boolean::tests::equalitymodel::TestEnum2");
        System.out.println(SearchTools.findInAllPackages(Pattern.compile("Test\\S+\\d+"), runtime.getModelRepository()).collect(CoreInstance::getName));
        Verify.assertSetsEqual(Sets.mutable.with(test_Enum1, test_Enum2, test_TestClass1, test_TestClass2, test_searchTools_TestClass2, test_TestAssociation1, test_searchTools_TestAssociation1),
                SearchTools.findInAllPackages(Pattern.compile("Test\\S+\\d+"), runtime.getModelRepository()).toSet());

        //find all paths
        Verify.assertEmpty(SearchTools.findInAllPackages(Pattern.compile("\\S::\\S"), runtime.getModelRepository()));
    }

    @Test
    public void testFindInPackages()
    {
        compileTestSource("SourceId.pure", """
                Class test::TestClass {}
                Class test::searchTools::TestClass {}
                Class test::test::searchTools::TestClass{}
                Class test::TestClass1{}
                Class test::TestClass2 {}
                Class test::searchTools::TestClass2 {}
                Profile test::TestProfile {}
                Profile test::searchTools::TestProfile {}
                function test::testFunction1():Any[*] {true;}
                function test::searchTools::testFunction1():Any[*] {true;}
                Enum test::TestEnum {A}
                Enum test::searchTools::TestEnum {A}
                Association test::TestAssociation1 {testClass2:test::TestClass2[1];testClass1:test::TestClass1[1];}
                Association test::searchTools::TestAssociation1 {testClass2:test::searchTools::TestClass2[1];testClass:test::searchTools::TestClass[1];}\
                """);

        MutableList<Package> testPackages = Lists.mutable.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(runtime.getModelRepository()))
        {
            if (pkg.getName().contains("searchTools"))
            {
                testPackages.add(pkg);
            }
        }

        //find no classes
        Verify.assertEmpty(SearchTools.findInPackages("TestClass1", testPackages));

        //find all classes
        CoreInstance test_searchTools_TestClass = runtime.getCoreInstance("test::searchTools::TestClass");
        CoreInstance test_test_searchTools_TestClass = runtime.getCoreInstance("test::test::searchTools::TestClass");
        Verify.assertSetsEqual(Sets.mutable.with(test_searchTools_TestClass, test_test_searchTools_TestClass), SearchTools.findInPackages("TestClass", testPackages).toSet());

        //find one function
        CoreInstance test_searchTools_testFunction1 = runtime.getCoreInstance("test::searchTools::testFunction1__Any_MANY_");
        Verify.assertSetsEqual(Sets.mutable.with(test_searchTools_testFunction1), SearchTools.findInPackages("testFunction1__Any_MANY_", testPackages).toSet());

        //find class that doesn't exist
        Verify.assertEmpty(SearchTools.findInPackages("testClass", testPackages));

        //find one Enum
        CoreInstance test_searchTools_TestEnum = runtime.getCoreInstance("test::searchTools::TestEnum");
        Verify.assertSetsEqual(Sets.mutable.with(test_searchTools_TestEnum), SearchTools.findInPackages("TestEnum", testPackages).toSet());

        //find one association
        CoreInstance test_searchTools_TestAssociation1 = runtime.getCoreInstance("test::searchTools::TestAssociation1");
        Verify.assertSetsEqual(Sets.mutable.with(test_searchTools_TestAssociation1), SearchTools.findInPackages("TestAssociation1", testPackages).toSet());

        //find with pattern
        CoreInstance test_searchTools_TestClass2 = runtime.getCoreInstance("test::searchTools::TestClass2");
        Verify.assertSetsEqual(Sets.mutable.with(test_searchTools_TestClass2, test_searchTools_TestAssociation1), SearchTools.findInPackages(Pattern.compile("Test\\S+\\d+"), testPackages).toSet());

        //find all paths
        Verify.assertEmpty(SearchTools.findInPackages(Pattern.compile("\\S::\\S"), testPackages));
    }
}
