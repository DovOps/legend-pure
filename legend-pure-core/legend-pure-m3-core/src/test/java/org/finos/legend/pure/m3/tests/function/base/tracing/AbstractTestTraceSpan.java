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

package org.finos.legend.pure.m3.tests.function.base.tracing;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import io.opentracing.util.GlobalTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractTestTraceSpan extends AbstractPureTestWithCoreCompiled
{
    protected static final InMemoryTracer tracer = new InMemoryTracer();


    @Test
    public void testTraceSpan()
    {
        compileTestSource("fromString.pure", """
                function testTraceSpan():Nil[0]
                {
                    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute');
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertTrue(tracer.spanExists("Test Execute"));
    }

    @Test
    public void testTraceSpanWithReturnValue()
    {
        compileTestSource("fromString.pure", """
                function testTraceSpan():Nil[0]
                {
                    let text = meta::pure::functions::tracing::traceSpan(|' World', 'Test Execute');
                    print('Hello' + $text, 1);
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    }

    @Test
    public void testTraceSpanWithAnnotations()
    {
        compileTestSource("fromString.pure", """
                function testTraceSpan():Nil[0]
                {
                   let annotations = newMap([
                      pair('key1', 'value1'),\s
                      pair('key2', 'value2')
                     ]); \s
                    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |$annotations);
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertTrue(tracer.spanExists("Test Execute"));
        Map<Object, Object> tags = this.tracer.getTags("Test Execute");
        Assertions.assertEquals(tags.get("key1"), "value1");
        Assertions.assertEquals(tags.get("key2"), "value2");
    }

    @Test
    public void testTraceSpanUsingEval()
    {
        compileTestSource("fromString.pure", """
                function testTraceSpan():Nil[0]
                {
                    let res = meta::pure::functions::tracing::traceSpan_Function_1__String_1__V_m_->eval(|'Hello World', 'Test Execute');
                    print($res,1);\
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertTrue(tracer.spanExists("Test Execute"));
    }

    @Test
    public void testDoNoTraceIfTracerNotRegistered()
    {
        tracer.reset();
        unregisterTracer();
        compileTestSource("fromString.pure", """
                function testTraceSpan():Nil[0]
                {
                    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute');
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertFalse(tracer.spanExists("Test Execute"));
        GlobalTracer.registerIfAbsent(tracer);
    }

    @Test
    public void testTraceSpanShouldHandleErrorWhileEvaluatingTagsLamda()
    {
        compileTestSource("fromString.pure", """
                function getTags(): Map<String, String>[1] {\
                   assert('a' == 'b', |'');    \
                   newMap([       \s
                      pair('key1', '')\s
                     ]); \s
                }\
                function testTraceSpan():Nil[0]
                {
                    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |getTags(), false);
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertTrue(tracer.spanExists("Test Execute"));
        Map<Object, Object> tags = this.tracer.getTags("Test Execute");
        Assertions.assertTrue(tags.get("Exception").toString().startsWith("Unable to resolve tags - "));
    }

    @Test
    public void testTraceSpanShouldHandleStackOverflowErrorWhileEvaluatingTagsLamda()
    {
        compileTestSource("fromString.pure", """
                function getTags(): Map<String, String>[1] {\
                   getTags(); \s
                }\
                function testTraceSpan():Nil[0]
                {
                    meta::pure::functions::tracing::traceSpan(|print('Hello World', 1), 'Test Execute', |getTags(), false);
                }
                """);
        this.execute("testTraceSpan():Nil[0]");
        Assertions.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
        Assertions.assertTrue(tracer.spanExists("Test Execute"));
        Map<Object, Object> tags = this.tracer.getTags("Test Execute");
        Assertions.assertTrue(tags.get("Exception").toString().startsWith("Unable to resolve tags - "));
    }

    @Test
    public void testTraceSpanShouldNotHandleErrorWhileEvaluatingTagsLamda()
    {
        assertThrows(PureAssertFailException.class, () -> {
            compileTestSource("fromString.pure", """
                    function getTags(): Map<String, String>[1] {\
                       assert('a' == 'b', |'');    \
                       newMap([       \s
                          pair('key1', '')\s
                         ]); \s
                    }\
                    function testTraceSpan():Nil[0]
                    {
                        meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |getTags());
                    }
                    """);
            this.execute("testTraceSpan():Nil[0]");
        });
    }

    @AfterAll
    public static void tearDown()
    {
        tracer.reset();
        unregisterTracer();
    }

    private static void unregisterTracer()
    {
        try
        {
            // HACK since GlobalTracer api doesnt provide a way to reset the tracer which is needed for testing
            Field tracerField = GlobalTracer.get().getClass().getDeclaredField("isRegistered");
            tracerField.setAccessible(true);
            tracerField.set(GlobalTracer.get(), false);
            Assertions.assertFalse(GlobalTracer.isRegistered());
        }
        catch (Exception ignored)
        {
        }
    }

}
