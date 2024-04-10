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

package org.finos.legend.pure.m3.tests.elements._class;

import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class TestGeneralizationLinearization extends AbstractPureTestWithCoreCompiledPlatform
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
    public void testSimpleLoop()
    {
        // A
        // |
        // A
        assertCompilationException("Class A extends A {}");
    }

    @Test
    public void testIndirectLoop()
    {
        // A
        // |
        // B
        // |
        // A
        assertCompilationException("""
                Class A extends B {}
                Class B extends A {}\
                """);
    }

    @Test
    public void testVeryIndirectLoop()
    {
        // B
        // |
        // C
        // |
        // B
        // |
        // A
        assertCompilationException("""
                Class A extends B {}
                Class B extends C {}
                Class C extends B {}\
                """);
    }

    @Test
    public void testInvalidComplexGeneralization1()
    {
        //    E
        //  /   \
        // C     D
        //  \  / |
        //   B  /
        //   | /
        //   A
        // (but A->D precedes A->B)
        //
        // This is invalid because D must precede B in the linearization since it
        // precedes it in A's generalizations, but B must precede D since D is a
        // generalization of it.
        assertCompilationException("""
                Class A extends D, B {}
                Class B extends C, D {}
                Class C extends E {}
                Class D extends E {}
                Class E {}\
                """);
    }

    @Test
    public void testInvalidComplexGeneralization2() throws PureCompilationException
    {
        //   ________
        //  /        \
        // |    F     |
        // |  /   \   |
        // |_D     E  |
        //   \  /  | /
        //    B    C
        //    | /
        //    A
        //
        // This is invalid because D must precede E in the linearization since it
        // precedes it in B's generalizations, but E must precede D in the
        // linearization since it precedes it in C's generalizations.
        assertCompilationException("""
                Class A extends B, C {}
                Class B extends D, E {}
                Class C extends E, D {}
                Class D extends F {}
                Class E extends F {}
                Class F {}\
                """);
    }

    private void assertCompilationException(String code)
    {
        try
        {
            compileTestSource("fromString.pure", code);
            Assertions.fail("Expected compilation error from:\n" + code);
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, Pattern.compile("Inconsistent generalization hierarchy for .*"), e);
        }
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsWithOneOrphanType()
    {
        String pureSource = "Class A{}";
        compileTestSource("fromString.pure", pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assertions.assertEquals(1, leafTypes.size());
        Assertions.assertEquals(classA, leafTypes.getFirst());
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsInComplexStructureWithOneSharedParent()
    {
        //     F
        //   /   \
        //  D     E
        //   \  /  |
        //    B    C
        //    | /
        //    A
        //
        String pureSource = """
                Class A extends B, C {}
                Class B extends D, E {}
                Class C extends E {}
                Class D extends F {}
                Class E extends F {}
                Class F {}\
                """;
        compileTestSource("fromString.pure", pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assertions.assertEquals(1, leafTypes.size());
        Assertions.assertEquals(this.runtime.getCoreInstance("F"), leafTypes.getFirst());
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsInComplexStructureWithMultipleParents()
    {
        //  D     E
        //   \  /  |
        //    B    C
        //    | /
        //    A
        //
        String pureSource = """
                Class A extends B, C {}
                Class B extends D, E {}
                Class C extends E {}
                Class D {}
                Class E {}\
                """;
        compileTestSource("fromString.pure", pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assertions.assertEquals(2, leafTypes.size());
        Assertions.assertTrue(leafTypes.contains(this.runtime.getCoreInstance("E")));
        Assertions.assertTrue(leafTypes.contains(this.runtime.getCoreInstance("D")));
    }
}
