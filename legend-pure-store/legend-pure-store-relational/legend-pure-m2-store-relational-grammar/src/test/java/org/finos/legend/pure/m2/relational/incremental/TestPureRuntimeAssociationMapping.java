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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeAssociationMapping extends AbstractPureRelationalTestWithCoreCompiled
{

    private static final String INITIAL_DATA = """
            import other::*;
            
            Class other::Person
            {
                name:String[1];
            }
            Class other::Firm
            {
                legalName:String[1];
            }
            """;

    private static final String ASSOCIATION = """
            import other::*;
            Association other::Firm_Person
            {
                firm:Firm[1];
                employees:Person[1];
            }
            """;

    private static final String STORE =
            """
            ###Relational
            Database mapping::db(
               Table employeeFirmDenormTable
               (
                id INT PRIMARY KEY,
                name VARCHAR(200),
                firmId INT,
                legalName VARCHAR(200)
               )
               Join firmJoin(employeeFirmDenormTable.firmId = {target}.firmId)
            )
            """;

    private static final String INITIAL_MAPPING =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::subMapping1
            (
                Person[per1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name
                }
                Firm[fir1]: Relational
                {
                    legalName : [db]employeeFirmDenormTable.legalName
                }
            
            )
            """;

    private static final String MAPPING_WITH_ASSOCIATION =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::subMapping1
            (
                Person[per1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name
                }
                Firm[fir1]: Relational
                {
                    legalName : [db]employeeFirmDenormTable.legalName
                }
            
                Firm_Person: Relational
                {
                    AssociationMapping
                    (
                       employees[fir1,per1] : [db]@firmJoin,
                       firm[per1,fir1] : [db]@firmJoin
                    )
                }
            )
            """;


    private static final String MAPPING1 =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::subMapping1
            (
                Person[per1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name
                }
            )
            """;

    private static final String MAPPING2 =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::subMapping2
            (
                Firm[fir1]: Relational
                {
                    legalName : [db]employeeFirmDenormTable.legalName
                }
            )
            """;

    private static final String MAPPING3 =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::subMapping3
            (
                include mappingPackage::subMapping1
                include mappingPackage::subMapping2
                Firm_Person: Relational
                {
                    AssociationMapping
                    (
                       employees[fir1,per1] : [db]@firmJoin,
                       firm[per1,fir1] : [db]@firmJoin
                    )
                }
            )
            """;

    @Test
    public void testCreateAndDeleteAssociationMappingSameFile() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                        "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_WITH_ASSOCIATION)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testDeleteAssociation() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                        "source3.pure", STORE, "source4.pure", MAPPING_WITH_ASSOCIATION))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source2.pure")
                        .compileWithExpectedCompileFailure("Firm_Person has not been defined!", "source4.pure", 15, 5)
                        .createInMemorySource("source2.pure", ASSOCIATION)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteAssociationMappingWithIncludes() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                        "source3.pure", STORE, "source4.pure", MAPPING1).withKeyValue("source5.pure", MAPPING2))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("mapping3.pure", MAPPING3)
                        .compile()
                        .deleteSource("mapping3.pure")
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testDeleteAndRecreateStore() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", ASSOCIATION,
                                        "source3.pure", STORE, "source4.pure", MAPPING1).withKeyValue("source5.pure", MAPPING2).withKeyValue("mapping3.pure", MAPPING3))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure")
                        .compileWithExpectedCompileFailure(null, null, 0, 0)
                        .createInMemorySource("source3.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
