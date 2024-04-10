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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestPureRuntimeXStoreMapping extends AbstractPureMappingTestWithCoreCompiled
{
    public static String model =
            """
            Class Firm
            {
               legalName : String[1];
            }
            
            Class Person
            {
               lastName : String[1];
            }
            
            Association Firm_Person
            {
               firm : Firm[1];
               employees : Person[*];
            }
            """;

    public static String modelInheritanceSuper =
            """
            Class SuperFirm\
            {\
               id : String[1];
            }\
            """;

    public static String modelInheritanceSuper2 =
            """
            Class SuperFirm\
            {\
               id2 : String[1];
            }\
            """;

    public static String modelInheritance =
            """
            Class Firm extends SuperFirm
            {
               legalName : String[1];
            }
            
            Class Person
            {
               lastName : String[1];
            }
            
            Association Firm_Person
            {
               firm : Firm[1];
               employees : Person[*];
            }
            """;

    public static String coreMapping =
            """
               Firm[f1] : Pure
               {
            ~src SrcFirm\
                  +id:String[1] : $src.id,
                  legalName : $src.legalName
               }
              \s
               Person[e] : Pure
               {
            ~src SrcPerson\
                  +firmId:String[1] : $src.firmId,
                  lastName : $src.lastName
               }
            """;

    public static String coreMappingInheritance =
            """
               Firm[f1] : Pure
               {
            ~src SrcFirm\
                  id : $src.id,
                  legalName : $src.legalName
               }
              \s
               Person[e] : Pure
               {
            ~src SrcPerson\
                  +firmId:String[1] : $src.firmId,
                  lastName : $src.lastName
               }
            """;

    public static String assoMapping =
            """
               Firm_Person : XStore
               {
                  firm[e, f1] : $this.firmId == $that.id,
                  employees[f1, e] : $this.id == $that.firmId
               }
            """;


    public static String initialMapping = "###Mapping\nMapping FirmMapping\n(" + coreMapping + ")";

    public static String mappingWithAssociation = "###Mapping\nMapping FirmMapping\n(" + coreMapping + assoMapping + ")\n";

    public static String baseMapping = "###Mapping\nMapping ModelMapping\n(" + coreMapping + ")\n";

    public static String inheritanceMapping = "###Mapping\nMapping ModelMapping\n(" + coreMappingInheritance + assoMapping + ")\n";

    public static String baseMappingEmpty = "###Mapping\nMapping ModelMapping\n()\n";

    public static String mainMapping = "###Mapping\nMapping FirmMapping\n(\ninclude ModelMapping\n" + assoMapping + ")\n";

    public static String relational =
            """
            ###Pure
            Class SrcFirm\
            {\
               id: String[1];\
               legalName : String[1];\
            }
            Class SrcPerson\
            {\
               firmId: String[1];\
               lastName : String[1];\
            }
            """;

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime();
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("source1.pure");
        runtime.delete("source3.pure");
        runtime.delete("source4.pure");
        runtime.delete("source5.pure");
    }

    @Test
    public void testCreateAndDeleteMappingProperties() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model)).compile(),
                new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source3.pure", initialMapping, "source4.pure", relational)).compile().deleteSource("source3.pure").deleteSource("source4.pure").compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", initialMapping, "source4.pure", relational)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", mappingWithAssociation).compile().updateSource("source3.pure", initialMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingWithInclude() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().deleteSource("source5.pure").compile().createInMemorySource("source5.pure", mainMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingErrorDeleteParent() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", model, "source3.pure", baseMapping, "source4.pure", relational, "source5.pure", mainMapping)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", baseMappingEmpty).compileWithExpectedCompileFailure("Unable to find source class mapping (id:e) for property 'firm' in Association mapping 'Firm_Person'. Make sure that you have specified a valid Class mapping id as the source id and target id, using the syntax 'property[sourceId, targetId]: ...'.", null, 7, 7).updateSource("source3.pure", baseMapping).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }

    @Test
    public void testCreateAndDeleteAssoXStoreMappingErrorDeleteSuperType() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(Maps.mutable.with("source1.pure", modelInheritance, "source3.pure", modelInheritanceSuper, "source4.pure", relational, "source5.pure", inheritanceMapping)).compile(),
                new RuntimeTestScriptBuilder().updateSource("source3.pure", modelInheritanceSuper2).compileWithExpectedCompileFailure("The property 'id' is unknown in the Element 'Firm'", null, 5, 19).updateSource("source3.pure", modelInheritanceSuper).compile(),
                runtime, functionExecution, Lists.fixedSize.of());
    }
}
