// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestNewQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("StandardCall.pure");
        runtime.compile();
    }

    @Test
    public void standardCall()
    {
        String source = """
                function go():Any[*]
                {
                    let classA = 'meta::pure::functions::meta::A'->newClass();
                    let classB = 'meta::pure::functions::meta::B'->newClass();
                    let qualifiedProperty = newQualifiedProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne, [^VariableExpression(name = 'newName', multiplicity = ZeroOne, genericType = ^GenericType(rawType = Any))]);
                    assertEquals('a', $qualifiedProperty.name);
                    assertEquals('a', $qualifiedProperty.functionName);
                    assertEquals('B', $qualifiedProperty.owner.name->toOne());
                    assertEquals(PureOne, $qualifiedProperty.multiplicity);
                    assertEquals('A', $qualifiedProperty.genericType.rawType->toOne().name);
                    assertEquals('QualifiedProperty', $qualifiedProperty.classifierGenericType.rawType->toOne().name);
                    let typeArguments = $qualifiedProperty.classifierGenericType.typeArguments;
                    assertEquals(1, $typeArguments->size());
                    assert($typeArguments->toOne().rawType->toOne()->instanceOf(FunctionType), |'Expected qualified property type argument to be instance of FunctionType');
                    assertEquals('A', $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnType.rawType->toOne().name);
                    assertEquals(PureOne, $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnMultiplicity);
                    let params = $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).parameters->evaluateAndDeactivate();
                    assertEquals(1, $params->size());
                    assert($params->toOne()->instanceOf(Any), |'Expected function type to have one parameter');
                    assertEquals(ZeroOne, $params->toOne().multiplicity);
                    assertEquals('newName', $params.name);
                }\
                """;

        compileTestSource("StandardCall.pure", source);
        CoreInstance func = runtime.getFunction("go():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }
}
