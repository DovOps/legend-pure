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

package org.finos.legend.pure.m3.tests.treepath;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// TODO: Investigate tests with setUp() added, those are causing other tests fail when runtime is shared
public class TestTreePathCompilation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }


    @Test
    public void testExceptionScenarios() throws Exception
    {
        try
        {
            runtime.createInMemorySource("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#UnknownFirm\
                              { \
                                 *\
                              }#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("UnknownFirm has not been defined!", 4, 12, e);
        }

        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#Firm\
                              {\
                                   *\
                              }#,1)
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Type argument mismatch for the class Firm<T> (expected 1, got 0): Firm", 4, 12, e);
        }


        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#Firm<BlaBla>{\
                       *\
                    }#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("TreePath doesn't support GenericTypes", 4, 12, e);
        }

        try
        {
            final String code = """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#Firm{
                                     +[employee] \s
                                   }#,2);
                    }
                    """;
            runtime.modify("file.pure", code);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {

            assertPureException("The property 'employee' can't be found in the type 'Firm' (or any supertype).", 5, 20, e);
        }

        try
        {
            runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    function test():Any[*]
                    {
                        print(#
                               Firm
                               {
                                   employees\s
                                   {
                                       address2 {}\s
                                   }\
                               }\
                              #,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("The property 'address2' can't be found in the type 'Person' (or any supertype).", 9, 20, e);
        }
    }

    @Test
    public void testSimpleTreePath() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person\
                {\s
                             *   \
                             firm { employees as Person }\
                             address as MyKingdom { * }\
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, children.size(), "Missing children");

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(1, simpleProperties.size(), "Missing simpleProperties");

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assertions.assertEquals(1, firmChildren.size(), "Missing children");
        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assertions.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(1, simpleAddressProperties.size(), "Missing simpleProperties");

        setUp();
    }


    @Test
    public void testTreePathIncludeAll() throws Exception
    {
        String code = """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person\
                {\s
                             *   \
                             firm { employees as Person }\
                             address as MyKingdom {  }\
                }#;\
                }
                """;
        runtime.createInMemorySource("file.pure", code);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, children.size(), "Missing children");

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(1, simpleProperties.size(), "Invalid simpleProperties");

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assertions.assertEquals(1, firmChildren.size(), "Missing children");
        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assertions.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(0, simpleAddressProperties.size(), "Invalid simpleProperties");

        setUp();
    }

    @Test
    public void testSimpleTreePathWithDerivedProperties() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person\
                {\s
                      >theName[ $this.name ]   \
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing children");
        CoreInstance derivedProperty = children.getFirst();
        Assertions.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("propertyName").getName());
    }

    @Test
    public void testSimpleTreePathWithDerivedLiteralProperties() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person\
                {\s
                      >theName[ $this.name ]   \
                      >lit[ 'string' ]   \
                      >street[ $this.address.street ]   \
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        ListIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport).toList();
        Assertions.assertEquals(3, children.size(), "Missing children");
        CoreInstance derivedProperty = children.getFirst();
        Assertions.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("theName", derivedProperty.getValueForMetaPropertyToOne("propertyName").getName());

        Assertions.assertEquals("lit", children.get(1).getValueForMetaPropertyToOne("name").getName());
        final CoreInstance street = children.get(2);
        Assertions.assertEquals("street", street.getValueForMetaPropertyToOne("name").getName());
        CoreInstance streetGT = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(street, M3Properties.specifications, processorSupport).getFirst(), M3Properties.genericType, processorSupport);
        CoreInstance streetRawType = Instance.getValueForMetaPropertyToOneResolved(streetGT, M3Properties.rawType, processorSupport);
        Assertions.assertEquals("String", streetRawType.getName());
    }

    @Test
    public void testTreePathWithSameTypeMultipleDefinitions() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1]; manager: Person[0..1]; } Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person as SP\
                {\s
                      *\s
                      manager as Manager
                 \
                      {    \s
                 \
                         +[name]   \s
                 \
                         address as BigHome\s
                 \
                      }    \s
                 \
                      address as Home\s
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, children.size(), "Missing children");
        CoreInstance manager = children.getFirst();
        CoreInstance rootAddress = children.getLast();

        Assertions.assertEquals("Home", rootAddress.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("address", rootAddress.getValueForMetaPropertyToOne("propertyName").getName());

        Assertions.assertEquals("Manager", manager.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("manager", manager.getValueForMetaPropertyToOne("propertyName").getName());

        RichIterable<? extends CoreInstance> managersChildren = Instance.getValueForMetaPropertyToManyResolved(manager, M3Properties.children, processorSupport);
        Assertions.assertEquals(1, managersChildren.size(), "Wrong number of children");

        CoreInstance address = managersChildren.getFirst();
        Assertions.assertEquals("BigHome", address.getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testTreePathWithSameTypeReferenced() throws Exception
    {
        runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1]; manager: Person[0..1]; } Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person as SP\
                {\s
                      *\s
                      manager as SP
                 \
                      address as Home\s
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, children.size(), "Missing children");
        CoreInstance manager = children.getFirst();
        CoreInstance rootAddress = children.getLast();

        Assertions.assertEquals("Home", rootAddress.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("address", rootAddress.getValueForMetaPropertyToOne("propertyName").getName());

        Assertions.assertEquals("SP", manager.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("manager", manager.getValueForMetaPropertyToOne("propertyName").getName());

        RichIterable<? extends CoreInstance> managersChildren = Instance.getValueForMetaPropertyToManyResolved(manager, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, managersChildren.size(), "Wrong number of children");

        CoreInstance managersManager = managersChildren.getFirst();
        Assertions.assertEquals("SP", managersManager.getValueForMetaPropertyToOne("name").getName());
        Assertions.assertEquals("manager", managersManager.getValueForMetaPropertyToOne("propertyName").getName());

        CoreInstance address = managersChildren.getLast();
        Assertions.assertEquals("Home", address.getValueForMetaPropertyToOne("name").getName());

        setUp();
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
                        print(#Person {+[nameWithTitle()]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithTitle'. Incorrect number of parameters, function expects 1 parameters", 3, 22, e);
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{+[nameWithTitle(Integer[1])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithTitle'. Expected:String, Found:Integer", 3, 21, e);
        }

        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#Person{+[nameWithTitle(String[1])]}#,2);
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
                   let t = #Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#;
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(4, properties.size());


        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);
                }
                """);
        runtime.compile();

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{+[nameWithPrefixAndSuffix(String[0..1], Integer[*])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer", 3, 21, e);
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{-[nameWithPrefixAndSuffix(String[0..1], Any[*])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any", 3, 21, e);
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{+[nameWithPrefixAndSuffix(String[0..1])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters", 3, 21, e);
        }

        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);
                }
                """);
        runtime.compile();
    }

    @Test
    public void testSimplePropertiesWithStereotypesAndTaggedValues() throws Exception
    {
        runtime.createInMemorySource("file.pure",
                """
                Profile m::p::TestProfile
                {
                   stereotypes : [ Root, NewProp, ExistingProp ];
                   tags : [ Id, Name, Description ];
                }
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
                    let t = #Person <<m::p::TestProfile.Root>> {m::p::TestProfile.Name = 'Stub_Person'} {+[nameWithPrefixAndSuffix(String[0..1], String[*]) <<m::p::TestProfile.ExistingProp>>]}#;
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");
        this.assertContainsStereoType(tree.getValueForMetaPropertyToMany(M3Properties.included).getFirst(), "ExistingProp");


        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#Person{-[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);
                }
                """);
        runtime.compile();

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{+[nameWithPrefixAndSuffix(String[0..1], Integer[*])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer", 3, 21, e);
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{-[nameWithPrefixAndSuffix(String[0..1], Any[*])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any", 3, 21, e);
        }

        try
        {
            runtime.modify("function.pure",
                    """
                    function test():Any[*]
                    {
                        print(#Person{+[nameWithPrefixAndSuffix(String[0..1])]}#,2);
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters", 3, 21, e);
        }

        runtime.modify("function.pure",
                """
                function test():Any[*]
                {
                    print(#Person{+[nameWithPrefixAndSuffix(String[0..1], String[*])]}#,2);
                }
                """);
        runtime.compile();
    }

    @Test
    public void testIncludedPropertiesHaveOwnerNode() throws Exception
    {
        runtime.createInMemorySource("file.pure",
                """
                Profile m::p::TestProfile
                {
                   stereotypes : [ Root, NewProp, ExistingProp ];
                   tags : [ Id, Name, Description ];
                }
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
                    employer: Organization[1];\
                }
                Class Organization
                {
                   legalName: String[1];\
                }\
                Class Team extends Organization
                {
                }\
                """);

        runtime.createInMemorySource("function.pure",
                """
                function test():Any[*]
                {
                    let t = #Person <<m::p::TestProfile.Root>> {m::p::TestProfile.Name = 'Stub_Person'} \
                { +[ firstName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'firstName'} , lastName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'lastName'} ]
                   employer \
                   {\
                       +[ legalName <<m::p::TestProfile.ExistingProp>> {m::p::TestProfile.Description = 'legalName'} ]\
                   }\
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");
        ListIterable<? extends CoreInstance> includedProperties = tree.getValueForMetaPropertyToMany(M3Properties.included);
        CoreInstance firstName = includedProperties.getFirst();
        this.assertContainsStereoType(firstName, "ExistingProp");
        this.assertContainsTaggedValue(firstName, "firstName");
        CoreInstance owner = firstName.getValueForMetaPropertyToOne(M3Properties.owner);
        Assertions.assertTrue(Instance.instanceOf(owner, M3Paths.RootRouteNode, processorSupport));
    }

    @Test
    public void testComplexPropertiesWithStereoTypesAndTaggedValues() throws Exception
    {
        runtime.createInMemorySource("file.pure",
                """
                Profile TestProfile
                {
                   stereotypes : [ Root, NewProp, ExistingProp ];
                   tags : [ Id, Name, Description ];
                }
                Class Person{ name: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                function test():Any[*]
                {
                 let t = \
                #Person<<TestProfile.Root>>{TestProfile.Name='Stub_Person'}\
                {\s
                             *   \
                             firm <<TestProfile.ExistingProp>>{TestProfile.Name='Stub_Firm'} { employees as Person <<TestProfile.ExistingProp>>{TestProfile.Name='Stub_Person'} }\
                             >myAddress [$this.address] <<TestProfile.ExistingProp>>{TestProfile.Name='MyKingdom'}{ * }\
                }#;\
                }
                """);
        runtime.compile();
        CoreInstance func = runtime.getCoreInstance("test__Any_MANY_");
        CoreInstance tree = func.getValueForMetaPropertyToMany("expressionSequence").getFirst().getValueForMetaPropertyToMany("parametersValues").getLast().getValueForMetaPropertyToMany("values").getFirst();

        Assertions.assertNotNull(tree);
        this.assertContainsStereoType(tree, "Root");
        this.assertContainsTaggedValue(tree, "Stub_Person");

        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.children, processorSupport);
        Assertions.assertEquals(2, children.size(), "Missing children");
        for (CoreInstance child : children)
        {
            this.assertContainsStereoType(child, "ExistingProp");
        }
        this.assertContainsTaggedValue(children.getFirst(), "Stub_Firm");
        this.assertContainsTaggedValue(children.getLast(), "MyKingdom");

        RichIterable<? extends CoreInstance> simpleProperties = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(1, simpleProperties.size(), "Missing simpleProperties");

        CoreInstance firm = children.getFirst();
        RichIterable<? extends CoreInstance> firmChildren = Instance.getValueForMetaPropertyToManyResolved(firm, M3Properties.children, processorSupport);
        Assertions.assertEquals(1, firmChildren.size(), "Missing children");
        CoreInstance employees = firmChildren.getFirst();
        this.assertContainsStereoType(employees, "ExistingProp");
        this.assertContainsTaggedValue(employees, "Stub_Person");

        CoreInstance address = children.getLast();
        RichIterable<? extends CoreInstance> addressChildren = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.children, processorSupport);
        Assertions.assertEquals(0, addressChildren.size());

        RichIterable<? extends CoreInstance> simpleAddressProperties = Instance.getValueForMetaPropertyToManyResolved(address, M3Properties.resolvedProperties, processorSupport);
        Assertions.assertEquals(1, simpleAddressProperties.size(), "Missing simpleProperties");

        setUp();
    }

    private void assertContainsTaggedValue(CoreInstance element, String tag)
    {
        RichIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(element, M3Properties.taggedValues, processorSupport);
        Assertions.assertTrue(taggedValues.size() > 0, "Missing Tagged Values");
        CoreInstance tv = taggedValues.getFirst();
        Assertions.assertEquals(tag, tv.getValueForMetaPropertyToOne(M3Properties.value).getName());
    }

    private void assertContainsStereoType(CoreInstance element, String stereoType)
    {
        RichIterable<? extends CoreInstance> stereoTypes = Instance.getValueForMetaPropertyToManyResolved(element, M3Properties.stereotypes, processorSupport);
        Assertions.assertTrue(stereoTypes.size() > 0, "Missing Stereotypes");
        Assertions.assertEquals(stereoType, stereoTypes.getFirst().getValueForMetaPropertyToOne(M3Properties.value).getName());
    }
}
