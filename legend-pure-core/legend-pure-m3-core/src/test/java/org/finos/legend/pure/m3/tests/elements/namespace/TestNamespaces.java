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

package org.finos.legend.pure.m3.tests.elements.namespace;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestNamespaces extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testClassNameConflict()
    {
        compileTestSource("class1.pure",
                "Class test::MyClass {}");
        CoreInstance myClass = runtime.getCoreInstance("test::MyClass");
        Assertions.assertNotNull(myClass);
        Assertions.assertTrue(Instance.instanceOf(myClass, M3Paths.Class, processorSupport));

        try
        {
            compileTestSource("class2.pure",
                    "Class test::MyClass {}");
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyClass' already exists in the package 'test'", "class2.pure", 1, 13, 1, 13, 1, 19, e);
        }
    }

    @Test
    public void testEnumerationNameConflict()
    {
        compileTestSource("enum1.pure",
                "Enum test::MyEnum {VALUE}");
        CoreInstance myEnum = runtime.getCoreInstance("test::MyEnum");
        Assertions.assertNotNull(myEnum);
        Assertions.assertTrue(Instance.instanceOf(myEnum, M3Paths.Enumeration, processorSupport));

        try
        {
            compileTestSource("enum2.pure",
                    "Enum test::MyEnum {VALUE}");
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyEnum' already exists in the package 'test'", "enum2.pure", 1, 12, 1, 12, 1, 17, e);
        }
    }

    @Test
    public void testAssociationNameConflict()
    {
        compileTestSource("assoc1.pure",
                """
                Class test::TestClass {}
                Association test::MyAssociation\
                {
                  prop1 : test::TestClass[*];
                  prop2 : test::TestClass[*];
                }\
                """);
        CoreInstance myAssoc = runtime.getCoreInstance("test::MyAssociation");
        Assertions.assertNotNull(myAssoc);
        Assertions.assertTrue(Instance.instanceOf(myAssoc, M3Paths.Association, processorSupport));

        try
        {
            compileTestSource("assoc2.pure",
                    """
                    Association test::MyAssociation\
                    {
                      prop1 : test::TestClass[*];
                      prop2 : test::TestClass[*];
                    }\
                    """);
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyAssociation' already exists in the package 'test'", "assoc2.pure", 1, 19, 1, 19, 1, 31, e);
        }
    }
}
