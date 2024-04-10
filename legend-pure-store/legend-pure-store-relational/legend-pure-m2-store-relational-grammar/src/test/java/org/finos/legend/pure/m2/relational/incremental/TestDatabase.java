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

import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDatabase extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testDatabase() throws Exception
    {
        String relationalDB = """
                ###Relational
                Database db
                (
                   Table myTable
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView
                   (
                       ~filter MyTableNameNotNullFilter\
                       myTableName : myTable.name
                   )
                Filter MyTableNameNotNullFilter(myTable.name is not null)\
                )\
                """;
        runtime.createInMemorySource("sourceId.pure", relationalDB);
        runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").size());
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").getFirst().getValueForMetaPropertyToMany("tables").size());

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                this.compileAndExecute("test():Boolean[1]");
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:1 column:40), \"db has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", relationalDB);
            runtime.compile();
            Assertions.assertEquals(size, runtime.getModelRepository().serialize().length);

            db = processorSupport.package_getByUserPath("db");
            Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").size());
            Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").getFirst().getValueForMetaPropertyToMany("tables").size());
        }
    }


    @Test
    public void testDatabaseError() throws Exception
    {
        String relationalDB = """
                ###Relational
                Database %s
                (
                   Table myTable
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView
                   (
                       myTableName : myTable.name
                   )
                )\
                """;
        String relationalDB1 = relationalDB.formatted("db");
        String relationalDB2 = relationalDB.formatted("db2");
        runtime.createInMemorySource("sourceId.pure", relationalDB1);
        runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");

        runtime.compile();
        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").size());
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").getFirst().getValueForMetaPropertyToMany("tables").size());
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:1 column:40), \"db has not been defined!\"", e.getMessage());
            }

            try
            {
                runtime.createInMemorySource("sourceId.pure", relationalDB2);
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:1 column:40), \"db has not been defined!\"", e.getMessage());
            }

            runtime.delete("sourceId.pure");
            runtime.createInMemorySource("sourceId.pure", relationalDB1);
            runtime.compile();
            Assertions.assertEquals(size, runtime.getModelRepository().serialize().length);
            db = processorSupport.package_getByUserPath("db");
            Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").size());
            Assertions.assertEquals(1, db.getValueForMetaPropertyToMany("schemas").getFirst().getValueForMetaPropertyToMany("tables").size());
        }
    }


    @Test
    public void testDatabaseWithSchema() throws Exception
    {
        String relationalDB = """
                ###Relational
                Database db
                (
                   Schema mySchema1\
                   (\
                       Table myTable
                       (
                           name VARCHAR(200) PRIMARY KEY
                       )\
                       View myView
                       (
                           myTableName : myTable.name
                       )
                   )\
                   Schema mySchema2\
                   (\
                       Table myTable2
                       (
                           name VARCHAR(200) PRIMARY KEY
                       )\
                       View myView2
                       (
                           myTableName : myTable2.name
                       )
                   )\
                )
                """;
        runtime.createInMemorySource("sourceId.pure", relationalDB);

        runtime.createInMemorySource("userId.pure", """
                function test():Boolean[1]{
                     assert(1 == db->meta::relational::metamodel::schema('default'), |'');
                     assert(1 == db->meta::relational::metamodel::schema('mySchema1').tables->size(), |'');
                     assert(1 == db->meta::relational::metamodel::schema('mySchema2').tables->size(), |'');
                }\
                """);

        runtime.compile();
        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(2, db.getValueForMetaPropertyToMany("schemas").size());
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:2 column:18), \"db has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", relationalDB);
            runtime.compile();
            db = processorSupport.package_getByUserPath("db");
            Assertions.assertEquals(2, db.getValueForMetaPropertyToMany("schemas").size());
            Assertions.assertEquals(size, runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testDatabaseWithSchemaAndJoins() throws Exception
    {
        String relationalDB = """
                ###Relational
                Database db
                (
                   Schema mySchema1\
                   (\
                       Table myTable
                       (
                           name VARCHAR(200) PRIMARY KEY
                       )\
                       View myView
                       (
                           myTableName : myTable.name
                       )
                   )\
                   Schema mySchema2\
                   (\
                       Table myTable2
                       (
                           name VARCHAR(200), id VARCHAR(200) PRIMARY KEY
                       )\
                       View myView2
                       (
                           myTableName : myTable2.name
                       )
                   )\
                   Join testJoin(mySchema1.myTable.name = mySchema2.myTable2.id)\
                )
                """;
        runtime.createInMemorySource("sourceId.pure", relationalDB);

        runtime.createInMemorySource("userId.pure", """
                function test():Boolean[1]{
                     assert([] == db->meta::relational::metamodel::schema('default'), |'');
                     assert(1 == db->meta::relational::metamodel::schema('mySchema1').tables->size(), |'');
                     assert(1 == db->meta::relational::metamodel::schema('mySchema2').tables->size(), |'');
                }\
                """);

        runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(2, db.getValueForMetaPropertyToMany("schemas").size());

        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:2 column:19), \"db has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", relationalDB);
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length);
            db = processorSupport.package_getByUserPath("db");
            Assertions.assertEquals(2, db.getValueForMetaPropertyToMany("schemas").size());
        }
    }

    @Test
    public void testDatabaseIncludes() throws Exception
    {
        String content = """
                ###Relational
                Database db1
                (
                   Table myTable1
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView1
                   (
                       myTableName : myTable1.name
                   )
                )
                ###Relational
                Database db2
                (
                   Table myTable2
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView2
                   (
                       myTableName : myTable2.name
                   )
                )
                """;
        runtime.createInMemorySource("sourceId.pure", content);
        runtime.createInMemorySource("userId.pure", """
                ###Relational
                Database db
                (\
                   include db1
                   include db2
                   Table myTable
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   Join myJoin(myTable1.name = myTable2.name)\
                )
                ###Pure
                function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}\
                """);

        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete("sourceId.pure");
            try
            {
                runtime.compile();
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Compilation error at (resource:userId.pure line:3 column:13), \"db1 has not been defined!\"", e.getMessage());
            }

            runtime.createInMemorySource("sourceId.pure", content);
            runtime.compile();
            Assertions.assertEquals(size, runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testDatabaseIncludesWithExplicitDBReferenceInJoin() throws Exception
    {
        String content1 = """
                ###Relational
                Database test::db1
                (
                   Table myTable1
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   Table myTable2
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView1
                   (
                       myTableName : myTable1.name
                   )
                )
                """;
        String content2 = """
                ###Relational
                Database test::db1
                (
                   Table myTable1
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   Table myTable2
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   Table myTable3
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   View myView1
                   (
                       myTableName : myTable1.name
                   )
                )
                """;
        runtime.createInMemorySource("sourceId.pure", content1);
        runtime.createInMemorySource("userId.pure", """
                ###Relational
                Database test::db
                (\
                   include test::db1
                   Table myTable
                   (
                       name VARCHAR(200) PRIMARY KEY
                   )
                   Join myJoin([test::db1]myTable1.name = myTable.name)\
                )
                ###Pure
                import test::*;
                function test():Boolean[1]
                {
                  assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');
                }
                """);

        runtime.compile();
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.modify("sourceId.pure", content2);
            runtime.compile();
            runtime.modify("sourceId.pure", content1);
            runtime.compile();
            Assertions.assertEquals(size, runtime.getModelRepository().serialize().length);
        }
    }

    @Test
    public void testDatabaseWithIncludeAndSelfJoin()
    {
        String store1SourceId = "store1.pure";
        String store1Code = """
                ###Relational
                
                Database test::TopDB
                (
                )\
                """;
        String store2SourceId = "store2.pure";
        String store2Code = """
                ###Relational
                
                Database test::BottomDB
                (
                   include test::TopDB
                   Table employee(id INT, name VARCHAR(200), manager INT)
                   Join Managers(employee.manager = {target}.id)
                )\
                """;
        compileTestSource(store1SourceId, store1Code);
        compileTestSource(store2SourceId, store2Code);
        int size = repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete(store1SourceId);
            try
            {
                runtime.compile();
                Assertions.fail("Expected compilation error");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "test::TopDB has not been defined!", store2SourceId, 5, 18, e);
            }
            compileTestSource(store1SourceId, store1Code);
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDatabaseWithIncludeSelfJoinAndSchema()
    {
        String store1SourceId = "store1.pure";
        String store1Code = """
                ###Relational
                
                Database test::TopDB
                (
                )\
                """;
        String store2SourceId = "store2.pure";
        String store2Code = """
                ###Relational
                
                Database test::BottomDB
                (
                   include test::TopDB
                   Schema employees
                   (
                      Table employee(id INT, name VARCHAR(200), manager INT)
                   )
                   Join Managers(employees.employee.manager = {target}.id)
                )\
                """;
        compileTestSource(store1SourceId, store1Code);
        compileTestSource(store2SourceId, store2Code);
        int size = repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            runtime.delete(store1SourceId);
            try
            {
                runtime.compile();
                Assertions.fail("Expected compilation error");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "test::TopDB has not been defined!", store2SourceId, 5, 18, e);
            }
            compileTestSource(store1SourceId, store1Code);
            Assertions.assertEquals(size, repository.serialize().length, "Failed on iteration #" + i);
        }
    }

}
