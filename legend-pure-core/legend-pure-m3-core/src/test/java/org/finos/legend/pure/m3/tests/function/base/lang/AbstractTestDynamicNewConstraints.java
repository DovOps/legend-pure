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

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestDynamicNewConstraints extends AbstractPureTestWithCoreCompiled
{
    private static final String EMPLOYEE_SOURCE_NAME = "employee.pure";
    private static final String EMPLOYEE_SOURCE_CODE = """
            Class Employee
            {
               lastName:String[1];
            }
            """;

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("/test/repro.pure");
        runtime.modify(EMPLOYEE_SOURCE_NAME, EMPLOYEE_SOURCE_CODE);
        runtime.compile();
    }

    @Test
    public void testExtendedConstraintExecutionCanEvaluateConstraintMessage()
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
                function meta::pure::functions::collection::isNotEmpty(p:Any[*]):Boolean[1]
                {
                    !isEmpty($p)
                }\
                function testNew():Any[*]
                {
                   assert(Position.constraints->at(0).messageFunction->toOne()->evaluate(^List<Any>(values=Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2011-01-01)
                    ])))->toOne()->cast(@String) == 'Contract ID: 1', |'');
                }\
                """
        );
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithOwnerGlobal()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Global
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
                function testNew():Any[*]
                {
                   Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2011-01-01)
                    ])
                }\
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 14, e);
    }

    @Test
    public void testDeeplyNestedThis()
    {
        compileTestSource("/test/repro.pure",
                """
                import my::supportDemo::*;
                Class my::supportDemo::Person
                {
                   name: String[1];
                }
                
                Class my::supportDemo::SuperPerson
                {
                   person: my::supportDemo::Person[1];
                   personWithTitle(title:String[1])
                   {
                     ^Person(name = $title + ' ' + $this.person.name)
                   }:Person[1];
                }
                Class my::supportDemo::SuperPeople
                [ \s
                   superPeopleHaveNoDuplicates
                   (
                      ~function: if($this.superPeople->isEmpty(),
                                    | true,
                                    | $this.superPeople->size() == $this.superPeople->removeDuplicates({left,right| $left.personWithTitle($this.title).name == $right.personWithTitle($this.title).name})->size())
                      ~enforcementLevel: Error
                      ~message: 'test'
                   )
                ]
                {
                   superPeople: my::supportDemo::SuperPerson[*];
                   title:String[1];
                }
                
                function testSucceed():Any[*]
                {
                   assert(SuperPeople.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=SuperPeople->dynamicNew([
                       ^KeyValue(key='superPeople', value=[^SuperPerson(person=^Person(name='John')), ^SuperPerson(person=^Person(name='Robert'))]),\s
                       ^KeyValue(key='title', value='Dr.')
                    ])))->toOne()->cast(@Boolean), |'')
                }
                
                function testFail():Any[*]
                {
                   ^SuperPeople(superPeople=[^SuperPerson(person=^Person(name='John')), ^SuperPerson(person=^Person(name='John'))], title='Dr.')
                }
                """);
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[superPeopleHaveNoDuplicates] violated in the Class SuperPeople, Message: test", "/test/repro.pure", 45, 4, 45, 4, 45, 128, e);
    }


    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsOtherConstraint()
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
                   c3 : $this.endDate > $this.startDate
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }
                
                function meta::pure::functions::collection::isNotEmpty(p:Any[*]):Boolean[1]
                {
                    !isEmpty($p)
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                function testNew():Any[*]
                {
                   Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2010-01-01)
                    ])
                }\
                """
        );

        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c3] violated in the Class Position", 27, 14, e);
    }

    @Test
    public void testConstraintWithDynamicNew()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   rule1 : $this.lastName->toOne()->length() < 10\
                ]\
                {\
                   lastName:String[0..1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function testNew():Any[*] {
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
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 11, 11, e);
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithNoOwner()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
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
                
                function testNew():Any[*]
                {
                   Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2011-01-01)
                    ])
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 14, e);
    }

    @Test
    public void testConstraintWithGenericTypeDynamicNew()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   rule1 : $this.lastName->toOne()->length() < 10\
                ]\
                {\
                   lastName:String[0..1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]
                {
                  [];
                }
                
                function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]
                {
                  [];
                }
                function testNew():Any[*] {
                  let r = dynamicNew(^GenericType(rawType=Employee),
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ],
                                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,
                                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,
                                   '2'
                                  )->cast(@Employee);
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 11, 11, e);
    }

    @Test
    public void testConstraintWithDynamicNewNoOverrides()
    {
        compileTestSource("fromString.pure", """
                Class EmployeeWithError\
                [\
                   rule1 : $this.lastName->toOne()->length() < 10\
                ]\
                {\
                   lastName:String[0..1];\
                }
                function testNew():Any[*] {
                  let r = dynamicNew(EmployeeWithError,
                                   [
                                      ^KeyValue(key='lastName',value='1234567891000')
                                   ])->cast(@EmployeeWithError);
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class EmployeeWithError", 3, 11, e);
    }

    @Test
    public void testExtendedConstraintExecutionCanEvaluateConstraint()
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
                function testNew():Any[*]
                {
                   assert(!Position.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2011-01-01)
                    ])))->toOne()->cast(@Boolean), |'');
                }\
                """
        );
        execute("testNew():Any[*]");
    }


    @Test
    public void testExtendedConstraintExecutionDynamicNewPassesWithOwner()
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
                
                function testNew():Any[*]
                {
                   Position->dynamicNew([
                       ^KeyValue(key='contractId', value='1'),\s
                       ^KeyValue(key='positionType', value='2'),
                       ^KeyValue(key='startDate', value=%2010-01-01),
                       ^KeyValue(key='endDate', value=%2011-01-01)
                    ])
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        execute("testNew():Any[*]");
    }


}
