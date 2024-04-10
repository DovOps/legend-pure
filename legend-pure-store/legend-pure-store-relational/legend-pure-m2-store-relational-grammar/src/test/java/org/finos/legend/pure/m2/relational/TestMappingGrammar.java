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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class TestMappingGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    RelationalGraphWalker graphWalker;

    @BeforeEach
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }


    @Test
    public void testColumnWithNoTable()
    {
        compileTestSource("model.pure",
                """
                Class Firm
                {
                  legalName : String[1];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database FirmDb
                (
                  Table FirmTable (legal_name VARCHAR(200))
                )\
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping FirmMapping
                    (
                      Firm : Relational
                             {
                                legalName : legal_name
                             }
                    )\
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing table or alias for column: legal_name", "mapping.pure", 6, 25, 6, 25, 6, 34, e);
        }
    }

    @Test
    public void testCombinationOfDistinctWithEmbeddedPropertyMappings()
    {
        compileTestSource("model.pure",
                """
                Class Firm
                {
                  legalName : String[1];
                  details : FirmDetails[0..1];
                }
                Class FirmDetails
                {
                  taxLocation : String[1];
                  extraDetails : FirmExtraDetails[1];
                }\
                Class FirmExtraDetails
                {
                  employeeCount : Integer[1];
                  taxLocation : String[1];
                }\
                """);

        compileTestSource("store.pure",
                """
                ###Relational
                Database FirmDb
                (
                  Table FirmTable (legal_name VARCHAR(200) PRIMARY KEY, tax_location VARCHAR(100), employee_count INTEGER)
                )\
                """);

        compileTestSource("mapping.pure",
                """
                ###Mapping
                Mapping FirmMapping
                (
                  Firm : Relational
                         {
                            ~distinct
                            scope([FirmDb]FirmTable)
                            (
                               legalName : legal_name,
                               details(taxLocation : tax_location, extraDetails(employeeCount : employee_count, taxLocation : tax_location))
                            )
                         }
                )\
                """);
        CoreInstance mapping = this.graphWalker.getMapping("FirmMapping");
        CoreInstance firmSetImpl = mapping.getValueForMetaPropertyToMany(M2MappingProperties.classMappings).getFirst();
        ListIterable<? extends CoreInstance> primaryKeys = firmSetImpl.getValueForMetaPropertyToMany(M2RelationalProperties.primaryKey);
        Assertions.assertEquals(3, primaryKeys.size());
        Assertions.assertFalse(primaryKeys.contains(null));
    }

    @Test
    public void testMappingWithIncludeError() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                  name:String[1];
                  firm:Firm[1];
                }
                Class Firm
                {
                  name:String[1];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database subDb
                (
                  Table personTb(name VARCHAR(200), firm VARCHAR(200))
                  Table firmTb(name VARCHAR(200))
                )
                ###Relational
                Database db
                (
                  include subDb
                )\
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        Firm[m1]: Relational
                                  {
                                     name : [db]firmTb.name
                                  }
                        Firm[m2]: Relational
                                  {
                                     name : [db]firmTb.name
                                  }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The class 'Firm' is mapped by 2 set implementations and has 0 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "mapping.pure", 2, 9, e);
        }
    }


    @Test
    public void testMappingWithIncludeErrorTooMany() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                  name:String[1];
                  firm:Firm[1];
                }
                Class Firm
                {
                  name:String[1];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database subDb
                (
                  Table personTb(name VARCHAR(200), firm VARCHAR(200))
                  Table firmTb(name VARCHAR(200))
                )
                ###Relational
                Database db
                (
                  include subDb
                )\
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        *Firm[m1]: Relational
                                   {
                                      name : [db]firmTb.name
                                   }
                        *Firm[m2]: Relational
                                   {
                                      name : [db]firmTb.name
                                   }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The class 'Firm' is mapped by 2 set implementations and has 2 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "mapping.pure", 2, 9, e);
        }
    }

    @Test
    public void testMappingDataTypeShouldNotMapToJoinError() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {\
                   bla : String[1];
                   name : String[1];
                }
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200),firm VARCHAR(200))
                   Table firmTb(name VARCHAR(200))
                   Table otherTb(name VARCHAR(200))
                   Join myJoin(personTb.firm = otherTb.name)
                )\
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        Person: Relational
                                {
                                    bla : [db]personTb.name,\
                                    name : [db]@myJoin
                                }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: The property 'name' returns a data type. However it's mapped to a Join.", "mapping.pure", 4, 5, e);
        }
    }

    @Test
    public void testMappingNonDataTypeShouldNotMapToColumn() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                   name:String[1];
                   other:Other[1];
                }
                Class Other
                {
                   name:String[1];
                }\
                """);

        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200),firm VARCHAR(200))
                   Table otherTb(name VARCHAR(200))
                   Join myJoin(personTb.firm = otherTb.name)
                )
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        Person: Relational
                                {
                                    other:[db]@myJoin|otherTb.name,
                                    name:[db]personTb.name
                                }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: The property 'other' doesn't return a data type. However it's mapped to a column or a function.", "mapping.pure", 4, 5, e);
        }
    }

    @Test
    public void testMappingJoinError() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                   name:String[1];
                   firm:Firm[1];
                   other:Other[1];
                }
                Class Firm
                {
                   name:String[1];
                }
                Class Other
                {
                   name:String[1];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200),firm VARCHAR(200))
                   Table firmTb(name VARCHAR(200))
                   Table otherTb(name VARCHAR(200))
                   Join myJoin(personTb.firm = otherTb.name)
                )
                """);

//        compileTestSource("mapping.pure",
//                "###Mapping\n" +
//                        "Mapping myMap(" +
//                        "    Firm[targetId]: Relational" +
//                        "          {" +
//                        "             name : [db]firmTb.name" +
//                        "          }" +
//                        "    Other: Relational" +
//                        "          {" +
//                        "             name : [db]otherTb.name" +
//                        "          }" +
//                        "    Person: Relational" +
//                        "            {" +
//                        "                firm[targetId]:[db]@myJoin," +
//                        "                other:[db]@myJoin," +
//                        "                name:[db]personTb.name" +
//                        "            }" +
//                        ")\n" +
//                "###Pure\n" +
//                "function test():Boolean[1]" +
//                "{" +
//                "   assertEquals('targetId', myMap.classMappingById('targetId')->cast(@meta::relational::mapping::RelationalInstanceSetImplementation).id);" +
//                "}");
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        Firm[targetId]: Relational
                              {
                                 name : [db]firmTb.name
                              }
                        Other: Relational
                              {
                                 name : [db]otherTb.name
                              }
                        Person: Relational
                                {
                                    firm[targetId]:[db]@myJoin,
                                    other:[db]@myJoin,
                                    name:[db]personTb.name
                                }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@myJoin does not connect from the source table [db]personTb to the target table [db]firmTb; instead it connects to [db]otherTb", "mapping.pure", 14, 37, e);
        }
    }

    @Test
    public void testMappingMultipleJoins() throws Exception
    {
        compileTestSource("mapping.pure",
                """
                Class Person{name:String[1];firm:Firm[1];other:Other[1];}\
                Class Firm{name:String[1];}\
                Class Other{name:String[1];}
                ###Relational
                Database db(Table personTb(name VARCHAR(200),firm VARCHAR(200))\
                            Table firmTb(name VARCHAR(200))\
                            Table otherTb(name VARCHAR(200))\
                            Join myJoin(personTb.firm = otherTb.name)
                            Join otherJoin(otherTb.name = firmTb.name))
                ###Mapping
                Mapping myMap(\
                    Firm: Relational
                          {\
                             name : [db]firmTb.name
                          }
                    Person: Relational\
                            {\
                                firm:[db]@myJoin > @otherJoin,
                                name:[db]personTb.name
                            }
                )
                """);
        // TODO add asserts
        CoreInstance mapping = this.graphWalker.getMapping("myMap");
        Assertions.assertNotNull(mapping);
        Assertions.assertNotNull(mapping.getSourceInformation());
        Assertions.assertEquals(6, mapping.getSourceInformation().getStartLine());
        Assertions.assertEquals(12, mapping.getSourceInformation().getEndLine());
        CoreInstance classMapping = this.graphWalker.getClassMapping(mapping, "Firm");
        Assertions.assertNotNull(classMapping);
        Assertions.assertNotNull(classMapping.getSourceInformation());
        Assertions.assertEquals(6, classMapping.getSourceInformation().getStartLine());
        Assertions.assertEquals(8, classMapping.getSourceInformation().getEndLine());
    }

    @Test
    public void testMappingMultipleJoinsError() throws Exception
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                   name:String[1];
                   firm:Firm[1];
                   other:Other[1];
                }
                Class Firm
                {
                   name:String[1];
                }
                Class Other
                {
                   name:String[1];
                }
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200), firm VARCHAR(200))
                   Table firmTb(name VARCHAR(200))
                   Table otherTb(name VARCHAR(200))
                   Table otherTb2(name VARCHAR(200))
                   Join myJoin(personTb.firm = otherTb.name)
                   Join otherJoin(otherTb2.name = firmTb.name)
                )
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap
                    (
                        Firm: Relational
                              {
                                 name : [db]firmTb.name
                              }
                        Person: Relational
                                {
                                    firm:[db]@myJoin > @otherJoin,
                                    name:[db]personTb.name
                                }
                    )
                    """);
            Assertions.fail("Expected compile error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@otherJoin does not contain the source table [db]otherTb", "mapping.pure", 10, 37, e);
        }
    }

    @Test
    public void testErrorInCrossMapping()
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                   name:String[1];
                }
                Class Firm
                {
                   name:String[1];
                }
                Association aa
                {
                   firm:Firm[1];
                   employees:Person[*];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200),firmId INT)
                   Table firmTb(id INT, name VARCHAR(200))
                   Table otherTb(id INT, name VARCHAR(200))
                   Join myJoin(personTb.firmId = otherTb.id)
                )\
                """);
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    Mapping myMap1
                    (
                        Firm: Relational
                              {
                                 name : [db]firmTb.name,
                                 employees : [db]@myJoin
                              }
                    )
                    Mapping myMap2
                    (
                        Person: Relational
                                {
                                    firm:[db]@myJoin,
                                    name:[db]personTb.name
                                }
                    )
                    ###Mapping
                    Mapping myMap3(\
                      include myMap1\
                      include myMap2\
                    )\
                    """);
            Assertions.fail("Expected compile exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping error: the join [db]@myJoin does not contain the source table [db]firmTb", "mapping.pure", 7, 31, e);
        }
    }

    @Test
    public void testGoodCrossMapping()
    {
        compileTestSource("model.pure",
                """
                Class Person
                {
                   name:String[1];
                }
                Class Firm
                {
                   name:String[1];
                }
                Association aa
                {
                   firm:Firm[1];\
                   employees:Person[*];
                }\
                """);
        compileTestSource("store.pure",
                """
                ###Relational
                Database db
                (
                   Table personTb(name VARCHAR(200),firmId INT)
                   Table firmTb(id INT, name VARCHAR(200))
                   Join myJoin(personTb.firmId = firmTb.id)
                )\
                """);
        compileTestSource("mapping.pure",
                """
                ###Mapping
                Mapping myMap1
                (
                    Firm: Relational
                          {
                             name : [db]firmTb.name,
                             employees : [db]@myJoin
                          }
                )
                Mapping myMap2
                (
                    Person: Relational
                            {
                                firm:[db]@myJoin,
                                name:[db]personTb.name
                            }
                )
                Mapping myMap3
                (
                  include myMap1
                  include myMap2
                )
                """);

        CoreInstance myMap1 = runtime.getCoreInstance("myMap1");
        Assertions.assertNotNull(myMap1);

        CoreInstance myMap2 = runtime.getCoreInstance("myMap2");
        Assertions.assertNotNull(myMap2);

        CoreInstance myMap3 = runtime.getCoreInstance("myMap3");
        Assertions.assertNotNull(myMap3);

        ListIterable<? extends CoreInstance> myMap3Includes = Instance.getValueForMetaPropertyToManyResolved(myMap3, M3Properties.includes, processorSupport);
        Verify.assertSize(2, myMap3Includes);
        Assertions.assertSame(myMap1, Instance.getValueForMetaPropertyToOneResolved(myMap3Includes.get(0), M3Properties.included, processorSupport));
        Assertions.assertSame(myMap2, Instance.getValueForMetaPropertyToOneResolved(myMap3Includes.get(1), M3Properties.included, processorSupport));
    }

    @Test
    public void testIncludeSelf()
    {
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    import test::*;
                    
                    Mapping test::A
                    (
                      include A
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Circular include in mapping test::A: test::A -> test::A", "mapping.pure", 4, 15, e);
        }
    }

    @Test
    public void testIncludeLoop()
    {
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    import test::*;
                    
                    Mapping test::A
                    (
                      include C
                    )
                    Mapping test::B
                    (
                      include A
                    )
                    Mapping test::C
                    (
                      include B
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, Pattern.compile("Circular include in mapping ((test::A: test::A -> test::C -> test::B -> test::A)|(test::B: test::B -> test::A -> test::C -> test::B)|(test::C: test::C -> test::B -> test::A -> test::C))"), "mapping.pure", e);
        }
    }

    @Test
    public void testDoubleInclude()
    {
        try
        {
            compileTestSource("mapping.pure",
                    """
                    ###Mapping
                    import test::*;
                    
                    Mapping test::A
                    (
                    )
                    Mapping test::B
                    (
                      include A
                      include A
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mapping test::A is included multiple times in test::B", "mapping.pure", 7, 15, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitution()
    {
        compileTestSource("model.pure",
                """
                ###Pure
                
                Class class1
                {
                   id : Integer[1];
                }
                
                Class class2
                {
                   id : Integer[1];
                }
                """
        );

        compileTestSource("store.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Relational
                Database test::db2
                (
                  Table T2 (id INT)
                )
                
                ###Relational
                Database test::db3
                (
                  include test::db1
                  include test::db2
                )
                """);

        CoreInstance db1 = runtime.getCoreInstance("test::db1");
        Assertions.assertNotNull(db1);

        CoreInstance db2 = runtime.getCoreInstance("test::db2");
        Assertions.assertNotNull(db2);

        CoreInstance db3 = runtime.getCoreInstance("test::db3");
        Assertions.assertNotNull(db3);

        compileTestSource("mapping.pure",
                """
                ###Mapping
                import test::*;
                
                Mapping test::mapping12
                (
                   class1 : Relational
                   {
                      scope([db1]T1)
                      (
                         id : id
                      )
                   }
                  \s
                   class2 : Relational
                   {
                      scope([db2]T2)
                      (
                         id : id
                      )
                   }
                )
                
                Mapping test::mapping3
                (
                   include mapping12[db1 -> db3, db2 -> db3]
                )
                """);

        CoreInstance mapping12 = runtime.getCoreInstance("test::mapping12");
        Assertions.assertNotNull(mapping12);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(mapping12, M3Properties.includes, processorSupport));

        CoreInstance mapping3 = runtime.getCoreInstance("test::mapping3");
        Assertions.assertNotNull(mapping3);

        ListIterable<? extends CoreInstance> includes = Instance.getValueForMetaPropertyToManyResolved(mapping3, M3Properties.includes, processorSupport);
        Verify.assertSize(1, includes);
        CoreInstance include = includes.get(0);
        Assertions.assertSame(mapping12, Instance.getValueForMetaPropertyToOneResolved(include, M3Properties.included, processorSupport));

        ListIterable<? extends CoreInstance> storeSubstitutions = Instance.getValueForMetaPropertyToManyResolved(include, M2MappingProperties.storeSubstitutions, processorSupport);
        Verify.assertSize(2, storeSubstitutions);
        CoreInstance storeSub1 = storeSubstitutions.get(0);
        Assertions.assertSame(db1, Instance.getValueForMetaPropertyToOneResolved(storeSub1, M2MappingProperties.original, processorSupport));
        Assertions.assertSame(db3, Instance.getValueForMetaPropertyToOneResolved(storeSub1, M2MappingProperties.substitute, processorSupport));

        CoreInstance storeSub2 = storeSubstitutions.get(1);
        Assertions.assertSame(db2, Instance.getValueForMetaPropertyToOneResolved(storeSub2, M2MappingProperties.original, processorSupport));
        Assertions.assertSame(db3, Instance.getValueForMetaPropertyToOneResolved(storeSub2, M2MappingProperties.substitute, processorSupport));
    }

    @Test
    public void testIncludeWithStoreSubstitutionToNonStore()
    {
        compileTestSource("stores.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Pure
                Class test::db2
                {
                }\
                """);
        compileTestSource("mapping1.pure",
                """
                ###Mapping
                Mapping test::mapping1
                (
                )
                """);
        try
        {
            compileTestSource("mapping2.pure",
                    """
                    ###Mapping
                    import test::*;
                    Mapping test::mapping2
                    (
                       include mapping1[db1 -> db2]
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 is not a Store", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionFromNonStore()
    {
        compileTestSource("stores.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Pure
                Class test::db2
                {
                }\
                """);
        compileTestSource("mapping1.pure",
                """
                ###Mapping
                Mapping test::mapping1
                (
                )
                """);
        try
        {
            compileTestSource("mapping2.pure",
                    """
                    ###Mapping
                    import test::*;
                    Mapping test::mapping2
                    (
                       include mapping1[db2 -> db1]
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 is not a Store", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionWithoutStoreInclude()
    {
        compileTestSource("stores.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Relational
                Database test::db2
                (
                  Table T2 (id INT)
                )\
                """);
        compileTestSource("mapping1.pure",
                """
                ###Mapping
                Mapping test::mapping1
                (
                )
                """);
        try
        {
            compileTestSource("mapping2.pure",
                    """
                    ###Mapping
                    import test::*;
                    Mapping test::mapping2
                    (
                       include mapping1[db1 -> db2]
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 does not include test::db1", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithMultipleSubstitutionsForOneStore()
    {
        compileTestSource("stores.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Relational
                Database test::db2
                (
                  include test::db1
                  Table T2 (id INT)
                )
                
                ###Relational
                Database test::db3
                (
                  include test::db1
                  Table T3 (id INT)
                )\
                """);
        compileTestSource("mapping1.pure",
                """
                ###Mapping
                Mapping test::mapping1
                (
                )
                """);
        try
        {
            compileTestSource("mapping2.pure",
                    """
                    ###Mapping
                    import test::*;
                    Mapping test::mapping2
                    (
                       include mapping1[db1 -> db2, db1 -> db3]
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: multiple substitutions for test::db1", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testIncludeWithStoreSubstitutionsForSubstitutedStore()
    {
        compileTestSource("stores.pure",
                """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                )
                
                ###Relational
                Database test::db2
                (
                  include test::db1
                  Table T2 (id INT)
                )
                
                ###Relational
                Database test::db3
                (
                  include test::db2
                  Table T3 (id INT)
                )\
                """);
        compileTestSource("mapping1.pure",
                """
                ###Mapping
                Mapping test::mapping1
                (
                )
                """);
        try
        {
            compileTestSource("mapping2.pure",
                    """
                    ###Mapping
                    import test::*;
                    Mapping test::mapping2
                    (
                       include mapping1[db1 -> db2, db2 -> db3]
                    )\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Store substitution error: test::db2 appears both as an original and a substitute", "mapping2.pure", 5, 12, e);
        }
    }

    @Test
    public void testFiltersAreAvailableFromIncludedStore()
    {
        compileTestSource("stores.pure", """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                  Filter idFilter(T1.id = 0)\
                )
                ###Relational
                Database test::db2
                (
                  include test::db1
                  Table T2 (id INT)
                  Filter idFilter(T1.id != 0)\
                )
                ###Relational
                Database test::db3
                (
                  include test::db2
                )\
                """);
        compileTestSource("domain.pure", """
                ###Pure
                Class T1{id:Integer[1];}\
                """);
        compileTestSource("mapping1.pure", """
                ###Mapping
                import test::*;
                Mapping test::mapping
                (
                 T1[db3]: Relational{\
                 ~filter [db3]idFilter\
                ~mainTable[db3]T1\
                 id : [db3]T1.id\
                }\
                )
                """);
        CoreInstance testMapping = runtime.getCoreInstance("test::mapping");
        CoreInstance classMapping = Instance.getValueForMetaPropertyToOneResolved(testMapping, M2MappingProperties.classMappings, processorSupport);
        CoreInstance mainTableAlias = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.mainTableAlias, processorSupport);
        CoreInstance filterMapping = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.filter, processorSupport);
        CoreInstance op = Instance.getValueForMetaPropertyToOneResolved(filterMapping, M2RelationalProperties.filter, M2RelationalProperties.operation, processorSupport);
        Assertions.assertTrue(Instance.instanceOf(op, "meta::relational::metamodel::DynaFunction", processorSupport));


    }


    @Test
    public void testDefaultMainTableForIncludedStore()
    {
        compileTestSource("stores.pure", """
                ###Relational
                Database test::db1
                (
                  Table T1 (id INT)
                  Filter idFilter(T1.id = 0)\
                )
                ###Relational
                Database test::db2
                (
                  include test::db1
                  Table T2 (id INT)
                  Filter idFilter(T1.id != 0)\
                )
                ###Relational
                Database test::db3
                (
                  include test::db2
                )\
                """);
        compileTestSource("domain.pure", """
                ###Pure
                Class T1{id:Integer[1];}\
                """);
        compileTestSource("mapping1.pure", """
                ###Mapping
                import test::*;
                Mapping test::mapping
                (
                 T1[db3]: Relational{\
                 id : [db3]T1.id\
                }\
                )
                """);
        CoreInstance testMapping = runtime.getCoreInstance("test::mapping");
        CoreInstance classMapping = Instance.getValueForMetaPropertyToOneResolved(testMapping, M2MappingProperties.classMappings, processorSupport);
        CoreInstance mainTableAlias = Instance.getValueForMetaPropertyToOneResolved(classMapping, M2RelationalProperties.mainTableAlias, processorSupport);

        CoreInstance db3 = runtime.getCoreInstance("test::db3");
        CoreInstance database = Instance.getValueForMetaPropertyToOneResolved(mainTableAlias, M2RelationalProperties.database, processorSupport);

        Assertions.assertEquals(db3, database);
    }
}
