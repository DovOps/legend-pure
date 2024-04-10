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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.elements.property.AbstractTestDefaultValue;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestDynamicNew extends AbstractPureTestWithCoreCompiled
{
    private static final String DECLARATION = """
            Enum myEnum{A,B}\
            Class A\s
            [ testConstraint: $this.a == 'rrr']
            {\s
                a: String[1];     \s
                b: String[0..1];  \s
                c: String[*];     \s
                d : D[0..1];      \s
                ds : D[*];        \s
                enum : myEnum[1]; \s
                enums : myEnum[*];\s
            } \s
            Class D\s
            { \s
               name : String[1];
            }                  \s
                               \s
            Class E\s
            { \s
               handler : ConstraintsOverride[0..1];
            }                  \s
                               \s
            function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\s
            {
                [^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne()), ^D(name = $o->cast(@A).b->toOne() + $o->getHiddenPayload()->cast(@String)->toOne())] \s
            } \s
              \s
            function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\s
            {\s
               ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());  \s
            } \
            """;

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("defaultValueSource.pure");
        runtime.delete("/test/testModel.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleClassDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[1] \n{"
                        + "let a = A;  \n"
                        + "let r = dynamicNew($a,  \n"
                        + " [^KeyValue(key='a',value='rrr'),\n"
                        + "  ^KeyValue(key='b',value='eee'),\n"
                        + "  ^KeyValue(key='c',value=['zzz','kkk']),\n"
                        + "  ^KeyValue(key='enum',value=myEnum.A),\n"
                        + "  ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + " getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,getterOverrideToMany_Any_1__Property_1__Any_MANY_, '2')->cast(@A);\n"
                        + " assert('2' == $r->getHiddenPayload(), |'');\n"
                        + " assert(['rrr2','eee2'] == $r.ds.name, |'');\n"
                        + "}\n"
        );
    }

    @Test
    public void testSimpleGenericDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(^GenericType(rawType=A), \n"
                        + " [ ^KeyValue(key='a',value='rrr'), \n"
                        + "   ^KeyValue(key='b',value='eee'), \n"
                        + "   ^KeyValue(key='c',value=['zzz','kkk']), \n"
                        + "   ^KeyValue(key='enum',value=myEnum.A), \n"
                        + "   ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + "  getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,getterOverrideToMany_Any_1__Property_1__Any_MANY_,'2')->cast(@A) ;\n"
                        + "   assert('2' == $r->getHiddenPayload(), |'');\n"
                        + "   assert(['rrr2','eee2'] == $r.ds.name, |'');\n"
                        + "}\n");
    }


    @Test
    public void testSimpleClassDynamicNewWithConstraintOverrideOnly()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function constraintsManager(o:Any[1]):Any[1]\n"
                        + "{\n"
                        + "  $o;\n"
                        + "}"
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(A, \n"
                        + " [ ^KeyValue(key='a',value='eee'), \n"
                        + "   ^KeyValue(key='b',value='eee'), \n"
                        + "   ^KeyValue(key='d',value=^D(name='rrr2')),"
                        + "   ^KeyValue(key='ds',value=[^D(name='rrr2'),^D(name='eee2')]),"
                        + "   ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + "  [],[],[],constraintsManager_Any_1__Any_1_)->cast(@A) ;\n"
                        + "   assert([] == $r->getHiddenPayload(), |'');\n"
                        + "   assert('eee' == $r.a, |'');\n"
                        + "   assert('rrr2' == $r.d.name, |'');\n"
                        + "   assertSameElements(['rrr2','eee2'], $r.ds.name);\n"
                        + "}\n");
    }

    @Test
    public void testSimpleGenericDynamicNewWithConstraintOverrideOnly()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function constraintsManager(o:Any[1]):Any[1]\n"
                        + "{\n"
                        + "  $o;\n"
                        + "}"
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(^GenericType(rawType=A), \n"
                        + " [ ^KeyValue(key='a',value='eee'), \n"
                        + "   ^KeyValue(key='b',value='eee')], \n"
                        + "  [],[],[],constraintsManager_Any_1__Any_1_)->cast(@A) ;\n"
                        + "   assert([] == $r->getHiddenPayload(), |'');\n"
                        + "   assert('eee' == $r.a, |'');\n"
                        + "}\n");
    }

    @Test
    public void testPrinting()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + " let r = dynamicNew(A, \n"
                        + "     [ ^KeyValue(key='a',value='rrr'), \n"
                        + "         ^KeyValue(key='b',value='eee'), \n"
                        + "         ^KeyValue(key='d',value=^D(name='rrr2')),"
                        + "         ^KeyValue(key='ds',value=[^D(name='rrr2'),^D(name='eee2')]),"
                        + "         ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])]);\n"
                        + " print($r, 1);\n"
                        + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void cyclicalReferencesAreNotImplicit()
    {
        compileTestSource("testSource.pure",
                """
                Class F\s
                {           \s
                   str : String[1];\s
                   g : G[1];\s
                }           \s
                Class G     \s
                {           \s
                   f : F[1];\s
                }           \s
                function test():Any[*]\s
                {\
                 let f = dynamicNew(F,\s
                     [ ^KeyValue(key='str',value='foo') ] ) -> cast(@F);
                 let g = dynamicNew(G,\s
                     [ ^KeyValue(key='f',value=$f) ]) -> cast(@G);
                 assert($f.g == [], |'');
                 assert($g.f == $f, |'');
                 assert($g.f.g == [], |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void cyclicalReferencesAreImplicitWhenFromAssociations()
    {
        compileTestSource("testSource.pure",
                """
                Class H       \s
                {           \s
                   str : String[1];\s
                }           \s
                Class I     \s
                {}           \s
                Association assoc\s
                {\
                    i : I[1];  \s
                    h : H[1];  \s
                }\
                function test():Any[*]\s
                {\
                    let h = dynamicNew(H,\s
                        [ ^KeyValue(key='str',value='foo') ] ) -> cast(@H);
                    let i = dynamicNew(I,\s
                        [ ^KeyValue(key='h',value=$h) ]) -> cast(@I);
                    assert($h.i == $i, |'');
                    assert($i.h == $h, |'');
                    assert($i.h.i == $i, |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDynamicNewWithZeroToOneAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                """
                import test::*;
                Class test::TestClassA
                {
                  name : String[1];
                }
                
                Class test::TestClassB
                {
                }
                
                Association test::TestAssocAB
                {
                  toB : TestClassB[0..1];
                  toA : TestClassA[1];
                }
                
                function test::testFn():Any[*]
                {
                  let a = dynamicNew(TestClassA, [^KeyValue(key='name', value='A'), ^KeyValue(key='toB', value=[])])->cast(@TestClassA);
                  assert('A' == $a.name, |'');
                  assert($a.toB->isEmpty(), |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDynamicNewWithZeroToManyAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                """
                import test::*;
                Class test::TestClassA
                {
                  name : String[1];
                }
                
                Class test::TestClassB
                {
                }
                
                Association test::TestAssocAB
                {
                  toB : TestClassB[*];
                  toA : TestClassA[1];
                }
                
                function test::testFn():Any[*]
                {
                  let a = dynamicNew(TestClassA, [^KeyValue(key='name', value='A'), ^KeyValue(key='toB', value=[])])->cast(@TestClassA);
                  assert('A' == $a.name, |'');
                  assert($a.toB->isEmpty(), |'');
                }
                """);
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithDynamicNew()
    {
        compileTestSource("testSource.pure", AbstractTestDefaultValue.DECLARATION
                + "function test():Any[*] \n{"
                + " let r = dynamicNew(A, \n"
                + "     [ ^KeyValue(key='stringProperty',value='dynamicNew')]);\n"
                + " assertEquals(0.12, $r->cast(@A).inheritProperty);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithKeyValuePassedAsVariableToDynamicNew()
    {
        compileTestSource("testSource.pure", AbstractTestDefaultValue.DECLARATION
                + "function test():Any[*] \n{"
                + " let a = ^KeyValue(key='stringProperty',value='variable');"
                + " let b = ^KeyValue(key='enumProperty',value=EnumWithDefault.AnotherValue);"
                + " let r = dynamicNew(A, \n"
                + "     [ $a, $b ]);\n"
                + " print($r, 1);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories()));
        CodeRepository test = new GenericCodeRepository("test", null, "platform", "platform_functions");
        repositories.add(test);
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }
}
