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

package org.finos.legend.pure.m2.dsl.diagram.test.incremental;

import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestSourceMutation extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testDiagram.pure");
        runtime.delete("testModel.pure");
        runtime.delete("testFile.pure");
    }

    @Test
    public void testTypeViewWithNonExistentType()
    {
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                }
                """);

        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(4, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(7, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentAssociation()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,
                                                    color=#000000, lineWidth=1.0,
                                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                    label='TestAssociation',
                                                    source=TestClass1,
                                                    target=TestClass2,
                                                    sourcePropertyPosition=(132.5, 76.2),
                                                    sourceMultiplicityPosition=(132.5, 80.0),
                                                    targetPropertyPosition=(155.2, 76.2),
                                                    targetMultiplicityPosition=(155.2, 80.0))
                }
                """);
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(21, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testPropertyViewWithNonExistentProperty()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    PropertyView TestClass1_testProperty(property=test::pure::TestClass1.testProperty, stereotypesVisible=true, nameVisible=false,
                                                         color=#000000, lineWidth=1.0,
                                                         lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                         label='Employment',
                                                         source=TestClass1,
                                                         target=TestClass2,
                                                         propertyPosition=(132.5, 76.2),
                                                         multiplicityPosition=(132.5, 80.0))
                }
                """);
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(19, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentTarget()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n");
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    GeneralizationView TestClass1_TestClass2(color=#000000, lineWidth=1.0,
                                                             lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                             label='',
                                                             source=TestClass1,
                                                             target=TestClass2)
                }
                """);
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(2, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(8, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(11, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());
        Assertions.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(1).getOne());
        Assertions.assertEquals(16, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(1).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentGeneralization()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    GeneralizationView TestClass1_TestClass2(color=#000000, lineWidth=1.0,
                                                             lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                             label='',
                                                             source=TestClass1,
                                                             target=TestClass2)
                }
                """);
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(16, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentTypeViewId()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                Association test::pure::TestAssociation
                {
                    prop1:test::pure::TestClass1[0..1];
                    prop2:test::pure::TestClass2[1..*];
                }
                """);
        SourceMutation m = compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,
                                                    color=#000000, lineWidth=1.0,
                                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                    label='TestAssociation',
                                                    source=TestClass1,
                                                    target=TestClass3,
                                                    sourcePropertyPosition=(132.5, 76.2),
                                                    sourceMultiplicityPosition=(132.5, 80.0),
                                                    targetPropertyPosition=(155.2, 76.2),
                                                    targetMultiplicityPosition=(155.2, 80.0))
                }
                """);
        // TODO consider whether this is the correct behavior
        Verify.assertSetsEqual(Sets.mutable.with("testDiagram.pure"), m.getModifiedFiles().toSet());
        Verify.assertSize(1, m.getLineRangesToRemoveByFile().get("testDiagram.pure"));
        Assertions.assertEquals(12, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getOne());
        Assertions.assertEquals(21, m.getLineRangesToRemoveByFile().get("testDiagram.pure").get(0).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testAssociationViewWithSourceViewWithNonExistentTypeInTheSameFile()
    {
        SourceMutation m1 = compileTestSource("testFile.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                Association test::pure::TestAssociation
                {
                  toTC1_1 : test::pure::TestClass1[*];
                  toTC2_1 : test::pure::TestClass2[*];
                }
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,
                                                    color=#000000, lineWidth=1.0,
                                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                    label='TestAssociation',
                                                    source=TestClass1,
                                                    target=TestClass2,
                                                    sourcePropertyPosition=(132.5, 76.2),
                                                    sourceMultiplicityPosition=(132.5, 80.0),
                                                    targetPropertyPosition=(155.2, 76.2),
                                                    targetMultiplicityPosition=(155.2, 80.0))
                }
                """);
        Verify.assertEmpty(m1.getLineRangesToRemoveByFile());
        Verify.assertEmpty(m1.getMarkedForDeletion());
        Verify.assertEmpty(m1.getModifiedFiles());

        runtime.modify("testFile.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass3 {}
                Association test::pure::TestAssociation
                {
                  toTC1_1 : test::pure::TestClass1[*];
                  toTC2_1 : test::pure::TestClass3[*];
                }
                
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                        attributeStereotypesVisible=true, attributeTypesVisible=true,
                                        color=#FFFFCC, lineWidth=1.0,
                                        position=(75.0, 97.1875), width=113.0, height=57.1875)
                    AssociationView TestAssociation(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,
                                                    color=#000000, lineWidth=1.0,
                                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                    label='TestAssociation',
                                                    source=TestClass1,
                                                    target=TestClass2,
                                                    sourcePropertyPosition=(132.5, 76.2),
                                                    sourceMultiplicityPosition=(132.5, 80.0),
                                                    targetPropertyPosition=(155.2, 76.2),
                                                    targetMultiplicityPosition=(155.2, 80.0))
                }
                """);
        SourceMutation m2 = runtime.compile();

        Verify.assertSetsEqual(Sets.mutable.with("testFile.pure"), m2.getModifiedFiles().toSet());
        Verify.assertSize(2, m2.getLineRangesToRemoveByFile().get("testFile.pure"));
        Assertions.assertEquals(16, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(0).getOne());
        Assertions.assertEquals(19, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(0).getTwo());
        Assertions.assertEquals(20, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(1).getOne());
        Assertions.assertEquals(29, m2.getLineRangesToRemoveByFile().get("testFile.pure").get(1).getTwo());

        CoreInstance testDiagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(testDiagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(testDiagram, M3Properties.generalizationViews, processorSupport));
    }
}
