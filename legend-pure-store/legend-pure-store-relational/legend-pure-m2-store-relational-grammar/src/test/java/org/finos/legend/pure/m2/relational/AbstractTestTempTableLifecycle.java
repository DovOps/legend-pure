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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractTestTempTableLifecycle extends AbstractPureTestWithCoreCompiled
{

    @Test
    public void testCreateTempTable()
    {
        Throwable exception = assertThrows(PureExecutionException.class, () -> {

            compileTestSource(
                    """
                    import meta::external::store::relational::runtime::*;
                    import meta::relational::metamodel::*;
                    import meta::relational::metamodel::execute::*;\
                    import meta::relational::functions::toDDL::*;\
                    function test():Any[0..1]
                    {
                       let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\
                       createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), \
                                       {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, \
                                       $dbConnection);\
                       let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\
                       let columnNames = $res.columnNames;\
                       print($columnNames, 1);\
                       assert('COL' == $columnNames, |'');\
                       dropTempTable('tt', $dbConnection);\
                       executeInDb('select * from tt', $dbConnection, 0, 1000);\
                    }
                    ###Relational
                    Database mydb()
                    """
            );
            try
            {
                compileAndExecute("test():Any[0..1]");
            }
            catch (PureExecutionException ex)
            {
                //expected
                Assertions.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
                throw ex;
            }
        });
        assertTrue(exception.getMessage().contains("Table \"TT\" not found"));
    }

    @Test
    public void testTempTableDroppedInFinally()
    {
        Throwable exception = assertThrows(PureExecutionException.class, () -> {

            compileTestSource(
                    """
                    import meta::external::store::relational::runtime::*;
                    import meta::relational::metamodel::*;
                    import meta::relational::metamodel::execute::*;\
                    function test():Any[0..1]
                    {
                       let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\
                       createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), \
                       {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, \
                       $dbConnection);\
                       let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\
                       let columnNames = $res.columnNames;\
                       print($columnNames, 1);\
                       assert('COL' == $columnNames, |'');\
                    }
                    ###Relational
                    Database mydb()
                    """
            );
            try
            {
                compileAndExecute("test():Any[0..1]");
            }
            catch (PureExecutionException ex)
            {
                //expected
                Assertions.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
                throw ex;
            }
        });
        assertTrue(exception.getMessage().contains("Temporary table: tt should be dropped explicitly"));
    }

    @Test
    public void testRelyOnFinallyTempTableFlow()
    {
        compileTestSource(
                """
                import meta::external::store::relational::runtime::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::execute::*;\
                function test():Any[0..1]
                {
                   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);\
                   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()), \
                   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE tt (col INT)'}, true,\
                   $dbConnection);\
                   let res = executeInDb('select * from tt', $dbConnection, 0, 1000);\
                   let columnNames = $res.columnNames;\
                   print($columnNames, 1);\
                   assert('COL' == $columnNames, |'');\
                }
                ###Relational
                Database mydb()
                """
        );
        try
        {
            compileAndExecute("test():Any[0..1]");
        }
        catch (PureExecutionException ex)
        {
            //expected
            Assertions.assertEquals("'COL'", this.functionExecution.getConsole().getLine(0));
            throw ex;
        }
    }
}
