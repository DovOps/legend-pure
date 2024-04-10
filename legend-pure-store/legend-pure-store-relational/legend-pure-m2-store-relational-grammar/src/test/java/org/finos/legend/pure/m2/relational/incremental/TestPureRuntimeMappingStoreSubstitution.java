// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.incremental;

import java.util.regex.Pattern;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeMappingStoreSubstitution extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final int TEST_COUNT = 10;

    private static final String MODEL_SOURCE_ID = "model.pure";
    private static final String PERSON_STORE_SOURCE_ID = "personStore.pure";
    private static final String FIRM_STORE_SOURCE_ID = "firmStore.pure";
    private static final String FULL_STORE_SOURCE_ID = "fullStore.pure";
    private static final String PERSON_MAPPING_SOURCE_ID = "personMapping.pure";
    private static final String FIRM_MAPPING_SOURCE_ID = "firmMapping.pure";
    private static final String FULL_MAPPING_SOURCE_ID = "fullMapping.pure";

    private static final ImmutableSet<String> MODEL_SOURCES = Sets.immutable.with(MODEL_SOURCE_ID);
    private static final ImmutableSet<String> STORE_SOURCES = Sets.immutable.with(PERSON_STORE_SOURCE_ID, FIRM_STORE_SOURCE_ID, FULL_STORE_SOURCE_ID);
    private static final ImmutableSet<String> MAPPING_SOURCES = Sets.immutable.with(PERSON_MAPPING_SOURCE_ID, FIRM_MAPPING_SOURCE_ID, FULL_MAPPING_SOURCE_ID);
    private static final ImmutableSet<String> ALL_SOURCES = Sets.immutable.withAll(LazyIterate.concatenate(MODEL_SOURCES, STORE_SOURCES, MAPPING_SOURCES));

    private MutableMap<String, String> sources = Maps.mutable.empty();

    @BeforeEach
    public void setUp()
    {
        this.sources.put(MODEL_SOURCE_ID,
                """
                import test::*;
                
                Class test::Person
                {
                   name:String[1];
                }
                
                Class test::Firm
                {
                   name:String[1];
                }
                
                Association test::Employment
                {
                   employer:Firm[0..1];\
                   employees:Person[*];
                }\
                """);
        this.sources.put(PERSON_STORE_SOURCE_ID,
                """
                ###Relational
                Database test::PersonDB
                (
                   Table personTb(name VARCHAR(200), firmId INT)
                )\
                """);
        this.sources.put(FIRM_STORE_SOURCE_ID,
                """
                ###Relational
                Database test::FirmDB
                (
                   Table firmTb(id INT, name VARCHAR(200))
                )\
                """);
        this.sources.put(FULL_STORE_SOURCE_ID,
                """
                ###Relational
                Database test::FullDB
                (
                   include test::PersonDB
                   include test::FirmDB
                   Join Person_Firm(personTb.firmId = firmTb.id)
                )\
                """);
        this.sources.put(PERSON_MAPPING_SOURCE_ID,
                """
                ###Mapping
                import test::*;
                
                Mapping test::PersonMapping
                (
                  Person[person] : Relational
                           {
                              name:[PersonDB]personTb.name
                           }
                )\
                """);
        this.sources.put(FIRM_MAPPING_SOURCE_ID,
                """
                ###Mapping
                import test::*;
                
                Mapping test::FirmMapping
                (
                  Firm[firm] : Relational
                           {
                              name:[FirmDB]firmTb.name
                           }
                )\
                """);
        this.sources.put(FULL_MAPPING_SOURCE_ID,
                """
                ###Mapping
                import test::*;
                
                Mapping test::FullMapping
                (
                   include PersonMapping[PersonDB -> FullDB]
                   include FirmMapping[FirmDB -> FullDB]
                   Employment : Relational
                                {
                                   AssociationMapping
                                   (
                                      employer[person,firm] : [FullDB]@Person_Firm,
                                      employees[firm,person] : [FullDB]@Person_Firm
                                   )
                                }
                )\
                """);
    }

    @Test
    public void testCompileMappings()
    {
        compileSources(STORE_SOURCES.union(MODEL_SOURCES));
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            compileSources(MAPPING_SOURCES);
            Assertions.assertNotNull(this.runtime.getCoreInstance("test::PersonMapping"));
            Assertions.assertNotNull(this.runtime.getCoreInstance("test::FirmMapping"));
            Assertions.assertNotNull(this.runtime.getCoreInstance("test::FullMapping"));
            deleteSources(MAPPING_SOURCES);
            Assertions.assertNull(this.runtime.getCoreInstance("test::PersonMapping"));
            Assertions.assertNull(this.runtime.getCoreInstance("test::FirmMapping"));
            Assertions.assertNull(this.runtime.getCoreInstance("test::FullMapping"));
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testCompileFullMapping()
    {
        compileSources(ALL_SOURCES.newWithout(FULL_MAPPING_SOURCE_ID));
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            compileSource(FULL_MAPPING_SOURCE_ID);
            Assertions.assertNotNull(this.runtime.getCoreInstance("test::FullMapping"));
            deleteSource(FULL_MAPPING_SOURCE_ID);
            Assertions.assertNull(this.runtime.getCoreInstance("test::FullMapping"));
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeleteModel()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(MODEL_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, Pattern.compile("(test::)?(Person|Firm|Employment) has not been defined!"), e);
            }
            compileSource(MODEL_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeletePersonStore()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(PERSON_STORE_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, Pattern.compile("(test::)?PersonDB has not been defined!"), e);
            }
            compileSource(PERSON_STORE_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeleteFirmStore()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(FIRM_STORE_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, Pattern.compile("(test::)?FirmDB has not been defined!"), e);
            }
            compileSource(FIRM_STORE_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeleteFullStore()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(FULL_STORE_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "FullDB has not been defined!", FULL_MAPPING_SOURCE_ID, 6, 38, e);
            }
            compileSource(FULL_STORE_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeletePersonMapping()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(PERSON_MAPPING_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "PersonMapping has not been defined!", FULL_MAPPING_SOURCE_ID, 6, 12, e);
            }
            compileSource(PERSON_MAPPING_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeleteFirmMapping()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            try
            {
                deleteSource(FIRM_MAPPING_SOURCE_ID);
                Assertions.fail("Expected compilation exception");
            }
            catch (Exception e)
            {
                assertPureException(PureCompilationException.class, "FirmMapping has not been defined!", FULL_MAPPING_SOURCE_ID, 7, 12, e);
            }
            compileSource(FIRM_MAPPING_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    @Test
    public void testDeleteFullMapping()
    {
        compileAllSources();
        int expectedSize = this.repository.serialize().length;

        for (int i = 0; i < TEST_COUNT; i++)
        {
            deleteSource(FULL_MAPPING_SOURCE_ID);
            compileSource(FULL_MAPPING_SOURCE_ID);
            Assertions.assertEquals(expectedSize, this.repository.serialize().length, "Failed on iteration #" + i);
        }
    }

    private void compileAllSources()
    {
        this.runtime.createInMemoryAndCompile(this.sources);
    }

    private void compileSources(Iterable<String> sourceIds)
    {
        ImmutableSet<String> sourceIdSet = Sets.immutable.withAll(sourceIds);
        Predicate<Pair<String, String>> isSourcePair = Predicates.attributePredicate(Functions.<String>firstOfPair(), Predicates.in(sourceIdSet));
        this.runtime.createInMemoryAndCompile(this.sources.keyValuesView().select(isSourcePair));
    }

    private void compileSource(String sourceId)
    {
        this.runtime.createInMemoryAndCompile(Tuples.pair(sourceId, this.sources.get(sourceId)));
    }

    private void deleteSources(Iterable<String> sourceIds)
    {
        for (String sourceId : sourceIds)
        {
            this.runtime.delete(sourceId);
        }
        this.runtime.compile();
    }

    private void deleteSource(String sourceId)
    {
        this.runtime.delete(sourceId);
        this.runtime.compile();
    }

}
