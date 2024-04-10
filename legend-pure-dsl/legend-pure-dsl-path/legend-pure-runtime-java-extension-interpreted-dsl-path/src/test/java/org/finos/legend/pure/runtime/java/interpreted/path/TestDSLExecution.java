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

package org.finos.legend.pure.runtime.java.interpreted.path;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDSLExecution extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testSimple() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", """
                               Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}
                               function test():Any[*]
                               {
                                   print(#/Firm<Any>/employees/address#, 2);
                               }
                               """);
        this.runtime.compile();
        this.execute("test():Any[*]");
        Assertions.assertEquals("""
                Anonymous_StripedId instance Path
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            multiplicityArguments(Property):
                                [X] PureOne instance PackageableMultiplicity
                            rawType(Property):
                                [X] Path instance Class
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [~>] Firm instance Class
                                    typeArguments(Property):
                                        [>2] Anonymous_StripedId instance GenericType
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [~>] Address instance Class
                    name(Property):
                         instance String
                    path(Property):
                        Anonymous_StripedId instance PropertyPathElement
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] PropertyPathElement instance Class
                            property(Property):
                                Anonymous_StripedId instance PropertyStub
                                    owner(Property):
                                        [X] Firm instance Class
                                    propertyName(Property):
                                        [>2] employees instance String
                                    resolvedProperty(Property):
                                        [>2] employees instance Property
                        Anonymous_StripedId instance PropertyPathElement
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] PropertyPathElement instance Class
                            property(Property):
                                Anonymous_StripedId instance PropertyStub
                                    owner(Property):
                                        [X] Person instance Class
                                    propertyName(Property):
                                        [>2] address instance String
                                    resolvedProperty(Property):
                                        [>2] address instance Property
                    referenceUsages(Property):
                        Anonymous_StripedId instance ReferenceUsage
                            offset(Property):
                                0 instance Integer
                            owner(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        [>2] Anonymous_StripedId instance GenericType
                                    multiplicity(Property):
                                        [X] PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        [>2] Anonymous_StripedId instance ParameterValueSpecificationContext
                                    values(Property):
                                        [>2] Anonymous_StripedId instance Path
                            propertyName(Property):
                                values instance String
                    start(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [~>] Firm instance Class
                            referenceUsages(Property):
                                Anonymous_StripedId instance ReferenceUsage
                                    offset(Property):
                                        [>2] 0 instance Integer
                                    owner(Property):
                                        [>2] Anonymous_StripedId instance Path
                                    propertyName(Property):
                                        [>2] start instance String
                            typeArguments(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [~>] Any instance Class
                                    referenceUsages(Property):
                                        [>2] Anonymous_StripedId instance ReferenceUsage\
                """, this.functionExecution.getConsole().getLine(0));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
