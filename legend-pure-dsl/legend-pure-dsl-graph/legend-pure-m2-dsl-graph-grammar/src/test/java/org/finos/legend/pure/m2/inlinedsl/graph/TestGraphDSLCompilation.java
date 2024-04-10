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

package org.finos.legend.pure.m2.inlinedsl.graph;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGraphDSLCompilation extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }

    @Test
    public void testSimple()
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#{UnknownFirm{employees}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:13), \"UnknownFirm has not been defined!\"", e.getMessage());
        }
        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#{Firm{employees1}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:18), \"The system can't find a match for the property / qualified property: employees1()\"", e.getMessage());
        }
        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#{Firm{employees{address1}}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:28), \"The system can't find a match for the property / qualified property: address1()\"", e.getMessage());
        }

        this.runtime.modify("file.pure", """
                Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}
                function test():Any[*]
                {
                    print(#{Firm{employees{address}}}#,2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testAdvanced()
    {
        //SubType
        this.runtime.createInMemorySource("file.pure", """
                Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}
                function test():Any[*]
                {
                    print(#{Firm{employees{address->subType(@PersonAddress)}, address->subType(@FirmAddress)}}#,2);
                }
                """);
        this.runtime.compile();

        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}
                    function test():Any[*]
                    {
                        print(#{Firm{employees{address->subType(@PersonAddress)}, address->subType(@Person)}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:81), \"The type Person is not compatible with Address\"", e.getMessage());
        }

        //Alias
        this.runtime.modify("file.pure", """
                Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}
                function test():Any[*]
                {
                    print(#{Firm{employees{address->subType(@PersonAddress)}, 'firmAddress' : address->subType(@FirmAddress)}}#,2);
                }
                """);
        this.runtime.compile();

    }

    @Test
    public void testGraphWithImports()
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", """
                    import meta::relational::tests::mapping::enumeration::model::domain::*;
                    Class meta::relational::tests::mapping::enumeration::model::domain::Product
                    {
                       description: String[1];
                       synonyms: ProductSynonym[*];
                       synonymsByType(type:ProductSynonymType[1])
                       {
                          $this.synonyms->filter(s | $s.type == $type);
                       }:ProductSynonym[*];
                    }
                    Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym
                    {
                       type:ProductSynonymType[1];
                       value:String[1];
                    }
                    Enum meta::relational::tests::mapping::enumeration::model::domain::ProductSynonymType
                    {
                       CUSIP,
                       GS_NUMBER
                    }
                    function test():Any[*]
                    {
                        print(#{
                                   Product {
                                       description
                                   }
                               }#,2);
                        print(#{
                                   Product {
                                       synonymsByType1(ProductSynonymType.CUSIP) {
                                           value
                                       }
                                   }
                               }#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:30 column:20), \"The system can't find a match for the property / qualified property: synonymsByType1(_:ProductSynonymType[1])\"", e.getMessage());
        }

        this.runtime.modify("file.pure", """
                import meta::relational::tests::mapping::enumeration::model::domain::*;
                Class meta::relational::tests::mapping::enumeration::model::domain::Product
                {
                   description: String[1];
                   synonyms: ProductSynonym[*];
                   synonymsByType(type:ProductSynonymType[1])
                   {
                      $this.synonyms->filter(s | $s.type == $type);
                   }:ProductSynonym[*];
                   synonymsByTypeString(typeString:String[1])
                   {
                      $this.synonyms->filter(s | $s.type.name == $typeString)->toOne();
                   }:ProductSynonym[1];
                }
                Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym
                {
                   type:ProductSynonymType[1];
                   value:String[1];
                }
                Enum meta::relational::tests::mapping::enumeration::model::domain::ProductSynonymType
                {
                   CUSIP,
                   GS_NUMBER
                }
                function test():Any[*]
                {
                    print(#{
                               Product {
                                   description
                               }
                           }#,2);
                    print(#{
                               Product {
                                   'CUSIP_SYNONYM 1' : synonymsByType(ProductSynonymType.CUSIP) {
                                       value
                                   },
                                   'CUSIP_SYNONYM 2' : synonymsByTypeString('CUSIP') {
                                       value
                                   }
                               }
                           }#,2);
                }
                """);
        this.runtime.compile();
    }


    @Test
    public void testGraphPropertyParameters()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                Class Person
                {
                    firstName : String[1];
                    lastName : String[1];
                    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];\
                nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])
                    {
                        if($prefix->isEmpty(),
                           | if($suffixes->isEmpty(),
                                | $this.firstName + ' ' + $this.lastName,
                                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),
                           | if($suffixes->isEmpty(),
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))
                    }:String[1];\
                }
                """);

        try
        {
            this.runtime.createInMemorySource("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#{Person{nameWithTitle()}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithTitle()\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#{Person{nameWithTitle(1)}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithTitle(_:Integer[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithTitle('1')}}#,2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testMultipleParameters()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                Class Person
                {
                    firstName : String[1];
                    lastName : String[1];
                    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];\
                    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])
                    {
                        if($prefix->isEmpty(),
                           | if($suffixes->isEmpty(),
                                | $this.firstName + ' ' + $this.lastName,
                                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),
                           | if($suffixes->isEmpty(),
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))
                    }:String[1];\
                    memberOf(org:Organization[1]){true}:Boolean[1];\
                }
                Class Organization
                {
                }\
                Class Team extends Organization
                {
                }\
                """);

        this.runtime.createInMemorySource("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithPrefixAndSuffix('a', 'b')}}#,2);
                }
                """);
        this.runtime.compile();


        this.runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithPrefixAndSuffix('a', ['a', 'b'])}}#,2);
                }
                """);
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#{Person{nameWithPrefixAndSuffix('a', [1, 2])}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Integer[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#{Person{nameWithPrefixAndSuffix('a', [1, 'b'])}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Any[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#{Person{nameWithPrefixAndSuffix('a')}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithPrefixAndSuffix([], ['a', 'b'])}}#,2);
                }
                """);
        this.runtime.compile();
    }

    @Test
    public void testVariables()
    {
        this.runtime.createInMemorySource("file.pure",
                """
                Class Person
                {
                    firstName : String[1];
                    lastName : String[1];
                    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];\
                    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])
                    {
                        if($prefix->isEmpty(),
                           | if($suffixes->isEmpty(),
                                | $this.firstName + ' ' + $this.lastName,
                                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),
                           | if($suffixes->isEmpty(),
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,
                                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))
                    }:String[1];\
                    memberOf(org:Organization[1]){true}:Boolean[1];\
                }
                Class Organization
                {
                }\
                Class Team extends Organization
                {
                }\
                """);

        this.runtime.createInMemorySource("function.pure",
                """
                function test():Any[*]
                {
                    let var = 'x';
                    print(#{Person{nameWithPrefixAndSuffix($var, 'b')}}#,2);
                }
                """);
        this.runtime.compile();


        this.runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithPrefixAndSuffix('a', ['a', 'b'])}}#,2);
                }
                """);
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        let y = 2;
                        print(#{Person{nameWithPrefixAndSuffix('a', [1, $y])}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Integer[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        let y = 'b';
                        print(#{Person{nameWithPrefixAndSuffix('a', [1, $y])}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Any[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        let x = 'a';
                        print(#{Person{nameWithPrefixAndSuffix($x)}}#,2);
                    }
                    """);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#{Person{nameWithPrefixAndSuffix([], ['a', 'b'])}}#,2);
                }
                """);
        this.runtime.compile();

    }
}
