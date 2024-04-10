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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestAccess extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("/test/testSource.pure");
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
        runtime.delete("fromString.pure");
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");

        runtime.compile();
    }

    @Test
    public void testPrivateFunctionApplicationInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(s:String[1]):String[1]
                {
                    pkg::privateFunc($s, ' from public')
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(privateFunc, Instance.getValueForMetaPropertyToOneResolved(expressions.get(0), M3Properties.func, processorSupport));
    }

    @Test
    public void testPrivateFunctionIndirectApplicationInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(s:String[1]):String[1]
                {
                    plus(['one string plus ', pkg::privateFunc($s, ' from public'), ' plus another string'])
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the privateFunc reference
    }

    @Test
    public void testPrivateFunctionApplicationInCollectInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(strings:String[*]):String[*]
                {
                    $strings->map(s | pkg::privateFunc($s, ' from public'))
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the privateFunc reference
    }

    @Test
    public void testPrivateFunctionApplicationInLetInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(strings:String[*]):String[*]
                {
                    let f = {s:String[1] | pkg::privateFunc($s, ' from public')};
                    $strings->map($f);
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the privateFunc reference
    }

    @Test
    public void testPrivateFunctionApplicationInKeyExpressionInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class pkg::MyClass
                {
                    sprop:String[*];
                }
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(strings:String[*]):pkg::MyClass[1]
                {
                    ^pkg::MyClass(sprop=$strings->map(s | pkg::privateFunc($s, ' from public')))
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):MyClass[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the privateFunc reference
    }

    @Test
    public void testPrivateFunctionReferenceInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc():String[0..1]
                {
                    pkg::privateFunc_String_1__String_1__String_1_.functionName
                }\
                """);

        CoreInstance privateFunc = runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc():String[0..1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(privateFunc, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(expressions.get(0), M3Properties.parametersValues, processorSupport).get(0), M3Properties.values, processorSupport));
    }

    // Private function applications and references in a different package

    @Test
    public void testPrivateFunctionApplicationInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc(s:String[1]):String[1]
                    {
                        pkg1::privateFunc($s, ' from public')
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 11, e);
        }
    }

    @Test
    public void testPrivateFunctionIndirectApplicationInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc(s:String[1]):String[1]
                    {
                        plus(['one string plus ', pkg1::privateFunc($s, ' from public'), ' plus another string'])
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 37, e);
        }
    }

    @Test
    public void testPrivateFunctionApplicationInMapInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):String[*]
                    {
                        $strings->map(s | pkg1::privateFunc($s, ' from public'))
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 29, e);
        }
    }

    @Test
    public void testPrivateFunctionApplicationInLetInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):String[*]
                    {
                        let f = {s:String[1] | pkg1::privateFunc($s, ' from public')};\
                        $strings->map($f);
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 34, e);
        }
    }

    @Test
    public void testPrivateFunctionApplicationInKeyExpressionInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class pkg1::MyClass
                    {
                        sprop:String[*];
                    }
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):pkg1::MyClass[1]
                    {
                        ^pkg1::MyClass(sprop=$strings->map(s | pkg1::privateFunc($s, ' from public')))
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 15, 50, e);
        }
    }

    @Test
    public void testPrivateFunctionReferenceInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc():String[0..1]
                    {
                        pkg1::privateFunc_String_1__String_1__String_1_.functionName
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 11, e);
        }
    }

    @Test
    public void testPrivateFunctionIndirectReferenceInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from private)'
                    }
                    
                    function pkg2::publicFunc():String[*]
                    {
                        [pkg2::publicFunc__String_MANY_, pkg1::privateFunc_String_1__String_1__String_1_]->map(f | $f.functionName->toOne())
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 44, e);
        }
    }

    // Protected function applications and references in the same package

    @Test
    public void testProtectedFunctionApplicationInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc(s:String[1]):String[1]
                {
                    pkg::protectedFunc($s, ' from public')
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(protectedFunc, Instance.getValueForMetaPropertyToOneResolved(expressions.get(0), M3Properties.func, processorSupport));
    }

    @Test
    public void testProtectedFunctionIndirectApplicationInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc(s:String[1]):String[1]
                {
                    plus(['one string plus ', pkg::protectedFunc($s, ' from public'), ' plus another string'])
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInCollectInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc(strings:String[*]):String[*]
                {
                    $strings->map(s | pkg::protectedFunc($s, ' from public'))
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInLetInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc(strings:String[*]):String[*]
                {
                    let f = {s:String[1] | pkg::protectedFunc($s, ' from public')};
                    $strings->map($f);
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInKeyExpressionInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class pkg::MyClass
                {
                    sprop:String[*];
                }
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc(strings:String[*]):pkg::MyClass[1]
                {
                    ^pkg::MyClass(sprop=$strings->map(s | pkg::protectedFunc($s, ' from public')))
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[*]):MyClass[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionReferenceInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::publicFunc():String[0..1]
                {
                    pkg::protectedFunc_String_1__String_1__String_1_.functionName
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc():String[0..1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(protectedFunc, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(expressions.get(0), M3Properties.parametersValues, processorSupport).get(0), M3Properties.values, processorSupport));
    }

    // Protected function applications and references in a sub-package

    @Test
    public void testProtectedFunctionApplicationInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc(s:String[1]):String[1]
                {
                    pkg::protectedFunc($s, ' from public')
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(protectedFunc, Instance.getValueForMetaPropertyToOneResolved(expressions.get(0), M3Properties.func, processorSupport));
    }

    @Test
    public void testProtectedFunctionIndirectApplicationInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc(s:String[1]):String[1]
                {
                    plus(['one string plus ', pkg::protectedFunc($s, ' from public'), ' plus another string'])
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInCollectInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc(strings:String[*]):String[*]
                {
                    $strings->map(s | pkg::protectedFunc($s, ' from public'))
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInLetInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc(strings:String[*]):String[*]
                {
                    let f = {s:String[1] | pkg::protectedFunc($s, ' from public')};
                    $strings->map($f);
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[*]):String[*]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionApplicationInKeyExpressionInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class pkg::MyClass
                {
                    sprop:String[*];
                }
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc(strings:String[*]):pkg::MyClass[1]
                {
                    ^pkg::MyClass(sprop=$strings->map(s | pkg::protectedFunc($s, ' from public')))
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[*]):MyClass[1]");
        Assertions.assertNotNull(publicFunc);
        // TODO verify the protectedFunc reference
    }

    @Test
    public void testProtectedFunctionReferenceInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function pkg::sub::publicFunc():String[0..1]
                {
                    pkg::protectedFunc_String_1__String_1__String_1_.functionName
                }\
                """);

        CoreInstance protectedFunc = runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc():String[0..1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertTrue(Instance.instanceOf(expressions.get(0), M3Paths.FunctionExpression, processorSupport));
        Assertions.assertSame(protectedFunc, Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(expressions.get(0), M3Properties.parametersValues, processorSupport).get(0), M3Properties.values, processorSupport));
    }

    // Protected function applications and references in a different package

    @Test
    public void testProtectedFunctionApplicationInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc(s:String[1]):String[1]
                    {
                        pkg1::protectedFunc($s, ' from public')
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 11, e);
        }
    }

    @Test
    public void testProtectedFunctionIndirectApplicationInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc(s:String[1]):String[1]
                    {
                        plus(['one string plus ', pkg1::protectedFunc($s, ' from public'), ' plus another string'])
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 37, e);
        }
    }

    @Test
    public void testProtectedFunctionApplicationInMapInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):String[*]
                    {
                        $strings->map(s | pkg1::protectedFunc($s, ' from public'))
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 29, e);
        }
    }

    @Test
    public void testProtectedFunctionApplicationInLetInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):String[*]
                    {
                        let f = {s:String[1] | pkg1::protectedFunc($s, ' from public')};\
                        $strings->map($f);
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 34, e);
        }
    }

    @Test
    public void testProtectedFunctionApplicationInKeyExpressionInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class pkg1::MyClass
                    {
                        sprop:String[*];
                    }
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc(strings:String[*]):pkg1::MyClass[1]
                    {
                        ^pkg1::MyClass(sprop=$strings->map(s | pkg1::protectedFunc($s, ' from public')))
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 15, 50, e);
        }
    }

    @Test
    public void testProtectedFunctionReferenceInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc():String[0..1]
                    {
                        pkg1::protectedFunc_String_1__String_1__String_1_.functionName
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 11, e);
        }
    }

    @Test
    public void testProtectedFunctionIndirectReferenceInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                    {
                        $string1 + $string2 + ' (from protected)'
                    }
                    
                    function pkg2::publicFunc():String[*]
                    {
                        [pkg2::publicFunc__String_MANY_, pkg1::protectedFunc_String_1__String_1__String_1_]->map(f | $f.functionName->toOne())
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "fromString.pure", 10, 44, e);
        }
    }

    // Private class references in the same package

    @Test
    public void testPrivateClassReferenceInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.private>> pkg::PrivateClass
                {
                    name : String[1];
                }
                
                function pkg::publicFunc(s:String[1]):pkg::PrivateClass[1]
                {
                    ^pkg::PrivateClass(name=$s)
                }\
                """);

        CoreInstance privateClass = runtime.getCoreInstance("pkg::PrivateClass");
        Assertions.assertNotNull(privateClass);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):PrivateClass[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        CoreInstance expression = expressions.get(0);
        Assertions.assertTrue(Instance.instanceOf(expression, M3Paths.FunctionExpression, processorSupport));
        CoreInstance parameterValue = Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.parametersValues, processorSupport).get(0);
        Assertions.assertSame(privateClass, Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport));
    }

    @Test
    public void testPrivateClassExtensionInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.private>> pkg::PrivateClass
                {
                    name : String[1];
                }
                
                Class pkg::PublicSubclass extends pkg::PrivateClass
                {
                }\
                """);

        CoreInstance privateClass = runtime.getCoreInstance("pkg::PrivateClass");
        Assertions.assertNotNull(privateClass);

        CoreInstance publicSubclass = runtime.getCoreInstance("pkg::PublicSubclass");
        Assertions.assertNotNull(publicSubclass);
        Assertions.assertSame(privateClass, Type.getGeneralizationResolutionOrder(publicSubclass, processorSupport).get(1));
    }

    @Test
    public void testPrivateClassPropertyTypeInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.private>> pkg::PrivateClass
                {
                    name : String[1];
                }
                
                Class pkg::PublicClass
                {
                    prop : pkg::PrivateClass[1];
                }\
                """);

        CoreInstance privateClass = runtime.getCoreInstance("pkg::PrivateClass");
        Assertions.assertNotNull(privateClass);

        CoreInstance publicClass = runtime.getCoreInstance("pkg::PublicClass");
        Assertions.assertNotNull(publicClass);
        // TODO validate property generic type
    }

    @Test
    public void testPrivateClassAssociationPropertyTypeInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.private>> pkg::PrivateClass
                {
                    name : String[1];
                }
                
                Class pkg::PublicClass
                {
                }
                
                Association pkg::PublicAssociation
                {
                    left : pkg::PrivateClass[1];
                    right : pkg::PublicClass[1];
                }\
                """);

        CoreInstance privateClass = runtime.getCoreInstance("pkg::PrivateClass");
        Assertions.assertNotNull(privateClass);

        CoreInstance publicClass = runtime.getCoreInstance("pkg::PublicClass");
        Assertions.assertNotNull(publicClass);

        CoreInstance publicAssociation = runtime.getCoreInstance("pkg::PublicAssociation");
        Assertions.assertNotNull(publicAssociation);
        // TODO validate property generic type
    }

    // Private class references in different packages

    @Test
    public void testPrivateClassReferenceInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class <<access.private>> pkg1::PrivateClass
                    {
                        name : String[1];
                    }
                    
                    function pkg2::publicFunc(s:String[1]):pkg1::PrivateClass[1]
                    {
                        ^pkg1::PrivateClass(name=$s)
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::PrivateClass is not accessible in pkg2", "fromString.pure", 8, 46, e);
        }
    }

    @Test
    public void testPrivateClassExtensionInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class <<access.private>> pkg1::PrivateClass
                    {
                        name : String[1];
                    }
                    
                    Class pkg2::PublicSubclass extends pkg1::PrivateClass
                    {
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::PrivateClass is not accessible in pkg2", "fromString.pure", 8, 42, e);
        }
    }

    @Test
    public void testPrivateClassPropertyTypeInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class <<access.private>> pkg1::PrivateClass
                    {
                        name : String[1];
                    }
                    
                    Class pkg2::PublicClass
                    {
                        prop : pkg1::PrivateClass[1];
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::PrivateClass is not accessible in pkg2", "fromString.pure", 10, 18, e);
        }
    }

    @Test
    public void testPrivateClassAssociationPropertyTypeInDifferentPackage()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    import meta::pure::profiles::*;
                    
                    Class <<access.private>> pkg1::PrivateClass
                    {
                        name : String[1];
                    }
                    
                    Class pkg1::PublicClass
                    {
                    }
                    
                    Association pkg2::PublicAssociation
                    {
                        left : pkg1::PrivateClass[1];
                        right : pkg1::PublicClass[1];
                    }\
                    """);
            Assertions.fail("Expected a compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "pkg1::PrivateClass is not accessible in pkg2", "fromString.pure", 14, 18, e);
        }
    }

    // Protected class references in the same package

    @Test
    public void testProtectedClassReferenceInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                function pkg::publicFunc(s:String[1]):pkg::ProtectedClass[1]
                {
                    ^pkg::ProtectedClass(name=$s)
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicFunc = runtime.getFunction("pkg::publicFunc(String[1]):ProtectedClass[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        CoreInstance expression = expressions.get(0);
        Assertions.assertTrue(Instance.instanceOf(expression, M3Paths.FunctionExpression, processorSupport));
        CoreInstance parameterValue = Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.parametersValues, processorSupport).get(0);
        Assertions.assertSame(protectedClass, Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport));
    }

    @Test
    public void testProtectedClassExtensionInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg::PublicSubclass extends pkg::ProtectedClass
                {
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicSubclass = runtime.getCoreInstance("pkg::PublicSubclass");
        Assertions.assertNotNull(publicSubclass);
        Assertions.assertSame(protectedClass, Type.getGeneralizationResolutionOrder(publicSubclass, processorSupport).get(1));
    }

    @Test
    public void testProtectedClassPropertyTypeInSamePackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg::PublicClass
                {
                    prop : pkg::ProtectedClass[1];
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicClass = runtime.getCoreInstance("pkg::PublicClass");
        Assertions.assertNotNull(publicClass);
        // TODO validate property generic type
    }

    // Protected class references in a sub-package

    @Test
    public void testProtectedClassReferenceInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                function pkg::sub::publicFunc(s:String[1]):pkg::ProtectedClass[1]
                {
                    ^pkg::ProtectedClass(name=$s)
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicFunc = runtime.getFunction("pkg::sub::publicFunc(String[1]):ProtectedClass[1]");
        Assertions.assertNotNull(publicFunc);
        ListIterable<? extends CoreInstance> expressions = Instance.getValueForMetaPropertyToManyResolved(publicFunc, M3Properties.expressionSequence, processorSupport);
        Assertions.assertEquals(1, expressions.size());
        CoreInstance expression = expressions.get(0);
        Assertions.assertTrue(Instance.instanceOf(expression, M3Paths.FunctionExpression, processorSupport));
        CoreInstance parameterValue = Instance.getValueForMetaPropertyToManyResolved(expression, M3Properties.parametersValues, processorSupport).get(0);
        Assertions.assertSame(protectedClass, Instance.getValueForMetaPropertyToOneResolved(parameterValue, M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport));
    }

    @Test
    public void testProtectedClassExtensionInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg::sub::PublicSubclass extends pkg::ProtectedClass
                {
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicSubclass = runtime.getCoreInstance("pkg::sub::PublicSubclass");
        Assertions.assertNotNull(publicSubclass);
        Assertions.assertSame(protectedClass, Type.getGeneralizationResolutionOrder(publicSubclass, processorSupport).get(1));
    }

    @Test
    public void testProtectedClassPropertyTypeInSubPackage()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg::sub::PublicClass
                {
                    prop : pkg::ProtectedClass[1];
                }\
                """);

        CoreInstance protectedClass = runtime.getCoreInstance("pkg::ProtectedClass");
        Assertions.assertNotNull(protectedClass);

        CoreInstance publicClass = runtime.getCoreInstance("pkg::sub::PublicClass");
        Assertions.assertNotNull(publicClass);
        // TODO validate property generic type
    }

    // Protected class references in different packages

    @Test
    public void testProtectedClassReferenceInDifferentPackage()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg1::ProtectedClass
                {
                    name : String[1];
                }
                
                function pkg2::publicFunc(s:String[1]):pkg1::ProtectedClass[1]
                {
                    ^pkg1::ProtectedClass(name=$s)
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg1::ProtectedClass is not accessible in pkg2", "fromString.pure", 8, 46, e);
    }

    @Test
    public void testProtectedClassExtensionInDifferentPackage()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg1::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg2::PublicSubclass extends pkg1::ProtectedClass
                {
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg1::ProtectedClass is not accessible in pkg2", "fromString.pure", 8, 42, e);
    }

    @Test
    public void testProtectedClassPropertyTypeInDifferentPackage()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                Class <<access.protected>> pkg1::ProtectedClass
                {
                    name : String[1];
                }
                
                Class pkg2::PublicClass
                {
                    prop : pkg1::ProtectedClass[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg1::ProtectedClass is not accessible in pkg2", "fromString.pure", 10, 18, e);
    }

    // Private properties are not allowed

    @Test
    public void testPrivateProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                Class pkg1::TestClass
                {
                    <<access.private>> name : String[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "Only classes and functions may have an access level", "fromString.pure", 5, 24, e);
    }

    // Protected properties are not allowed

    @Test
    public void testProtectedProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                Class pkg1::TestClass
                {
                    <<access.protected>> name : String[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "Only classes and functions may have an access level", "fromString.pure", 5, 26, e);
    }

    // Private with incremental compilation

    @Test
    public void testPrivateFunctionApplicationInDifferentPackageWithIncrementalCompilation()
    {
        String source1 = """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }\
                """;
        String source2 = """
                import meta::pure::profiles::*;
                function pkg2::publicFunc(s:String[1]):String[1]
                {
                    $s + ' from public'
                }\
                """;
        String source22 = """
                import meta::pure::profiles::*;
                function pkg2::publicFunc(s:String[1]):String[1]
                {
                    pkg1::privateFunc($s, ' from public')
                }\
                """;

        // compile source1 and source2, which should work
        runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source1), Tuples.pair("source2.pure", source2));

        // remove source2
        runtime.delete("source2.pure");
        runtime.compile();

        // now compile source3, which should fail
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> runtime.createInMemoryAndCompile(Tuples.pair("source2.pure", source22)));
        assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "source2.pure", 4, 11, e);
    }

    @Test
    public void testPrivateFunctionApplicationInDifferentPackageWithIncrementalCompilation2()
    {
        String source1 = """
                import meta::pure::profiles::*;
                
                function pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }\
                """;
        String source12 = """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg1::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }\
                """;
        String source2 = """
                import meta::pure::profiles::*;
                function pkg2::publicFunc(s:String[1]):String[1]
                {
                    pkg1::privateFunc($s, ' from public')
                }\
                """;
        // compile source1 and source2, which should work
        runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source1), Tuples.pair("source2.pure", source2));

        // remove source1
        runtime.delete("source1.pure");
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The system can't find a match for the function: pkg1::privateFunc(_:String[1],_:String[1])", "source2.pure", 4, 11, e1);

        // now compile source12, which should fail
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, () -> runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source12)));
        assertPureException(PureCompilationException.class, "pkg1::privateFunc(String[1], String[1]):String[1] is not accessible in pkg2", "source2.pure", 4, 11, e2);
    }

    // Protected with incremental compilation

    @Test
    public void testProtectedFunctionApplicationInDifferentPackageWithIncrementalCompilation()
    {
        String source1 = """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }\
                """;
        String source2 = """
                import meta::pure::profiles::*;
                function pkg1::sub::publicFunc(s:String[1]):String[1]
                {
                    pkg1::protectedFunc($s, ' from public')
                }\
                """;
        String source22 = """
                import meta::pure::profiles::*;
                function pkg2::publicFunc(s:String[1]):String[1]
                {
                    pkg1::protectedFunc($s, ' from public')
                }\
                """;

        // compile source1 and source2, which should work
        runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source1), Tuples.pair("source2.pure", source2));

        // remove source2
        runtime.delete("source2.pure");
        runtime.compile();

        // now compile source3, which should fail
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> runtime.createInMemoryAndCompile(Tuples.pair("source2.pure", source22)));
        assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "source2.pure", 4, 11, e);
    }

    @Test
    public void testProtectedFunctionApplicationInDifferentPackageWithIncrementalCompilation2()
    {
        String source1 = """
                import meta::pure::profiles::*;
                
                function pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }\
                """;
        String source12 = """
                import meta::pure::profiles::*;
                
                function <<access.protected>> pkg1::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }\
                """;
        String source2 = """
                import meta::pure::profiles::*;
                function pkg2::publicFunc(s:String[1]):String[1]
                {
                    pkg1::protectedFunc($s, ' from public')
                }\
                """;
        // compile source1 and source2, which should work
        runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source1), Tuples.pair("source2.pure", source2));

        // remove source1
        runtime.delete("source1.pure");
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The system can't find a match for the function: pkg1::protectedFunc(_:String[1],_:String[1])", "source2.pure", 4, 11, e1);

        // now compile source12, which should fail
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, () -> runtime.createInMemoryAndCompile(Tuples.pair("source1.pure", source12)));
        assertPureException(PureCompilationException.class, "pkg1::protectedFunc(String[1], String[1]):String[1] is not accessible in pkg2", "source2.pure", 4, 11, e2);
    }

    // Externalizable non-function

    @Test
    public void testExternalizableClass()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                """
                Class <<access.externalizable>> TestClass
                {
                }\
                """));
        assertPureException(PureCompilationException.class, "Only functions may have an access level of externalizable", "/test/testSource.pure", 1, 1, 1, 33, 3, 1, e);
    }

    // Externalizable with invalid parameter types

    @Test
    public void testExternalizableFunctionWithInvalidParameterType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                """
                function <<access.externalizable>> pkg1::extFunc(primitiveParam:String[1], nonPrimitiveParam:test::List<String>[0..1]):String[*]
                {
                  []
                }
                Class test::List<T>
                {
                    <<equality.Key>> values : T[*];
                }\
                """));
        assertPureException(PureCompilationException.class, "Functions with access level externalizable may only have primitive types or 'Maps' as parameter types; found List<String> for the type of parameter 'nonPrimitiveParam'", "/test/testSource.pure", 1, 1, 1, 42, 4, 1, e);
    }

    @Test
    public void testExternalizableFunctionWithInvalidParameterMultiplicity()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                """
                function <<access.externalizable>> pkg1::extFunc(toOneParam:String[1], zeroToOneParam:Integer[0..1], toManyParam:Number[*], toOneManyParam:Boolean[1..*]):String[*]
                {
                  []
                }
                """));
        assertPureException(PureCompilationException.class, "Functions with access level externalizable may only have parameters with multiplicity 0..1, 1, or *; found 1..* for the multiplicity of parameter 'toOneManyParam'", "/test/testSource.pure", 1, 1, 1, 42, 4, 1, e);
    }

    @Test
    public void testExternalizableFunctionWithInvalidReturnType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                """
                function <<access.externalizable>> pkg1::extFunc(toOneParam:String[1]):test::List<String>[1]
                {
                  ^test::List<String>(values=$toOneParam)
                }
                Class test::List<T>{values:T[*];}\
                """));
        assertPureException(PureCompilationException.class, "Functions with access level externalizable may only have primitive types as return types; found List<String>", "/test/testSource.pure", 1, 1, 1, 42, 4, 1, e);
    }

    @Test
    public void testExternalizableFunctionWithNameConflict()
    {
        compileTestSource("/test/testSource1.pure",
                """
                function <<access.externalizable>> test::pkg1::extFunc(strings:String[*]):String[1]
                {
                  'k'
                }
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource2.pure",
                """
                function <<access.externalizable>> test::pkg2::extFunc(string:String[1], n:Integer[1]):String[1]
                {
                  'z'
                }
                """));
        assertPureException(PureCompilationException.class, """
                Externalizable function name conflict - multiple functions with the name 'extFunc':
                	test::pkg1::extFunc(String[*]):String[1] (/test/testSource1.pure:1c1-4c1)
                	test::pkg2::extFunc(String[1], Integer[1]):String[1] (/test/testSource2.pure:1c1-4c1)\
                """, "/test/testSource2.pure", 1, 1, 1, 48, 4, 1, e);
    }

    // Multiple access levels are not allowed

    @Test
    public void testMultipleAccessLevels()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                import meta::pure::profiles::*;
                
                function <<access.private, access.protected>> pkg::func(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }\
                """));
        assertPureException(PureCompilationException.class, "pkg::func(String[1], String[1]):String[1] has multiple access level stereotypes", "fromString.pure", 3, 52, e);
    }

    @Test
    public void testHasExplicitAccessLevel()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function pkg::publicFunc(s:String[1]):String[1]
                {
                    pkg::privateFunc($s, ' from public')
                }\
                """);

        PackageableFunction<?> privateFunc = (PackageableFunction<?>) runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);
        Assertions.assertTrue(AccessLevel.hasExplicitAccessLevel(privateFunc, processorSupport));

        PackageableFunction<?> publicFunc = (PackageableFunction<?>) runtime.getFunction("pkg::publicFunc(String[1]):String[1]");
        Assertions.assertNotNull(publicFunc);
        Assertions.assertFalse(AccessLevel.hasExplicitAccessLevel(publicFunc, processorSupport));
    }

    @Test
    public void testGetAccessLevelStereotypes()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function <<access.public>> pkg::explicitPublicFunc(s:String[1]):String[1]
                {
                    $s + ' (from explicit public)'
                }
                
                function pkg::implicitPublicFunc(s:String[1]):String[1]
                {
                    $s + ' (from implicit public)'
                }
                
                function <<access.externalizable>> pkg::externalizableFunc(s:String[1]):String[1]
                {
                  $s + ' (from externalizable)'
                }\
                """);

        PackageableFunction<?> privateFunc = (PackageableFunction<?>) runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);
        Assertions.assertEquals(ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) privateFunc._stereotypesCoreInstance(), processorSupport), AccessLevel.getAccessLevelStereotypes(privateFunc, processorSupport));

        PackageableFunction<?> protectedFunc = (PackageableFunction<?>) runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);
        Assertions.assertEquals(ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) protectedFunc._stereotypesCoreInstance(), processorSupport), AccessLevel.getAccessLevelStereotypes(protectedFunc, processorSupport));

        PackageableFunction<?> explicitPublicFunc = (PackageableFunction<?>) runtime.getFunction("pkg::explicitPublicFunc(String[1]):String[1]");
        Assertions.assertNotNull(explicitPublicFunc);
        Assertions.assertEquals(ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) explicitPublicFunc._stereotypesCoreInstance(), processorSupport), AccessLevel.getAccessLevelStereotypes(explicitPublicFunc, processorSupport));

        PackageableFunction<?> implicitPublicFunc = (PackageableFunction<?>) runtime.getFunction("pkg::implicitPublicFunc(String[1]):String[1]");
        Assertions.assertNotNull(implicitPublicFunc);
        Assertions.assertEquals(Lists.immutable.with(), AccessLevel.getAccessLevelStereotypes(implicitPublicFunc, processorSupport));

        PackageableFunction<?> externalizableFunc = (PackageableFunction<?>) runtime.getFunction("pkg::externalizableFunc(String[1]):String[1]");
        Assertions.assertNotNull(externalizableFunc);
        Assertions.assertEquals(ImportStub.withImportStubByPasses((ListIterable<? extends CoreInstance>) externalizableFunc._stereotypesCoreInstance(), processorSupport), AccessLevel.getAccessLevelStereotypes(externalizableFunc, processorSupport));
    }

    @Test
    public void testGetAccessLevel()
    {
        compileTestSource("fromString.pure", """
                import meta::pure::profiles::*;
                
                function <<access.private>> pkg::privateFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from private)'
                }
                
                function <<access.protected>> pkg::protectedFunc(string1:String[1], string2:String[1]):String[1]
                {
                    $string1 + $string2 + ' (from protected)'
                }
                
                function <<access.public>> pkg::explicitPublicFunc(s:String[1]):String[1]
                {
                    $s + ' (from explicit public)'
                }
                
                function pkg::implicitPublicFunc(s:String[1]):String[1]
                {
                    $s + ' (from implicit public)'
                }
                
                function <<access.externalizable>> pkg::externalizableFunc(s:String[1]):String[1]
                {
                  $s + ' (from externalizable)'
                }\
                """);

        PackageableFunction<?> privateFunc = (PackageableFunction<?>) runtime.getFunction("pkg::privateFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(privateFunc);
        Assertions.assertSame(AccessLevel.PRIVATE, AccessLevel.getAccessLevel(privateFunc, context, processorSupport));

        PackageableFunction<?> protectedFunc = (PackageableFunction<?>) runtime.getFunction("pkg::protectedFunc(String[1], String[1]):String[1]");
        Assertions.assertNotNull(protectedFunc);
        Assertions.assertSame(AccessLevel.PROTECTED, AccessLevel.getAccessLevel(protectedFunc, context, processorSupport));

        PackageableFunction<?> explicitPublicFunc = (PackageableFunction<?>) runtime.getFunction("pkg::explicitPublicFunc(String[1]):String[1]");
        Assertions.assertNotNull(explicitPublicFunc);
        Assertions.assertSame(AccessLevel.PUBLIC, AccessLevel.getAccessLevel(explicitPublicFunc, context, processorSupport));

        PackageableFunction<?> implicitPublicFunc = (PackageableFunction<?>) runtime.getFunction("pkg::implicitPublicFunc(String[1]):String[1]");
        Assertions.assertNotNull(implicitPublicFunc);
        Assertions.assertSame(AccessLevel.PUBLIC, AccessLevel.getAccessLevel(implicitPublicFunc, context, processorSupport));

        PackageableFunction<?> externalizableFunc = (PackageableFunction<?>) runtime.getFunction("pkg::externalizableFunc(String[1]):String[1]");
        Assertions.assertNotNull(externalizableFunc);
        Assertions.assertSame(AccessLevel.EXTERNALIZABLE, AccessLevel.getAccessLevel(externalizableFunc, context, processorSupport));
    }
}
