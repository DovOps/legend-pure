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
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class TestGeneralization extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform"),
                GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("fromString.pure");
        runtime.delete("/test/testModel.pure");
        runtime.compile();
    }

    @Test
    public void testClassGeneralizationToEnum()
    {
        compileTestSource("fromString.pure", "Enum test::TestEnum {A, B, C}");
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("testSource.pure", "Class test::TestClass extends test::TestEnum {}"));
        assertPureException(PureCompilationException.class, "Invalid generalization: test::TestClass cannot extend test::TestEnum as it is not a Class", "testSource.pure", 1, 1, 1, 13, 1, 47, e);
    }

    @Test
    public void testEnumGeneralizationToEnum()
    {
        compileTestSource("fromString.pure", "Enum test::TestEnum {A, B, C}");
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource("testSource.pure", "Enum test::TestEnum2 extends test::TestEnum {}"));
        assertPureException(PureParserException.class, "expected: '{' found: 'extends'", "testSource.pure", 1, 22, 1, 22, 1, 29, e);
    }

    @Test
    public void testClassGeneralizationToPrimitiveType()
    {
        for (String typeName : PrimitiveUtilities.getPrimitiveTypeNames())
        {
            String sourceFile = "test%s.pure".formatted(typeName);
            try
            {
                PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(sourceFile, "Class test::TestClass extends %s {}".formatted(typeName)));
                String expectedMessage = "Invalid generalization: test::TestClass cannot extend %s as it is not a Class".formatted(typeName);
                assertPureException(PureCompilationException.class, expectedMessage, sourceFile, 1, 1, 1, 13, 1, 33 + typeName.length(), e);
            }
            finally
            {
                runtime.delete(sourceFile);
                runtime.compile();
            }
        }
    }

    @Test
    public void testDiamondWithGenericIssue()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class A<T>{prop:T[1];}
                Class B extends A<String>{}
                Class C extends A<Integer>{}
                Class D extends B,C{}
                function simpleTest():D[1]
                {
                   ^D(prop=333);
                }
                """));
        assertPureException(PureCompilationException.class, Pattern.compile("^Diamond inheritance error! (('Integer' is not compatible with 'String')|('String' is not compatible with 'Integer')) going from 'D' to 'A<T>'$"), 7, 4, e);
    }

    @Test
    public void testGeneralizationWithSelfReferenceInGenerics()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testModel.pure",
                """
                import test::*;
                Class test::A<T> {}
                Class test::B extends A<B> {}
                """));
        assertPureException(PureCompilationException.class, "Class B extends A<B> which contains a reference to B itself", "/test/testModel.pure", 3, 13, e);
    }

    @Test
    public void testGeneralizationWithNestedSelfReferenceInGenerics()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testModel.pure",
                """
                import test::*;
                Class test::A<T> {}
                Class test::B<U> {}
                Class test::C extends A<B<C>> {}
                """));
        assertPureException(PureCompilationException.class, "Class C extends A<B<C>> which contains a reference to C itself", "/test/testModel.pure", 4, 13, e);
    }

    @Test
    public void testGeneralizationWithSubtypeReferenceInGenerics()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testModel.pure",
                """
                import test::*;
                Class test::A<T> {}
                Class test::B extends A<C> {}
                Class test::C extends B {}
                """));
        assertPureException(PureCompilationException.class, "Class B extends A<C> which contains a reference to C which is a subtype of B", "/test/testModel.pure", 3, 13, e);
    }

    @Test
    public void testGeneralizationWithNestedSubtypeReferenceInGenerics()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testModel.pure",
                """
                import test::*;
                Class test::A<T> {}
                Class test::B<U> {}
                Class test::C extends A<B<D>> {}
                Class test::D extends C {}
                """));
        assertPureException(PureCompilationException.class, "Class C extends A<B<D>> which contains a reference to D which is a subtype of C", "/test/testModel.pure", 4, 13, e);
    }
}
