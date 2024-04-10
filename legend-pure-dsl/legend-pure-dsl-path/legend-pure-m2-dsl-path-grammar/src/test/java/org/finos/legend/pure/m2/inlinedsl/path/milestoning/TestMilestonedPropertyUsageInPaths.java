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

package org.finos.legend.pure.m2.inlinedsl.path.milestoning;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMilestonedPropertyUsageInPaths extends AbstractPureTestWithCoreCompiled
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
    }

    @Test
    public void testProcessingErrorWhenMilestoningContextNotAvailableToNoArgQualifiedPropertyFromRootInPath() throws Exception
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {

            runtime.createInMemorySource("sourceId.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    Class meta::test::milestoning::domain::Product{
                       classification : Classification[1];
                    }
                    Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                       classificationType : String[1];
                    }
                    function go():Any[*]
                    {
                       print(#/Product/classification/classificationType#)\
                    }
                    """);

            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:sourceId.pure line:10 column:20), \"No-Arg milestoned property: 'classification' must be either called in a milestoning context or supplied with [businessDate] parameters"));
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateThroughEdgePointPropertyToNoArgMilestonedPropertyInPath() throws Exception
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {

            runtime.createInMemorySource("sourceId.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                       classification : Classification[1];
                    }
                    Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                       exchange : Exchange[1];
                    }
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                       exchangeName : String[1];
                    }
                    function go():Any[*]
                    {
                       print(#/Product/classificationAllVersions/exchange/exchangeName#)\
                    }
                    """);

            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:sourceId.pure line:13 column:46), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters"));
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateThroughNonMilestonedPropertyToNoArgMilestonedPropertyInPath() throws Exception
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {

            runtime.createInMemorySource("sourceId.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                       classification : Classification[1];
                    }
                    Class  meta::test::milestoning::domain::Classification{
                       exchange : Exchange[1];
                    }
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                       exchangeName : String[1];
                    }
                    function go():Any[*]
                    {
                       print(#/Product/classification/exchange/exchangeName#)\
                    }
                    """);

            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:sourceId.pure line:13 column:35), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters"));
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughProjectToNoArgMilestonedPropertyInLambdaPath() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationName : String[0..1];
                }
                function go():Any[*]
                {
                let date=%2015;
                  {|Product.all($date)->project([#/Product/classification/classificationName#])};
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]
                {
                 []\
                }
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectToNoArgMilestonedPropertyInLambdaPath() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationName : String[0..1];
                }
                function go():Any[*]
                {
                  {|Product.all(%2015)->filter(p|!$p->isEmpty())->project([#/Product/classification/classificationName#])}
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]
                {
                 []\
                }
                """);

        runtime.compile();
    }

    @Test
    public void testNoProcessingErrorWhenMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectAndOverridenInMilestonedPropertyWithDateParamInOneOfManyLambdaPaths() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];
                   nonTemporalClassification : NonTemporalClassification[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationName : String[0..1];
                   system : ClassificationSystem[0..1];
                }
                Class meta::test::milestoning::domain::NonTemporalClassification{
                   classificationName : String[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ClassificationSystem{
                   systemName : String[0..1];
                }
                function go():Any[*]
                {
                  {|Product.all(%2015)->filter(p|!$p->isEmpty())->project([#/Product/nonTemporalClassification/classificationName#,#/Product/classification(%2016-1-1)/system/systemName#])}
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]
                {
                 []\
                }
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectToNoArgMilestonedPropertyInOneOfManyLambdaPaths() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                   name: String[0..1];
                   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationName : String[0..1];
                }
                function go():Any[*]
                {
                  {|Product.all(%2015)->project([#/Product/name#,#/Product/classification/classificationName#])}
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]
                {
                 []\
                }
                """);

        runtime.compile();
    }
}
