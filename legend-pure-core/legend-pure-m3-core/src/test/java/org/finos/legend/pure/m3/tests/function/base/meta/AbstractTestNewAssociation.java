// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestNewAssociation extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("StandardCall.pure");
        runtime.compile();
    }

    @Test
    public void standardCall()
    {
        String source = """
                function go():Any[*]
                {
                    let classA = 'meta::pure::functions::meta::A'->newClass();
                    let classB = 'meta::pure::functions::meta::B'->newClass();
                    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);
                    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);
                    let newAssociation = 'meta::pure::functions::meta::A_B'->newAssociation($propertyA, $propertyB);
                    assertEquals('A_B', $newAssociation.name);
                    assertEquals('meta', $newAssociation.package.name);
                    assertEquals('meta::pure::functions::meta::A_B', $newAssociation->elementToPath());
                    assertEquals('a', $newAssociation.properties->at(0).name);
                    assertEquals('b', $newAssociation.properties->at(1).name);
                }
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmptyPackage()
    {
        String source = """
                function go():Any[*]
                {
                    let classA = 'A'->newClass();
                    let classB = 'B'->newClass();
                    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);
                    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);
                    let newAssociation = 'A_B'->newAssociation($propertyA, $propertyB);
                    assertEquals('A_B', $newAssociation.name);
                    assertEquals('A_B', $newAssociation->elementToPath());
                }\
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void callWithEmpty()
    {
        String source = """
                function go():Any[*]
                {
                    let classA = 'A'->newClass();
                    let classB = 'B'->newClass();
                    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);
                    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);
                    let newAssociation = ''->newAssociation($propertyA, $propertyB);
                    assertEquals('', $newAssociation.name);
                    assertEquals('', $newAssociation->elementToPath());
                }\
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void newPackage()
    {
        String source = """
                function go():Any[*]
                {
                    let classA = 'A'->newClass();
                    let classB = 'B'->newClass();
                    let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);
                    let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);
                    let newAssociation = 'foo::bar::A_B'->newAssociation($propertyA, $propertyB);
                    assertEquals('foo::bar::A_B', $newAssociation->elementToPath());
                }\
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
