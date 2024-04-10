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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeOperationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("modelCode.pure");
        runtime.delete("mappingCode.pure");
    }

    @Test
    public void testSimpleOperation() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("sourceId.pure", """
                                Class Person{name:String[1];}
                                function a():meta::pure::mapping::SetImplementation[*]{[]}
                                """)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("userId.pure", """
                                ###Mapping
                                Mapping myMap(
                                   Person[ppp]: Operation
                                           {
                                               a__SetImplementation_MANY_()
                                           }
                                )
                                """)
                        .compile()
                        .deleteSource("userId.pure")
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());


    }


    @Test
    public void testSimpleOperationReverse() throws Exception
    {
        runtime.createInMemorySource("userId.pure", """
                Class Person{name:String[1];}
                ###Mapping
                Mapping myMap(
                   Person[ppp]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                """
        );
        runtime.createInMemorySource("sourceId.pure", "function a():meta::pure::mapping::SetImplementation[*]{[]}\n");
        runtime.compile();

        int size = repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:6 column:16), \"a__SetImplementation_MANY_ has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", "function a():meta::pure::mapping::SetImplementation[*]{[]}\n");
            runtime.compile();
        }

        Assertions.assertEquals(size, repository.serialize().length);

    }


    @Test
    public void testSimpleOperationWithParameters() throws Exception
    {
        runtime.createInMemorySource("userId.pure", """
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
                """
        );
        runtime.compile();
    }


    @Test
    public void testSimpleOperationWithParametersWithError() throws Exception
    {
        runtime.createInMemorySource("userId.pure", """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap(
                   Person[op]: Operation
                           {
                               a__SetImplementation_MANY_(rel1,rel3)
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
                """
        );
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:userId.pure lines:5c4-8c12), \"The SetImplementation 'rel3' can't be found in the mapping 'myMap'\"", e.getMessage());
        }
    }


    @Test
    public void testSimpleOperationWithInclude() throws Exception
    {
        runtime.createInMemorySource("userId.pure", """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMapToInclude(
                   *Person[rel1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[rel2]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                ###Mapping
                Mapping myMap(
                   include myMapToInclude\
                   Person[op]: Operation
                           {
                               a__SetImplementation_MANY_(rel1,rel2)
                           }
                )
                """
        );
        runtime.compile();
    }

    @Test
    public void testSimpleOperationWithIncludeDelta() throws Exception
    {
        runtime.createInMemorySource("userId.pure", """
                Class Person{name:String[1];}
                function a():meta::pure::mapping::SetImplementation[*]{[]}
                ###Mapping
                Mapping myMap(
                   include myMapToInclude\
                   Person[op]: Operation
                           {
                               a__SetImplementation_MANY_(rel1,rel2)
                           }
                )
                """);
        String content = """
                ###Mapping
                Mapping myMapToInclude(
                   *Person[rel1]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                   Person[rel2]: Operation
                           {
                               a__SetImplementation_MANY_()
                           }
                )
                """;
        runtime.createInMemorySource("sourceId.pure", content);
        runtime.compile();

        int size = repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:5 column:12), \"myMapToInclude has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", content);
            runtime.compile();
        }

        Assertions.assertEquals(size, repository.serialize().length);
    }
}
