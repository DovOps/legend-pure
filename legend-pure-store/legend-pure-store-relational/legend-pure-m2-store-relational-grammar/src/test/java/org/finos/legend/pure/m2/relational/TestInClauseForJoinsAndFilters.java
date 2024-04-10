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

package org.finos.legend.pure.m2.relational;

import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestInClauseForJoinsAndFilters extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testSimpleJoin()
    {
        String dbDef = """
                ###Relational
                 \
                Database db
                (
                   Table persontable(firstName VARCHAR(200) PRIMARY KEY, firmId INTEGER)
                   Table firmtable(legalName VARCHAR(200) PRIMARY KEY, id INTEGER)
                   Join firm_person(firmtable.id = persontable.firmId)
                )\
                """;

        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");
        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assertions.assertEquals(2, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());

        Join join = (Join) db.getValueForMetaPropertyToMany(M2RelationalProperties.joins).getFirst();
        Assertions.assertEquals("firm_person", join.getName());
    }

    @Test
    public void testJoinWithIds()
    {
        String dbDef = """
                ###Relational
                 \
                Database db
                (
                   Table persontable(firstName VARCHAR(200) PRIMARY KEY, firmId INTEGER)
                   Table firmtable(legalName VARCHAR(200) PRIMARY KEY, id INTEGER)
                   Join firm_person(firmtable.id = persontable.firmId and firmtable.id = 1 or firmtable.id = 2)
                )\
                """;

        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");
        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assertions.assertEquals(2, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());

        Join join = (Join) db.getValueForMetaPropertyToMany(M2RelationalProperties.joins).getFirst();
        Assertions.assertEquals("firm_person", join.getName());
    }

    @Test
    public void testJoinWithPreixInClause()
    {
        String dbDef = """
                ###Relational
                 \
                Database db
                (
                   Table persontable(firstName VARCHAR(200) PRIMARY KEY, firmId INTEGER)
                   Table firmtable(legalName VARCHAR(200) PRIMARY KEY, id INTEGER)
                   Join firm_personNumber(firmtable.id = persontable.firmId and in(firmtable.id, [1,2]))
                   Join firm_personString(firmtable.id = persontable.firmId and in(firmtable.legalName, ['Google', 'Apple']))
                )\
                """;

        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");
        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assertions.assertEquals(2, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());

        Join firstJoin = (Join) db.getValueForMetaPropertyToMany(M2RelationalProperties.joins).get(0);
        Join secondJoin = (Join) db.getValueForMetaPropertyToMany(M2RelationalProperties.joins).get(1);
        Assertions.assertEquals("firm_personNumber", firstJoin.getName());
        Assertions.assertEquals("firm_personString", secondJoin.getName());
    }

    @Test
    public void testFilterWithIds()
    {
        String dbDef = """
                ###Relational
                 \
                Database db
                (
                   Table persontable(firstName VARCHAR(200) PRIMARY KEY, age INTEGER)
                   Filter young_adults(persontable.age = 18 or persontable.age = 19)
                )\
                """;

        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");
        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());

        Filter filter = (Filter) db.getValueForMetaPropertyToMany(M2RelationalProperties.filters).getFirst();
        Assertions.assertEquals("young_adults", filter.getName());
    }

    @Test
    public void testFilterWithPrefixInClause()
    {
        String dbDef = """
                ###Relational
                 \
                Database db
                (
                   Table persontable(firstName VARCHAR(200) PRIMARY KEY, age INTEGER)
                   Filter young_adults(in(persontable.age, [18, 19]))
                )\
                """;

        this.runtime.createInMemorySource("sourceId.pure", dbDef);
        this.runtime.createInMemorySource("userId.pure", "function test():Boolean[1]{assert(1 == db->meta::relational::metamodel::schema('default').tables->size(), |'');}");
        this.runtime.compile();

        CoreInstance db = processorSupport.package_getByUserPath("db");
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).size());
        Assertions.assertEquals(1, db.getValueForMetaPropertyToMany(M2RelationalProperties.schemas).getFirst().getValueForMetaPropertyToMany(M2RelationalProperties.tables).size());

        Filter filter = (Filter) db.getValueForMetaPropertyToMany(M2RelationalProperties.filters).getFirst();
        Assertions.assertEquals("young_adults", filter.getName());
    }

}
