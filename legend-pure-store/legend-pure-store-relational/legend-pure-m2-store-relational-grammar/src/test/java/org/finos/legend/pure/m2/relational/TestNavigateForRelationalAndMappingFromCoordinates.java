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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.FilterInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinInstance;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestNavigateForRelationalAndMappingFromCoordinates extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testNavigateForFilter() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "filterMappingSample.pure",
                """
                Class a::b::Firm
                {
                   legalName : String[1];
                }
                
                ###Relational
                Database a::b::db
                (
                    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT)
                    Filter GoldmanSachsFilter(firmTable.LEGALNAME = 'Goldman Sachs')
                )
                
                ###Mapping
                Mapping a::b::simpleRelationalMappingWithFilter
                (
                   a::b::Firm : Relational
                          {
                             ~filter [a::b::db] GoldmanSachsFilter
                             legalName : [a::b::db]firmTable.LEGALNAME
                          }
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(18, 33, this.processorSupport);
        Assertions.assertTrue(found instanceof Filter);
        Assertions.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assertions.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(10, found.getSourceInformation().getLine());
        Assertions.assertEquals(12, found.getSourceInformation().getColumn());

        found = source.navigate(18, 40, this.processorSupport);
        Assertions.assertTrue(found instanceof Filter);
        Assertions.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assertions.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(10, found.getSourceInformation().getLine());
        Assertions.assertEquals(12, found.getSourceInformation().getColumn());

        found = source.navigate(18, 50, this.processorSupport);
        Assertions.assertTrue(found instanceof Filter);
        Assertions.assertEquals("GoldmanSachsFilter", ((FilterInstance)found)._name());
        Assertions.assertEquals("filterMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(10, found.getSourceInformation().getLine());
        Assertions.assertEquals(12, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForJoin() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "joinSample.pure",
                """
                Class a::b::Firm
                {
                   legalName : String[1];
                   address  : a::b::Address[1];
                }
                
                Class a::b::Address
                {
                   name : String[1];
                }
                
                ###Relational
                Database a::b::db
                (
                    Table firmTable(ID INT PRIMARY KEY, LEGALNAME VARCHAR(200), ADDRESSID INT, FLAG INT)
                    Table addressTable(ID INT PRIMARY KEY, TYPE INT, NAME VARCHAR(200), STREET VARCHAR(100), COMMENTS VARCHAR(100))
                    Join Address_Firm(addressTable.ID = firmTable.ADDRESSID)       \s
                )
                
                ###Mapping
                Mapping a::b::chainedJoinsInner
                (
                   a::b::Firm : Relational
                          {
                             legalName : [a::b::db]firmTable.LEGALNAME,
                             address(
                                name : [a::b::db] case(equal(@Address_Firm |addressTable.ID, 1), 'UK', 'Europe')\s
                             )
                          }
                  \s
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(27, 47, this.processorSupport);
        Assertions.assertTrue(found instanceof Join);
        Assertions.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assertions.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(17, found.getSourceInformation().getLine());
        Assertions.assertEquals(10, found.getSourceInformation().getColumn());

        found = source.navigate(27, 50, this.processorSupport);
        Assertions.assertTrue(found instanceof Join);
        Assertions.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assertions.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(17, found.getSourceInformation().getLine());
        Assertions.assertEquals(10, found.getSourceInformation().getColumn());

        found = source.navigate(27, 58, this.processorSupport);
        Assertions.assertTrue(found instanceof Join);
        Assertions.assertEquals("Address_Firm", ((JoinInstance)found)._name());
        Assertions.assertEquals("joinSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(17, found.getSourceInformation().getLine());
        Assertions.assertEquals(10, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForAssociationMapping() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "associationMappingSample.pure",
                """
                ###Pure
                import a::*;
                
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                   firmId : Integer[1];
                }
                
                Class a::Firm
                {
                   id : Integer[1];
                   name : String[1];
                }
                
                Association a::Person_Firm
                {
                   person : Person[1];
                   firm : Firm[1];
                }
                
                ###Relational
                Database a::PersonFirmDatabase
                (
                   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)
                   Table firm(ID INT, NAME VARCHAR(200))
                   Join person_firm(person.FIRM_ID = firm.ID)
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMappin
                (
                   Person[personAlias] : Relational
                   {
                      scope([PersonFirmDatabase]person)
                      (
                         id : ID,
                         name : NAME,
                         firmId : FIRM_ID
                      )
                   }
                  \s
                   Firm : Relational
                   {
                      scope([PersonFirmDatabase]firm)
                      (
                        id : ID,
                        name : NAME
                      )
                   }
                  \s
                   Person_Firm : Relational
                   {
                      AssociationMapping
                      (
                         firm[personAlias,a_Firm] : [PersonFirmDatabase]@person_firm,
                         person[a_Firm,personAlias] : [PersonFirmDatabase]@person_firm
                      )
                   }
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(59, 14, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(59, 15, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 20, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 25, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 26, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(59, 27, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 30, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 32, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 33, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(59, 34, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(60, 16, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(60, 17, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 20, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 22, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(46, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 23, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(60, 24, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 30, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 34, this.processorSupport);
        Assertions.assertTrue(found instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("associationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(36, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 35, this.processorSupport);
        Assertions.assertNull(found);

        found = source.navigate(60, 36, this.processorSupport);
        Assertions.assertNull(found);
    }

    @Test
    public void testNavigateForProperty() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "propertySample.pure",
                """
                ###Pure
                import a::*;
                
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                   firmId : Integer[1];
                }
                
                Class a::Firm
                {
                   id : Integer[1];
                   name : String[1];
                }
                
                Association a::Person_Firm
                {
                   person : Person[1];
                   firm : Firm[1];
                }
                
                ###Relational
                Database a::PersonFirmDatabase
                (
                   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)
                   Table firm(ID INT, NAME VARCHAR(200))
                   Join person_firm(person.FIRM_ID = firm.ID)
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonFirmMappin
                (
                   Person[personAlias] : Relational
                   {
                      scope([PersonFirmDatabase]person)
                      (
                         id : ID,
                         name : NAME,
                         firmId : FIRM_ID
                      )
                   }
                  \s
                   Firm : Relational
                   {
                      scope([PersonFirmDatabase]firm)
                      (
                        id : ID,
                        name : NAME
                      )
                   }
                  \s
                   Person_Firm : Relational
                   {
                      AssociationMapping
                      (
                         firm[personAlias,a_Firm] : [PersonFirmDatabase]@person_firm,
                         person[a_Firm,personAlias] : [PersonFirmDatabase]@person_firm
                      )
                   }
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(59, 10, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("firm", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 11, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("firm", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(59, 13, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("firm", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 10, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("person", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(19, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 12, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("person", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(19, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

        found = source.navigate(60, 15, this.processorSupport);
        Assertions.assertTrue(found instanceof PropertyInstance);
        Assertions.assertEquals("person", ((PropertyInstance)found)._name());
        Assertions.assertEquals("propertySample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(19, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForView() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "viewSample.pure",
                """
                ###Pure
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                   firmId : Integer[1];
                }
                
                Class a::Firm
                {
                   id : Integer[1];
                   name : String[1];
                }
                
                ###Relational
                Database a::PersonFirmDatabase
                (
                   Table person (ID INT, NAME VARCHAR(200), FIRM_ID INT)
                   Table firm(ID INT, NAME VARCHAR(200))
                   View person_firm_view(personId : person.ID, personName : person.NAME, firmId : person.FIRM_ID)
                   Join person_firm(person_firm_view.firmId = firm.ID)
                )
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(21, 21, this.processorSupport);
        Assertions.assertTrue(found instanceof View);
        Assertions.assertEquals("person_firm_view", ((View)found)._name());
        Assertions.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(9, found.getSourceInformation().getColumn());

        found = source.navigate(21, 30, this.processorSupport);
        Assertions.assertTrue(found instanceof View);
        Assertions.assertEquals("person_firm_view", ((View)found)._name());
        Assertions.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(9, found.getSourceInformation().getColumn());

        found = source.navigate(21, 36, this.processorSupport);
        Assertions.assertTrue(found instanceof View);
        Assertions.assertEquals("person_firm_view", ((View)found)._name());
        Assertions.assertEquals("viewSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(20, found.getSourceInformation().getLine());
        Assertions.assertEquals(9, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForEmbeddedRelationalInstanceSetImplementation() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "embeddedRelationalInstanceSetImplementationSample.pure",
                """
                ###Pure
                import a::*;
                Class a::Name
                {
                   first : String[1];
                   last : String[1];
                }
                
                Class a::Person
                {
                   id : Integer[1];
                   name : Name[1];
                }
                
                ###Relational
                Database a::PersonDatabase
                (
                   Table person (ID INT, FIRST_NAME VARCHAR(200), LAST_NAME VARCHAR(200))
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonMapping
                (
                   Person[personAlias] : Relational
                   {
                      scope([PersonDatabase]person)
                      (
                         id : ID,
                         name
                         (last: LAST_NAME,first: FIRST_NAME)
                      )
                   }
                  \s
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(31, 10, this.processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("name", ((Property)found)._name());
        Assertions.assertEquals("embeddedRelationalInstanceSetImplementationSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(12, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForTableAliasWithSchema() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "tableAliasSample.pure",
                """
                ###Pure
                import a::*;
                
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                }
                
                ###Relational
                Database a::PersonDatabase
                (
                   Schema personSchema (
                       Table person (ID INT, NAME VARCHAR(200))
                   )
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonMapping
                (
                   Person[personAlias] : Relational
                   {
                      scope([PersonDatabase]personSchema.person)
                      (
                         id : ID,
                         name : NAME
                      )
                   }
                  \s
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(25, 42, this.processorSupport);
        Assertions.assertTrue(found instanceof Table);
        Assertions.assertEquals("person", ((Table)found)._name());
        Assertions.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(14, found.getSourceInformation().getLine());
        Assertions.assertEquals(14, found.getSourceInformation().getColumn());

        found = source.navigate(25, 29, this.processorSupport);
        Assertions.assertTrue(found instanceof Schema);
        Assertions.assertEquals("personSchema", ((Schema)found)._name());
        Assertions.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(13, found.getSourceInformation().getLine());
        Assertions.assertEquals(11, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigationForTableAliasWithoutSchema() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "tableAliasSample.pure",
                """
                ###Pure
                import a::*;
                
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                }
                
                ###Relational
                Database a::PersonDatabase
                (
                   Table person (ID INT, NAME VARCHAR(200))
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonMapping
                (
                   Person[personAlias] : Relational
                   {
                      scope([PersonDatabase]person)
                      (
                         id : ID,
                         name : NAME
                      )
                   }
                  \s
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(23, 29, this.processorSupport);
        Assertions.assertTrue(found instanceof Table);
        Assertions.assertEquals("person", ((Table)found)._name());
        Assertions.assertEquals("tableAliasSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(13, found.getSourceInformation().getLine());
        Assertions.assertEquals(10, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForEnumerationMapping() throws Exception
    {
        Source source = this.runtime.createInMemorySource(
                "enumerationMappingSample.pure",
                """
                ###Pure
                import a::*;
                
                Enum a::Gender
                {
                    FEMALE,
                   \s
                    MALE
                }
                
                Class a::Person
                {
                   id : Integer[1];
                   name : String[1];
                   gender : Gender[1];
                }
                
                ###Relational
                Database a::PersonDatabase
                (
                   Schema personSchema (
                       Table person (ID INT, NAME VARCHAR(200), GENDER CHAR(1))
                   )
                )
                
                ###Mapping
                import a::*;
                
                Mapping a::PersonMapping
                (
                   Gender: EnumerationMapping GenderMapping
                   {
                        FEMALE:  'F',
                        MALE:    'M'\s
                   }
                
                   Person[personAlias] : Relational
                   {
                      scope([PersonDatabase]personSchema.person)
                      (
                         id : ID,
                         name : NAME,
                         gender : EnumerationMapping GenderMapping: GENDER
                      )
                   }
                  \s
                )\
                """
        );
        this.runtime.compile();

        CoreInstance found = source.navigate(43, 39, this.processorSupport);
        Assertions.assertTrue(found instanceof EnumerationMapping);
        Assertions.assertEquals("GenderMapping", ((EnumerationMapping)found)._name());
        Assertions.assertEquals("enumerationMappingSample.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(31, found.getSourceInformation().getLine());
        Assertions.assertEquals(4, found.getSourceInformation().getColumn());

    }
}
