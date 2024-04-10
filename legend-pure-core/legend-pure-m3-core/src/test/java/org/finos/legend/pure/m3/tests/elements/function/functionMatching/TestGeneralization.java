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

package org.finos.legend.pure.m3.tests.elements.function.functionMatching;

import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGeneralization extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @Test
    public void testMultipleGeneralizationFunctionMatchingError()
    {
        try
        {
            compileTestSource("""
                    Class A
                    {
                       propA:String[1];
                    }
                    Class C
                    {
                       propC:String[1];
                    }
                    Class B extends A,C
                    {
                       propB:String[1];
                    }
                    Class D
                    {
                    }
                    function simpleTest():String[1]
                    {
                       let b = ^B(propA='iA',propB='iB',propC='iC');
                       callWithA($b);
                       callWithC($b);
                       callWithD($b);
                    }
                    function callWithA(a:A[1]):String[1]
                    {
                       $a.propA;
                    }
                    function callWithC(c:C[1]):String[1]
                    {
                       $c.propC;
                    }
                    function callWithD(d:D[1]):String[1]
                    {
                       'D';
                    }\
                    """);
            Assertions.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "callWithD(_:B[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tcallWithD(D[1]):String[1]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, 21, 4, e);
        }
    }
}
