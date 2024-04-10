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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.MappingParser;
import org.finos.legend.pure.m2.relational.serialization.grammar.v1.RelationalParser;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.Loader;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestExtendGrammar extends AbstractPureRelationalTestWithCoreCompiled
{
    private RelationalGraphWalker graphWalker;

    @BeforeEach
    public void setUpRelational()
    {
        this.graphWalker = new RelationalGraphWalker(runtime, processorSupport);
    }

    @Test
    public void testExtend()
    {
        Loader.parseM3(
                """
import other::*;

Class other::Person
{
    name:String[1];
    otherInfo:String[1];
}
###Relational
Database mapping::db(
   Table employeeTable
   (
    id INT PRIMARY KEY,
    name VARCHAR(200),
    firm VARCHAR(200),
    otherInfo VARCHAR(200),
    postcode VARCHAR(10)
   )
)
###Mapping
import other::*;
import mapping::*;
Mapping mappingPackage::myMapping
(
    *Person[superClass]: Relational
    {
       name : [db]employeeTable.name\s
    }
    Person[p_subclass] extends [superClass]: Relational
    {
       otherInfo: [db]employeeTable.otherInfo
    }
)
""", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMappingById(mapping, "superClass");
        Assertions.assertNotNull(personMapping);
        Assertions.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assertions.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assertions.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance personSubMapping = this.graphWalker.getClassMappingById(mapping, "p_subclass");
        Assertions.assertNotNull(personSubMapping);
        Assertions.assertFalse(PrimitiveUtilities.getBooleanValue(personSubMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assertions.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personSubMapping)));
        Assertions.assertEquals(1, this.graphWalker.getClassMappingImplementationPropertyMappings(personSubMapping).size());
        Assertions.assertEquals("superClass", personSubMapping.getValueForMetaPropertyToOne(M2MappingProperties.superSetImplementationId).getName());
    }


    @Test
    public void testExtendEmptySubtype()
    {
        Loader.parseM3(
                """
import other::*;

Class other::Person
{
    name:String[1];
    otherInfo:String[1];
}
Class other::MyPerson extends other::Person
{
}
###Relational
Database mapping::db(
   Table employeeTable
   (
    id INT PRIMARY KEY,
    name VARCHAR(200),
    otherInfo VARCHAR(200)
   )
)
###Mapping
import other::*;
import mapping::*;
Mapping mappingPackage::myMapping
(
    *Person[superClass]: Relational
    {
       name : [db]employeeTable.name,
       otherInfo: [db]employeeTable.otherInfo
    }
    MyPerson[p_subclass] extends [superClass]: Relational
    {
    }
)
""", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


        CoreInstance mapping = this.graphWalker.getMapping("mappingPackage::myMapping");
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(2, this.graphWalker.getClassMappings(mapping).size());

        CoreInstance personMapping = this.graphWalker.getClassMappingById(mapping, "superClass");
        Assertions.assertNotNull(personMapping);
        Assertions.assertTrue(PrimitiveUtilities.getBooleanValue(personMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assertions.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personMapping)));
        Assertions.assertEquals(2, this.graphWalker.getClassMappingImplementationPropertyMappings(personMapping).size());

        CoreInstance personSubMapping = this.graphWalker.getClassMappingById(mapping, "p_subclass");
        Assertions.assertNotNull(personSubMapping);
        Assertions.assertTrue(PrimitiveUtilities.getBooleanValue(personSubMapping.getValueForMetaPropertyToOne(M3Properties.root)));
        Assertions.assertEquals("employeeTable", this.graphWalker.getName(this.graphWalker.getClassMappingImplementationMainTable(personSubMapping)));
        Assertions.assertEquals(0, this.graphWalker.getClassMappingImplementationPropertyMappings(personSubMapping).size());
        Assertions.assertEquals("superClass", personSubMapping.getValueForMetaPropertyToOne(M2MappingProperties.superSetImplementationId).getName());

    }


    @Test
    public void testInvalidExtendSubtype()
    {
        try
        {
            Loader.parseM3(
                    """
import other::*;

Class other::Person
{
    name:String[1];
    otherInfo:String[1];
}
Class other::MyPerson extends other::Person
{
    title:String[1];
}
###Relational
Database mapping::db(
   Table employeeTable
   (
    id INT PRIMARY KEY,
    name VARCHAR(200),
    otherInfo VARCHAR(200)
   )
)
###Mapping
import other::*;
import mapping::*;
Mapping mappingPackage::myMapping
(
    *Person[superClass]: Relational
    {
       name : [db]employeeTable.name,
       otherInfo: [db]employeeTable.otherInfo
    }
    MyPerson[p_subclass] extends [superClass]: Relational
    {
    }
)
""", repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid extends mapping. Class [MyPerson] has properties of it's own. Extends mappings are only allowed for subtypes when the subtype has no simple properties of it's own", "fromString.pure", 31, 5, e);
        }
    }

    @Test
    public void testExtendInvalidSet()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                        *Person[superClass]: Relational
                        {
                           name : [db]employeeTable.name\s
                        }
                        Person[p_subclass] extends [badId]: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
            Assertions.fail(" this should not compile!");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 28, 5, e);
        }

    }

    @Test
    public void testExtendWithInclude()
    {
        Loader.parseM3(
                """
                import other::*;
                
                Class other::Person
                {
                    name:String[1];
                    otherInfo:String[1];
                }
                ###Relational
                Database mapping::db(
                   Table employeeTable
                   (
                    id INT PRIMARY KEY,
                    name VARCHAR(200),
                    firm VARCHAR(200),
                    otherInfo VARCHAR(200),
                    postcode VARCHAR(10)
                   )
                )
                ###Mapping
                import other::*;
                import mapping::*;
                Mapping mappingPackage::myMapping
                (
                    *Person[superClass]: Relational
                    {
                       name : [db]employeeTable.name\s
                    }
                )
                Mapping mappingPackage::myMappingWithIncludes
                (
                    include mappingPackage::myMapping
                    Person[p_subclass] extends [superClass]: Relational
                    {
                       otherInfo: [db]employeeTable.otherInfo
                    }
                )
                """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
        runtime.compile();


    }

    @Test
    public void testExtendInvalidIdWithInclude()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                        *Person[superClass]: Relational
                        {
                           name : [db]employeeTable.name\s
                        }
                    )
                    Mapping mappingPackage::myMappingWithIncludes
                    (
                        include mappingPackage::myMapping
                        Person[p_subclass] extends [badId]: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 32, 5, e);
        }

    }

    @Test
    public void testExtendInvalidSetCannotBeSelf()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                        Person[p_subclass] extends [p_subclass]: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
            Assertions.fail("this should not compile");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Extend mapping id cannot reference self 'p_subclass'", "fromString.pure", 24, 5, e);

        }

    }


    @Test
    public void testExtendInvalidSetCannotBeEmpty()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                        Person[p_subclass] extends []: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
            Assertions.fail("this should not parse");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: a valid identifier text; found: ']'", "fromString.pure", 24, 33, e);

        }

    }

    @Test
    public void testExtendInvalidSetCannotBeDifferentMappingTypes()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                       *Person[pure]: Pure
                        {
                           name :'Test'
                        }
                        Person[p_subclass] extends [pure]: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();

            Assertions.fail();


        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Invalid superMapping for mapping [p_subclass]", "fromString.pure", 28, 5, e);

        }

    }

    @Test
    public void testExtendInvalidSetCannotBeDifferentClassTypes()
    {
        try
        {
            Loader.parseM3(
                    """
                    import other::*;
                    
                    Class other::Person
                    {
                        name:String[1];
                        otherInfo:String[1];
                    }
                    Class other::Firm
                    {
                        legalName:String[1];
                        otherInformation:String[1];
                    }
                    ###Relational
                    Database mapping::db(
                       Table employeeTable
                       (
                        id INT PRIMARY KEY,
                        name VARCHAR(200),
                        firm VARCHAR(200),
                        otherInfo VARCHAR(200),
                        postcode VARCHAR(10)
                       )
                    )
                    ###Mapping
                    import other::*;
                    import mapping::*;
                    Mapping mappingPackage::myMapping
                    (
                        Firm[f1]: Relational
                        {
                           legalName : [db]employeeTable.firm\s
                        }
                        Person[p_subclass] extends [f1]: Relational
                        {
                           otherInfo: [db]employeeTable.otherInfo
                        }
                    )
                    """, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(), new MappingParser(), new RelationalParser())), ValidationType.DEEP, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context);
            runtime.compile();
            Assertions.fail("this should not compile");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Class [Person] != [Firm], when [p_subclass] extends [ 'f1'] they must map the same class", "fromString.pure", 33, 5, e);
        }
   }
}