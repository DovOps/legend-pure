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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeEnumerationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String TEST_ENUM_MODEL_SOURCE_ID = "testModel.pure";
    private static final String TEST_ENUMERATION_MAPPING_SOURCE_ID = "testMapping.pure";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
            """
            Enum test::EmployeeType
            {
                CONTRACT,
                FULL_TIME
            }\
            """,
            TEST_ENUMERATION_MAPPING_SOURCE_ID,
            """
            ###Mapping
            Mapping test::employeeTestMapping
            (
            
                test::EmployeeType: EnumerationMapping Foo
                {
                    CONTRACT:  ['FTC', 'FTO'],
                    FULL_TIME: 'FTE'
                }
            )
            """
    );

    private static final ImmutableMap<String, String> TEST_SOURCES_WITH_TYPO = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
            """
            Enum test::EmployeeType
            {
                CONTRCAT,
                FULL_TIME
            }\
            """);

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
        runtime.delete(TEST_ENUMERATION_MAPPING_SOURCE_ID);
        runtime.delete("modelCode.pure");
        runtime.delete("mappingCode.pure");
    }

    @Test
    public void testPureEnumerationMapping_EnumValueWithTypoShouldNotCompile() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            try
            {
                runtime.createInMemoryAndCompile(TEST_SOURCES_WITH_TYPO);
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                this.assertPureException(PureCompilationException.class,
                        "The enum value 'CONTRACT' can't be found in the enumeration test::EmployeeType",
                        TEST_ENUMERATION_MAPPING_SOURCE_ID, 7, 9, 7, 9, 7, 16, e);
            }

            runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testDeleteEnumeration() throws Exception
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_ENUM_MODEL_SOURCE_ID);
            try
            {
                runtime.compile();
                Assertions.fail("Expected compilation exception on iteration #" + i);
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "test::EmployeeType has not been defined!", TEST_ENUMERATION_MAPPING_SOURCE_ID, 5, 11, 5, 11, 5, 22, e);
            }

            runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testPureEnumerationMapping_UnloadMapping() throws Exception
    {
        runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUM_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_ENUM_MODEL_SOURCE_ID)));
        int size = repository.serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {

            runtime.createInMemoryAndCompile(Tuples.pair(TEST_ENUMERATION_MAPPING_SOURCE_ID, TEST_SOURCES.get(TEST_ENUMERATION_MAPPING_SOURCE_ID)));
            runtime.delete(TEST_ENUMERATION_MAPPING_SOURCE_ID);
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testDuplicateError() throws Exception
    {
        runtime.createInMemorySource(TEST_ENUMERATION_MAPPING_SOURCE_ID, """
                Enum OK {e_true,e_false}
                ###Mapping
                Mapping myMap1(
                    OK: EnumerationMapping Foo
                    {
                        e_true:  ['FTC', 'FTO'],
                        e_false: 'FTE'
                    }
                    OK: EnumerationMapping Foo
                    {
                        e_true:  ['FTC', 'FTO'],
                        e_false: 'FTE'
                    }
                )
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'Foo' in mapping myMap1", 9, 5, e);
        }
    }

    @Test
    public void testStabilityOnDeletionForSimpleEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : my::SourceEnum.B
                   }
                )
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForSimpleEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : my::SourceEnum.B
                   }
                )
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'B' can't be found in the enumeration my::SourceEnum", "mappingCode.pure", 9, 26)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnDeletionForComplexEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B, C
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : [SourceEnum.B, my::SourceEnum.C]
                   }
                )
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForComplexEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B, C
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : [SourceEnum.B, my::SourceEnum.C]
                   }
                )
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'C' can't be found in the enumeration my::SourceEnum", "mappingCode.pure", 9, 41)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnDeletionForHybridEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum1
                {
                   A, B, C, D
                }
                
                Enum my::SourceEnum2
                {
                   P, Q, R, S
                }
                
                Enum my::TargetEnum
                {
                   U, V, W, X, Y, Z
                }
                
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      U : SourceEnum2.P,
                      V : my::SourceEnum2.P,
                      W : [SourceEnum2.P, my::SourceEnum2.Q],
                      X : [my::SourceEnum2.P, SourceEnum2.Q, SourceEnum2.S],
                      Y : [SourceEnum2.R, SourceEnum2.S, SourceEnum2.Q],
                      Z : SourceEnum2.Q
                   }
                )\
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::SourceEnum1
                {
                   A, B, C, D
                }
                
                Enum my::TargetEnum
                {
                   U, V, W, X, Y, Z
                }
                
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("SourceEnum2 has not been defined!", "mappingCode.pure", 8, 11)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testStabilityOnUpdationForHybridEumToEnumMapping()
    {
        String modelCode = """
                ###Pure
                
                Enum my::SourceEnum1
                {
                   A, B, C, D
                }
                
                Enum my::SourceEnum2
                {
                   P, Q, R, S
                }
                
                Enum my::TargetEnum
                {
                   U, V, W, X, Y, Z
                }
                
                """;

        String mappingCode = """
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      U : my::SourceEnum2.P,
                      V : SourceEnum2.P,
                      W : [SourceEnum2.P, my::SourceEnum2.Q],
                      X : [my::SourceEnum2.P, SourceEnum2.Q],
                      Y : [SourceEnum2.R, SourceEnum2.Q, SourceEnum2.P],
                      Z : SourceEnum2.P
                   }
                )\
                """;

        String updatedModelCode = """
                ###Pure
                
                Enum my::SourceEnum1
                {
                   A, B, C
                }
                
                Enum my::SourceEnum2
                {
                   P, Q
                }
                
                Enum my::TargetEnum
                {
                   U, V, W, X, Y, Z
                }
                
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("modelCode.pure", modelCode)
                        .createInMemorySource("mappingCode.pure", mappingCode)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("modelCode.pure", updatedModelCode)
                        .compileWithExpectedCompileFailure("The enum value 'R' can't be found in the enumeration my::SourceEnum2", "mappingCode.pure", 12, 24)
                        .updateSource("modelCode.pure", modelCode)
                        .compile(),
                runtime,
                functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
