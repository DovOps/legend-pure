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

package org.finos.legend.pure.m3.tests.function;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public abstract class AbstractTestFunctionDefinitionModify extends AbstractPureTestWithCoreCompiled
{
    private static final String DECLARATION = """
            function performCompare(origLambda : FunctionDefinition<{String[1]->String[1]}>[1],\s
                               mutator : FunctionDefinition<{FunctionDefinition<{String[1]->String[1]}>[1],
                                                             FunctionDefinition<{String[1]->String[1]}>[1]
                                                             ->FunctionDefinition<{String[1]->String[1]}>[1]
                                                           }>[1],
                               expectedLambda : FunctionDefinition<{String[1]->String[1]}>[1]):Boolean[1]
            {\s
                if($origLambda->openVariableValues()->keyValues()->isEmpty(), \
                     | '',\
                     | print('WARNING: Copy/clone of lambdas with open variables fully supported, failures may occur', 1)\
                     );
             \s
              let newLambda = $mutator->eval($origLambda, $expectedLambda);
             \s
              let inputVal = 'hello';
            
              print('Evaluating $origLambda\\n', 1);
              let resultOrigLambda = $origLambda->eval($inputVal);
              print('Evaluating $expectedLambda\\n', 1);
              let resultExpectedLambda = $expectedLambda->eval($inputVal);
              print('Evaluating $newLambda\\n', 1);
              let resultNewLambda = $newLambda->eval($inputVal);
            
              print('$resultOrigLambda: ' + $resultOrigLambda + '\\n', 1);
              print('$resultExpectedLambda: ' + $resultExpectedLambda + '\\n', 1);
              print('$resultNewLambda: ' + $resultNewLambda + '\\n', 1);
              print('$resultOrigLambda sourceInformation: ', 1);
              print($resultOrigLambda->sourceInformation(), 1);
              print('$resultNewLambda sourceInformation: ', 1);
              print($resultNewLambda->sourceInformation(), 1);
            
              //if($resultNewLambda == $resultOrigLambda,
              //     | fail('Modified lambda result not changed, got original: \\'' + $resultOrigLambda +  '\\''),
              //     | true);
            
              if($resultNewLambda != $resultExpectedLambda,
                   | fail('Modified lambda result not as expected, expected: \\'' + $resultExpectedLambda +  '\\' got: \\'' + $resultNewLambda +  '\\''),
                   | true);
            }
            
            
            function test::hierarchicalProperties(class:Class<Any>[1]):Property<Nil,Any|*>[*]
            {
               if($class==Any,
                  | [],
                  | $class.properties->concatenate($class.generalizations->map(g| test::hierarchicalProperties($g.general.rawType->cast(@Class<Any>)->toOne())))->removeDuplicates()
               );
            }
            
            function modifyExpressionSequenceWithDynamicNew<T>(fd:FunctionDefinition<T>[1], es : ValueSpecification[1..*]) : FunctionDefinition<T>[1]
            {
                let genericType = ^KeyValue(key='classifierGenericType', value= $fd.classifierGenericType);
            
                let fdClass = $fd->type()->cast(@Class<Any>);
                let properties = $fdClass->test::hierarchicalProperties()->map(p|
                  if($p.name == 'expressionSequence',\s
                    | ^KeyValue(key=$p.name->toOne(), value= $es),\s
                    | ^KeyValue(key=$p.name->toOne(), value= $p->eval($fd))
                    );
                  );
            
                dynamicNew($fd.classifierGenericType->toOne(), $properties->concatenate($genericType))->cast($fd);
            }
            
            """;

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testConcreteFunctionDefinitionModifyWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function xx::myFunc(s : String[1]) : String[1] { 'answer: ' + $s; }\n"
                        + "\n"
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origFunc = xx::myFunc_String_1__String_1_;\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f(expressionSequence = $f2->evaluateAndDeactivate().expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda);\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testConcreteFunctionDefinitionModifyWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function xx::myFunc(s : String[1]) : String[1] { 'answer: ' + $s; }\n"
                        + "\n"
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origFunc = xx::myFunc_String_1__String_1_;\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f2.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda);\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testLambdaModifyWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origLambda = {s:String[1]|'answer: ' + $s};\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f(expressionSequence = $f2->evaluateAndDeactivate().expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda);\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @Disabled(value = "This is not supported (Dynamic new can't pass in the variable context / values for open variables)")
    public void testLambdaCloneWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n" +
                        "  let openVarValue = 'xyz';\n" +
                        "  \n" +
                        "  let origLambda = {s:String[1]|'answer: ' + $openVarValue + '/' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $origLambda);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @Disabled(value = "This is not supported (copy doesn't in the variable context / values for open variables)")
    public void testLambdaCloneWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n" +
                        "  let openVarValue = 'xyz';\n" +
                        "  \n" +
                        "  let origLambda = {s:String[1]|'answer: ' + $openVarValue + '/' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f()\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $origLambda);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testLambdaModifyWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origLambda = {s:String[1]|'answer: ' + $s};\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f2.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda);\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository test = new GenericCodeRepository("test", null, "platform");
        repositories.add(test);
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }
}
