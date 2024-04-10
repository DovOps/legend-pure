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

public class TestPureRuntimeInlineEmbeddedMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String INITIAL_DATA = """
            import other::*;
            Class other::Person
            {
                name:String[1];
                firm:Firm[1];
            }
            Class other::Address
            {
                name:String[1];
            }
            Class other::Firm
            {
                legalName:String[1];
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
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    ) Inline [firm1]\s
                }
            )
            """;

    private static final String MAPPING_INVALID_INLINE =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    ) Inline [firm2]\s
                }
            )
            """;

    private static final String MAPPING_DELETED1_INLINE =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name
                }
            )
            """;

    private static final String MAPPING_DELETED_INLINE =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm () Inline[firm1]\s
                }
            )
            """;

    private static final String MAPPING_EMPTY_INLINE =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    ) Inline []\s
                }
            )
            """;

    private static final String MAPPING_REMOVE_INLINE_KEYWORD =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firm1]: Relational
                {
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    )
                }
            )
            """;


    private static final String MAPPING_CHANGE_INLINE_SETID =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Firm[firmNew]: Relational
                {
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    ) Inline [firm1]\s
                }
            )
            """;

    private static final String INITIAL_MAPPING_CHANGE_TO_EMBEDDED =
            """
###Mapping
import other::*;
import mapping::*;
Mapping mappingPackage::myMapping
(
    Firm[firm1]: Relational
    {
       legalName : [db]employeeFirmDenormTable.name
    }
    Person[alias1]: Relational
    {
        name : [db]employeeFirmDenormTable.name,
        firm(
            legalName : [db]employeeFirmDenormTable.legalName\
        ) \s
    }
)
""";

    private static final String MAPPING_INVALID_TARGET_TYPE =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Address[address1]: Relational
                {
                   name : [db]employeeFirmDenormTable.address1
                }
                Firm[firm1]: Relational
                {
                   legalName : [db]employeeFirmDenormTable.name
                }
                Person[alias1]: Relational
                {
                    name : [db]employeeFirmDenormTable.name,
                    firm(
                    ) Inline [address1]\s
                }
            )
            """;


    @Test
    public void testCreateAndDeleteInlineEmbeddedMappingFile()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource("source4.pure", INITIAL_MAPPING)
                        .compile()
                        .deleteSource("source4.pure")
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCreateAndDeleteInlineEmbeddedMappingSameFile()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_DELETED1_INLINE)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeRootIDForInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_CHANGE_INLINE_SETID)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm1 does not exists.", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeInlineTargetMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_INVALID_INLINE)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm2 does not exists.", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testDeleteInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_DELETED_INLINE)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' property, inline set id firm1 does not exists.", "source4.pure", 9, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testEmptyInlineMapping()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_EMPTY_INLINE)
                        .compileWithExpectedParserFailure("expected: a valid identifier text; found: ']'", "source4.pure", 14, 19)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testRemoveInlineKeyword()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_REMOVE_INLINE_KEYWORD)
                        .compileWithExpectedCompileFailure("Invalid Inline mapping found: 'firm' mapping has not inline set defined, please use: firm() Inline[setid].", "source4.pure", 13, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeInlineEmbeddedMappingSameFileToNormalEmbedded()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", INITIAL_MAPPING_CHANGE_TO_EMBEDDED)
                        .compile()
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testChangeToInvalidTarget()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING_INVALID_TARGET_TYPE)
                        .compileWithExpectedCompileFailure("Mapping Error! The inlineSetImplementationId 'address1' is implementing the class 'Address' which is not a subType of 'Firm' (return type of the mapped property 'firm')", "source4.pure", 17, 9)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }
}
