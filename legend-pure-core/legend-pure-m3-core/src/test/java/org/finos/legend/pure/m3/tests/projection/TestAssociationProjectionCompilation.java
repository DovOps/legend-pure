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
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestAssociationProjectionCompilation extends AbstractPureTestWithCoreCompiledPlatform
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
    }

    @Test
    public void testExceptionScenario()
    {
        runtime.createInMemorySource("file.pure",
                """
                Class a::b::Person
                {
                   name: String[1];
                   yearsEmployed : Integer[1];
                }
                Class a::b::Address
                {
                   street:String[1];
                }
                Class a::b::PersonProjection projects #a::b::Person
                {
                   *
                }#
                Class a::b::AddressProjection projects #a::b::Address
                {
                   *
                }#
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>
                Association a::b::PersonAddress\s
                {
                   person:  a::b::PersonProjection[1];
                   address: a::b::Address[*];
                }
                """);
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'person' is not specified.", "file.pure", 18, 19, e1);

        runtime.modify("file.pure",
                """
                Class a::b::Person
                {
                   name: String[1];
                   yearsEmployed : Integer[1];
                }
                Class a::b::Address
                {
                   street:String[1];
                }
                Class a::b::PersonProjection projects #a::b::Person
                {\s
                   *
                }#
                Class a::b::AddressProjection projects #a::b::Address
                {
                   *
                }#
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::Address>
                Association a::b::PersonAddress\s
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];
                }
                """);
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "AssociationProjection 'a::b::PerAddProjection' can only be applied to ClassProjections; 'a::b::Address' is not a ClassProjection", "file.pure", 18, 19, e2);

        runtime.modify("file.pure",
                """
                Class a::b::Person
                {
                   name: String[1];
                   yearsEmployed : Integer[1];
                }
                Class a::b::Address
                {
                   street:String[1];
                }
                Class a::b::Random
                {
                   arbit:String[1];
                }
                Class a::b::RandomProjection projects # a::b::Random
                {
                   *
                }#
                Class a::b::PersonProjection projects #a::b::Person
                {
                   *
                }#
                Class a::b::AddressProjection projects #a::b::Address
                {\s
                   *
                }#
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::RandomProjection>
                Association a::b::PersonAddress
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];
                }
                """);
        PureCompilationException e3 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'address' is not specified.", "file.pure", 26, 19, e3);

        runtime.modify("file.pure",
                """
                Class a::b::Person
                {
                   name: String[1];
                   yearsEmployed : Integer[1];
                }
                Class a::b::Address
                {
                   street:String[1];
                }
                Class a::b::Random
                {
                   arbit:String[1];
                }
                Class a::b::RandomProjection projects # a::b::Random
                {
                   *
                }#
                Class a::b::PersonProjection projects #a::b::Person
                {
                   *
                }#
                Class a::b::AddressProjection projects #a::b::Address
                {
                   *
                }#
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::PersonProjection>
                Association a::b::PersonAddress\s
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];
                }
                """);
        PureCompilationException e4 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Invalid AssociationProjection 'a::b::PerAddProjection'. Projection for property 'address' is not specified.", "file.pure", 26, 19, e4);
    }

    @Test
    public void testSimpleAssociationProjection()
    {
        runtime.createInMemorySource("file.pure", """
                Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }
                Class a::b::Address{ street:String[1]; }
                Class a::b::PersonProjection projects #a::b::Person\
                {\s
                   *   \
                }#\
                Class a::b::AddressProjection projects #a::b::Address\
                {\s
                   *   \
                }#\
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\
                Association a::b::PersonAddress\s
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];\
                }
                """);
        runtime.compile();

        CoreInstance personProjection = runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    @Test
    public void testSimpleAssociationProjectionWithOrderFlipped()
    {
        runtime.createInMemorySource("file.pure", """
                Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }
                Class a::b::Address{ street:String[1]; }
                Class a::b::PersonProjection projects #a::b::Person\
                {\s
                   *   \
                }#\
                Class a::b::AddressProjection projects #a::b::Address\
                {\s
                   *   \
                }#\
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::AddressProjection, a::b::PersonProjection>\
                Association a::b::PersonAddress\s
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];\
                }
                """);
        runtime.compile();

        CoreInstance personProjection = runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    @Test
    public void testInheritedAssociationProjection()
    {
        runtime.createInMemorySource("file.pure", """
                Class a::b::Person{ name: String[1]; yearsEmployed : Integer[1]; }
                Class a::b::Address{ street:String[1]; }
                Class a::b::ZipAddress extends a::b::Address { zip:String[1]; }
                Class a::b::PersonProjection projects #a::b::Person\
                {\s
                   *   \
                }#\
                Class a::b::AddressProjection projects #a::b::ZipAddress\
                {\s
                   *   \
                }#\
                Association a::b::PerAddProjection projects a::b::PersonAddress<a::b::PersonProjection, a::b::AddressProjection>\
                Association a::b::PersonAddress\s
                {
                   person:  a::b::Person[1];
                   address: a::b::Address[*];\
                }
                """);
        runtime.compile();

        CoreInstance personProjection = runtime.getCoreInstance("a::b::PersonProjection");
        CoreInstance addressProjection = runtime.getCoreInstance("a::b::AddressProjection");

        assertPropertiesFromAssociationProjection(personProjection, "address");
        assertPropertiesFromAssociationProjection(addressProjection, "person");
    }

    public void assertPropertiesFromAssociationProjection(CoreInstance projection, String properties)
    {
        Assertions.assertNotNull(projection);
        RichIterable<? extends CoreInstance> propertiesFromAssociation = Instance.getValueForMetaPropertyToManyResolved(projection, M3Properties.propertiesFromAssociations, processorSupport);
        Assertions.assertEquals(1, propertiesFromAssociation.size(), "Missing properties");

        RichIterable<String> names = propertiesFromAssociation.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(names.toList(), properties);
    }

    @Test
    public void testAssociationProjectionPropertiesReferencedInQualifiedProperties()
    {
        runtime.createInMemorySource("file.pure", """
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
                
                Association meta::pure::tests::model::simple::TP_TEP projects Trade_TradeEvent<TradeProjection, meta::pure::tests::model::simple::TradeEventProjection>
                Association meta::pure::tests::model::simple::Trade_TradeEvent\s
                {
                   trade:  Trade[*];
                   events: TradeEvent [*];
                }
                function meta::pure::tests::model::simple::tradeEventProjectionType(): Property<TradeProjection, Any|*>[1]
                {
                      TradeProjection.properties->filter(p | $p.name=='events')->toOne()
                }
                function meta::pure::tests::model::simple::tradeEventProjectionReturnType(): TradeEventProjection[1]
                {
                      TradeProjection.properties->filter(p | $p.name=='events')->toOne()->genericType().typeArguments->at(0).rawType->toOne()->cast(@FunctionType).returnType.rawType->toOne()->cast(@TradeEventProjection)
                }
                """);
        runtime.compile();
    }
}
