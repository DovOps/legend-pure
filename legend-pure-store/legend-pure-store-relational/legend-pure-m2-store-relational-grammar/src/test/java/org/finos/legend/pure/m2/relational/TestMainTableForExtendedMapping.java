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

import org.eclipse.collections.api.block.predicate.Predicate2;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMainTableForExtendedMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final Predicate2<SetImplementation, String> CLASS_MAPPING_BY_ID = new Predicate2<SetImplementation, String>()
    {
        @Override
        public boolean accept(SetImplementation classMapping, String id)
        {
            return classMapping._id().equals(id);
        }
    };

    private static final String MAIN_SOURCE_ID = "main.pure";
    private static final String TEST_SOURCE_ID = "test.pure";

    private static final String MAIN_SOURCE_CODE = """
            ###Pure
            import test::*;
            
            Class test::Vehicle
            {
               vehicleId : Integer[1];
               vehicleName : String[1];
               vehicleType : String[1];
            }
            
            ###Relational
            Database test::VehicleDatabase
            (
               Table VehicleTable(vehicleId INT PRIMARY KEY, vehicleName VARCHAR(20), vehicleType VARCHAR(20))
            )
            
            ###Mapping
            import test::*;
            
            Mapping test::VehicleMapping
            (  \s
               Vehicle: Relational
               {
                  vehicleId   : [VehicleDatabase]VehicleTable.vehicleId,
                  vehicleName : [VehicleDatabase]VehicleTable.vehicleName,
                  vehicleType : [VehicleDatabase]VehicleTable.vehicleType
               }
            )
            """;

    @Test
    public void testStableMainTableForSimpleMappingExtends()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                   }
                )
                """;
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForSimpleExtendedMapping()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                   }
                )
                """;

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testStableMainTableForNestedMappingExtends()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                Class test::Bicycle extends RoadVehicle
                {
                  \s
                }
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                   }
                )
                
                Mapping test::BicycleMapping
                (  \s
                   include RoadVehicleMapping
                  \s
                   Bicycle extends [test_RoadVehicle]: Relational
                   {
                   }
                )
                """;
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForNestedExtendedMapping()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                Class test::Bicycle extends RoadVehicle
                {
                  \s
                }
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                   }
                )
                
                Mapping test::BicycleMapping
                (  \s
                   include RoadVehicleMapping
                  \s
                   Bicycle extends [test_RoadVehicle]: Relational
                   {
                   }
                )
                """;

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Mapping bicycleMapping = (Mapping)this.runtime.getCoreInstance("test::BicycleMapping");
        RootRelationalInstanceSetImplementation bicycleSetImplementation = (RootRelationalInstanceSetImplementation)bicycleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Bicycle").getFirst();

        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());

        Assertions.assertEquals(roadVehicleSetImplementation._mainTableAlias()._relationalElement(), bicycleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertEquals(roadVehicleSetImplementation._mainTableAlias()._database(), bicycleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testUserDefinedMainTableNotAllowedForExtendedMapping()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                       ~mainTable[MainDatabase]VehicleTable
                   }
                )
                """;

        this.verifyInValidMainTableForExtendedMapping("Cannot specify main table explicitly for extended mapping [test_RoadVehicle]", testSourceCode, 16, 4);
    }

    @Test
    public void testStableMainTableForSimpleExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Relational
                Database test::RoadVehicleDatabase
                (
                   include test::VehicleDatabase
                )
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                      vehicleName : concat('roadVehicle_', [RoadVehicleDatabase]VehicleTable.vehicleName)
                   }
                )
                """;
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForSimpleExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Relational
                Database test::RoadVehicleDatabase
                (
                   include test::VehicleDatabase
                )
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                      vehicleName : concat('roadVehicle_', [RoadVehicleDatabase]VehicleTable.vehicleName)
                   }
                )
                """;

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertNotEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testStableMainTableForNestedExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                Class test::Bicycle extends RoadVehicle
                {
                  \s
                }
                
                ###Relational
                Database test::RoadVehicleDatabase
                (
                   include test::VehicleDatabase
                )
                
                ###Relational
                Database test::BicycleDatabase
                (
                   include test::RoadVehicleDatabase
                )
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                      vehicleName : concat('roadVehicle_', [RoadVehicleDatabase]VehicleTable.vehicleName)
                   }
                )
                
                Mapping test::BicycleMapping
                (  \s
                   include RoadVehicleMapping[RoadVehicleDatabase->BicycleDatabase]
                  \s
                   Bicycle extends [test_RoadVehicle]: Relational
                   {
                      vehicleType : [BicycleDatabase]VehicleTable.vehicleType
                   }
                )
                """;
        this.verifyValidMainTableForExtendedMapping(testSourceCode);
    }

    @Test
    public void testValidMainTableForNestedExtendedMappingWithStoreSubstitution()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                Class test::Bicycle extends RoadVehicle
                {
                  \s
                }
                
                ###Relational
                Database test::RoadVehicleDatabase
                (
                   include test::VehicleDatabase
                )
                
                ###Relational
                Database test::BicycleDatabase
                (
                   include test::RoadVehicleDatabase
                )
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping[VehicleDatabase->RoadVehicleDatabase]
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                      vehicleName : concat('roadVehicle_', [RoadVehicleDatabase]VehicleTable.vehicleName)
                   }
                )
                
                Mapping test::BicycleMapping
                (  \s
                   include RoadVehicleMapping[RoadVehicleDatabase->BicycleDatabase]
                  \s
                   Bicycle extends [test_RoadVehicle]: Relational
                   {
                      vehicleType : [BicycleDatabase]VehicleTable.vehicleType
                   }
                )
                """;

        this.runtime.createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE);
        this.runtime.createInMemorySource(TEST_SOURCE_ID, testSourceCode);
        this.runtime.compile();

        Mapping vehicleMapping = (Mapping)this.runtime.getCoreInstance("test::VehicleMapping");
        RootRelationalInstanceSetImplementation vehicleSetImplementation = (RootRelationalInstanceSetImplementation)vehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Vehicle").getFirst();

        Mapping roadVehicleMapping = (Mapping)this.runtime.getCoreInstance("test::RoadVehicleMapping");
        RootRelationalInstanceSetImplementation roadVehicleSetImplementation = (RootRelationalInstanceSetImplementation)roadVehicleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_RoadVehicle").getFirst();

        Mapping bicycleMapping = (Mapping)this.runtime.getCoreInstance("test::BicycleMapping");
        RootRelationalInstanceSetImplementation bicycleSetImplementation = (RootRelationalInstanceSetImplementation)bicycleMapping._classMappings().selectWith(CLASS_MAPPING_BY_ID, "test_Bicycle").getFirst();

        Assertions.assertEquals(vehicleSetImplementation._mainTableAlias()._relationalElement(), roadVehicleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertNotEquals(vehicleSetImplementation._mainTableAlias()._database(), roadVehicleSetImplementation._mainTableAlias()._database());

        Assertions.assertEquals(roadVehicleSetImplementation._mainTableAlias()._relationalElement(), bicycleSetImplementation._mainTableAlias()._relationalElement());
        Assertions.assertNotEquals(roadVehicleSetImplementation._mainTableAlias()._database(), bicycleSetImplementation._mainTableAlias()._database());
    }

    @Test
    public void testInvalidMainTableForExtendedMappingWithoutStoreSubstitution()
    {
        String testSourceCode = """
                ###Pure
                import test::*;
                
                Class test::RoadVehicle extends Vehicle
                {
                  \s
                }
                
                ###Relational
                Database test::RoadVehicleDatabase
                (
                   include test::VehicleDatabase
                )
                
                ###Mapping
                import test::*;
                
                Mapping test::RoadVehicleMapping
                (  \s
                   include VehicleMapping
                  \s
                   RoadVehicle extends [test_Vehicle]: Relational
                   {
                      vehicleName : concat('roadVehicle_', [RoadVehicleDatabase]VehicleTable.vehicleName)
                   }
                )
                """;
        this.verifyInValidMainTableForExtendedMapping("Can't find the main table for class 'RoadVehicle'. Inconsistent database definitions for the mapping",
                testSourceCode, 22, 4);
    }

    private void verifyValidMainTableForExtendedMapping(String testSourceCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
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

    private void verifyInValidMainTableForExtendedMapping(String errorMessage, String testSourceCode, int line, int column)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAIN_SOURCE_ID, MAIN_SOURCE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(TEST_SOURCE_ID, testSourceCode)
                        .compileWithExpectedCompileFailure(errorMessage, TEST_SOURCE_ID, line, column)
                        .deleteSource(TEST_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
