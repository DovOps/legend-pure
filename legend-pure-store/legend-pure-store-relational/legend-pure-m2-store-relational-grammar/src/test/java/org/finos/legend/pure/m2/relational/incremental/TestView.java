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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestView extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String GroupByModel =
            """
            ###Pure
            Class AccountPnl
            {
               pnl:Float[1];
            }\
            """;

    private static final String GroupByStoreTemplate =
            """
            ###Relational
            Database db(
               Table orderTable(ID INT PRIMARY KEY, accountID INT)
               Table orderPnlTable( ORDER_ID INT PRIMARY KEY, pnl FLOAT)
               Join OrderPnlTable_Order(orderPnlTable.ORDER_ID = orderTable.ID)
            
               View accountOrderPnlView
               (
                  ~groupBy (orderTable.accountID)
                  accountId : orderTable.accountID PRIMARY KEY,
                  %s : sum(@OrderPnlTable_Order | orderPnlTable.pnl)
               )
            )\
            """;

    private static final String GroupByMappingTemplate =
            """
            ###Mapping
            Mapping testMapping
            (
               AccountPnl : Relational \s
               {
                  pnl : [db]accountOrderPnlView.%s\s
               }
            )\
            """;

    @BeforeEach
    @Override
    public void _setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.api.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(GenericCodeRepository.build("test", "((test)|(meta))(::.*)?", "platform"));
        return repositories;
    }

    @Test
    public void testGroupByIncrementalSyntaticStoreChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = GroupByStoreTemplate.formatted(viewDynaColName);
        String groupByMapping = GroupByMappingTemplate.formatted(viewDynaColName);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("store.pure", groupByStore + " ")
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testGroupByIncrementalModelChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = GroupByStoreTemplate.formatted(viewDynaColName);
        String groupByMapping = GroupByMappingTemplate.formatted(viewDynaColName);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("model.pure", GroupByModel + "\n\n\n")
                        .compile()
                        .updateSource("model.pure", GroupByModel)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testGroupByIncrementalStoreAndMappingChange()
    {
        String viewDynaColName = "orderPnl";
        String groupByStore = GroupByStoreTemplate.formatted(viewDynaColName);
        String groupByMapping = GroupByMappingTemplate.formatted(viewDynaColName);
        String viewDynaColNameUpdated = "orderPnlUpdated";
        String groupByStoreUpdated = GroupByStoreTemplate.formatted(viewDynaColNameUpdated);
        String groupByMappingUpdated = GroupByMappingTemplate.formatted(viewDynaColNameUpdated);
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                                Maps.mutable.with("store.pure", groupByStore, "model.pure", GroupByModel, "mapping.pure", groupByMapping))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("store.pure", groupByStoreUpdated).updateSource("mapping.pure", groupByMappingUpdated)
                        .compile()
                        .updateSource("store.pure", groupByStore).updateSource("mapping.pure", groupByMapping)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.empty());
    }

    @Test
    public void testViewInSchemaReferencingTableInIncludedDB()
    {
        String includedDB1Source = "/test/includedDB1.pure";
        String includedDB1 = """
                ###Relational
                Database test::store::IncludedDB1
                (
                    Schema S1
                    (
                        Table T1 (ID INT PRIMARY KEY, NAME VARCHAR(200), OTHER_ID INT)
                    )
                )
                """;
        String mainDBSource = "/test/mainDB.pure";
        String mainDB = """
                ###Relational
                Database test::store::MainDB
                (
                    include test::store::IncludedDB1
                
                    Schema S1
                    (
                        View V1
                        (
                            id: T1.ID PRIMARY KEY,
                            name: T1.NAME
                        )
                    )
                )
                """;
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource(includedDB1Source, includedDB1)
                        .createInMemorySource(mainDBSource, mainDB)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(includedDB1Source)
                        .createInMemorySource(includedDB1Source, includedDB1)
                        .compile(),
                runtime, functionExecution, Lists.immutable.empty());
    }
}