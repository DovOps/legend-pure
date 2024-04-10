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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestDynamicNewConstraintsHandler extends AbstractPureTestWithCoreCompiled
{

    @Test
    public void testClassDefaultConstraintHandler()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function testNew():Any[*] {
                  let func = [];
                  let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2'
                                  )->cast(@Employee);
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> compileAndExecute("testNew():Any[*]"));
        assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 19, 11, e);
    }

    @Test
    public void testClassInvalidConstraintHandler()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[*]
                {
                  [$o,$o];
                }
                function testNew():Any[*] {
                  let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_MANY_
                                  )->cast(@Employee);
                }
                """));

        assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "dynamicNew(_:Class<Employee>[1],_:KeyValue[1],_:ConcreteFunctionDefinition<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[1],_:ConcreteFunctionDefinition<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[1],_:String[1],_:ConcreteFunctionDefinition<{Any[1]->Any[*]}>[1])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::lang::dynamicNew(Class<Any>[1], KeyValue[*]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(Class<Any>[1], KeyValue[*], Function<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[0..1], Function<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[0..1], Any[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(Class<Any>[1], KeyValue[*], Function<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[0..1], Function<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[0..1], Any[0..1], Function<{Any[1]->Any[1]}>[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*], Function<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[0..1], Function<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[0..1], Any[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*], Function<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[0..1], Function<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[0..1], Any[0..1], Function<{Any[1]->Any[1]}>[0..1]):Any[1]\n", 22, 11, e);
    }

    @Test
    public void testClassConstraintHandler()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  ^Employee(lastName='new');
                }
                function testNew():Any[*] {
                  let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_1_
                                  )->cast(@Employee);
                 assert('new' == $r.lastName, |'');
                }
                """);

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerSignature()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  $o;
                }
                function testNew():Any[*] {
                  let r = ^ConstraintsOverride(constraintsManager=
                                   constraintsManager_Any_1__Any_1_
                                  );
                 assert(constraintsManager_Any_1__Any_1_ == $r.constraintsManager, |'');
                }
                """);

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerNoException()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                Class ConstraintResult\
                {\
                   instance:Any[1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  assert(!$o->genericType().rawType->isEmpty(), |'no raw type');
                  assert($o->genericType().rawType->toOne()->instanceOf(ElementWithConstraints), |'input is not a sub type of ElementWithConstraints');
                  let constraints = $o->genericType().rawType->cast(@ElementWithConstraints).constraints;
                  assert($constraints->size()>0, |'constraints should not be empty');
                  ^ConstraintResult(instance=$o);
                }
                function testNew():Any[*] {
                 let r1 = constraintsManager(^Employee(lastName='name'));\s
                 assert($r1->instanceOf(ConstraintResult), |'');
                 assert($r1->cast(@ConstraintResult).instance->instanceOf(Employee), |'');
                 assert('name' == $r1->at(0)->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');
                 let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_1_
                                  );
                 assert($r->instanceOf(ConstraintResult), |'');
                 assert($r->cast(@ConstraintResult).instance->instanceOf(Employee), |'');
                 assert('1234567891000' == $r->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');
                }
                """);

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testGenericTypeConstraintHandlerNoException()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10,
                   rule2: $this.lastName->toOne()->length() > 3
                ]
                {
                   lastName:String[0..1];
                }
                Class ConstraintResult
                {\
                   instance:Any[1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  assert(!$o->genericType().rawType->isEmpty(), |'no raw type');
                  assert($o->genericType().rawType->toOne()->instanceOf(ElementWithConstraints), |'input is not a sub type of ElementWithConstraints');
                  let constraints = $o->genericType().rawType->cast(@ElementWithConstraints).constraints;
                  assert($constraints->size()>0, |'constraints should not be empty');
                  ^ConstraintResult(instance=$o);
                }
                function testNew():Any[*] {
                 let r1 = constraintsManager(^Employee(lastName='name'));\s
                 assert($r1->instanceOf(ConstraintResult), |'');
                 assert($r1->cast(@ConstraintResult).instance->instanceOf(Employee), |'');
                 assert('name' == $r1->at(0)->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');
                 let r = dynamicNew(^GenericType(rawType=Employee),
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_1_
                                  );
                 assert($r->instanceOf(ConstraintResult), |'');
                 assert($r->cast(@ConstraintResult).instance->instanceOf(Employee), |'');
                 assert('1234567891000' == $r->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');
                }
                """);

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testGenericTypeConstraintHandlerCopyAfterDynamicNew()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                Class ConstraintResult
                {
                   instance:Any[1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  $o;
                }
                function testNew():Any[*] {
                 let r1 = constraintsManager(^Employee(lastName='name'));\s
                 assert($r1->instanceOf(Employee), |'');
                 let r = dynamicNew(^GenericType(rawType=Employee),
                                   [
                                      ^KeyValue(key='lastName',value='12345687458973425839855')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_1_
                                  );
                 assert($r->instanceOf(Employee), |'');
                 assert('12345687458973425839855' == $r->cast(@Employee).lastName, |'');
                 let emp = $r->cast(@Employee);
                 assert('123456789101010101' == ^$emp(lastName='123456789101010101').lastName, |'');
                }
                """);


        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerCopyAfterDynamicNew()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                Class ConstraintResult
                {
                   instance:Any[1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintsManager(o:Any[1]):Any[1]
                {
                  $o;
                }
                function testNew():Any[*] {
                 let r1 = constraintsManager(^Employee(lastName='name'));\s
                 assert($r1->instanceOf(Employee), |'');
                 let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='12345687458973425839855')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',
                                   constraintsManager_Any_1__Any_1_
                                  )->cast(@Employee);
                 assert('12345687458973425839855' == $r.lastName, |'');
                 assert('123456789101010101' == ^$r(lastName='123456789101010101').lastName, |'');
                }
                """);
        this.compileAndExecute("testNew():Any[*]");
    }


    @Test
    public void testEvaluateConstraint()
    {
        compileTestSource("fromString.pure", """
                Class Employee
                [
                   rule1 : $this.lastName->toOne()->length() < 10
                ]
                {
                   lastName:String[0..1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function constraintManager(o:Any[1]):Any[1]
                {
                  let failed = evaluate(Employee.constraints->at(0).functionDefinition,^List<Any>(values=$o))->toOne()->cast(@Boolean);
                  print($failed,1);\
                  ^Pair<Boolean,Any>(first=$failed,second=$o);\
                }
                function testNew():Any[*] {
                  let r = dynamicNew(Employee,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2',\
                                   constraintManager_Any_1__Any_1_
                                  )->cast(@Pair<Boolean,Any>);
                  assert(!$r.first, |'');
                }
                """);

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testConstraintManagerWithExtendedConstraintGrammar()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                      ~message          : 'Contract ID: ' + $this.contractId
                   ),
                   c3
                   (
                      ~owner            : Finance
                      ~function         : $this.endDate > $this.startDate
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                
                function constraintManager(o: Any[1]):Any[1]
                {
                   let constraints = $o->genericType()->genericTypeClass().constraints;
                   ^List<Boolean>(values=$constraints->map(c | evaluate($c.functionDefinition, ^List<Any>(values=$o))->cast(@Boolean)->toOne()));\s
                }
                
                function testNew():Any[*]
                {
                   let res = Position->dynamicNew([
                                            ^KeyValue(key='contractId', value='1'),\s
                                            ^KeyValue(key='positionType', value='2'),
                                            ^KeyValue(key='startDate', value=%2010-01-01),
                                            ^KeyValue(key='endDate', value=%2011-01-01)
                                         ],
                                         getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                         getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                         '2',
                                         constraintManager_Any_1__Any_1_
                                        );
                   assert($res->cast(@List<Boolean>).values == [false, true], |'');
                }\
                """);

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testConstraintManager2WithExtendedConstraintGrammar()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~externalId       : 'My_Ext_Id_1'
                      ~function         : $this.contractId->startsWith('A')
                      ~message          : 'Contract ID: ' + $this.contractId
                   ),
                   c3
                   (
                      ~owner            : Finance
                      ~externalId       : 'My_Ext_Id_2'
                      ~function         : $this.endDate > $this.startDate
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                
                function constraintManager(o: Any[1]):Any[1]
                {
                   let constraints = $o->genericType()->genericTypeClass().constraints;
                   ^List<Any>(values=$constraints->map(c | [evaluate($c.functionDefinition, ^List<Any>(values=$o))->cast(@Boolean)->toOne()]->concatenate($c.externalId->toOne())->concatenate(if($c.messageFunction->isEmpty(),|[],|evaluate($c.messageFunction->toOne(), ^List<Any>(values=$o))->cast(@String)->toOne()))));\s
                }
                
                function testNew():Any[*]
                {
                   let res = Position->dynamicNew([
                                            ^KeyValue(key='contractId', value='1'),\s
                                            ^KeyValue(key='positionType', value='2'),
                                            ^KeyValue(key='startDate', value=%2010-01-01),
                                            ^KeyValue(key='endDate', value=%2011-01-01)
                                         ],
                                         getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                         getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                         '2',
                                         constraintManager_Any_1__Any_1_
                                        );
                   assert($res->cast(@List<Any>).values == [false, 'My_Ext_Id_1', 'Contract ID: 1', true, 'My_Ext_Id_2'], |'');
                }\
                """);

        this.execute("testNew():Any[*]");
    }

}
