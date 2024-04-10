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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumValueMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestEnumerationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
        runtime.delete("model.pure");
    }

    private static Predicate<EnumerationMapping<?>> detectByEnumerationMappingName(String name)
    {
        return enumMapping -> name.equals(enumMapping._name());
    }

    @Test
    public void testInvalidEnumeration()
    {
        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  test::TestEnumeration: EnumerationMapping TestMapping
                  {
                    VAL1 : '1',
                    VAL2 : '2'
                  }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "test::TestEnumeration has not been defined!", "mapping.pure", 4, 9, 4, 9, 4, 23, e);
        }
    }

    @Test
    public void testValidEnumeration()
    {
        runtime.createInMemorySource("model.pure",
                """
                Enum test::TestEnumeration
                {
                  VAL1, VAL2\
                }\
                """);
        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  test::TestEnumeration: EnumerationMapping TestMapping
                  {
                    VAL1 : '1',
                    VAL2 : '2'
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "test::TestEnumeration");
    }

    @Test
    public void testInvalidEnum()
    {
        runtime.createInMemorySource("model.pure",
                """
                Enum test::TestEnumeration
                {
                  VAL1, VAL2\
                }\
                """);
        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  test::TestEnumeration: EnumerationMapping TestMapping
                  {
                    VAL1 : '1',
                    VAL2 : '2',
                    NOT_A_VAL : '3'
                  }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The enum value 'NOT_A_VAL' can't be found in the enumeration test::TestEnumeration", "mapping.pure", 8, 5, 8, 5, 8, 13, e);
        }
    }

    @Test
    public void testValidSimpleEnumToEnumMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : my::SourceEnum.B
                   }
                )\
                """);
        runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().getFirst();
        Assertions.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>) enumerationMapping._enumValueMappings();
        Assertions.assertEquals(2, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assertions.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances1.size());
        Assertions.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValues();
        Assertions.assertEquals(1, sourceValues1.size());
        Assertions.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assertions.assertEquals("A", ((Enum) sourceValues1.get(0))._name());

        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assertions.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances2.size());
        Assertions.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances2.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValues();
        Assertions.assertEquals(1, sourceValues2.size());
        Assertions.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assertions.assertEquals("B", ((Enum) sourceValues2.get(0))._name());
    }

    @Test
    public void testInvalidSourceTypes()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                
                Enum my::SourceEnumA
                {
                   A, B
                }
                
                Enum my::SourceEnumB
                {
                   C, D
                }
                
                Enum my::TargetEnum
                {
                   X, Y, Z
                }
                
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : [SourceEnumA.A, 'A', 1],
                      Y : SourceEnumB.C,
                      Z : 'Z'
                   }
                )\
                """);
        try
        {
            runtime.compile();
            Assertions.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Enumeration Mapping 'TargetEnum' has Source Types: 'my::SourceEnumA', 'String', 'Integer', 'my::SourceEnumB'. Only one source Type is allowed for an Enumeration Mapping", "model.pure", 23, 4, 23, 4, 23, 13, e);
        }
    }

    @Test
    public void testValidComplexEnumToEnumMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                
                Enum my::SourceEnum
                {
                   A, B, C
                }
                
                Enum my::TargetEnum
                {
                   X, Y
                }
                
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping
                   {
                      X : SourceEnum.A,
                      Y : [SourceEnum.B, my::SourceEnum.C]
                   }
                )\
                """);
        runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().getFirst();
        Assertions.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>) enumerationMapping._enumValueMappings();
        Assertions.assertEquals(2, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assertions.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances1.size());
        Assertions.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValues();
        Assertions.assertEquals(1, sourceValues1.size());
        Assertions.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assertions.assertEquals("A", ((Enum) sourceValues1.get(0))._name());

        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assertions.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValuesCoreInstance();
        Assertions.assertEquals(2, sourceValuesCoreInstances2.size());
        Assertions.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances2.get(0))._enumeration()));
        Assertions.assertTrue(sourceValuesCoreInstances2.get(1) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances2.get(1))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValues();
        Assertions.assertEquals(2, sourceValues2.size());
        Assertions.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assertions.assertEquals("B", ((Enum) sourceValues2.get(0))._name());
        Assertions.assertTrue(sourceValues2.get(1) instanceof Enum);
        Assertions.assertEquals("C", ((Enum) sourceValues2.get(1))._name());
    }

    @Test
    public void testValidHybridEnumToEnumMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                
                Enum my::SourceEnum1
                {
                   A, B, C
                }
                
                Enum my::SourceEnum2
                {
                   P, Q, R
                }
                
                Enum my::TargetEnum
                {
                   U, V, W, X, Y, Z
                }
                
                ###Mapping
                import my::*;
                
                Mapping my::TestMapping
                (
                   TargetEnum : EnumerationMapping enumsATargetEnum
                   {
                      U : my::SourceEnum1.A,
                      V : my::SourceEnum1.A,
                      W : [my::SourceEnum1.A, my::SourceEnum1.B],
                      X : [my::SourceEnum1.A, my::SourceEnum1.B, my::SourceEnum1.C],
                      Y : [my::SourceEnum1.A, my::SourceEnum1.B, my::SourceEnum1.C],
                      Z : my::SourceEnum1.C
                   }
                   TargetEnum : EnumerationMapping enumsBTargetEnum
                   {
                      U : my::SourceEnum2.P,
                      V : my::SourceEnum2.P
                ,\
                      W : [my::SourceEnum2.P, my::SourceEnum2. Q,my::SourceEnum2.R]
                   }
                   TargetEnum : EnumerationMapping integersTargetEnum2
                   {
                      X : [4,5,6],
                      Y : 3
                   }
                   TargetEnum : EnumerationMapping stringsTargetEnum2
                   {
                      Y : ['One','Two','Three'],
                      Z : 'A'
                   }
                )\
                """);
        runtime.compile();

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) runtime.getCoreInstance("my::TestMapping");
        EnumerationMapping enumerationMapping = mapping._enumerationMappings().detect(detectByEnumerationMappingName("enumsATargetEnum"));
        Assertions.assertEquals("my::TargetEnum", PackageableElement.getUserPathForPackageableElement(enumerationMapping._enumeration()));

        ImmutableList<EnumValueMapping> enumValueMappings = (ImmutableList<EnumValueMapping>) enumerationMapping._enumValueMappings();
        Assertions.assertEquals(6, enumValueMappings.size());

        EnumValueMapping enumValueMapping1 = enumValueMappings.get(0);
        Assertions.assertEquals("U", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances1.size());
        Assertions.assertTrue(sourceValuesCoreInstances1.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum1", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances1.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues1 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValues();
        Assertions.assertEquals(1, sourceValues1.size());
        Assertions.assertTrue(sourceValues1.get(0) instanceof Enum);
        Assertions.assertEquals("A", ((Enum) sourceValues1.get(0))._name());

        enumValueMappings = (ImmutableList<EnumValueMapping>) mapping._enumerationMappings().detect(detectByEnumerationMappingName("enumsBTargetEnum"))._enumValueMappings();
        EnumValueMapping enumValueMapping2 = enumValueMappings.get(1);
        Assertions.assertEquals("V", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances2.size());
        Assertions.assertTrue(sourceValuesCoreInstances2.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances2.get(0))._enumeration()));
        ImmutableList<CoreInstance> sourceValues2 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValues();
        Assertions.assertEquals(1, sourceValues2.size());
        Assertions.assertTrue(sourceValues2.get(0) instanceof Enum);
        Assertions.assertEquals("P", ((Enum) sourceValues2.get(0))._name());

        EnumValueMapping enumValueMapping3 = enumValueMappings.get(2);
        Assertions.assertEquals("W", enumValueMapping3._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances3 = (ImmutableList<CoreInstance>) enumValueMapping3._sourceValuesCoreInstance();
        Assertions.assertEquals(3, sourceValuesCoreInstances3.size());
        Assertions.assertTrue(sourceValuesCoreInstances3.get(0) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances3.get(0))._enumeration()));
        Assertions.assertTrue(sourceValuesCoreInstances3.get(1) instanceof EnumStub);
        Assertions.assertEquals("my::SourceEnum2", PackageableElement.getUserPathForPackageableElement(((EnumStub) sourceValuesCoreInstances3.get(1))._enumeration()));
        ImmutableList<CoreInstance> sourceValues3 = (ImmutableList<CoreInstance>) enumValueMapping3._sourceValues();
        Assertions.assertEquals(3, sourceValues3.size());
        Assertions.assertTrue(sourceValues3.get(0) instanceof Enum);
        Assertions.assertEquals("P", ((Enum) sourceValues3.get(0))._name());
        Assertions.assertTrue(sourceValues3.get(1) instanceof Enum);
        Assertions.assertEquals("Q", ((Enum) sourceValues3.get(1))._name());
        Assertions.assertTrue(sourceValues3.get(2) instanceof Enum);
        Assertions.assertEquals("R", ((Enum) sourceValues3.get(2))._name());

        enumValueMappings = (ImmutableList<EnumValueMapping>) mapping._enumerationMappings().detect(detectByEnumerationMappingName("integersTargetEnum2"))._enumValueMappings();
        enumValueMapping1 = enumValueMappings.get(0);
        Assertions.assertEquals("X", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances4 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValuesCoreInstance();
        Assertions.assertEquals(3, sourceValuesCoreInstances4.size());
        Assertions.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues4 = enumValueMapping1._sourceValues().toList();
        Assertions.assertEquals(3, sourceValues4.size());
        Assertions.assertEquals(4L, sourceValues4.get(0));
        Assertions.assertEquals(5L, sourceValues4.get(1));
        Assertions.assertEquals(6L, sourceValues4.get(2));
        enumValueMapping2 = enumValueMappings.get(1);
        Assertions.assertEquals("Y", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances5 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances5.size());
        Assertions.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues5 = enumValueMapping2._sourceValues().toList();
        Assertions.assertEquals(1, sourceValues5.size());
        Assertions.assertEquals(3L, sourceValues5.get(0));

        enumValueMappings = (ImmutableList<EnumValueMapping>) mapping._enumerationMappings().detect(detectByEnumerationMappingName("stringsTargetEnum2"))._enumValueMappings();
        enumValueMapping1 = enumValueMappings.get(0);
        Assertions.assertEquals("Y", enumValueMapping1._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances6 = (ImmutableList<CoreInstance>) enumValueMapping1._sourceValuesCoreInstance();
        Assertions.assertEquals(3, sourceValuesCoreInstances6.size());
        Assertions.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues6 = enumValueMapping1._sourceValues().toList();
        Assertions.assertEquals(3, sourceValues6.size());
        Assertions.assertEquals("One", sourceValues6.get(0));
        Assertions.assertEquals("Two", sourceValues6.get(1));
        Assertions.assertEquals("Three", sourceValues6.get(2));
        enumValueMapping2 = enumValueMappings.get(1);
        Assertions.assertEquals("Z", enumValueMapping2._enum()._name());
        ImmutableList<CoreInstance> sourceValuesCoreInstances7 = (ImmutableList<CoreInstance>) enumValueMapping2._sourceValuesCoreInstance();
        Assertions.assertEquals(1, sourceValuesCoreInstances7.size());
        Assertions.assertTrue(sourceValuesCoreInstances4.get(0) instanceof IntegerCoreInstance);
        MutableList<? extends Object> sourceValues7 = enumValueMapping2._sourceValues().toList();
        Assertions.assertEquals(1, sourceValues7.size());
        Assertions.assertEquals("A", sourceValues7.get(0));
    }
}
