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

package org.finos.legend.pure.m3.tests.imports;

import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

public class TestImports extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("/platform/pure/graph.pure");
    }

    @Test
    public void testGetImportGroupsForSource()
    {
        String sourceId = "/platform/pure/anonymousCollections.pure";
        Source source = runtime.getSourceById(sourceId);
        Assertions.assertNotNull(source, "Could not find source: " + sourceId);

        ListIterable<? extends CoreInstance> selectedImportGroups = Imports.getImportGroupsForSource(sourceId, processorSupport);
        Verify.assertSize(1, selectedImportGroups);
        Assertions.assertEquals(Source.importForSourceName(sourceId, 1), selectedImportGroups.get(0).getName());
    }

    @Test
    public void testIsImportGroupForSource()
    {
        String sourceId = "/platform/pure/anonymousCollections.pure";
        Source source = runtime.getSourceById(sourceId);
        Assertions.assertNotNull(source, "Could not find source: " + sourceId);

        Predicate2<CoreInstance, String> isImportGroupForSourcePredicate = new Predicate2<CoreInstance, String>()
        {
            @Override
            public boolean accept(CoreInstance importGroup, String sourceId)
            {
                return Imports.isImportGroupForSource(importGroup, sourceId);
            }
        };

        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        ListIterable<? extends CoreInstance> importGroups = imports.getValueForMetaPropertyToMany(M3Properties.children);

        ListIterable<? extends CoreInstance> selectedImportGroups = importGroups.selectWith(isImportGroupForSourcePredicate, sourceId);
        Verify.assertSize(1, selectedImportGroups);
        Assertions.assertEquals(Source.importForSourceName(sourceId, 1), selectedImportGroups.get(0).getName());

        ListIterable<? extends CoreInstance> rejectedImportGroups = importGroups.rejectWith(isImportGroupForSourcePredicate, sourceId);
        Verify.assertNotEmpty(rejectedImportGroups);
        Verify.assertContains((Collection<String>) rejectedImportGroups.collect(CoreInstance::getName), "coreImport");
    }

    @Test
    public void testGetImportGroupBaseName()
    {
        String sourceId = "/platform/pure/anonymousCollections.pure";
        Source source = runtime.getSourceById(sourceId);
        Assertions.assertNotNull(source, "Could not find source: " + sourceId);

        String importGroupName = Source.importForSourceName(sourceId, 1);
        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        CoreInstance importGroup = imports.getValueInValueForMetaPropertyToMany(M3Properties.children, importGroupName);
        Assertions.assertNotNull(importGroup, "Could not find import group: " + importGroupName);

        Assertions.assertEquals(Source.formatForImportGroupId(sourceId), Imports.getImportGroupBaseName(importGroup));
    }
}
