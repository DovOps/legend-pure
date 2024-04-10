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

package org.finos.legend.pure.m3.tests.elements._enum;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestEnumeration extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("enumDefinition.pure");
        runtime.delete("enumReference.pure");
        runtime.delete("/test/model.pure");
        runtime.delete("/test/test.pure");
        runtime.compile();
    }

    @Test
    public void testEnumeration()
    {
        compileTestSource("fromString.pure", """
                Enum BooleanEnum
                {
                   TRUE, FALSE
                }
                function testAssert():Boolean[1]
                {
                    assertEquals('TRUE', BooleanEnum.TRUE->id());
                    assertEquals('FALSE', BooleanEnum.FALSE->id());
                }
                """);
        execute("testAssert():Boolean[1]");
    }

    @Test
    public void testEnumerationAsFuncParam()
    {
        compileTestSource("fromString.pure", """
                Enum BooleanEnum
                {
                   TRUE, FALSE
                }
                function testCall():Boolean[1]
                {
                    other(BooleanEnum.TRUE);
                }
                function other(b:BooleanEnum[1]):Boolean[1]
                {
                   assertEquals('TRUE', $b->id());
                }
                """);
        execute("testCall():Boolean[1]");
    }

    @Test
    public void testEnumerationVariable()
    {
        compileTestSource("fromString.pure", """
                Enum BooleanEnum
                {
                   TRUE, FALSE
                }
                function test():Boolean[1]
                {
                    let a = BooleanEnum.TRUE;
                    assertEquals('BooleanEnum.TRUE', $a->genericType().rawType->at(0)->id()+'.'+$a->id());
                }\
                """);
        execute("test():Boolean[1]");
    }

    @Test
    public void testEnumerationUsedAsAPropertyType()
    {
        compileTestSource("fromString.pure", """
                Enum BooleanEnum
                {
                   TRUE, FALSE
                }
                
                Class MyClass
                {
                   prop : BooleanEnum[1];
                   prop2 : BooleanEnum[*];
                }
                
                function test():Boolean[1]
                {\
                    let test = ^MyClass test(prop = BooleanEnum.TRUE, prop2 = [BooleanEnum.FALSE, BooleanEnum.TRUE]);
                    assertEquals('TRUE', $test.prop->id());
                    assertEquals('FALSE',$test.prop2->at(0)->id());
                }
                """);
        execute("test():Boolean[1]");
    }

    @Test
    public void testInvalidEnumReference()
    {
        compileTestSource("enumDefinition.pure", "Enum test::TestEnum {VAL1, VAL2}");
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "enumReference.pure",
                """
                function test::test():test::TestEnum[1]
                {
                  test::TestEnum.VAL3
                }
                """));
        assertPureException(PureCompilationException.class, "The enum value 'VAL3' can't be found in the enumeration test::TestEnum", "enumReference.pure", 3, 18, 3, 18, 3, 21, e);
    }

    @Test
    public void testInvalidEnumReferenceInQualifiedProperty()
    {
        compileTestSource("enumDefinition.pure", "Enum test::TestEnum {VAL1, VAL2}");
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "enumReference.pure",
            """
            Class test::TestClass
            {
                test()
                {
                    test::TestEnum.VAL3
                }:test::TestEnum[1];
            }
            """));
        assertPureException(PureCompilationException.class, "The enum value 'VAL3' can't be found in the enumeration test::TestEnum", "enumReference.pure", 5, 24, 5, 24, 5, 27, e);
    }

    @Test
    public void testEnumerationInCollection()
    {
        compileTestSource("/test/model.pure",
                """
                Enum test::Enum1
                {
                  A, B, C
                }
                """);
        compileTestSource("/test/test.pure",
                """
                function test::testFn():Any[*]
                {
                  [test::Enum1]
                }
                """);
        execute("test::testFn():Any[*]");
    }

    @Test
    public void testEnumerationInDoubleCollection()
    {
        compileTestSource("/test/model.pure",
                """
                Enum test::Enum1
                {
                  A, B, C
                }
                """);
        compileTestSource("/test/test.pure",
                """
                function test::testFn():Any[*]
                {
                  [[test::Enum1]]
                }
                """);
        execute("test::testFn():Any[*]");
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.<CodeRepository>withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories())
                .with(GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform"))
                .with(GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }
}
