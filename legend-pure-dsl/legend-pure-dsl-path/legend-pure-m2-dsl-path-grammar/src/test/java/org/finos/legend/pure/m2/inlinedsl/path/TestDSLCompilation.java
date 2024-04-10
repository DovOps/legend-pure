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

package org.finos.legend.pure.m2.inlinedsl.path;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDSLCompilation extends AbstractPureTestWithCoreCompiled
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
    public void testSimple() throws Exception
    {
        try
        {
            runtime.createInMemorySource("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#/UnknownFirm/employees/address#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:12), \"UnknownFirm has not been defined!\"", e.getMessage());
        }

        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#/Firm/employees/address#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:12), \"Type argument mismatch for the class Firm<T> (expected 1, got 0): Firm\"", e.getMessage());
        }

        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#/Firm<BlaBla>/employees/address#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:18), \"BlaBla has not been defined!\"", e.getMessage());
        }


        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#/Firm<Any>/employee/address#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:23), \"The property 'employee' can't be found in the type 'Firm' (or any supertype).\"", e.getMessage());
        }

        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#/Firm/employees/address2#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:4 column:28), \"The property 'address2' can't be found in the type 'Person' (or any supertype).\"", e.getMessage());
        }

    }

    @Test
    public void testPathWithImports() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
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
                    print(#/Product/description#,2);
                    print(#/Product/synonymsByType(ProductSynonymType.CUSIP)/value!cusip#,2);
                }
                """);
        runtime.compile();
    }


    @Test
    public void testParameters() throws Exception
    {

        runtime.createInMemorySource("file.pure",
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
            runtime.createInMemorySource("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/Person/nameWithTitle()#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Error finding match for function 'nameWithTitle'. Incorrect number of parameters, function expects 1 parameters\"", e.getMessage());
        }


        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/Person/nameWithTitle(1)#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithTitle'. Expected:String, Found:Integer\"", e.getMessage());
        }

        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#/Person/nameWithTitle('1')#,2);
                }
                """);
        runtime.compile();
    }

    @Test
    public void testMultipleParameters() throws Exception
    {

        runtime.createInMemorySource("file.pure",
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

        runtime.createInMemorySource("function.pure",
                """
                function test():Any[*]
                {
                    print(#/Person/nameWithPrefixAndSuffix('a', 'b')#,2);
                }
                """);
        runtime.compile();


        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#/Person/nameWithPrefixAndSuffix('a', ['a', 'b'])#,2);
                }
                """);
        runtime.compile();

        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#/Person/nameWithPrefixAndSuffix([], ['a', 'b'])#,2);
                }
                """);
        runtime.compile();

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/Person/nameWithPrefixAndSuffix('a', [1, 2])#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer\"", e.getMessage());
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/Person/nameWithPrefixAndSuffix('a', [1, 'b'])#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any\"", e.getMessage());
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/Person/nameWithPrefixAndSuffix('a')#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters\"", e.getMessage());
        }
    }

    @Test
    public void testVisibility() throws Exception
    {
        try
        {
            runtime.createInMemorySource("file.pure",
                    """
                    Class <<access.private>> a::Person
                    {
                        firstName : String[1];
                    }
                    """);

            runtime.createInMemorySource("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#/a::Person/firstName#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:function.pure line:3 column:16), \"a::Person is not accessible in Root\"", e.getMessage());
        }
    }

    @Test
    public void testMapReturn() throws Exception
    {
        runtime.createInMemorySource("file.pure",
                """
                Class <<access.private>> Person
                {
                    firstName : String[1];\
                    stuff : Map<String, Integer>[1];
                }
                """);

        runtime.createInMemorySource("function.pure",
                """
                function test():Any[*]
                {
                    print(#/Person/stuff#,2);
                }
                """);
        runtime.compile();
    }
}
