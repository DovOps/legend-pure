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

public class TestPureRuntimeExtendMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String INITIAL_DATA = """
            import other::*;
            Class other::Person
            {
                name:String[1];
                otherInfo:String[1];
            }
            """;


    private static final String STORE =
            """
            ###Relational
            Database mapping::db(
               Table employeeTable
               (
                id INT PRIMARY KEY,
                name VARCHAR(200),
                firmId INT,
                other VARCHAR(200),
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
                *Person[person1]: Relational
                {
                   otherInfo: [db]employeeTable.other
                }
                Person[alias1] extends [person1]: Relational
                {
                    name : [db]employeeTable.name
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
                *Person[person1]: Relational
                {
                   otherInfo: [db]employeeTable.other
                }
                Person[alias1] extends [person2] : Relational
                {
                    name : [db]employeeTable.name
                }
            )
            """;

    private static final String CHANGE_SUPER__MAPPING =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                *Person[person2]: Relational
                {
                   otherInfo: [db]employeeTable.other
                }
                Person[alias1] extends [person1]: Relational
                {
                    name : [db]employeeTable.name
                }
            )
            """;
    private static final String DELETE_SUPER__MAPPING =
            """
            ###Mapping
            import other::*;
            import mapping::*;
            Mapping mappingPackage::myMapping
            (
                Person[alias1] extends [person1]: Relational
                {
                    name : [db]employeeTable.name
                }
            )
            """;

    @Test
    public void testChangeMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING1)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 10, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testChangeExtendMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", CHANGE_SUPER__MAPPING)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 10, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testDeleteExtendMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", DELETE_SUPER__MAPPING)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 6, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
