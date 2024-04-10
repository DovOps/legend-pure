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
import org.finos.legend.pure.m3.tools.ThrowableTools;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestMutateAdd extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void addNonExistingProperty() throws Exception
    {
        try
        {
            this.compileTestSource("""
                    Class SimpleClass{att:String[*];}
                    function test():Any[*]
                    {
                      let sc = ^SimpleClass();
                      $sc->mutateAdd('nonExistingAttribute', [42, 1]);\
                    }\
                    """);
            this.execute("test():Any[*]");
            Assertions.fail("Expected cast error");
        }
        catch (Exception e)
        {
            PureException pureException = ThrowableTools.findTopThrowableOfClass(e, PureException.class);
            this.assertPureException("Cannot find property 'nonExistingAttribute' on SimpleClass", pureException);
        }
    }

}
