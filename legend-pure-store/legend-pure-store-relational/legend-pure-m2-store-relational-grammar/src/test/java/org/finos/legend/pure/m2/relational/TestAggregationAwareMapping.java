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

import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

public class TestAggregationAwareMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testAggregationAwareMappingGrammarSingleAggregate()
    {
        String source = """
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assertions.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assertions.assertEquals("a", aggSetImpl._id());

        Assertions.assertNotNull(aggSetImpl._mainSetImplementation());
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 1);

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiAggregate()
    {
        String source = """
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
                               ~canAggregate false,
                               ~groupByFunctions (
                                  $this.salesDate.fiscalYear,
                                  $this.salesDate.fiscalQtr
                               ),
                               ~aggregateValues (
                                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                               )
                            },
                            ~aggregateMapping : Relational {
                               salesDate (\s
                                     fiscalQtr : [db]@sales_qtr_calendar | calendar.fiscal_qtr,\s
                                     fiscalYear : [db]@sales_qtr_calendar | calendar.fiscal_year
                                  ),
                               revenue : [db]sales_by_qtr.net_revenue
                            }
                         ),
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
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assertions.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assertions.assertEquals("a", aggSetImpl._id());

        Assertions.assertNotNull(aggSetImpl._mainSetImplementation());
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 1);

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiViewsMultiAggregateValues()
    {
        String source = """
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
                               ~canAggregate false,
                               ~groupByFunctions (
                                  $this.salesDate.fiscalYear,
                                  $this.salesDate.fiscalQtr
                               ),
                               ~aggregateValues (
                                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                               )
                            },
                            ~aggregateMapping : Relational {
                               salesDate (\s
                                     fiscalQtr : [db]@sales_qtr_calendar | calendar.fiscal_qtr,\s
                                     fiscalYear : [db]@sales_qtr_calendar | calendar.fiscal_year
                                  ),
                               revenue : [db]sales_by_qtr.net_revenue
                            }
                         ),
                         (
                            ~modelOperation : {
                               ~canAggregate true,
                               ~groupByFunctions (
                                  $this.salesDate
                               ),
                               ~aggregateValues (
                                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() ),
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
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation)((Mapping)this.runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assertions.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assertions.assertEquals("a", aggSetImpl._id());

        Assertions.assertNotNull(aggSetImpl._mainSetImplementation());
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 2);

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof RootRelationalInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationTarget()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                            salesDate [b] : [db]@sales_calendar_nonExistent,
                            revenue : revenue
                         )
                      }
                   }
                )\
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:67 column:34), \"The join 'sales_calendar_nonExistent' has not been found in the database 'db'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                            salesDate_nonExistent [b] : [db]@sales_calendar,
                            revenue : revenue
                         )
                      }
                   }
                )\
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:67 column:13), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewTarget()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                                  salesDate [b] : [db]@sales_date_calendar_nonExistent,
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
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:58 column:40), \"The join 'sales_date_calendar_nonExistent' has not been found in the database 'db'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                                  salesDate_nonExistent [b] : [db]@sales_date_calendar,
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
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:58 column:19), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationGroupByFunction()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                                  $this.salesDate_nonExistent
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
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:48 column:25), \"Can't find the property 'salesDate_nonExistent' in the class Sales\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationAggregateFunction()
    {
        this.runtime.createInMemorySource("mapping.pure",
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
                
                ###Relational
                Database db\s
                (
                   Table sales_base (id INT PRIMARY KEY, sales_date DATE, revenue FLOAT)
                   Table calendar (date DATE PRIMARY KEY, fiscal_year INT, fiscal_qtr INT, fiscal_month INT)
                  \s
                   Table sales_by_date (sales_date DATE, net_revenue FLOAT)
                  \s
                   Join sales_calendar (sales_base.sales_date = calendar.date)
                   Join sales_date_calendar (sales_by_date.sales_date = calendar.date)
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
                                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->summation() )
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
                """);
        try
        {
            this.runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:51 column:67), \"The system can't find a match for the function: summation(_:Float[*])\"", e.getMessage());
        }
    }
}
