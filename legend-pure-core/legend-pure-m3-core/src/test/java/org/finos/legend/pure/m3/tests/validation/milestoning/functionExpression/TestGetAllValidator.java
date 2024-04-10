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
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGetAllValidator extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("domain.pure");
    }

    @Test
    public void testParameterValidationForBusinessTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product
                {
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let f={|Product.all()};
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testParameterValidationForProcessingTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product
                {
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let f={|Product.all()};
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is  [processingtemporal], [processingDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testParameterValidationForBusinessTemporalSubTypeInGetAllWithVariableParam()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::BaseProduct{}
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct{
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let date=getDate();
                   let f={|Product.all($date)};
                }
                function getDate():Date[1]
                {
                   %9999-12-31
                }
                """);

        runtime.compile();
    }

    @Test
    public void testParameterValidationForNonTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Product
                {
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let f={|Product.all(%2015)};
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is not Temporal, Dates should not be supplied to all()\"", e.getMessage());
    }

    @Test
    public void testGetAllWithNonTemporalVariableParam()
    {
        this.compileTestSourceM3("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function testGetAllAsFunc<Product>(clazz:Class<Product>[1]):Any[*]
                {
                   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);
                }
                """);
    }

    @Test
    public void testGetAllWithTemporalVariableParam()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSourceM3("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function testGetAllAsFunc():Any[*]
                {
                   let f=getProduct()->getAll()->deactivate()->cast(@SimpleFunctionExpression);
                }
                function getProduct():Class<Product>[1]
                {
                  ^Product(businessDate=%2015)->cast(@Class<Product>);
                }
                """));
        Assertions.assertEquals("Compilation error at (resource:sourceId.pure line:7 column:24), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testGetAllWithTemporalVariableParamWithDateSupplied()
    {

        this.compileTestSourceM3("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function testGetAllAsFunc():Any[*]
                {
                   let f=getProduct()->getAll(%2015)->deactivate()->cast(@SimpleFunctionExpression);
                }
                function getProduct():Class<Product>[1]
                {
                  ^Product(id=1, businessDate=%2015)->cast(@Class<Product>);
                }
                """);
    }

    @Test
    public void testGetAllWithGenericVariableParam()
    {

        this.compileTestSourceM3("sourceId.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function testGetAllAsFunc<T>(clazz:Class<T>[1]):Any[*]
                {
                   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);
                }
                """);
    }

    /**
     * bitemporal
     */

    @Test
    public void testBiTemporalGetAllWithCorrectTemporalDateParams() throws Exception
    {
        String domain = """
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}
                function go():Any[*]
                {
                   {|meta::relational::tests::milestoning::Location.all(%9999, %2017-5-26)};\
                }
                """;

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testBiTemporalGetAllWithNoTemporalDateParams() throws Exception
    {
        String domain = """
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location
                {
                  place : String[1];
                }
                function go():Any[*]
                {
                   {|meta::relational::tests::milestoning::Location.all()};
                }
                """;

        runtime.createInMemorySource("domain.pure", domain);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:domain.pure line:7 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testBiTemporalGetAllWithInsufficientTemporalDateParams() throws Exception
    {
        String domain = """
                Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location
                {
                  place : String[1];
                }
                function go():Any[*]
                {
                   {|meta::relational::tests::milestoning::Location.all(%9999)};
                }
                """;

        runtime.createInMemorySource("domain.pure", domain);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        Assertions.assertEquals("Compilation error at (resource:domain.pure line:7 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }
}



