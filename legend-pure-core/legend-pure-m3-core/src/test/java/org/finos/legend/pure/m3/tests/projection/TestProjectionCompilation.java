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

package org.finos.legend.pure.m3.tests.projection;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestProjectionCompilation extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("projection.pure");
        runtime.delete("model.pure");
    }

    @Test
    public void testExceptionScenarios() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    Class PersonProjection projects
                    #
                        UnknownFirm\
                              { \
                                 *\
                              }
                    #
                    """);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("UnknownFirm has not been defined!", 4, 5, e);
        }

        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                    Class FirmProjection projects
                    #Firm\
                    {\
                       *\
                    }#
                    function test():Any[*]
                    {
                        print(FirmProjection,1)
                    }
                    """);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Type argument mismatch for the class Firm<T> (expected 1, got 0): Firm", 3, 2, e);
        }

        try
        {
            String code = """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    Class FirmProjection projects
                    #Firm{
                       +[employee] \s
                    }#
                    """;
            this.runtime.modify("file.pure", code);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {

            assertPureException("The property 'employee' can't be found in the type 'Firm' (or any supertype).", 4, 6, e);
        }

        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    Class MyFirm projects\s
                     #Firm
                    {
                       employees\s
                                   {
                                       address2 {}\s
                                   }\
                    }\
                    #
                    """);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("The property 'address2' can't be found in the type 'Person' (or any supertype).", 7, 20, e);
        }

        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    Class MyFirm projects\s
                     #Firm
                    {
                       employees\s
                                   {
                                       *\s
                                   }\
                    }\
                    #
                    """);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Invalid projection specification. Found complex property 'employees', only simple properties are allowed in a class projection.", 5, 4, e);
        }

        try
        {
            this.runtime.modify("file.pure", """
                    Class Person{ name: String[1]; nameWithTitle(title:String[1]){$title + $this.name}: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                    Class PersonProjection projects #Person\
                    {
                          +[ nameWithTitle(String[1]) ]  \s
                    }#\
                    """);
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Error compiling projection 'PersonProjection'. Property 'nameWithTitle' cannot be resolved due to underlying cause: Can't find the property 'name' in the class PersonProjection", 2, 7, e);
        }

        try
        {
            String code = """
                    Class Person{address:Address[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{}
                    Class FirmProjection projects
                    #Firm{
                       * \s
                    }#
                    Class FirmProjectionSubClass extends FirmProjection {}\
                    """;
            this.runtime.modify("file.pure", code);
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException("Class 'FirmProjection' is a projection and cannot be extended.", 6, 38, e);
        }
    }

    @Test
    public void testSimpleClassProjection() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; yearsEmployed : Integer[1]; address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                Class PersonProjection projects #Person\
                {\s
                   *   \
                }#\
                """);
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(2, properties.size(), "Missing properties");

        RichIterable<String> names = properties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(names.toList(), "name", "yearsEmployed");
    }

    @Test
    public void testSimpleClassProjectionWithoutDSL() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; yearsEmployed : Integer[1]; address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                Class PersonProjection projects Person\
                {\s
                   *   \
                }\
                """);
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(2, properties.size(), "Missing properties");

        RichIterable<String> names = properties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(names.toList(), "name", "yearsEmployed");
    }

    @Test
    public void testClassProjectionWithAnnotations() throws Exception
    {
        String annotations = """
                Profile TPP
                {
                   stereotypes:[Root, ExistingProperty, DerivedProperty, SimpleProperty]; \s
                   tags: [name, description];
                }
                """;
        this.runtime.createInMemorySource("file.pure", annotations + "Class {TPP.name='Person Class'} Person \n{\n {TPP.name = 'name prop'} name: String[1];\n <<TPP.SimpleProperty>> nameWithPrefix(prefix:String[1]){ $prefix + ' ' + $this.name;}:String[1];\n yearsEmployed : Integer[1];\n}\n" +
                "Class PersonProjection projects #Person <<TPP.Root>> {TPP.description = 'Person Class Projection'}" +
                "{ \n" +
                "   +[name {TPP.description='Full Name'}, nameWithPrefix(String[1]) <<TPP.ExistingProperty>>]   " +
                "}#");
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(projection);

        CoreInstance tppProfile = this.runtime.getCoreInstance("TPP");
        Assertions.assertNotNull(tppProfile);

        CoreInstance rootST = Profile.findStereotype(tppProfile, "Root");
        CoreInstance existingPropertyST = Profile.findStereotype(tppProfile, "ExistingProperty");
        CoreInstance derivedPropertyST = Profile.findStereotype(tppProfile, "DerivedProperty");
        CoreInstance simplePropertyST = Profile.findStereotype(tppProfile, "SimpleProperty");
        CoreInstance nameTag = Profile.findTag(tppProfile, "name");
        CoreInstance descriptionTag = Profile.findTag(tppProfile, "description");
        Assertions.assertNotNull(rootST);
        Assertions.assertNotNull(existingPropertyST);
        Assertions.assertNotNull(derivedPropertyST);
        Assertions.assertNotNull(simplePropertyST);
        Assertions.assertNotNull(nameTag);
        Assertions.assertNotNull(descriptionTag);

        CoreInstance nameProp = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport).getFirst();
        CoreInstance nameWithPrefixProp = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.qualifiedProperties, processorSupport).getFirst();
        Assertions.assertNotNull(nameProp);
        Assertions.assertNotNull(nameWithPrefixProp);

        validateAnnotations(projection, Sets.mutable.with(rootST), Maps.mutable.with(nameTag, Sets.mutable.with("Person Class"), descriptionTag, Sets.mutable.with("Person Class Projection")));
        validateAnnotations(nameProp, Sets.mutable.<CoreInstance>empty(), Maps.mutable.with(descriptionTag, Sets.mutable.with("Full Name"), nameTag, Sets.mutable.with("name prop")));
        validateAnnotations(nameWithPrefixProp, Sets.mutable.with(existingPropertyST, simplePropertyST), Maps.mutable.<CoreInstance, MutableSet<String>>empty());
    }

    @Test
    public void testClassProjectionWithoutDSLWithAnnotations() throws Exception
    {
        String annotations = """
                Profile TPP
                {
                   stereotypes:[Root, ExistingProperty, DerivedProperty, SimpleProperty]; \s
                   tags: [name, description];
                }
                """;
        this.runtime.createInMemorySource("file.pure", annotations + "Class {TPP.name='Person Class'} Person \n{\n {TPP.name = 'name prop'} name: String[1];\n <<TPP.SimpleProperty>> nameWithPrefix(prefix:String[1]){ $prefix + ' ' + $this.name;}:String[1];\n yearsEmployed : Integer[1];\n}\n" +
                "Class PersonProjection projects Person <<TPP.Root>> {TPP.description = 'Person Class Projection'}" +
                "{ \n" +
                "   +[name {TPP.description='Full Name'}, nameWithPrefix(String[1]) <<TPP.ExistingProperty>>]   " +
                "}");
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(projection);

        CoreInstance tppProfile = this.runtime.getCoreInstance("TPP");
        Assertions.assertNotNull(tppProfile);

        CoreInstance rootST = Profile.findStereotype(tppProfile, "Root");
        CoreInstance existingPropertyST = Profile.findStereotype(tppProfile, "ExistingProperty");
        CoreInstance derivedPropertyST = Profile.findStereotype(tppProfile, "DerivedProperty");
        CoreInstance simplePropertyST = Profile.findStereotype(tppProfile, "SimpleProperty");
        CoreInstance nameTag = Profile.findTag(tppProfile, "name");
        CoreInstance descriptionTag = Profile.findTag(tppProfile, "description");
        Assertions.assertNotNull(rootST);
        Assertions.assertNotNull(existingPropertyST);
        Assertions.assertNotNull(derivedPropertyST);
        Assertions.assertNotNull(simplePropertyST);
        Assertions.assertNotNull(nameTag);
        Assertions.assertNotNull(descriptionTag);

        CoreInstance nameProp = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport).getFirst();
        CoreInstance nameWithPrefixProp = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.qualifiedProperties, processorSupport).getFirst();
        Assertions.assertNotNull(nameProp);
        Assertions.assertNotNull(nameWithPrefixProp);

        validateAnnotations(projection, Sets.mutable.with(rootST), Maps.mutable.with(nameTag, Sets.mutable.with("Person Class"), descriptionTag, Sets.mutable.with("Person Class Projection")));
        validateAnnotations(nameProp, Sets.mutable.<CoreInstance>empty(), Maps.mutable.with(descriptionTag, Sets.mutable.with("Full Name"), nameTag, Sets.mutable.with("name prop")));
        validateAnnotations(nameWithPrefixProp, Sets.mutable.with(existingPropertyST, simplePropertyST), Maps.mutable.<CoreInstance, MutableSet<String>>empty());
    }

    private void validateAnnotations(CoreInstance instance, MutableSet<CoreInstance> expectedStereotypes, MutableMap<CoreInstance, MutableSet<String>> expectedTaggedValues)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.stereotypes, this.processorSupport);
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(instance, M3Properties.taggedValues, this.processorSupport);

        // Check that we have the expected stereotypes and tagged values
        MutableSetMultimap<CoreInstance, String> actualTaggedValues = Multimaps.mutable.set.empty();
        for (CoreInstance taggedValue : taggedValues)
        {
            CoreInstance tag = Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.tag, this.processorSupport);
            CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, this.processorSupport);
            actualTaggedValues.put(tag, PrimitiveUtilities.getStringValue(value));
        }

        Verify.assertSetsEqual(expectedStereotypes, stereotypes.toSet());
        Verify.assertMapsEqual(expectedTaggedValues, actualTaggedValues.toMap());

        // Check that the stereotypes and tags have the appropriate model elements
        for (CoreInstance stereotype : expectedStereotypes)
        {
            ListIterable<? extends CoreInstance> modelElements = Instance.getValueForMetaPropertyToManyResolved(stereotype, M3Properties.modelElements, this.processorSupport);
            if (!modelElements.contains(instance))
            {
                Assertions.fail("model elements for " + stereotype + " did not contain " + instance);
            }
        }
        for (CoreInstance tag : expectedTaggedValues.keysView())
        {
            ListIterable<? extends CoreInstance> modelElements = Instance.getValueForMetaPropertyToManyResolved(tag, M3Properties.modelElements, this.processorSupport);
            if (!modelElements.contains(instance))
            {
                Assertions.fail("model elements for " + tag + " did not contain " + instance);
            }
        }
    }

    @Test
    public void testClassProjectionFlattening() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; yearsEmployed : Integer[1]; address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                Class PersonProjection projects #Person\
                {\s
                   *\
                   >address [$this.address.street]\
                }#\
                """);
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(3, properties.size(), "Missing properties");

        RichIterable<String> names = properties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(names.toList(), "name", "yearsEmployed", "address");
    }

    @Test
    public void testClassProjectionWithFunctionRecompile() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class demo::A
                {
                
                    name()
                    {
                       $this->printName()->toOne();
                    }: String[1];
                }
                
                Class demo::AP projects\s
                #demo::A{
                  +[name()]\
                }
                #
                
                function printName(a:demo::A[1]):String[*]
                {  \s
                   $a->type().name;
                }
                
                function printName(a:demo::AP[1]):String[*]
                {  \s
                   'Projection ' +  $a->type().name->toOne();
                }\
                """);
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("demo::AP");

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(1, properties.size(), "Missing properties");

        String expressionVariableGenericType = properties.getFirst().getValueForMetaPropertyToMany(M3Properties.expressionSequence).getFirst().getValueForMetaPropertyToOne(M3Properties.parametersValues).getValueForMetaPropertyToOne(M3Properties.parametersValues).getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName();

        Assertions.assertEquals("AP", expressionVariableGenericType);
    }

    @Test
    public void testClassProjectionWithFunctionRecompileExceptionScenario() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class demo::A
                {
                    name()
                    {
                       $this->printName()->toOne();
                    }: String[1];
                }
                Class demo::AP projects\s
                #demo::A{
                  +[name()]\
                }
                #
                
                function printName(a:demo::A[1]):String[*]
                {  \s
                   $a->type().name;
                }
                
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException("Error compiling projection 'demo::AP'. Property 'name' cannot be resolved due to underlying cause: " + PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "printName(_:AP[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tprintName(A[1]):String[*]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, 8, 13, e);
        }
    }

    @Test
    public void testSimpleProjectionWithQualifiedProperties() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; nameWithTitle(title:String[1]){$title + $this.name}: String[1];address:Address[1]; firm: Firm[1];} Class Firm {employees : Person[1];address:Address[1];} Class Address{ street:String[1]; }
                Class PersonProjection projects #Person\
                {
                      +[ name, nameWithTitle(String[1]) ]  \s
                }#\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("nameWithTitle", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testSimpleProjectionWithQualifiedPropertiesIntLiteral() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; propInt(){1}: Integer[1];}\
                Class PersonProjection projects #Person\
                {
                      +[ name, propInt() ]  \s
                }#\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, this.processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("propInt", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testSimpleProjectionWithQualifiedPropertiesBooleanLiteral() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; propBool(){true}: Boolean[1];}\
                Class PersonProjection projects #Person\
                {
                      +[ propBool() ]  \s
                }#\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, this.processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("propBool", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testSimpleProjectionWithQualifiedPropertiesDateLiteral() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; propDate(){%12-12-12}: Date[1];}\
                Class PersonProjection projects #Person\
                {
                      +[ propDate() ]  \s
                }#\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, this.processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("propDate", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testSimpleProjectionWithQualifiedPropertiesEnumLiteral() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; propEnum(){NumNum.Cookie}: NumNum[1];}\
                Class PersonProjection projects #Person\
                {
                      +[ propEnum() ]  \s
                }#
                Enum NumNum\
                {\
                   Cookie, Monster\
                }\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("propEnum", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testSimpleProjectionWithQualifiedPropertiesLiteralArrayAny() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                Class Person{ name: String[1]; propManyAny(){[NumNum.Cookie, 1 , true, %12-12-12, 'pi', 3.14]}: Any[*];}\
                Class PersonProjection projects #Person\
                {
                      +[ propManyAny() ]  \s
                }#
                Enum NumNum\
                {\
                   Cookie, Monster\
                }\
                """);
        this.runtime.compile();
        CoreInstance tree = this.runtime.getCoreInstance("PersonProjection");
        Assertions.assertNotNull(tree);
        RichIterable<? extends CoreInstance> children = Instance.getValueForMetaPropertyToManyResolved(tree, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(1, children.size(), "Missing properties");
        Assertions.assertEquals("propManyAny", children.getFirst().getValueForMetaPropertyToOne("name").getName());
    }

    @Test
    public void testMultipleParameters() throws Exception
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

        this.runtime.createInMemorySource("projection.pure",
                """
                Class PersonProjection projects #Person
                {-[nameWithPrefixAndSuffix(String[0..1], String[*])]}\
                #
                """);
        this.runtime.compile();

        CoreInstance projection = this.runtime.getCoreInstance("PersonProjection");

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(2, properties.size());
        RichIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(2, qualifiedProperties.size());
    }

    @Test
    public void testClassProjectionWithQualifiedPropertyBoundToOtherType() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                import meta::pure::tests::model::simple::*;
                Class meta::pure::tests::model::simple::Trade
                {
                   id : Integer[1];
                   date : Date[1];
                   quantity : Float[1];
                   settlementDateTime : Date[0..1];
                   latestEventDate : Date[0..1];
                
                   customerQuantity()
                   {
                      $this.quantity + $this.quantity;
                   }:Float[1];
                  \s
                   daysToLastEvent()
                   {
                      dateDiff($this.latestEventDate->toOne(), $this.date, DurationUnit.DAYS);
                   }:Integer[1];
                  \s
                   latestEvent()
                   {
                      $this.events->filter(e | $e.date == $this.latestEventDate)->toOne()
                   }:TradeEvent[1];
                  \s
                   eventsByDate(date:Date[1])
                   {
                      $this.events->filter(e | $e.date == $date)
                   }:TradeEvent[*];
                  \s
                   tradeDateEventType()
                   {
                      $this.eventsByDate($this.date->toOne()).eventType->toOne()
                   }:String[1];
                  \s
                   tradeDateEventTypeInlined()
                   {
                      $this.events->filter(e | $e.date == $this.date).eventType->toOne()
                   }:String[1];
                }
                
                Class meta::pure::tests::model::simple::TradeEvent
                {
                   eventType : String[0..1];
                   date: Date[1];
                }
                Class meta::pure::tests::model::simple::TradeProjection projects\s
                #
                   Trade
                   {
                      -[tradeDateEventType()]
                   }
                #
                
                Class meta::pure::tests::model::simple::TradeEventProjection projects\s
                #
                   TradeEvent
                   {
                      *
                   }
                #
                
                Association meta::pure::tests::model::simple::TP_TEP projects meta::pure::tests::model::simple::Trade_TradeEvent<meta::pure::tests::model::simple::TradeProjection, meta::pure::tests::model::simple::TradeEventProjection>
                Association meta::pure::tests::model::simple::Trade_TradeEvent\s
                {
                   trade:  Trade[*];
                   events: TradeEvent [*];
                }
                """);
        this.runtime.compile();
        CoreInstance tradeProjection = this.runtime.getCoreInstance("meta::pure::tests::model::simple::TradeProjection");
        Assertions.assertNotNull(tradeProjection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(tradeProjection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(5, properties.size());
        RichIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(tradeProjection, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(3, qualifiedProperties.size());
    }

    @Test
    public void testProjectionWithNonResolvableQualifiedProperties() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                import meta::pure::tests::model::simple::*;
                import meta::pure::tests::model::simple::projection::*;
                
                
                native function average(s:Number[*]):Float[1];
                native function sum(s:Integer[*]):Integer[1];
                Class meta::pure::tests::model::simple::projection::EntityWithAddressProjection projects
                #
                EntityWithAddress
                {
                    > address [$this.address.name]\s
                }
                #
                
                
                Class meta::pure::tests::model::simple::projection::FirmProjection projects
                #
                meta::pure::tests::model::simple::Firm
                {
                   *
                }
                #\
                """);
        this.runtime.createInMemorySource("model.pure", testModel);
        try
        {
            this.runtime.compile();
        }
        catch (Exception e)
        {
            assertPureException("Error compiling projection 'meta::pure::tests::model::simple::projection::FirmProjection'. Property 'nameAndAddress' cannot be resolved due to underlying cause: Can't find the property 'address' in the class meta::pure::tests::model::simple::projection::FirmProjection", 16, 53, e);
        }
    }

    @Test
    public void testPrintFlattenedProperty() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                import meta::pure::tests::model::simple::*;
                import meta::pure::tests::model::simple::projection::*;
                
                native function average(s:Number[*]):Float[1];
                native function sum(s:Integer[*]):Integer[1];
                Class meta::pure::tests::model::simple::projection::EntityWithAddressProjection projects
                #
                EntityWithAddress
                {
                    > address [$this.address.name]\s
                }
                #
                
                function testPrint():Any[*]\
                {\
                   print(EntityWithAddressProjection,1);\
                }\
                """);
        this.runtime.createInMemorySource("model.pure", testModel);
        this.runtime.compile();
        CoreInstance projection = this.runtime.getCoreInstance("meta::pure::tests::model::simple::projection::EntityWithAddressProjection");
        Printer.print(projection, this.runtime.getProcessorSupport());

        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        Assertions.assertEquals(1, properties.size());
    }

    @Test
    public void testProjectionWithQualifiedPropertyInclude()
    {
        this.runtime.createInMemorySource("projection.pure", """
                import meta::pure::tests::model::simple::*;\
                import meta::pure::tests::model::simple::projection::*;
                native function average(s:Number[*]):Float[1];
                native function sum(s:Integer[*]):Integer[1];
                Class meta::pure::tests::model::simple::projection::FirmProjection projects
                meta::pure::tests::model::simple::Firm
                {
                   +[legalName,sumEmployeesAge]
                }
                
                Class meta::pure::tests::model::simple::projection::PersonProjection projects
                meta::pure::tests::model::simple::Person
                {
                   +[firstName, lastName, age, name()]
                   >employerName [$this.firm.legalName]
                }
                
                Class meta::pure::tests::model::simple::projection::AddressProjection projects
                Address
                {
                   *
                }
                
                Association meta::pure::tests::model::simple::projection::EmploymentProjection projects Employment<FirmProjection, PersonProjection>\s
                """);
        this.runtime.createInMemorySource("model.pure", testModel);
        this.runtime.compile();
        CoreInstance projection = this.runtime.getCoreInstance("meta::pure::tests::model::simple::projection::PersonProjection");
        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.properties, processorSupport);
        RichIterable<? extends CoreInstance> qProperties = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals(4, properties.size());
        Assertions.assertEquals(1, qProperties.size());
    }

    private static String testModel = """
            import meta::pure::profiles::*;
            import meta::pure::tests::model::simple::*;
            native function in(s:String[1],c:String[*]):Boolean[1];
            Class meta::pure::tests::model::simple::EntityWithAddress
            {
                address : Address[0..1];
            }
            
            Class meta::pure::tests::model::simple::EntityWithLocations
            {
                locations : Location[*];
                locationsByType(types:GeographicEntityType[*])
                {
                    $this.locations->filter(l | $types->exists(type | is($l.type, $type)))
                }:Location[*];
            }
            
            Class meta::pure::tests::model::simple::Firm extends EntityWithAddress
            {
                <<equality.Key>> legalName : String[1];
                averageEmployeesAge(){$this.employees.age->average()*2.0}:Float[1];
                sumEmployeesAge(){$this.employees.age->sum()}:Integer[1];
            
                nameAndAddress(){
                   $this.legalName + ',' + $this.address.name->toOne();
                }:String[1];
            
                isFirmX(){
                   if ($this.legalName->toOne() == 'FirmX', | 'Yes', | 'No')
                }:String[1];
               \s
                nameAndMaskedAddress(){
                   if ($this.legalName == 'FirmX', | $this.legalName + ' , LegalFirm', |  $this.legalName + ',' + $this.address.name->toOne())
                }:String[1];
            
                employeeByLastName(lastName:String[1]){$this.employees->filter(e|$e.lastName == $lastName)->toOne()}:Person[0..1];
            
                employeesByAge(age:Integer[1]){$this.employees->filter(e|$e.age->toOne() < $age)}:Person[*];
            
                employeesByCityOrManager(city:String[1], managerName:String[1]){$this.employees->filter(e|$e.address.name == $city || $e.manager.name == $managerName)}:Person[*];
            
                employeesByCityOrManagerAndLastName(name:String[1], city:String[1], managerName:String[1]){$this.employees->filter(e|$e.lastName == $name && ($e.address.name == $city || $e.manager.name == $managerName))->toOne()}:Person[1];
            }
            
            Class meta::pure::tests::model::simple::PersonNameParameter
            {
               lastNameFirst:Boolean[1];
               nested:PersonNameParameterNested[1];
            }
            
            Class meta::pure::tests::model::simple::PersonNameParameterNested
            {
               prefix:String[1];
            }
            
            Class meta::pure::tests::model::simple::Person extends EntityWithAddress, EntityWithLocations
            {
                firstName : String[1];
                lastName : String[1];
                otherNames : String[*];
                name(){$this.firstName+' '+$this.lastName}:String[1];
                nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];
                nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])
                {
                    if($prefix->isEmpty(),
                       | if($suffixes->isEmpty(),
                            | $this.firstName + ' ' + $this.lastName,
                            | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),
                       | if($suffixes->isEmpty(),
                            | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,
                            | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))
                }:String[1];
            
                fullName(lastNameFirst:Boolean[1])
                {
                    if($lastNameFirst, | $this.lastName + ', ' + $this.firstName, | $this.firstName + ' ' + $this.lastName)
                }:String[1];
            
                parameterizedName(personNameParameter:PersonNameParameter[1])
                {
                    if($personNameParameter.lastNameFirst, | $personNameParameter.nested.prefix+' '+$this.lastName + ', ' + $this.firstName, | $this.firstName + ' ' + $this.lastName)
                }:String[1];
            
                allOrganizations()
                {
                    concatenate($this.organizations, $this.organizations->map(o | $o.superOrganizations()))->removeDuplicates()
                }:Organization[*];
                extraInformation : String[0..1];
                manager : Person[0..1];
                age : Integer[0..1];
                constant() { 'constant' } : String[1];
            }
            
            Class meta::pure::tests::model::simple::Interaction
            {
               id : String[1];
               source : Person[0..1];
               target : Person[0..1];
               active : Boolean[1];
               time : Integer[1];
               longestInteractionBetweenSourceAndTarget : Integer[1];
            }
            
            Class meta::pure::tests::model::simple::GeographicEntity
            {
                type : GeographicEntityType[1];
            }
            
            Class meta::pure::tests::model::simple::Location extends GeographicEntity
            {
                place : String[1];
            }
            
            Class meta::pure::tests::model::simple::Address extends GeographicEntity
            {
                name : String[1];
            }
            
            Enum meta::pure::tests::model::simple::GeographicEntityType
            {
                {doc.doc = 'A city, town, village, or other urban area.'} CITY,
                <<doc.deprecated>> COUNTRY,
                {doc.doc = 'Any geographic entity other than a city or country.'} REGION
            }
            
            Class meta::pure::tests::model::simple::Organization
            {
                name : String[1];
                superOrganizations()
                {
                    let parent = $this.parent;
                    if($parent->isEmpty(), |[], |concatenate($parent, $parent->toOne().superOrganizations()));
                }:Organization[*];
                subOrganizations()
                {
                    concatenate($this.children, $this.children->map(c | $c.subOrganizations()))->removeDuplicates()
                }:Organization[*];
                child(name:String[1])
                {
                    $this.children->filter(c | $c.name == $name)->toOne()
                }:Organization[1];
                allMembers()
                {
                    concatenate($this.members, $this.subOrganizations()->map(o | $o.members))->removeDuplicates()
                }:Person[*];
            }
            
            Class meta::pure::tests::model::simple::Division extends Organization
            {
            }
            
            Class meta::pure::tests::model::simple::Department extends Organization
            {
            }
            
            Class meta::pure::tests::model::simple::Team extends Organization
            {
            }
            
            Association meta::pure::tests::model::simple::Employment
            {
                firm : Firm[0..1];
                employees : Person[*];
            }
            
            Association meta::pure::tests::model::simple::FirmOrganizations
            {
                firm : Firm[1];
                organizations : Organization[*];
            }
            
            Association meta::pure::tests::model::simple::SubOrganization
            {
                parent : Organization[0..1];
                children : Organization[*];
            }
            
            Association meta::pure::tests::model::simple::Membership
            {
                organizations : Organization[*];
                members : Person[*];
            }
            
            
            Class meta::pure::tests::model::simple::Product
            {
               name : String[1];
               synonymByType(type:ProductSynonymType[1]){$this.synonyms->filter(s|$s.type == $type)->toOne()}:Synonym[1];
               cusip(){$this.synonymByType(ProductSynonymType.CUSIP).name}:String[1];
               isin(){$this.synonymByType(ProductSynonymType.ISIN).name}:String[1];
               cusipSynonym(){$this.synonymByType(ProductSynonymType.CUSIP)}:Synonym[1];
               isinSynonym(){$this.synonymByType(ProductSynonymType.ISIN)}:Synonym[1];
               classification : ProductClassification[0..1];
            }
            
            Class <<temporal.businesstemporal>> meta::pure::tests::model::simple::ProductClassification{
               type : String[1];
               description : String[1];
            }
            
            Enum meta::pure::tests::model::simple::ProductSynonymType
            {
               CUSIP,
               ISIN,
               GSN
            }
            
            Class meta::pure::tests::model::simple::Synonym
            {
               typeAsString : String[1];
               type : ProductSynonymType[1];
               name : String[1];
            }
            
            Association meta::pure::tests::model::simple::ProdSynonym
            {
               synonyms : Synonym[*];
               product : Product[1];
            }
            
            Class meta::pure::tests::model::simple::Account
            {
               name : String[1];
               createDate : Date[1];
              \s
               accountCategory(){
                  if ( $this.name->in(['Account 1', 'Account 2']), | 'A', | 'B')
               }:String[1];
            }
            
            Class meta::pure::tests::model::simple::Trade
            {
               id : Integer[1];
               date : Date[1];
               quantity : Float[1];
               product : Product[0..1];
               settlementDateTime : Date[0..1];
               latestEventDate : Date[0..1];
               events: TradeEvent[*];
            
               productIdentifier()
               {
                   $this.product.cusip->toOne();
               }:String[1];
              \s
                 \s
            
            
               productDescription()
               {
                  if ($this.product->isEmpty(), | 'Unknown', | $this.product.name->toOne())
               }:String[1];
                \s
               accountDescription()
               {
                 $this.account.name->toOne();
               }:String[1];
              \s
               productIdentifierWithNull()
               {
                  $this.product.cusip;
               }:String[0..1];
            
               customerQuantity()
               {
                  -$this.quantity;
               }:Float[1];
              \s
               daysToLastEvent()
               {
                  dateDiff($this.latestEventDate->toOne(), $this.date, DurationUnit.DAYS);
               }:Integer[1];
              \s
               latestEvent()
               {
                  $this.events->filter(e | $e.date == $this.latestEventDate)->toOne()
               }:TradeEvent[1];
              \s
            
               eventsByDate(date:Date[1])
               {
                  $this.events->filter(e | $e.date == $date)
               }:TradeEvent[*];
              \s
               tradeDateEventType()
               {
                  $this.eventsByDate($this.date->toOne()).eventType->toOne()
               }:String[1];
              \s
               tradeDateEventTypeInlined()
               {
                  $this.events->filter(e | $e.date == $this.date).eventType->toOne()
               }:String[1];
            
               initiator()
               {
                  $this.eventsByDate($this.date).initiator->toOne()
               }:Person[0..1];
            
               initiatorInlined()
               {
                  $this.events->filter(e | $e.date == $this.date).initiator->toOne()
               }:Person[0..1];
            }
            
            Class meta::pure::tests::model::simple::TradeEvent
            {
               eventType : String[0..1];
               date: Date[1];
               initiator: Person[0..1];
            }
            
            Association meta::pure::tests::model::simple::Trade_Accounts
            {
               account : Account[0..1];
               trades : Trade[*];
            }
            
            Class meta::pure::tests::model::simple::Contract
            {
               id : String[1];
               money : Money[1];
            }
            
            Class meta::pure::tests::model::simple::Currency
            {
               currency : String[1];
            }
            
            Class meta::pure::tests::model::simple::Money
            {
               amount: Float[1];
               usdRates:  FxReferenceRate[*];
               currency : Currency[1];
               usdRate(d:Date[1], t:NonStandardTenorQualifier[1])
               {
                  $this.usdRates->filter(u|$u.observationDate == $d && $u.nonStandardTenorQualifier == $t)->toOne()
               }:FxReferenceRate[1];
               usdValueWithMap(d:Date[1], t:NonStandardTenorQualifier[1])
               {
                  if ($this.currency.currency == 'USD',|$this.amount, |$this.amount * $this.usdRate($d, $t)->map(u|$u.rate))
               }:Float[1] ;
               usdValueNoMap(d:Date[1], t:NonStandardTenorQualifier[1])
               {
                  if ($this.currency.currency == 'USD',|$this.amount, |$this.amount * $this.usdRate($d, $t).rate)
               }:Float[1] ;
            }
            
            Class meta::pure::tests::model::simple::FxReferenceRate
            {
               observationDate: Date[1];
               nonStandardTenorQualifier: NonStandardTenorQualifier[0..1];
               rate: Float[1];
            }
            
            Enum meta::pure::tests::model::simple::NonStandardTenorQualifier
            {
               S,   // Spot
               F,   // Forward
               None // None
            }
            
            Association meta::pure::tests::model::simple::BridgeAsso1
            {
                bridge : Bridge[0..1];
                employees : Person[*];
            }
            
            Association meta::pure::tests::model::simple::BridgeAsso2
            {
                bridge : Bridge[0..1];
                firm : Firm[0..1];
            }
            
            Class meta::pure::tests::model::simple::Bridge
            {
            }
            
            
            """;

}
