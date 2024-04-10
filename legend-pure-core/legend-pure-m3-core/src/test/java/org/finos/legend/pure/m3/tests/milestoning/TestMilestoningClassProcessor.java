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

package org.finos.legend.pure.m3.tests.milestoning;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMilestoningClassProcessor extends AbstractTestMilestoning
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("domain.pure");
        runtime.delete("singleInheritance.pure");

        runtime.compile();
    }

    @Test
    public void testNonTemporalStereotypeProcessing()
    {

        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::domain::*;\
                Profile testProfile{stereotypes:[s1,s2];}\
                Class <<testProfile.s1>> meta::test::milestoning::domain::NonTemporalClassWithStereotype{
                }\
                """
        );
        runtime.compile();
    }

    @Test
    public void testImportOfBusinessMilestonedClass()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;\
                Class meta::test::domain::account::Account{
                   orderId : Integer[1];
                }\
                """
        );
        runtime.createInMemorySource("sourceId2.pure",
                """
                import meta::test::domain::account::*;\
                Class <<temporal.businesstemporal>> meta::test::domain::account::Account_Request{
                   name : String[1];
                }\
                Association meta::test::domain::account::Case_Accounts{\
                  Account_Request:meta::test::domain::account::Account_Request[*];\
                  Account:meta::test::domain::account::Account[1];\
                }\
                """
        );
        runtime.compile();
    }

    @Test
    public void testGeneratedMilestonedPropertyHasACorrectlyGeneratedImportGroup()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                Class <<temporal.businesstemporal>> meta::test::domain::account::FirmAccount\s
                {}\
                """
        );
        runtime.createInMemorySource("sourceId2.pure",
                """
                ###Pure
                import meta::test::domain::account::*;\
                Class meta::test::domain::account::trade::Contract\s
                {
                   firmRiskAccount : FirmAccount[1];\
                }\
                """
        );
        runtime.compile();
    }

    @Test
    public void testConflictingStereotypesOnParent()
    {
        String domain = """
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Product{}
                Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Instrument{}
                Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Stock extends meta::relational::tests::milestoning::Product, meta::relational::tests::milestoning::Instrument{}\
                """;

        runtime.createInMemorySource("domain.pure", domain);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:domain.pure line:3 column:77), \"A Type may only have one Temporal Stereotype, 'meta::relational::tests::milestoning::Stock' has [businesstemporal, processingtemporal]\"", e.getMessage());
    }

    @Test
    public void testPropertiesGeneratedForSubclassOfBusinessTemporalSuperClass()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import model::domain::subdom1::account::*;
                Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{}\s
                Class model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{}\s
                function go():Any[*]
                {
                   let a = ^model::domain::subdom1::account::FirmAccount(businessDate=%2015);
                }\
                """
        );
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:3 column:40), \"Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testMilestoningStereotypeExistsAtAllLevelsInAClassHierarchy()
    {
        String singleInheritance = """
                Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{}\s
                Class model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{}\s
                """;

        runtime.createInMemorySource("singleInheritance.pure", singleInheritance);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:40), \"Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchy()
    {
        String singleInheritance = """
                Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{}\s
                Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{}\s
                """;

        runtime.createInMemorySource("singleInheritance.pure", singleInheritance);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:72), \"All temporal stereotypes in a hierarchy must be the same, class model::domain::subdom1::account::FirmAccount is processingtemporal, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchyNonTemporalSupertype()
    {
        String singleInheritance = """
                Class model::domain::subdom1::account::Account{}\s
                Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{}\s
                """;

        runtime.createInMemorySource("singleInheritance.pure", singleInheritance);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:72), \"All temporal stereotypes in a hierarchy must be the same, class model::domain::subdom1::account::FirmAccount is processingtemporal, top most supertype model::domain::subdom1::account::Account is not temporal\"", e.getMessage());
    }

    @Test
    public void testValidTemporalSpecificationInAClassHierarchyCompiles() throws Exception
    {
        String singleInheritance = """
                Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{}\s
                Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{}\s
                """;

        runtime.createInMemorySource("singleInheritance.pure", singleInheritance);
        runtime.compile();
    }

    @Test
    public void testParserGeneratedProcessingTemporalProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::domain::*;
                Class meta::test::milestoning::domain::Order{
                   orderId : Integer[1];
                }
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   name : String[1];
                }
                """
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assertions.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assertions.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "milestoning")));

        Property<?, ?> processingDateProperty = parserGeneratedMilestoningProperties.detect(p -> "processingDate".equals(p._name()));
        Assertions.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assertions.assertEquals("PureOne", processingDateProperty._multiplicity().getName());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assertions.assertEquals("meta::pure::milestoning::ProcessingDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assertions.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assertions.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }

    @Test
    public void testParserGeneratedBusinessTemporalProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::domain::*;
                Class meta::test::milestoning::domain::Order{
                   orderId : Integer[1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   name : String[1];
                }
                """
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assertions.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assertions.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("businessDate", "milestoning")));

        Property<?, ?> businessDateProperty = parserGeneratedMilestoningProperties.detect(p -> "businessDate".equals(p._name()));
        Assertions.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assertions.assertEquals("PureOne", businessDateProperty._multiplicity().getName());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assertions.assertEquals("meta::pure::milestoning::BusinessDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assertions.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assertions.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }

    @Test
    public void testParserGeneratedBiTemporalProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::domain::*;
                Class meta::test::milestoning::domain::Order{
                   orderId : Integer[1];
                }
                Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{
                   name : String[1];
                }
                """
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assertions.assertEquals(3, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assertions.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "businessDate", "milestoning")));

        Property<?, ?> processingDateProperty = parserGeneratedMilestoningProperties.detect(p -> "processingDate".equals(p._name()));
        Assertions.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assertions.assertEquals("PureOne", processingDateProperty._multiplicity().getName());

        Property<?, ?> businessDateProperty = parserGeneratedMilestoningProperties.detect(p -> "businessDate".equals(p._name()));
        Assertions.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assertions.assertEquals("PureOne", businessDateProperty._multiplicity().getName());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assertions.assertEquals("meta::pure::milestoning::BiTemporalMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assertions.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assertions.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }
}
