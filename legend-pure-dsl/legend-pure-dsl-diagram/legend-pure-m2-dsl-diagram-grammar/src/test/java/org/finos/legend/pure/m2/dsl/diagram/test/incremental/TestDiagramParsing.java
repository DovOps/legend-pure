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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AssociationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.TypeView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class TestDiagramParsing extends AbstractPureTestWithCoreCompiled
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

    private Parser getRuntimeDiagramParser() throws NoSuchFieldException, IllegalAccessException
    {
        Field field = runtime.getSourceRegistry().getClass().getDeclaredField("parserLibrary");
        field.setAccessible(true);
        ParserLibrary parserLibrary = (ParserLibrary) field.get(runtime.getSourceRegistry());
        Parser parser = parserLibrary.getParser("Diagram");

        return parser;
    }

    @Test
    public void testDiagramWithInvalidGeometry() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty geometry
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram() {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {WIDTH, HEIGHT} found: ')'", 2, 33, e);
        }

        // Width but no height
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0) {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ',' found: ')'", 2, 43, e);
        }

        // Height but no width
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(height=10.0) {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ',' found: ')'", 2, 44, e);
        }

        // Wrong property first
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(junk=10.0) {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "token recognition error at: '='", 2, 37, e);
        }

        // Wrong property second
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0, junk=10.0) {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: HEIGHT found: 'junk'", 2, 45, e);
        }

        // Extra property
        try
        {
            compileTestSource("###Diagram\nDiagram test::pure::TestDiagram(width=10.0, height=10.0, junk=13.2) {}");
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: ')' found: ','", 2, 56, e);
        }
    }

    @Test
    public void testDiagramWithInvalidTypeView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty type view
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass_1()
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {TYPE, STEREOTYPES_VISIBLE, ATTRIBUTES_VISIBLE, ATTRIBUTE_STEREOTYPES_VISIBLE, ATTRIBUTE_TYPES_VISIBLE, COLOR, LINE_WIDTH, POSITION, WIDTH, HEIGHT} found: ')'", 4, 26, e);
        }

        // Type view with one bogus property
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass_1(junk=13.6)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "token recognition error at: '='", 4, 30, e);
        }

        // Type view with all valid properties but one
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass_1(stereotypesVisible=true, attributesVisible=true,
                                             attributeStereotypesVisible=true, attributeTypesVisible=true,
                                             color=#FFFFCC, lineWidth=1.0,
                                             position=(874.0, 199.46875), width=353.0, height=57.1875)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'type' on TypeView TestClass_1", 4, 14, e);
        }

        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass_1(type=test::pure::TestClass, stereotypesVisible=true, attributesVisible=true,
                                             attributeStereotypesVisible=true, attributeTypesVisible=true,
                                             color=#FFFFCC,
                                             position=(874.0, 199.46875), width=353.0, height=57.1875)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on TypeView TestClass_1", 4, 14, e);
        }
    }

    @Test
    public void testDiagramWithInvalidAssociationView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty association view
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        AssociationView TestAssociation_1()
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {ASSOCIATION, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, SOURCE_PROP_POSITION, SOURCE_MULT_POSITION, TARGET_PROP_POSITION, TARGET_MULT_POSITION} found: ')'", 4, 39, e);
        }

        // Association view with one bogus property
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        AssociationView TestAssociation_1(junk=13.6)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "token recognition error at: '='", 4, 43, e);
        }

        // Association view with all valid properties but one
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        AssociationView TestAssociation_1(stereotypesVisible=true, nameVisible=false,
                                                          color=#000000, lineWidth=1.0,
                                                          lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                          label='Employment',
                                                          source=TestClass1_1,
                                                          target=TestClass2_2,
                                                          sourcePropertyPosition=(132.5, 76.2),
                                                          sourceMultiplicityPosition=(132.5, 80.0),
                                                          targetPropertyPosition=(155.2, 76.2),
                                                          targetMultiplicityPosition=(155.2, 80.0))
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'association' on AssociationView TestAssociation_1", 12, 21, e);
        }

        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        AssociationView TestAssociation_1(association=test::pure::TestAssociation, stereotypesVisible=true, nameVisible=false,
                                                          color=#000000,
                                                          lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                          label='Employment',
                                                          source=TestClass1_1,
                                                          target=TestClass2_2,
                                                          sourcePropertyPosition=(132.5, 76.2),
                                                          sourceMultiplicityPosition=(132.5, 80.0),
                                                          targetPropertyPosition=(155.2, 76.2),
                                                          targetMultiplicityPosition=(155.2, 80.0))
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on AssociationView TestAssociation_1", 12, 21, e);
        }
    }

    @Test
    public void testDiagramWithInvalidPropertyView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Create class with property
        compileTestSource("""
                Class test::pure::TestClass1
                {
                    testProperty : test::pure::TestClass2[1];
                }
                Class test::pure::TestClass2
                {
                }
                """);

        // Empty property view
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        PropertyView TestClass1_testProperty_1()
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {PROPERTY, STEREOTYPES_VISIBLE, NAME_VISIBLE, COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET, PROP_POSITION, MULT_POSITION} found: ')'", 4, 44, e);
        }

        // Property view with one bogus property
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        PropertyView TestClass1_testProperty_1(junk=13.6)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "token recognition error at: '='", 4, 48, e);
        }

        // Property view with all valid properties but one
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        PropertyView TestClass1_testProperty_1(stereotypesVisible=true, nameVisible=false,
                                                               color=#000000, lineWidth=1.0,
                                                               lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                               label='Employment',
                                                               source=TestClass1_1,
                                                               target=TestClass2_2,
                                                               propertyPosition=(132.5, 76.2),
                                                               multiplicityPosition=(132.5, 80.0))
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'property' on PropertyView TestClass1_testProperty_1", 12, 18, e);
        }

        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        PropertyView TestClass1_testProperty_1(property=test::pure::TestClass1.testProperty, stereotypesVisible=true, nameVisible=false,
                                                               color=#000000,
                                                               lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                               label='Employment',
                                                               source=TestClass1_1,
                                                               target=TestClass2_2,
                                                               propertyPosition=(132.5, 76.2),
                                                               multiplicityPosition=(132.5, 80.0))
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on PropertyView TestClass1_testProperty_1", 12, 18, e);
        }
    }

    @Test
    public void testDiagramWithInvalidGeneralizationView() throws NoSuchFieldException, IllegalAccessException
    {
        Parser parser = getRuntimeDiagramParser();
        // Empty generalization view
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        GeneralizationView TestClass1_TestClass2_1()
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {COLOR, LINE_WIDTH, LINE_STYLE, POINTS, LABEL, SOURCE, TARGET} found: ')'", 4, 48, e);
        }

        // Association view with one bogus property
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        GeneralizationView TestClass1_TestClass2_1(junk=13.6)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "token recognition error at: '='", 4, 52, e);
        }

        // Association view with all valid properties but one
        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        GeneralizationView TestClass1_TestClass2_1(lineWidth=1.0,
                                                                   lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                                   label='',
                                                                   source=TestClass1_1,
                                                                   target=TestClass2_2)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'color' on GeneralizationView TestClass1_TestClass2_1", 12, 24, e);
        }

        try
        {
            compileTestSource("""
                    ###Diagram
                    Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                    {
                        TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(874.0, 199.46875), width=353.0, height=57.1875)
                        TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                              attributeStereotypesVisible=true, attributeTypesVisible=true,
                                              color=#FFFFCC, lineWidth=1.0,
                                              position=(75.0, 97.1875), width=113.0, height=57.1875)
                        GeneralizationView TestClass1_TestClass2_1(color=#000000,
                                                                   lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                                   label='',
                                                                   source=TestClass1_1,
                                                                   target=TestClass2_2)
                    }
                    """);
            Assertions.fail("Expected a parser error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "Missing value for property 'lineWidth' on GeneralizationView TestClass1_TestClass2_1", 12, 24, e);
        }
    }

    @Test
    public void testDiagramWithNoGeometry()
    {
        compileTestSource("""
                ###Diagram
                Diagram test::pure::TestDiagram {}
                """);
        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        Assertions.assertNotNull(diagram);

        CoreInstance geometry = Instance.getValueForMetaPropertyToOneResolved(diagram, "rectangleGeometry", processorSupport);
        Assertions.assertNotNull(diagram);

        CoreInstance width = Instance.getValueForMetaPropertyToOneResolved(geometry, "width", processorSupport);
        Assertions.assertNotNull(width);
        Assertions.assertTrue(Instance.instanceOf(width, "Float", processorSupport));
        Assertions.assertEquals("0.0", width.getName());

        CoreInstance height = Instance.getValueForMetaPropertyToOneResolved(geometry, "height", processorSupport);
        Assertions.assertNotNull(height);
        Assertions.assertTrue(Instance.instanceOf(height, "Float", processorSupport));
        Assertions.assertEquals("0.0", height.getName());
    }

    @Test
    public void testPropertyViewWithPropertyNamedPosition()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1
                {
                    position:test::pure::TestClass2[1];
                }
                
                Class test::pure::TestClass2
                {
                }
                """);
        compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=true, attributesVisible=true,
                                          attributeStereotypesVisible=true, attributeTypesVisible=true,
                                          color=#FFFFCC, lineWidth=1.0,
                                          position=(874.0, 199.46875), width=353.0, height=57.1875)
                    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                          attributeStereotypesVisible=true, attributeTypesVisible=true,
                                          color=#FFFFCC, lineWidth=1.0,
                                          position=(75.0, 97.1875), width=113.0, height=57.1875)
                    PropertyView TestClass1_position_1(property=test::pure::TestClass1.position, stereotypesVisible=true, nameVisible=false,
                                                       color=#000000, lineWidth=1.0,
                                                       lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                       label='Employment',
                                                       source=TestClass1_1,
                                                       target=TestClass2_2,
                                                       propertyPosition=(132.5, 76.2),
                                                       multiplicityPosition=(132.5, 80.0))
                }
                """);
        CoreInstance testClass1 = runtime.getCoreInstance("test::pure::TestClass1");
        CoreInstance positionProp = processorSupport.class_findPropertyUsingGeneralization(testClass1, "position");
        Assertions.assertNotNull(positionProp);

        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        CoreInstance propView = Instance.getValueForMetaPropertyToOneResolved(diagram, M3Properties.propertyViews, processorSupport);
        CoreInstance viewProp = Instance.getValueForMetaPropertyToOneResolved(propView, M3Properties.property, processorSupport);
        Assertions.assertSame(positionProp, viewProp);
    }

    @Test
    public void testDiagramWithVariousWhiteSpace()
    {
        compileTestSource("testModel.pure",
                """
                Class test::pure::TestClass1
                {
                    position:test::pure::TestClass2[1];
                }
                
                Class test::pure::TestClass2
                {
                }
                """);
        compileTestSource("testDiagram.pure",
                """
                ###Diagram
                Diagram test::pure::TestDiagram(width=10.0, height=10.0)
                {
                    TypeView TestClass1_1(type=test::pure::TestClass1, stereotypesVisible=
                \
                                                                     true, attributesVisible=true,
                                          attributeStereotypesVisible     =    true, attributeTypesVisible=true,
                                                    color=#FFFFCC, lineWidth=\
                                                                            1.0,
                                                    position\s
                \
                                                       =(874.0, 199.46875), width   
                                                         =353.0, height=57.1875)
                    TypeView TestClass2_2(type=test::pure::TestClass2, stereotypesVisible=true, attributesVisible=true,
                                                    attributeStereotypesVisible=true, attributeTypesVisible=true,
                                                    color=#FFFFCC, lineWidth=1.0,
                                                    position=(75.0, 97.1875), width=113.0, height=57.1875)
                    PropertyView TestClass1_position_1(property=test::pure::TestClass1.position, stereotypesVisible=true, nameVisible=false,
                                                                 color=#000000, lineWidth=1.0,
                                                                 lineStyle=SIMPLE, points=[(132.5, 77.0), (155.2, 77.0)],
                                                                 label='Employment',
                                                                 source=TestClass1_1,
                                                                 target=TestClass2_2,
                                                                 propertyPosition=(132.5, 76.2),
                                                                 multiplicityPosition=(132.5, 80.0))
                }
                """);
        CoreInstance testClass1 = runtime.getCoreInstance("test::pure::TestClass1");
        CoreInstance positionProp = processorSupport.class_findPropertyUsingGeneralization(testClass1, "position");
        Assertions.assertNotNull(positionProp);

        CoreInstance diagram = runtime.getCoreInstance("test::pure::TestDiagram");
        CoreInstance propView = Instance.getValueForMetaPropertyToOneResolved(diagram, M3Properties.propertyViews, processorSupport);
        CoreInstance viewProp = Instance.getValueForMetaPropertyToOneResolved(propView, M3Properties.property, processorSupport);
        Assertions.assertSame(positionProp, viewProp);
    }

    @Test
    public void testDiagramModelDiagram()
    {
        final String source = """
                ###Diagram
                Diagram meta::pure::diagram::DiagramDiagram(width=924.0, height=798.0)
                {
                    TypeView AbstractPathView(type=meta::pure::diagram::AbstractPathView,
                                              stereotypesVisible=true,
                                              attributesVisible=true,
                                              attributeStereotypesVisible=true,
                                              attributeTypesVisible=true,
                                              color=#FFFFCC,
                                              lineWidth=1.0,
                                              position=(599.0, 278.0),
                                              width=123.0,
                                              height=57.1875)
                    TypeView AssociationView(type=meta::pure::diagram::AssociationView,
                                             stereotypesVisible=true,
                                             attributesVisible=true,
                                             attributeStereotypesVisible=true,
                                             attributeTypesVisible=true,
                                             color=#FFFFCC,
                                             lineWidth=1.0,
                                             position=(402.0, 476.0),
                                             width=115.0,
                                             height=42.09375)
                    TypeView Diagram(type=meta::pure::diagram::Diagram,
                                     stereotypesVisible=true,
                                     attributesVisible=true,
                                     attributeStereotypesVisible=true,
                                     attributeTypesVisible=true,
                                     color=#FFFFCC,
                                     lineWidth=1.0,
                                     position=(37.5, 476.0),
                                     width=68.0,
                                     height=42.09375)
                    TypeView DiagramNode(type=meta::pure::diagram::DiagramNode,
                                         stereotypesVisible=true,
                                         attributesVisible=true,
                                         attributeStereotypesVisible=true,
                                         attributeTypesVisible=true,
                                         color=#FFFFCC,
                                         lineWidth=1.0,
                                         position=(310.0, 124.0),
                                         width=299.0,
                                         height=57.1875)
                    TypeView GeneralizationView(type=meta::pure::diagram::GeneralizationView,
                                                stereotypesVisible=true,
                                                attributesVisible=true,
                                                attributeStereotypesVisible=true,
                                                attributeTypesVisible=true,
                                                color=#FFFFCC,
                                                lineWidth=1.0,
                                                position=(599.0, 476.0),
                                                width=129.0,
                                                height=42.09375)
                    TypeView PropertyView(type=meta::pure::diagram::PropertyView,
                                          stereotypesVisible=true,
                                          attributesVisible=true,
                                          attributeStereotypesVisible=true,
                                          attributeTypesVisible=true,
                                          color=#FFFFCC,
                                          lineWidth=1.0,
                                          position=(786.0, 476.0),
                                          width=97.0,
                                          height=42.09375)
                    TypeView TypeView(type=meta::pure::diagram::TypeView,
                                      stereotypesVisible=true,
                                      attributesVisible=true,
                                      attributeStereotypesVisible=true,
                                      attributeTypesVisible=true,
                                      color=#FFFFCC,
                                      lineWidth=1.0,
                                      position=(216.0, 286.0),
                                      width=75.0,
                                      height=42.09375)
                    PropertyView AbstractPathView_source(property=meta::pure::diagram::AbstractPathView.source,
                                                         stereotypesVisible=true,
                                                         nameVisible=true,
                                                         color=#000000,
                                                         lineWidth=-1.0,
                                                         lineStyle=SIMPLE,
                                                         points=[(600.23, 320.0), (290.25, 320.0)],
                                                         label='',
                                                         source=AbstractPathView,
                                                         target=TypeView,
                                                         propertyPosition=(297.5, 320.453125),
                                                         multiplicityPosition=(356.0, 320.453125))
                    PropertyView AbstractPathView_target(property=meta::pure::diagram::AbstractPathView.target,
                                                         stereotypesVisible=true,
                                                         nameVisible=true,
                                                         color=#000000,
                                                         lineWidth=-1.0,
                                                         lineStyle=SIMPLE,
                                                         points=[(600.23, 292.0), (424.0, 292.0), (290.0, 293.0)],
                                                         label='',
                                                         source=AbstractPathView,
                                                         target=TypeView,
                                                         propertyPosition=(299.13357281145454, 278.2436741823325),
                                                         multiplicityPosition=(358.8651741778389, 278.31183889983697))
                    GeneralizationView AbstractPathView_DiagramNode(color=#000000,
                                                                    lineWidth=-1.0,
                                                                    lineStyle=SIMPLE,
                                                                    points=[(459.5, 152.59375), (459.5, 229.59375), (660.5, 229.59375), (660.5, 306.59375)],
                                                                    label='',
                                                                    source=AbstractPathView,
                                                                    target=DiagramNode)
                    GeneralizationView AssociationView_AbstractPathView(color=#000000,
                                                                        lineWidth=-1.0,
                                                                        lineStyle=SIMPLE,
                                                                        points=[(660.5, 306.59375), (660.5, 405.59375), (459.5, 405.59375), (459.5, 497.046875)],
                                                                        label='',
                                                                        source=AssociationView,
                                                                        target=AbstractPathView)
                    GeneralizationView GeneralizationView_AbstractPathView(color=#000000,
                                                                           lineWidth=-1.0,
                                                                           lineStyle=SIMPLE,
                                                                           points=[(660.5, 306.59375), (660.5, 405.59375), (663.5, 405.59375), (663.5, 497.046875)],
                                                                           label='',
                                                                           source=GeneralizationView,
                                                                           target=AbstractPathView)
                    GeneralizationView PropertyView_AbstractPathView(color=#000000,
                                                                     lineWidth=-1.0,
                                                                     lineStyle=SIMPLE,
                                                                     points=[(660.5, 306.59375), (660.5, 405.59375), (834.5, 405.59375), (834.5, 497.046875)],
                                                                     label='',
                                                                     source=PropertyView,
                                                                     target=AbstractPathView)
                    GeneralizationView TypeView_DiagramNode(color=#000000,
                                                            lineWidth=-1.0,
                                                            lineStyle=SIMPLE,
                                                            points=[(459.5, 152.59375), (459.5, 229.59375), (253.5, 229.59375), (253.5, 307.046875)],
                                                            label='',
                                                            source=TypeView,
                                                            target=DiagramNode)
                }
                Diagram meta::pure::diagram::DiagramDiagram1(width=924.0, height=798.0)
                {
                    TypeView AbstractPathView(type=meta::pure::diagram::AbstractPathView,
                                              stereotypesVisible=true,
                                              attributesVisible=true,
                                              attributeStereotypesVisible=true,
                                              attributeTypesVisible=true,
                                              color=#FFFFCC,
                                              lineWidth=1.0,
                                              position=(599.0, 278.0),
                                              width=123.0,
                                              height=57.1875)
                    TypeView AssociationView(type=meta::pure::diagram::AssociationView,
                                             stereotypesVisible=true,
                                             attributesVisible=true,
                                             attributeStereotypesVisible=true,
                                             attributeTypesVisible=true,
                                             color=#FFFFCC,
                                             lineWidth=1.0,
                                             position=(402.0, 476.0),
                                             width=115.0,
                                             height=42.09375)
                    TypeView Diagram(type=meta::pure::diagram::Diagram,
                                     stereotypesVisible=true,
                                     attributesVisible=true,
                                     attributeStereotypesVisible=true,
                                     attributeTypesVisible=true,
                                     color=#FFFFCC,
                                     lineWidth=1.0,
                                     position=(37.5, 476.0),
                                     width=68.0,
                                     height=42.09375)
                    TypeView DiagramNode(type=meta::pure::diagram::DiagramNode,
                                         stereotypesVisible=true,
                                         attributesVisible=true,
                                         attributeStereotypesVisible=true,
                                         attributeTypesVisible=true,
                                         color=#FFFFCC,
                                         lineWidth=1.0,
                                         position=(310.0, 124.0),
                                         width=299.0,
                                         height=57.1875)
                    TypeView GeneralizationView(type=meta::pure::diagram::GeneralizationView,
                                                stereotypesVisible=true,
                                                attributesVisible=true,
                                                attributeStereotypesVisible=true,
                                                attributeTypesVisible=true,
                                                color=#FFFFCC,
                                                lineWidth=1.0,
                                                position=(599.0, 476.0),
                                                width=129.0,
                                                height=42.09375)
                    TypeView PropertyView(type=meta::pure::diagram::PropertyView,
                                          stereotypesVisible=true,
                                          attributesVisible=true,
                                          attributeStereotypesVisible=true,
                                          attributeTypesVisible=true,
                                          color=#FFFFCC,
                                          lineWidth=1.0,
                                          position=(786.0, 476.0),
                                          width=97.0,
                                          height=42.09375)
                    TypeView TypeView(type=meta::pure::diagram::TypeView,
                                      stereotypesVisible=true,
                                      attributesVisible=true,
                                      attributeStereotypesVisible=true,
                                      attributeTypesVisible=true,
                                      color=#FFFFCC,
                                      lineWidth=1.0,
                                      position=(216.0, 286.0),
                                      width=75.0,
                                      height=42.09375)
                    PropertyView AbstractPathView_source(property=meta::pure::diagram::AbstractPathView.source,
                                                         stereotypesVisible=true,
                                                         nameVisible=true,
                                                         color=#000000,
                                                         lineWidth=-1.0,
                                                         lineStyle=SIMPLE,
                                                         points=[(600.23, 320.0), (290.25, 320.0)],
                                                         label='',
                                                         source=AbstractPathView,
                                                         target=TypeView,
                                                         propertyPosition=(297.5, 320.453125),
                                                         multiplicityPosition=(356.0, 320.453125))
                    PropertyView AbstractPathView_target(property=meta::pure::diagram::AbstractPathView.target,
                                                         stereotypesVisible=true,
                                                         nameVisible=true,
                                                         color=#000000,
                                                         lineWidth=-1.0,
                                                         lineStyle=SIMPLE,
                                                         points=[(600.23, 292.0), (424.0, 292.0), (290.0, 293.0)],
                                                         label='',
                                                         source=AbstractPathView,
                                                         target=TypeView,
                                                         propertyPosition=(299.13357281145454, 278.2436741823325),
                                                         multiplicityPosition=(358.8651741778389, 278.31183889983697))
                    GeneralizationView AbstractPathView_DiagramNode(color=#000000,
                                                                    lineWidth=-1.0,
                                                                    lineStyle=SIMPLE,
                                                                    points=[(459.5, 152.59375), (459.5, 229.59375), (660.5, 229.59375), (660.5, 306.59375)],
                                                                    label='',
                                                                    source=AbstractPathView,
                                                                    target=DiagramNode)
                    GeneralizationView AssociationView_AbstractPathView(color=#000000,
                                                                        lineWidth=-1.0,
                                                                        lineStyle=SIMPLE,
                                                                        points=[(660.5, 306.59375), (660.5, 405.59375), (459.5, 405.59375), (459.5, 497.046875)],
                                                                        label='',
                                                                        source=AssociationView,
                                                                        target=AbstractPathView)
                    GeneralizationView GeneralizationView_AbstractPathView(color=#000000,
                                                                           lineWidth=-1.0,
                                                                           lineStyle=SIMPLE,
                                                                           points=[(660.5, 306.59375), (660.5, 405.59375), (663.5, 405.59375), (663.5, 497.046875)],
                                                                           label='',
                                                                           source=GeneralizationView,
                                                                           target=AbstractPathView)
                    GeneralizationView PropertyView_AbstractPathView(color=#000000,
                                                                     lineWidth=-1.0,
                                                                     lineStyle=SIMPLE,
                                                                     points=[(660.5, 306.59375), (660.5, 405.59375), (834.5, 405.59375), (834.5, 497.046875)],
                                                                     label='',
                                                                     source=PropertyView,
                                                                     target=AbstractPathView)
                    GeneralizationView TypeView_DiagramNode(color=#000000,
                                                            lineWidth=-1.0,
                                                            lineStyle=SIMPLE,
                                                            points=[(459.5, 152.59375), (459.5, 229.59375), (253.5, 229.59375), (253.5, 307.046875)],
                                                            label='',
                                                            source=TypeView,
                                                            target=DiagramNode)
                }\
                """;
        compileTestSource("testDiagram.pure",
                source);

        Class typeViewClass = (Class) runtime.getCoreInstance("meta::pure::diagram::TypeView");
        RichIterable<? extends ReferenceUsage> typeViewReferenceUsages = typeViewClass._referenceUsages().select(new Predicate<ReferenceUsage>()
        {
            @Override
            public boolean accept(ReferenceUsage usage)
            {
                return usage._owner() instanceof TypeView;
            }
        });

        String[] lines = source.split("\n");
        for (ReferenceUsage referenceUsage : typeViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assertions.assertEquals("TypeView", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + "TypeView".length() - 1));
        }
    }

    @Test
    public void testDiagramModelDiagramWithAssociationView()
    {
        final String source = """
                ###Diagram
                Diagram meta::pure::diagram::DiagramDiagram(width=924.0, height=798.0)
                {
                    TypeView AssociationView(type=meta::pure::diagram::AssociationView,
                                             stereotypesVisible=true,
                                             attributesVisible=true,
                                             attributeStereotypesVisible=true,
                                             attributeTypesVisible=true,
                                             color=#FFFFCC,
                                             lineWidth=1.0,
                                             position=(402.0, 476.0),
                                             width=115.0,
                                             height=42.09375)
                    TypeView Diagram(type=meta::pure::diagram::Diagram,
                                     stereotypesVisible=true,
                                     attributesVisible=true,
                                     attributeStereotypesVisible=true,
                                     attributeTypesVisible=true,
                                     color=#FFFFCC,
                                     lineWidth=1.0,
                                     position=(37.5, 476.0),
                                     width=68.0,
                                     height=42.09375)
                    TypeView TypeView(type=meta::pure::diagram::TypeView,
                                      stereotypesVisible=true,
                                      attributesVisible=true,
                                      attributeStereotypesVisible=true,
                                      attributeTypesVisible=true,
                                      color=#FFFFCC,
                                      lineWidth=1.0,
                                      position=(216.0, 286.0),
                                      width=75.0,
                                      height=42.09375)
                   AssociationView aview_1(association = meta::pure::diagram::DiagramTypeViews,
                                            stereotypesVisible=true,
                                            nameVisible=false,
                                            color=#000000,
                                            lineWidth=-1.0,
                                            lineStyle=SIMPLE,
                                            points=[(745.13726,491.24757),(444.11680,471.43059)],
                                            label='DiagramTypeViews',
                                            source=Diagram,
                                            target=TypeView,
                                            sourcePropertyPosition=(462.85152, 296.10994),
                                            sourceMultiplicityPosition=(555.18941, 275.10994),
                                            targetPropertyPosition=(450.33789, 270.70564),
                                            targetMultiplicityPosition=(450.33789, 291.70564))
                    AssociationView aview_2(association = meta::pure::diagram::DiagramAssociationViews,
                                            stereotypesVisible=true,
                                            nameVisible=false,
                                            color=#000000,
                                            lineWidth=-1.0,
                                            lineStyle=SIMPLE,
                                            points=[(745.13726,491.24757),(444.11680,471.43059)],
                                            label='DiagramAssociationViews',
                                            source=Diagram,
                                            target=AssociationView,
                                            sourcePropertyPosition=(462.85152, 296.10994),
                                            sourceMultiplicityPosition=(555.18941, 275.10994),
                                            targetPropertyPosition=(450.33789, 270.70564),
                                            targetMultiplicityPosition=(450.33789, 291.70564))
                   } \
                """;
        compileTestSource("testDiagram.pure", source);
        String[] lines = source.split("\n");

        Association associationView = (Association) runtime.getCoreInstance("meta::pure::diagram::DiagramAssociationViews");
        RichIterable<? extends ReferenceUsage> associationViewReferenceUsages = associationView._referenceUsages().select(new Predicate<ReferenceUsage>()
        {
            @Override
            public boolean accept(ReferenceUsage usage)
            {
                return usage._owner() instanceof AssociationView;
            }
        });
        for (ReferenceUsage referenceUsage : associationViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assertions.assertEquals("DiagramAssociationViews", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + "DiagramAssociationViews".length() - 1));
        }

        associationView = (Association) runtime.getCoreInstance("meta::pure::diagram::DiagramTypeViews");
        associationViewReferenceUsages = associationView._referenceUsages().select(new Predicate<ReferenceUsage>()
        {
            @Override
            public boolean accept(ReferenceUsage usage)
            {
                return usage._owner() instanceof AssociationView;
            }
        });
        for (ReferenceUsage referenceUsage : associationViewReferenceUsages)
        {
            SourceInformation sourceInformation = referenceUsage.getSourceInformation();
            Assertions.assertEquals("DiagramTypeViews", lines[sourceInformation.getLine() - 1].substring(sourceInformation.getColumn() - 1, sourceInformation.getColumn() + "DiagramTypeViews".length() - 1));
        }
    }
}
