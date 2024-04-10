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

package org.finos.legend.pure.m3.tests.multiplicity;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMultiplicity
{
    private static int DEFAULT_BOUND_RANGE = 50;

    private static PureRuntime runtime;
    private static ModelRepository repository;
    private static ProcessorSupport support;

    @BeforeAll
    public static void setUp()
    {
        runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();
        repository = runtime.getModelRepository();
        support = runtime.getProcessorSupport();
    }

    @Test
    public void testIsMultiplicityConcrete()
    {
        // Concrete
        Assertions.assertTrue(Multiplicity.isMultiplicityConcrete(support.package_getByUserPath(M3Paths.PureOne)));
        Assertions.assertTrue(Multiplicity.isMultiplicityConcrete(support.package_getByUserPath(M3Paths.ZeroOne)));
        Assertions.assertTrue(Multiplicity.isMultiplicityConcrete(support.package_getByUserPath(M3Paths.ZeroMany)));
        Assertions.assertTrue(Multiplicity.isMultiplicityConcrete(support.package_getByUserPath(M3Paths.OneMany)));

        // Non-concrete
        CoreInstance multiplicity = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.Multiplicity));
        Instance.addValueToProperty(multiplicity, M3Properties.multiplicityParameter, repository.newCoreInstance("m", support.package_getByUserPath(M3Paths.String), null), support);
        Assertions.assertFalse(Multiplicity.isMultiplicityConcrete(multiplicity));
    }

    @Test
    public void testGetMultiplicityParameter()
    {
        CoreInstance multiplicity = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.Multiplicity));
        Instance.addValueToProperty(multiplicity, M3Properties.multiplicityParameter, repository.newCoreInstance("m", support.package_getByUserPath(M3Paths.String), null), support);
                Assertions.assertEquals("m", Multiplicity.getMultiplicityParameter(multiplicity));
    }

    @Test
    public void testIsToOne()
    {
        Assertions.assertTrue(Multiplicity.isToOne(Multiplicity.newMultiplicity(1, 1, support)));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 1, support)));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 10, support)));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(3, 10, support)));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, -1, support)));

        Assertions.assertTrue(Multiplicity.isToOne(Multiplicity.newMultiplicity(1, 1, support), false));
        Assertions.assertTrue(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 1, support), false));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 10, support), false));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(3, 10, support), false));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, -1, support), false));

        Assertions.assertTrue(Multiplicity.isToOne(Multiplicity.newMultiplicity(1, 1, support), true));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 1, support), true));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, 10, support), true));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(3, 10, support), true));
        Assertions.assertFalse(Multiplicity.isToOne(Multiplicity.newMultiplicity(0, -1, support), true));
    }

    @Test
    public void testMultiplicityLowerBoundToInt()
    {
        int upper = DEFAULT_BOUND_RANGE * 2;
        for (int lower = 0; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            CoreInstance multiplicity = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.Multiplicity));
            CoreInstance lowerBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
            CoreInstance upperBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
            Instance.addValueToProperty(lowerBound, M3Properties.value, repository.newCoreInstance(String.valueOf(lower), support.package_getByUserPath(M3Paths.Integer), null), support);
            Instance.addValueToProperty(upperBound, M3Properties.value, repository.newCoreInstance(String.valueOf(upper), support.package_getByUserPath(M3Paths.Integer), null), support);
            Instance.addValueToProperty(multiplicity, M3Properties.lowerBound, lowerBound, support);
            Instance.addValueToProperty(multiplicity, M3Properties.upperBound, upperBound, support);
            Assertions.assertEquals(lower, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        }
    }

    @Test
    public void testMultiplicityUpperBoundToInt()
    {
        int lower = 0;
        for (int upper = 0; upper < DEFAULT_BOUND_RANGE; upper++)
        {
            CoreInstance multiplicity = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.Multiplicity));
            CoreInstance lowerBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
            CoreInstance upperBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
            Instance.addValueToProperty(lowerBound, M3Properties.value, repository.newCoreInstance(String.valueOf(lower), support.package_getByUserPath(M3Paths.Integer), null), support);
            Instance.addValueToProperty(upperBound, M3Properties.value, repository.newCoreInstance(String.valueOf(upper), support.package_getByUserPath(M3Paths.Integer), null), support);
            Instance.addValueToProperty(multiplicity, M3Properties.lowerBound, lowerBound, support);
            Instance.addValueToProperty(multiplicity, M3Properties.upperBound, upperBound, support);
            Assertions.assertEquals(upper, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
        }

        CoreInstance unboundedMultiplicity = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.Multiplicity));
        CoreInstance lowerBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
        CoreInstance upperBound = repository.newAnonymousCoreInstance(null, support.package_getByUserPath(M3Paths.MultiplicityValue));
        Instance.addValueToProperty(lowerBound, M3Properties.value, repository.newCoreInstance(String.valueOf(lower), support.package_getByUserPath(M3Paths.Integer), null), support);
        Instance.addValueToProperty(unboundedMultiplicity, M3Properties.lowerBound, lowerBound, support);
        Instance.addValueToProperty(unboundedMultiplicity, M3Properties.upperBound, upperBound, support);
        Assertions.assertEquals(-1, Multiplicity.multiplicityUpperBoundToInt(unboundedMultiplicity));
    }

    @Test
    public void testPrint()
    {
        // Common multiplicities
        assertPrint("1", Multiplicity.newMultiplicity(1, support));
        assertPrint("0..1", Multiplicity.newMultiplicity(0, 1, support));
        assertPrint("*", Multiplicity.newMultiplicity(0, -1, support));
        assertPrint("1..*", Multiplicity.newMultiplicity(1, -1, support));

        // Other multiplicities
        assertPrint("0", Multiplicity.newMultiplicity(0, 0, support));
        for (int lower = 1; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            assertPrint(lower + "..*", Multiplicity.newMultiplicity(lower, -1, support));
            assertPrint(Integer.toString(lower), Multiplicity.newMultiplicity(lower, support));
            for (int upper = lower + 1; upper < lower + DEFAULT_BOUND_RANGE; upper++)
            {
                assertPrint(lower + ".." + upper, Multiplicity.newMultiplicity(lower, upper, support));
            }
        }
    }

    private void assertPrint(String expected, CoreInstance multiplicity)
    {
        // Without brackets
        Assertions.assertEquals(expected, Multiplicity.print(multiplicity, false));

        StringBuilder builder = new StringBuilder(expected.length());
        Multiplicity.print(builder, multiplicity, false);
        Assertions.assertEquals(expected, builder.toString());

        // With brackets
        String expectedWithBrackets = "[" + expected + "]";

        Assertions.assertEquals(expectedWithBrackets, Multiplicity.print(multiplicity, true));
        Assertions.assertEquals(expectedWithBrackets, Multiplicity.print(multiplicity));

        StringBuilder builderWithBrackets = new StringBuilder(expectedWithBrackets.length());
        Multiplicity.print(builderWithBrackets, multiplicity, true);
        Assertions.assertEquals(expectedWithBrackets, builderWithBrackets.toString());
    }

    @Test
    public void testMultiplicityToSignatureString()
    {
        // Common multiplicities
        Assertions.assertEquals("_1_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(1, support)));
        Assertions.assertEquals("_$0_1$_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(0, 1, support)));
        Assertions.assertEquals("_MANY_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(0, -1, support)));
        Assertions.assertEquals("_$1_MANY$_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(1, -1, support)));

        // Other multiplicities
        Assertions.assertEquals("_0_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(0, 0, support)));
        for (int lower = 1; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            Assertions.assertEquals("_$" + lower + "_MANY$_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(lower, -1, support)));
            Assertions.assertEquals("_" + lower + "_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(lower, support)));
            for (int upper = lower + 1; upper < lower + DEFAULT_BOUND_RANGE; upper++)
            {
                Assertions.assertEquals("_$" + lower + "_" + upper + "$_", Multiplicity.multiplicityToSignatureString(Multiplicity.newMultiplicity(lower, upper, support)));
            }
        }
    }

    @Test
    public void testMakeMultiplicityAsConcreteAsPossible()
    {
        MutableMap<String, CoreInstance> resolvedTypeParameters = Maps.mutable.with();

        CoreInstance concreteMultiplicity = Multiplicity.newMultiplicity(0, 1, support);
        Assertions.assertSame(concreteMultiplicity, Multiplicity.makeMultiplicityAsConcreteAsPossible(concreteMultiplicity, resolvedTypeParameters));

        CoreInstance nonConcreteMultiplicity = Multiplicity.newMultiplicity("m", support);
        Assertions.assertSame(nonConcreteMultiplicity, Multiplicity.makeMultiplicityAsConcreteAsPossible(nonConcreteMultiplicity, resolvedTypeParameters));

        resolvedTypeParameters.put(Multiplicity.getMultiplicityParameter(nonConcreteMultiplicity), concreteMultiplicity);
        Assertions.assertSame(concreteMultiplicity, Multiplicity.makeMultiplicityAsConcreteAsPossible(nonConcreteMultiplicity, resolvedTypeParameters));
    }

    @Test
    public void testNewConcreteMultiplicity()
    {
        for (int lower = 0; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            for (int upper = lower; upper < lower + DEFAULT_BOUND_RANGE; upper++)
            {
                CoreInstance multiplicity = Multiplicity.newMultiplicity(lower, upper, support);
                validateConcreteMultiplicity(multiplicity, lower, upper);
            }
            CoreInstance unboundedMultiplicity = Multiplicity.newMultiplicity(lower, -1, support);
            validateConcreteMultiplicity(unboundedMultiplicity, lower, -1);
        }
    }

    @Test
    public void testNewConcreteSingleValueMultiplicity()
    {
        for (int value = 0; value < DEFAULT_BOUND_RANGE; value++)
        {
            CoreInstance multiplicity = Multiplicity.newMultiplicity(value, support);
            validateConcreteMultiplicity(multiplicity, value, value);
        }
    }

    @Test
    public void testNewNonConcreteMultiplicity()
    {
        validateNonConcreteMultiplicity(Multiplicity.newMultiplicity("m", support), "m");
    }

    @Test
    public void testNewConcreteUnboundedMultiplicity()
    {
        for (int lower = 0; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            CoreInstance multiplicity = Multiplicity.newUnboundedMultiplicity(lower, support);
            validateConcreteMultiplicity(multiplicity, lower, -1);
        }
    }

    @Test
    public void testNewInvalidMultiplicity()
    {
        try
        {
            Multiplicity.newMultiplicity(-1, -1, support);
            Assertions.fail("Expected exception for negative lower bound");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid multiplicity lower bound: -1", e.getMessage());
        }

        try
        {
            Multiplicity.newMultiplicity(5, 2, support);
            Assertions.fail("Expected exception for lower bound greater than upper bound");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid multiplicity: lower bound (5) greater than upper bound (2)", e.getMessage());
        }
    }

    @Test
    public void testCopyMultiplicity()
    {
        CoreInstance pureZero = runtime.getCoreInstance(M3Paths.PureZero);
        Assertions.assertSame(pureZero, Multiplicity.copyMultiplicity(pureZero, true, support));

        CoreInstance zeroOne = runtime.getCoreInstance(M3Paths.ZeroOne);
        Assertions.assertSame(zeroOne, Multiplicity.copyMultiplicity(zeroOne, true, support));

        CoreInstance pureOne = runtime.getCoreInstance(M3Paths.PureOne);
        Assertions.assertSame(pureOne, Multiplicity.copyMultiplicity(pureOne, true, support));

        CoreInstance zeroMany = runtime.getCoreInstance(M3Paths.ZeroMany);
        Assertions.assertSame(zeroMany, Multiplicity.copyMultiplicity(zeroMany, true, support));

        CoreInstance oneMany = runtime.getCoreInstance(M3Paths.OneMany);
        Assertions.assertSame(oneMany, Multiplicity.copyMultiplicity(oneMany, true, support));

        CoreInstance multiplicity1 = Multiplicity.newMultiplicity(15, 93, support);
        CoreInstance copy1 = Multiplicity.copyMultiplicity(multiplicity1, true, support);
        Assertions.assertNotSame(multiplicity1, copy1);
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity1, copy1));

        CoreInstance multiplicity2 = Multiplicity.newUnboundedMultiplicity(41, support);
        CoreInstance copy2 = Multiplicity.copyMultiplicity(multiplicity2, true, support);
        Assertions.assertNotSame(multiplicity2, copy2);
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity2, copy2));

        CoreInstance multiplicity3 = Multiplicity.newMultiplicity("m", support);
        CoreInstance copy3 = Multiplicity.copyMultiplicity(multiplicity3, true, support);
        Assertions.assertNotSame(multiplicity3, copy3);
        Assertions.assertEquals(Multiplicity.getMultiplicityParameter(multiplicity3), Multiplicity.getMultiplicityParameter(copy3));

        // TODO test source info
    }

    @Test
    public void testIsValid()
    {
        for (int lower = 0; lower < DEFAULT_BOUND_RANGE; lower++)
        {
            for (int upper = lower; upper < lower + DEFAULT_BOUND_RANGE; upper++)
            {
                CoreInstance multiplicity = Multiplicity.newMultiplicity(lower, upper, support);
                for (int i = lower; i <= upper; i++)
                {
                    Assertions.assertTrue(Multiplicity.isValid(multiplicity, i), i + " should be valid for " + Multiplicity.print(multiplicity));
                }
                for (int i = 0; i < lower; i++)
                {
                    Assertions.assertFalse(Multiplicity.isValid(multiplicity, i), i + " should NOT be valid for " + Multiplicity.print(multiplicity));
                }
                for (int i = upper + 1; i < upper + DEFAULT_BOUND_RANGE; i++)
                {
                    Assertions.assertFalse(Multiplicity.isValid(multiplicity, i), i + " should NOT be valid for " + Multiplicity.print(multiplicity));
                }
                Assertions.assertFalse(Multiplicity.isValid(multiplicity, Integer.MAX_VALUE));
            }
            CoreInstance multiplicity = Multiplicity.newMultiplicity(lower, -1, support);
            Assertions.assertTrue(Multiplicity.isValid(multiplicity, Integer.MAX_VALUE));
            for (int i = lower; i < lower + DEFAULT_BOUND_RANGE; i++)
            {
                Assertions.assertTrue(Multiplicity.isValid(multiplicity, i), i + " should be valid for " + Multiplicity.print(multiplicity));
            }
            for (int i = 0; i < lower; i++)
            {
                Assertions.assertFalse(Multiplicity.isValid(multiplicity, i), i + " should NOT be valid for " + Multiplicity.print(multiplicity));
            }
        }
    }

    @Test
    public void testConcreteMultiplicitiesEqual()
    {
        CoreInstance multiplicity = Multiplicity.newMultiplicity(1, support);
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, multiplicity));
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity(1, support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity(10, support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity(0, 1, support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity(0, -1, support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity(1, -1, support)));
    }

    @Test
    public void testNonConcreteMultiplicitiesEqual()
    {
        CoreInstance multiplicity = Multiplicity.newMultiplicity("m", support);

        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, multiplicity));
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, multiplicity, false));
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, multiplicity, true));

        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("n", support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("n", support), false));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("n", support), true));

        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("m", support)));
        Assertions.assertFalse(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("m", support), false));
        Assertions.assertTrue(Multiplicity.multiplicitiesEqual(multiplicity, Multiplicity.newMultiplicity("m", support), true));
    }

    @Test
    public void testIntersect()
    {
        // Vacuous cases
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.<CoreInstance>empty()));
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support))));

        // Non-vacuous, bounded cases
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(5, 20, support))));
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(10, 20, support))));
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(10, 20, support), Multiplicity.newMultiplicity(5, 15, support))));

        Assertions.assertFalse(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(11, 20, support))));
        Assertions.assertFalse(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(10, 20, support), Multiplicity.newMultiplicity(11, 21, support))));

        // Non-vacuous, unbounded cases
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newUnboundedMultiplicity(250, support), Multiplicity.newUnboundedMultiplicity(500, support))));
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newUnboundedMultiplicity(0, support), Multiplicity.newUnboundedMultiplicity(250, support), Multiplicity.newUnboundedMultiplicity(500, support))));
        Assertions.assertTrue(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newUnboundedMultiplicity(0, support), Multiplicity.newMultiplicity(5, 20, support))));

        Assertions.assertFalse(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newUnboundedMultiplicity(30, support), Multiplicity.newMultiplicity(5, 20, support))));
        Assertions.assertFalse(Multiplicity.intersect(Lists.immutable.with(Multiplicity.newUnboundedMultiplicity(0, support), Multiplicity.newMultiplicity(5, 20, support), Multiplicity.newUnboundedMultiplicity(21, support))));
    }

    @Test
    public void testSubsumes()
    {
        // Bounded reflexive subsumption
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(0, 10, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newMultiplicity(10, 100, support), Multiplicity.newMultiplicity(10, 100, support)));

        // Unbounded reflexive subsumtion
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(0, support), Multiplicity.newUnboundedMultiplicity(0, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newUnboundedMultiplicity(10, support)));

        // Bounded proper subsumption
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(5, 10, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(0, 5, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newMultiplicity(10, 100, support), Multiplicity.newMultiplicity(30, 50, support)));

        // Unbounded proper subsumption
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(0, support), Multiplicity.newUnboundedMultiplicity(10, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newUnboundedMultiplicity(100, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newMultiplicity(10, 200, support)));
        Assertions.assertTrue(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newMultiplicity(100, 200, support)));

        // Bounded intersecting non-subsumption
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(5, 11, support)));
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(2, 10, support), Multiplicity.newMultiplicity(0, 5, support)));
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(10, 100, support), Multiplicity.newMultiplicity(5, 50, support)));

        // Unbounded intersecting non-subsumption
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newUnboundedMultiplicity(5, support)));
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newMultiplicity(5, 200, support)));

        // Bounded disjoint non-subsumption
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(100, 200, support)));
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(0, 10, support), Multiplicity.newMultiplicity(11, 12, support)));
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newMultiplicity(10, 100, support), Multiplicity.newMultiplicity(300, 500, support)));

        // Unbounded disjoint non-subsumption
        Assertions.assertFalse(Multiplicity.subsumes(Multiplicity.newUnboundedMultiplicity(10, support), Multiplicity.newMultiplicity(5, 7, support)));
    }

    @Test
    public void testMinSubsumingMultiplicity()
    {
        MutableList<CoreInstance> multiplicities = Lists.mutable.with();
        try
        {
            Multiplicity.minSubsumingMultiplicity(multiplicities, support);
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Cannot find minimal subsuming multiplicity for an empty set", e.getMessage());
        }

        multiplicities.add(Multiplicity.newMultiplicity(4, 10, support));
        assertMultiplicityHasBounds(4, 10, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newMultiplicity(5, 10, support));
        assertMultiplicityHasBounds(4, 10, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newMultiplicity(7, 21, support));
        assertMultiplicityHasBounds(4, 21, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newUnboundedMultiplicity(71, support));
        assertMultiplicityHasBounds(4, -1, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newMultiplicity(1, support));
        assertMultiplicityHasBounds(1, -1, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newMultiplicity(0, 1, support));
        assertMultiplicityHasBounds(0, -1, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        multiplicities.add(Multiplicity.newMultiplicity("m", support));
        assertMultiplicityHasBounds(0, -1, Multiplicity.minSubsumingMultiplicity(multiplicities, support));

        assertMultiplicityHasBounds(0, -1, Multiplicity.minSubsumingMultiplicity(Lists.mutable.with(Multiplicity.newMultiplicity("m", support)), support));
    }

    // Helpers

    private void assertMultiplicityHasBounds(int lower, int upper, CoreInstance multiplicity)
    {
        assertMultiplicitiesEqual(Multiplicity.newMultiplicity(lower, upper, support), multiplicity);
    }

    private void assertMultiplicitiesEqual(CoreInstance expected, CoreInstance actual)
    {
        if (!Multiplicity.multiplicitiesEqual(expected, actual))
        {
            Assertions.assertEquals(Multiplicity.print(expected), Multiplicity.print(actual));
        }
    }

    private void validateConcreteMultiplicity(CoreInstance multiplicity, int lower, int upper)
    {
        assertInstanceOf(multiplicity, M3Paths.Multiplicity);
        CoreInstance parameter = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.multiplicityParameter, support);
        Assertions.assertNull(parameter);

        CoreInstance lowerBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.lowerBound, support);
        Assertions.assertNotNull(lowerBound);
        CoreInstance lowerBoundValue = Instance.getValueForMetaPropertyToOneResolved(lowerBound, M3Properties.value, support);
        assertInstanceOf(lowerBoundValue, M3Paths.Integer);
        Assertions.assertEquals(lower, PrimitiveUtilities.getIntegerValue(lowerBoundValue));

        CoreInstance upperBound = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.upperBound, support);
        Assertions.assertNotNull(upperBound);
        CoreInstance upperBoundValue = Instance.getValueForMetaPropertyToOneResolved(upperBound, M3Properties.value, support);
        if (upper < 0)
        {
            Assertions.assertNull(upperBoundValue);
        }
        else
        {
            Assertions.assertNotNull(upperBoundValue);
            assertInstanceOf(upperBoundValue, M3Paths.Integer);
            Assertions.assertEquals(upper, PrimitiveUtilities.getIntegerValue(upperBoundValue));
        }

        Assertions.assertTrue(Multiplicity.isMultiplicityConcrete(multiplicity));
        Assertions.assertEquals(lower, Multiplicity.multiplicityLowerBoundToInt(multiplicity));
        Assertions.assertEquals(upper, Multiplicity.multiplicityUpperBoundToInt(multiplicity));
    }

    private void validateNonConcreteMultiplicity(CoreInstance multiplicity, String parameterName)
    {
        assertInstanceOf(multiplicity, M3Paths.Multiplicity);
        CoreInstance parameter = Instance.getValueForMetaPropertyToOneResolved(multiplicity, M3Properties.multiplicityParameter, support);
        Assertions.assertNotNull(parameter);
        assertInstanceOf(parameter, M3Paths.String);
        Assertions.assertEquals(parameterName, parameter.getName());

        Assertions.assertFalse(Multiplicity.isMultiplicityConcrete(multiplicity));
        Assertions.assertEquals(parameterName, Multiplicity.getMultiplicityParameter(multiplicity));
    }

    private void assertInstanceOf(CoreInstance instance, String typeName)
    {
        if (!Instance.instanceOf(instance, typeName, support))
        {
            Assertions.fail("Expected instance of " + typeName + ", got: " + PackageableElement.getUserPathForPackageableElement(instance.getClassifier()));
        }
    }
}
