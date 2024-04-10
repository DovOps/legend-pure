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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestViewProcessing extends AbstractPureRelationalTestWithCoreCompiled
{

    private RelationalGraphWalker graphWalker;

    @BeforeEach
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }

    private static String dbWithLongJoinChain = """
            ###Relational
            Database with::view::Db
            (
               Schema pureTestSchema
               (\s
                  Table table1( id Integer PRIMARY KEY, t2id Integer)
                  Table table2( id Integer PRIMARY KEY, t3id Integer)
                  Table table3( id Integer PRIMARY KEY, t4id Integer)
                  Table table4( id Integer PRIMARY KEY, t5id Integer)
                  Table table5( id Integer PRIMARY KEY, t6id Integer)
                  Table table6( id Integer PRIMARY KEY, t7id Integer)
                  Table table7( id Integer PRIMARY KEY, t8id Integer)
                  Table table8( id Integer PRIMARY KEY, t9id Integer)
                  Table table9( id Integer PRIMARY KEY, value DOUBLE)
            
                   View aView
                   (
                       id : table1.id PRIMARY KEY,
                       value : @t1t2 > @t2t3 > @t3t4 > @t4t5 > @t5t6 > @t6t7 > @t7t8 > @t8t9 | table9.value
                   )
                )
            
                Join t1t2(pureTestSchema.table1.t2id=pureTestSchema.table2.id)
                Join t2t3(pureTestSchema.table2.t3id=pureTestSchema.table3.id)
                Join t3t4(pureTestSchema.table3.t4id=pureTestSchema.table4.id)
                Join t4t5(pureTestSchema.table4.t5id=pureTestSchema.table5.id)
                Join t5t6(pureTestSchema.table5.t6id=pureTestSchema.table6.id)
                Join t6t7(pureTestSchema.table6.t7id=pureTestSchema.table7.id)
                Join t7t8(pureTestSchema.table7.t8id=pureTestSchema.table8.id)
                Join t8t9(pureTestSchema.table8.t9id=pureTestSchema.table9.id)
            )\
            """;

    private static String dBWithViewMainTableValidationError = """
            ###Relational
            Database with::view::dBWithViewMainTableValidationError
            (
                  Table orderTable(id Integer PRIMARY KEY, prodFk Integer)
            
                  Table orderPnlTable(\s
                        ORDER_ID INT PRIMARY KEY,\s
                        pnl FLOAT
                  )
            
                  Table productDataSet(id Integer)
                      \s
                  View multipleMainTablesView
                   (
                       orderId : orderPnlTable.ORDER_ID PRIMARY KEY,\s
                       pnl: orderTable.id,
                       productName : @OrderTable_Product | orderTable.id
                   )
            
                Join OrderTable_Product(orderTable.prodFk=productDataSet.id)\s
            )\
            """;

    private static String dBWithFilteredView = """
            ###Relational
            Database with::view::filter
            (
                  Table orderTable(id Integer PRIMARY KEY, prodFk Integer)
            
                  Table orderPnlTable(\s
                        ORDER_ID INT PRIMARY KEY,\s
                        pnl FLOAT
                  )
                  View multipleMainTablesView
                  (
                       ~filter nonZeroPnlFilter\
                       pnl : orderPnlTable.pnl,\s
                       orderId: @OrderPnlTable_OrderTable | orderTable.id
                  )
                  Join OrderPnlTable_OrderTable(orderPnlTable.ORDER_ID=orderTable.id)
                  Filter nonZeroPnlFilter(orderPnlTable.pnl != 0)\s
            
            )\
            """;

    private static String dbWithViewDependentOnTableInOtherDb = """
            ###Relational
            Database db
            (
                  Table orderTable(id Integer PRIMARY KEY, prodFk Integer)
            )
            
            ###Relational
            Database with::view::db
            (
                  Table orderPnlTable(\s
                        ORDER_ID INT PRIMARY KEY,\s
                        pnl FLOAT
                  )
            
                  Table productDataSet(id Integer)
                      \s
                  View multipleMainTablesView
                  (
                     orderId : [db]orderTable.id
                  )
            )\
            """;

    private static String dbWithViewDependentOnTableInOtherDbViaIncludes = """
            ###Relational
            Database db
            (
                  Table orderTable(id Integer PRIMARY KEY, prodFk Integer)
            )
            
            ###Relational
            Database with::view::db
            (
                  include db\
                  Table orderPnlTable(\s
                        ORDER_ID INT PRIMARY KEY,\s
                        pnl FLOAT
                  )
            
                  Table productDataSet(id Integer)
                      \s
                  View multipleMainTablesView
                  (
                     orderId : [db]orderTable.id
                  )
            )\
            """;

    @Test
    public void testMainTableValidatesWithLongJoinChain()
    {
        Loader.parseM3(dbWithLongJoinChain, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        CoreInstance db = this.graphWalker.getDbInstance("with::view::Db");
        CoreInstance pureTestSchema = this.graphWalker.getSchema(db, "pureTestSchema");
        CoreInstance view = this.graphWalker.getView(pureTestSchema, "aView");
        assertMainTableAlias(view, "pureTestSchema", "table1");
    }

    @Test
    public void testViewFilter()
    {
        Loader.parseM3(dBWithFilteredView, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        CoreInstance db = this.graphWalker.getDbInstance("with::view::filter");
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);

        CoreInstance multipleMainTablesView = this.graphWalker.getView(defaultSchema, "multipleMainTablesView");
        CoreInstance filter = multipleMainTablesView.getValueForMetaPropertyToOne(M2RelationalProperties.filter);
        Assertions.assertEquals("nonZeroPnlFilter", filter.getValueForMetaPropertyToOne(M2RelationalProperties.filterName).getName());
        Assertions.assertNotNull(filter.getValueForMetaPropertyToOne(M2RelationalProperties.filter).getValueForMetaPropertyToOne(M2RelationalProperties.operation));
        Assertions.assertEquals(db, Instance.getValueForMetaPropertyToOneResolved(filter, M2RelationalProperties.database, processorSupport));
    }

    @Test
    public void testRelationalViewMainTableValidation()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            Loader.parseM3(dBWithViewMainTableValidationError, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:fromString.pure lines:13c12-18c8), \"View: multipleMainTablesView contains multiple main tables: [orderPnlTable,orderTable,productDataSet] there should be only one root Table for Views"));
    }

    @Test
    public void testRelationalViewDisallowOtherDbDependencyValidation()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            Loader.parseM3(dbWithViewDependentOnTableInOtherDb, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        });
        assertTrue(exception.getMessage().contains("All tables referenced in View: multipleMainTablesView should come from the View's owning or included DB: 'with::view::db', table: 'orderTable' does not"));
    }

    @Test
    public void testRelationalViewAllowOtherDbDependencyFromIncludesValidation()
    {
        Loader.parseM3(dbWithViewDependentOnTableInOtherDbViaIncludes, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
    }

    private void assertMainTableAlias(CoreInstance orderPnlView, String expectedSchemaName, String expectedTableName)
    {
        CoreInstance mainTableAlias = orderPnlView.getValueForMetaPropertyToOne(M2RelationalProperties.mainTableAlias);
        CoreInstance mainTable = mainTableAlias.getValueForMetaPropertyToOne(M2RelationalProperties.relationalElement);
        Assertions.assertEquals(expectedSchemaName, graphWalker.getName(mainTable.getValueForMetaPropertyToOne(M2RelationalProperties.schema)));
        Assertions.assertEquals(expectedTableName, graphWalker.getName(mainTable));
    }

    @Test
    public void testPrimaryKeyCannotBeSpecifiedInMapping()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            Loader.parseM3("""
                    Class person::Person
                    {
                        name:String[1];
                    }
                    ###Relational
                    Database db(
                       Table employeeTable
                       (
                        name VARCHAR(200)
                       )
                    )
                    ###Mapping
                    Mapping mappingPackage::myMapping
                    (
                        person::Person: Relational
                        {
                           name : [db]employeeTable.name PRIMARY KEY
                        }
                    )\
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("'PRIMARY KEY' cannot be specified in mapping"));
    }

    @Test
    public void testViewInSchemaPotentialIssue()
    {
        Loader.parseM3(
                """
                ###Relational
                 Database db(
                    Schema ep_Datastore(
                       Table Team( TEAM VARCHAR(200) PRIMARY KEY  )
                    )
                   \s
                    Schema viewSchema(
                       View TeamDistinct(
                             ~distinct
                              TEAM: ep_Datastore.Team.TEAM PRIMARY KEY\s
                        )\s
                    )
                )\
                """,
                repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");
        CoreInstance teamCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.columns);
        CoreInstance teamMappingCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.columnMappings).getValueForMetaPropertyToOne(M2RelationalProperties.relationalOperationElement).getValueForMetaPropertyToOne(M2RelationalProperties.column);
        CoreInstance viewPkCol = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.primaryKey);
        CoreInstance mainTable = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.mainTableAlias).getValueForMetaPropertyToOne(M2RelationalProperties.relationalElement);
        Assertions.assertEquals(mainTable, teamMappingCol.getValueForMetaPropertyToOne(M3Properties.owner));
        Assertions.assertEquals(teamDistinctView, teamCol.getValueForMetaPropertyToOne(M3Properties.owner));
        Assertions.assertEquals(teamDistinctView, viewPkCol.getValueForMetaPropertyToOne(M3Properties.owner));
    }

    @Test
    public void testViewGroupByCompilation()
    {
        Loader.parseM3(
                """
                ###Relational
                 Database db(
                    Schema ep_Datastore(
                       Table Order( ID Integer PRIMARY KEY,
                            ACCOUNT_ID INTEGER,
                            PNL FLOAT
                         )
                    )
                   \s
                    Schema viewSchema(
                       View TeamDistinct(
                             ~groupBy (ep_Datastore.Order.ACCOUNT_ID)
                              accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY,\s
                              summedPnl: sum(ep_Datastore.Order.PNL)\s
                        )\s
                    )
                )\
                """,
                repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");

        ListIterable<? extends CoreInstance> viewMappingCols = teamDistinctView.getValueForMetaPropertyToMany(M2RelationalProperties.columnMappings);
        Assertions.assertEquals(2, viewMappingCols.size());
        CoreInstance groupByMapping = teamDistinctView.getValueForMetaPropertyToOne(M2RelationalProperties.groupBy);
        Assertions.assertNotNull(groupByMapping);
        Assertions.assertEquals(1, groupByMapping.getValueForMetaPropertyToMany(M2RelationalProperties.columns).size());
    }

    @Test
    public void testViewGroupWithJoinCompilation()
    {
        Loader.parseM3(
                """
                ###Relational
                 Database db(
                    Schema ep_Datastore(
                       Table Order( ID Integer PRIMARY KEY,
                            ACCOUNT_ID INTEGER,
                            PNL FLOAT
                         )
                       Table orderPnlTable( ORDER_ID INT PRIMARY KEY, pnl FLOAT)\
                    )
                   \s
                    Schema viewSchema(
                       View TeamDistinct(
                             ~groupBy (ep_Datastore.Order.ACCOUNT_ID)
                              accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY,\s
                              orderPnl : sum(@OrderPnlTable_Order | ep_Datastore.orderPnlTable.pnl)\s
                        )\s
                    )
                    Join OrderPnlTable_Order(ep_Datastore.orderPnlTable.ORDER_ID = ep_Datastore.Order.ID)\
                )\
                """,
                repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("db");
        CoreInstance viewSchema = this.graphWalker.getSchema(db, "viewSchema");
        CoreInstance teamDistinctView = this.graphWalker.getView(viewSchema, "TeamDistinct");
        CoreInstance orderPnlColMapping = teamDistinctView.getValueForMetaPropertyToMany(M2RelationalProperties.columnMappings).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M2RelationalProperties.columnName).getName().equals("orderPnl");
            }
        });
        CoreInstance roeWithJoin = Instance.getValueForMetaPropertyToOneResolved(orderPnlColMapping, M2RelationalProperties.relationalOperationElement, M3Properties.parameters, processorSupport);
        CoreInstance join = roeWithJoin.getValueForMetaPropertyToOne(M2RelationalProperties.joinTreeNode);
        CoreInstance alias = join.getValueForMetaPropertyToOne(M2RelationalProperties.alias);
        Assertions.assertNotNull(alias);
    }

    @Test
    public void testViewGroupWithIncorrectGroupByTableSpecification()
    {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            Loader.parseM3(
                    """
                    ###Relational
                     Database db(
                        Schema ep_Datastore(
                           Table Order( ID Integer PRIMARY KEY,
                                ACCOUNT_ID INTEGER,
                                PNL FLOAT
                             )
                     Table otherOrder( ID Integer PRIMARY KEY)\
                        )
                       \s
                        Schema viewSchema(
                           View TeamDistinct(
                                 ~groupBy (ep_Datastore.otherOrder.ID)
                                  accountId: ep_Datastore.Order.ACCOUNT_ID PRIMARY KEY,\s
                                  summedPnl: sum(ep_Datastore.Order.PNL)\s
                            )\s
                        )
                    )\
                    """,
                    repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("View: TeamDistinct has a groupBy which refers to table: 'otherOrder' which is not the mainTable: 'Order'"));
    }
}
