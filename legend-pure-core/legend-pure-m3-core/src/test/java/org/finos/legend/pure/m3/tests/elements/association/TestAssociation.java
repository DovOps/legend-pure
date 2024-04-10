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

package org.finos.legend.pure.m3.tests.elements.association;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.jupiter.api.*;

public class TestAssociation extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("fromString2.pure");
    }

    @Test
    public void testAssociationNotEnoughProperties()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Expected 2 properties for association 'ProdSyn', found 1", 6, 13, e);
        }
    }

    @Test
    public void testAssociationTooManyProperties()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       moreSynonyms : Synonym[*];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Expected 2 properties for association 'ProdSyn', found 3", 6, 13, e);
        }
    }

    @Test
    public void testAssociationWithWrongTypes()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : SynonymErr[*];
                    }\
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "SynonymErr has not been defined!", 9, 15, e);
        }
    }

    @Test
    public void testAssociationWithWrongTypesInGeneric()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym<Error>[*];
                    }
                    
                    Class Synonym<T>
                    {
                       name : T[1];
                    }\
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Error has not been defined!", 9, 23, e);
        }
    }

    @Test
    public void testAssociationWithWrongGenericTypeArgs()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                    }
                    
                    Class Synonym<T>
                    {
                       name : T[1];
                    }\
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Type argument mismatch for the class Synonym<T> (expected 1, got 0): Synonym", 9, 15, e);
        }
    }

    @Test
    public void testAssociationWithValidQualifiedPropertyIsProcessedWithoutError()
    {
        compileTestSource("fromString.pure", """
                Class Product
                {
                   name : String[1];
                }
                
                Association ProdSyn
                {
                   product : Product[1];
                   synonyms : Synonym[*];
                   synonymsByName(st:String[1])
                   {
                     $this.synonyms->filter(s|$s.name == $st)->toOne()
                   }: Synonym[1];
                }
                
                Class Synonym
                {
                   name : String[1];
                }\
                """);
    }

    @Test
    public void testAssociationWithInvalidQualifiedPropertySpecificationNoFilter()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       newSynonym(strings:String[*])
                       {
                         ^Synonym();
                       }:Synonym[*];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. Qualified property: 'newSynonym_0' in association: 'ProdSyn'  does not use the 'filter' function", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(10, sourceInfo.getLine());
            Assertions.assertEquals(4, sourceInfo.getColumn());
        }
    }

    @Test
    public void testAssociationWithInvalidQualifiedPropertySpecificationMultipleExpressions()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       synonymsByName(st:String[1])
                       {
                         $this.synonyms->filter(s|$s.name == $st)->toOne(); ^Synonym();
                       }: Synonym[1];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. Qualified property: 'synonymsByName_0' in association: 'ProdSyn'  has more than one Expression Sequence", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(10, sourceInfo.getLine());
            Assertions.assertEquals(4, sourceInfo.getColumn());
        }
    }

    @Test
    public void testAssociationWithQualifiedPropertyWithInvalidReturnType()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                       orderVersions : OrderVersion[*];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       orderVersionById(id:String[1])
                       {
                         $this.orderVersions->filter(o|$o.id == $id)->toOne()
                       }: OrderVersion[1];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }\
                    Class OrderVersion
                    {
                       id : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Qualified property: 'orderVersionById_0' in association: 'ProdSyn' has returnType of : OrderVersion it should be one of Association: 'ProdSyn' properties' return types: [Synonym, Product]", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(11, sourceInfo.getLine());
            Assertions.assertEquals(4, sourceInfo.getColumn());
        }
    }

    @Test
    public void testAssociationWithQualifiedPropertyReturnTypeNotConsistentWithLhsOfFilter()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       synonymsByName(st:String[1])
                       {
                         $this.synonyms->filter(s|$s.name == $st)->map(s|^Product(name=''))
                       }: Product[*];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Qualified property: 'synonymsByName_0' in association: 'ProdSyn' should return a subset of property: 'synonyms' (left side of filter) and consequently should have a returnType of : 'Synonym'", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(10, sourceInfo.getLine());
            Assertions.assertEquals(4, sourceInfo.getColumn());
        }
    }

    @Test
    public void testAssociationWithQualifiedPropertyWhichConflictsWithQualifiedPropertyOnOwningClass()
    {
        try
        {
            compileTestSource("fromString.pure", """
                    Class Product
                    {
                       name : String[1];
                       synonymsByName(st:String[1]){$this.synonyms->filter(s | $s.name == $st)->toOne()}: Synonym[1];\
                    }
                    
                    Association ProdSyn
                    {
                       product : Product[1];
                       synonyms : Synonym[*];
                       synonymsByName(st:String[1])
                       {
                         $this.synonyms->filter(s|$s.name == $st)->toOne();
                       }: Synonym[1];
                    }
                    
                    Class Synonym
                    {
                       name : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            PureException pe = PureException.findPureException(e);
            Assertions.assertNotNull(pe);
            Assertions.assertTrue(pe instanceof PureCompilationException);
            Assertions.assertEquals("Property conflict on class Product: qualified property 'synonymsByName' defined more than once", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assertions.assertNotNull(sourceInfo);
            Assertions.assertEquals(1, sourceInfo.getLine());
            Assertions.assertEquals(7, sourceInfo.getColumn());
        }
    }


    @Test
    @Disabled
    @ToFix
    public void testAssociationWithDuplicatePropertyNames()
    {
        // TODO consider whether we want to allow this case
        try
        {
            compileTestSource("fromString.pure",
                    """
                    Class Class1 {}
                    Class Class2 {}
                    Association Association12
                    {
                        prop:Class1[*];
                        prop:Class2[0..1];
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, "Property conflict on association Association12: property 'prop' defined more than once", "testSource.pure", 3, 1, 3, 13, 7, 1, e);
        }
    }

    @Test
    public void testAssociationWithDuplicatePropertyNamesAndTargetTypes()
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    Class Class1 {}
                    Association Association12
                    {
                        prop:Class1[*];
                        prop:Class1[0..1];
                    }
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property conflict on association Association12: property 'prop' defined more than once with the same target type", "fromString.pure", 2, 1, 2, 13, 6, 1, e);
        }
    }

    @Test
    public void testAssociationWithPropertyNameConflict()
    {
        try
        {
            compileTestSource("fromString.pure",
                    """
                    Class Class1
                    {
                      prop:Class2[*];
                    }
                    Class Class2 {}
                    Association Association12
                    {
                      prop:Class2[*];
                      prop2:Class1[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property conflict on class Class1: property 'prop' defined more than once", "fromString.pure", 1, 1, 1, 7, 4, 1, e);
        }
    }

    @Test
    public void testAssociationWithPropertyNameConflictInOtherSource()
    {
        compileTestSource("fromString.pure",
                """
                Class Class1
                {
                  prop:Class2[*];
                }
                Class Class2 {}
                """);
        try
        {
            compileTestSource("fromString2.pure",
                    """
                    Association Association12
                    {
                      prop:Class2[*];
                      prop2:Class1[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Property conflict on class Class1: property 'prop' defined more than once", "fromString.pure", 1, 1, 1, 7, 4, 1, e);
        }
    }

    @Test
    public void testAssociationWithNonClass()
    {
        compileTestSource("fromString.pure", "Class Class1 {}");
        try
        {
            compileTestSource("fromString2.pure",
                    """
                    Association Association1
                    {
                      prop1 : Class1[1];
                      prop2 : String[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Association 'Association1' can only be applied to Classes; 'String' is not a Class", "fromString2.pure", 1, 1, 1, 13, 5, 1, e);
        }

        try
        {
            compileTestSource("fromString3.pure",
                    """
                    Association Association2
                    {
                      prop1 : Integer[1];
                      prop2 : Class1[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Association 'Association2' can only be applied to Classes; 'Integer' is not a Class", "fromString3.pure", 1, 1, 1, 13, 5, 1, e);
        }

        try
        {
            compileTestSource("fromString4.pure",
                    """
                    Association Association3
                    {
                      prop1 : Integer[1];
                      prop2 : Date[1];
                    }\
                    """);
            Assertions.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Association 'Association3' can only be applied to Classes; 'Date' is not a Class", "fromString4.pure", 1, 1, 1, 13, 5, 1, e);
        }
    }
}
