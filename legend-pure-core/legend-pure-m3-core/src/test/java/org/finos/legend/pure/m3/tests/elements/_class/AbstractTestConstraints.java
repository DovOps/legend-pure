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

package org.finos.legend.pure.m3.tests.elements._class;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestConstraints extends AbstractPureTestWithCoreCompiled
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
    public void testFunction()
    {
        compileTestSource("fromString.pure", """
                function myFunction(s:String[1], k:Integer[1]):String[1]\
                [\
                   $s->startsWith('A'),\
                   $return->startsWith('A'),\
                   $k > 2\
                ]\
                {\
                   $s+$k->toString();\
                }
                function testNew():Any[*]
                {
                   myFunction('A test', 4)\
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void testFunctionError()
    {
        compileTestSource("fromString.pure", """
                function myFunction(s:String[1], k:Integer[1]):String[1]\
                [\
                   $s->startsWith('A'),\
                   $return->startsWith('A'),\
                   $k > 2\
                ]\
                {\
                   $s+$k->toString();\
                }
                function testNew():Any[*]
                {
                   myFunction('A test', 2)\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[2] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }

    @Test
    public void testFunctionErrorOnReturnPreConstrainId()
    {
        compileTestSource("fromString.pure", """
                function myFunction(s:String[1], k:Integer[1]):String[1]\
                [\
                   pre1:$s->startsWith('A'),\
                   pre2:$k > 2\
                ]\
                {\
                   'B';\
                }
                function testNew():Any[*]
                {
                   myFunction(' test', 3)\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[pre1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }

    @Test
    public void testFunctionErrorOnReturn()
    {
        compileTestSource("fromString.pure", """
                function myFunction(s:String[1], k:Integer[1]):String[1]\
                [\
                   $s->startsWith('A'),\
                   $return->startsWith('A'),\
                   $k > 2\
                ]\
                {\
                   'B';\
                }
                function testNew():Any[*]
                {
                   myFunction('A test', 3)\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (POST):[1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }


    @Test
    public void testNewError()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   $this.lastName->startsWith('A')\
                ]\
                {\
                   lastName:String[1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = 'CDE')\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void testNewErrorDuplicateConstraints()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class EmployeeWithError
                [
                  one: $this.lastName->startsWith('A'),
                  one: $this.lastName->length() == 2
                ]
                {
                   lastName:String[1];
                }
                function testNew():Any[*]
                {
                   let t = ^EmployeeWithError(lastName = 'CDE')\
                }
                """));
        assertPureException(PureCompilationException.class, "Constraints for EmployeeWithError must be unique, [one] is duplicated", 4, 3, e);
    }

    @Test
    public void testNewOk()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   rule1 : $this.lastName->startsWith('A')\
                ]\
                {\
                   lastName:String[1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = 'ABC')\
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void testNewWarn()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   warn($this.lastName->startsWith('A'), 'ok')\
                ]\
                {\
                   lastName:String[1];\
                }
                function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]
                {
                    if($b,|$b,|true)
                }\
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = 'CDE')\
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithMoreThanOneConstraint()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   warn($this.lastName->startsWith('A'), 'ok'),\
                   $this.lastName->substring($this.lastName->length()-1) == 'E'\
                ]\
                {\
                   lastName:String[1];\
                }
                function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]
                {
                    if($b,|$b,|true;)
                }\
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = 'CDE')\
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithFilterConstraint()
    {
        runtime.modify("employee.pure", """
                Class Employee
                {
                  id:Integer[1];
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                Class Firm
                [
                   $this.employees->filter(e | ($e.id < $this.minId) || ($e.id > $this.maxId))->isEmpty()
                ]
                {
                  minId:Integer[1];
                  maxId:Integer[1];
                  employees:Employee[*];
                }
                function testNewSuccess():Any[*]
                {
                   let f1 = ^Firm(minId=1, maxId=10);
                   let f2 = ^Firm(minId=1, maxId=10, employees=[^Employee(id=1), ^Employee(id=9)]);
                }
                function testNewFailure():Any[*]
                {
                   ^Firm(minId=1, maxId=10, employees=[^Employee(id=1), ^Employee(id=19)]);
                }\
                """);
        execute("testNewSuccess():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNewFailure():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 17, 4, e);
    }

    @Test
    public void testClassWithMapConstraint()
    {
        runtime.modify("employee.pure", """
                Class Employee
                {
                  id:Integer[1];
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                Class Firm
                [
                   $this.employees->removeDuplicates(e | $e.id, [])->size() == $this.employees->size()
                ]
                {
                  employees:Employee[*];
                }
                function testNewSuccess():Any[*]
                {
                   let f1 = ^Firm();
                   let f2 = ^Firm(employees=[^Employee(id=1), ^Employee(id=9)]);
                }
                function testNewFailure():Any[*]
                {
                   ^Firm(employees=[^Employee(id=1), ^Employee(id=2), ^Employee(id=1)]);
                }\
                """);
        execute("testNewSuccess():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNewFailure():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 15, 4, e);
    }

    @Test
    public void testCopyError()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   $this.lastName->startsWith('A')\
                ]\
                {\
                   lastName:String[1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = 'ABC');
                   ^$t(lastName = 'KK');\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 4, 4, e);
    }


    @Test
    public void testInheritanceFailingSubClass()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   $this.lastName->startsWith('A')\
                ]\
                {\
                   lastName:String[1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                Class Manager extends Employee
                {
                  manages:Employee[*];
                }
                function testNew():Any[*]
                {
                   let t = ^Manager(lastName = 'BC')\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 7, 12, e);
    }

    @Test
    public void testInheritanceFailingSubClassConstraint()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                   $this.lastName->startsWith('A')\
                ]\
                {\
                   lastName:String[1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                Class Manager extends Employee
                [\
                   $this.manages->size() > 1\
                ]\
                {
                  manages:Employee[*];
                }
                function testNew():Any[*]
                {
                   let t = ^Manager(lastName = 'BC')\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 7, 12, e);
    }

    @Test
    public void testBooleanWithMoreThanOneOperand()
    {
        runtime.modify("employee.pure", """
                Class Employee\
                [\
                    $this.lastName->toOne()->length() < 10\
                ]\
                {\
                   lastName:String[0..1];\
                }
                """);
        runtime.compile();
        compileTestSource("fromString.pure",
                """
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = '1234567891011121213454545')\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void tesIdInConstraint()
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
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = '123456789')\
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void tesIdInConstraintWrong()
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
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = '1234567893536536536')\
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void testIdInConstraintInWrong()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Employee\
                [\
                  : $this.lastName->toOne()->length() < 10\
                ]\
                {\
                   lastName:String[0..1];\
                }
                function testNew():Any[*]
                {
                   let t = ^Employee(lastName = '123456789')\
                }
                """));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: ':'", 1, 18, e);
    }







    @Test
    public void testExtendedConstraintGrammar()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~externalId       : 'My_Ext_Id'
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Error
                      ~message          : 'Contract ID: ' + $this.contractId
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """);
    }

    @Test
    public void testExtendedConstraintGrammarAllowedOnlyForClasses()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function myFunction(s:String[1], k:Integer[1]):String[1]
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $s->startsWith('A')
                   )
                ]\
                {\
                   $s+$k->toString();\
                }
                function testNew():Any[*]
                {
                   myFunction('A test', 4)\
                }
                """));
        assertPureException(PureParserException.class, "Complex constraint specifications are supported only for class definitions", 3, 4, e);
        runtime.modify("fromString.pure", """
                function myFunction(s:String[1], k:Integer[1]):String[1]
                {\
                   $s+$k->toString();\
                }
                """);
        runtime.compile();
    }

    @Test
    public void testExtendedConstraintGrammarOptionalOwner()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Warn
                      ~message          : 'Contract ID: ' + $this.contractId
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """);
    }

    @Test
    public void testExtendedConstraintGrammarOptionalLevelAndMessage()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~externalId       : 'My_Ext_Id'
                      ~function         : $this.contractId->startsWith('A')
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """);
    }

    @Test
    public void testExtendedConstraintGrammarFunctionRequired()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureParserException.class, "expected: one of {'~externalId', '~function'} found: ')'", 6, 4, e);
    }

    @Test
    public void testExtendedConstraintGrammarLevelIsWarnOrError()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Something
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureParserException.class, "expected: ENFORCEMENT_LEVEL found: 'Something'", 7, 27, e);
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsBoolean()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')->toString()
                      ~enforcementLevel : Warn
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsOneBoolean()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')->toOneMany()
                      ~enforcementLevel : Warn
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsString()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Warn
                      ~message          : 'Contract ID: ' == $this.contractId
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 43, e);
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsOneString()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Warn
                      ~message          : [('Contract ID: ' + $this.contractId), ('Contract ID: ' + $this.contractId)]
                   )
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                """));
        assertPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 27, e);
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraints()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                   ),
                   c2
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
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """);
    }


    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypes()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                   ),
                   c2 : $this.endDate > $this.startDate
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """);
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypesAlternating()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                   ),
                   c2 : $this.endDate > $this.startDate
                ,\
                   c3
                   (
                      ~owner            : Finance
                      ~function         : $this.endDate > $this.startDate
                   )\
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """);
    }

    @Test
    public void testExtendedConstraintGrammarNameConflict()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                   ),
                   c1
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
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """));
        assertPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
    }

    @Test
    public void testExtendedConstraintGrammarNameConflictInDifferentType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~function         : $this.contractId->startsWith('A')
                   ),
                   c1 : $this.endDate > $this.startDate
                ]
                {
                   contractId: String[1];
                   positionType: String[1];
                   startDate: Date[1];
                   endDate: Date[1];
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """));
        assertPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithOwner()
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
                   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """);
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithOwnerGlobal()
    {
        compileTestSource(
                "fromString.pure",
                """
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
                   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)
                }
                """);

        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithNoOwner()
    {
        compileTestSource(
                "fromString.pure",
                """
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
                   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithGlobalOwner()
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
                   ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)
                }\
                """
        );
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyPassesWithOwner()
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
                   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);
                   ^$a(contractId='1');
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithOwnerGlobal()
    {
        compileTestSource(
                "fromString.pure",
                """
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
                
                function testNew():Any[*]
                {
                   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);
                   ^$a(contractId='1');
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """);
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 25, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithNoOwner()
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
                   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);
                   ^$a(contractId='1');
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 24, 4, e);
    }











    @Test
    public void testExtendedConstraintExecutionCanGetOwnerExtIdEnforcement()
    {
        compileTestSource("fromString.pure", """
                Class Position
                [
                   c1
                   (
                      ~owner            : Finance
                      ~externalId       : 'My_Ext_Id'
                      ~function         : $this.contractId->startsWith('A')
                      ~enforcementLevel : Warn
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
                   assert(Position.constraints->at(0).owner == 'Finance', |'');
                   assert(Position.constraints->at(0).externalId == 'My_Ext_Id', |'');
                   assert(Position.constraints->at(0).enforcementLevel == 'Warn', |'');
                }\
                function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]
                {
                   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);
                }
                """
        );
        execute("testNew():Any[*]");
    }


    @Test
    public void testConstraintInClassWithTypeParameters()
    {
        compileTestSource("/test/repro.pure",
                """
                Class ClassWrapper<T|m>
                [
                   notAny: $this.classes->filter(c | $c == Any)->size() == 0
                ]
                {
                   classes: Class<T>[m];
                }
                
                function testSucceed():Any[*]
                {
                    ^ClassWrapper<Type|1>(classes=Type);
                }
                
                function testFail():Any[*]
                {
                    ^ClassWrapper<Any|2>(classes=[Type, Any]);
                }
                """
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[notAny] violated in the Class ClassWrapper", "/test/repro.pure", 16, 5, 16, 5, 16, 45, e);
    }

    @Test
    public void testConstraintInClassWithTypeParameters2()
    {
        compileTestSource("/test/repro.pure",
                """
                Class Wrapper<T>
                [
                   notEmpty: $this.values->size() > 0
                ]
                {
                   values: T[*];
                }
                
                function testSucceed():Any[*]
                {
                    ^Wrapper<Integer>(values=1);
                }
                
                function testFail():Any[*]
                {
                    ^Wrapper<Any>(values=[]);
                }
                """
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[notEmpty] violated in the Class Wrapper", "/test/repro.pure", 16, 5, 16, 5, 16, 28, e);
    }

    @Test
    public void testConstraintInFunctionWithTypeParameters()
    {
        compileTestSource("/test/repro.pure",
                """
                function myFunction<T>(col:T[*], toRemove:Any[*]):T[*]
                [
                   notEmptyBefore: $col->size() > 0,
                   notEmptyAfter: $return->size() > 0
                ]
                {
                   $col->filter(x | !$toRemove->filter(y | $x == $y)->size() != 0);
                }
                
                function testSucceed():Any[*]
                {
                   myFunction([1, 2, 3], [4, 5, 6]);
                }
                
                function testFailPre():Any[*]
                {
                   myFunction([], [4, 5, 6]);
                }
                function testFailPost():Any[*]
                {
                   myFunction([1, 2, 3], [1, 2, 3]);
                }
                """
        );
        execute("testSucceed():Any[*]");
        PureExecutionException ePre = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFailPre():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[notEmptyBefore] violated. (Function:myFunction_T_MANY__Any_MANY__T_MANY_)", "/test/repro.pure", 17, 4, 17, 4, 17, 13, ePre);
        PureExecutionException ePost = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFailPost():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (POST):[notEmptyAfter] violated. (Function:myFunction_T_MANY__Any_MANY__T_MANY_)", "/test/repro.pure", 21, 4, 21, 4, 21, 13, ePost);
    }

    @Test
    public void testConstraintInClassWithMilestoning()
    {
        compileTestSource("/test/repro.pure",
                """
                Class <<temporal.businesstemporal>> MyClass
                [
                   differentName: $this.others($this.businessDate)->filter(o | $o.name == $this.name)->size() == 0
                ]
                {
                   name: String[1];
                   others: OtherClass[*];
                }
                
                Class <<temporal.businesstemporal>> OtherClass
                {
                   name: String[1];
                }
                
                function testSucceed():Any[*]
                {
                    let bd = %2023-01-11;
                    ^MyClass(name='me', businessDate=$bd, othersAllVersions=^OtherClass(name='you', businessDate=$bd));
                }
                
                function testFail():Any[*]
                {
                    let bd = %2023-01-11;
                    ^MyClass(name='me', businessDate=$bd, othersAllVersions=[^OtherClass(name='you', businessDate=$bd), ^OtherClass(name='me', businessDate=$bd)]);
                }
                """
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assertions.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[differentName] violated in the Class MyClass", "/test/repro.pure", 24, 5, 24, 5, 24, 146, e);
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository test = new GenericCodeRepository("test", null, "platform");
        repositories.add(test);
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(EMPLOYEE_SOURCE_NAME, EMPLOYEE_SOURCE_CODE);
    }
}
