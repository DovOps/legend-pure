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

package org.finos.legend.pure.m3.tests.elements.function.inference;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.*;

import java.util.Optional;

public class TestFunctionTypeInference extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final boolean shouldSetTypeInferenceObserver = false;
    private static final String typeInferenceTestProperty = "pure.typeinference.test";
    private static boolean typeInferenceTestPropertySet = false;
    private static String previousTypeInferenceTest;

    private static final String inferenceTestFileName = "inferenceTest.pure";

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());

        if (shouldSetTypeInferenceObserver)
        {
            previousTypeInferenceTest = System.setProperty(typeInferenceTestProperty, "true");
            typeInferenceTestPropertySet = true;
        }
    }

    @AfterAll
    public static void cleanUp()
    {
        if (typeInferenceTestPropertySet)
        {
            if (previousTypeInferenceTest == null)
            {
                System.clearProperty(typeInferenceTestProperty);
            }
            else
            {
                System.setProperty(typeInferenceTestProperty, previousTypeInferenceTest);
            }
        }
    }

    @AfterEach
    public void clearRuntime()
    {
        deleteInferenceTest();
    }

    @Test
    public void inferClassTypeParameter()
    {
        compileInferenceTest(
                """
                Class MyClass<Z>{value:Z[1];}
                function f<T>(s:T[*]):MyClass<T>[1]{^MyClass<T>(value='ok')}
                function test():Any[*]{f(['a','b']).value+'ee';}
                """);
        deleteInferenceTest();

        compileInferenceTest(
                """
                Class MyClass<Z>{value:Z[1];}
                function f<T>(s:T[*]):MyClass<T>[1]{^MyClass<T>(value='ok')}
                function test():Any[*]{f([1,2]).value+3;}
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                Class MyClass<Z>{value:Z[1];}
                function f<T>(s:T[*]):MyClass<T>[1]{^MyClass<T>(value='ok')}
                function test():Any[*]{f([1,2]).value+'3';}
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", inferenceTestFileName, 3, 38, e);
    }

    @Test
    public void inferMultiplicityParameter()
    {
        compileInferenceTest(
                """
                function f<|m>(s:String[m]):String[m]{$s}
                function pl(a:String[1], b:String[1]):String[1]{$a+$b}
                function test():Any[*]{f('a')->pl('ok')}
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                function f<|m>(s:String[m]):String[m]{$s}
                function pl(a:String[1], b:String[1]):String[1]{$a+$b}
                function test():Any[*]{f(['a','b'])->pl('ok')}
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "pl(_:String[2],_:String[1])\n" +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                "\tpl(String[1], String[1]):String[1]\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, inferenceTestFileName, 3, 38, e);
    }

    @Test
    public void inferMultipleParametersInLambda()
    {
        compileInferenceTest(
                """
                Profile decision
                {
                   tags: [name, id, domainType, dataType, include];
                }
                native function meta::pure::functions::meta::value4Tag(f:ElementWithTaggedValues[1], tagName:String[1], profile:Profile[1]):TaggedValue[*];
                function <<access.private>> datamarts::dtm::domain::bdm::sapiens::impl::getTagValue(p:ElementWithTaggedValues[1], tagName:String[1]):String[1] {
                   $p->value4Tag($tagName, decision)->fold({t, str:String[1]|$t.value}, ''); \s
                }
                """);
    }

    @Test
    public void inferVariableTypeOfFunctionUsedAsParameter()
    {
        compileInferenceTest(
                """
                function f<T>(s:Function<{->T[1]}>[1]):T[1]{$s->eval();}
                function test():Any[*]{f(|1)+1}
                """);
        deleteInferenceTest();

        compileInferenceTest(
                """
                function f<T>(s:Function<{->T[1]}>[1]):T[1]{$s->eval();}
                function test():Any[*]{f(|'1')+'1'}
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                function f<T>(s:Function<{->T[1]}>[1]):T[1]{$s->eval();}
                function test():Any[*]{f(|'1')+1}
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", inferenceTestFileName, 2, 31, e);
    }

    @Test
    public void inferLambdaTypeParameterUsingMultiPass()
    {
        compileInferenceTest(
                """
                Class MyClass<Z>{}
                function g<K>(f:Function<{K[1]->Boolean[1]}>[1]):MyClass<K>[1]{^MyClass<K>()}
                function f<T>(s:T[*], a:MyClass<T>[1]):String[1]{'a'}
                function test():Any[*]{f(['a','b'], g(t|$t->startsWith('ee')))}
                """);
        deleteInferenceTest();

        compileInferenceTest(
                """
                Class MyClass<Z>{}
                function g<K>(f:Function<{K[1]->Boolean[1]}>[1]):MyClass<K>[1]{^MyClass<K>()}
                function f<T>(s:T[*], a:MyClass<T>[1]):String[1]{'a'}
                function test():Any[*]{f([1,2], g(t|let e = $t+3; $e == 2;))}
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                Class MyClass<Z>{}
                function g<K>(f:Function<{K[1]->Boolean[1]}>[1]):MyClass<K>[1]{^MyClass<K>()}
                function f<T>(s:T[*], a:MyClass<T>[1]):String[1]{'a'}
                function test():Any[*]{f(['a','b'], g(t|let e = $t+3; $e == 2;))}
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", inferenceTestFileName, 4, 51, e);
    }

    @Test
    public void ensureNoCrashWhenAnyIsUsedForAFunctionType()
    {
        compileInferenceTest(
                """
                function f<T>(a:T[*]):T[*]
                {
                     $a;
                }
                function t(f:Function<Any>[1]):Any[*]
                {
                     $f;
                }
                function go():Any[*]
                {
                     t(f_T_MANY__T_MANY_);
                }
                """
        );
    }

    @Test
    public void ensureInferenceIsResilientEnoughToHaveFunctionsProcessedDuringFunctionExpressionProcessing()
    {
        compileInferenceTest(
                """
                Class TabularDataSet{}
                function project<T>(set:T[*], paths:meta::pure::metamodel::path::Path<T,Any|*>[*]):TabularDataSet[1]{^TabularDataSet()}
                Class QueryFunctionPair extends Pair<meta::pure::metamodel::function::Function<Any>, meta::pure::metamodel::function::Function<{->String[1]}>>{}
                Class meta::pure::metamodel::path::Path<-U,V|m> extends Function<{U[1]->V[m]}>
                {
                }\
                function f<T>(a:T[*]):T[*]
                {
                     $a;
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let dispatch = [
                             ^QueryFunctionPair(first = project_T_MANY__Path_MANY__TabularDataSet_1_, second = | p($k)),
                             ^QueryFunctionPair(first = f_T_MANY__T_MANY_, second = | p($k))
                        ]
                }
                function p(o:FunctionExpression[1]):String[1]
                {'l';}
                """);
    }

    @Test
    public void inferTheTypeOfParametersOfACollectionOfLambdas()
    {
        compileInferenceTest(
                """
                Class Person{age:Integer[1];}
                function tt<T>(a:T[*],e:Function<{T[1]->Integer[1]}>[*]):Any[*]
                {
                 'aa';
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let f = tt([1,2,3],[f|$f+2, f|$f+4]);
                }
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                Class Person{age:Integer[1];}
                function tt<T>(a:T[*],e:Function<{T[1]->Integer[1]}>[*]):Any[*]
                {
                 'aa';
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let f = tt([1,2,3],[f|$f+'2', f|$f+4]);
                }
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", inferenceTestFileName, 8, 28, e);
    }

    @Test
    public void ensureProperFailureWhenTheLambdaWithACollectionHaveDifferentParametersCount()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                Class Person{age:Integer[1];}
                function tt<T>(a:T[*],e:Function<Any>[*]):Any[*]
                {
                 'aa';
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let f = tt([1,2,3],[f|2, {f,k|4}]);
                }
                """));
        assertPureException(PureCompilationException.class, "Can't infer the parameters' types for the lambda. Please specify it in the signature.", inferenceTestFileName, 8, 24, e);
    }

    @Test
    public void testFunctionTypeForListOfAbstractProperties()
    {
        compileInferenceTest(
                """
                function test::propByName(name:String[1]):AbstractProperty<Any>[0..1]
                {
                  []
                }
                
                function test::testFn():Any[*]
                {
                  [test::propByName('name1')->toOne(),
                   test::propByName('name2')->toOne()]
                }\
                """);
        CoreInstance testFn = runtime.getFunction("test::testFn():Any[*]");
        Assertions.assertNotNull(testFn);
        CoreInstance firstExpression = testFn.getValueForMetaPropertyToOne(M3Properties.expressionSequence);
        CoreInstance genericType = firstExpression.getValueForMetaPropertyToOne(M3Properties.genericType);
        Assertions.assertEquals("meta::pure::metamodel::function::property::AbstractProperty<meta::pure::metamodel::type::Any>", GenericType.print(genericType, true, processorSupport));
    }

    @Test
    public void inferTheTypeOfParametersOfACollectionOfLambdasAndFunctions()
    {
        compileInferenceTest(
                """
                function tt<T>(a:T[*],e:Function<{T[1]->String[1]}>[*]):Any[*]
                {
                 'aa';
                }
                function a(e:Integer[1]):String[1]
                {
                 'ee';
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let f = tt([1,2,3],[z|let k = $z+2; $k->toString();, a_Integer_1__String_1_]);
                }
                """);
        deleteInferenceTest();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                function tt<T>(a:T[*],e:Function<{T[1]->Integer[1]}>[*]):Any[*]
                {
                 'aa';
                }
                function a(e:Integer[1]):String[1]
                {
                 'ee';
                }
                function a(k:FunctionExpression[1]):Any[*]
                {
                   let f = tt([1,2,3],[f|$f+2, a_Integer_1__String_1_]);
                }
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "tt(_:Integer[3],_:FunctionDefinition<{Integer[1]->Any[1]}>[2])\n" +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                "\ttt(T[*], Function<{T[1]->Integer[1]}>[*]):Any[*]\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, inferenceTestFileName, 11, 12, e);
    }

    @Test
    public void typeInferenceNested()
    {
        compileInferenceTest(
                """
                Class TypeInferenceTest
                {
                   name: String[1];
                   number: Integer[1];
                }
                
                function typInferenceTest():Any[*]
                {
                   let result2 = [^TypeInferenceTest(name='David',  number=23)];
                    $result2->A(b(c(p|$p.name)));
                }
                
                
                function A<T>(sets:T[*], b:BA<T>[1]): Any[*]
                {
                   print('resolved',1);  \s
                }
                
                function c<V>(f:Function<{V[1]->Any[1]}>[1]): CA<V>[1]
                {
                   ^CA<V>
                   (
                   )
                }
                
                function b<U>(c:CA<U>[1]): BA<U>[1]
                {
                   ^BA<U>
                   (
                      c=$c
                   )
                }
                
                Class BA<U>
                {
                   c: CA<U>[1];
                }
                
                Class CA<V>
                {
                }\
                """);
    }

    @Test
    public void testIfType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                function test():Any[*]
                {
                    let r = if(true,|'a',|1)->toOne();
                    $r + 2;
                }\
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "plus(_:Any[2])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::math::plus(Decimal[*]):Decimal[1]\n" +
                "\tmeta::pure::functions::math::plus(Float[*]):Float[1]\n" +
                "\tmeta::pure::functions::math::plus(Integer[*]):Integer[1]\n" +
                "\tmeta::pure::functions::math::plus(Number[*]):Number[1]\n" +
                "\tmeta::pure::functions::string::plus(String[*]):String[1]\n", inferenceTestFileName, 4, 8, e);
    }

    @Test
    public void testIfMul()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileInferenceTest(
                """
                function a(a:Integer[1],b:Integer[1]):Integer[1]
                {
                    $a + $b;
                }
                function test():Any[*]
                {
                    let r = if(true,|[1,2],|1);
                    a($r, 2);
                }\
                """));
        assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "a(_:Integer[1..2],_:Integer[1])\n" +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                "\ta(Integer[1], Integer[1]):Integer[1]\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, inferenceTestFileName, 8, 5, e);
    }

    @Test
    public void inferTypeParameterUpAndDownWithNestedFunctionArrayDiffAgg()
    {
        compileInferenceTest(
                """
                Class meta::pure::functions::collection::AggregateValue<A,B,C>
                {
                   mapFn : FunctionDefinition<{A[1]->B[1]}>[1];
                   aggregateFn : FunctionDefinition<{B[*]->C[1]}>[1];
                }
                Class meta::pure::tds::TabularDataSet
                {
                }
                native function sum(s:Number[*]):Number[1];\
                native function count(s:Any[*]):Integer[1];\
                function meta::pure::functions::collection::agg<K,L,M>(mapFn:FunctionDefinition<{K[1]->L[1]}>[1], aggregateFn:FunctionDefinition<{L[*]->M[1]}>[1]):meta::pure::functions::collection::AggregateValue<K,L,M>[1]
                {
                   ^meta::pure::functions::collection::AggregateValue<K,L,M>(mapFn=$mapFn, aggregateFn=$aggregateFn);
                }
                function meta::pure::functions::collection::groupBy<T,V,U>(set:T[*], functions:meta::pure::metamodel::function::Function<{T[1]->Any[*]}>[*], aggValues:meta::pure::functions::collection::AggregateValue<T,V,U>[*], ids:String[*]):TabularDataSet[1]
                {
                   fail('Currently only supported in the SQL flow!');
                   ^TabularDataSet();
                }
                Class Trade
                {
                   tradeId : String[1];
                   quantity : Float[1];
                }
                
                function go():Any[*]
                {
                      Trade.all()
                         ->groupBy([f|$f.quantity],
                                  [
                                     meta::pure::functions::collection::agg(f|$f.tradeId, f|$f->count()),
                                     meta::pure::functions::collection::agg(x|$x.quantity, x|$x->sum())
                                  ],
                            ['region', 'total', 'count']);
                }\
                """);
    }

    @Test
    public void infersToOneEvaluation()
    {
        compileInferenceTest(
                """
                function test():Integer[1]
                {
                   toOne_T_MANY__T_1_->eval([1]);
                }
                """);
    }

    @Test
    public void infersEvalFromFunctionReference()
    {
        compileInferenceTest(
                """
                Class Simple
                {
                }
                
                function test():Any[*]
                {
                   getAll_Class_1__T_MANY_->eval(Simple);
                }\
                """);
    }

    @Test
    public void infersEvalFromFunctionAsParameter()
    {
        compileInferenceTest(
                """
                Class Simple
                {
                }
                
                function test<T>(func:Function<{Class<T>[1]->T[*]}>[1]):Any[*]
                {
                   getAll_Class_1__T_MANY_->eval(Simple);
                }\
                """);
    }

    @Test
    public void testProperties()
    {
        compileInferenceTest(
                """
                Class A{p(s:String[1]){$s}:String[1];}
                native function meta::pure::functions::meta::functionType(f:Function<Any>[1]):FunctionType[1];
                native function meta::pure::functions::collection::sortBy<T,U|m>(col:T[m], key:Function<{T[1]->U[1]}>[0..1]):T[m];
                function filterToSimpleFunctionProperties(qualifiedProperties : QualifiedProperty<Any>[*]) : QualifiedProperty<Any>[*]
                {
                   $qualifiedProperties->filter(p|$p->functionType().parameters->size() == 1);
                }
                function test(c : Class<Any>[1], propertyFilterFunc : Function<{AbstractProperty<Any>[1]->Boolean[1]}>[1]):Any[*]
                {
                                                   let prop = $c.qualifiedProperties
                                                             ->concatenate($c.propertiesFromAssociations)
                                                             ->sortBy(p|$p.name->toOne());
                                                   $prop->map(p|$p.name);
                }
                """);
    }

    @Test
    public void testTypePropagation()
    {
        compileInferenceTest("function f(a:Pair<String, String>[*]):String[*]{$a->filter(p|$p.first == 'name').second}");
    }

    @Test
    public void testMulPropagation()
    {
        compileInferenceTest(
                """
                Class Address{name:String[1];}
                function a():Boolean[1]
                {
                   let address = ^Address(name='Hoboken, NJ');
                   assert('Hoboken, NJ' == {a | $a.name}->eval($address), |'');
                }\
                """);
    }

    @Test
    public void testLambdaType()
    {
        compileInferenceTest(
                """
                Class Person{lastName:String[1];}
                function a():Boolean[1]
                {
                   let name = 'ee';
                   let lambda = {|Person.all()->filter(p|$p.lastName == $name)};
                   assert('name' == $lambda.openVariables, |'');
                }\
                """);
    }

    @Test
    public void testFunctionReturnType()
    {
        compileInferenceTest(
                """
                function countValues<T>(collection:T[*]):Pair<T,Integer>[*]
                {
                   $collection->map(v|^Pair<T, Integer>(first=$v, second=2))
                }\
                """);
        Assertions.assertEquals("Pair<T, Integer>", GenericType.print(processorSupport.package_getByUserPath("countValues_T_MANY__Pair_MANY_").getValueForMetaPropertyToOne(M3Properties.expressionSequence).getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));
    }

    @Test
    public void testConflictingParameterTypes()
    {
        compileInferenceTest(
                        """
                        function funcT<T>(c:T[*], f:Function<{T[1]->Boolean[1]}>[1]):Pair<List<T>,List<T>>[1]
                        {
                           $c->fold({i,a|if($f->eval($i),
                                            |let l = $a.first; ^$a(first=^$l(values+=$i));,
                                            |let l = $a.second;^$a(second=^$l(values+=$i));
                                         )
                                    },
                                    pair(^List<T>(), ^List<T>())
                           );  \s
                        }
                        """);
        CoreInstance ci = runtime.getCoreInstance("funcT_T_MANY__Function_1__Pair_1_");
        CoreInstance expressionSeq = Instance.getValueForMetaPropertyToOneResolved(ci, M3Properties.expressionSequence, processorSupport);
        CoreInstance expSeqGT = Instance.getValueForMetaPropertyToOneResolved(expressionSeq, M3Properties.genericType, processorSupport);
        CoreInstance pairRT = Instance.getValueForMetaPropertyToOneResolved(expSeqGT, M3Properties.rawType, processorSupport);
        Assertions.assertEquals(M3Paths.Pair, PackageableElement.getUserPathForPackageableElement(pairRT));
        ListIterable<? extends CoreInstance> pairTAs = Instance.getValueForMetaPropertyToManyResolved(expSeqGT, M3Properties.typeArguments, processorSupport);
        CoreInstance listRT = Instance.getValueForMetaPropertyToOneResolved(pairTAs.get(0), M3Properties.rawType, processorSupport);
        Assertions.assertNotNull(listRT);
        CoreInstance listTA = Instance.getValueForMetaPropertyToOneResolved(pairTAs.get(0), M3Properties.typeArguments, processorSupport);
        CoreInstance listTypeParameter = Instance.getValueForMetaPropertyToOneResolved(listTA, M3Properties.typeParameter, processorSupport);
        Assertions.assertNotNull(listTypeParameter);
        Assertions.assertEquals("T", listTypeParameter.getValueForMetaPropertyToOne(M3Properties.name).getName());
    }

    @Test
    public void testLambdasInferForDecide()
    {
        compileInferenceTest(
                """
                Class DecisionRule<T,U|m,n>
                {
                   condition: Function<{T[m]->Boolean[1]}>[1];
                   action: Function<{T[m]->U[n]}>[1];
                }
                
                function rule<T, U|m,n>(condition:Function<{T[m]->Boolean[1]}>[1], action:Function<{T[m]->U[n]}>[1]):DecisionRule<T,U|m,n>[1]
                {
                     ^DecisionRule<T, U|m,n>(condition=$condition, action=$action);
                }
                
                function decide<T, U|m,n>(input:T[m], rules:DecisionRule<T,U|m,n>[*]):U[n]
                {
                     let matched = $rules->filter(r| $r.condition->eval($input));
                     assert($matched->size() > 0, 'Expected at least one match');
                     $matched->first()->toOne().action->eval($input);
                }
                
                function useDecide(s:String[1]):Integer[1]
                {
                   $s->decide([
                      rule(x|$x == 'Dave', x| 1),
                      rule(x|$x == 'Mark', x| 2)
                   ]);
                }
                """);
    }

    @Test
    public void ensureReturnTypeIsCorrectlyInferredForComplexNew()
    {
        compileInferenceTest(
                        """
                        Class Container { values : Any[*]; }
                        
                        function test<T>(value:T[1], func:Function<{T[1]->Any[*]}>[1]):List<Container>[*]
                        {
                           ^List<Container>(values = $func->eval($value)->map(v|^Container(values=$v)));
                        }
                        """);
    }

    @Test
    public void ensureNoCrashWhenSameMultiplicityTemplateNameAsParent()
    {
        // Stop recurrence of bug where type inference using the same multiplicity letter (n is multiplicity for
        // test3 and eval) causes resolution to the wrong type.
        compileInferenceTest(
                """
                function test3<T,U|m,n>(a:T[m], f:Function<{T[m]->U[n]}>[1]):U[n]
                {
                   $f->eval($a);
                }\
                """);
    }

    @Test
    public void testMap()
    {
        compileInferenceTest(
                """
                Class Person
                {
                 age:Integer[1];
                }
                
                function test<T>(f:T[1], func:Function<{T[1]->Person[*]}>[1]):Person[*]
                {
                   $f->map($func);
                }
                """);
    }

    @Test
    public void testIfWithFuncTypes()
    {
        compileInferenceTest(
                """
                function testFn<|m>(col:Integer[m], func:Function<{Integer[1]->Number[1]}>[0..1]):Number[m]
                {
                  let toStringFunc = if($func->isEmpty(), |{x:Number[1] | 5}, |$func->toOne());
                  $col->map(x | $toStringFunc->eval($x));
                }
                """);
        ConcreteFunctionDefinition<?> function = (ConcreteFunctionDefinition<?>) runtime.getFunction("testFn_Integer_m__Function_$0_1$__Number_m_");
        Assertions.assertNotNull(function);

        FunctionType functionType = (FunctionType) function._classifierGenericType()._typeArguments().getOnly()._rawType();
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{Integer[1]->Number[1]}>", functionType._parameters().getLast()._genericType());

        ListIterable<? extends ValueSpecification> expressionSequence = function._expressionSequence().toList();
        SimpleFunctionExpression letExpr = (SimpleFunctionExpression) expressionSequence.get(0);
        SimpleFunctionExpression ifExpr = (SimpleFunctionExpression) letExpr._parametersValues().toList().getLast();
        ListIterable<? extends ValueSpecification> ifParams = ifExpr._parametersValues().toList();
        LambdaFunction<?> ifTrue = (LambdaFunction<?>) ((InstanceValue) ifParams.get(1))._values().getOnly();
        LambdaFunction<?> ifFalse = (LambdaFunction<?>) ((InstanceValue) ifParams.get(2))._values().getOnly();
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{->meta::pure::metamodel::function::LambdaFunction<{Number[1]->Integer[1]}>[1]}>", ifTrue._classifierGenericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{->meta::pure::metamodel::function::Function<{Integer[1]->Number[1]}>[1]}>", ifFalse._classifierGenericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{Integer[1]->Number[1]}>", ifExpr._genericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{Integer[1]->Number[1]}>", letExpr._genericType());

        SimpleFunctionExpression mapExpr = (SimpleFunctionExpression) expressionSequence.get(1);
        LambdaFunction<?> mapFn = (LambdaFunction<?>) ((InstanceValue) mapExpr._parametersValues().toList().getLast())._values().getOnly();
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{Integer[1]->Number[1]}>", mapFn._classifierGenericType());
    }

    @Test
    public void testIfWithFuncTypesAndTypeParams()
    {
        compileInferenceTest(
                """
                function testFn<T|m>(col:T[m], func:Function<{T[1]->String[1]}>[0..1]):String[m]
                {
                  let toStringFunc = if($func->isEmpty(), |{x:T[1] | $x->toString()}, |$func->toOne());
                  $col->map(x | $toStringFunc->eval($x));
                }
                """);
        ConcreteFunctionDefinition<?> function = (ConcreteFunctionDefinition<?>) runtime.getFunction("testFn_T_m__Function_$0_1$__String_m_");
        Assertions.assertNotNull(function);

        FunctionType functionType = (FunctionType) function._classifierGenericType()._typeArguments().getOnly()._rawType();
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{T[1]->String[1]}>", functionType._parameters().getLast()._genericType());

        ListIterable<? extends ValueSpecification> expressionSequence = function._expressionSequence().toList();
        SimpleFunctionExpression letExpr = (SimpleFunctionExpression) expressionSequence.get(0);
        SimpleFunctionExpression ifExpr = (SimpleFunctionExpression) letExpr._parametersValues().toList().getLast();
        ListIterable<? extends ValueSpecification> ifParams = ifExpr._parametersValues().toList();
        LambdaFunction<?> ifTrue = (LambdaFunction<?>) ((InstanceValue) ifParams.get(1))._values().getOnly();
        LambdaFunction<?> ifFalse = (LambdaFunction<?>) ((InstanceValue) ifParams.get(2))._values().getOnly();
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{->meta::pure::metamodel::function::LambdaFunction<{T[1]->String[1]}>[1]}>", ifTrue._classifierGenericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{->meta::pure::metamodel::function::Function<{T[1]->String[1]}>[1]}>", ifFalse._classifierGenericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{T[1]->String[1]}>", ifExpr._genericType());
        assertGenericTypeEquals("meta::pure::metamodel::function::Function<{T[1]->String[1]}>", letExpr._genericType());

        SimpleFunctionExpression mapExpr = (SimpleFunctionExpression) expressionSequence.get(1);
        LambdaFunction<?> mapFn = (LambdaFunction<?>) ((InstanceValue) mapExpr._parametersValues().toList().getLast())._values().getOnly();
        assertGenericTypeEquals("meta::pure::metamodel::function::LambdaFunction<{T[1]->String[1]}>", mapFn._classifierGenericType());
    }

    private void assertGenericTypeEquals(String expected, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType)
    {
        String actual = GenericType.print(genericType, true, processorSupport);
        Assertions.assertEquals(expected, actual, Optional.ofNullable(genericType.getSourceInformation()).map(SourceInformation::getMessage).orElse(null));
    }

    @Test
    public void testChainedFilters()
    {
        compileInferenceTest(
                """
                function test():Profile[*]
                {
                  meta::pure::metamodel::extension::Profile.all()
                   ->filter(p | $p.p_stereotypes->size() > 1)
                   ->filter(p | $p.p_stereotypes->size() > 2)
                   ->filter(p | $p.p_stereotypes->size() > 3)
                   ->filter(p | $p.p_stereotypes->size() > 4)
                   ->filter(p | $p.p_stereotypes->size() > 5)
                   ->filter(p | $p.p_stereotypes->size() > 6)
                   ->filter(p | $p.p_stereotypes->size() > 7)
                   ->filter(p | $p.p_stereotypes->size() > 8)
                   ->filter(p | $p.p_stereotypes->size() > 9)
                   ->filter(p | $p.p_stereotypes->size() > 10)
                   ->filter(p | $p.p_stereotypes->size() > 11)
                   ->filter(p | $p.p_stereotypes->size() > 12)
                   ->filter(p | $p.p_stereotypes->size() > 13)
                   ->filter(p | $p.p_stereotypes->size() > 14)
                   ->filter(p | $p.p_stereotypes->size() > 15)
                   ->filter(p | $p.p_stereotypes->size() > 16)
                   ->filter(p | $p.p_stereotypes->size() > 17)
                   ->filter(p | $p.p_stereotypes->size() > 18)
                   ->filter(p | $p.p_stereotypes->size() > 19)
                   ->filter(p | $p.p_stereotypes->size() > 20)
                   ->filter(p | $p.p_stereotypes->size() > 21)
                   ->filter(p | $p.p_stereotypes->size() > 22)
                   ->filter(p | $p.p_stereotypes->size() > 23)
                   ->filter(p | $p.p_stereotypes->size() > 24)
                   ->filter(p | $p.p_stereotypes->size() > 25)
                   ->filter(p | $p.p_stereotypes->size() > 26)
                   ->filter(p | $p.p_stereotypes->size() > 27)
                   ->filter(p | $p.p_stereotypes->size() > 28)
                   ->filter(p | $p.p_stereotypes->size() > 29)
                   ->filter(p | $p.p_stereotypes->size() > 30)
                   ->filter(p | $p.p_stereotypes->size() > 31)
                   ->filter(p | $p.p_stereotypes->size() > 32)
                   ->filter(p | $p.p_stereotypes->size() > 33)
                   ->filter(p | $p.p_stereotypes->size() > 34)
                   ->filter(p | $p.p_stereotypes->size() > 35)
                   ->filter(p | $p.p_stereotypes->size() > 36)
                   ->filter(p | $p.p_stereotypes->size() > 37)
                   ->filter(p | $p.p_stereotypes->size() > 38)
                   ->filter(p | $p.p_stereotypes->size() > 39)
                   ->filter(p | $p.p_stereotypes->size() > 40)
                   ->filter(p | $p.p_stereotypes->size() > 41)
                   ->filter(p | $p.p_stereotypes->size() > 42)
                   ->filter(p | $p.p_stereotypes->size() > 43)
                   ->filter(p | $p.p_stereotypes->size() > 44)
                   ->filter(p | $p.p_stereotypes->size() > 45)
                   ->filter(p | $p.p_stereotypes->size() > 46)
                   ->filter(p | $p.p_stereotypes->size() > 47)
                   ->filter(p | $p.p_stereotypes->size() > 48)
                   ->filter(p | $p.p_stereotypes->size() > 49)
                   ->filter(p | $p.p_stereotypes->size() > 50)
                   ->filter(p | $p.p_stereotypes->size() > 51)
                   ->filter(p | $p.p_stereotypes->size() > 52)
                   ->filter(p | $p.p_stereotypes->size() > 53)
                   ->filter(p | $p.p_stereotypes->size() > 54)
                   ->filter(p | $p.p_stereotypes->size() > 55)
                   ->filter(p | $p.p_stereotypes->size() > 56)
                   ->filter(p | $p.p_stereotypes->size() > 57)
                   ->filter(p | $p.p_stereotypes->size() > 58)
                   ->filter(p | $p.p_stereotypes->size() > 59)
                   ->filter(p | $p.p_stereotypes->size() > 60)
                   ->filter(p | $p.p_stereotypes->size() > 61)
                   ->filter(p | $p.p_stereotypes->size() > 62)
                   ->filter(p | $p.p_stereotypes->size() > 63)
                   ->filter(p | $p.p_stereotypes->size() > 64)
                   ->filter(p | $p.p_stereotypes->size() > 65)
                   ->filter(p | $p.p_stereotypes->size() > 66)
                   ->filter(p | $p.p_stereotypes->size() > 67)
                   ->filter(p | $p.p_stereotypes->size() > 68)
                   ->filter(p | $p.p_stereotypes->size() > 69)
                   ->filter(p | $p.p_stereotypes->size() > 70)
                   ->filter(p | $p.p_stereotypes->size() > 71)
                   ->filter(p | $p.p_stereotypes->size() > 72)
                   ->filter(p | $p.p_stereotypes->size() > 73)
                   ->filter(p | $p.p_stereotypes->size() > 74)
                   ->filter(p | $p.p_stereotypes->size() > 75)
                   ->filter(p | $p.p_stereotypes->size() > 76)
                   ->filter(p | $p.p_stereotypes->size() > 77)
                   ->filter(p | $p.p_stereotypes->size() > 78)
                   ->filter(p | $p.p_stereotypes->size() > 79)
                   ->filter(p | $p.p_stereotypes->size() > 80)
                   ->filter(p | $p.p_stereotypes->size() > 81)
                   ->filter(p | $p.p_stereotypes->size() > 82)
                   ->filter(p | $p.p_stereotypes->size() > 83)
                   ->filter(p | $p.p_stereotypes->size() > 84)
                   ->filter(p | $p.p_stereotypes->size() > 85)
                   ->filter(p | $p.p_stereotypes->size() > 86)
                   ->filter(p | $p.p_stereotypes->size() > 87)
                   ->filter(p | $p.p_stereotypes->size() > 88)
                   ->filter(p | $p.p_stereotypes->size() > 89)
                   ->filter(p | $p.p_stereotypes->size() > 90)
                   ->filter(p | $p.p_stereotypes->size() > 91)
                   ->filter(p | $p.p_stereotypes->size() > 92)
                   ->filter(p | $p.p_stereotypes->size() > 93)
                   ->filter(p | $p.p_stereotypes->size() > 94)
                   ->filter(p | $p.p_stereotypes->size() > 95)
                   ->filter(p | $p.p_stereotypes->size() > 96)
                   ->filter(p | $p.p_stereotypes->size() > 97)
                   ->filter(p | $p.p_stereotypes->size() > 98)
                   ->filter(p | $p.p_stereotypes->size() > 99)
                   ->filter(p | $p.p_stereotypes->size() > 100)
                }\
                """);
    }

    @Test
    public void testMixedChain()
    {
        compileInferenceTest(
                """
                function test():Profile[*]
                {
                  meta::pure::metamodel::extension::Profile.all()
                   ->filter(p | $p.p_stereotypes->size() > 1)
                   ->map(p | $p.p_stereotypes)
                   ->filter(s | !$s.value->isEmpty())
                   ->map(s | $s.profile)
                   ->filter(p | $p.p_stereotypes->size() > 2)
                   ->map(p | $p.p_stereotypes)
                   ->filter(s | !$s.value->isEmpty())
                   ->map(s | $s.profile)
                   ->filter(p | $p.p_stereotypes->size() > 3)
                   ->map(p | $p.p_stereotypes)
                   ->filter(s | !$s.value->isEmpty())
                   ->map(s | $s.profile)
                   ->filter(p | $p.p_stereotypes->size() > 4)
                   ->map(p | $p.p_stereotypes)
                   ->filter(s | !$s.value->isEmpty())
                   ->map(s | $s.profile)
                   ->filter(p | $p.p_stereotypes->size() > 5)
                   ->map(p | $p.p_stereotypes)
                   ->filter(s | !$s.value->isEmpty())
                   ->map(s | $s.profile)
                }\
                """);
    }

    @Test
    public void testChainWithParameterizedReturn()
    {
        compileInferenceTest(
                """
                function test<X, Y, Z>(classes:Class<X>[m], funcXY:Function<{Class<X>[1]->Y[1]}>[1], predY:Function<{Y[1]->Boolean[1]}>[1], funcYZ:Function<{Y[1]->Z[0..1]}>[1], predZ:Function<{Z[1]->Boolean[1]}>[1]):Z[*]
                {
                  $classes
                   ->filter(c | $c.properties->size() > 1)
                   ->filter(c | $c.properties->size() > 2)
                   ->filter(c | $c.properties->size() > 3)
                   ->filter(c | $c.properties->size() > 4)
                   ->filter(c | $c.properties->size() > 5)
                   ->filter(c | $c.properties->size() > 6)
                   ->filter(c | $c.properties->size() > 7)
                   ->filter(c | $c.properties->size() > 8)
                   ->filter(c | $c.properties->size() > 9)
                   ->filter(c | $c.properties->size() > 10)
                   ->map(c | $funcXY->eval($c))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->filter(y | $predY->eval($y))
                   ->map(y | $funcYZ->eval($y))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                   ->filter(z | $predZ->eval($z))
                }\
                """);
    }

    private void compileInferenceTest(String source)
    {
        compileTestSource(inferenceTestFileName, source);
    }

    private void deleteInferenceTest()
    {
        runtime.delete(inferenceTestFileName);
        runtime.compile();
    }
}
