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
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeDiagram extends AbstractPureTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;
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

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete(TEST_DIAGRAM_SOURCE_ID);
        runtime.delete(TEST_MODEL_SOURCE_ID);
    }

    @Test
    public void testPureRuntimeDiagram_UnloadModel()
    {
        runtime.createInMemoryAndCompile(TEST_SOURCES);
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.delete(TEST_MODEL_SOURCE_ID);
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID)));
            runtime.compile();
            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }

    @Test
    public void testPureRuntimeDiagram_UnloadDiagram()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(TEST_SOURCES)
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .deleteSource(TEST_DIAGRAM_SOURCE_ID)
                        .compile()
                        .createInMemorySource(TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID))
                        .compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testPureRuntimeDiagram_LoadUnloadDiagram()
    {
        runtime.createInMemoryAndCompile(Tuples.pair(TEST_MODEL_SOURCE_ID, TEST_SOURCES.get(TEST_MODEL_SOURCE_ID)));
        int size = runtime.getModelRepository().serialize().length;

        for (int i = 1; i <= TEST_COUNT; i++)
        {
            runtime.createInMemoryAndCompile(Tuples.pair(TEST_DIAGRAM_SOURCE_ID, TEST_SOURCES.get(TEST_DIAGRAM_SOURCE_ID)));
            runtime.compile();

            runtime.delete(TEST_DIAGRAM_SOURCE_ID);
            runtime.compile();

            Assertions.assertEquals(size, repository.serialize().length, "Graph size mismatch at iteration #" + i);
        }
    }
}
