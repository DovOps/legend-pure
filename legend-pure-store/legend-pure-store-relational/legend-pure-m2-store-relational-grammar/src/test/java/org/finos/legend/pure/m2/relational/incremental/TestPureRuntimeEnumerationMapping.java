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
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestPureRuntimeEnumerationMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String TEST_ENUM_MODEL_SOURCE_ID = "testModel.pure";
    private static final String RELATIONAL_DB_SOURCE_ID = "testDb.pure";
    private static final String TEST_ENUMERATION_MAPPING_SOURCE_ID = "testMapping.pure";

    private static final String FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE = """
            ###Pure
            function test():Boolean[1]{assert(1 == employeeTestMapping.enumerationMappings->size(), |'');}\
            """;

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
            """
            Class Employee
            {
                id: Integer[1];
                name: String[1];
                dateOfHire: Date[1];
                type: EmployeeType[0..1];
            }
            
            Enum EmployeeType
            {
                CONTRACT,
                FULL_TIME
            }\
            """,
            RELATIONAL_DB_SOURCE_ID, """
                    ###Relational
                    
                    Database myDB
                    (
                        Table employeeTable
                        (
                            id INT,
                            name VARCHAR(200),
                            firmId INT,
                            doh DATE,
                            type VARCHAR(20)
                        )
                    )
                    """,
            TEST_ENUMERATION_MAPPING_SOURCE_ID,
            """
            ###Mapping
            
            Mapping employeeTestMapping
            (
            
                EmployeeType: EnumerationMapping Foo
                {
                /* comment */
                    CONTRACT:  ['FTC', 'FTO'],
                    FULL_TIME: 'FTE'
                }
               Employee: Relational
               {
                    scope([myDB]default.employeeTable)
                    (
                        id: id,
                        name: name,
                        dateOfHire: doh,
                        type : EnumerationMapping Foo : type
                    )
               }
            )
            """
    );

    @Test
    public void testDeleteAndReloadEachSource() throws Exception
    {
        this.testDeleteAndReloadEachSource(
                TEST_SOURCES, FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadEachSource(ImmutableMap<String, String> sources, String testFunctionSource)
    {

        for (Pair<String, String> source : sources.keyValuesView())
        {
            RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(sources)
                            .createInMemorySource("functionSourceId.pure", testFunctionSource)
                            .compile(),
                    new RuntimeTestScriptBuilder()
                            .deleteSource(source.getOne())
                            .compileWithExpectedCompileFailure(null, null, 0, 0)
                            .createInMemorySource(source.getOne(), source.getTwo())
                            .compile(),
                    runtime, functionExecution, Lists.fixedSize.of());

            //reset
            setUpRuntime();
        }
    }

    @Test
    public void testDeleteAndReloadSourcePairs() throws Exception
    {
        this.testDeleteAndReloadTwoSources(
                TEST_SOURCES, FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadTwoSources(ImmutableMap<String, String> sources,
                                              String testFunctionSource)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            List<Pair<String, String>> sourcesClone = sources.keyValuesView().toList();
            sourcesClone.remove(source);

            for (Pair<String, String> secondSource : sourcesClone)
            {
                RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(sources)
                                .createInMemorySource("functionSourceId.pure", testFunctionSource)
                                .compile(),
                        new RuntimeTestScriptBuilder()
                                .deleteSources(Lists.fixedSize.of(source.getOne(), secondSource.getOne()))
                                .compileWithExpectedCompileFailure(null, null, 0, 0)
                                .createInMemorySource(source.getOne(), source.getTwo())
                                .createInMemorySource(secondSource.getOne(), secondSource.getTwo())
                                .compile(),
                        runtime, functionExecution, Lists.fixedSize.of());

                //reset so that the next iteration has a clean environment
                setUpRuntime();
            }
        }
    }
}

