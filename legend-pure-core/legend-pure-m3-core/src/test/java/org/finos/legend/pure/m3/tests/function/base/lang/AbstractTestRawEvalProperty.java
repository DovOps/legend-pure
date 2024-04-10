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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestRawEvalProperty extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void objectDoesntHavePropertyFail()
    {
        try
        {
            this.compileTestSource("""
                    Class Person
                    {
                       name: String[1];
                    }
                    Class Alien
                    {
                       species: String[1];
                    }
                    function test():Nil[0]
                    {
                       let person = ^Person(name = 'Obi Wan');
                       let alien = ^Alien(species='Wookiee');
                       print(Person -> classPropertyByName('name') -> toOne() -> rawEvalProperty($alien), 1);
                    }\
                    """);
            this.execute("test():Nil[0]");
            Assertions.fail();
        }
        catch (Exception e)
        {
            this.assertPureException(PureExecutionException.class, "Can't find the property 'name' in the class Alien", 13, 62, e);
        }
    }
}
