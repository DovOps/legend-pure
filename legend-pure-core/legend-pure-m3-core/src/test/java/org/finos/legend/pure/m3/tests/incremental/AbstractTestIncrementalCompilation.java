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

package org.finos.legend.pure.m3.tests.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestIncrementalCompilation extends AbstractPureTestWithCoreCompiled
{
    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository platform = repositories.detect(x -> x.getName().equals("platform"));
        CodeRepository core = new GenericCodeRepository("x_core", null, "platform");
        CodeRepository system = new GenericCodeRepository("system", null, "platform", "x_core");
        CodeRepository model = new GenericCodeRepository("model", null, "platform", "x_core", "system");
        CodeRepository other = new GenericCodeRepository("datamart_other", null, "platform", "x_core", "system", "model");
        repositories.add(core);
        repositories.add(system);
        repositories.add(model);
        repositories.add(other);
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("2.pure");
        runtime.delete("3.pure");
        runtime.delete("s1.pure");
        runtime.delete("s2.pure");
        runtime.delete("s3.pure");
        runtime.delete("s4.pure");
        runtime.delete("s5.pure");
        runtime.delete("sourceId1.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("sourceId3.pure");
        runtime.delete("/model/sourceId1.pure");
        runtime.delete("/model/domain/sourceId1.pure");
        runtime.delete("/model/domain/sourceId2.pure");
        runtime.delete("/model/domain/sourceId3.pure");
        runtime.delete("/datamart_other/sourceId1.pure");
        runtime.delete("/datamart_other/sourceId2.pure");
        runtime.delete("/datamart_other/domain/sourceId3.pure");
        runtime.delete("/system/tests/sourceId1.pure");
        runtime.delete("/system/tests/resources/sourceId2.pure");
        runtime.compile();
    }

    @Test
    public void test1()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                function myFunc1():Any[*]
                                {
                                   let a = 1;
                                }
                                
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   myFunc1();
                                   let obj = ^myClass( property1 = 0 );
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   myFunc2();
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                function myFunc1():Any[*]
                                {
                                   let a  1;
                                }
                                
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }\
                                """)
                        .compileWithExpectedParserFailureAndAssertions("expected: '}' found: 'a'", "sourceId1.pure", 3, 8, Lists.mutable.with("myFunc2__Any_MANY_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myClass", "myFunc1__Any_MANY_"))
                        .updateSource("sourceId1.pure", """
                                function myFunc1():Any[*]
                                {
                                   let a = 1;
                                }
                                
                                Class myClass
                                {
                                   property2 : Integer[1];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("The property 'property1' can't be found in the type 'myClass' or in its hierarchy.", "sourceId2.pure", 4, 24, Lists.mutable.with("myFunc2__Any_MANY_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myClass"))
                        .updateSource("sourceId1.pure", """
                                function myFunc1():Any[*]
                                {
                                   let a = 1;
                                }
                                
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test2()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc():Any[*]
                                {
                                   let obj = ^myClass( property1 = 10 );
                                   $obj.property1;
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                    assert(myFunc()==10, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Class myClass
                                {
                                   property1 : Integer[1];
                                   property2 : Undefined[1];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Undefined has not been defined!", "sourceId1.pure", 4, 16, Lists.mutable.with("start__Any_MANY_", "myFunc__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myClass"))
                        .updateSource("sourceId1.pure", """
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test3()
    {
        MutableList<String> processed = Lists.mutable.empty();
        MutableList<String> notProcessed = Lists.mutable.empty();

        processed.add("start__Any_MANY_");
        notProcessed.add("myFunc__Any_MANY_");

        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   MALE, FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0..1];
                                   gender      :   my::Gender[0..1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc():Any[*]
                                {
                                   let set =\s
                                   [
                                      ^my::Person(firstName = 'Marie', lastName='Random', gender = my::Gender.FEMALE),
                                      ^my::Person(firstName = 'John', lastName='Doe', gender = my::Gender.MALE)
                                   ];
                                   $set->filter(p|$p.gender == my::Gender.FEMALE).lastName;
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   assert( myFunc() == 'Random', |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0];
                                   gender      :   my::Gender[0..1];
                                }
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Multiplicity Error: [1] is not compatible with [0]", "sourceId2.pure", 5, 48, Lists.mutable.with("start__Any_MANY_", "myFunc__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("my::Person, my::Gender"))
                        .updateSource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   MALE, FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0..1];
                                   gender      :   my::Gender[0..1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test4()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                import my::pkg2::*;
                                
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                import my::pkg1::*;
                                import my::pkg2::*;
                                
                                function my::pkg4::myFunc():Any[*]
                                {
                                   let f = ^Firm( legalName='FirmX', employee = [^Person(name='David'),^Person(name='Pierre')]);
                                   $f.legalName;
                                }
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                    assert(my::pkg4::myFunc() == 'FirmX', |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                import my::pkg2::*;
                                import my::pkg5::*;
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .compile()
                        .updateSource("sourceId1.pure", """
                                import my::pkg2::*;
                                
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test5()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class my::pkg1::X
                                {
                                   propertyX : Integer[1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                Class my::pkg2::A extends my::pkg1::X
                                {
                                   propertyA : String[1];
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let a = ^my::pkg2::A( propertyA = '', propertyX = 0 );
                                   assert($a.propertyX == 0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Class my::pkg1::X
                                {
                                   propertyY : Integer[1];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'propertyX' in the class my::pkg2::A", "sourceId3.pure", 4, 14, Lists.mutable.with("my::pkg2::A"), Lists.mutable.empty(), Lists.mutable.with("my::pkg1::X"))
                        .updateSource("sourceId1.pure", """
                                Class my::pkg1::X
                                {
                                   propertyX : Integer[1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test6()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId1.pure", """
                                function myFunc1():Integer[1]
                                {
                                   1;
                                }\
                                """)
                        .createInMemorySource("/datamart_other/sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   assert(myFunc1() == 1, |'');
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   myFunc2();
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/model/sourceId1.pure", """
                                function myFunc():Integer[1]
                                {
                                   1;
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: myFunc1()", "/datamart_other/sourceId2.pure", 3, 11, Lists.mutable.with("myFunc2__Any_MANY_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myFunc__Any_MANY"))
                        .updateSource("/model/sourceId1.pure", """
                                function myFunc1():Integer[1]
                                {
                                   1;
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test7()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                import my::pkg2::*;
                                
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                import my::pkg1::*;
                                import my::pkg2::*;
                                
                                function my::pkg4::myFunc():Any[*]
                                {
                                   let f = ^Firm( legalName='FirmX', employee = [^Person(name='David'),^Person(name='Pierre')]);
                                   $f.legalName;
                                }
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                    assert(my::pkg4::myFunc() == 'FirmX', |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("""
                                Person has not been defined! The system found 1 possible matches:
                                    my::pkg2::Person\
                                """, "sourceId1.pure", 14, 15, Lists.mutable.with("my::pkg4::myFunc__Any_MANY_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("my::pkg1::Firm", "my::pkg2::Person", "my::pkg3::Firm_Person"))
                        .updateSource("sourceId1.pure", """
                                import my::pkg2::*;
                                
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }
                                
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : Person[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test8()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                function myFunc1():Any[*]
                                {
                                   myFunc2();
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   'inside myFunc2';
                                   'Parse error test';
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   myFunc1();
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   'inside myFunc2'
                                   'Parse error test';
                                }\
                                """)
                        .compileWithExpectedParserFailureAndAssertions("expected: '}' found: ''Parse error test''", "sourceId2.pure", 4, 4, Lists.mutable.with("myFunc1__Any_MANY_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myFunc2__Any_MANY_"))
                        .updateSource("sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   'inside myFunc2';
                                   'Parse error test';
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test9()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class myClass
                                {
                                   property1 : Integer[1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc():Integer[*]
                                {
                                   let obj = ^myClass(property1 = 0);
                                   $obj.property1;
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   assert(myFunc()==0, |'');
                                }
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId2.pure", """
                                function myFunc():Integer[*]
                                {
                                   let obj = ^myClass(property2 = 0);
                                   $obj.property2;
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'property2' in the class myClass", "sourceId2.pure", 4, 9, Lists.mutable.with("start__Any_MANY_", "myClass"), Lists.mutable.empty(), Lists.mutable.with("myFunc__Integer_MANY_"))
                        .updateSource("sourceId2.pure", """
                                function myFunc():Integer[*]
                                {
                                   let obj = ^myClass(property1 = 0);
                                   $obj.property1;
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test10()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   MALE, FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0..1];
                                   gender      :   my::Gender[0..1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                function myFunc():Any[*]
                                {
                                   let set =\s
                                   [
                                      ^my::Person(firstName = 'Marie', lastName='Random', gender = my::Gender.FEMALE),
                                      ^my::Person(firstName = 'John', lastName='Doe', gender = my::Gender.MALE)
                                   ];
                                   $set->filter(p|$p.gender == my::Gender.FEMALE).lastName;
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   assert( myFunc() == 'Random', |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0..1];
                                   gender      :   my::Gender[0..1];
                                }
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("The enum value 'MALE' can't be found in the enumeration my::Gender", "sourceId2.pure", 6, 75, Lists.mutable.with("start__Any_MANY_", "myFunc__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("my::Person", "my::Gender"))
                        .updateSource("sourceId1.pure", """
                                Enum my::Gender
                                {
                                   MALE, FEMALE
                                }
                                
                                Class my::Person
                                {
                                   firstName   :   String[0..1];
                                   lastName    :   String[0..1];
                                   gender      :   my::Gender[0..1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test11()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class my::pkg1::Firm
                                {
                                   legalName : String[0..1];
                                }
                                
                                Class my::pkg2::Person
                                {
                                   name : String[1];
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : my::pkg2::Person[*];
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                import my::pkg1::*;
                                import my::pkg2::*;
                                import my::pkg3::*;
                                
                                function start():Any[*]
                                {
                                   let f = ^Firm(legalName='FirmX', employee = [^Person(name='David'),^Person(name='Peter')]);
                                   assert($f.legalName == 'FirmX', |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("The property 'employee' can't be found in the type 'Firm' or in its hierarchy.", "sourceId3.pure", 7, 37, Lists.mutable.with("my::pkg1::Firm", "my::pkg2::Person"), Lists.mutable.empty(), Lists.mutable.with("my::pkg3::Firm_Person"))
                        .createInMemorySource("sourceId2.pure", """
                                Association my::pkg3::Firm_Person
                                {
                                   firm : my::pkg1::Firm[0..1];
                                   employee : my::pkg2::Person[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test12()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Car has not been defined!", "sourceId3.pure", 6, 25, Lists.mutable.with("my::enterprise::Color", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("my::enterprise::vehicle::Car", "my::enterprise::vehicle::MotorCycle"))
                        .createInMemorySource("sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test13()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE BLACK, WHITE
                                }\
                                """)
                        .compileWithExpectedParserFailureAndAssertions("expected: one of {'}', ','} found: 'BLACK'", "sourceId1.pure", 17, 14, Lists.mutable.with("my::enterprise::vehicle::Motorcycle", "start__Any_MANY_", "my::enterprise::vehicle::Car", "my::enterprise::Manufacture"), Lists.mutable.empty(), Lists.mutable.with("my::enterprise::Color", "my::enterprise::Vehicle"))
                        .updateSource("sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Disabled("Inconsistency of new operator in compiled vs interpreted mode")
    public void test14()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .createInMemorySource("sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   let maruti = ^Company(name='Maruti', vehicle = [$maruti_suzuki]);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunctionWithExpectedExecutionFailureandAssertions("start():Any[*]", "Error instantiating the type 'Car'. The property 'company' has a multiplicity range of [1] when the given list has a cardinality equal to 2", "sourceId3.pure", 7, 17, Lists.mutable.with("my::enterprise::vehicle::Motorcycle", "start__Any_MANY_", "my::enterprise::vehicle::Car", "my::enterprise::Manufacture"), Lists.mutable.empty(), Lists.mutable.empty())
                        .updateSource("sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test15()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/sourceId1.pure", """
                                function myFunc1():Integer[1]
                                {
                                   1;
                                }\
                                """)
                        .createInMemorySource("/datamart_other/sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   myFunc1();
                                }\
                                """)
                        .createInMemorySource("sourceId3.pure", """
                                function start():Any[*]
                                {
                                   assert(myFunc2() == 1, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/datamart_other/sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: myFunc2()", "sourceId3.pure", 3, 11, Lists.mutable.with("myFunc1__Integer_1_", "start__Any_MANY_"), Lists.mutable.empty(), Lists.mutable.with("myFunc2__Any_MANY_"))
                        .createInMemorySource("/datamart_other/sourceId2.pure", """
                                function myFunc2():Any[*]
                                {
                                   myFunc1();
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test16()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .createInMemorySource("/datamart_other/sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }
                                """)
                        .createInMemorySource("/datamart_other/domain/sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                   assert($maruti_suzuki.company.vehicle->at(0)->cast(@Car).numberOfWheels == 4, |'');
                                }
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/datamart_other/sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car\s
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle\s
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'company' in the class my::enterprise::vehicle::Car", "/datamart_other/domain/sourceId3.pure", 8, 26, Lists.mutable.with("start__Any_MANY_", "my::enterprise::Color", "my::enterprise::Company", "my::enterprise::Vehicle"), Lists.mutable.empty(), Lists.mutable.with("my::enterprise::vehicle::Car", "my::enterprise::vehicle::Motorcycle"))
                        .updateSource("/datamart_other/sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test17()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                import my::enterprise::*;
                                
                                Class my::enterprise::Company
                                {
                                   name : String[1];
                                }
                                
                                Association my::enterprise::Manufacture
                                {
                                   company : Company[1];
                                   vehicle : Vehicle[*];
                                }
                                
                                Enum my::enterprise::Color
                                {
                                   RED, BLUE, BLACK, WHITE
                                }\
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                import my::enterprise::vehicle::*;
                                import my::enterprise::*;
                                
                                function start():Any[*]
                                {\s
                                   let maruti_suzuki = ^Car(model='Suzuki', color = Color.BLUE, company = ^Company(name='Maruti'), cost = 10000.0, dateOfManufacture = %2015-01-09);
                                   assert($maruti_suzuki.discountedPrice == 9000.0, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/system/tests/sourceId1.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Vehicle has not been defined!", "/system/tests/resources/sourceId2.pure", 11, 14, Lists.mutable.with("start__Any_MANY_", "my::enterprise::Color", "my::enterprise::Company", "my::enterprise::Manufacture"), Lists.mutable.empty(), Lists.mutable.with("my::enterprise::vehicle::Car", "my::enterprise::vehicle::Motorcycle", "my::enterprise::Vehicle"))
                        .createInMemorySource("/system/tests/sourceId1.pure", """
                                Class my::enterprise::Vehicle
                                {
                                   horsePower : Integer[0..1];
                                   cost     : Float[1];
                                   dateOfManufacture : StrictDate[0..1];
                                   description   : String[*];
                                   color      : my::enterprise::Color[1..*];
                                }
                                
                                Class my::enterprise::vehicle::Car extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){4}:Integer[1];
                                   discountedPrice(){$this.cost*.9}:Float[1];
                                }
                                
                                Class my::enterprise::vehicle::Motorcycle extends my::enterprise::Vehicle
                                {
                                   model : String[1];
                                   numberOfWheels(){2}:Integer[1];
                                   discountedPrice(){$this.cost*.75}:Float[1];
                                }
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }


    @Test
    public void test18()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().deleteSource("/system/tests/resources/sourceId2.pure")
                        .compileWithExpectedCompileFailureAndAssertions("Can't find the property 'b' in the class A", "/model/domain/sourceId3.pure", 4, 17, Lists.mutable.with("start__Any_MANY_", "A", "B", "C"), Lists.mutable.empty(), Lists.mutable.with("AB", "AC"))
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test19()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[0];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Multiplicity Error: [1] is not compatible with [0]", "/model/domain/sourceId3.pure", 3, 19, Lists.mutable.with("start__Any_MANY_", "A", "B", "C", "AB", "AC"), Lists.mutable.empty(), Lists.mutable.empty())
                        .updateSource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Disabled("Inconsistency of new operator in compiled vs interpreted mode")
    public void test20()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                  \s
                                   let objC = ^C(a=$objA);
                                   assert($objC.a.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[1];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("Missing value(s) for required property 'propertyC1' which has a multiplicity of [1] for type C", "/model/domain/sourceId3.pure", 6, 15, Lists.mutable.with("start__Any_MANY_", "A", "B", "C", "AB", "AC"), Lists.immutable.empty(), Lists.immutable.empty())
                        .updateSource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    @Disabled("Inconsistency of new operator in compiled vs interpreted mode")
    public void test21()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                  \s
                                   let objC = ^C(a=$objA);
                                   assert($objC.a.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }
                                
                                Association BC
                                {
                                   b   :   B[1];
                                   c   :   C[*];
                                }\
                                """)
                        .executeFunctionWithExpectedExecutionFailureandAssertions("start():Any[*]", "Error instantiating class 'C'.  The following properties have multiplicity violations: 'b' requires 1 value, got 0", "/model/domain/sourceId3.pure", 6, 15, Lists.mutable.with("start__Any_MANY_", "A", "B", "C", "AB", "AC", "BC"), Lists.mutable.empty(), Lists.mutable.empty())
                        .updateSource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                  \s
                                   let objC = ^C(a=$objA, b=$objA.b);
                                   assert($objC.a.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]")
                        .updateSource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }\
                                """)
                        .updateSource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                  \s
                                   let objC = ^C(a=$objA);
                                   assert($objC.a.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test22()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .createInMemorySource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/system/tests/sourceId1.pure", """
                                Class my::pkgA::A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("A has not been defined! The system found 1 possible matches:\n    my::pkgA::A", "/system/tests/resources/sourceId2.pure", 3, 12, Lists.mutable.with("start__Any_MANY_", "AB"), Lists.mutable.empty(), Lists.mutable.with("A"))
                        .updateSource("/model/domain/sourceId3.pure", """
                                import my::pkgA::*;
                                
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                  \s
                                   let objC = ^C(a=$objA, b=$objA.b);
                                   assert($objC.a.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .updateSource("/system/tests/resources/sourceId2.pure", """
                                import my::pkgA::*;
                                
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                
                                Association AC
                                {
                                   a   :   A[1];
                                   c   :   C[*];
                                }
                                
                                Association BC
                                {
                                   b   :   B[1];
                                   c   :   C[*];
                                }\
                                """)
                        .executeFunction("start():Any[*]")
                        .updateSource("/system/tests/sourceId1.pure", """
                                Class A
                                {
                                   propertyA1 : Integer[*];
                                }
                                
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                Class C
                                {
                                   propertyC1 : String[0..1];
                                }\
                                """)
                        .updateSource("/system/tests/resources/sourceId2.pure", """
                                Association AB
                                {
                                   a   :   A[1];
                                   b   :   B[1];
                                }
                                """
                        )
                        .updateSource("/model/domain/sourceId3.pure", """
                                function start():Any[*]
                                {
                                   let objA = ^A(b=^B(propertyB1=%2018-01-01));
                                   assert($objA.b.propertyB1 == %2018-01-01, |'');
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test23()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("/model/domain/sourceId1.pure", """
                                Class <<temporal.businesstemporal>> A
                                {
                                   propertyA1 : Integer[*];
                                   qualifiedProp(){$this.b(%latest).propertyB1}:StrictDate[*];
                                }
                                
                                """)
                        .createInMemorySource("/model/domain/sourceId2.pure", """
                                Association <<temporal.businesstemporal>> AB
                                {
                                   a   :   A[1];
                                   b   :   B[*];
                                }
                                """)
                        .createInMemorySource("/model/domain/sourceId3.pure", """
                                Class <<temporal.businesstemporal>> B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                function start():Any[*]
                                {
                                   let objA = ^A(businessDate = [%2018-01-01]);
                                   assert($objA.businessDate == [%2018-01-01], |'');
                                  \s
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                new RuntimeTestScriptBuilder().updateSource("/model/domain/sourceId3.pure", """
                                Class B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                function start():Any[*]
                                {
                                   let objA = ^A(businessDate = [%2018-01-01]);
                                   assert($objA.businessDate == [%2018-01-01], |'');
                                  \s
                                }\
                                """)
                        .compileWithExpectedCompileFailureAndAssertions("The system can't find a match for the function: b(_:A[1],_:LatestDate[1])", "/model/domain/sourceId1.pure", 4, 26, Lists.mutable.with("A", "AB"), Lists.mutable.empty(), Lists.mutable.with("B"))
                        .updateSource("/model/domain/sourceId3.pure", """
                                Class <<temporal.businesstemporal>> B
                                {
                                   propertyB1 : StrictDate[1];
                                }
                                
                                function start():Any[*]
                                {
                                   let objA = ^A(businessDate = [%2018-01-01]);
                                   assert($objA.businessDate == [%2018-01-01], |'');
                                  \s
                                }\
                                """)
                        .executeFunction("start():Any[*]"),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test24()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("s1.pure", """
                                Class <<temporal.businesstemporal>> A {a: String[1];}\s
                                
                                 Class <<temporal.businesstemporal>> B {b: String[1];}\
                                """)
                        .createInMemorySource("3.pure", """
                                Class <<temporal.businesstemporal>> E {e: String[1];}\s
                                
                                 Class <<temporal.businesstemporal>> F {f: String[1];}
                                
                                Class <<temporal.businesstemporal>>G {g: String[1]; gQualified(){$this.g}:String[1];}
                                Association
                                myAssoc2{
                                 assoc2C: C[1];\s
                                 assocF: F[1];
                                }
                                
                                
                                Association
                                myAssoc3{
                                 assoc3F: F[1];\s
                                 assocG: G[1];
                                }\
                                """)
                        .createInMemorySource("2.pure", """
                                Class <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {
                                thisB: B[1];
                                 en: myEnum[1];
                                thisE: E[1];
                                thisE2: E[1];
                                thisE3: E[*];}
                                Enum myEnum{AAA,
                                BBB}
                                
                                Association
                                myAssoc{
                                 assocC: C[1];\s
                                 assocD: D[1];}
                                Class <<temporal.businesstemporal>> D {d: String[1];}\
                                """)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("2.pure", """
                                Class
                                 <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {
                                thisB: B[1];
                                 en: myEnum[1];
                                thisE: E[1];
                                thisE2: E[1];
                                thisE3: E[*];}
                                Enum myEnum{AAA,
                                BBB}
                                
                                Association
                                
                                myAssoc{
                                 assocC: C[1];\s
                                 assocD: D[1];}
                                Class <<temporal.businesstemporal>> D {d: String[1];}\
                                """)
                        .compile()
                        .updateSource("2.pure", """
                                Class <<temporal.businesstemporal>> {doc.doc='Hello'} C extends A {
                                thisB: B[1];
                                 en: myEnum[1];
                                thisE: E[1];
                                thisE2: E[1];
                                thisE3: E[*];}
                                Enum myEnum{AAA,
                                BBB}
                                
                                Association
                                myAssoc{
                                 assocC: C[1];\s
                                 assocD: D[1];}
                                Class <<temporal.businesstemporal>> D {d: String[1];}\
                                """)
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test25()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class <<temporal.businesstemporal>> A {a: String[1]; c: C[1];} \n Class B{hubA: A[1];}")
                        .createInMemorySource("s2.pure", "Class C{c: String[1];}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s2.pure", "Class\n C{c: String[1];}")
                        .compile()
                        .updateSource("s2.pure", "Class C{c: String[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test26()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class <<temporal.businesstemporal>> A {a: String[1];} \n Class  <<temporal.businesstemporal>> B{b: String[1];}\n Association AB {ab: B[1]; ba: A[1];}")
                        .createInMemorySource("s4.pure", "Class <<temporal.businesstemporal>> E {e: String[1];} ")
                        .createInMemorySource("s2.pure", "Class C{c: String[1]; a: A[1]; }")
                        .createInMemorySource("s3.pure", "Class <<temporal.businesstemporal>> D{c: C[1]; e:E[1];}")
                        .createInMemorySource("s5.pure", "function usage():Any[*]{{|D.all(%latest).e.e}}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s2.pure", "Class\n C{c: String[1]; a: A[1]; }")
                        .compile()
                        .updateSource("s2.pure", "Class C{c: String[1]; a: A[1];}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }

    @Test
    public void test27()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                        .createInMemorySource("s1.pure", "Class A {}")
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("s1.pure", "Class A {}\nClass A {}")
                        .compileWithExpectedParserFailure("The element 'A' already exists in the package '::'", "s1.pure", 2, 7)
                        .updateSource("s1.pure", "Class A {}")
                        .compile(),
                runtime, functionExecution, this.getAdditionalVerifiers());
    }
}
