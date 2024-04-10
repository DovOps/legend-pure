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

public class TestMappingInheritanceValidOnlyForClassMappings extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MODEL_SOURCE_CODE = """
            ###Pure
            import test::*;
            
            Class test::Person
            {
               personId : Integer[1];
               personName : String[1];
               vehicleId : Integer[1];
            }
            
            Class test::Vehicle
            {
               vehicleId : Integer[1];
               vehicleName : String[1];
            }
            
            Association test::PersonVehicle
            {
               person : Person[1];
               vehicles : Vehicle[*];
            }
            """;

    private static final String STORE_SOURCE_CODE = """
            ###Relational
            Database test::MainDatabase
            (
               Table PersonTable(personId INT PRIMARY KEY, personName VARCHAR(20), vehicleId INT)
               Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20))
               Join PersonVehicle(PersonTable.vehicleId = VehicleTable.vehicleId)
            )
            """;

    private static final String MAPPING_SOURCE_CODE = """
            ###Mapping
            import test::*;
            
            Mapping test::MainMapping
            ( \s
               Person: Relational
               {
                  scope([MainDatabase]PersonTable)
                  (
                    personId: personId,
                    personName: personName,
                    vehicleId: vehicleId
                  )
               }
              \s
               Vehicle: Relational
               {
                  scope([MainDatabase]VehicleTable)
                  (
                    vehicleId: vehicleId,
                    vehicleName: vehicleName
                  )
               }
            )
            """;

    @Test
    public void testMappingInheritanceValidForClassMapping()
    {
        String testSourceCode = """
                ###Mapping
                import test::*;
                
                Mapping test::TestMapping
                (
                   include test::MainMapping
                  \s
                   Person[person1] extends [test_Person] : Relational
                   {
                     \s
                   }
                )
                """;
        this.verifyValidMappingInheritance(testSourceCode);
    }

    @Test
    public void testMappingInheritanceInValidForAssociationMapping()
    {
        String testSourceCode = """
                ###Mapping
                import test::*;
                
                Mapping test::TestMapping
                (
                   include test::MainMapping
                  \s
                   PersonVehicle extends [test_Person] : Relational
                   {
                      AssociationMapping
                      (
                         person: [MainDatabase]@PersonVehicle,
                         vehicles: [MainDatabase]@PersonVehicle
                      )
                   }
                )
                """;
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Association Mappings.", 10, 7);
    }

    @Test
    public void testMappingInheritanceInValidForOperationMapping()
    {
        String testSourceCode = """
                ###Mapping
                import test::*;
                
                Mapping test::TestMapping
                (
                   include test::MainMapping
                  \s
                   *Person extends [test_Person] : Operation
                   {
                      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(test_Person, test_Firm)
                   }
                )
                """;
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Operation Mappings.", 8, 36);
    }

    @Test
    public void testMappingInheritanceInValidForModelToModelMapping()
    {
        String testSourceCode = """
                ###Mapping
                import test::*;
                
                Mapping test::TestMapping
                (
                   include MainMapping
                  \s
                   Person extends [test_Firm] : Pure
                   {
                      ~src Firm
                      personId : $src.firmId,
                      personName : $src.firmName,
                      firmId : $src.firmId,
                   }
                )
                """;
        this.verifyInValidMappingInheritance(testSourceCode, "Mapping Inheritance feature is applicable only for Class Mappings, not applicable for Model to Model Pure Mappings.", 8, 33);
    }

    private void verifyValidMappingInheritance(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .createInMemorySource(MAPPING_SOURCE_ID, MAPPING_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compile()
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    private void verifyInValidMappingInheritance(String testSourceCode, String errorMessage, int line, int column)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_SOURCE_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_SOURCE_CODE)
                        .createInMemorySource(MAPPING_SOURCE_ID, MAPPING_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compileWithExpectedParserFailure(errorMessage, TEST_SOURCE_ID, line, column)
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
