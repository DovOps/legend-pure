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

package org.finos.legend.pure.m4.serialization;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.grammar.M4Parser;
import org.finos.legend.pure.m4.statelistener.M4StateListener;
import org.finos.legend.pure.m4.statelistener.VoidM4StateListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSerialization
{
    @Test
    public void testSerial()
    {
        ModelRepository repository = new ModelRepository(370);
        M4StateListener listener = new VoidM4StateListener();

        new M4Parser().parse("""
                             ^package.children[deep].children[Class] PrimitiveType
                             {
                             }
                             
                             ^PrimitiveType String
                             {
                             }
                             
                             ^Package package
                             {
                                 Package.properties[children] :
                                     [
                                         ^Package deep
                                         {
                                               Package.properties[children] :
                                                   [
                                                       package.children[deep].children[Class]
                                                   ]
                                         }
                                     ]
                             }
                             ^package.children[deep].children[Class] Class ?[a/b/c/file.txt:1,3,1,9,45,89]? @package.children[deep].children
                             {
                                 Element.properties[name] : 'Class',
                                 package.children[deep].children[Class].properties[properties] :
                                     [
                                         ^Property properties
                                             {
                                                 Property.properties[type] : Property
                                             }
                                     ]
                             }
                             
                             ^package.children[deep].children[Class] Element ?[e/f/file2.txt:5,13,5,13,12,16]?
                             {
                                 Element.properties[name] : 'Element\\u2022',
                                 package.children[deep].children[Class].properties[properties] :
                                     [
                                         ^Property name
                                             {
                                                 Property.properties[type] : String
                                             }
                                     ]
                             }
                             
                             ^package.children[deep].children[Class] Package ?[t/y/file4.txt:1,2,1,2,12,13]?
                             {
                                 package.children[deep].children[Class].properties[properties] :
                                     [
                                         ^Property children
                                         {
                                             Property.properties[type] : package.children[deep].children[Class]
                                         }
                                     ]
                             }
                             
                             ^package.children[deep].children[Class] Property
                             {
                                 Element.properties[name] : 'Property',
                                 package.children[deep].children[Class].properties[properties] :
                                     [
                                         ^Property type
                                             {
                                                 Property.properties[type] : package.children[deep].children[Class]
                                             }
                                     ]
                             }
                             """, repository, new VoidM4StateListener());

        repository.validate(listener);
        byte[] res = repository.serialize();

        ModelRepository newRepository = new ModelRepository();
        BinaryRepositorySerializer.build(res, newRepository);
        newRepository.validate(listener);

        Assertions.assertEquals(6, newRepository.getTopLevels().size());
        Assertions.assertEquals("PrimitiveType instance Class(a/b/c/file.txt:1,3,1,9,45,89)",newRepository.getTopLevel("PrimitiveType").print("", 10));
        Assertions.assertEquals("String instance PrimitiveType",newRepository.getTopLevel("String").print("", 10));
        Assertions.assertEquals("""
                            package instance Package(t/y/file4.txt:1,2,1,2,12,13)
                                children(Property):
                                    deep instance Package(t/y/file4.txt:1,2,1,2,12,13)
                                        children(Property):
                                            Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                                name(Property):
                                                    Class instance String
                                                properties(Property):
                                                    properties instance Property
                                                        type(Property):
                                                            Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)\
                            """,newRepository.getTopLevel("package").print("", 10));
        Assertions.assertEquals("""
                            Package(t/y/file4.txt:1,2,1,2,12,13) instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                properties(Property):
                                    children instance Property
                                        type(Property):
                                            Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                                name(Property):
                                                    Class instance String
                                                properties(Property):
                                                    properties instance Property
                                                        type(Property):
                                                            Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)\
                            """,newRepository.getTopLevel("Package").print("", 10));
        Assertions.assertEquals("""
                            Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                name(Property):
                                    Property instance String
                                properties(Property):
                                    type instance Property
                                        type(Property):
                                            Class(a/b/c/file.txt:1,3,1,9,45,89) instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                                name(Property):
                                                    Class instance String
                                                properties(Property):
                                                    properties instance Property
                                                        type(Property):
                                                            Property instance Class(a/b/c/file.txt:1,3,1,9,45,89)\
                            """,newRepository.getTopLevel("Property").print("", 10));
        Assertions.assertEquals("""
                            Element(e/f/file2.txt:5,13,5,13,12,16) instance Class(a/b/c/file.txt:1,3,1,9,45,89)
                                name(Property):
                                    Element• instance String
                                properties(Property):
                                    name instance Property
                                        type(Property):
                                            String instance PrimitiveType\
                            """,newRepository.getTopLevel("Element").print("", 10));

    }
}
