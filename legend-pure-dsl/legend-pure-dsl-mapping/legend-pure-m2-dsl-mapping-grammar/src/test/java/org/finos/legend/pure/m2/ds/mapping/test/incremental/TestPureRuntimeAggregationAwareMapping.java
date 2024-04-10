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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class TestPureRuntimeAggregationAwareMapping extends AbstractPureMappingTestWithCoreCompiled
{
    private static final String model =
            """
            ###Pure
            Class Sales
            {
               id: Integer[1];
               salesDate: FiscalCalendar[1];
               revenue: Float[1];
            }
            
            Class FiscalCalendar
            {
               date: Date[1];
               fiscalYear: Integer[1];
               fiscalMonth: Integer[1];
               fiscalQtr: Integer[1];
            }
            
            Class Sales_By_Date
            {
               salesDate: FiscalCalendar[1];
               netRevenue: Float[1];
            }
            function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]
            {
                $numbers->plus();
            }
            """;

    private static final String modelWithSalesPersonDimension =
            """
            ###Pure
            Class Sales
            {
               id: Integer[1];
               salesDate: FiscalCalendar[1];
               salesPerson: Person[1];
               revenue: Float[1];
            }
            Class Person
            {
               lastName: String[1];
            }
            
            Class FiscalCalendar
            {
               date: Date[1];
               fiscalYear: Integer[1];
               fiscalMonth: Integer[1];
               fiscalQtr: Integer[1];
            }
            
            Class Sales_By_Date
            {
               salesDate: FiscalCalendar[1];
               netRevenue: Float[1];
            }\
            function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]
            {
                $numbers->plus();
            }
            """;

    private static final String mapping = """
            ###Mapping
            Mapping map
            (
               FiscalCalendar : Pure {
                  ~src FiscalCalendar
                  date : $src.date,
                  fiscalYear : $src.fiscalYear,
                  fiscalMonth : $src.fiscalMonth,
                  fiscalQtr : $src.fiscalQtr
               }
              \s
               Sales : AggregationAware {
                  Views : [
                     (
                        ~modelOperation : {
                           ~canAggregate true,
                           ~groupByFunctions (
                              $this.salesDate
                           ),
                           ~aggregateValues (
                              ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                           )
                        },
                        ~aggregateMapping : Pure {
                           ~src Sales_By_Date
                           salesDate : $src.salesDate,
                           revenue : $src.netRevenue
                        }
                     )
                  ],
                  ~mainMapping : Pure {
                     ~src Sales
                     salesDate : $src.salesDate,
                     revenue : $src.revenue
                  }
               }
            )\
            """;

    private static final String mappingWithSalesPersonDimension = """
            ###Mapping
            Mapping map
            (
               FiscalCalendar : Pure {
                  ~src FiscalCalendar
                  date : $src.date,
                  fiscalYear : $src.fiscalYear,
                  fiscalMonth : $src.fiscalMonth,
                  fiscalQtr : $src.fiscalQtr
               }
               Person : Pure {
                  ~src Person
                  lastName : $src.lastName
               }
              \s
               Sales : AggregationAware {
                  Views : [
                     (
                        ~modelOperation : {
                           ~canAggregate true,
                           ~groupByFunctions (
                              $this.salesDate
                           ),
                           ~aggregateValues (
                              ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                           )
                        },
                        ~aggregateMapping : Pure {
                           ~src Sales_By_Date
                           salesDate : $src.salesDate,
                           revenue : $src.netRevenue
                        }
                     )
                  ],
                  ~mainMapping : Pure {
                     ~src Sales
                     salesDate : $src.salesDate,
                     salesPerson : $src.salesPerson,
                     revenue : $src.revenue
                  }
               }
            )\
            """;

    private static final String mappingWithFunction = """
            ###Mapping
            Mapping map
            (
               FiscalCalendar : Pure {
                  ~src FiscalCalendar
                  date : $src.date,
                  fiscalYear : $src.fiscalYear,
                  fiscalMonth : $src.fiscalMonth,
                  fiscalQtr : $src.fiscalQtr
               }
              \s
               Sales : AggregationAware {
                  Views : [
                     (
                        ~modelOperation : {
                           ~canAggregate true,
                           ~groupByFunctions (
                              $this.salesDate->myFunction()
                           ),
                           ~aggregateValues (
                              ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                           )
                        },
                        ~aggregateMapping : Pure {
                           ~src Sales_By_Date
                           salesDate : $src.salesDate,
                           revenue : $src.netRevenue
                        }
                     )
                  ],
                  ~mainMapping : Pure {
                     ~src Sales
                     salesDate : $src.salesDate,
                     revenue : $src.revenue
                  }
               }
            )\
            """;

    private static final String function = "function myFunction(d: FiscalCalendar[1]) : FiscalCalendar[1] {$d}";

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");
        runtime.delete("source3.pure");
    }

    @Test
    public void testCreateAndDeleteAggregationAwareMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).compile(),
                new RuntimeTestScriptBuilder().createInMemorySource("source2.pure", mapping).compile().deleteSource("source2.pure").compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteNewPropertyInModel() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mapping).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", modelWithSalesPersonDimension)
                        .compile()
                        .updateSource("source1.pure", model)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteNewPropertyAndMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mapping).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source1.pure", modelWithSalesPersonDimension).updateSource("source2.pure", mappingWithSalesPersonDimension)
                        .compile()
                        .updateSource("source1.pure", model).updateSource("source2.pure", mapping)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testDeleteFunctionUsedInAggregationAwareMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("source1.pure", model).createInMemorySource("source2.pure", mappingWithFunction).createInMemorySource("source3.pure", function).compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source3.pure", "//" + function)
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: myFunction(_:FiscalCalendar[1])", "source2.pure", 18, 36)
                        .updateSource("source3.pure", function)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
