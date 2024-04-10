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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestRelationDatabaseAccessor extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private MutableMap<String, String> sources = Maps.mutable.empty();

    @BeforeEach
    public void setUp()
    {
        this.sources.put(MODEL_SOURCE_ID,
                """
                import test::*;
                
                function myFunc():Any[1]
                {
                   #>{test::TestDB.personTb}#
                }\
                """);
        this.sources.put(STORE_SOURCE_ID,
                """
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name VARCHAR(200), firmId INT)
                )\
                """);
    }

    @Test
    public void testCompileAndDeleteModel()
    {
        compileSources(STORE_SOURCE_ID);
        int expectedSize = this.repository.serialize().length;
        ImmutableSet<CoreInstance> expectedInstances = Sets.immutable.withAll(this.context.getAllInstances());
        for (int i = 0; i < TEST_COUNT; i++)
        {
            compileSource(MODEL_SOURCE_ID);
            Assertions.assertNotNull(this.runtime.getCoreInstance("myFunc__Any_1_"));
            deleteSource(MODEL_SOURCE_ID);
            Assertions.assertNull(this.runtime.getCoreInstance("myFunc__Any_1_"));
            Assertions.assertEquals(expectedInstances, this.context.getAllInstances());
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testCompileAndDeleteStore()
    {
        String INITIAL_DATA = """
                import test::*;
                
                function myFunc():Any[1]
                {\s
                   #>{test::TestDB.personTb}#->filter(t|$t.name == 'ee')
                }\
                """;

        String STORE = """
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name VARCHAR(200), firmId INT)
                )\
                """;

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource("source3.pure")
                        .compileWithExpectedCompileFailure("The store 'test::TestDB' can't be found", "source1.pure", 5, 5)
                        .createInMemorySource("source3.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }


    @Test
    public void testCompileAndDeleteMutateStore()
    {
        String INITIAL_DATA = """
                import test::*;
                
                function myFunc():Any[1]
                {\s
                   #>{test::TestDB.personTb}#\
                       ->filter(t|$t.name == 'ee')\
                }\
                """;

        String STORE = """
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name VARCHAR(200), firmId INT)
                )\
                """;

        String STORE2 = """
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name22 VARCHAR(200), firmId INT)
                )\
                """;

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", STORE2)
                        .compileWithExpectedCompileFailure("The system can't find the column name in the Relation (name22:String, firmId:Integer)", "source1.pure", 5, 51)
                        .updateSource("source3.pure", STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCompileAndDeleteRenameTable()
    {
        String INITIAL_DATA = """
                import test::*;
                
                function myFunc():Any[1]
                {\s
                   #>{test::mainDb.PersonTable}#\
                }\
                """;

        String STORE1 = """
                ###Relational
                Database test::incDb
                (\s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                )
                """;

        String STORE1_CHANGED = """
                ###Relational
                Database test::incDb
                (\s
                   Table PersonTable_Renamed(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                )
                """;

        String STORE2 = """
                ###Relational
                Database test::mainDb
                (\s
                   include test::incDb
                   Table FirmTable(legalName VARCHAR(200), firmId INTEGER)
                )
                """;

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA, "source2.pure", STORE1, "source3.pure", STORE2))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source2.pure", STORE1_CHANGED)
                        .compileWithExpectedCompileFailure("The table 'PersonTable' can't be found in the schema 'default' in the database 'test::mainDb'", "source1.pure", 5, 5)
                        .updateSource("source2.pure", STORE1)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testCompileAndDeleteFull()
    {
        String INITIAL_DATA = """
                import test::*;
                
                function myFunc():Any[1]
                {\s
                   #>{test::TestDB.personTb}#\
                       ->filter(t|$t.name == 'ee')\
                }\
                """;

        String STORE = """
                
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name VARCHAR(200), firmId INT)
                )\
                """;

        String STORE2 = """
                
                ###Relational
                Database test::TestDB
                (
                   Table personTb(name22 VARCHAR(200), firmId INT)
                )\
                """;

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                org.eclipse.collections.api.factory.Maps.mutable.with("source1.pure", INITIAL_DATA + STORE))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", INITIAL_DATA + STORE2)
                        .compileWithExpectedCompileFailure("The system can't find the column name in the Relation (name22:String, firmId:Integer)", "source1.pure", 5, 51)
                        .updateSource("source1.pure", INITIAL_DATA + STORE)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    private void compileSources(String... sourceIds)
    {
        for (String sourceId : sourceIds)
        {
            this.runtime.createInMemorySource(sourceId, this.sources.get(sourceId));
        }
        this.runtime.compile();
    }

    private void compileSource(String sourceId)
    {
        compileSources(sourceId);
    }

    private void deleteSource(String sourceId)
    {
        this.runtime.delete(sourceId);
        this.runtime.compile();
    }
}
