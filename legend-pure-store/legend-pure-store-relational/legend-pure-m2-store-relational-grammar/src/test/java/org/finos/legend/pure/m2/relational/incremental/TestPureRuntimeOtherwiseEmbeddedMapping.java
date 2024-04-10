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

public class TestPureRuntimeOtherwiseEmbeddedMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String INITIAL_DATA = """
            import other::*;
            Class other::Person
            {
                name:String[1];
                firm:Firm[1];
            }
            Class other::Firm
            {
                legalName:String[1];
                otherInformation:String[1];
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
                legalName VARCHAR(200),
                address1 VARCHAR(200),
                postcode VARCHAR(10)
               )
               Table FirmInfoTable
               (
                id INT PRIMARY KEY,
                name VARCHAR(200),
                other VARCHAR(200)
               )
               Join PersonFirmJoin(employeeFirmDenormTable.firmId = FirmInfoTable.id)
            )
            """;

    private static final String STORE_NO_JOIN =
            """
            ###Relational
            Database mapping::db(
               Table employeeFirmDenormTable
               (
                id INT PRIMARY KEY,
                name VARCHAR(200),
                firmId INT,
                legalName VARCHAR(200),
                address1 VARCHAR(200),
                postcode VARCHAR(10)
               )
               Table FirmInfoTable
               (
                id INT PRIMARY KEY,
                name VARCHAR(200),
                other VARCHAR(200)
               )
            )
            """;

    private static final String INITIAL_MAPPING =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]FirmInfoTable.name ,
                   otherInformation: [db]FirmInfoTable.other
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                        ~primaryKey ([db]employeeFirmDenormTable.legalName)
                        legalName : [db]employeeFirmDenormTable.legalName
                    ) Otherwise ( [firm1]:[db]@PersonFirmJoin)\s
                }
            )
            """;

    private static final String MAPPING_WITH_JOIN =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]FirmInfoTable.name ,
                   otherInformation: [db]FirmInfoTable.other
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm[alias1,firm1]:[db]@PersonFirmJoin\s
                }
            )
            """;

    private static final String MAPPING1 =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]FirmInfoTable.name ,
                   otherInformation: [db]FirmInfoTable.other
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                        ~primaryKey ([db]employeeFirmDenormTable.legalName)
                        legalName : [db]employeeFirmDenormTable.legalName
                    ) Otherwise ( [firm2]:[db]@PersonFirmJoin)\s
                }
            )
            """;


    private static final String MAPPING2 =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]FirmInfoTable.name ,
                   otherInformation: [db]FirmInfoTable.other
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                        ~primaryKey ([db]employeeFirmDenormTable.legalName)
                        legalName : [db]employeeFirmDenormTable.legalName
                    ) Otherwise ( [firm2]:[db]employeeFirmDenormTable.legalName)\s
                }
            )
            """;


    @Test
    public void testCreateAndDeleteOtherwiseEmbeddedMappingSameFile()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_WITH_JOIN)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeOtherwiseTargetMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING1)
                        .compileWithExpectedCompileFailure("Invalid Otherwise mapping found for 'firm' property, targetId firm2 does not exists.", "source4.pure", 14, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testDeleteAndRecreateStore()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", STORE, "source3.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", STORE_NO_JOIN)
                        .compileWithExpectedCompileFailure(null, null, 0, 0)
                        .updateSource("source2.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testChangeOtherwisePropertyMappingFromJoinToOther()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", STORE, "source3.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", MAPPING2)
                        .compileWithExpectedParserFailure("expected: '@' found: 'employeeFirmDenormTable'", "source3.pure", 17, 35)
                        .updateSource("source3.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }
}
