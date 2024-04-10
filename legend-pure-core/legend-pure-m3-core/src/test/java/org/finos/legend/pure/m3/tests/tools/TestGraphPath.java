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

package org.finos.legend.pure.m3.tests.tools;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.GraphPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGraphPath extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), getFactoryRegistryOverride(), getOptions(), getExtra());
        compileTestSource("/test/testModel.pure",
                """
                import test::domain::*;
                Class test::domain::ClassA
                {
                  prop1 : String[1];
                  prop2 : ClassB[*];
                }
                
                Class test::domain::ClassB
                {
                  prop3 : String[0..1];
                }
                
                Measure test::domain::RomanLength
                {
                   *Pes: x -> $x;
                   Cubitum: x -> $x * 1.5;
                   Passus: x -> $x * 5;
                   Actus: x -> $x * 120;
                   Stadium: x -> $x * 625;
                }
                """);
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepositoryProviderHelper.findPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform"),
                GenericCodeRepository.build("test", "test(::.*)?", "platform", "system"));
    }

    @Test
    public void testGetDescription()
    {
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getDescription());
        Assertions.assertEquals(
                "test::domain::ClassA.classifierGenericType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getDescription());
        Assertions.assertEquals(
                "test::domain::ClassA.classifierGenericType.rawType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getDescription());
        Assertions.assertEquals(
                "test::domain::ClassA.properties[0].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getDescription());
        Assertions.assertEquals(
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());
        Assertions.assertEquals(
                "test::domain::ClassA.properties[name='prop2'].genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getDescription());

        Assertions.assertEquals(
                "::",
                GraphPath.buildPath("::").getDescription());
        Assertions.assertEquals(
                "::.children['test']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assertions.assertEquals(
                "::.children['test'].children['domain']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assertions.assertEquals(
                "::.children['test'].children['domain'].children['ClassA']",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

        Assertions.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getDescription());
        Assertions.assertEquals(
                "Root.children['test']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getDescription());
        Assertions.assertEquals(
                "Root.children['test'].children['domain']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getDescription());
        Assertions.assertEquals(
                "Root.children['test'].children['domain'].children['ClassA']",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getDescription());

        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getDescription());
        Assertions.assertEquals(
                "Integer.generalizations",
                GraphPath.buildPath("Integer", "generalizations").getDescription());
        Assertions.assertEquals(
                "Integer.generalizations.general",
                GraphPath.buildPath("Integer", "generalizations", "general").getDescription());
        Assertions.assertEquals(
                "Integer.generalizations.general.rawType",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getDescription());

        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getDescription());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getDescription());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getDescription());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getDescription());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits['RomanLength~Actus']",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getDescription());
    }

    @Test
    public void testGetPureExpression()
    {
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getPureExpression());
        Assertions.assertEquals(
                "test::domain::ClassA.classifierGenericType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getPureExpression());
        Assertions.assertEquals(
                "test::domain::ClassA.classifierGenericType.rawType",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getPureExpression());
        Assertions.assertEquals(
                "test::domain::ClassA.properties->at(0).genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assertions.assertEquals(
                "test::domain::ClassA.properties->get('prop2')->toOne().genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());
        Assertions.assertEquals(
                "test::domain::ClassA.properties->filter(x | $x.name == 'prop2')->toOne().genericType.rawType",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getPureExpression());

        Assertions.assertEquals(
                "::",
                GraphPath.buildPath("::").getPureExpression());
        Assertions.assertEquals(
                "::.children->get('test')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assertions.assertEquals(
                "::.children->get('test')->toOne().children->get('domain')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assertions.assertEquals(
                "::.children->get('test')->toOne().children->get('domain')->toOne().children->get('ClassA')->toOne()",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

        Assertions.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getPureExpression());
        Assertions.assertEquals(
                "Root.children->get('test')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getPureExpression());
        Assertions.assertEquals(
                "Root.children->get('test')->toOne().children->get('domain')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getPureExpression());
        Assertions.assertEquals(
                "Root.children->get('test')->toOne().children->get('domain')->toOne().children->get('ClassA')->toOne()",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getPureExpression());

        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getPureExpression());
        Assertions.assertEquals(
                "Integer.generalizations",
                GraphPath.buildPath("Integer", "generalizations").getPureExpression());
        Assertions.assertEquals(
                "Integer.generalizations.general",
                GraphPath.buildPath("Integer", "generalizations", "general").getPureExpression());
        Assertions.assertEquals(
                "Integer.generalizations.general.rawType",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getPureExpression());

        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getPureExpression());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getPureExpression());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getPureExpression());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getPureExpression());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits->get('RomanLength~Actus')->toOne()",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getPureExpression());
    }

    @Test
    public void testGetStartNodePath()
    {
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());
        Assertions.assertEquals(
                "test::domain::ClassA",
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().getStartNodePath());

        Assertions.assertEquals(
                "::",
                GraphPath.buildPath("::").getStartNodePath());
        Assertions.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assertions.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assertions.assertEquals(
                "::",
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

        Assertions.assertEquals(
                "Root",
                GraphPath.buildPath("Root").getStartNodePath());
        Assertions.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().getStartNodePath());
        Assertions.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().getStartNodePath());
        Assertions.assertEquals(
                "Root",
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().getStartNodePath());

        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer").getStartNodePath());
        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations").getStartNodePath());
        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations", "general").getStartNodePath());
        Assertions.assertEquals(
                "Integer",
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").getStartNodePath());

        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").getStartNodePath());
        Assertions.assertEquals(
                "test::domain::RomanLength~Cubitum",
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().getStartNodePath());
    }

    @Test
    public void testResolve()
    {
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueForMetaPropertyToOne("classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance(M3Paths.Class),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                ImportStub.withImportStubByPass(GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType").build().resolve(processorSupport), processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassB"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").build().resolve(processorSupport));

        Assertions.assertSame(
                runtime.getCoreInstance("::"),
                GraphPath.buildPath("::").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Root"),
                GraphPath.buildPath("Root").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().resolve(processorSupport));

        Assertions.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer").resolve(processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolve(processorSupport));

        Assertions.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").resolve(processorSupport));
        Assertions.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").resolve(processorSupport));
        Assertions.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Pes"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").resolve(processorSupport));
        Assertions.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").resolve(processorSupport));
        Assertions.assertEquals(
                runtime.getCoreInstance("test::domain::RomanLength~Actus"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().resolve(processorSupport));
    }

    @Test
    public void testResolveUpTo()
    {
        Assertions.assertSame(
                runtime.getCoreInstance(M3Paths.String),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueAtIndex("properties", 0).addToOneProperties("genericType", "rawType").build().resolve(processorSupport));

        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(0, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-3, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(1, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-2, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(2, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-1, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("test::domain::ClassA").getValueInValueForMetaPropertyToMany("properties", "prop2").getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(3, processorSupport));
        Assertions.assertEquals(
                "Index: 4; size: 3",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(4, processorSupport)).getMessage());
        Assertions.assertEquals(
                "Index: -4; size: 3",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithName("properties", "prop2").addToOneProperties("genericType", "rawType").build().resolveUpTo(-4, processorSupport)).getMessage());


        Assertions.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(0, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Integer"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-3, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(1, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-2, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(2, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Integer").getValueForMetaPropertyToOne("generalizations").getValueForMetaPropertyToOne("general"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-1, processorSupport));
        Assertions.assertSame(
                runtime.getCoreInstance("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(3, processorSupport));
        Assertions.assertEquals(
                "Index: 4; size: 3",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(4, processorSupport)).getMessage());
        Assertions.assertEquals(
                "Index: -4; size: 3",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("Integer", "generalizations", "general", "rawType").resolveUpTo(-4, processorSupport)).getMessage());
    }

    @Test
    public void testSubpath()
    {
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(0));
        Assertions.assertEquals(
                "Index: 1; size: 0",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(1)).getMessage());
        Assertions.assertEquals(
                "Index: -1; size: 0",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum").subpath(-1)).getMessage());

        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(0));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(-1));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(1));
        Assertions.assertEquals(
                "Index: 2; size: 1",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(2)).getMessage());
        Assertions.assertEquals(
                "Index: -2; size: 1",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").subpath(-2)).getMessage());

        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(0));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-4));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(1));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-3));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(2));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-2));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(3));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-1));
        Assertions.assertEquals(
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build(),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(4));
        Assertions.assertEquals(
                "Index: 5; size: 4",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(5)).getMessage());
        Assertions.assertEquals(
                "Index: -5; size: 4",
                Assertions.assertThrows(
                        IndexOutOfBoundsException.class,
                        () -> GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().subpath(-5)).getMessage());
    }

    @Test
    public void testReduce()
    {
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath(M3Paths.Class),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath(M3Paths.Class, "name"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType", "rawType", "name").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.newPathBuilder("test::domain::ClassB").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build().reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath(M3Paths.Class, "name"),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode", "classifierGenericType", "rawType", "name").build().reduce(processorSupport));

        Assertions.assertEquals(
                GraphPath.buildPath("::"),
                GraphPath.buildPath("::").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.newPathBuilder("::").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

        Assertions.assertEquals(
                GraphPath.buildPath("Root"),
                GraphPath.buildPath("Root").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").build().reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").build().reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.newPathBuilder("Root").addToManyPropertyValueWithName("children", "test").addToManyPropertyValueWithName("children", "domain").addToManyPropertyValueWithName("children", "ClassA").build().reduce(processorSupport));

        Assertions.assertEquals(
                GraphPath.buildPath("Integer"),
                GraphPath.buildPath("Integer").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("Integer", "generalizations"),
                GraphPath.buildPath("Integer", "generalizations").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("Integer", "generalizations", "general"),
                GraphPath.buildPath("Integer", "generalizations", "general").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("Number"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("Number", "name"),
                GraphPath.buildPath("Integer", "generalizations", "general", "rawType", "name").reduce(processorSupport));

        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Cubitum"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Pes"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength"),
                GraphPath.buildPath("test::domain::RomanLength~Cubitum", "measure", "canonicalUnit", "measure").reduce(processorSupport));
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::RomanLength~Actus"),
                GraphPath.newPathBuilder("test::domain::RomanLength~Cubitum").addToOneProperties("measure", "canonicalUnit", "measure").addToManyPropertyValueWithName("nonCanonicalUnits", "RomanLength~Actus").build().reduce(processorSupport));
    }

    @Test
    public void testEquals()
    {
        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassA"));
        Assertions.assertNotEquals(
                GraphPath.buildPath("test::domain::ClassA"),
                GraphPath.buildPath("test::domain::ClassB"));

        Assertions.assertEquals(
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"),
                GraphPath.buildPath("test::domain::ClassA", "classifierGenericType"));
        Assertions.assertEquals(
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build(),
                GraphPath.newPathBuilder("test::domain::ClassA").addToManyPropertyValueWithKey("properties", "name", "prop2").addToOneProperties("genericType", "rawType", "resolvedNode").addToManyPropertyValueWithKey("properties", "name", "prop3").build());
    }

    @Test
    public void testParse()
    {
        ListIterable<String> descriptions = Lists.immutable.with(
                "test::domain::ClassA",
                "test::domain::ClassB",
                "test::domain::ClassA.properties[0].genericType.rawType",
                "test::domain::ClassA.properties['prop2'].genericType.rawType",
                "test::domain::ClassA.properties[name='prop2'].genericType.rawType",
                "::",
                "::.children['test']",
                "::.children['test'].children['domain']",
                "::.children['test'].children['domain'].children['ClassA']",
                "Root",
                "Root.children['test']",
                "Root.children['test'].children['domain']",
                "Root.children['test'].children['domain'].children['ClassA']",
                "Integer",
                "Integer.generalizations",
                "Integer.generalizations.general",
                "Integer.generalizations.general.rawType",
                "test::domain::RomanLength~Cubitum",
                "test::domain::RomanLength~Cubitum.measure",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure",
                "test::domain::RomanLength~Cubitum.measure.canonicalUnit.measure.nonCanonicalUnits['RomanLength~Actus']");
        for (String description : descriptions)
        {
            Assertions.assertEquals(description, GraphPath.parseDescription(description).getDescription());
        }
    }
}
