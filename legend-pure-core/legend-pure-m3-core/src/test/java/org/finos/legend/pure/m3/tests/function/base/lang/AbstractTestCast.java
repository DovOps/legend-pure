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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestCast extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testCastError()
    {
        compileTestSource("fromString.pure",
                """
                Class A
                {
                  prop3:String[1];
                }
                Class B extends A
                {
                  prop2 : String[1];
                }
                Class C
                {
                  prop:String[1];
                }
                
                function testError():Nil[0]
                {
                   print(^A(prop3='a')->cast(@C).prop,1);
                }
                """);

        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testError():Nil[0]"));
        checkException(e, "Cast exception: A cannot be cast to C", "fromString.pure", 16, 25);
        Assertions.assertSame(e, findRootException(e));
    }

    @Test
    public void testInvalidCastWithTypeParameters()
    {
        compileTestSource("fromString.pure",
                """
                function test():Any[*]
                {
                   ^List<X>(values=^X(nameX = 'my name is X'))->castToListY().values.nameY->print(1);
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        checkInvalidCastWithTypeParametersTopLevelException(e);
        checkInvalidCastWithTypeParametersRootException(e);
    }

    protected void checkInvalidCastWithTypeParametersTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: List<X> cannot be cast to List<Y>", "fromString.pure", 3, 49);
    }

    protected void checkInvalidCastWithTypeParametersRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: List<X> cannot be cast to List<Y>", "/test/cast.pure", 46, 11);
    }

    @Test
    public void testInvalidPrimitiveDownCast()
    {
        compileTestSource("fromString.pure",
                """
                function test():Number[*]
                {
                   [1, 3.0, 'the cat sat on the mat']->cast(@Number)->plus()
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Number[*]"));
        checkException(e, "Cast exception: String cannot be cast to Number", "fromString.pure", 3, 40);
        Assertions.assertSame(e, findRootException(e));
    }

    @Test
    public void testPrimitiveConcreteOne()
    {
        compileTestSource("fromString.pure",
                """
                function test():Any[*]
                {
                   1->castToString()->joinStrings('');
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        checkPrimitiveConcreteOneTopLevelException(e);
        checkPrimitiveConcreteOneRootException(e);
    }

    protected void checkPrimitiveConcreteOneTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: Integer cannot be cast to String", "fromString.pure", 3, 7);
    }

    protected void checkPrimitiveConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: Integer cannot be cast to String", "/test/cast.pure", 31, 10);
    }

    @Test
    public void testPrimitiveConcreteMany()
    {
        compileTestSource("fromString.pure",
                """
                function testMany():Any[*]
                {
                   [1, 2.5, 'abc']->castToNumber()->plus();
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testMany():Any[*]"));
        checkPrimitiveConcreteManyTopLevelException(e);
        checkPrimitiveConcreteManyRootException(e);
    }

    protected void checkPrimitiveConcreteManyTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: String cannot be cast to Number", "fromString.pure", 3, 21);
    }

    protected void checkPrimitiveConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: String cannot be cast to Number", "/test/cast.pure", 36, 13);
    }

    @Test
    public void testNonPrimitiveConcreteOne()
    {
        compileTestSource("fromString.pure",
                """
                function test():Any[*]
                {
                   ^X()->castToY().nameY;
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        checkNonPrimitiveConcreteOneTopLevelException(e);
        checkNonPrimitiveConcreteOneRootException(e);
    }

    protected void checkNonPrimitiveConcreteOneTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: X cannot be cast to Y", "fromString.pure", 3, 10);
    }

    protected void checkNonPrimitiveConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 41, 12);
    }

    @Test
    public void testNonPrimitiveConcreteMany()
    {
        compileTestSource("fromString.pure",
                """
                function test():Any[*]
                {
                   [^X(), ^Y(), ^S()]->castToY().nameY;
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        checkNonPrimitiveConcreteManyTopLevelException(e);
        checkNonPrimitiveConcreteManyRootException(e);
    }

    protected void checkNonPrimitiveConcreteManyTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: X cannot be cast to Y", "fromString.pure", 3, 24);
    }

    protected void checkNonPrimitiveConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 41, 12);
    }

    @Test
    public void testPrimitiveNonConcreteOne()
    {
        compileTestSource("fromString.pure",
                """
                function testConcrete():Any[*]
                {
                   1->nonConcreteCastToString()->joinStrings('');
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testConcrete():Any[*]"));
        checkPrimitiveNonConcreteOneTopLevelException(e);
        checkPrimitiveNonConcreteOneRootException(e);
    }

    protected void checkPrimitiveNonConcreteOneTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: Integer cannot be cast to String", "fromString.pure", 3, 7);
    }

    protected void checkPrimitiveNonConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: Integer cannot be cast to String", "/test/cast.pure", 67, 26);
    }

    @Test
    public void testPrimitiveNonConcreteMany()
    {
        compileTestSource("fromString.pure",
                """
                function testNonConcrete():Any[*]
                {
                   [1, 2.5, 'abc']->nonConcreteCastToNumber()->plus();
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNonConcrete():Any[*]"));
        checkPrimitiveNonConcreteManyTopLevelException(e);
        checkPrimitiveNonConcreteManyRootException(e);
    }

    protected void checkPrimitiveNonConcreteManyTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: String cannot be cast to Number", "fromString.pure", 3, 21);
    }

    protected void checkPrimitiveNonConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: String cannot be cast to Number", "/test/cast.pure", 68, 17);
    }

    @Test
    public void testNonPrimitiveNonConcreteOne()
    {
        compileTestSource("fromString.pure",
                """
                function testNonPrimitive():Any[*]
                {
                   ^X()->nonConcreteCastToY().nameY;
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNonPrimitive():Any[*]"));
        checkNonPrimitiveNonConcreteOneTopLevelException(e);
        checkNonPrimitiveNonConcreteOneRootException(e);
    }

    protected void checkNonPrimitiveNonConcreteOneTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: X cannot be cast to Y", "fromString.pure", 3, 10);
    }

    protected void checkNonPrimitiveNonConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 67, 26);
    }

    @Test
    public void testNonPrimitiveNonConcreteMany()
    {
        compileTestSource("fromString.pure",
                """
                function test():Any[*]
                {
                   [^X(), ^Y(), ^S()]->nonConcreteCastToY().nameY;
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        checkNonPrimitiveNonConcreteManyTopLevelException(e);
        checkNonPrimitiveNonConcreteManyRootException(e);
    }

    protected void checkNonPrimitiveNonConcreteManyTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: X cannot be cast to Y", "fromString.pure", 3, 24);
    }

    protected void checkNonPrimitiveNonConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 68, 17);
    }

    @Test
    public void testEnumToStringCast()
    {
        compileTestSource("fromString.pure",
                """
                function testEnum():Any[*]
                {
                   Month.January -> castToString() -> joinStrings('');
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testEnum():Any[*]"));
        checkEnumToStringCastTopLevelException(e);
        checkEnumToStringCastRootException(e);
    }

    protected void checkEnumToStringCastTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: Month cannot be cast to String", "fromString.pure", 3, 21);
    }

    protected void checkEnumToStringCastRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: Month cannot be cast to String", "/test/cast.pure", 31, 10);
    }

    @Test
    public void testStringToEnumCast()
    {
        compileTestSource("fromString.pure",
                """
                function test():Nil[0]
                {
                   'January' -> cast(@Month) -> print(1);
                }\
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("test():Nil[0]"));
        checkStringToEnumCastTopLevelException(e);
        Assertions.assertSame(e, findRootException(e));
    }

    protected void checkStringToEnumCastTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: String cannot be cast to Month", "fromString.pure", 3, 17);
    }

    protected void checkException(Throwable t, String expectedInfo, String expectedSource, int expectedLine, int expectedCol)
    {
        assertPureException(PureExecutionException.class, expectedInfo, expectedSource, expectedLine, expectedCol, (Exception) t);
    }

    protected Exception findRootException(Throwable t)
    {
        Throwable cause = t.getCause();
        if ((cause != null) && (cause != t))
        {
            Exception root = findRootException(cause);
            if (root != null)
            {
                return root;
            }
        }
        return (t instanceof Exception e) ? e : null;
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(new GenericCodeRepository("test", null, "platform", "platform_functions"));
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }

    public static Pair<String, String> getExtra()
    {
        String code = readTextResource("org/finos/legend/pure/m3/cast/cast.pure", AbstractTestCast.class.getClassLoader());
        return Tuples.pair("/test/cast.pure", code);
    }
}
