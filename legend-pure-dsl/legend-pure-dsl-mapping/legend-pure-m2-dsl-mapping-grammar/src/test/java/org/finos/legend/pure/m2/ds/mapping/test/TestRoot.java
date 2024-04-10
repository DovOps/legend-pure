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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestRoot extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
    }

    @Test
    public void testRoot()
    {
        String source = """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap(
                   *Person[op]: Operation
                           {
                               a__SetImplementation_MANY_(rel1,rel2)
                           }
                   Person[rel1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[rel2]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                """;
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testRootError() throws Exception
    {
        runtime.createInMemorySource("userId.pure",
                """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap(
                   *Person[op]: Operation
                           {
                               a__SetImplementation_MANY_(rel1,rel2)
                           }
                   *Person[rel1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[rel2]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                """
        );
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The class 'Person' is mapped by 3 set implementations and has 2 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "userId.pure", 4, 9, e);
    }


    @Test
    public void testRootWithInclude()
    {
        String source = """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap1(
                   *Person[one]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[two]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                ###Mapping
                Mapping myMap2(
                   *Person[one_1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[two_1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                Mapping includeMap(
                   include myMap1\
                   include myMap2\
                )
                """;
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testRootWithIncludeDuplicate()
    {
        String source = """
                Class Person{name:String[1];}
                Enum OK {e_true,e_false}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap1(\
                   OK: EnumerationMapping Foo
                   {
                        e_true:  ['FTC', 'FTO'],
                        e_false: 'FTE'
                   }
                   *Person[one]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[two]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                ###Mapping
                Mapping myMap2(
                   *Person[one_1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[two_1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                Mapping myMap3
                (
                   include myMap1
                )
                Mapping includeMap(
                   include myMap1
                   include myMap2
                   include myMap3
                )
                """;
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testDuplicateError()
    {
        runtime.createInMemorySource("userId.pure",
                """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap1(
                   *Person[one]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[one]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'one' in mapping myMap1", 9, 4, e);
    }
}


