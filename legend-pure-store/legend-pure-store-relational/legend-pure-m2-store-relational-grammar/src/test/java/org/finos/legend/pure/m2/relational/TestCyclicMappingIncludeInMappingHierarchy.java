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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

public class TestCyclicMappingIncludeInMappingHierarchy extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";

    private static final String MODEL_SOURCE_CODE = """
            ###Pure
            
            Class test::A
            {
               id : Integer[1];
            }
            
            Class test::C extends test::A
            {
            }
            """;

    private static final String STORE_SOURCE_CODE = """
            ###Relational
            
            Database test::ADatabase
            (
               Table ATable(id INT)
            )
            """;

    @Test
    public void testAcyclicMappingIncludeAllowedInMappingHierarchy()
    {
        String mappingSourceCode = """
                ###Mapping
                
                Mapping test::AMapping
                (
                   test::A : Relational
                   {
                      id : [test::ADatabase]ATable.id
                   }
                )
                
                Mapping test::BMapping
                (
                   include test::AMapping
                )
                
                Mapping test::CMapping
                (
                   include test::BMapping
                   test::C extends [test_A] : Relational
                   {\s
                   }
                )
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingSourceCode)
                        .compile()
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    @Test
    public void testCyclicMappingIncludeNotAllowedInMappingHierarchy()
    {
        String mappingSourceCode = """
                ###Mapping
                
                Mapping test::AMapping
                (     \s
                   test::A : Relational
                   {
                      id : [test::ADatabase]ATable.id
                   }
                )
                
                Mapping test::BMapping
                (     \s
                   include test::CMapping
                   include test::AMapping
                )
                
                Mapping test::CMapping
                (     \s
                   include test::BMapping
                   test::C extends [test_A] : Relational
                   {\s
                   }
                )\
                """;

        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingSourceCode)
                        .compileWithExpectedCompileFailure("Cyclic mapping include for mapping [test::CMapping] in mapping hierarchy", MAPPING_SOURCE_ID, 17, 15)
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
