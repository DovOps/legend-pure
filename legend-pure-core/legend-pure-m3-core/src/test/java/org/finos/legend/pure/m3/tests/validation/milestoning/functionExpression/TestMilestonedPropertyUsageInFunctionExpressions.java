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

package org.finos.legend.pure.m3.tests.validation.milestoning.functionExpression;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.*;

public class TestMilestonedPropertyUsageInFunctionExpressions extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("source.pure");
        runtime.delete("test.pure");
        runtime.delete("domain.pure");
    }

    //mixed

    @Test
    public void testProcessingDateDoesntPropagateToBusinessContext()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   name : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification.exchange.name}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:13 column:40), \"The property 'exchange' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]\"", e.getMessage());
    }

    @Test
    public void testBusinessDateDoesntPropagateToBusinessContext()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Exchange{
                   name : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification.exchange.name}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:13 column:40), \"The property 'exchange' is milestoned with stereotypes: [ processingtemporal ] and requires date parameters: [ processingDate ]\"", e.getMessage());
    }

    // map

    @Test
    public void testProcessingMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification->map(c|$c.exchangeName)}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification->map(t|$t.exchange.exchangeName)}
                }
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromEdgePointPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classificationAllVersions->map(t|$t.exchange.exchangeName)}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:15 column:61), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromDerivedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).myClassification(%2015)->map(t|$t.exchange.exchangeName)}
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:16 column:59), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToFilter()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification->map(c|^Classification(businessDate=%2015))->filter(t|$t.exchange.exchangeName == '')}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:16 column:97), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    // filter
    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda()
    {
        testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(false);
        testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(true);
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter()
    {
        testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(false, 66);
        testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(true, 91);
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter()
    {
        testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(false, 62);
        testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(true, 87);
    }

    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(boolean extraFilter)
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[*];\n" +
                        "   myClassification(bd:Date[1]){^Classification(name='',businessDate=$bd)} : Classification[*];\n" +
                        "}\n" +
                        "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   name : String[1];\n" +
                        "   exchange : Exchange[0..1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{\n" +
                        "   exchangeName : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|Product.all(%2015).classification" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t2|$t2.exchange.exchangeName == '')}\n" +
                        "}");

        runtime.compile();
        runtime.delete("sourceId.pure");
    }

    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(boolean extraFilter, int colErrorNo)
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[*];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   name : String[1];\n" +
                        "   exchange : Exchange[0..1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{\n" +
                        "   exchangeName : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|Product.all(%2015).classificationAllVersions" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t2|$t2.exchange.exchangeName == '')}\n" +
                        "}");

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:16 column:" + colErrorNo + "), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
        runtime.delete("sourceId.pure");
    }

    public void testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(boolean extraFilter, int colErrorNo)
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[*];\n" +
                        "   myClassification(bd:Date[1]){^Classification(name='',businessDate=$bd)} : Classification[*];\n" +
                        "}\n" +
                        "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   name : String[1];\n" +
                        "   exchange : Exchange[0..1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{\n" +
                        "   exchangeName : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|Product.all(%2015).myClassification(%2015)" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t|$t.exchange.exchangeName == '')}\n" +
                        "}");

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:17 column:" + colErrorNo + "), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
        runtime.delete("sourceId.pure");
    }

    // exists
    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015)->filter(p|$p.classification->exists(c|$c.exchange.exchangeName == ''))}
                }\
                """);
        runtime.compile();
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromEdgePointPropertyThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015)->filter(p|$p.classificationAllVersions->exists(c|$c.exchange.exchangeName == ''))}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:15 column:77), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromDerivedPropertyThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015)->filter(p|$p.myClassification(%2015)->exists(c|$c.exchange.exchangeName == ''))}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:16 column:75), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextAllowedToPropagateThroughSubTypeToNoArgMilestonedProperty()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                native function meta::pure::functions::lang::subType<T|m>(source:Any[m], object:T[1]):T[m];
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification->subType(@ExtendedClassification).exchange.exchangeName}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapThroughSubTypeToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                native function meta::pure::functions::lang::subType<T|m>(source:Any[m], object:T[1]):T[m];
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[*];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ExtendedClassification extends Classification{
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification->map(t|$t->subType(@ExtendedClassification).exchange.exchangeName)}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateThroughFunctionWhichDoesNotAllowMilestoningContextPropagation()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationType : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015)->outOfScopeFunction(p|$p.classification.classificationType == '')}
                }\
                function outOfScopeFunction<T>(value:T[*], func:Function<{T[1]->Boolean[1]}>[1]):T[*]{$value}
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:10 column:50), \"No-Arg milestoned property: 'classification' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllToMultipleNoArgQualifiedProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).classification.exchange.exchangeName}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateAsVariableFromAllToNoArgQualifiedProperty()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationName : String[1];
                }
                function go():Any[*]
                {
                   let bdVar=%2015;\
                   {|Product.all($bdVar).classification.classificationName};\
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromMilestonedQualifiedPropertyToNoArgMilestonedProperty()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                   myClassification(bd:Date[1]){^Classification(businessDate=$bd)} : Classification[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015).myClassification(%2015).exchange.exchangeName}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:14 column:49), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedProperty()
    {
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
                   {|Product.all(%2015).classificationAllVersions.exchange.exchangeName}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:13 column:51), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters\"", e.getMessage());
    }

    @Test
    public void testProcessingErrorWhenMilestoningContextIsNotAvailableToNoArgMilestonedProperty()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Order{
                   product : Product[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   name : String[1];
                }
                function go():Any[*]
                {
                   {|Order.all().product.name}
                }\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:10 column:18), \"The property 'product' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]\"", e.getMessage());
    }

    @Test
    public void testNoProcessingErrorWhenMilestoningContextIsNotAvailableFromSourceQualifiedMilestonedPropertyWithDateParam()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Order{
                   product : Product[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   name : String[1];
                }
                function go():Any[*]
                {
                   {|Order.all().product(%2016-5-1).name}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateThroughAutoMappedQualifiedMilestonedPropertyWithDateParam()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Order{
                   orderEvents : OrderEvents[*];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::OrderEvents{
                   classification : Classification[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationType : String[1];
                }
                function go():Any[*]
                {
                   {|Order.all().orderEvents(%2016-5-1).classification.classificationType}
                }\
                """);

        runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughProjectToNoArgMilestonedPropertyInLambda()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchange : Exchange[1];
                   classificationType : String[1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{
                   exchangeName : String[1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2015)->project([p|$p.classification.classificationType, p|$p.classification.exchange.exchangeName],['exchangeType','classificationType'])}
                }
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*], ids:String[*]):Any[0..1]
                {
                 []\
                }\
                """
        );
        runtime.compile();
    }

    @Test
    public void testLatestDateIsNotUsableInANonMilestonedPropertyExpression()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Order{
                   product : Product[0..1];
                   derivedProduct(d:Date[1]){$this.product($d)}:Product[0..1];
                }
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   name:String[1];
                }
                function go():Any[*]
                {
                   {|Order.all(%latest).derivedProduct(%latest).name};\
                }
                """
        );

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:11 column:25), \"%latest may only be used as an argument to milestoned properties\"", e.getMessage());
    }

    /**
     * bitemporal
     */
    @Test
    public void testBiTemporalDatesNotSupplied()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*] { {|Order.all().createdLocation.place} }\
                """;

        runtime.createInMemorySource("domain.pure", domain);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:domain.pure line:4 column:38), \"The property 'createdLocation' is milestoned with stereotypes: [ bitemporal ] and requires date parameters: [ processingDate, businessDate ]\"", e.getMessage());
    }

    @Test
    public void testBiTemporalDatesArePropagatedFromBiTemporalRoot()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*] { {|Order.all(%9999,%2017).createdLocation.place} }\
                """;

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testBiTemporalDatesArePropagatedFromBiTemporalToBiTemporalInProject()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class  meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : Place[1];}
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Place{ name : String[1];}
                function go():Any[*] { {|Order.all().createdLocation(%9999,%2017).place.name} }
                """;

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testBusinessTemporalDatesArePropagatedFromBiTemporalToBiTemporalInProject()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class  meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Location{ place : Place[1];}
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Place{ name : String[1];}
                function go():Any[*] { {|Order.all().createdLocation(%9999).place.name} }\
                """; //Order.all(%2017)->map(v_automap|$v_automap.createdLocation(%9999))->map(v_automap|$v_automap.place)

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testBiTemporalPropertyUsageWhenOnlyOneDatePropagated()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*] { {|Order.all(%2017).createdLocation.place} }\
                """;

        runtime.createInMemorySource("domain.pure", domain);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:domain.pure line:4 column:43), \"The property 'createdLocation' is milestoned with stereotypes: [ bitemporal ] and requires date parameters: [ processingDate, businessDate ]\"", e.getMessage());
    }

    @Test
    public void testBusinessDatePropagatedToBiTemporalTypeWhenProcessingDateSupplied()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*] { {|Order.all(%2017).createdLocation(%2016).place} }\
                """; //Order.all(%2017)->map(v_automap|$v_automap.createdLocation(%9999))->map(v_automap|$v_automap.place)

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testProcessingDatePropagatedToBiTemporalTypeWhenBusinessDateSupplied()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*] { {|Order.all(%2017).createdLocation(%2016).place} }\
                """;
        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testPropagationOfSingleDateFromBiTemporalAll()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Product { exchange : Exchange[0..1]; }
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Exchange{ location : Location[1];}
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Location{ street : String[1];}
                function go():Any[*] { {|Product.all(%9999, %2017-8-14).exchange.location.street} }\
                """;

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();

        CoreInstance locationMilestonedQp = getLocationQualifiedProperty();
        Assertions.assertEquals("location", locationMilestonedQp.getValueForMetaPropertyToOne(M3Properties.qualifiedPropertyName).getValueForMetaPropertyToOne(M3Properties.values).getName());
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 8, 14), ((DateCoreInstance) locationMilestonedQp.getValueForMetaPropertyToMany(M3Properties.parametersValues).get(1).getValueForMetaPropertyToOne(M3Properties.values)).getValue());
    }

    @Test
    public void testPropagationOfSingleDateFromBiTemporalQualifiedProperty()
    {
        String domain = """
                import meta::relational::tests::milestoning::*;
                Class meta::relational::tests::milestoning::Product { exchange : Exchange[0..1]; }
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Exchange{ location : Location[1];}
                Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Location{ street : String[1];}
                function go():Any[*] { {|Product.all().exchange(%9999, %2017-8-14).location.street} }\
                """;

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();

        CoreInstance locationMilestonedQp = getLocationQualifiedProperty();
        Assertions.assertEquals("location", locationMilestonedQp.getValueForMetaPropertyToOne(M3Properties.qualifiedPropertyName).getValueForMetaPropertyToOne(M3Properties.values).getName());
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 8, 14), ((DateCoreInstance) locationMilestonedQp.getValueForMetaPropertyToMany(M3Properties.parametersValues).get(1).getValueForMetaPropertyToOne(M3Properties.values)).getValue());
    }

    @Test
    public void testAllVersionsInRangePropertyUsageForBusinessTemporal()
    {
        runtime.createInMemorySource("source.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                """);
        runtime.compile();

        runtime.createInMemorySource("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange}
                }
                """);
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The property 'classificationAllVersionsInRange' is milestoned with stereotypes: [ businesstemporal ] and requires 2 date parameters : [start, end]\"", e1.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1)}
                }
                """);
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1])\"", e2.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-5, %2018-1-9)}
                }
                """);
        PureCompilationException e3 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1],_:StrictDate[1])\"", e3.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();
    }

    @Test
    public void testAllVersionsInRangePropertyUsageForProcessingTemporal()
    {
        runtime.createInMemorySource("source.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                """);
        runtime.compile();

        runtime.createInMemorySource("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange}
                }
                """);
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The property 'classificationAllVersionsInRange' is milestoned with stereotypes: [ processingtemporal ] and requires 2 date parameters : [start, end]\"", e1.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1)}
                }
                """);
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1])\"", e2.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-5, %2018-1-9)}
                }
                """);
        PureCompilationException e3 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1],_:StrictDate[1])\"", e3.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();
    }

    @Test
    public void testAllVersionsInRangePropertyUsageForLatestDate()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%latest, %latest)}
                }
                """);
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:10 column:55), \"%latest not a valid parameter for AllVersionsInRange()\"", e1.getMessage());

        runtime.modify("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%latest, %latest)}
                }
                """);
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:10 column:55), \"%latest not a valid parameter for AllVersionsInRange()\"", e2.getMessage());
    }

    @ToFix
    @Disabled
    @Test
    public void testAllVersionsInRangePropertyUsageForCrossTemporal()
    {
        runtime.createInMemorySource("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.all(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.bitemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:10 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1])", e1.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.bitemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:test.pure line:10 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1])", e2.getMessage());

        runtime.modify("test.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[*];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   exchangeName : String[0..1];
                }
                function go():Any[*]
                {
                   {|Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}
                }
                """);
        runtime.compile();
    }

    private CoreInstance getLocationQualifiedProperty()
    {
        CoreInstance lambda = runtime.getFunction("go__Any_MANY_").getValueForMetaPropertyToOne(M3Properties.expressionSequence).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance exprSeq = lambda.getValueForMetaPropertyToOne(M3Properties.expressionSequence);
        return exprSeq.getValueForMetaPropertyToMany(M3Properties.parametersValues).get(0).getValueForMetaPropertyToMany(M3Properties.parametersValues).get(1).getValueForMetaPropertyToOne(M3Properties.values).getValueForMetaPropertyToOne(M3Properties.expressionSequence);
    }
}