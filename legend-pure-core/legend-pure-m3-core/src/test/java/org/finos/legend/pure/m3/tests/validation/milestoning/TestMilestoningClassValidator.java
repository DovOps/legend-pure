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

package org.finos.legend.pure.m3.tests.validation.milestoning;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMilestoningClassValidator extends AbstractPureTestWithCoreCompiledPlatform
{

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubType()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::BaseProduct
                {
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct
                {
                   id : Integer[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure lines:5c70-8c1), \"All temporal stereotypes in a hierarchy must be the same, class meta::test::milestoning::domain::Product is businesstemporal, top most supertype meta::test::milestoning::domain::BaseProduct is not temporal\"", e.getMessage());
    }

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubTypeWithMultipleParents() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::BaseProduct1
                {
                }
                Class meta::test::milestoning::domain::BaseProduct2
                {
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct1, BaseProduct2
                {
                   id : Integer[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure lines:8c70-11c1), \"All temporal stereotypes in a hierarchy must be the same, class meta::test::milestoning::domain::Product is businesstemporal, top most supertypes meta::test::milestoning::domain::BaseProduct1, meta::test::milestoning::domain::BaseProduct2 are not temporal\"", e.getMessage());
    }

    @Test
    public void testATypeMayOnlyHaveOneTemporalStereotype() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal,temporal.processingtemporal>> meta::test::milestoning::domain::Product
                {
                   id : Integer[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure lines:2c98-5c1), \"A Type may only have one Temporal Stereotype, 'meta::test::milestoning::domain::Product' has [businesstemporal, processingtemporal]\"", e.getMessage());
    }

    @Test
    public void testNonTemporalTypesCanHaveReservedTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Product
                {
                   businessDate : Date[1];
                   processingDate : Date[1];
                   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];
                }
                """);

        runtime.compile();
    }

    @Test
    public void testTemporalTypesCanNotHaveReservedTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product
                {
                   businessDate : Date[1];
                   processingDate : Date[1];
                   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure lines:2c64-7c1), \"Type: meta::test::milestoning::domain::Product has temporal specification: [businessDate, milestoning, processingDate] properties: [businessDate, milestoning, processingDate] are reserved and should not be explicit in the Model\"", e.getMessage());
    }
}
