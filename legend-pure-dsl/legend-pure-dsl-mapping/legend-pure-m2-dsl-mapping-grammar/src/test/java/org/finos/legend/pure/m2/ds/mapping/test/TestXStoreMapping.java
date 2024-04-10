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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestXStoreMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
    }

    @Test
    public void testXStoreMapping()
    {
        String source = """
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
                Class SrcFirm
                {
                   _id : String[1];
                   _legalName : String[1];
                }
                
                Class SrcPerson
                {
                   _lastName : String[1];
                   _firmId : String[1];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Pure
                   {
                      ~src SrcFirm \
                      +id:String[1] : $src._id,
                      legalName : $src._legalName
                   }
                  \s
                   Person[e] : Pure
                   {
                      ~src SrcPerson\
                      +firmId:String[1] : $src._firmId,
                      lastName : $src._lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id,
                      employees[f1, e] : $this.id == $that.firmId
                   }
                )
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_Person");
    }

    @Test
    public void testXStoreMappingTypeError()
    {
        runtime.createInMemorySource("mapping.pure",
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
                Class SrcFirm
                {
                   _id : String[1];
                   _legalName : String[1];
                }
                
                Class SrcPerson
                {
                   _lastName : String[1];
                   _firmId : String[1];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Pure
                   {
                      ~src SrcFirm \
                      +id:String[1] : $src._id,
                      legalName : $src._legalName
                   }
                  \s
                   Person[e] : Pure
                   {
                      ~src SrcPerson\
                      +firmId:Strixng[1] : $src._firmId,
                      lastName : $src._lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id,
                      employees[f1, e] : $this.id == $that.firmId
                   }
                )
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:38 column:35), \"Strixng has not been defined!\"", e.getMessage());
        }
    }

    @Test
    public void testXStoreMappingDiffMul()
    {
        String source = """
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
                Class SrcFirm
                {
                   _id : String[1];
                   _legalName : String[1];
                }
                
                Class SrcPerson
                {
                   _lastName : String[1];
                   _firmId : String[1];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Pure
                   {
                      ~src SrcFirm \
                      +id:String[*] : $src._id,
                      legalName : $src._legalName
                   }
                  \s
                   Person[e] : Pure
                   {
                      ~src SrcPerson\
                      +firmId:String[0..1] : $src._firmId,
                      lastName : $src._lastName
                   }
                  \s
                   Firm_Person : XStore
                   {
                      firm[e, f1] : $this.firmId == $that.id->toOne(),
                      employees[f1, e] : $this.id->toOne() == $that.firmId
                   }
                )
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_Person");
    }


    @Test
    public void testXStoreAssociationSubtypeMapping()
    {
        String source = """
                Class Firm
                {
                   legalName : String[1];
                }
                
                Class Person
                {
                   lastName : String[1];
                }
                Class MyPerson extends Person
                {
                }
                
                Association Firm_MyPerson
                {
                   firm : Firm[1];
                   employees : MyPerson[*];
                }
                Class SrcFirm
                {
                   _id : String[1];
                   _legalName : String[1];
                }
                
                Class SrcPerson
                {
                   _lastName : String[1];
                   _firmId : String[1];
                }
                ###Mapping
                Mapping FirmMapping
                (
                   Firm[f1] : Pure
                   {
                      ~src SrcFirm \
                      +id:String[*] : $src._id,
                      legalName : $src._legalName
                   }
                  \s
                   Person : Pure
                   {
                      ~src SrcPerson\
                      +firmId:String[0..1] : $src._firmId,
                      lastName : $src._lastName
                   }
                  \s
                   MyPerson : Pure
                   {
                      ~src SrcPerson\
                      +firmId:String[0..1] : $src._firmId,
                      lastName : $src._lastName
                   }
                  \s
                   Firm_MyPerson : XStore
                   {
                      firm[MyPerson, f1] : $this.firmId == $that.id->toOne(),
                      employees[f1, MyPerson] : $this.id->toOne() == $that.firmId
                   }
                )
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_MyPerson");
    }
}
