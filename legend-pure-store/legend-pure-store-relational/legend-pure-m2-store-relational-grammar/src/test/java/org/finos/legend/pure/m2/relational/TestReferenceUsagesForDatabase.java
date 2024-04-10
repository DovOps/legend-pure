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

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestReferenceUsagesForDatabase extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final Predicate NULL_SOURCE_INFORMATION = new Predicate<ReferenceUsage>()
    {
        @Override
        public boolean accept(ReferenceUsage referenceUsage)
        {
            return referenceUsage.getSourceInformation() == null;
        }
    };

    private static final HashingStrategy COMPARE_SOURCE_ID_LINE_COLUMN = new HashingStrategy<ReferenceUsage>()
    {
        @Override
        public int computeHashCode(ReferenceUsage referenceUsage)
        {
            return 0;
        }

        @Override
        public boolean equals(ReferenceUsage object1, ReferenceUsage object2)
        {
            return object1.getSourceInformation().getSourceId().equals(object2.getSourceInformation().getSourceId()) &&
                    object1.getSourceInformation().getLine() == object2.getSourceInformation().getLine() &&
                    object1.getSourceInformation().getColumn() == object2.getSourceInformation().getColumn();
        }
    };

    private static void createAndCompileSourceCode(PureRuntime runtime, String sourceId, String sourceCode)
    {
        runtime.delete(sourceId);
        runtime.createInMemorySource(sourceId, sourceCode);
        runtime.compile();
    }

    private static void assertDatabaseReferenceUsages(PureRuntime runtime, String sourceCode, String dbName, int dbCount)
    {
        String[] lines = sourceCode.split("\n");
        Database database = (Database)runtime.getCoreInstance("my::" + dbName);
        MutableList<? extends ReferenceUsage> databaseReferenceUsageList = Lists.mutable.ofAll(database._referenceUsages()).reject(NULL_SOURCE_INFORMATION).distinct(COMPARE_SOURCE_ID_LINE_COLUMN);
        Assertions.assertEquals(dbCount, databaseReferenceUsageList.size());
        for (ReferenceUsage referenceUsage : databaseReferenceUsageList)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assertions.assertEquals(dbName, lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + dbName.length() - 1));
        }
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithNoDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                Database my::mainDb
                (\s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson(PersonTable.firmId = FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnRightHandSide()
    {
        String sourceCode = """
                ###Relational
                Database my::mainDb
                (\s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson(PersonTable.firmId = [my::mainDb]FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnLeftHandSide()
    {
        String sourceCode = """
                ###Relational
                Database my::mainDb
                (\s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinWithDatabaseMarkerOnBothSides()
    {
        String sourceCode = """
                ###Relational
                Database my::mainDb
                (\s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 2);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinAcrossDatabasesWithDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::db1
                (
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                )
                
                ###Relational
                
                Database my::db2
                (
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join EmploymentJoin([my::db1]PersonTable.firmId = [my::db2]FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 1);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithJoinAcrossDatabasesWithNoDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::db1
                (
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                )
                
                ###Relational
                
                Database my::db2
                (
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join EmploymentJoin([my::db1]PersonTable.firmId = FirmTable.id)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 1);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithAllFourDatabaseMarkers()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and\s
                                   [my::mainDb]PersonTable.lastName = [my::mainDb]FirmTable.legalName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 4);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithThreeOutOfFourDatabaseMarkers()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and\s
                                   [my::mainDb]PersonTable.lastName = FirmTable.legalName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithTwoOutOfFourDatabaseMarkers()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = [my::mainDb]FirmTable.id and\s
                                   PersonTable.lastName = FirmTable.legalName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 2);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithOneOutOfFourDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson([my::mainDb]PersonTable.firmId = FirmTable.id and\s
                                   PersonTable.lastName = FirmTable.legalName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithComplexJoinWithNoDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson(PersonTable.firmId = FirmTable.id and\s
                                   PersonTable.lastName = FirmTable.legalName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsageForDatabaseWithSelfJoinWithDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Join DummySelfJoin([my::mainDb]PersonTable.firstName = {target}.lastName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 1);
    }

    @Test
    public void testReferenceUsageForDatabaseWithSelfJoinWithNoDatabaseMarker()
    {
        String sourceCode = """
                ###Relational
                
                Database my::mainDb
                ( \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Join DummySelfJoin(PersonTable.firstName = {target}.lastName)
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 0);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMapping()
    {
        String sourceCode = """
                Class my::Firm
                {
                   id :  Integer[1];
                   legalName : String[1];
                  \s
                }
                
                Class my::Person
                {
                   firstName : String[0..1];
                   lastName : String[0..1];
                }
                
                ###Relational
                
                Database my::mainDb
                (
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                )
                
                ###Mapping
                import my::*;
                Mapping my::mainMap
                (
                   Person : Relational
                   {
                      scope([mainDb]PersonTable)
                      (
                         firstName : firstName,
                         lastName : lastName
                      )
                   }
                  \s
                   Firm : Relational
                   {
                      id : [mainDb]FirmTable.id,
                      legalName : [mainDb]FirmTable.legalName
                     \s
                   }
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsageForDatabaseWithMappingWithMainTable()
    {
        String sourceCode = """
                Class my::Firm
                {
                   id :  Integer[1];
                   legalName : String[1];
                  \s
                }
                
                Class my::Person
                {
                   firstName : String[0..1];
                   lastName : String[0..1];
                }
                
                ###Relational
                
                Database my::mainDb
                (
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                )
                
                ###Mapping
                import my::*;
                
                Mapping my::mainMap
                (
                   Person : Relational
                   {
                      ~mainTable [mainDb] PersonTable
                      scope([mainDb]PersonTable)
                      (
                         firstName : firstName,
                         lastName : lastName
                      )
                   }
                  \s
                   Firm : Relational
                   {
                      id : [mainDb]FirmTable.id,
                      legalName : [mainDb]FirmTable.legalName
                     \s
                   }
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 4);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMappingWithFilter()
    {
        String sourceCode = """
                Class my::Person
                {
                   firstName : String[0..1];
                   lastName : String[0..1];
                }
                
                ###Relational
                
                Database my::mainDb
                (
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))
                   Filter PersonFilter(PersonTable.firstName = 'Utkarsh')
                )
                
                ###Mapping
                import my::*;
                
                Mapping my::mainMap
                (
                   Person : Relational
                   {
                      ~filter [mainDb] PersonFilter
                      firstName : [mainDb]PersonTable.firstName,
                      lastName : [mainDb]PersonTable.lastName
                     \s
                   }
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 3);
    }

    @Test
    public void testReferenceUsagesForDatabaseWithMappingWithInclude()
    {
        String sourceCode = """
                Class my::Firm
                {
                   id :  Integer[1];
                   legalName : String[1];
                  \s
                }
                
                Class my::Person
                {
                   firstName : String[0..1];
                   lastName : String[0..1];
                }
                
                ###Relational
                
                Database my::db1
                (
                   include my::db2
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200))
                )
                
                ###Relational
                
                Database my::db2
                (
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                )
                
                ###Mapping
                import my::*;
                
                Mapping my::map1
                (
                   include map2[db2->db1]
                   Person : Relational
                   {
                      scope([db1]PersonTable)
                      (
                         firstName : firstName,
                         lastName : lastName
                      )
                   }
                )
                
                Mapping my::map2  \s
                (
                   Firm : Relational
                   {
                      id : [db2]FirmTable.id,
                      legalName : [db2]FirmTable.legalName
                     \s
                   }
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db1", 2);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "db2", 4);
    }

    @Test
    public void testReferenceUsageForDatabaseWithAll()
    {
        String sourceCode = """
                import my::*;
                
                Class my::Firm
                {
                   legalName : String[1];
                   employees : Person[*];
                }
                
                Class my::Person
                {
                   firstName : String[0..1];
                   lastName : String[0..1];
                }
                
                Class my::Salary
                {
                   firstName : String[0..1];
                   salary : Integer[0..1];
                }
                
                ###Relational
                
                Database my::mainDb
                (
                   include my::subDb
                  \s
                   Table PersonTable(firstName VARCHAR(200), lastName VARCHAR(200), firmId INTEGER)
                   Table FirmTable(id INTEGER, legalName VARCHAR(200))
                   Join FirmPerson(PersonTable.firmId = FirmTable.id and [my::mainDb]PersonTable.lastName = [my::mainDb]FirmTable.legalName)
                )
                
                ###Relational
                
                Database my::subDb
                (
                   Table SalaryTable(firstName VARCHAR(200), salary INTEGER)
                   Filter SalaryFilter(SalaryTable.salary > 100000)
                )
                
                ###Relational
                
                Database my::tempDb
                (
                   Table TempTable(string1 VARCHAR(200), string2 VARCHAR(200))
                   Join SelfJoin([my::tempDb]TempTable.string1 = {target}.string2)
                )
                
                ###Mapping
                import my::*;
                
                Mapping my::mainMap
                (
                   include subMap[subDb->mainDb]
                   Person : Relational
                   {
                      ~mainTable [mainDb] PersonTable
                      scope([mainDb]PersonTable)
                      (
                         firstName : firstName,
                         lastName : lastName
                      )
                   }
                  \s
                   Firm : Relational
                   {
                      legalName : [mainDb]FirmTable.legalName,
                      employees : [mainDb]@FirmPerson
                     \s
                   }
                )
                
                Mapping my::subMap
                (
                   Salary : Relational
                   {
                      ~filter [subDb] SalaryFilter
                      firstName : [subDb]SalaryTable.firstName,
                      salary : [subDb]SalaryTable.salary
                     \s
                   }
                )
                """;
        createAndCompileSourceCode(this.runtime, "myFile.pure", sourceCode);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "mainDb", 7);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "subDb", 5);
        assertDatabaseReferenceUsages(this.runtime, sourceCode, "tempDb", 1);
    }
}
