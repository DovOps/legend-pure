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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeAggregationAwareMapping extends AbstractPureRelationalTestWithCoreCompiled
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
            native function sum(f:Float[*]):Float[1];
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
            native function sum(f:Float[*]):Float[1];
            """;

    private static final String mapping = """
            ###Relational
            Database db\s
            (
               Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
               Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
              \s
               Table sales_by_date (sales_date DATE, net_revenue FLOAT)
               Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)
              \s
               Join sales_calendar (sales_base.sales_date = calendar.date)
               Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
               Join sales_qtr_calendar (sales_by_qtr.sales_qtr_first_date = calendar.date)
            
            )
            
            ###Mapping
            Mapping map
            (
               FiscalCalendar [b] : Relational {
                  scope([db]calendar)
                  (
                     date : date,
                     fiscalYear : fiscal_year,
                     fiscalQtr : fiscal_qtr,
                     fiscalMonth : fiscal_month
                  )
               }
              \s
               Sales [a] : AggregationAware {
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
                        ~aggregateMapping : Relational {
                           scope([db]sales_by_date)
                           (
                              salesDate [b] : [db]@sales_date_calendar,
                              revenue : net_revenue
                           )
                        }
                     )
                  ],
                  ~mainMapping : Relational {
                     scope([db]sales_base)
                     (
                        salesDate [b] : [db]@sales_calendar,
                        revenue : revenue
                     )
                  }
               }
            )\
            """;

    private static final String mappingWithSalesPersonDimension = """
            ###Relational
            Database db\s
            (
               Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT, personID INT)
               Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
               Table person(ID INT PRIMARY KEY, last_name VARCHAR(100))
              \s
               Table sales_by_date (sales_date DATE, net_revenue FLOAT)
               Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)
              \s
               Join sales_calendar (sales_base.sales_date = calendar.date)
               Join sales_person (sales_base.personID = person.ID)
               Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
               Join sales_qtr_calendar (sales_by_qtr.sales_qtr_first_date = calendar.date)
            
            )
            
            ###Mapping
            Mapping map
            (
               FiscalCalendar [b] : Relational {
                  scope([db]calendar)
                  (
                     date : date,
                     fiscalYear : fiscal_year,
                     fiscalQtr : fiscal_qtr,
                     fiscalMonth : fiscal_month
                  )
               }
              \s
               Person [c] : Relational {
                  scope([db]person)
                  (
                     lastName: last_name
                  )
               }
              \s
               Sales [a] : AggregationAware {
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
                        ~aggregateMapping : Relational {
                           scope([db]sales_by_date)
                           (
                              salesDate [b] : [db]@sales_date_calendar,
                              salesPerson [p] : [db]@sales_date_calendar,
                              revenue : net_revenue
                           )
                        }
                     )
                  ],
                  ~mainMapping : Relational {
                     scope([db]sales_base)
                     (
                        salesDate [b] : [db]@sales_calendar,
                        revenue : revenue
                     )
                  }
               }
            )\
            """;

    private static final String mappingWithFunction = """
            ###Relational
            Database db\s
            (
               Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
               Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
              \s
               Table sales_by_date (sales_date DATE, net_revenue FLOAT)
               Table sales_by_qtr (sales_qtr_first_date DATE, net_revenue FLOAT)
              \s
               Join sales_calendar (sales_base.sales_date = calendar.date)
               Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
               Join sales_qtr_calendar (sales_by_qtr.sales_qtr_first_date = calendar.date)
            
            )
            
            ###Mapping
            Mapping map
            (
               FiscalCalendar [b] : Relational {
                  scope([db]calendar)
                  (
                     date : date,
                     fiscalYear : fiscal_year,
                     fiscalQtr : fiscal_qtr,
                     fiscalMonth : fiscal_month
                  )
               }
              \s
               Sales [a] : AggregationAware {
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
                        ~aggregateMapping : Relational {
                           scope([db]sales_by_date)
                           (
                              salesDate [b] : [db]@sales_date_calendar,
                              revenue : net_revenue
                           )
                        }
                     )
                  ],
                  ~mainMapping : Relational {
                     scope([db]sales_base)
                     (
                        salesDate [b] : [db]@sales_calendar,
                        revenue : revenue
                     )
                  }
               }
            )\
            """;

    private static final String function = "function myFunction(d: FiscalCalendar[1]) : FiscalCalendar[1] {$d}";

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
                        .compileWithExpectedCompileFailure("The system can't find a match for the function: myFunction(_:FiscalCalendar[1])", "source2.pure", 35, 36)
                        .updateSource("source3.pure", function)
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
