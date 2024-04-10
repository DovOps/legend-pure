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

package org.finos.legend.pure.m2.relational.incremental;

import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

public class TestIncludedMappingOwnerUnloaderUnbind extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MAIN_SOURCE_ID = "main.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MAIN_SOURCE_CODE = """
            ###Pure
            import test::*;
            
            Class test::Vehicle
            {
               vehicleId : Integer[1];
               vehicleName : String[1];
            }
            
            Class test::RoadVehicle extends Vehicle
            {
              \s
            }
            
            ###Relational
            Database test::MainDatabase
            (
               Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20))
               Filter VehicleFilter(VehicleTable.vehicleId = 1)
            )
            
            ###Mapping
            import test::*;
            
            Mapping test::RoadVehicleMapping
            (  \s
               include MainMapping
              \s
               RoadVehicle extends [test_Vehicle]: Relational
               {
                 \s
               }
            )
            """;

    private static final String TEST_V1_SOURCE_CODE = """
            ###Mapping
            import test::*;
            
            Mapping test::MainMapping
            (  \s
               Vehicle: Relational
               {
                  vehicleId : [MainDatabase]VehicleTable.vehicleId,
                  vehicleName : [MainDatabase]VehicleTable.vehicleName
               }
            )
            """;

    private static final String TEST_V2_SOURCE_CODE = """
            ###Mapping
            import test::*;
            
            Mapping test::MainMapping
            (  \s
               Vehicle[newId]: Relational
               {
                  vehicleId : [MainDatabase]VehicleTable.vehicleId,
                  vehicleName : [MainDatabase]VehicleTable.vehicleName
               }
            )
            """;

    @Test
    public void testIncludedMappingUnloaderUnbind()
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .createInMemorySource(TEST_SOURCE_ID, TEST_V1_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource(TEST_SOURCE_ID, TEST_V2_SOURCE_CODE)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [test_RoadVehicle]", MAIN_SOURCE_ID, 29, 4)
                        .updateSource(TEST_SOURCE_ID, TEST_V1_SOURCE_CODE)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
