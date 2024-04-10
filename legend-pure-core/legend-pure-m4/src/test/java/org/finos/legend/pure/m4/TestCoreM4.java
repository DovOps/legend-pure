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

package org.finos.legend.pure.m4;

import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCoreM4
{
    @Test
    public void testReferentialIntegrity()
    {
        ModelRepository repository = new ModelRepository();
        try
        {
            new M4Parser().parse("""
                    ^Class Class
                    {
                        Element.properties[name] : 'Class'
                    }
                    ^Class String\
                    {\
                    }
                    ^Class Element
                    {
                        Element.properties[name] : 'Element',
                        Class.properties[properties] :
                            [
                                ^Property name
                                    {
                                        Property.properties[type] : String
                                    }
                            ]
                    }
                    """, repository, new VoidM4StateListener());

            Assertions.assertEquals("""
                    Class_0 instance Class_0
                        name_6(Property_5):
                            Class_2 instance String_1\
                    """, repository.getTopLevel("Class").printFull(""));

            try
            {
                repository.getTopLevel("Element").printFull("");
                Assertions.fail();
            }
            catch (Exception e)
            {
                Assertions.assertEquals("Error resolving path [Class, properties, properties]: 'properties' is unknown for the key 'properties' in 'Class'", e.getMessage());
            }

            repository.validate(new VoidM4StateListener());

            Assertions.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Property has not been defined!", pe.getInfo());
        }
    }


    @Test
    public void testSimpleConsistent() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("""
                ^Class PrimitiveType
                {
                }
                
                ^PrimitiveType String
                {
                }
                
                ^Class Class
                {
                    Element.properties[name] : 'Class',
                    Class.properties[properties] :
                        [
                            ^Property properties
                                {
                                    Property.properties[type] : Property
                                }
                        ]
                }
                
                ^Class Element
                {
                    Element.properties[name] : 'Element',
                    Class.properties[properties] :
                        [
                            ^Property name
                                {
                                    Property.properties[type] : String
                                }
                        ]
                }
                
                ^Class Property
                {
                    Element.properties[name] : 'Property',
                    Class.properties[properties] :
                        [
                            ^Property type
                                {
                                    Property.properties[type] : Class
                                }
                        ]
                }
                """, repository, new VoidM4StateListener());

        repository.validate(new VoidM4StateListener());

        Assertions.assertEquals("""
                Class_0 instance Class_0
                    name_8(Property_4):
                        Class_3 instance String_2
                    properties_5(Property_4):
                        properties_5 instance Property_4
                            type_10(Property_4):
                                Property_4 instance Class_0
                                    name_8(Property_4):
                                        Property_9 instance String_2
                                    properties_5(Property_4):
                                        type_10 instance Property_4
                                            type_10(Property_4):
                                                Class_0 instance Class_0
                                                    [...]\
                """, repository.getTopLevel("Class").printFull(""));

        Assertions.assertEquals("""
                Element_6 instance Class_0
                    name_8(Property_4):
                        Element_7 instance String_2
                    properties_5(Property_4):
                        name_8 instance Property_4
                            type_10(Property_4):
                                String_2 instance PrimitiveType_1\
                """, repository.getTopLevel("Element").printFull(""));

    }

    @Test
    public void testNameSpacing() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        String body = """
                ^Class Class
                {
                    Class.properties[properties] :
                        [
                            ^Property properties
                                {
                                    Property.properties[type] : Property
                                }
                        ]
                }
                
                ^Class Property
                {
                    Class.properties[properties] :
                        [
                            ^Property type
                                {
                                    Property.properties[type] : Class
                                }
                        ]
                }
                
                ^Class Package
                {
                    Class.properties[properties] :
                        [
                            ^Property children
                            {
                                Property.properties[type] : Class
                            }
                        ]
                }
                
                ^Package Root
                {
                    Package.properties[children] :
                        [
                            ^Package subPackage
                            {
                                Package.properties[children] :
                                                [
                                                    ^Class ClassTest
                                                    {
                
                                                    }
                                                ]
                            }
                        ]
                }
                
                ^Root.children[subPackage].children[ClassTest] T
                {
                
                }
                
                ^Class NewInstance @Root.children[subPackage].children
                {
                
                }\
                """;

        new M4Parser().parse(body, repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());

        Assertions.assertEquals("T_9 instance ClassTest_8", repository.getTopLevel("T").printFull(""));

        Assertions.assertEquals("""
                Root_6 instance Package_4
                    children_5(Property_1):
                        subPackage_7 instance Package_4
                            children_5(Property_1):
                                ClassTest_8 instance Class_0
                                NewInstance_10 instance Class_0\
                """, repository.getTopLevel("Root").printFull(""));
    }


    @Test
    public void testCodeLineAnnotation()
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("""
                ^Class Class ?[a/b/c/file.txt:1,3,1,9,8,9]?\s
                {
                    Element.properties[name] : 'Class',
                    Class.properties[properties] :
                        [
                            ^Property properties
                                {
                                    Property.properties[type] : Property
                                }
                        ]
                }
                ^Class String\
                {\
                }\
                ^Class Property\
                {
                    Class.properties[properties] :
                        [
                            ^Property type
                                {
                                    Property.properties[type] : String
                                }
                        ]
                }\
                ^Class Element ?[otherElement:4,5,4,11,10,19]?
                {
                    Element.properties[name] : 'Element',
                    Class.properties[properties] :
                        [
                            ^Property name ?[file:5,7,5,7,14,22]?
                                {
                                    Property.properties[type] : String
                                }
                        ]
                }
                """, repository, new VoidM4StateListener());

        Assertions.assertEquals("""
                Class_0(a/b/c/file.txt:1,3,1,9,8,9) instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)
                    name_8(file:5,7,5,7,14,22)(Property_3):
                        Class_2 instance String_1
                    properties_4(Property_3):
                        properties_4 instance Property_3
                            type_5(Property_3):
                                Property_3 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)
                                    properties_4(Property_3):
                                        type_5 instance Property_3
                                            type_5(Property_3):
                                                String_1 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)\
                """, repository.getTopLevel("Class").printFull(""));

        Assertions.assertEquals("""
                Element_6(otherElement:4,5,4,11,10,19) instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)
                    name_8(file:5,7,5,7,14,22)(Property_3):
                        Element_7 instance String_1
                    properties_4(Property_3):
                        name_8(file:5,7,5,7,14,22) instance Property_3
                            type_5(Property_3):
                                String_1 instance Class_0(a/b/c/file.txt:1,3,1,9,8,9)\
                """, repository.getTopLevel("Element").printFull(""));
    }


    @Test
    public void testForwardReference() throws Exception
    {
        ModelRepository repository = new ModelRepository();
        new M4Parser().parse("""
                ^Root.children[core].children[Package] Root
                {
                    Root.children[core].children[Any].properties[name] : 'Root',
                    Root.children[core].children[Package].properties[children] : [
                                                        ^Root.children[core].children[Package] core
                                                        {
                                                            Root.children[core].children[Any].properties[name] : 'core',
                                                            Root.children[core].children[Package].properties[children] : []
                                                        }
                                                    ]
                }
                ^Root.children[core].children[Class] Property @Root.children[core].children\
                {\
                }\
                ^Root.children[core].children[Class] String\
                {\
                }\
                ^Root.children[core].children[Class] Package @Root.children[core].children
                {
                    Root.children[core].children[Any].properties[name] : 'Package',
                    Root.children[core].children[Class].properties[properties] :
                        [
                            ^Root.children[core].children[Property] children
                            {
                                Root.children[core].children[Any].properties[name] : 'children'
                            }
                        ]
                }
                ^Root.children[core].children[Class] Class @Root.children[core].children
                {
                    Root.children[core].children[Class].properties[properties] :
                        [
                            ^Root.children[core].children[Property] properties
                                {
                                    Root.children[core].children[Any].properties[name] : 'properties'
                                }
                        ]\
                }\
                ^Root.children[core].children[Class] Any @Root.children[core].children
                {
                    Root.children[core].children[Any].properties[name] : 'Any',
                    Root.children[core].children[Class].properties[properties] :
                        [
                            ^Root.children[core].children[Property] name
                                {
                                    Root.children[core].children[Any].properties[name] : 'classifierGenericType'
                                }
                        ]
                }
                """, repository, new VoidM4StateListener());
        repository.validate(new VoidM4StateListener());
    }
}
