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

package org.finos.legend.pure.m2.inlinedsl.graph;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMilestonedPropertyUsageInGraph extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
    }

    @Test
    public void testGeneratedQualifiedPropertyUsage() throws Exception
    {
        try
        {
            runtime.createInMemorySource("file.pure", """
                    import meta::test::milestoning::domain::*;
                    Class meta::test::milestoning::domain::Product{
                       classification : Classification[1];
                    }
                    Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                       classificationType : String[1];
                    }
                    function go():Any[*]
                    {
                       print(#{Product{classification{classificationType}}}#)\
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:10 column:20), \"The system can't find a match for the property / qualified property: classification(). No-Arg milestoned property: 'classification' is not supported yet in graph fetch flow! It needs to be supplied with [businessDate] parameters\"", e.getMessage());
        }

        runtime.modify("file.pure", """
                import meta::test::milestoning::domain::*;
                Class meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationType : String[1];
                }
                function go():Any[*]
                {
                   print(#{Product{classification(%latest){classificationType}}}#, 1)\
                }
                """);
        runtime.compile();
    }

    @Test
    public void testQualifiedPropertyInference() throws Exception
    {
        try
        {
            runtime.createInMemorySource("file.pure", """
                    import meta::test::milestoning::domain::*;
                    Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                       classification : Classification[1];
                    }
                    Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                       classificationType : String[1];
                    }
                    function go():Any[*]
                    {
                       print(#{Product{classification{classificationType}}}#)\
                    }
                    """);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:file.pure line:10 column:20), \"No-Arg milestoned property: 'classification' is not supported yet in graph fetch flow! It needs to be supplied with [businessDate] parameters\"", e.getMessage());
        }

        runtime.modify("file.pure", """
                import meta::test::milestoning::domain::*;
                Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{
                   classification : Classification[1];
                }
                Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{
                   classificationType : String[1];
                }
                function go():Any[*]
                {
                   print(#{Product{classification(%2015-01-01){classificationType}}}#, 1)\
                }
                """);
        runtime.compile();
    }
}
