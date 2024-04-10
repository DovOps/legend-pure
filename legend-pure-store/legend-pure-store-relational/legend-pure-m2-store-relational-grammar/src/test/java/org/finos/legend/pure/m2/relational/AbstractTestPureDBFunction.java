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

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public abstract class AbstractTestPureDBFunction extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testCreateTempTableError()
    {
        compileTestSource(
                """
                import meta::external::store::relational::runtime::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::execute::*;
                import meta::relational::functions::toDDL::*;
                function test():Any[0..1]
                {
                   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);
                   createTempTable('tt', ^Column(name='col', type=^meta::relational::metamodel::datatype::Integer()),\s
                                   {ttName:String[1], cols: Column[*], dbType: meta::relational::runtime::DatabaseType[1]| 'Create LOCAL TEMPORARY TABLE (col INT)'},\s
                                   $dbConnection);
                }
                ###Relational
                Database mydb()
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("""
                Error executing sql query; SQL reason: Syntax error in SQL statement "Create LOCAL TEMPORARY TABLE \\[\\*]\\(col INT\\) ?"; expected "identifier"; SQL statement:
                Create LOCAL TEMPORARY TABLE \\(col INT\\) \\[42001-\\d++]; SQL error code: 42001; SQL state: 42001\
                """), 8, 4, e);
    }

    @Test
    public void testDropTempTableError()
    {
        compileTestSource(
                """
                import meta::external::store::relational::runtime::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::execute::*;
                import meta::relational::functions::toDDL::*;
                function test():Any[0..1]
                {
                   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);
                   dropTempTable('tt', $dbConnection);
                }
                ###Relational
                Database mydb()
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("""
                Error executing sql query; SQL reason: Table "TT" not found; SQL statement:
                drop table tt \\[42102-\\d++]; SQL error code: 42102; SQL state: 42S02\
                """), 8, 4, e);
    }

    @Test
    public void testExecuteInDbError()
    {
        compileTestSource(
                """
                import meta::external::store::relational::runtime::*;
                import meta::relational::metamodel::*;
                import meta::relational::metamodel::execute::*;
                import meta::relational::functions::toDDL::*;
                function test():Any[0..1]
                {
                   let dbConnection = ^TestDatabaseConnection(type = meta::relational::runtime::DatabaseType.H2);
                   executeInDb('select * from tt', $dbConnection, 0, 1000);
                }
                ###Relational
                Database mydb()
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> compileAndExecute("test():Any[0..1]"));
        assertPureException(PureExecutionException.class, Pattern.compile("""
                Error executing sql query; SQL reason: Table "TT" not found \\(this database is empty\\); SQL statement:
                select \\* from tt \\[42104-\\d++]; SQL error code: 42104; SQL state: 42S04\
                """), 8, 4, e);
    }
}
