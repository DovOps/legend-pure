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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGetAllVersionsInRangeValidator extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("source.pure");
        runtime.delete("test.pure");
    }

    @Test
    public void testAllVersionsInRangeForBusinessTemporal()
    {
        runtime.createInMemorySource("source.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};
                }
                
                """);
        runtime.compile();
    }

    @Test
    public void testAllVersionsInRangeForProcessingTemporal()
    {
        runtime.createInMemorySource("source.pure",
                """
                import meta::test::milestoning::domain::*;
                Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                   id : Integer[1];
                }
                function go():Any[*]
                {
                   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};
                }
                
                """);
        runtime.compile();
    }

    @Test
    public void testAllVersionsInRangeForBiTemporal()
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {
            runtime.createInMemorySource("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{
                       id : Integer[1];
                    }
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};
                    }
                    
                    """);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:test.pure line:7 column:19), \".allVersionsInRange() is applicable only for businessTemporal and processingTemporal types"));
    }

    @Test
    public void testAllVersionsInRangeForNonTemporal()
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {
            runtime.createInMemorySource("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    Class meta::test::milestoning::domain::Product{
                       id : Integer[1];
                    }
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};
                    }
                    
                    """);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:test.pure line:7 column:19), \".allVersionsInRange() is applicable only for businessTemporal and processingTemporal types"));
    }

    @Test
    public void testLatestDateUsageForAllVersionsInRangeForBusinessTemporal()
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {
            runtime.createInMemorySource("source.pure",
                    """
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                       id : Integer[1];
                    }
                    """);
            runtime.compile();
            runtime.createInMemorySource("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%latest, %2018-1-1)};
                    }
                    
                    """);
            runtime.compile();
            runtime.modify("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%2018-1-1, %latest)};
                    }
                    
                    """);
            runtime.compile();
            runtime.modify("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%latest, %latest)};
                    }
                    
                    """);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()"));
    }

    @Test
    public void testLatestDateUsageForAllVersionsInRangeForProcessingTemporal()
    {
        Throwable exception = assertThrows(PureCompilationException.class, () -> {
            runtime.createInMemorySource("source.pure",
                    """
                    Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{
                       id : Integer[1];
                    }
                    """);
            runtime.compile();
            runtime.createInMemorySource("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%latest, %2018-1-1)};
                    }
                    
                    """);
            runtime.compile();
            runtime.modify("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%2018-1-1, %latest)};
                    }
                    
                    """);
            runtime.compile();
            runtime.modify("test.pure",
                    """
                    import meta::test::milestoning::domain::*;
                    function go():Any[*]
                    {
                       let f={|Product.allVersionsInRange(%latest, %latest)};
                    }
                    
                    """);
            runtime.compile();
        });
        assertTrue(exception.getMessage().contains("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()"));
    }
}
