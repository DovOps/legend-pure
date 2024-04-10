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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.EnumerationMappingParser;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessSnapshotMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSimpleGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    private RelationalGraphWalker graphWalker;

    @BeforeEach
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }

    @Test
    public void testTable()
    {
        Loader.parseM3("""
                Class Employee
                {
                    name : String[1];
                }
                
                ###Relational
                Database pack::myDatabase (
                /* Comment */
                Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                ))
                ###Pure
                import pack::*;
                function test():Boolean[1]
                {
                    let et = myDatabase->meta::relational::metamodel::schema('default')->map(x|$x->meta::relational::metamodel::table('employeeTable'));
                    assert('employeeTable' == $et.name, |'');
                    assert($et.columns->size() == 2, |'');
                    assert(['id', 'name'] == $et.columns->cast(@meta::relational::metamodel::Column)->map(c | $c.name), |'');
                    let ft = myDatabase->meta::relational::metamodel::schema('default')->map(x|$x->meta::relational::metamodel::table('firmTable'));
                    assert('firmTable' == $ft.name, |'');
                    assert(['id', 'name'] == $ft.columns->cast(@meta::relational::metamodel::Column)->map(c | $c.name), |'');
                }
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("pack::myDatabase");
        Assertions.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assertions.assertNotNull(defaultSchema);

        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        ListIterable<? extends CoreInstance> firmTableColumns = this.graphWalker.getColumns(firmTable);
        Assertions.assertEquals(2, employeeTableColumns.size());
        Assertions.assertEquals(2, firmTableColumns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));
    }

    @Test
    public void testTableWithBusinessSnapshotMilestoning()
    {
        runtime.createInMemorySource("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(BUS_SNAPSHOT_DATE = snapshotDate)
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        runtime.compile();

        Database productDatabase = (Database) runtime.getCoreInstance("pack::ProductDatabase");
        RichIterable<? extends Milestoning> milestonings = productDatabase._schemas().getFirst()._tables().getFirst()._milestoning().selectInstancesOf(BusinessSnapshotMilestoning.class);
        Assertions.assertEquals(1, milestonings.size());
        Assertions.assertTrue(milestonings.getFirst() instanceof BusinessSnapshotMilestoning);
        BusinessSnapshotMilestoning businessSnapshotMilestoning = (BusinessSnapshotMilestoning) milestonings.getFirst();
        Assertions.assertEquals("snapshotDate", businessSnapshotMilestoning._snapshotDate()._name());
        Assertions.assertNull(businessSnapshotMilestoning._infinityDate());
    }

    @Test
    public void testSnapshotDateColumnType()
    {
        runtime.createInMemorySource("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(BUS_SNAPSHOT_DATE = snapshotDate)
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Timestamp
                   )
                )
                """);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:9 column:8), \"Column set as BUS_SNAPSHOT_DATE can only be of type : [Date]\"", e.getMessage());
    }

    @Test
    public void testTableWithMilestoningInformationWithIsThruInclusive()
    {
        runtime.createInMemorySource("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false, INFINITY_DATE=%9999-12-30T19:00:00.0000),
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false, INFINITY_DATE=%9999-12-30T19:00:00.0000)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                   Table ProductTable2(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=true, INFINITY_DATE=%9999-12-30T19:00:00.0000),
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=true, INFINITY_DATE=%9999-12-30T19:00:00.0000)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                   Table ProductTable3(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false),
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=true)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                   Table ProductTable4(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=true),
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        runtime.compile();

        CoreInstance db = this.graphWalker.getDbInstance("pack::ProductDatabase");
        Assertions.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assertions.assertNotNull(defaultSchema);
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "ProductTable3"));

        CoreInstance ProductTable1 = this.graphWalker.getTable(defaultSchema, "ProductTable1");
        ListIterable<? extends CoreInstance> ProductTable1Columns = this.graphWalker.getColumns(ProductTable1);
        Assertions.assertEquals(6, ProductTable1Columns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable1, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable1, "name"));
        Assertions.assertNotNull(this.graphWalker.getMany(ProductTable1, "milestoning"));
        CoreInstance processingMilestoning1 = this.graphWalker.getMany(ProductTable1, "milestoning").get(0);
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "in"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "out"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "outIsInclusive"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning1, "infinityDate"));
        CoreInstance businessMilestoning1 = this.graphWalker.getMany(ProductTable1, "milestoning").get(1);
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "from"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "thru"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "thruIsInclusive"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning1, "infinityDate"));

        CoreInstance ProductTable2 = this.graphWalker.getTable(defaultSchema, "ProductTable2");
        ListIterable<? extends CoreInstance> ProductTable2Columns = this.graphWalker.getColumns(ProductTable2);
        Assertions.assertEquals(6, ProductTable2Columns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable2, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable2, "name"));
        Assertions.assertNotNull(this.graphWalker.getMany(ProductTable2, "milestoning"));
        CoreInstance processingMilestoning2 = this.graphWalker.getMany(ProductTable2, "milestoning").get(0);
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "in"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "out"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "outIsInclusive"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning2, "infinityDate"));

        CoreInstance businessMilestoning2 = this.graphWalker.getMany(ProductTable2, "milestoning").get(1);
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "from"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "thru"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "thruIsInclusive"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning2, "infinityDate"));


        CoreInstance ProductTable3 = this.graphWalker.getTable(defaultSchema, "ProductTable3");
        ListIterable<? extends CoreInstance> ProductTable3Columns = this.graphWalker.getColumns(ProductTable3);
        Assertions.assertEquals(6, ProductTable3Columns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable3, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable3, "name"));
        Assertions.assertNotNull(this.graphWalker.getMany(ProductTable3, "milestoning"));
        CoreInstance processingMilestoning3 = this.graphWalker.getMany(ProductTable3, "milestoning").get(0);
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "in"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "out"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning3, "outIsInclusive"));
        CoreInstance businessMilestoning3 = this.graphWalker.getMany(ProductTable3, "milestoning").get(1);
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "from"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "thru"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning3, "thruIsInclusive"));

        CoreInstance ProductTable4 = this.graphWalker.getTable(defaultSchema, "ProductTable4");
        ListIterable<? extends CoreInstance> ProductTable4Columns = this.graphWalker.getColumns(ProductTable4);
        Assertions.assertEquals(6, ProductTable4Columns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable4, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(ProductTable4, "name"));
        Assertions.assertNotNull(this.graphWalker.getMany(ProductTable4, "milestoning"));
        CoreInstance processingMilestoning4 = this.graphWalker.getMany(ProductTable4, "milestoning").get(0);
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "in"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "out"));
        Assertions.assertNotNull(this.graphWalker.getOne(processingMilestoning4, "outIsInclusive"));

        CoreInstance businessMilestoning4 = this.graphWalker.getMany(ProductTable4, "milestoning").get(1);
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "from"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "thru"));
        Assertions.assertNotNull(this.graphWalker.getOne(businessMilestoning4, "thruIsInclusive"));
    }

    @Test
    public void testOutIsInclusiveSyntax()
    {
        runtime.createInMemorySource("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z OUT_IS_INCLUSIVE=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE
                   )\s
                  \s
                )
                """);
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e1 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        assertPureException(PureParserException.class, "expected: ')' found: 'OUT_IS_INCLUSIVE'", 7, 68, e1);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z,, OUT_IS_INCLUSIVE=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e2 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'OUT_IS_INCLUSIVE', 'INFINITY_DATE'} found: ','", 7, 68, e2);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, OUT_IS_INCLUSIVE=false, )
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e3 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'INFINITY_DATE' found: '<EOF>'", 7, 92, e3);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, OUT_IS_INCLUSIVE=false, PROCESSING_OUT=out_z)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e4 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'PROCESSING_OUT' found: 'OUT_IS_INCLUSIVE'", 7, 47, e4);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z, Out_Is_Inclusive=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            in_z DATE,\s
                            out_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e5 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'OUT_IS_INCLUSIVE', 'INFINITY_DATE'} found: 'Out_Is_Inclusive'", 7, 69, e5);
    }

    @Test
    public void testThruIsInclusiveSyntax()
    {
        runtime.createInMemorySource("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               business(BUS_FROM=from_z, BUS_THRU=thru_z THRU_IS_INCLUSIVE=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e1 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        assertPureException(PureParserException.class, "expected: ')' found: 'THRU_IS_INCLUSIVE'", 7, 58, e1);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               business(BUS_FROM=from_z, BUS_THRU=thru_z,, THRU_IS_INCLUSIVE=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e2 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'THRU_IS_INCLUSIVE', 'INFINITY_DATE'} found: ','", 7, 58, e2);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, THRU_IS_INCLUSIVE=false, )
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e3 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'INFINITY_DATE' found: '<EOF>'", 7, 83, e3);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               business(BUS_FROM=from_z, THRU_IS_INCLUSIVE=false, BUS_THRU=thru_z)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e4 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: 'BUS_THRU' found: 'THRU_IS_INCLUSIVE'", 7, 42, e4);

        runtime.modify("database.pure", """
                ###Relational
                
                Database pack::ProductDatabase
                (
                   Table ProductTable1(
                            milestoning(
                               business(BUS_FROM=from_z, BUS_THRU=thru_z, Thru_Is_Inclusive=false)
                            )
                            id Integer PRIMARY KEY,\s
                            name VARCHAR(200) PRIMARY KEY,
                            from_z DATE,\s
                            thru_z DATE
                   )\s
                  \s
                )
                """);
        PureParserException e5 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class, "expected: one of {'THRU_IS_INCLUSIVE', 'INFINITY_DATE'} found: 'Thru_Is_Inclusive'", 7, 59, e5);
    }

    @Test
    public void testBusinessSnapshotMilestoningSyntax()
    {
        runtime.createInMemorySource("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(INFINITY_DATE=%9999-12-31, BUS_SNAPSHOT_DATE = snapshotDate)
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        PureParserException e1 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        Assertions.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: 'INFINITY_DATE'", e1.getMessage());

        runtime.modify("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(BUS_SNAPSHOT_DATE = snapshotDate, INFINITY_DATE=%9999-12-31, )
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e2 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        Assert.assertEquals("expected: ')' found: ','", e2.getMessage());

        runtime.modify("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(BUS_SNAPSHOT_DATE = snapshotDate, )
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        runtime.compile(); // parser can auto-correct this syntax error
//        PureParserException e3 = Assert.assertThrows(PureParserException.class, runtime::compile);
//        Assert.assertEquals("expected: \")\" found: \",\"", e3.getMessage());

        runtime.modify("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(, BUS_SNAPSHOT_DATE = snapshotDate)
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        PureParserException e4 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        Assertions.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: ','", e4.getMessage());

        runtime.modify("test.pure", """
                ###Relational
                Database pack::ProductDatabase (
                   Table ProductTable
                   (
                       milestoning(\s
                          business(bus_snapshot_date = snapshotDate)
                       )\
                       id INT PRIMARY KEY,
                       name VARCHAR(200),
                       snapshotDate Date
                   )
                )
                """);
        PureParserException e5 = Assertions.assertThrows(PureParserException.class, runtime::compile);
        Assertions.assertEquals("Parser error at (resource:test.pure line:6 column:20), expected: one of {'BUS_FROM', 'BUS_SNAPSHOT_DATE', 'processing'} found: 'bus_snapshot_date'", e5.getMessage());
    }

    @Test
    public void testJoin()
    {
        Loader.parseM3("""
                ###Relational
                Database myDB ( Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                ))
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("myDB");
        Assertions.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assertions.assertNotNull(defaultSchema);

        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        Assertions.assertEquals(3, employeeTableColumns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));

        ListIterable<? extends CoreInstance> joins = this.graphWalker.getJoins(db);
        Assertions.assertEquals(1, joins.size());
        final CoreInstance employeeFirmJoin = this.graphWalker.getJoin(db, "Employee_Firm");
        Assertions.assertNotNull(employeeFirmJoin);

        ListIterable<? extends CoreInstance> aliases = this.graphWalker.getJoinAliases(employeeFirmJoin);
        Assertions.assertEquals(2, aliases.size());
        CoreInstance firstAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getFirst());

        Assertions.assertEquals("employeeTable", this.graphWalker.getName(firstAliasFirst));

        CoreInstance firstAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getFirst());

        Assertions.assertEquals("firmTable", this.graphWalker.getName(firstAliasSecond));

        CoreInstance secondAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getLast());

        Assertions.assertEquals("firmTable", this.graphWalker.getName(secondAliasFirst));

        CoreInstance secondAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getLast());

        Assertions.assertEquals("employeeTable", this.graphWalker.getName(secondAliasSecond));

        CoreInstance joinOperation = this.graphWalker.getJoinOperation(employeeFirmJoin);
        ListIterable<? extends CoreInstance> operationParameters = this.graphWalker.getJoinOperationParameters(joinOperation);
        CoreInstance operationLeft = operationParameters.get(0);
        CoreInstance operationRight = operationParameters.get(1);

        CoreInstance operationLeftAlias = this.graphWalker.getJoinOperationAlias(operationLeft);
        CoreInstance operationRightAlias = this.graphWalker.getJoinOperationAlias(operationRight);

        Assertions.assertEquals("employeeTable", this.graphWalker.getName(operationLeftAlias));
        Assertions.assertEquals("firmTable", this.graphWalker.getName(operationRightAlias));

        CoreInstance operationLeftColumn = this.graphWalker.getJoinOperationRelationalElement(operationLeft);

        CoreInstance operationRightColumn = this.graphWalker.getJoinOperationRelationalElement(operationRight);

        Assertions.assertEquals("firmId", this.graphWalker.getName(operationLeftColumn));
        Assertions.assertEquals("id", this.graphWalker.getName(operationRightColumn));
    }


    @Test
    public void testJoinTableError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                """
                ###Relational
                Database db
                (
                Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTableErr.id
                )
                )
                """));
        assertPureException(PureCompilationException.class, "The table 'firmTableErr' can't be found in the schema 'default' in the database 'db'", "testStore.pure", 17, 28, e);
    }

    @Test
    public void testJoinColumnError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                """
                ###Relational
                Database db(Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.idErr
                ))
                """));
        assertPureException(PureCompilationException.class, "The column 'idErr' can't be found in the table 'firmTable'", "testStore.pure", 16, 38, e);
    }

    @Test
    public void testSelfJoinError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStoreWithError.pure",
                """
                ###Relational
                Database TestDB
                (
                  Schema TestSchema
                  (
                    Table TestTable
                    (
                      id1 INT PRIMARY KEY,
                      id2 INT,
                      name VARCHAR(128)
                    )
                  )
                
                  Schema TestSchema2
                  (
                    Table TestTable
                    (
                      id1 INT PRIMARY KEY,
                      id2 INT,
                      name VARCHAR(128)
                    )
                  )
                
                  Join TestJoin
                  (
                    TestSchema.TestTable.id1 = {target}.id1 and
                    TestSchema2.TestTable.id2 = {target}.id2\
                  )
                )
                """));
        assertPureException(PureCompilationException.class, "A self join can only contain 1 table, found 2", "testStoreWithError.pure", 24, 8, e);
    }

    @Test
    public void testDuplicateTablesCauseError()
    {
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore1.pure",
                """
                ###Relational
                Database db
                (
                   Table employeeTable
                   (
                       id INT PRIMARY KEY
                   )
                   Table employeeTable
                   (
                       id INT PRIMARY KEY
                   )
                )
                """));
        assertPureException(PureCompilationException.class, "More than one Table found with the name 'employeeTable': Table names must be unique within a database", "testStore1.pure", 8, 10, e1);

        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore2.pure",
                """
                ###Relational
                Database db
                (
                   Schema hr
                   (
                      Table employeeTable
                      (
                          id INT PRIMARY KEY
                      )
                      Table employeeTable
                      (
                          id INT PRIMARY KEY
                      )
                   )
                )
                """));
        assertPureException(PureCompilationException.class, "More than one Table found with the name 'employeeTable': Table names must be unique within a schema", "testStore2.pure", 10, 13, e2);
    }

    @Test
    public void testDuplicateJoinsCauseError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                """
                ###Relational
                Database db
                (
                Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                )
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                )
                )
                """));
        assertPureException(PureCompilationException.class, "More than one Join found with the name 'Employee_Firm': Join names must be unique within a database", "testStore.pure", 20, 6, e);
    }

    @Test
    public void testDuplicateFiltersCauseError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testStore.pure",
                """
                ###Relational
                Database db
                (
                Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Filter myFilter(employeeTable.firmId = 2)
                Filter myFilter(employeeTable.firmId = 3)
                )
                """));
        assertPureException(PureCompilationException.class, "More than one Filter found with the name 'myFilter': Filter names must be unique within a database", "testStore.pure", 11, 8, e);
    }

    @Test
    public void testMappingScope()
    {
        Loader.parseM3("""
                import other::*;
                
                Class other::Person
                {
                    name : String[1];
                    id : Integer[1];\
                    other : String[1];\
                }
                ###Relational
                Database mapping::pack::db\
                (\
                    Table employeeTable
                    (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        other VARCHAR(200),
                        firmId INT
                    )\
                )
                ###Mapping
                import mapping::pack::*;
                Mapping mappingPackage::myMapping
                (
                 /* comment */
                    other::Person: Relational
                            {\
                                scope([db])\
                                (\
                                    name : employeeTable.name
                                ),\
                                scope([db]default.employeeTable)\
                                (\
                                    id : id\
                                ),\
                                scope([db]employeeTable)\
                                (\
                                    other : other\
                                )\
                            }
                )
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mappingPackage::myMapping");
        CoreInstance personClassMappingImplementation = Instance.getValueForMetaPropertyToManyResolved(mapping, "classMappings", processorSupport).getFirst();
        ListIterable<? extends CoreInstance> personClassMappingImplementationPropertyMappings = Instance.getValueForMetaPropertyToManyResolved(personClassMappingImplementation, "propertyMappings", processorSupport);
        final StringBuilder sb = new StringBuilder("[\n");
        personClassMappingImplementationPropertyMappings.forEach(each ->
        {
            CoreInstance relationalOperationElement = Instance.getValueForMetaPropertyToOneResolved(each, "relationalOperationElement", TestSimpleGrammar.processorSupport);
            Printer.print(sb, relationalOperationElement, 3, runtime.getProcessorSupport());
            sb.append("\n");
        });
        String mappingGraphDump = sb.append("]").toString();
        Assertions.assertEquals("""
                [
                Anonymous_StripedId instance TableAliasColumn
                    alias(Property):
                        Anonymous_StripedId instance TableAlias
                            database(Property):
                                [~>] db instance Database
                            name(Property):
                                employeeTable instance String
                            relationalElement(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                    column(Property):
                        Anonymous_StripedId instance Column
                            name(Property):
                                name instance String
                            nullable(Property):
                                true instance Boolean
                            owner(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                            type(Property):
                                Anonymous_StripedId instance Varchar
                                    size(Property):
                                        200 instance Integer
                    columnName(Property):
                        name instance String
                    setMappingOwner(Property):
                        Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                            class(Property):
                                [~>] other::Person instance Class
                            distinct(Property):
                                false instance Boolean
                            id(Property):
                                other_Person instance String
                            mainTableAlias(Property):
                                Anonymous_StripedId instance TableAlias
                                    database(Property):
                                        [X] db instance Database
                                    name(Property):
                                         instance String
                                    relationalElement(Property):
                                        Anonymous_StripedId instance Table
                                            columns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            milestoning(Property):
                                            name(Property):
                                                [>3] employeeTable instance String
                                            primaryKey(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            schema(Property):
                                                [>3] Anonymous_StripedId instance Schema
                                            setColumns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            temporaryTable(Property):
                                                [>3] false instance Boolean
                            parent(Property):
                                [~>] mappingPackage::myMapping instance Mapping
                            primaryKey(Property):
                                Anonymous_StripedId instance TableAliasColumn
                                    alias(Property):
                                        Anonymous_StripedId instance TableAlias
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3]  instance String
                                            relationalElement(Property):
                                                [>3] Anonymous_StripedId instance Table
                                    column(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                            propertyMappings(Property):
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        name instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] name instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        [_] Anonymous_StripedId instance TableAliasColumn
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        id instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] id instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] id instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        other instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] other instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] other instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                            root(Property):
                                true instance Boolean
                            stores(Property):
                                [X] db instance Database
                            userDefinedPrimaryKey(Property):
                                false instance Boolean
                Anonymous_StripedId instance TableAliasColumn
                    alias(Property):
                        Anonymous_StripedId instance TableAlias
                            database(Property):
                                [~>] db instance Database
                            name(Property):
                                employeeTable instance String
                            relationalElement(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                            schema(Property):
                                default instance String
                    column(Property):
                        Anonymous_StripedId instance Column
                            name(Property):
                                id instance String
                            nullable(Property):
                                false instance Boolean
                            owner(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        [_] Anonymous_StripedId instance Column
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                            type(Property):
                                Anonymous_StripedId instance Integer
                    columnName(Property):
                        id instance String
                    setMappingOwner(Property):
                        Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                            class(Property):
                                [~>] other::Person instance Class
                            distinct(Property):
                                false instance Boolean
                            id(Property):
                                other_Person instance String
                            mainTableAlias(Property):
                                Anonymous_StripedId instance TableAlias
                                    database(Property):
                                        [X] db instance Database
                                    name(Property):
                                         instance String
                                    relationalElement(Property):
                                        Anonymous_StripedId instance Table
                                            columns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            milestoning(Property):
                                            name(Property):
                                                [>3] employeeTable instance String
                                            primaryKey(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            schema(Property):
                                                [>3] Anonymous_StripedId instance Schema
                                            setColumns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            temporaryTable(Property):
                                                [>3] false instance Boolean
                            parent(Property):
                                [~>] mappingPackage::myMapping instance Mapping
                            primaryKey(Property):
                                Anonymous_StripedId instance TableAliasColumn
                                    alias(Property):
                                        Anonymous_StripedId instance TableAlias
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3]  instance String
                                            relationalElement(Property):
                                                [>3] Anonymous_StripedId instance Table
                                    column(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                            propertyMappings(Property):
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        name instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] name instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] name instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        id instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] id instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        [_] Anonymous_StripedId instance TableAliasColumn
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        other instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] other instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] other instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                            root(Property):
                                true instance Boolean
                            stores(Property):
                                [X] db instance Database
                            userDefinedPrimaryKey(Property):
                                false instance Boolean
                Anonymous_StripedId instance TableAliasColumn
                    alias(Property):
                        Anonymous_StripedId instance TableAlias
                            database(Property):
                                [~>] db instance Database
                            name(Property):
                                employeeTable instance String
                            relationalElement(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] other instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                    column(Property):
                        Anonymous_StripedId instance Column
                            name(Property):
                                other instance String
                            nullable(Property):
                                true instance Boolean
                            owner(Property):
                                Anonymous_StripedId instance Table
                                    columns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    milestoning(Property):
                                    name(Property):
                                        employeeTable instance String
                                    primaryKey(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    schema(Property):
                                        Anonymous_StripedId instance Schema
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3] default instance String
                                            relations(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            tables(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            views(Property):
                                    setColumns(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] name instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Varchar
                                        [_] Anonymous_StripedId instance Column
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] firmId instance String
                                            nullable(Property):
                                                [>3] true instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                                    temporaryTable(Property):
                                        false instance Boolean
                            type(Property):
                                Anonymous_StripedId instance Varchar
                                    size(Property):
                                        200 instance Integer
                    columnName(Property):
                        other instance String
                    setMappingOwner(Property):
                        Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                            class(Property):
                                [~>] other::Person instance Class
                            distinct(Property):
                                false instance Boolean
                            id(Property):
                                other_Person instance String
                            mainTableAlias(Property):
                                Anonymous_StripedId instance TableAlias
                                    database(Property):
                                        [X] db instance Database
                                    name(Property):
                                         instance String
                                    relationalElement(Property):
                                        Anonymous_StripedId instance Table
                                            columns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            milestoning(Property):
                                            name(Property):
                                                [>3] employeeTable instance String
                                            primaryKey(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            schema(Property):
                                                [>3] Anonymous_StripedId instance Schema
                                            setColumns(Property):
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                                [>3] Anonymous_StripedId instance Column
                                            temporaryTable(Property):
                                                [>3] false instance Boolean
                            parent(Property):
                                [~>] mappingPackage::myMapping instance Mapping
                            primaryKey(Property):
                                Anonymous_StripedId instance TableAliasColumn
                                    alias(Property):
                                        Anonymous_StripedId instance TableAlias
                                            database(Property):
                                                [X] db instance Database
                                            name(Property):
                                                [>3]  instance String
                                            relationalElement(Property):
                                                [>3] Anonymous_StripedId instance Table
                                    column(Property):
                                        Anonymous_StripedId instance Column
                                            name(Property):
                                                [>3] id instance String
                                            nullable(Property):
                                                [>3] false instance Boolean
                                            owner(Property):
                                                [>3] Anonymous_StripedId instance Table
                                            type(Property):
                                                [>3] Anonymous_StripedId instance Integer
                            propertyMappings(Property):
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        name instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] name instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] name instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        id instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] id instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        Anonymous_StripedId instance TableAliasColumn
                                            alias(Property):
                                                [>3] Anonymous_StripedId instance TableAlias
                                            column(Property):
                                                [>3] Anonymous_StripedId instance Column
                                            columnName(Property):
                                                [>3] id instance String
                                            setMappingOwner(Property):
                                                [>3] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                                Anonymous_StripedId instance RelationalPropertyMapping
                                    localMappingProperty(Property):
                                        false instance Boolean
                                    owner(Property):
                                        [_] Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    property(Property):
                                        other instance Property
                                            aggregation(Property):
                                                [>3] None instance AggregationKind
                                            classifierGenericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            genericType(Property):
                                                [>3] Anonymous_StripedId instance GenericType
                                            multiplicity(Property):
                                                [X] PureOne instance PackageableMultiplicity
                                            name(Property):
                                                [>3] other instance String
                                            owner(Property):
                                                [X] Person instance Class
                                            referenceUsages(Property):
                                                [>3] Anonymous_StripedId instance ReferenceUsage
                                    relationalOperationElement(Property):
                                        [_] Anonymous_StripedId instance TableAliasColumn
                                    sourceSetImplementationId(Property):
                                        other_Person instance String
                                    targetSetImplementationId(Property):
                                         instance String
                            root(Property):
                                true instance Boolean
                            stores(Property):
                                [X] db instance Database
                            userDefinedPrimaryKey(Property):
                                false instance Boolean
                ]\
                """, mappingGraphDump);
    }

    @Test
    public void testMapping()
    {
        Loader.parseM3("""
                import other::deep::*;
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::deep::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database mapping::pack::db(Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = [mapping::pack::db]firmTable.id
                ))
                ###Mapping
                import other::deep::*;
                import mapping::pack::*;
                Mapping mappingPackage::myMapping
                (
                    other::Person: Relational
                            {
                                name : [db]employeeTable.name,
                                firm : [db]@Employee_Firm
                            }
                    Firm : Relational
                           {
                                legalName: [db]firmTable.name,
                                employees: [db]@Employee_Firm
                           }
                )
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("mapping::pack::db");
        Assertions.assertNotNull(db);
        ListIterable<? extends CoreInstance> schemas = this.graphWalker.getSchemas(db);
        Assertions.assertEquals(1, schemas.size());
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assertions.assertNotNull(defaultSchema);
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "firmTable"));

        CoreInstance employeeTable = this.graphWalker.getTable(defaultSchema, "employeeTable");
        CoreInstance firmTable = this.graphWalker.getTable(defaultSchema, "firmTable");
        ListIterable<? extends CoreInstance> employeeTableColumns = this.graphWalker.getColumns(employeeTable);
        ListIterable<? extends CoreInstance> firmTableColumns = this.graphWalker.getColumns(firmTable);
        Assertions.assertEquals(3, employeeTableColumns.size());
        Assertions.assertEquals(2, firmTableColumns.size());

        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(employeeTable, "name"));

        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "id"));
        Assertions.assertNotNull(this.graphWalker.getColumn(firmTable, "name"));
        Assertions.assertEquals(200, this.graphWalker.getColumnSize(this.graphWalker.getColumn(firmTable, "name")));
        Assertions.assertEquals("Varchar", this.graphWalker.getClassifier(this.graphWalker.getColumnType(this.graphWalker.getColumn(firmTable, "name"))));

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());
        CoreInstance personMapping = this.graphWalker.getClassMapping(mapping, "Person");
        Assertions.assertNotNull(personMapping);
        CoreInstance firmMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assertions.assertNotNull(firmMapping);


        Assertions.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assertions.assertEquals("firmTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(firmMapping)));
        Assertions.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());
        Assertions.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(firmMapping).size());

        CoreInstance namePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "name");
        CoreInstance firmPropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(personMapping, "firm");
        Assertions.assertNotNull(firmPropMapping);
        CoreInstance nameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(namePropMapping);

        Assertions.assertEquals("employeeTable", this.graphWalker.getTableAliasColumnAliasName(nameColumnAlias));
        Assertions.assertEquals("name", this.graphWalker.getTableAliasColumnColumnName(nameColumnAlias));

        CoreInstance firmJoinNode = this.graphWalker.getRelationalOperationElementJoinTreeNode(this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(firmPropMapping));
        Assertions.assertEquals("Employee_Firm", this.graphWalker.getRelationalOperationElementJoinTreeNodeJoinName(firmJoinNode));

        CoreInstance legalNamePropMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(firmMapping, "legalName");
        CoreInstance legalNameColumnAlias = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(legalNamePropMapping);
        Assertions.assertEquals("firmTable", this.graphWalker.getTableAliasColumnAliasName(legalNameColumnAlias));
        Assertions.assertEquals("name", this.graphWalker.getTableAliasColumnColumnName(legalNameColumnAlias));

        CoreInstance employeesPropMapping = this.graphWalker.getClassMappingImplementationPropertyMappingRelationalOperationElement(firmPropMapping);
        CoreInstance employeesJoinNode = this.graphWalker.getRelationalOperationElementJoinTreeNode(employeesPropMapping);
        Assertions.assertEquals("Employee_Firm", this.graphWalker.getRelationalOperationElementJoinTreeNodeJoinName(employeesJoinNode));
    }

    @Test
    public void testMappingErrorClass()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                ###Pure
                Class Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database db (Table employeeTable
                (
                    id INT,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                ))
                ###Mapping
                Mapping myMapping
                (
                    PersonErr: Relational
                            {
                                name : employeeTable.name,
                                firm : @Employee_Firm
                            }
                    Firm : Relational
                           {
                                legalName: firmTable.name,
                                employees: @Employee_Firm
                           }
                )
                ###Pure
                function test():Nil[0]
                {
                    print(myMapping);
                }
                """));
        assertPureException(PureCompilationException.class, "PersonErr has not been defined!", "testSource.pure", 32, 5, e);
    }


    @Test
    public void testMappingErrorProperty()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                ###Pure
                Class Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database db(Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                ))
                ###Mapping
                Mapping myMapping
                (
                    Person: Relational
                            {
                                name : [db]employeeTable.name,
                                firmErr : [db]@Employee_Firm
                            }
                    Firm : Relational
                           {
                                legalName: [db]firmTable.name,
                                employees: [db]@Employee_Firm
                           }
                )
                ###Pure
                function test():Nil[0]
                {
                    print(myMapping);
                }
                """));
        assertPureException(PureCompilationException.class, "The property 'firmErr' is unknown in the Element 'Person'", "testSource.pure", 35, 17, e);
    }

    @Test
    public void testMappingErrorColumn()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                ###Pure
                Class Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database db (Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                ))
                ###Mapping
                Mapping myMapping
                (
                    Person: Relational
                            {
                                name : [db]employeeTable.nameErr,
                                firm : [db]@Employee_Firm
                            }
                    Firm : Relational
                           {
                                legalName: [db]firmTable.name,
                                employees: [db]@Employee_Firm
                           }
                )
                ###Pure
                function test():Nil[0]
                {
                    print(myMapping);
                }
                """));
        assertPureException(PureCompilationException.class, "The column 'nameErr' can't be found in the table 'employeeTable'", "testSource.pure", 34, 42, e);
    }


    @Test
    public void testMappingErrorJoin()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                ###Pure
                Class Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database db(Table employeeTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firmId INT
                )
                Table firmTable
                (
                    id INT PRIMARY KEY,
                    name VARCHAR(200)
                )
                
                Join Employee_Firm
                (
                    employeeTable.firmId = firmTable.id
                ))
                ###Mapping
                Mapping myMapping
                (
                    Person: Relational
                            {
                                name : [db]employeeTable.name,
                                firm : [db]@Employee_Firm
                            }
                    Firm : Relational
                           {
                                legalName: [db]firmTable.name,
                                employees: [db]@Employee_FirmErr
                           }
                )
                ###Pure
                function test():Nil[0]
                {
                    print(myMapping);
                }
                """));
        assertPureException(PureCompilationException.class, "The join 'Employee_FirmErr' has not been found in the database 'db'", "testSource.pure", 40, 33, e);
    }


    @Test
    public void testSelfJoin()
    {
        Loader.parseM3("""
                import other::*;
                ###Relational
                Database mapping::pack::db\
                (\
                    Table employeeTable
                    (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),\
                        manager_id INT
                    )\
                    Join Employee_Manager
                    (
                        {target}.id = employeeTable.manager_id\
                    )\
                )\
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance db = this.graphWalker.getDbInstance("mapping::pack::db");
        Assertions.assertNotNull(db);
        CoreInstance defaultSchema = this.graphWalker.getDefaultSchema(db);
        Assertions.assertNotNull(defaultSchema);
        Assertions.assertNotNull(this.graphWalker.getTable(defaultSchema, "employeeTable"));
        ListIterable<? extends CoreInstance> joins = this.graphWalker.getJoins(db);
        Assertions.assertEquals(1, joins.size());
        final CoreInstance employeeManagerJoin = this.graphWalker.getJoin(db, "Employee_Manager");
        Assertions.assertNotNull(employeeManagerJoin);

        ListIterable<? extends CoreInstance> aliases = this.graphWalker.getJoinAliases(employeeManagerJoin);
        Assertions.assertEquals(2, aliases.size());
        CoreInstance firstAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getFirst());
        Assertions.assertEquals("employeeTable", this.graphWalker.getName(firstAliasFirst));
        CoreInstance firstAliasSecond = this.graphWalker.getJoinAliasSecond(aliases.getFirst());
        Assertions.assertEquals("t_employeeTable", this.graphWalker.getName(firstAliasSecond));
        CoreInstance secondAliasFirst = this.graphWalker.getJoinAliasFirst(aliases.getLast());
        Assertions.assertEquals("t_employeeTable", this.graphWalker.getName(secondAliasFirst));
    }

    @Test
    public void testGroupBy()
    {
        Loader.parseM3("""
                Class mapping::groupby::model::domain::IncomeFunction
                {
                   code:Integer[1];
                   name:String[1];
                }
                
                
                
                ###Relational
                
                Database mapping::groupby::model::store::myDB
                (
                    Table ACCOUNT_INFO
                    (
                        id INT PRIMARY KEY,
                        ACC_NUM INT,
                        IF_CODE INT,
                        IF_NAME VARCHAR(200)
                    )
                
                )
                
                ###Mapping
                import mapping::groupby::model::domain::*;
                import mapping::groupby::model::store::*;
                
                Mapping mapping::testMapping
                (
                    IncomeFunction: Relational
                    {
                       ~groupBy([myDB]ACCOUNT_INFO.IF_CODE, [myDB]ACCOUNT_INFO.IF_NAME)
                       scope([myDB]ACCOUNT_INFO)
                       (
                          code: [myDB]IF_CODE,
                          name : [myDB]IF_NAME
                       )
                      \s
                    }
                )
                ###Pure
                import other::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::relation::*;
                import mapping::groupby::model::domain::*;
                import meta::relational::mapping::*;
                
                function test():Boolean[1]
                {\
                   let groupBy = mapping::testMapping->meta::pure::mapping::_classMappingByClass(IncomeFunction)->cast(@RootRelationalInstanceSetImplementation).groupBy;
                   print($groupBy, 2);
                   assert(2 == $groupBy.columns->size(), |'');
                }
                
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mapping::testMapping");
        CoreInstance classMapping = this.graphWalker.getMany(mapping, "classMappings").getFirst();
        CoreInstance groupBy = this.graphWalker.getOne(classMapping, "groupBy");
        Assertions.assertEquals("""
                Anonymous_StripedId instance GroupByMapping
                    columns(Property):
                        Anonymous_StripedId instance TableAliasColumn
                            alias(Property):
                                Anonymous_StripedId instance TableAlias
                                    database(Property):
                                        [~>] myDB instance Database
                                    name(Property):
                                        [>2] ACCOUNT_INFO instance String
                                    relationalElement(Property):
                                        [>2] Anonymous_StripedId instance Table
                            column(Property):
                                Anonymous_StripedId instance Column
                                    name(Property):
                                        [>2] IF_CODE instance String
                                    nullable(Property):
                                        [>2] true instance Boolean
                                    owner(Property):
                                        [>2] Anonymous_StripedId instance Table
                                    type(Property):
                                        [>2] Anonymous_StripedId instance Integer
                            columnName(Property):
                                IF_CODE instance String
                            setMappingOwner(Property):
                                Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    class(Property):
                                        [~>] IncomeFunction instance Class
                                    distinct(Property):
                                        [>2] false instance Boolean
                                    groupBy(Property):
                                        [>2] Anonymous_StripedId instance GroupByMapping
                                    id(Property):
                                        [>2] mapping_groupby_model_domain_IncomeFunction instance String
                                    mainTableAlias(Property):
                                        [>2] Anonymous_StripedId instance TableAlias
                                    parent(Property):
                                        [~>] mapping::testMapping instance Mapping
                                    primaryKey(Property):
                                        [>2] Anonymous_StripedId instance TableAliasColumn
                                        [>2] Anonymous_StripedId instance TableAliasColumn
                                    propertyMappings(Property):
                                        [>2] Anonymous_StripedId instance RelationalPropertyMapping
                                        [>2] Anonymous_StripedId instance RelationalPropertyMapping
                                    root(Property):
                                        [>2] true instance Boolean
                                    stores(Property):
                                        [X] myDB instance Database
                                    userDefinedPrimaryKey(Property):
                                        [>2] false instance Boolean
                        Anonymous_StripedId instance TableAliasColumn
                            alias(Property):
                                Anonymous_StripedId instance TableAlias
                                    database(Property):
                                        [~>] myDB instance Database
                                    name(Property):
                                        [>2] ACCOUNT_INFO instance String
                                    relationalElement(Property):
                                        [>2] Anonymous_StripedId instance Table
                            column(Property):
                                Anonymous_StripedId instance Column
                                    name(Property):
                                        [>2] IF_NAME instance String
                                    nullable(Property):
                                        [>2] true instance Boolean
                                    owner(Property):
                                        [>2] Anonymous_StripedId instance Table
                                    type(Property):
                                        [>2] Anonymous_StripedId instance Varchar
                            columnName(Property):
                                IF_NAME instance String
                            setMappingOwner(Property):
                                Anonymous_StripedId instance RootRelationalInstanceSetImplementation
                                    class(Property):
                                        [~>] IncomeFunction instance Class
                                    distinct(Property):
                                        [>2] false instance Boolean
                                    groupBy(Property):
                                        [>2] Anonymous_StripedId instance GroupByMapping
                                    id(Property):
                                        [>2] mapping_groupby_model_domain_IncomeFunction instance String
                                    mainTableAlias(Property):
                                        [>2] Anonymous_StripedId instance TableAlias
                                    parent(Property):
                                        [~>] mapping::testMapping instance Mapping
                                    primaryKey(Property):
                                        [>2] Anonymous_StripedId instance TableAliasColumn
                                        [>2] Anonymous_StripedId instance TableAliasColumn
                                    propertyMappings(Property):
                                        [>2] Anonymous_StripedId instance RelationalPropertyMapping
                                        [>2] Anonymous_StripedId instance RelationalPropertyMapping
                                    root(Property):
                                        [>2] true instance Boolean
                                    stores(Property):
                                        [X] myDB instance Database
                                    userDefinedPrimaryKey(Property):
                                        [>2] false instance Boolean\
                """, Printer.print(groupBy, 2, runtime.getProcessorSupport()));
    }

    @Test
    public void testDistinct()
    {
        Loader.parseM3("""
                Class mapping::distinct::model::domain::IncomeFunction
                {
                   code:Integer[1];
                }
                
                ###Relational
                
                Database mapping::distinct::model::store::myDB
                (
                    Table ACCOUNT_INFO
                    (
                        ACC_NUM INT,
                        IF_CODE INT  PRIMARY KEY
                    )
                
                )
                
                ###Mapping
                import mapping::distinct::model::domain::*;
                import mapping::distinct::model::store::*;
                
                Mapping mapping::testMapping
                (
                    IncomeFunction: Relational
                    {
                       ~distinct
                       scope([myDB]ACCOUNT_INFO)
                       (
                          code: IF_CODE
                       )
                      \s
                    }
                )
                ###Pure
                import other::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::relation::*;
                import mapping::distinct::model::domain::*;
                import meta::relational::mapping::*;
                
                function test():Boolean[1]
                {\
                   let distinct = mapping::testMapping->meta::pure::mapping::_classMappingByClass(IncomeFunction)->cast(@RootRelationalInstanceSetImplementation).distinct;
                   assert($distinct->toOne(), |'');
                }
                
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = runtime.getCoreInstance("mapping::testMapping");
        CoreInstance classMapping = this.graphWalker.getMany(mapping, "classMappings").getFirst();
        CoreInstance distinct = this.graphWalker.getOne(classMapping, "distinct");
        Assertions.assertEquals("true instance Boolean", Printer.print(distinct, 1, runtime.getProcessorSupport()));
    }

    @Test
    public void duplicatePropertyMappingCausesError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::myMapping
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name,
                        firm: [db]@firmJoin,
                        firm: [db]@firmJoin
                    }
                )
                """));
        assertPureException(PureCompilationException.class, "Duplicate mappings found for the property 'firm' (targetId: other_Firm) in the mapping for class Person, the property should have one mapping.", "testSource.pure", 33, 9, e);
    }

    @Test
    public void testValidateEnumPropertiesHaveEnumMappings()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                Class Employee
                {
                    name: String[1];
                    type: EmployeeType[0..1];
                }
                
                Enum EmployeeType
                {
                    CONTRACT,
                    FULL_TIME
                }
                ###Relational
                
                Database myDB
                (
                    Table employeeTable
                    (
                        type VARCHAR(20)
                    )
                )
                ###Mapping
                
                Mapping employeeTestMapping
                (
                   Employee: Relational
                   {
                        scope([myDB]default.employeeTable)
                        (
                            type : type
                        )
                   }
                )
                """));
        assertPureException(PureCompilationException.class, "Missing an EnumerationMapping for the enum property 'type'. Enum properties require an EnumerationMapping in order to transform the store values into the Enum.", "testSource.pure", 29, 13, e);
    }

    @Test
    public void testMappingIncludes()
    {
        String pureCode = """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                import mappingPackage::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                Mapping mappingPackage::subMapping2
                (
                    Firm: Relational
                    {
                        legalName : [db]employeeFirmDenormTable.legalName
                    }
                )
                Mapping mappingPackage::myMapping
                (
                    include mappingPackage::subMapping1
                    include subMapping2
                )
                """;
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assertions.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> includes = this.graphWalker.getMany(mapping, M3Properties.includes);
        Assertions.assertEquals(2, includes.size());
        MutableList<String> includedMappingPaths = includes.collect(include -> PackageableElement.getUserPathForPackageableElement(this.graphWalker.getOne(include, M3Properties.included)), Lists.mutable.ofInitialCapacity(2));
        Verify.assertListsEqual(Lists.fixedSize.with("mappingPackage::subMapping1", "mappingPackage::subMapping2"), includedMappingPaths);
    }

    @Test
    public void testInvalidMappingInclude()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                Mapping mappingPackage::myMapping
                (
                    include mappingPackage::subMapping1
                    include subMapping112
                )
                """));
        assertPureException(PureCompilationException.class, "subMapping112 has not been defined!", "testSource.pure", 37, 13, 37, 13, 37, 25, e);
    }

    @Test
    public void testDuplicateMappingInclude()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                Mapping mappingPackage::subMapping2
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                Mapping mappingPackage::myMapping
                (
                    include mappingPackage::subMapping1
                    include mappingPackage::subMapping2
                )
                """));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'other_Person' in mapping mappingPackage::myMapping", "testSource.pure", 36, 5, 36, 5, 39, 5, e);
    }

    @Test
    public void testNestedDuplicateMappingInclude()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                Mapping mappingPackage::subMapping2
                (
                    include mappingPackage::subMapping1
                )
                Mapping mappingPackage::subMapping3
                (
                    include mappingPackage::subMapping2
                )
                Mapping mappingPackage::myMapping
                (
                    include mappingPackage::subMapping3
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                )
                """));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'other_Person' in mapping mappingPackage::myMapping", "testSource.pure", 45, 5, 45, 5, 48, 5, e);
    }


    @Test
    public void testValidDuplicateEnumMapping()
    {
        String pureCode =
                """
                Enum TradeType
                {
                    BUY,
                    SELL
                }
                ###Mapping
                Mapping tradeMapping
                (
                   TradeType: EnumerationMapping TradeSource1
                   {
                       BUY:  ['BUY', 'B'],
                       SELL: ['SELL', 'S']
                   }
                   TradeType: EnumerationMapping TradeSource2
                   {
                       BUY:  ['CREDIT'],
                       SELL: ['DEBIT']
                   }
                )
                """;
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser(), new EnumerationMappingParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void testReferredEnumMappingFromIncludes()
    {
        String pureCode =
                """
                Enum TradeType
                {
                    BUY,
                    SELL
                }
                Class Trade
                {
                    type: TradeType[1];
                }
                
                Class EquityTrade extends Trade
                {
                    product: String[1];
                    quantity: Integer[1];
                }
                ###Relational
                
                Database tradeDB
                (
                    Table eqTradeTable
                    (
                        id INT PRIMARY KEY,
                        product VARCHAR(200),
                        type VARCHAR(10),
                        qty INTEGER
                    )
                )
                ###Mapping
                
                Mapping tradeMapping1
                (
                    TradeType: EnumerationMapping TradeSource1
                    {
                       BUY:  ['BUY', 'B'],
                       SELL: ['SELL', 'S']
                    }
                )
                
                Mapping tradeMapping2
                (
                    include tradeMapping1
                
                    TradeType: EnumerationMapping TradeSource2
                    {
                       BUY:  ['CREDIT'],
                       SELL: ['DEBIT']
                    }
                )
                
                Mapping tradeMapping3
                (
                    include tradeMapping2
                
                    EquityTrade: Relational
                    {
                        scope( [tradeDB] default.eqTradeTable)
                        (
                            product: product,
                            quantity: qty,
                            type : EnumerationMapping TradeSource1 : type
                        )
                    }
                )
                
                """;
        Loader.parseM3(pureCode, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser(), new EnumerationMappingParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();
    }

    @Test
    public void testInValidDuplicateEnumMapping()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testSource.pure",
                """
                Enum TradeType
                {
                    BUY,
                    SELL
                }
                ###Mapping
                Mapping tradeMapping1
                (
                   TradeType: EnumerationMapping\s
                   {
                       BUY:  ['BUY', 'B'],
                       SELL: ['SELL', 'S']
                   }
                )
                Mapping tradeMapping2
                (
                   include tradeMapping1
                   TradeType: EnumerationMapping\s
                   {
                       BUY:  ['CREDIT'],
                       SELL: ['DEBIT']
                   }
                )
                """));
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'default' in mapping tradeMapping2", "testSource.pure", 18, 4, 18, 4, 18, 12, e);
    }

    @Test
    public void wrongClassMappingFilterIdentifierCausesError()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    firstName:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database mapping::db(
                   Table personTable
                   (
                    id INT PRIMARY KEY,
                    firstName VARCHAR(200),
                    firmId INT,
                    legalName VARCHAR(200)
                   )
                   Table firmTable
                   (
                    id INT PRIMARY KEY,
                    legalName VARCHAR(200)
                   )
                   View personFirmView
                   (
                    id : personTable.id,
                    firstName : personTable.firstName,
                    firmId : personTable.firmId
                   )
                   Filter GoldmanSachsFilter(firmTable.legalName = 'GoldmanSachs')
                   Join Firm_Person(firmTable.id = personTable.firmId)
                )
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::myMapping
                (
                    Person: Relational
                    {
                        ~filter [mapping::db](Hello)@Firm_Person | [mapping::db] GoldmanSachsFilter\s
                        firstName : [db]personTable.firstName
                    }
                )
                """));
        assertPureException(PureParserException.class, "The joinType is not recognized. Valid join types are: [INNER, OUTER]", "testSource.pure", 43, 31, e);
    }

    @Test
    public void testClassMappingFilterWithInnerJoin()
    {
        compileTestSource("testSource.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    firstName:String[1];
                    firm:Firm[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                    employees:Person[1];
                }
                ###Relational
                Database mapping::db(
                   Table personTable
                   (
                    id INT PRIMARY KEY,
                    firstName VARCHAR(200),
                    firmId INT,
                    legalName VARCHAR(200)
                   )
                   Table firmTable
                   (
                    id INT PRIMARY KEY,
                    legalName VARCHAR(200)
                   )
                   View personFirmView
                   (
                    id : personTable.id,
                    firstName : personTable.firstName,
                    firmId : personTable.firmId
                   )
                   Filter GoldmanSachsFilter(firmTable.legalName = 'GoldmanSachs')
                   Join Firm_Person(firmTable.id = personTable.firmId)
                )
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::myMapping
                (
                    Person: Relational
                    {
                        ~filter [mapping::db](INNER)@Firm_Person | [mapping::db] GoldmanSachsFilter\s
                        firstName : [db]personTable.firstName
                    }
                )
                """);
    }

    @Test
    public void testNotNull()
    {
        compileTestSource("""
                ###Relational\s
                 \
                Database test::TestDb\s
                (\s
                Table testTable\s
                (\s
                testColumn1 varchar(200), \
                testColumn2 varchar(200) NOT NULL \
                )\s
                )\s
                """);
        CoreInstance coreInstanceForDb = runtime.getCoreInstance("test::TestDb");
        CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(coreInstanceForDb, M2RelationalProperties.schemas, M2RelationalProperties.tables, processorSupport);
        ListIterable<? extends CoreInstance> columns = Instance.getValueForMetaPropertyToManyResolved(table, M2RelationalProperties.columns, processorSupport);
        CoreInstance testColumn1 = columns.get(0);
        CoreInstance testColumn2 = columns.get(1);
        Assertions.assertTrue((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn1, M2RelationalProperties.nullable, processorSupport))));
        Assertions.assertFalse((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn2, M2RelationalProperties.nullable, processorSupport))));
    }

    @Test
    public void testPrimaryKeyIsNotNull()
    {
        compileTestSource("""
                ###Relational\s
                 \
                Database test::TestDb\s
                (\s
                Table testTable\s
                (\s
                testColumn1 varchar(200), \
                testColumn2 varchar(200) PRIMARY KEY \
                )\s
                )\s
                """);
        CoreInstance coreInstanceForDb = runtime.getCoreInstance("test::TestDb");
        CoreInstance table = Instance.getValueForMetaPropertyToOneResolved(coreInstanceForDb, M2RelationalProperties.schemas, M2RelationalProperties.tables, processorSupport);
        ListIterable<? extends CoreInstance> columns = Instance.getValueForMetaPropertyToManyResolved(table, M2RelationalProperties.columns, processorSupport);
        CoreInstance testColumn1 = columns.get(0);
        CoreInstance testColumn2 = columns.get(1);
        Assertions.assertTrue((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn1, M2RelationalProperties.nullable, processorSupport))));
        Assertions.assertFalse((PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(testColumn2, M2RelationalProperties.nullable, processorSupport))));
    }

    @Test
    public void testNotNullWithPrimaryKeyIsNotAllowed()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "testFile.pure",
                """
                ###Relational\s
                 \
                Database test::TestDb\s
                (\s
                Table testTable\s
                (\s
                testColumn1 varchar(200),\s
                testColumn2 varchar(200) PRIMARY KEY NOT NULL
                )\s
                )\s
                """));
        assertPureException(PureParserException.class, "expected: ')' found: 'NOT NULL'", 7, 38, e);
    }

    @Test
    public void testMappingAssociationDirectlyIsNotAllowed()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "testFile.pure",
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                }
                Association other::Firm_Person
                {
                    firm:Firm[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                    Firm_Person: Relational
                    {
                        employees : [db]@firmJoin
                    }
                )
                """));
        assertPureException(PureCompilationException.class, "Trying to map an unsupported type in Relational: Type Error: 'Association' not a subtype of 'Class<Any>'", e);
    }


    @Test
    public void testMappingAssociation()
    {
        Loader.parseM3("""
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                }
                Association other::Firm_Person
                {
                    firm:Firm[1];
                    employees:Person[1];
                }
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
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::subMapping1");
        Assertions.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping);
        Assertions.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assertions.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assertions.assertNotNull(employeesMapping);
        Assertions.assertEquals("fir1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("per1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());


        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assertions.assertNotNull(firmPropertyMapping);
        Assertions.assertEquals("per1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("fir1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());

    }

    @Test
    public void testMappingAssociationWithIncludes()
    {
        Loader.parseM3("""
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                }
                Association other::Firm_Person
                {
                    firm:Firm[1];
                    employees:Person[1];
                }
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
                Mapping mappingPackage::subMapping2
                (
                    Firm[fir1]: Relational
                    {
                        legalName : [db]employeeFirmDenormTable.legalName
                    }
                )
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
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping3 = this.graphWalker.getMapping("mappingPackage::subMapping3");
        Assertions.assertNotNull(mapping3);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping3);
        Assertions.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assertions.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assertions.assertNotNull(employeesMapping);
        Assertions.assertEquals("fir1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("per1", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());

        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assertions.assertNotNull(firmPropertyMapping);
        Assertions.assertEquals("per1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("fir1", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());
    }

    @Test
    public void testMappingAssociationDefaultIds()
    {
        Loader.parseM3("""
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                }
                Class other::Firm
                {
                    legalName:String[1];
                }
                Association other::Firm_Person
                {
                    firm:Firm[1];
                    employees:Person[1];
                }
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
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::subMapping1
                (
                    Person: Relational
                    {
                        name : [db]employeeFirmDenormTable.name
                    }
                    Firm: Relational
                    {
                        legalName : [db]employeeFirmDenormTable.legalName
                    }
                
                    Firm_Person: Relational
                    {
                        AssociationMapping
                        (
                           employees : [db]@firmJoin,
                           firm : [db]@firmJoin
                        )
                    }
                )
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);

        runtime.compile();

        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::subMapping1");
        Assertions.assertNotNull(mapping);
        ListIterable<? extends CoreInstance> associationMappings = this.graphWalker.getAssociationMappings(mapping);
        Assertions.assertEquals(1, associationMappings.size());
        CoreInstance associationMapping = associationMappings.getFirst();
        Assertions.assertNotNull(associationMapping);

        CoreInstance employeesMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "employees");

        Assertions.assertNotNull(employeesMapping);
        Assertions.assertEquals("other_Firm", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("other_Person", employeesMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());


        CoreInstance firmPropertyMapping = this.graphWalker.getClassMappingImplementationPropertyMapping(associationMapping, "firm");
        Assertions.assertNotNull(firmPropertyMapping);
        Assertions.assertEquals("other_Person", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.sourceSetImplementationId).getName());
        Assertions.assertEquals("other_Firm", firmPropertyMapping.getValueForMetaPropertyToOne(M2MappingProperties.targetSetImplementationId).getName());
    }
}
