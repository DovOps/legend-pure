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

package org.finos.legend.pure.runtime.java.interpreted.function.base.collection;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestAppendTreeToNode extends AbstractPureTestWithCoreCompiled
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testSimple()
    {
        compileTestSource("""
                Class MyNode extends TreeNode
                {
                    value :String[1];
                    children(){$this.childrenData->cast(@MyNode)}:MyNode[*];
                }
                
                function meta::pure::functions::collection::appendTreeToNode(root:TreeNode[1], position: TreeNode[1], joinTree:TreeNode[1]): TreeNode[1]
                {
                   $root->meta::pure::functions::collection::replaceTreeNode($position, ^$position(childrenData += $joinTree));
                }
                function test():Nil[0]
                {
                    let tree = ^MyNode(value='1', childrenData = [^MyNode(value ='2', childrenData=[^MyNode(value ='4')]), ^MyNode(value='3')]);
                    let subTree = ^MyNode(value='10', childrenData = [^MyNode(value ='11'), ^MyNode(value='12', childrenData=[^MyNode(value ='13')])]);
                    let point = $tree.children()->filter(n|$n.value == '3')->at(0);
                    print($tree->appendTreeToNode($point, $subTree),10);
                    print($tree,10);
                }
                """);
        this.execute("test():Nil[0]");
        Assertions.assertEquals("""
                Anonymous_StripedId instance MyNode
                    childrenData(Property):
                        Anonymous_StripedId instance MyNode
                            childrenData(Property):
                                Anonymous_StripedId instance MyNode
                                    classifierGenericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] MyNode instance Class
                                    value(Property):
                                        4 instance String
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] MyNode instance Class
                            value(Property):
                                2 instance String
                        Anonymous_StripedId instance MyNode
                            childrenData(Property):
                                Anonymous_StripedId instance MyNode
                                    childrenData(Property):
                                        Anonymous_StripedId instance MyNode
                                            classifierGenericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        [X] MyNode instance Class
                                            value(Property):
                                                11 instance String
                                        Anonymous_StripedId instance MyNode
                                            childrenData(Property):
                                                Anonymous_StripedId instance MyNode
                                                    classifierGenericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                [X] MyNode instance Class
                                                    value(Property):
                                                        13 instance String
                                            classifierGenericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        [X] MyNode instance Class
                                            value(Property):
                                                12 instance String
                                    classifierGenericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] MyNode instance Class
                                    value(Property):
                                        10 instance String
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] MyNode instance Class
                            value(Property):
                                3 instance String
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] MyNode instance Class
                    value(Property):
                        1 instance String\
                """, functionExecution.getConsole().getLine(0));
        Assertions.assertEquals("""
                Anonymous_StripedId instance MyNode
                    childrenData(Property):
                        Anonymous_StripedId instance MyNode
                            childrenData(Property):
                                Anonymous_StripedId instance MyNode
                                    classifierGenericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                [X] MyNode instance Class
                                    value(Property):
                                        4 instance String
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] MyNode instance Class
                            value(Property):
                                2 instance String
                        Anonymous_StripedId instance MyNode
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        [X] MyNode instance Class
                            value(Property):
                                3 instance String
                    classifierGenericType(Property):
                        Anonymous_StripedId instance GenericType
                            rawType(Property):
                                [X] MyNode instance Class
                    value(Property):
                        1 instance String\
                """, functionExecution.getConsole().getLine(1));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
