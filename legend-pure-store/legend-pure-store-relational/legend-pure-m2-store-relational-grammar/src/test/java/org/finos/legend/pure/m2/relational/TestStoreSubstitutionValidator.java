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

import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

public class TestStoreSubstitutionValidator extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String STORE_SOURCE_ID = "store.pure";
    private static final String MAPPING_SOURCE_ID = "mapping.pure";

    private static final String MODEL_CODE = """
            ###Pure
            import a::*;
            
            Class a::Person1
            {
               personId : Integer[1];
               personName : String[1];
               firmId : Integer[1];
            }
            
            Class a::Firm1
            {
               firmId : Integer[1];
               firmName : String[1];
            }
            
            Association a::PersonFirm1
            {
               person1 : Person1[*];
               firm1 : Firm1[0..1];
            }
            
            Class a::Person2
            {
               personId : Integer[1];
               personName : String[1];
               firmId : Integer[1];
            }
            
            Class a::Firm2
            {
               firmId : Integer[1];
               firmName : String[1];
            }
            
            Association a::PersonFirm2
            {
               person2 : Person2[*];
               firm2 : Firm2[0..1];
            }
            
            Class a::Person3
            {
               personId : Integer[1];
               personName : String[1];
               firmId : Integer[1];
            }
            
            Class a::Firm3
            {
               firmId : Integer[1];
               firmName : String[1];
            }
            
            Association a::PersonFirm3
            {
               person3 : Person3[*];
               firm3 : Firm3[0..1];
            }
            
            Class a::Person4
            {
               personId : Integer[1];
               personName : String[1];
               firmId : Integer[1];
            }
            
            Class a::Firm4
            {
               firmId : Integer[1];
               firmName : String[1];
            }
            
            Association a::PersonFirm4
            {
               person4 : Person4[*];
               firm4 : Firm4[0..1];
            }
            
            Class a::Person5
            {
               personId : Integer[1];
               personName : String[1];
               firmId : Integer[1];
            }
            
            Class a::Firm5
            {
               firmId : Integer[1];
               firmName : String[1];
            }
            
            Association a::PersonFirm5
            {
               person5 : Person5[*];
               firm5 : Firm5[0..1];
            }
            """;

    private static final String STORE_CODE = """
            ###Relational
            Database a::PersonFirmDatabase1
            (
               include a::PersonFirmDatabase2
               include a::PersonFirmDatabase3
               include a::PersonFirmDatabase4
               Table PersonTable1(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)
               Table FirmTable1(firmId INT PRIMARY KEY, firmName VARCHAR(20))
               Join PersonFirm1(PersonTable1.firmId = FirmTable1.firmId)
            )
            
            ###Relational
            Database a::PersonFirmDatabase2
            (
               include a::PersonFirmDatabase3
               include a::PersonFirmDatabase4
               Table PersonTable2(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)
               Table FirmTable2(firmId INT PRIMARY KEY, firmName VARCHAR(20))
               Join PersonFirm2(PersonTable2.firmId = FirmTable2.firmId)
            )
            
            ###Relational
            Database a::PersonFirmDatabase3
            (
               include a::PersonFirmDatabase5
               Table PersonTable3(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)
               Table FirmTable3(firmId INT PRIMARY KEY, firmName VARCHAR(20))
               Join PersonFirm3(PersonTable3.firmId = FirmTable3.firmId)
            )
            
            ###Relational
            Database a::PersonFirmDatabase4
            (
               Table PersonTable4(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)
               Table FirmTable4(firmId INT PRIMARY KEY, firmName VARCHAR(20))
               Join PersonFirm4(PersonTable4.firmId = FirmTable4.firmId)
            )
            
            ###Relational
            Database a::PersonFirmDatabase5
            (
               Table PersonTable5(personId INT PRIMARY KEY, personName VARCHAR(20), firmId INT)
               Table FirmTable5(firmId INT PRIMARY KEY, firmName VARCHAR(20))
               Join PersonFirm5(PersonTable5.firmId = FirmTable5.firmId)
            )
            """;

    @Test
    public void testValidNoStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping2
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping2
                ( \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                """;

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidDirectStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping23[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping23
                ( \s
                   include a::PersonFirmMapping3
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                  \s
                   PersonFirm3 : Relational
                   {
                      AssociationMapping
                      (
                         person3: [PersonFirmDatabase3]@PersonFirm3,
                         firm3: [PersonFirmDatabase3]@PersonFirm3
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping3
                ( \s
                   Person3: Relational
                   {
                      scope([PersonFirmDatabase3]PersonTable3)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm3: Relational
                   {
                      scope([PersonFirmDatabase3]FirmTable3)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                )
                """;

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidDoubleStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping2[PersonFirmDatabase2->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping2
                ( \s
                   include a::PersonFirmMapping3[PersonFirmDatabase3->PersonFirmDatabase2]
                )
                
                Mapping a::PersonFirmMapping3
                (\s
                   Person3: Relational
                   {
                      scope([PersonFirmDatabase3]PersonTable3)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm3: Relational
                   {
                      scope([PersonFirmDatabase3]FirmTable3)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm3 : Relational
                   {
                      AssociationMapping
                      (
                         person3: [PersonFirmDatabase3]@PersonFirm3,
                         firm3: [PersonFirmDatabase3]@PersonFirm3
                      )
                   }
                )
                """;

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidNestedStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping23[PersonFirmDatabase3->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping23
                ( \s
                   include a::PersonFirmMapping3
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping3
                (\s
                   Person3: Relational
                   {
                      scope([PersonFirmDatabase3]PersonTable3)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm3: Relational
                   {
                      scope([PersonFirmDatabase3]FirmTable3)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm3 : Relational
                   {
                      AssociationMapping
                      (
                         person3: [PersonFirmDatabase3]@PersonFirm3,
                         firm3: [PersonFirmDatabase3]@PersonFirm3
                      )
                   }
                )
                """;

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testValidHybridStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping234
                ( \s
                   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping45
                (\s
                   Person4: Relational
                   {
                      scope([PersonFirmDatabase4]PersonTable4)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm4: Relational
                   {
                      scope([PersonFirmDatabase4]FirmTable4)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm4 : Relational
                   {
                      AssociationMapping
                      (
                         person4: [PersonFirmDatabase4]@PersonFirm4,
                         firm4: [PersonFirmDatabase4]@PersonFirm4
                      )
                   }
                  \s
                   Person5: Relational
                   {
                      scope([PersonFirmDatabase5]PersonTable5)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm5: Relational
                   {
                      scope([PersonFirmDatabase5]FirmTable5)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm5 : Relational
                   {
                      AssociationMapping
                      (
                         person5: [PersonFirmDatabase5]@PersonFirm5,
                         firm5: [PersonFirmDatabase5]@PersonFirm5
                      )
                   }
                )
                """;

        this.verifyValidSubstitution(mappingCode);
    }

    @Test
    public void testInValidDirectStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping234[PersonFirmDatabase5->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping234
                ( \s
                   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping45
                (\s
                   Person4: Relational
                   {
                      scope([PersonFirmDatabase4]PersonTable4)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm4: Relational
                   {
                      scope([PersonFirmDatabase4]FirmTable4)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm4 : Relational
                   {
                      AssociationMapping
                      (
                         person4: [PersonFirmDatabase4]@PersonFirm4,
                         firm4: [PersonFirmDatabase4]@PersonFirm4
                      )
                   }
                  \s
                   Person5: Relational
                   {
                      scope([PersonFirmDatabase5]PersonTable5)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm5: Relational
                   {
                      scope([PersonFirmDatabase5]FirmTable5)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm5 : Relational
                   {
                      AssociationMapping
                      (
                         person5: [PersonFirmDatabase5]@PersonFirm5,
                         firm5: [PersonFirmDatabase5]@PersonFirm5
                      )
                   }
                )
                """;

        this.verifyInvalidSubstitution(mappingCode);
    }

    @Test
    public void testInValidDoubleStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase5->PersonFirmDatabase1, PersonFirmDatabase4->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping234
                ( \s
                   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping45
                (\s
                   Person4: Relational
                   {
                      scope([PersonFirmDatabase4]PersonTable4)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm4: Relational
                   {
                      scope([PersonFirmDatabase4]FirmTable4)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm4 : Relational
                   {
                      AssociationMapping
                      (
                         person4: [PersonFirmDatabase4]@PersonFirm4,
                         firm4: [PersonFirmDatabase4]@PersonFirm4
                      )
                   }
                  \s
                   Person5: Relational
                   {
                      scope([PersonFirmDatabase5]PersonTable5)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm5: Relational
                   {
                      scope([PersonFirmDatabase5]FirmTable5)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm5 : Relational
                   {
                      AssociationMapping
                      (
                         person5: [PersonFirmDatabase5]@PersonFirm5,
                         firm5: [PersonFirmDatabase5]@PersonFirm5
                      )
                   }
                )
                """;

        this.verifyInvalidSubstitution(mappingCode);
    }

    @Test
    public void testInValidNestedStoreSubstitution()
    {
        String mappingCode = """
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMapping1
                ( \s
                   include a::PersonFirmMapping234[PersonFirmDatabase2->PersonFirmDatabase1, PersonFirmDatabase3->PersonFirmDatabase1, PersonFirmDatabase5->PersonFirmDatabase1]
                  \s
                   Person1: Relational
                   {
                      scope([PersonFirmDatabase1]PersonTable1)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm1: Relational
                   {
                      scope([PersonFirmDatabase1]FirmTable1)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm1 : Relational
                   {
                      AssociationMapping
                      (
                         person1: [PersonFirmDatabase1]@PersonFirm1,
                         firm1: [PersonFirmDatabase1]@PersonFirm1
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping234
                ( \s
                   include a::PersonFirmMapping45[PersonFirmDatabase5->PersonFirmDatabase3]
                  \s
                   Person2: Relational
                   {
                      scope([PersonFirmDatabase2]PersonTable2)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm2: Relational
                   {
                      scope([PersonFirmDatabase2]FirmTable2)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm2 : Relational
                   {
                      AssociationMapping
                      (
                         person2: [PersonFirmDatabase2]@PersonFirm2,
                         firm2: [PersonFirmDatabase2]@PersonFirm2
                      )
                   }
                )
                
                Mapping a::PersonFirmMapping45
                (\s
                   Person4: Relational
                   {
                      scope([PersonFirmDatabase4]PersonTable4)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm4: Relational
                   {
                      scope([PersonFirmDatabase4]FirmTable4)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm4 : Relational
                   {
                      AssociationMapping
                      (
                         person4: [PersonFirmDatabase4]@PersonFirm4,
                         firm4: [PersonFirmDatabase4]@PersonFirm4
                      )
                   }
                  \s
                   Person5: Relational
                   {
                      scope([PersonFirmDatabase5]PersonTable5)
                      (
                        personId: personId,
                        personName: personName,
                        firmId: firmId
                      )
                   }
                  \s
                   Firm5: Relational
                   {
                      scope([PersonFirmDatabase5]FirmTable5)
                      (
                        firmId: firmId,
                        firmName: firmName
                      )
                   }
                  \s
                   PersonFirm5 : Relational
                   {
                      AssociationMapping
                      (
                         person5: [PersonFirmDatabase5]@PersonFirm5,
                         firm5: [PersonFirmDatabase5]@PersonFirm5
                      )
                   }
                )
                """;

        this.verifyInvalidSubstitution(mappingCode);
    }

    private void verifyValidSubstitution(String mappingCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingCode)
                        .compile()
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }

    private void verifyInvalidSubstitution(String mappingCode)
    {
        RuntimeVerifier.verifyOperationIsStable(
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MODEL_SOURCE_ID, MODEL_CODE)
                        .createInMemorySource(STORE_SOURCE_ID, STORE_CODE)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .createInMemorySource(MAPPING_SOURCE_ID, mappingCode)
                        .compileWithExpectedCompileFailure("Store Substitution Error in mapping [a::PersonFirmMapping1] as [a::PersonFirmDatabase5] does not exist in included mapping [a::PersonFirmMapping234]", MAPPING_SOURCE_ID, 6, 15)
                        .deleteSource(MAPPING_SOURCE_ID)
                        .compile(),
                this.runtime,
                this.functionExecution,
                this.getAdditionalVerifiers()
        );
    }
}
