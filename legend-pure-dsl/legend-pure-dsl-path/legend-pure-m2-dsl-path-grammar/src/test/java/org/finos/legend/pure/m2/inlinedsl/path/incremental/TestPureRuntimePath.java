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

package org.finos.legend.pure.m2.inlinedsl.path.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimePath extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceIdA.pure");
        runtime.delete("sourceIdB.pure");
        runtime.delete("function.pure");
        runtime.delete("userId.pure");
    }

    @Test
    public void testPureRuntimeProperty()
    {
        String source = "Class XA{version : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#/XA/version#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "XA has not been defined!", "userId.pure", 1, 35);
    }

    @Test
    public void testPureRuntimePropertyChange()
    {
        String source = "Class A{version : Integer[1];}";
        String inValidSource = "Class A{vers : Integer[1];}";
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#/A/version#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(sourceId, inValidSource)), "The property 'version' can't be found in the type 'A' (or any supertype).",
                "userId.pure", 1, 38);

    }

    @Test
    public void testPureRuntimeWithQualifiedPropertyWithEnum()
    {
        String source = """
                Class EntityWithLocations
                {
                    locations : Location[*];
                    locationsByType(types:GeographicEntityType[*])
                    {
                        $this.locations->filter(l | $types->filter(type | is($l.type, $type))->isNotEmpty())
                    }:Location[*];
                }
                Class Location
                {
                    type : GeographicEntityType[1];
                }
                Enum GeographicEntityType
                {
                    CITY,
                    COUNTRY
                }\
                """;
        String sourceId = "sourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#/EntityWithLocations/locationsByType(GeographicEntityType.CITY)#,0);true;}");
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceId, source)), "EntityWithLocations has not been defined!", "userId.pure", 1, 35);
    }

    @Test
    public void testPureRuntimeWithQualifiedPropertyWithEnumCompileError()
    {
        String source = """
                Class EntityWithLocations
                {
                    locations : Location[*];
                    locationsByType(types:GeographicEntityType[*])
                    {
                        $this.locations->filter(l | $types->filter(type | is($l.type, $type))->isNotEmpty())
                    }:Location[*];
                }
                Class Location
                {
                    type : GeographicEntityType[1];
                }
                """;

        String enumSource = """
                Enum GeographicEntityType
                {
                    CITY,
                    COUNTRY
                }\
                """;
        String sourceId = "sourceId.pure";
        String enumSourceId = "enumSourceId.pure";
        this.runtime.createInMemorySource(sourceId, source);
        this.runtime.createInMemorySource(enumSourceId, enumSource);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{print(#/EntityWithLocations/locationsByType(GeographicEntityType.CITY)#,0);true;}");
        this.runtime.compile();

        String enumSourceChange = """
                Enum GeographicEntityType
                {
                    CTIY,
                    COUNTRY
                }\
                """;

        RuntimeVerifier.replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(this.runtime,
                Lists.fixedSize.of(Tuples.pair(enumSourceId, enumSourceChange)), "The enum value 'CITY' can't be found in the enumeration GeographicEntityType", "userId.pure", 1, 72);
    }

    @Test
    public void testPathUnbindStabilityForMilestonedQualifiedProperties()
    {
        String sourceA = "Class <<temporal.businesstemporal>> A{}";
        String sourceB = """
                Class <<temporal.businesstemporal>> B{bAttr:String[0..1];}
                Association AB{a:A[0..1]; b:B[0..1];}\
                """;

        String function = """
                function go():Any[*]
                {\
                  {|A.all(%2015)->filter(a|!$a->isEmpty())->project([#/A/b/bAttr#])}\
                }\
                function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]
                {[]}
                """;

        String sourceIdB = "sourceIdB.pure";
        this.runtime.createInMemorySource("sourceIdA.pure", sourceA);
        this.runtime.createInMemorySource(sourceIdB, sourceB);
        this.runtime.createInMemorySource("function.pure", function);
        this.runtime.compile();

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(this.runtime, this.functionExecution,
                Lists.fixedSize.of(Tuples.pair(sourceIdB, sourceB)), "The property 'b' can't be found in the type 'A' (or any supertype).", "function.pure", 2, 59);

    }
}
