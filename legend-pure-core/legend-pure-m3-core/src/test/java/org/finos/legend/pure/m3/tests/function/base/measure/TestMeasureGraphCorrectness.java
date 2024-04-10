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

package org.finos.legend.pure.m3.tests.function.base.measure;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMeasureGraphCorrectness extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("testModel.pure");
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    private static final String massDefinition =
            """
            Measure pkg::Mass
            {
               *Gram: x -> $x;
               Kilogram: x -> $x*1000;
               Pound: x -> $x*453.59;
            }\
            """;

    private static final String distanceDefinition =
            """
            Measure pkg::Distance
            {
               *Meter: x -> $x;
            }
            """;

    private static final String currencyDefinition =
            """
            Measure pkg::Currency
            {
               USD;
               GBP;
               EUR;
            }
            """;

    private static final String currencyDefinitionWithConversions =
            """
            Measure pkg::Currency
            {
               USD: x -> $x * 10;
               GBP;
               EUR;
            }
            """;

    private static final String currencyDefinitionWithCanonicalUnit =
            """
            Measure pkg::Currency
            {
               *USD;
               GBP;
               EUR;
            }
            """;

    @Test
    public void testMeasureBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", massDefinition);
        CoreInstance massCoreInstance = runtime.getCoreInstance("pkg::Mass");
        Assertions.assertEquals("Mass", massCoreInstance.getName());
        Assertions.assertTrue(massCoreInstance instanceof Measure);
        Assertions.assertEquals("Measure", massCoreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName());
        CoreInstance canonicalUnit = massCoreInstance.getValueForMetaPropertyToOne("canonicalUnit");
        Assertions.assertEquals("Mass~Gram", canonicalUnit.getName());
        Assertions.assertTrue(canonicalUnit instanceof Unit);
        ListIterable<? extends CoreInstance> nonCanonicalUnits = massCoreInstance.getValueForMetaPropertyToMany("nonCanonicalUnits");
        Assertions.assertEquals("Mass~Kilogram", nonCanonicalUnits.get(0).getName());
        Assertions.assertTrue(nonCanonicalUnits.get(0) instanceof Unit);
        Assertions.assertEquals("Mass~Pound", nonCanonicalUnits.get(1).getName());
        Assertions.assertTrue(nonCanonicalUnits.get(1) instanceof Unit);
        Assertions.assertEquals("pkg", massCoreInstance.getValueForMetaPropertyToOne(M3Properties._package).getName());
    }

    @Test
    public void testMeasureWithOnlyCanonicalUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", distanceDefinition);
        CoreInstance distanceCoreInstance = runtime.getCoreInstance("pkg::Distance");
        Assertions.assertEquals("Distance", distanceCoreInstance.getName());
        Assertions.assertTrue(distanceCoreInstance instanceof Measure);
        Assertions.assertEquals("Measure", distanceCoreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName());
        CoreInstance canonicalUnit = distanceCoreInstance.getValueForMetaPropertyToOne("canonicalUnit");
        Assertions.assertEquals("Distance~Meter", canonicalUnit.getName());
        Assertions.assertTrue(canonicalUnit instanceof Unit);
        Assertions.assertEquals("pkg", distanceCoreInstance.getValueForMetaPropertyToOne(M3Properties._package).getName());
    }

    @Test
    public void testUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", massDefinition);
        CoreInstance kilogramCoreInstance = runtime.getCoreInstance("pkg::Mass~Kilogram");
        Assertions.assertEquals("Mass~Kilogram", kilogramCoreInstance.getName());
        Assertions.assertTrue(kilogramCoreInstance instanceof Unit);
        Assertions.assertEquals("Unit", kilogramCoreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName());
        Assertions.assertTrue(kilogramCoreInstance.getValueForMetaPropertyToOne(M3Properties.conversionFunction) instanceof LambdaFunction);
        CoreInstance myMeasure = kilogramCoreInstance.getValueForMetaPropertyToOne(M3Properties.measure);
        Assertions.assertEquals("Mass", myMeasure.getName());
        Assertions.assertTrue(myMeasure instanceof Measure);
        CoreInstance myPackage = kilogramCoreInstance.getValueForMetaPropertyToOne(M3Properties._package);
        Assertions.assertEquals("pkg", myPackage.getName());
    }

    @Test
    public void testNonConvertibleUnitBuildsCorrectlyInGraph()
    {
        compileTestSource("testModel.pure", currencyDefinition);
        CoreInstance dollarCoreInstance = runtime.getCoreInstance("pkg::Currency~USD");
        Assertions.assertEquals("Currency~USD", dollarCoreInstance.getName());
        Assertions.assertTrue(dollarCoreInstance instanceof Unit);
        Assertions.assertEquals("Unit", dollarCoreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName());
        Assertions.assertNull(dollarCoreInstance.getValueForMetaPropertyToOne(M3Properties.conversionFunction));
        CoreInstance myMeasure = dollarCoreInstance.getValueForMetaPropertyToOne(M3Properties.measure);
        Assertions.assertEquals("Currency", myMeasure.getName());
        Assertions.assertTrue(myMeasure instanceof Measure);
        CoreInstance myPackage = dollarCoreInstance.getValueForMetaPropertyToOne(M3Properties._package);
        Assertions.assertEquals("pkg", myPackage.getName());
    }

    @Test
    public void testNonConvertibleUnitWithConversionFunctionFailsToCompile()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource("testSource.pure", currencyDefinitionWithConversions));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: '}'", 6, 1, e);
    }

    @Test
    public void testNonConvertibleUnitWithCanonicalUnitUnitFailsToCompile()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () -> compileTestSource("testSource.pure", currencyDefinitionWithCanonicalUnit));
        assertPureException(PureParserException.class, "expected: ':' found: ';'", 3, 8, e);
    }
}
