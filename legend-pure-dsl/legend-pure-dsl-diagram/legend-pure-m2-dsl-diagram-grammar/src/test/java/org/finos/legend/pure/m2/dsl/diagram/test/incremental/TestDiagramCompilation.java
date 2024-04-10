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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDiagramCompilation extends AbstractPureTestWithCoreCompiled
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
    }

    @Test
    public void testTypeViewWithNonExistentType()
    {
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testAssociationViewWithNonExistentAssociation()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testPropertyViewWithNonExistentProperty()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentTarget()
    {
        compileTestSource("testModel.pure",
                "Class test::pure::TestClass1 {}\n");
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertSize(1, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testGeneralizationViewWithNonExistentGeneralization()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1 {}
                Class test::pure::TestClass2 {}
                """);
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
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
        compileTestSource("testDiagram.pure",
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
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);
        Verify.assertSize(2, Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.typeViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.associationViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.propertyViews, processorSupport));
        Verify.assertEmpty(Instance.getValueForMetaPropertyToManyResolved(diagram, M3Properties.generalizationViews, processorSupport));
    }

    @Test
    public void testAssociationViewWithInvalidTypeViewId()
    {
        try
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
            compileTestSource("testDiagram.pure",
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
                                                        target=TestAssociation,
                                                        sourcePropertyPosition=(132.5, 76.2),
                                                        sourceMultiplicityPosition=(132.5, 80.0),
                                                        targetPropertyPosition=(155.2, 76.2),
                                                        targetMultiplicityPosition=(155.2, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Object with id 'TestAssociation' is not a TypeView", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testDiagramWithIdConflict()
    {
        try
        {
            compileTestSource("testModel.pure",
                    """
                    Class test::pure::TestClass1 {}
                    Class test::pure::TestClass2 {}
                    """);
            compileTestSource("testDiagram.pure",
                    """
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                           attributeStereotypesVisible=true, attributeTypesVisible=true,
                                           color=#FFFFCC, lineWidth=1.0,
                                           position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                           attributeStereotypesVisible=true, attributeTypesVisible=true,
                                           color=#FFFFCC, lineWidth=1.0,
                                           position=(75.0, 97.1875), width=113.0, height=57.1875)
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Id 'TestClass' is used more than once", "testDiagram.pure", 2, 1, 2, 21, 12, 1, e);
        }
    }

    @Test
    public void testAssociationViewWithWrongSourceType()
    {
        try
        {
            compileTestSource("testModel.pure", """
                    Class test::pure::TestClass1 {}
                    Class test::pure::TestClass2 {}
                    Association test::pure::TestAssociation
                    {
                        prop1:test::pure::TestClass1[0..1];
                        prop2:test::pure::TestClass2[1..*];
                    }
                    """);
            compileTestSource("testDiagram.pure", """
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
                                                        source=TestClass2,
                                                        target=TestClass2,
                                                        sourcePropertyPosition=(132.5, 76.2),
                                                        sourceMultiplicityPosition=(132.5, 80.0),
                                                        targetPropertyPosition=(155.2, 76.2),
                                                        targetMultiplicityPosition=(155.2, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for AssociationView TestAssociation (test::pure::TestClass2) does not match the source type of the association test::pure::TestAssociation (test::pure::TestClass1)", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testAssociationViewWithWrongTargetType()
    {
        try
        {
            compileTestSource("testModel.pure", """
                    Class test::pure::TestClass1 {}
                    Class test::pure::TestClass2 {}
                    Association test::pure::TestAssociation
                    {
                        prop1:test::pure::TestClass1[0..1];
                        prop2:test::pure::TestClass2[1..*];
                    }
                    """);
            compileTestSource("testDiagram.pure", """
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
                                                        target=TestClass1,
                                                        sourcePropertyPosition=(132.5, 76.2),
                                                        sourceMultiplicityPosition=(132.5, 80.0),
                                                        targetPropertyPosition=(155.2, 76.2),
                                                        targetMultiplicityPosition=(155.2, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Target type for AssociationView TestAssociation (test::pure::TestClass1) does not match the target type of the association test::pure::TestAssociation (test::pure::TestClass2)", "testDiagram.pure", 12, 5, 12, 21, 21, 77, e);
        }
    }

    @Test
    public void testPropertyViewWithWrongSourceType()
    {
        try
        {
            compileTestSource("testModel.pure", """
                    Class test::pure::TestClass1
                    {
                        prop:test::pure::TestClass2[1];
                    }
                    Class test::pure::TestClass2 {}
                    """);
            compileTestSource("testDiagram.pure", """
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
                        PropertyView TestClass1_prop(property=test::pure::TestClass1.prop, stereotypesVisible=true, nameVisible=false,
                                                     color=#000000, lineWidth=1.0,
                                                     lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                     label='TestClass1.prop',
                                                     source=TestClass2,
                                                     target=TestClass2,
                                                     propertyPosition=(132.5, 76.2),
                                                     multiplicityPosition=(132.5, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for PropertyView TestClass1_prop (test::pure::TestClass2) does not match the owner of the property test::pure::TestClass1.prop (test::pure::TestClass1)", "testDiagram.pure", 12, 5, 12, 18, 19, 68, e);
        }
    }

    @Test
    public void testPropertyViewWithAssociationProperty()
    {
        try
        {
            compileTestSource("testModel.pure",
                    """
                    Class test::pure::TestClass1 {}
                    Class test::pure::TestClass2 {}
                    Association test::pure::TestAssoc
                    {
                      prop1:test::pure::TestClass1[*];
                      prop2:test::pure::TestClass2[*];
                    }
                    """);
            compileTestSource("testDiagram.pure",
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
                        PropertyView TestClass1_testProperty(property=test::pure::TestClass1.prop2, stereotypesVisible=true, nameVisible=false,
                                                             color=#000000, lineWidth=1.0,
                                                             lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                             label='Employment',
                                                             source=TestClass1,
                                                             target=TestClass2,
                                                             propertyPosition=(132.5, 76.2),
                                                             multiplicityPosition=(132.5, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Source type for PropertyView TestClass1_testProperty (test::pure::TestClass1) does not match the owner of the property test::pure::TestAssoc.prop2 (test::pure::TestAssoc)", "testDiagram.pure", 12, 5, 12, 18, 19, 76, e);
        }
    }

    @Test
    public void testPropertyViewWithWrongTargetType()
    {
        try
        {
            compileTestSource("testModel.pure", """
                    Class test::pure::TestClass1
                    {
                        prop:test::pure::TestClass2[1];
                    }
                    Class test::pure::TestClass2 {}
                    """);
            compileTestSource("testDiagram.pure", """
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
                        PropertyView TestClass1_prop(property=test::pure::TestClass1.prop, stereotypesVisible=true, nameVisible=false,
                                                     color=#000000, lineWidth=1.0,
                                                     lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                     label='TestClass1.prop',
                                                     source=TestClass1,
                                                     target=TestClass1,
                                                     propertyPosition=(132.5, 76.2),
                                                     multiplicityPosition=(132.5, 80.0))
                    }
                    """);
            Assertions.fail("Expected a compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Target type for PropertyView TestClass1_prop (test::pure::TestClass1) does not match the target type of the property test::pure::TestClass1.prop (test::pure::TestClass2)", "testDiagram.pure", 12, 5, 12, 18, 19, 68, e);
        }
    }

    private static final String TEST_MODEL_SOURCE_ID = "testModel.pure";
    private static final String TEST_DIAGRAM_SOURCE_ID = "testDiagram.pure";
    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(
            TEST_MODEL_SOURCE_ID,
            """
            import model::test::*;
            Class model::test::A
            {
              prop:model::test::B[0..1];
            }
            Class model::test::B extends A {}
            Association model::test::A2B
            {
              a : A[1];
              b : B[*];
            }
            """,
            TEST_DIAGRAM_SOURCE_ID,
            """
            ###Diagram
            import model::test::*;
            Diagram model::test::TestDiagram(width=5000.3, height=2700.6)
            {
                TypeView A(type=model::test::A, stereotypesVisible=true, attributesVisible=true,
                           attributeStereotypesVisible=true, attributeTypesVisible=true,
                           color=#FFFFCC, lineWidth=1.0,
                           position=(874.0, 199.46875), width=353.0, height=57.1875)
                TypeView B(type=model::test::B, stereotypesVisible=true, attributesVisible=true,
                           attributeStereotypesVisible=true, attributeTypesVisible=true,
                           color=#FFFFCC, lineWidth=1.0,
                           position=(75.0, 97.1875), width=113.0, height=57.1875)
                AssociationView A2B(association=model::test::A2B, stereotypesVisible=true, nameVisible=false,
                                    color=#000000, lineWidth=1.0,
                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                    label='A to B',
                                    source=A,
                                    target=B,
                                    sourcePropertyPosition=(132.5, 76.2),
                                    sourceMultiplicityPosition=(132.5, 80.0),
                                    targetPropertyPosition=(155.2, 76.2),
                                    targetMultiplicityPosition=(155.2, 80.0))
                PropertyView A_prop(property=A.prop, stereotypesVisible=true, nameVisible=false,
                                    color=#000000, lineWidth=1.0,
                                    lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                    label='A.prop',
                                    source=A,
                                    target=B,
                                    propertyPosition=(132.5, 76.2),
                                    multiplicityPosition=(132.5, 80.0))
                GeneralizationView B_A(color=#000000, lineWidth=1.0,
                                       lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                       label='',
                                       source=B,
                                       target=A)
            }
            """
    );

    @Test
    public void testMilestonedPropertiesQualifiedPropertiesAreAvailableInTheDiagrams() throws Exception
    {
        MutableMap<String, String> MILESTONED_TEST_SOURCES = Maps.mutable.with(TEST_MODEL_SOURCE_ID,
                """
                import model::test::*;
                Class model::test::A
                {
                  prop:model::test::B[0..1];
                }
                Class <<temporal.businesstemporal>> model::test::B {}
                Association model::test::A2B
                {
                  a : A[1];
                  prop2 : B[*];
                }
                """, TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID));


        runtime.createInMemoryAndCompile(MILESTONED_TEST_SOURCES);
        CoreInstance a = runtime.getCoreInstance("model::test::A");
        CoreInstance edgePointPropertyFromAssociations = a.getValueForMetaPropertyToOne(M3Properties.properties);
        Assertions.assertTrue(MilestoningFunctions.isEdgePointProperty(edgePointPropertyFromAssociations, processorSupport));
        Assertions.assertEquals("propAllVersions", edgePointPropertyFromAssociations.getName());
        CoreInstance testDiagram = runtime.getCoreInstance("model::test::TestDiagram");
        CoreInstance edgePointPropertyInDiagram = Instance.getValueForMetaPropertyToOneResolved(testDiagram, M3Properties.propertyViews, M3Properties.property, processorSupport);
        Assertions.assertTrue(MilestoningFunctions.isGeneratedQualifiedPropertyWithWithAllMilestoningDatesSpecified(edgePointPropertyInDiagram, processorSupport));
        Assertions.assertEquals("prop", edgePointPropertyInDiagram.getValueForMetaPropertyToOne(M3Properties.functionName).getName());

        CoreInstance association = Instance.getValueForMetaPropertyToOneResolved(testDiagram, M3Properties.associationViews, M3Properties.association, processorSupport);
        ListIterable<String> associationPropertyNames = association.getValueForMetaPropertyToMany(M3Properties.properties).collect(CoreInstance.GET_NAME);
        Assertions.assertEquals(Lists.mutable.with("a", "prop2AllVersions"), associationPropertyNames);
        ListIterable<? extends CoreInstance> qualifiedPropertyNames = Instance.getValueForMetaPropertyToManyResolved(association, M3Properties.qualifiedProperties, processorSupport);
        Assertions.assertEquals("prop2", Instance.getValueForMetaPropertyToOneResolved(qualifiedPropertyNames.getFirst(), M3Properties.functionName, processorSupport).getName());
        Assertions.assertEquals("prop2AllVersionsInRange", Instance.getValueForMetaPropertyToOneResolved(qualifiedPropertyNames.getLast(), M3Properties.functionName, processorSupport).getName());
    }
}
