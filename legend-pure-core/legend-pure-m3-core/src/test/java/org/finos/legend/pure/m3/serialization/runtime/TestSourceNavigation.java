// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSourceNavigation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeEach
    public void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testNavigateForPropertyReference()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                """
                Class test::ParentNode
                {
                    children: test::ChildNode[*];
                }
                
                Class test::ChildNode
                {
                    id: String[1];
                }
                
                Class test::NodeInfo {
                    name: String[1];
                    alias: String[1];
                }
                
                Class test::AdvChildNode extends test::ChildNode
                {
                    info: test::NodeInfo[1];
                }
                
                function test::getChildNodeName(node:test::ParentNode[0..1]):String[1]
                {
                  if($node.children->isEmpty(),|'',|$node->match([
                    a:test::AdvChildNode[1]| $a.info.name + '(' + $a.info.alias + ')',
                    j:test::ChildNode[1]| 'generic'
                  ]));
                }\
                """
        );
        runtime.compile();

        CoreInstance found = source.navigate(24, 35, processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("info", ((Property<?, ?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(18, found.getSourceInformation().getLine());
        Assertions.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 41, processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("name", ((Property<?, ?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(12, found.getSourceInformation().getLine());
        Assertions.assertEquals(5, found.getSourceInformation().getColumn());

        found = source.navigate(24, 63, processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("alias", ((Property<?, ?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(13, found.getSourceInformation().getLine());
        Assertions.assertEquals(5, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateForClassOrAssociationProperty()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                """
                Class model::C1 {
                }
                
                Class model::C2 {
                  name: String[1];
                  al(){''}: String[1]; \s
                }
                
                Association model::Assoc
                {
                  prop3: model::C2[1];
                  prop2: model::C1[1];
                }\
                """
        );
        runtime.compile();

        CoreInstance found = source.navigate(5, 3, processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("name", ((Property<?, ?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(5, found.getSourceInformation().getLine());
        Assertions.assertEquals(3, found.getSourceInformation().getColumn());

        found = source.navigate(6, 3, processorSupport);
        Assertions.assertTrue(found instanceof QualifiedProperty);
        Assertions.assertEquals("al", ((QualifiedProperty<?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(6, found.getSourceInformation().getLine());
        Assertions.assertEquals(3, found.getSourceInformation().getColumn());

        found = source.navigate(11, 3, processorSupport);
        Assertions.assertTrue(found instanceof Property);
        Assertions.assertEquals("prop3", ((Property<?, ?>) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(11, found.getSourceInformation().getLine());
        Assertions.assertEquals(3, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateEnumValue()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                """
                Enum model::TestEnum {
                  VAL1,
                  VAL2
                }
                
                function doSomething(): Any[*]
                {
                  model::TestEnum.VAL1->toString();
                }\
                """
        );
        runtime.compile();

        // parameter
        CoreInstance found = source.navigate(8, 20, processorSupport);
        Assertions.assertTrue(found instanceof EnumInstance);
        Assertions.assertEquals("VAL1", ((EnumInstance) found)._name());
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(2, found.getSourceInformation().getLine());
        Assertions.assertEquals(3, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateVariableAndParameter()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                """
                function doSomething(param: String[1]): Any[*]
                {
                  let var = 1;
                  $var->toString();
                  let var_lambda1 = var: String[1]|$var->toString();
                  let var_lambda2 = {x: String[1]| let var = 1; $var->toString();};
                  let var_lambda3 = {x: String[1]| $var->toString();};
                  $param->toString();
                  let param_lambda1 = param: String[1]|$param->toString();
                  let param_lambda2 = {x: String[1]| let param = 1; $param->toString();};
                  let param_lambda3 = {x: String[1]| $param->toString();};
                  let param_lambda4 = {x: String[1]| [1,2,3]->map(y|$param->toString());};
                }\
                """
        );

        runtime.compile();
        CoreInstance found = source.navigate(3, 7, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(3, found.getSourceInformation().getLine());
        Assertions.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(4, 4, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(3, found.getSourceInformation().getLine());
        Assertions.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(5, 21, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(5, found.getSourceInformation().getLine());
        Assertions.assertEquals(21, found.getSourceInformation().getColumn());

        found = source.navigate(5, 37, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(5, found.getSourceInformation().getLine());
        Assertions.assertEquals(21, found.getSourceInformation().getColumn());

        found = source.navigate(6, 40, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(6, found.getSourceInformation().getLine());
        Assertions.assertEquals(40, found.getSourceInformation().getColumn());

        found = source.navigate(6, 50, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(6, found.getSourceInformation().getLine());
        Assertions.assertEquals(40, found.getSourceInformation().getColumn());

        found = source.navigate(7, 37, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(3, found.getSourceInformation().getLine());
        Assertions.assertEquals(7, found.getSourceInformation().getColumn());

        found = source.navigate(8, 4, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(1, found.getSourceInformation().getLine());
        Assertions.assertEquals(22, found.getSourceInformation().getColumn());

        found = source.navigate(9, 23, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(9, found.getSourceInformation().getLine());
        Assertions.assertEquals(23, found.getSourceInformation().getColumn());

        found = source.navigate(9, 41, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(9, found.getSourceInformation().getLine());
        Assertions.assertEquals(23, found.getSourceInformation().getColumn());

        found = source.navigate(10, 42, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(10, found.getSourceInformation().getLine());
        Assertions.assertEquals(42, found.getSourceInformation().getColumn());

        found = source.navigate(10, 54, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(10, found.getSourceInformation().getLine());
        Assertions.assertEquals(42, found.getSourceInformation().getColumn());

        found = source.navigate(11, 39, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(1, found.getSourceInformation().getLine());
        Assertions.assertEquals(22, found.getSourceInformation().getColumn());

        found = source.navigate(12, 54, processorSupport);
        Assertions.assertEquals("test.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(1, found.getSourceInformation().getLine());
        Assertions.assertEquals(22, found.getSourceInformation().getColumn());
    }

    @Test
    public void testNavigateFunctionDescriptor()
    {
        Source source = runtime.createInMemorySource(
                "test.pure",
                """
                function doSomething(param: String[1]): Any[*]
                {
                  [
                    print_Any_MANY__Integer_1__Nil_0_
                  ];
                  print_Any_MANY__Integer_1__Nil_0_;
                }\
                """
        );

        runtime.compile();
        CoreInstance found = source.navigate(4, 7, processorSupport);
        Assertions.assertEquals("/platform/pure/basics/io/print.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(15, found.getSourceInformation().getLine());
        Assertions.assertEquals(44, found.getSourceInformation().getColumn());

        found = source.navigate(6, 7, processorSupport);
        Assertions.assertEquals("/platform/pure/basics/io/print.pure", found.getSourceInformation().getSourceId());
        Assertions.assertEquals(15, found.getSourceInformation().getLine());
        Assertions.assertEquals(44, found.getSourceInformation().getColumn());
    }
}
