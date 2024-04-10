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

package org.finos.legend.pure.runtime.java.interpreted.function.base.tools;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestProfile extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testPathToElementProfile()
    {
        compileTestSource("""
                Class A::B::C::D::E
                {
                }
                
                Class A::B::C::K::D
                {
                }
                
                function test::findElement(path:String[1], separator:String[1]):PackageableElement[1]
                {
                    $path->split($separator)->fold({str:String[1], pkg:PackageableElement[1] | let p = $pkg->cast(@Package).children->filter(c | $c.name == $str);
                                                                                               if ($p->isEmpty(), | fail(| $path + ' is not a valid PackageableElement. Package \\'' + $str + '\\' not found'); $p->toOne();, | $p->toOne());
                                                   }, ::)
                }
                
                function test::testProfile():Nil[0]
                {
                    print('A::B::C::K::D'->test::findElement('::')->profile(false).result, 1);
                }
                """);
        this.execute("test::testProfile():Nil[0]");
        Assertions.assertEquals("""
                '
                ################################################## Profiler report ##################################################
                       ##---------------------------- TreeStart
                       + 1 profile_T_m__Boolean_1__ProfileResult_1_
                       +     1 findElement_String_1__String_1__PackageableElement_1_
                       +         1 fold_T_MANY__Function_1__V_m__V_m_
                       +             1 split_String_1__String_1__String_MANY_
                       +             5 letFunction_String_1__T_m__T_m_
                       +                 5 filter_T_MANY__Function_1__T_MANY_
                       +                     5 children(P)
                       +                         5 cast_Any_m__T_1__T_m_
                       +                     9 equal_Any_MANY__Any_MANY__Boolean_1_
                       +                         9 name(P)
                       +             5 if_Boolean_1__Function_1__Function_1__T_m_
                       +                 5 isEmpty_Any_MANY__Boolean_1_
                       +                 5 toOne_T_MANY__T_1_
                       ##---------------------------- TreeEnd
                ################################################## Finished Report ##################################################
                '\
                """, functionExecution.getConsole().getLine(0));

        Assertions.assertEquals("""
                D instance Class
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] Class instance Class
                            typeArguments(Property):
                                [>1] Anonymous_StripedId instance GenericType
                    generalizations(Property):
                        Anonymous_StripedId instance Generalization
                            general(Property):
                                [>1] Anonymous_StripedId instance GenericType
                            specific(Property):
                                [>1] D instance Class
                    name(Property):
                        D instance String
                    package(Property):
                        K instance Package
                            children(Property):
                                [>1] D instance Class
                            name(Property):
                                [>1] K instance String
                            package(Property):
                                [>1] C instance Package\
                """, functionExecution.getConsole().getLine(1));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
