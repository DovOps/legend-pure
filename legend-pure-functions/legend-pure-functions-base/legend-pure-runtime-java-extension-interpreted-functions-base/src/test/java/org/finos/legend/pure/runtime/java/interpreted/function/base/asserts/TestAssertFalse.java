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

package org.finos.legend.pure.runtime.java.interpreted.function.base.asserts;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestAssertFalse extends PureExpressionTest
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFailWithoutMessage()
    {
        assertExpressionRaisesPureException("Assert failed", 3, 9, "assertFalse(true)");
        assertExpressionRaisesPureException("Assert failed", 3, 9, "assertFalse(2 == 2)");
    }

    @Test
    public void testFailWithMessageString()
    {
        assertExpressionRaisesPureException("Test message", 3, 9, "assertFalse(true, 'Test message')");
        assertExpressionRaisesPureException("Test message", 3, 9, "assertFalse(2 == 2, 'Test message')");
    }

    @Test
    public void testFailWithFormattedMessage()
    {
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(true, 'Test message: %d', 2 + 3)");
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(2 == 2, 'Test message: %d', 2 + 3)");
    }

    @Test
    public void testFailWithMessageFunction()
    {
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(true, |format('Test message: %d', 2 + 3))");
        assertExpressionRaisesPureException("Test message: 5", 3, 9, "assertFalse(2 == 2, |format('Test message: %d', 2 + 3))");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
