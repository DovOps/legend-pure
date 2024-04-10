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

package org.finos.legend.pure.m3.tests.elements.property;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestDefaultValue extends AbstractPureTestWithCoreCompiledPlatform
{
    public static final String DECLARATION = """
import test::long::path::*;
Class my::exampleRootType
{
}
Class my::exampleSubType extends my::exampleRootType
{
}
Enum test::long::path::EnumWithDefault
{
   DefaultValue,
   AnotherValue
}
Class test::long::path::A
{
   stringProperty:String[1] = 'default';
   classProperty:my::exampleRootType[1] = ^my::exampleRootType();
   enumProperty:EnumWithDefault[1] = EnumWithDefault.DefaultValue;
   floatProperty:Float[1] = 0.12;
   inheritProperty:Number[1] = 0.12;
   booleanProperty:Boolean[1] = false;
   integerProperty:Integer[1] = 0;
   collectionProperty:String[1..*] = ['one', 'two'];
   enumCollection:test::long::path::EnumWithDefault[1..*] = [test::long::path::EnumWithDefault.DefaultValue, test::long::path::EnumWithDefault.AnotherValue];
   classCollection:my::exampleRootType[1..4] = [^my::exampleRootType(), ^my::exampleSubType()];
   singleProperty:String[1] = ['one'];
   anyProperty:Any[1] = 'anyString';
}
""";

    @Test
    public void testDefaultValue()
    {
        compileTestSource("defaultValueSource.pure", DECLARATION
                + "function check():Boolean[1]\n"
                + "{" +
                "     let d = ^test::long::path::A();" +
                "     let s = setAllValues();"
                + "   assertEquals($d.stringProperty,$d.stringProperty);\n"
                + "   assertEquals($d.classProperty->size(),$d.classProperty->size());\n"
                + "   assertEquals($d.enumProperty,$d.enumProperty);\n"
                + "   assertEquals($d.floatProperty,$d.floatProperty);\n"
                + "   assertEquals($d.inheritProperty,$d.inheritProperty);\n"
                + "   assertEquals($d.booleanProperty,$d.booleanProperty);\n"
                + "   assertEquals($d.integerProperty,$d.integerProperty);\n"
                + "   assertEquals($d.collectionProperty,$d.collectionProperty);\n"
                + "   assertEquals($d.enumCollection,$d.enumCollection);\n"
                + "   assertEquals($d.singleProperty,$d.singleProperty);\n"
                + "   assertEquals($d.anyProperty,$d.anyProperty);\n"
                + "   assertEquals($d.classCollection->size(),$d.classCollection->size());\n"
                + "}\n"
                + "function setAllValues():A[1]\n"
                + "{"
                + " ^test::long::path::A("
                + "     stringProperty='default', "
                + "     classProperty=^my::exampleRootType(), "
                + "     enumProperty = EnumWithDefault.DefaultValue,"
                + "     floatProperty = 0.12,"
                + "     inheritProperty = 0.12,"
                + "     booleanProperty = false,"
                + "     integerProperty = 0,"
                + "     collectionProperty=['one', 'two'],"
                + "     enumCollection=[EnumWithDefault.DefaultValue, EnumWithDefault.AnotherValue],"
                + "     classCollection=[^my::exampleRootType(), ^my::exampleSubType()],"
                + "     singleProperty=['one'],"
                + "     anyProperty='anyString'" +
                "   )" +
                ";\n"
                + "}"

        );
        this.execute("check():Boolean[1]");
     }

    @Test
    public void testDefaultValueOverridden()
    {
        compileTestSource("defaultValueSource.pure", """
                import test::*;
                Class test::A
                {
                   stringProperty:String[1] = 'default';
                   booleanProperty:Boolean[1] = false;
                }
                function testDefaultValueOverridden():Any[*]
                {
                   assertEquals('override', ^test::A(stringProperty = 'override').stringProperty);
                }
                """
        );
        execute("testDefaultValueOverridden():Any[*]");
    }

    @Test
    public void testDefaultValueWithQualifiedProperty()
    {
        compileTestSource("defaultValueSource.pure", """
                import test::*;
                Class test::A
                {
                   stringProperty:String[1] = 'default';
                   prop(){$this.stringProperty + 'Value'}:String[1];
                }
                function test():Boolean[1]
                {\
                   assertEquals('defaultValue', ^test::A().prop());
                }
                """
        );
        execute("test():Boolean[1]");
     }
}
