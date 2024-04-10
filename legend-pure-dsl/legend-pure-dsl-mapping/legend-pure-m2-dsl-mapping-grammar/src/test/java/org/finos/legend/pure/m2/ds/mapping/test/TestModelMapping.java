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

import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PurePropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.*;

public class TestModelMapping extends AbstractPureMappingTestWithCoreCompiled
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
        runtime.delete("projection.pure");
        runtime.compile();
    }

    @Test
    public void testLiteralMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                Enum myEnum\
                {\
                   a,b\
                }\
                Class Firm\
                {\
                  legalName : String[1];\
                  count : Integer[1];\
                  flag : Boolean[1];\
                  date : Date[1];\
                  f_val :Float[1];\
                  enumVal : myEnum[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {
                    legalName : 'name',
                    count : 2,\
                    flag : true,\
                    f_val : 1.0,\
                    date : %2005-10-10,\
                    enumVal : myEnum.a
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testLiteralEnumErrorMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                Enum myEnum\
                {\
                   a,b\
                }\
                Class Firm\
                {\
                  enumVal : myEnum[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {
                    enumVal : myEnum.z
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "The enum value 'z' can't be found in the enumeration myEnum",
                "mapping.pure", 6, 22, 6, 22, 6, 22, e);
    }

    @Test
    public void testCompileFailureWithMappingTests()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Mapping
                Mapping my::query::TestMapping
                (
                MappingTests
                   [
                      test1
                      (
                         ~query: {x:ui::ClassA|$x.propA},
                         ~inputData: [],
                         ~assert: 'assertString'
                      ),
                      defaultTest
                      (
                         ~query: {|model::domain::Target.all()->graphFetchChecked(#{ClassNotHere{name}}#)->serialize(#{model::domain::Target{name}}#)},
                         ~inputData:\s
                           [\
                           <Object,model::domain::Source, '{"oneName":"oneName 2","anotherName":"anotherName 16","oneDate":"2020-02-05","anotherDate":"2020-04-13","oneNumber":24,"anotherNumber":29}'>,\
                           <Object,SourceClass, '{"oneName":"oneName 2","anotherName":"anotherName 16","oneDate":"2020-02-05","anotherDate":"2020-04-13","oneNumber":24,"anotherNumber":29}'>\
                           ],\
                         ~assert: '{"defects":[],"value":{"name":"oneName 2"},"source":{"defects":[],"value":{"oneName":"oneName 2"},"source":{"number":1,"record":"{"oneName":"oneName 2","anotherName":"anotherName 16","oneDate":"2020-02-05","anotherDate":"2020-04-13","oneNumber":24,"anotherNumber":29}"}}}'
                      )
                   ]\
                )\
                """);
        PureParserException e = Assertions.assertThrows(PureParserException.class, runtime::compile);
        assertPureException(PureParserException.class,
                "Grammar Tests in Mapping currently not supported in Pure",
                "model.pure", 4, 1, 4, 1, 4, 12, e);
    }

    @Test
    public void testMappingWithSource()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class pack::FirmSource\
                {\
                   name : String[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src pack::FirmSource
                    legalName : $src.name
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testMappingWithSourceInt()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                  val : Integer[1];\
                }\
                Class AB\
                {\
                   vale : Integer[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src AB
                    legalName : ['a','b']->map(k|$k+'Yeah!')->joinStrings(','),
                    val : $src.vale
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testProjectionClassMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                Enum myEnum\
                {\
                   a,b\
                }\
                Class Firm\
                {\
                  legalName : String[1];\
                  count : Integer[1];\
                  flag : Boolean[1];\
                  date : Date[1];\
                  f_val :Float[1];\
                  enumVal : myEnum[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  FirmProjection : Pure
                  {
                    legalName : 'name',
                    count : 2,\
                    flag : true,\
                    f_val : 1.0,\
                    date : %2005-10-10,\
                    enumVal : myEnum.a
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.createInMemorySource("projection.pure", """
                Class FirmProjection projects Firm
                {
                   *\
                }\
                """);
        runtime.compile();
        assertSetSourceInformation(source, "FirmProjection");
    }

    @Test
    public void testMappingWithSourceWrongProperty()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class pack::FirmSource\
                {\
                   name : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src pack::FirmSource
                    legalName : $src.nameX
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Can't find the property 'nameX' in the class pack::FirmSource",
                "mapping.pure", 6, 22, 6, 22, 6, 26, e);
    }

    @Test
    public void testMappingWithSourceError()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src pack::FirmSource
                    legalName : 'name'
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "pack::FirmSource has not been defined!",
                "mapping.pure", 5, 10, 5, 10, 5, 13, e);
    }

    @Test
    public void testMappingWithTypeMismatch()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    legalName : 1
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Type Error: 'Integer' not a subtype of 'String'",
                "mapping.pure", 5, 17, 5, 17, 5, 17, e);
    }


    @Test
    public void testMappingWithMultiplicityMismatch()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    legalName : ['a','b']
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Multiplicity Error ' The property 'legalName' has a multiplicity range of [1] when the given expression has a multiplicity range of [2]",
                "mapping.pure", 5, 17, 5, 17, 5, 25, e);
    }


    @Test
    public void testFilter()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class FirmSource\
                {\
                   val : String[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm[firm] : Pure
                  {
                    ~src FirmSource
                    ~filter $src.val == 'ok'
                    legalName : $src.val
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
        CoreInstance mapping = runtime.getCoreInstance("test::TestMapping");
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(new SourceInformation("mapping.pure", 2, 1, 2, 15, 10, 1), mapping.getSourceInformation());
        PureInstanceSetImplementation classMapping = (PureInstanceSetImplementation) mapping.getValueInValueForMetaPropertyToManyWithKey(M2MappingProperties.classMappings, M3Properties.id, "firm");
        Assertions.assertNotNull(classMapping);
        Assertions.assertEquals(new SourceInformation("mapping.pure", 4, 3, 9, 3), classMapping.getSourceInformation());
        Assertions.assertNotNull(classMapping._filter());
        Assertions.assertNotNull(classMapping._filter().getSourceInformation());
    }

    @Test
    public void testFilterError()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class FirmSource\
                {\
                   val : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src FirmSource
                    ~filter $src.valX == 'ok'\
                    legalName : $src.val
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Can't find the property 'valX' in the class FirmSource",
                "mapping.pure", 6, 18, 6, 18, 6, 21, e);
    }


    @Test
    public void testFilterTypeError()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class FirmSource\
                {\
                   val : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {\
                    ~src FirmSource
                    ~filter $src.val\
                    legalName : $src.val
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "A filter should be a Boolean expression",
                "mapping.pure", 6, 18, 6, 18, 6, 20, e);
    }

    @Test
    public void testComplexTypePropertyMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Person\
                {\
                   firms : Firm[*];\
                }\
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class _Person\
                {\
                   firms : _Firm[*];\
                }\
                Class _Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {
                    ~src _Firm
                    legalName : $src.legalName\
                  }
                  Person : Pure
                  {
                    ~src _Person
                    firms : $src.firms\
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Firm");
    }

    @Test
    public void testComplexTypePropertyMappingError()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Person\
                {\
                   firms : Firm[*];\
                }\
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class _Person\
                {\
                   name : String[1];\
                   firms : _Firm[*];\
                }\
                Class _Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {
                    ~src _Person\
                    legalName  : $src.name
                  }
                  Person : Pure
                  {
                    ~src _Person
                    firms : $src.firms\
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Type Error: '_Person' is not '_Firm'",
                "mapping.pure", 11, 18, 11, 18, 11, 22, e);
    }

    @Test
    public void testComplexTypePropertyMappingWithWrongTargetIdError()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Person\
                {\
                   firms : Firm[*];\
                }\
                Class Firm\
                {\
                  legalName : String[1];\
                }\
                Class _Person\
                {\
                   name : String[1];\
                   firms : _Firm[*];\
                }\
                Class _Firm\
                {\
                  legalName : String[1];\
                }\
                """);

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                Mapping test::TestMapping
                (
                  Firm : Pure
                  {
                    ~src _Person\
                    legalName  : $src.name
                  }
                  Person : Pure
                  {
                    ~src _Person
                    firms[f2] : $src.firms\
                  }
                )\
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "The set implementation 'f2' is unknown in the mapping 'TestMapping'",
                "mapping.pure", 11, 5, 11, 5, 11, 9, e);
    }

    @Test
    public void testMilestonedMappingWithLatestDate()
    {
        runtime.createInMemorySource("model.pure",
                """
                Class Firm
                {
                  legalName : String[1];
                  employees : Person[*];
                }
                Class <<temporal.businesstemporal>> Person
                {
                  name : String[1];
                }
                Class TargetFirm
                {
                  legalName : String[1];
                  employeeNames : String[*];
                }
                """);

        String source = """
                ###Mapping
                Mapping test::TestMapping
                (
                  TargetFirm : Pure
                  {
                    ~src Firm\
                    legalName : $src.legalName,
                    employeeNames : $src.employees(%latest)->map(e | $e.name)
                  }
                )\
                """;
        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "TargetFirm");
    }

    @Test
    public void testM2MMappingWithEnumerationMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                import my::*;
                
                Class my::SourceProduct
                {
                   id : Integer[1];
                   state : String[1];
                }
                
                Class my::TargetProduct
                {
                   id : Integer[1];
                   state : State[1];
                }
                
                Enum my::State
                {
                   ACTIVE,
                   INACTIVE
                }\
                """
        );

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                import my::*;
                
                Mapping my::modelMapping
                (
                   TargetProduct : Pure
                   {
                      ~src SourceProduct
                      id : $src.id,
                      state : EnumerationMapping StateMapping : $src.state
                   }
                  \s
                   State : EnumerationMapping StateMapping
                   {
                      ACTIVE : 'ACTIVE',
                      INACTIVE : 'INACTIVE'
                   }
                )\
                """
        );

        runtime.compile();
        Mapping mapping = (Mapping) runtime.getCoreInstance("my::modelMapping");
        PureInstanceSetImplementation m2mMapping = mapping._classMappings().selectInstancesOf(PureInstanceSetImplementation.class).getFirst();

        PurePropertyMapping purePropertyMapping1 = m2mMapping._propertyMappings().selectInstancesOf(PurePropertyMapping.class).getFirst();
        Assertions.assertNull(purePropertyMapping1._transformer());

        PurePropertyMapping purePropertyMapping2 = m2mMapping._propertyMappings().selectInstancesOf(PurePropertyMapping.class).getLast();
        Assertions.assertNotNull(purePropertyMapping2._transformer());
        Assertions.assertTrue(purePropertyMapping2._transformer() instanceof EnumerationMapping);

        EnumerationMapping<?> transformer = (EnumerationMapping<?>) purePropertyMapping2._transformer();
        Assertions.assertEquals("StateMapping", transformer._name());
        Assertions.assertEquals("my::State", PackageableElement.getUserPathForPackageableElement(transformer._enumeration()));
        Assertions.assertEquals(2, transformer._enumValueMappings().size());
    }

    @Test
    public void testM2MMappingWithInvalidEnumerationMapping()
    {
        runtime.createInMemorySource("model.pure",
                """
                ###Pure
                import my::*;
                
                Class my::SourceProduct
                {
                   id : Integer[1];
                   state : String[1];
                }
                
                Class my::TargetProduct
                {
                   id : Integer[1];
                   state : State[1];
                }
                
                Enum my::State
                {
                   ACTIVE,
                   INACTIVE
                }
                
                Enum my::Option
                {
                   CALL,
                   PUT
                }
                """
        );

        runtime.createInMemorySource("mapping.pure",
                """
                ###Mapping
                import my::*;
                
                Mapping my::modelMapping
                (
                   TargetProduct : Pure
                   {
                      ~src SourceProduct
                      id : $src.id,
                      state : EnumerationMapping OptionMapping : $src.state
                   }
                  \s
                   Option : EnumerationMapping OptionMapping
                   {
                      CALL : 'ACTIVE',
                      PUT : 'INACTIVE'
                   }
                )\
                """
        );

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Property : [state] is of type : [my::State] but enumeration mapping : [OptionMapping] is defined on enumeration : [my::Option].", "mapping.pure", 10, 7, e);
    }

    @Test
    @Disabled
    @ToFix
    public void testMissingRequiredPropertyError()
    {
        compileTestSource("model.pure",
                """
                Class test::SourceClass
                {
                    prop1 : String[1];
                }
                
                Class test::TargetClass
                {
                    prop2 : String[1];
                    prop3 : Integer[1];
                }
                """);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "mapping.pure",
                """
                ###Mapping
                import test::*;
                Mapping test::TestMapping
                (
                    TargetClass : Pure
                    {
                        ~src SourceClass
                        prop2 : $src.prop1
                    }
                )
                """));
        assertPureException(PureCompilationException.class, "The following required properties for test::TargetClass are not mapped: prop3", "/test/mapping.pure", 5, 5, 5, 5, 5, 15, e);
    }

    @Test
    public void testMappingWithMerge()
    {
        String source =
                """
Class  example::SourcePersonWithFirstName
{
   id:Integer[1];
   firstName:String[1];
}


Class example::SourcePersonWithLastName
{
   id:Integer[1];
   lastName:String[1];
}
Class example::Person
{
   firstName:String[1];
   lastName:String[1];
}

function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}
###Mapping
Mapping  example::MergeModelMappingSourceWithMatch
(
   *example::Person : Operation
           {
             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })
           }

   example::Person[p1] : Pure
            {
               ~src example::SourcePersonWithFirstName
               firstName : $src.firstName
            }

   example::Person[p2] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }
   example::Person[p3] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }

)\
""";


        runtime.createInMemorySource("mapping.pure", source);
        runtime.compile();
    }

    @Test
    public void testMappingWithMergeInvalidReturn()
    {
        String source =
                """
Class  example::SourcePersonWithFirstName
{
   id:Integer[1];
   firstName:String[1];
}


Class example::SourcePersonWithLastName
{
   id:Integer[1];
   lastName:String[1];
}
Class example::Person
{
   firstName:String[1];
   lastName:String[1];
}

function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}
###Mapping
Mapping  example::MergeModelMappingSourceWithMatch
(
   *example::Person : Operation
           {
             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] |  'test' })
           }

   example::Person[p1] : Pure
            {
               ~src example::SourcePersonWithFirstName
               firstName : $src.firstName
            }

   example::Person[p2] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }
   example::Person[p3] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }

)\
""";


        runtime.createInMemorySource("mapping.pure", source);

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Merge validation function for class: Person does not return Boolean",
                "mapping.pure", 23, 5, 23, 14, 26, 12, e);
    }

    @Test
    public void testMappingWithMergeInvalidParameter()
    {
        String source =
                """
Class  example::SourcePersonWithFirstName
{
   id:Integer[1];
   firstName:String[1];
}


Class example::SourcePersonWithLastName
{
   id:Integer[1];
   lastName:String[1];
}
Class example::Person
{
   id:Integer[1];
   firstName:String[1];
   lastName:String[1];
}

function meta::pure::router::operations::merge(o:meta::pure::mapping::OperationSetImplementation[1]):meta::pure::mapping::SetImplementation[*] {[]}
###Mapping
Mapping  example::MergeModelMappingSourceWithMatch
(
   *example::Person : Operation
           {
             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::Person[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })
           }

   example::Person[p1] : Pure
            {
               ~src example::SourcePersonWithFirstName
               firstName : $src.firstName
            }

   example::Person[p2] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }
   example::Person[p3] : Pure
            {
               ~src example::SourcePersonWithLastName
        lastName :  $src.lastName
            }

)\
""";


        runtime.createInMemorySource("mapping.pure", source);
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class,
                "Merge validation function for class: Person has an invalid parameter. All parameters must be a src class of a merged set",
                "mapping.pure", 24, 5, 24, 14, 27, 12, e);
    }
}
