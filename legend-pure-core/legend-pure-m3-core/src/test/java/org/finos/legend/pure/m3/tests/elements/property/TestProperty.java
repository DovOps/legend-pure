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

package org.finos.legend.pure.m3.tests.elements.property;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestProperty extends AbstractPureTestWithCoreCompiledPlatform
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
    }


    @Test
    public void testGetPath()
    {
        compileTestSource("fromString.pure", """
                import test::*;
                Class test::A
                {
                   prop1 : String[1];
                   prop2 : String[0..1];
                }
                
                Class test::B
                {
                  prop3 : Integer[0..1];
                }
                
                Association test::AB
                {
                  aToB : B[*];
                  bToA : A[*];
                }
                
                Class test::subpackage::C extends A
                {
                  prop2 : String[0..1];
                  prop4 : String[1];
                }\
                """);

        CoreInstance classA = runtime.getCoreInstance("test::A");
        Assertions.assertNotNull(classA);
        MapIterable<String, CoreInstance> classAProperties = processorSupport.class_getSimplePropertiesByName(classA);
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop1"), processorSupport.property_getPath(classAProperties.get("prop1")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop2"), processorSupport.property_getPath(classAProperties.get("prop2")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "propertiesFromAssociations", "aToB"), processorSupport.property_getPath(classAProperties.get("aToB")));

        CoreInstance classB = runtime.getCoreInstance("test::B");
        Assertions.assertNotNull(classB);
        MapIterable<String, CoreInstance> classBProperties = processorSupport.class_getSimplePropertiesByName(classB);
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "B", "properties", "prop3"), processorSupport.property_getPath(classBProperties.get("prop3")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "B", "propertiesFromAssociations", "bToA"), processorSupport.property_getPath(classBProperties.get("bToA")));

        CoreInstance classC = runtime.getCoreInstance("test::subpackage::C");
        Assertions.assertNotNull(classC);
        MapIterable<String, CoreInstance> classCProperties = processorSupport.class_getSimplePropertiesByName(classC);
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "properties", "prop1"), processorSupport.property_getPath(classCProperties.get("prop1")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "subpackage", "children", "C", "properties", "prop2"), processorSupport.property_getPath(classCProperties.get("prop2")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "subpackage", "children", "C", "properties", "prop4"), processorSupport.property_getPath(classCProperties.get("prop4")));
        Assertions.assertEquals(Lists.immutable.with("Root", "children", "test", "children", "A", "propertiesFromAssociations", "aToB"), processorSupport.property_getPath(classCProperties.get("aToB")));
    }

    @Test
    public void testGetSourceType()
    {
        compileTestSource("fromString.pure", """
                import test::*;
                Class test::A
                {
                   prop1 : String[1];
                }
                
                Class test::B
                {
                  prop2 : Integer[0..1];
                }
                
                Association test::AB
                {
                  aToB : B[*];
                  bToA : A[*];
                }\
                """);

        CoreInstance classA = runtime.getCoreInstance("test::A");
        CoreInstance classB = runtime.getCoreInstance("test::B");
        CoreInstance associationAB = runtime.getCoreInstance("test::AB");

        Assertions.assertNotNull(classA);
        Assertions.assertNotNull(classB);
        Assertions.assertNotNull(associationAB);

        CoreInstance prop1 = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "prop1");
        CoreInstance prop2 = classB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "prop2");
        CoreInstance aToB = associationAB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "aToB");
        CoreInstance bToA = associationAB.getValueInValueForMetaPropertyToMany(M3Properties.properties, "bToA");

        Assertions.assertNotNull(prop1);
        Assertions.assertNotNull(prop2);
        Assertions.assertNotNull(aToB);
        Assertions.assertNotNull(bToA);

        Assertions.assertSame(classA, Property.getSourceType(prop1, processorSupport));
        Assertions.assertSame(classB, Property.getSourceType(prop2, processorSupport));
        Assertions.assertSame(classA, Property.getSourceType(aToB, processorSupport));
        Assertions.assertSame(classB, Property.getSourceType(bToA, processorSupport));
    }
}
