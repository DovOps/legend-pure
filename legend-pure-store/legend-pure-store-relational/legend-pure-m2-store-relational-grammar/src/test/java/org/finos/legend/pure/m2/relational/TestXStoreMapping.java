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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestXStoreMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testXStoreMapping()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Relational
                   {
                      +id:String[1] : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person[e] : Relational
                   {
                      +firmId:String[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id,
                      employees[f1, e] : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingTypeError()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Relational
                   {
                      +id:String[1] : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person[e] : Relational
                   {
                      +firmId:Strixng[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id,
                      employees[f1, e] : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("""
                    Parser error at (resource:mapping.pure line:14), (Not Found: Strixng) in
                    '
                    Mapping FirmMapping
                    (
                       Firm[f1] : Relational
                       {
                          +id:String[1] : [db]FirmTable.id,
                          legalName : [db]FirmTable.legal_name
                       }
                      \s
                       Person[e] : Relational
                       {
                          +firmId:Strixng[1] : [db]PersonTable.firmId,
                          lastName : [db]PersonTable.lastName
                       }
                      \s
                       Firm_Person : XStore
                       {
                          firm[e, f1] : $this.firmId == $that.id,
                          employees[f1, e] : $this.id == $that.firmId
                       }
                    )'\
                    """, e.getMessage());
        }
    }

    @Test
    public void testXStoreMappingDiffMul()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Relational
                   {
                      +id:String[*] : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person[e] : Relational
                   {
                      +firmId:String[0..1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id->toOne(),
                      employees[f1, e] : $this.id->toOne() == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNotSetId()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm : Relational
                   {
                      +id:Integer[1] : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person : Relational
                   {
                      +firmId:Integer[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm : $this.firmId == $that.id,
                      employees : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNaturalProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   id : Integer[1];
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm : Relational
                   {
                      id : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person : Relational
                   {
                      +firmId:Integer[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm : $this.firmId == $that.id,
                      employees : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNaturalPropertyError()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class Firm
                {
                   id : Integer[1];
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm : Relational
                   {
                      id : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person : Relational
                   {
                      +firmId:Integer[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm : $this.firmId == $that.ixd,
                      employees : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:34 column:36), \"Can't find the property 'ixd' in the class Firm\"", e.getMessage());
        }

    }

    @Test
    public void testXStoreMappingNaturalPropertyUsingInheritance()
    {
        this.runtime.createInMemorySource("mapping.pure",
                """
                Class SuperFirm\
                {\
                   id : Integer[1];
                }\
                Class Firm extends SuperFirm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                
                Association Firm_Person
                {
                   firm : Firm[1];
                   employees : Person[*];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm : Relational
                   {
                      id : [db]FirmTable.id,
                      +xid:Integer[1] : [db]FirmTable.id,
                      legalName : [db]FirmTable.legal_name
                   }
                  \s
                   Person : Relational
                   {
                      +firmId:Integer[1] : [db]PersonTable.firmId,
                      lastName : [db]PersonTable.lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm : $this.firmId == $that.id,
                      employees : $this.id == $that.firmId
                   }
                )
                ###Relational
                Database db
                (
                   Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                )\
                """);
        this.runtime.compile();
    }


    @Test
    public void testXStoreMappingToMilestonedType()
    {
        this.runtime.createInMemorySource("mapping.pure",
                        """
                        Class Firm
                        {
                           legalName : String[1];
                        }
                        
                        Class <<temporal.businesstemporal>>Person
                        {
                           lastName : String[1];
                        }
                        
                        Association Firm_Person
                        {
                           firm : Firm[1];
                           employees : Person[*];
                        }
                        ###Mapping
                        Mapping FirmMapping
                        (
                           Firm : Relational
                           {
                              +id:Integer[1] : [db]FirmTable.id,
                              legalName : [db]FirmTable.legal_name
                           }
                          \s
                           Person : Relational
                           {
                              +firmId:Integer[1] : [db]PersonTable.firmId,
                              lastName : [db]PersonTable.lastName
                           }
                          \s
                           Firm_Person : XStore
                           {
                              firm : $this.firmId == $that.id,
                              employees : $this.id == $that.firmId
                           }
                        )
                        ###Relational
                        Database db
                        (
                           Table FirmTable (id INTEGER, legal_name VARCHAR(200))
                           Table PersonTable (firmId INTEGER, lastName VARCHAR(200))
                        )\
                        """);
        this.runtime.compile();
    }
}
