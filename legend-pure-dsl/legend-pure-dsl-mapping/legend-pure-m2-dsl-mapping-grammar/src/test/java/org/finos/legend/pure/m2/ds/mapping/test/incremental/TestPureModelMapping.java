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

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureModelMapping extends AbstractPureMappingTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String TEST_MODEL_SOURCE_ID1 = "testModel1.pure";
    private static final String TEST_MODEL_SOURCE_ID2 = "testModel2.pure";
    private static final String TEST_MAPPING_SOURCE_ID1 = "testMapping1.pure";
    private static final String TEST_MAPPING_SOURCE_ID2 = "testMapping2.pure";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_MODEL_SOURCE_ID1,
            """
            Class Firm
            {
              legalName : String[1];\
              other : Integer[1];
              other1 : Float[1];
            }\
            """,
            TEST_MODEL_SOURCE_ID2,
            """
            Enum MyEnum
            {
               a,b
            }
            Class SourceFirm
            {
              name : String[1];\
              other2 : MyEnum[1];
            }\
            """,
            TEST_MAPPING_SOURCE_ID1,
            """
            ###Mapping
            Mapping FirmMapping
            (
              Firm : Pure
                     {
                        legalName : ['a','b']->map(k|$k+'Yeah!')->joinStrings(',') ,
                        other : 1+2,
                        other1 : 1.0+2.0
                     }
            )\
            """,
            TEST_MAPPING_SOURCE_ID2,
            """
            ###Mapping
            Mapping FirmMapping2
            (
              Firm : Pure
                     {
                        ~src SourceFirm
                        ~filter $src.other2 == MyEnum.b
                        legalName : $src.name,
                        other : $src.name->length(),
                        other1 : 3.14
                     }
            )\
            """
    );

    private static final ImmutableMap<String, String> TEST_SOURCES_WITH_TYPO = Maps.immutable.with(TEST_MODEL_SOURCE_ID1,
            """
            Class Firm
            {
              legalNameX : String[1];
              other : String[1];
              other1 : String[1];
            }\
            """);

    private static final ImmutableMap<String, String> TEST_MAPPING_SOURCE_WITH_ERROR = Maps.immutable.with(TEST_MAPPING_SOURCE_ID1,
            """
            ###Mapping
            Mapping FirmMapping
            (
              Firm : Pure
                     {
                        legalName : ['a','b']->maXp(k|$src->toString() + 'Yeah!') ,
                        other : 'ok' + 'op',
                        other1 : ['o','e']\
                     }
            )\
            """);

    private static final ImmutableMap<String, String> TEST_MAPPING_SOURCE_NO_SOURCE__ERROR = Maps.immutable.with(TEST_MAPPING_SOURCE_ID2,
            """
            ###Mapping
            Mapping FirmMapping2
            (
              Firm : Pure
                     {
                        legalName : $src.name
                     }
            )\
            """
    );

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete(TEST_MAPPING_SOURCE_ID1);
        runtime.delete(TEST_MAPPING_SOURCE_ID2);
        runtime.delete(TEST_MODEL_SOURCE_ID1);
        runtime.delete(TEST_MODEL_SOURCE_ID2);
        runtime.delete("modelCode.pure");
        runtime.delete("modelMappingCode.pure");
        runtime.delete("enumerationMappingCode.pure");
    }

    @Test
    public void testPureModelMapping_ModelWithDiffPropertiesShouldNotCompile() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        runtime.delete(TEST_MAPPING_SOURCE_ID2);
        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_MODEL_SOURCE_ID1);
            try
            {
                runtime.createInMemoryAndCompile(TEST_SOURCES_WITH_TYPO);
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The property 'legalName' is unknown in the Element 'Firm'",
                        TEST_MAPPING_SOURCE_ID1, 6, 13, 6, 13, 6, 21, e);
            }

            runtime.delete(TEST_MODEL_SOURCE_ID1);
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID1, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID1)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }


    @Test
    public void testPureModelMapping_WithErrorShouldNotCompile() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_MAPPING_SOURCE_ID1);
            try
            {
                runtime.createInMemoryAndCompile(TEST_MAPPING_SOURCE_WITH_ERROR);
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The system can't find a match for the function: maXp(_:String[2],_:LambdaFunction<{NULL[NULL]->NULL[NULL]}>[1])",
                        TEST_MAPPING_SOURCE_ID1, 6, 36, 6, 36, 6, 39, e);
            }

            runtime.delete(TEST_MAPPING_SOURCE_ID1);
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_MAPPING_SOURCE_ID1, TEST_SOURCES.get(TEST_MAPPING_SOURCE_ID1)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testPureModelMapping_WithErrorNoSourceShouldNotCompile() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_MAPPING_SOURCE_ID2);
            try
            {
                runtime.createInMemoryAndCompile(TEST_MAPPING_SOURCE_NO_SOURCE__ERROR);
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The variable 'src' is unknown!",
                        TEST_MAPPING_SOURCE_ID2, 6, 26, 6, 26, 6, 28, e);
            }

            runtime.delete(TEST_MAPPING_SOURCE_ID2);
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_MAPPING_SOURCE_ID2, TEST_SOURCES.get(TEST_MAPPING_SOURCE_ID2)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testPureModelMapping_WithErrorWrongSourceShouldNotCompile() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_MODEL_SOURCE_ID2);
            try
            {
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "SourceFirm has not been defined!",
                        TEST_MAPPING_SOURCE_ID2, 6, 18, 6, 18, 6, 27, e);
            }

            runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID2, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID2)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testPropertyMappingValueSpecificationContext()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class A {a: String[1];}")
                        .createInMemorySource("s2.pure", "Class B {b: String[1];} ")
                        .createInMemorySource("s3.pure", "###Mapping\n Mapping map(A : Pure {~src B\na: $src.bc.c})")
                        .createInMemorySource("s4.pure", "Class C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s4.pure", "Class\n C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile()
                        .updateSource("s4.pure", "Class C {c: String[1];} \n Association BC {bc: C[1]; cb: B[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void testPropertyMappingStabilityOnEnumerationMappingUpdation()
    {
        String modelCode = """
                ###Pure
                import my::*;
                
                Class my::SourceProduct
                {
                   id : Integer[1];
                   state : String[1];
                }
                
                Class my::TargetProduct
                {
                   id : Integer[1];
                   state : State[1];
                }
                
                Enum my::State
                {
                   ACTIVE,
                   INACTIVE
                }
                """;

        String modelMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::modelMapping
                (
                   include enumerationMapping
                
                   TargetProduct : Pure
                   {
                      ~src SourceProduct
                      id : $src.id,
                      state : EnumerationMapping StateMapping : $src.state
                   }
                )
                """;

        String enumerationMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::enumerationMapping
                (
                   State : EnumerationMapping StateMapping
                   {
                      ACTIVE : 1,
                      INACTIVE : 0
                   }
                )
                """;

        String updatedEnumerationMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::enumerationMapping
                (
                   State : EnumerationMapping StateMapping
                   {
                      ACTIVE : 'ACTIVE',
                      INACTIVE : 'INACTIVE'
                   }
                )
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("modelMappingCode.pure", modelMappingCode)
                        .createInMemorySource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("enumerationMappingCode.pure", updatedEnumerationMappingCode)
                        .compile()
                        .updateSource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testPropertyMappingStabilityOnEnumerationMappingDeletion()
    {
        String modelCode = """
                ###Pure
                import my::*;
                
                Class my::SourceProduct
                {
                   id : Integer[1];
                   state : String[1];
                }
                
                Class my::TargetProduct
                {
                   id : Integer[1];
                   state : State[1];
                }
                
                Enum my::State
                {
                   ACTIVE,
                   INACTIVE
                }
                """;

        String modelMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::modelMapping
                (
                   include enumerationMapping
                
                   TargetProduct : Pure
                   {
                      ~src SourceProduct
                      id : $src.id,
                      state : EnumerationMapping StateMapping : $src.state
                   }
                )
                """;

        String enumerationMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::enumerationMapping
                (
                   State : EnumerationMapping StateMapping
                   {
                      ACTIVE : 1,
                      INACTIVE : 0
                   }
                )
                """;

        String updatedEnumerationMappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::enumerationMapping
                (
                )
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("modelMappingCode.pure", modelMappingCode)
                        .createInMemorySource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("enumerationMappingCode.pure", updatedEnumerationMappingCode)
                        .compileWithExpectedCompileFailure("The transformer 'StateMapping' is unknown or is not of type EnumerationMapping in the Mapping 'my::modelMapping' for property state", "modelMappingCode.pure", 12, 7)
                        .updateSource("enumerationMappingCode.pure", enumerationMappingCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
