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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistry;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class TestMemoryFileManager extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testLoadClassesFromZipInputStream() throws IOException
    {
        compileTestSource("""
                Class Person
                {
                   lastName:String[1];
                }
                function testPrint():Nil[0]
                {
                    s(Person);
                }
                function s(a:Any[1]):Nil[0]\
                {\
                   []\
                }\
                """);

        PureJavaCompiler compiler = ((FunctionExecutionCompiled) functionExecution).getJavaCompiler();
        MutableMap<URI, ClassJavaSource> expectedSourcesByURI = compiler.getFileManager().getAllClassJavaSources(true).groupByUniqueKey(SimpleJavaFileObject::toUri, Maps.mutable.empty());
        Verify.assertNotEmpty(expectedSourcesByURI);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jarStream = new JarOutputStream(bytes))
        {
            compiler.writeClassJavaSourcesToJar(jarStream);
        }

        MemoryFileManager fileManager = new MemoryFileManager(ToolProvider.getSystemJavaCompiler());
        try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(bytes.toByteArray())))
        {
            fileManager.loadClassesFromZipInputStream(jarStream);
        }

        MutableMap<URI, ClassJavaSource> actualSourcesByURI = fileManager.getAllClassJavaSources(true).groupByUniqueKey(SimpleJavaFileObject::toUri, Maps.mutable.empty());
        Verify.assertSetsEqual(expectedSourcesByURI.keySet(), actualSourcesByURI.keySet());
        for (URI uri : expectedSourcesByURI.keysView())
        {
            ClassJavaSource expectedSource = expectedSourcesByURI.get(uri);
            ClassJavaSource actualSource = actualSourcesByURI.get(uri);
            Assertions.assertArrayEquals(expectedSource.getBytes(), actualSource.getBytes(), uri.toString());
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
