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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

public class TestAggregationAwareMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
    }

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
                
                Class Sales_By_Date
                {
                   salesDate: FiscalCalendar[1];
                   netRevenue: Float[1];
                }
                
                function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]
                {
                    $numbers->plus();
                }
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) ((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("map"))._classMappings().toSortedListBy(SetImplementation::_id).get(0);

        Assertions.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assertions.assertEquals("a", aggSetImpl._id());

        Assertions.assertNotNull(aggSetImpl._mainSetImplementation());
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().size());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size());

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
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
                
                Class Sales_By_Date
                {
                   salesDate: FiscalCalendar[1];
                   netRevenue: Float[1];
                }
                
                Class Sales_By_Qtr
                {
                   salesQtrFirstDate: FiscalCalendar[1];
                   netRevenue: Float[1];
                }
                
                function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]
                {
                    $numbers->plus();
                }
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Qtr
                               salesDate [b] : $src.salesQtrFirstDate,
                               revenue : $src.netRevenue
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) ((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
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
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertEquals(2, aggSetImpl._aggregateSetImplementations().size());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertEquals(2, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size());

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size());

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
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
                
                Class Sales_By_Date
                {
                   salesDate: FiscalCalendar[1];
                   netRevenue: Float[1];
                }
                
                Class Sales_By_Qtr
                {
                   salesQtrFirstDate: FiscalCalendar[1];
                   netRevenue: Float[1];
                }
                
                function meta::pure::functions::math::sum(numbers:Float[*]):Float[1]
                {
                    $numbers->plus();
                }
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Qtr
                               salesDate [b] : $src.salesQtrFirstDate,
                               revenue : $src.netRevenue
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) ((org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("map"))._classMappings().toSortedList(new Comparator<SetImplementation>()
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
        Assertions.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assertions.assertEquals(2, aggSetImpl._aggregateSetImplementations().size());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assertions.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assertions.assertEquals(2, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size());

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assertions.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assertions.assertEquals(1, aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size());
        Assertions.assertEquals(2, aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size());

        Assertions.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
        Assertions.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationTarget()
    {
        runtime.createInMemorySource("mapping.pure",
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
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate_NonExistent,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:59 column:31), \"Can't find the property 'salesDate_NonExistent' in the class Sales\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationProperty()
    {
        runtime.createInMemorySource("mapping.pure",
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
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate_nonExistent [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:59 column:10), \"The property 'salesDate_nonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewTarget()
    {
        runtime.createInMemorySource("mapping.pure",
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
                
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate_NonExistent,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:52 column:37), \"Can't find the property 'salesDate_NonExistent' in the class Sales_By_Date\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewProperty()
    {
        runtime.createInMemorySource("mapping.pure",
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
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate_NonExistent [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:52 column:16), \"The property 'salesDate_NonExistent' is unknown in the Element 'Sales'\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationGroupByFunction()
    {
        runtime.createInMemorySource("mapping.pure",
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
                
                
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
                   }
                  \s
                   Sales [a] : AggregationAware {
                      Views : [
                         (
                            ~modelOperation : {
                               ~canAggregate true,
                               ~groupByFunctions (
                                  $this.salesDate_NonExistent
                               ),
                               ~aggregateValues (
                                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->sum() )
                               )
                            },
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:41 column:25), \"Can't find the property 'salesDate_NonExistent' in the class Sales\"", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationAggregateFunction()
    {
        runtime.createInMemorySource("mapping.pure",
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
                
                
                ###Mapping
                Mapping map
                (
                   FiscalCalendar [b] : Pure {
                      ~src FiscalCalendar
                      date : $src.date,
                      fiscalYear : $src.fiscalYear,
                      fiscalMonth : $src.fiscalMonth,
                      fiscalQtr : $src.fiscalQtr
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
                            ~aggregateMapping : Pure {
                               ~src Sales_By_Date
                               salesDate [b] : $src.salesDate,
                               revenue : $src.netRevenue
                            }
                         )
                      ],
                      ~mainMapping : Pure {
                         ~src Sales
                         salesDate [b] : $src.salesDate,
                         revenue : $src.revenue
                      }
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Compilation error at (resource:mapping.pure line:44 column:67), \"The system can't find a match for the function: summation(_:Float[*])\"", e.getMessage());
        }
    }
}
