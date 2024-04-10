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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGenerics extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        try
        {
            runtime.compile();
        }
        catch (PureCompilationException e)
        {
            setUp();
        }
    }

    @Test
    public void testGenericInstanceWithoutTypeArguments()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Address
                    {
                       value:String[1];
                    }
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    ^Employee(name='test', address = ^Address(value='coool'))\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 0): Employee", 10, 2, e);
        }
    }

    @Test
    public void testGenericInstanceTooManyTypeArguments()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Address
                    {
                       value:String[1];
                    }
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    ^Employee<Address,Address> emp(name='test', address = ^Address(value='coool'))\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 2): Employee<Address, Address>", 10, 2, e);
        }
    }

    @Test
    public void testGenericInstance()
    {
        Loader.parseM3("""
                Class Address\
                {\
                   value:String[1];\
                }\
                Class Employee<E>\
                {\
                   name : String[1];\
                   address:E[*];\
                }\
                ^Employee<Address> emp (name='test', address = ^Address(value='coool'))\
                """, repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        CoreInstance elem = runtime.getCoreInstance("emp");
        Assertions.assertEquals("""
                emp instance Employee
                    address(Property):
                        Anonymous_StripedId instance Address
                            value(Property):
                                coool instance String
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                Employee instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        Anonymous_StripedId instance ImportStub
                                            idOrPath(Property):
                                                Address instance String
                                            importGroup(Property):
                                                import_fromString_pure_1 instance ImportGroup
                                            resolvedNode(Property):
                                                Address instance Class
                    name(Property):
                        test instance String\
                """, elem.printWithoutDebug("", 10));

        setUp();
    }

    @Test
    public void testGenericInstanceUnknownType()
    {
        try
        {
            Loader.parseM3("""
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    
                    ^Employee<AddressXX> emp (name='test')\
                    """, repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "AddressXX has not been defined!", 7, 11, e);
            setUp();
        }
    }

    @Test
    public void testGenericInstanceTypeMismatch()
    {
        try
        {
            Loader.parseM3("""
                    Class Address
                    {
                       value:String[1];
                    }
                    Class OtherType
                    {
                    }
                    Class Employee<Add>
                    {
                       name : String[1];
                       address:Add[*];
                    }
                    ^Employee<OtherType> emp (name='test', address = ^Address(value='coool'))\
                    """, repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property: 'address' / Type Error: 'Address' not a subtype of 'OtherType'", 13, 51, e);
            setUp();
        }
    }

    @Test
    public void testGenericInstanceWithoutTypeArgumentsInFunction()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Address
                    {
                       value:String[1];
                    }
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    function test():Any[1]
                    {
                       let a = ^Employee(name='test', address = ^Address(value='coool'))
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 0): Employee", 12, 13, e);
        }
    }

    @Test
    public void testGenericInstanceWithTooManyTypeArgumentsInFunction()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Address
                    {
                       value:String[1];
                    }
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    function test():Any[1]
                    {
                       let a = ^Employee<Address,Address>(name='test', address = ^Address(value='coool'))
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Employee<E> (expected 1, got 2): Employee<Address, Address>", 12, 13, e);
        }
    }

    @Test
    public void testGenericInstanceTypeMismatchInFunction()
    {
        try
        {
            runtime.createInMemorySource("fcdffdf.pure", """
                    Class Address
                    {
                       value:String[1];
                    }
                    Class OtherType
                    {
                    }
                    Class Employee<E>
                    {
                       name : String[1];
                       address:E[*];
                    }
                    function test():Nil[0]
                    {
                       let a = ^Employee<OtherType>(name='test', address = ^Address(value='coool'));
                       print($a,1);
                    }\
                    """);
            runtime.compile();
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: Address not a subtype of OtherType", 15, 54, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationNoArguments()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A<E>
                    {
                       value:E[1];
                    }
                    Class B extends A
                    {
                       name : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class A<E> (expected 1, got 0): A", 5, 17, e);
        }
    }

    @Test
    public void testGenericWithGeneralization()
    {
        compileTestSource("fromString.pure", """
                Class A<E>
                {
                   value:E[1];
                }
                Class C
                {
                }
                Class B extends A<C>
                {
                   name : String[1];
                }
                
                ^B x (name='test', value=^C())\
                """);

        Assertions.assertEquals("""
                x instance B
                    name(Property):
                        test instance String
                    value(Property):
                        Anonymous_StripedId instance C\
                """, runtime.getCoreInstance("x").printWithoutDebug(""));
    }

    @Test
    public void testGenericWithGeneralizationWrongType()
    {
        try
        {
            Loader.parseM3("""
                    Class A<E>
                    {
                       value:E[1];
                    }
                    Class C
                    {
                    }
                    Class D
                    {
                    }
                    Class B extends A<C>
                    {
                       name : String[1];
                    }
                    ^B x (name='test', value=^D())\
                    """, repository, ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property: 'value' / Type Error: 'D' not a subtype of 'C'", 15, 27, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationWrongTypeInFunction()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A<E>
                    {
                       value:E[1];
                    }
                    Class C
                    {
                    }
                    Class D
                    
                    {
                    }
                    Class B extends A<C>
                    {
                       name : String[1];
                    }
                    function test():Any[1]
                    {
                       let a = ^B x (name='test', value=^D())
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: D not a subtype of C", 18, 36, e);
        }
    }

    @Test
    public void testGenericWithGeneralizationChainTypeParamWrongType()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class A<T>
                    {
                       value:T[1];
                    }
                    Class C
                    {
                    }
                    Class D
                    {
                    }
                    Class B<U> extends A<U>
                    {
                       name : String[1];
                    }
                    function test():Any[1]
                    {
                       let a = ^B<C> x (name='test', value=^D())
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type Error: D not a subtype of C", 17, 39, e);
        }
    }

    @Test
    public void testGenericsUsedAsParameterError()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Person<T>{firstName:T[1];}
                    function test(p:Person[1]):Nil[0]
                    {
                       [];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Person<T> (expected 1, got 0): Person", 2, 17, e);
        }
    }

    @Test
    public void testGenericsUsedAsReturnTypeError()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Person<T>{firstName:T[1];}
                    function test():Person[*]
                    {
                       [];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Person<T> (expected 1, got 0): Person", 2, 17, e);
        }
    }

    @Test
    public void testGenericAsExtendsError()
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    Class SuperClass<T>
                    {
                      prop:T[1];
                    }
                    Class SubClass extends SuperClass
                    {
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class SuperClass<T> (expected 1, got 0): SuperClass", "fromString.pure", 5, 24, e);
        }
    }
}
