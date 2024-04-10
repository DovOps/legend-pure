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

package org.finos.legend.pure.m3.tests.validation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestServiceURL extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

//    @Test
//    public void testPattern()
//    {
//        Pattern p = Pattern.compile("([^{]+)|\\{([a-zA-Z_0-9]+)(:(([^}]+)))?\\}");
//        Matcher m = p.matcher("/myURL{eop:a(b)*c}/{a}/ee/{l:opop}/p{op}");
//        String res = "";
//        while (m.find())
//        {
//            if (m.group(1) != null)
//            {
//                res = res + m.group(1);
//            }
//            else
//            {
//                if (m.group(4) == null)
//                {
//                    res = res + "(?<" + m.group(2) + ">.+)";
//                }
//                else
//                {
//                    System.out.println(m.start(4) + "-" + m.end(4));
//                    res = res + "(?<" + m.group(2) + ">" + m.group(4) + ")";
//                }
//            }
//        }
//        System.out.println(res);
//        Pattern o = Pattern.compile(res);
//        Matcher om = o.matcher("/myURLabbbbbc/arf/ee/opop/parg");
//        if (om.matches())
//        {
//            System.out.println("eop = " + om.group("eop"));
//            System.out.println("l = " + om.group("l"));
//            System.out.println("a = " + om.group("a"));
//            System.out.println("op = " + om.group("op"));
//        }
//    }

    @Test
    public void testSimpleHappyPath()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testRegExpHappyPath()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param:a(b)*c}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testVariableNameNonOrdered()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}/{param2}'} myFunc(param2:String[1], param:String[1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testAllMandatoryArgumentsInURI()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1], otherOne:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter multiplicity issue. All parameters with multiplicity [1] must be a part of the service url", e);
    }

    @Test
    public void testOptionalArguments()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testOptionalArgumentsInUri()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[0..1], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter multiplicity issue. A service function parameter specified in the URI has to be String[1]", 1, 56, e);
    }

    @Test
    public void testZeroToManyQueryParameters()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1], otherOne:String[*]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testOneToManyQueryParameter()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL}'} myFunc(param:String[1..*], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter multiplicity issue. Parameters not part of the service url must have multiplicity [0..1] or [*]", 1, 49, e);
    }

    @Test
    public void testZeroToManyServiceUrlParameter()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[*], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter multiplicity issue. A service function parameter specified in the URI has to be String[1]", 1, 56, e);
    }

    @Test
    public void testOneToManyServiceUrlParameter()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}}'} myFunc(param:String[1..*], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter multiplicity issue. A service function parameter specified in the URI has to be String[1]", 1, 57, e);
    }

    @Test
    public void testNoURIArguments()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL'} myFunc(param:String[0..1], otherOne:String[0..1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testPrimitiveArgsInURI()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{s}/{d}/{dt}/{sd}/{b}/{i}/{f}'} myFunc(s:String[1], d:Date[1], dt:DateTime[1], sd:StrictDate[1], b:Boolean[1], i:Integer[1], f:Float[1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testEnumArgInURI()
    {
        compileTestSource(
                "fromString.pure",
                """
                Enum MyEnum {VALUE1}
                function {service.url='/testURL/{enum}'} myFunc(enum:MyEnum[1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testPrimitiveArgsInQueryParameters()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL'} myFunc(s:String[0..1], d:Date[0..1], dt:DateTime[0..1], sd:StrictDate[0..1], b:Boolean[0..1], i:Integer[0..1], f:Float[0..1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testEnumInQueryParameters()
    {
        compileTestSource(
                "fromString.pure",
                """
                Enum MyEnum {VALUE1}
                function {service.url='/testURL}'} myFunc(enum:MyEnum[0..1]):String[1]
                {
                   'ee';
                }
                """);
    }

    @Test
    public void testInvalidTypeInQueryParameters()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class MyClass {}
                function {service.url='/testURL'} myFunc(param:MyClass[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter type issue. All parameters must be a primitive type or enum", 2, 48, e);
    }

    @Test
    public void testInvalidTypeInURIParameters()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                Class MyClass {}
                function {service.url='/testURL/{param}'} myFunc(param:MyClass[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Parameter type issue. All parameters must be a primitive type or enum", 2, 56, e);
    }

    @Test
    public void testRegExpError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param:testReg{b}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Error in the user provided regexp: testReg{b", "fromString.pure", 1, 40, 1, 40, 1, 48, e);
    }

    @Test
    public void testRegExpErrorMultiline()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url=
                         '/testURL/{param1:testReg[abc]}/{param2:testReg{b}'
                        }
                 myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "Error in the user provided regexp: testReg{b", "fromString.pure", 2, 50, 2, 50, 2, 58, e);
    }

    @Test
    public void testReturnTypeError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1]):Integer[1]
                {
                   1;
                }
                """));
        assertPureException(PureCompilationException.class, "Return type issue. A service function has to return a 'String' or a subtype of 'ServiceResult'.", 1, 67, e);
    }

    @Test
    public void testServiceReturnReturnType()
    {
        // verify that this compiles
        compileTestSource(
                "fromString.pure",
                """
                Class EmptyResult<T> extends ServiceResult<T|0>
                {
                }
                
                function {service.url='/testURL/{param}'} myFunc(param:String[1]):EmptyResult<Any>[1]
                {
                   ^EmptyResult<Any>();
                }
                """);
    }

    @Test
    public void testReturnMultiplicityError()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/testURL/{param}'} myFunc(param:String[1]):String[*]
                {
                   1;
                }
                """));
        assertPureException(PureCompilationException.class, "Return multiplicity issue. A service function has to return one ([1]) element.", 1, 67, e);
    }

    @Test
    public void testOverlap()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/hello/{param:testReg}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                function {service.url='/hello/{param:testReg}'} myFunc3(param:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "A function has already been registered with the key '/hello/' (myFunc3_String_1__String_1_)", 5, 23, e);
    }

    @Test
    public void testOverlapOptionalParameters()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/hello/{param}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                function {service.url='/hello/{param}'} myFunc3(param:String[1], param2:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "A function has already been registered with the key '/hello/' (myFunc3_String_1__String_$0_1$__String_1_)", 5, 23, e);
    }

    @Test
    public void testOverlappingURIsQueryParamOnly()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/hello'} myFunc(param:String[0..1]):String[1]
                {
                   'ee';
                }
                function {service.url='/hello'} myFunc3(param:String[0..1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "A function has already been registered with the key '/hello' (myFunc3_String_$0_1$__String_1_)", 5, 23, e);
    }

    @Test
    public void keyStructureStart()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='hello/{param:testReg}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "The URL needs to start with '/' (hello/)", 1, 23, e);
    }

    @Test
    public void keyStructure()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                """
                function {service.url='/hello{param:testReg}'} myFunc(param:String[1]):String[1]
                {
                   'ee';
                }
                """));
        assertPureException(PureCompilationException.class, "The first part of the URL (/hello) needs to end with '/'", 1, 23, e);
    }

    @Test
    public void keyStructureNoParams()
    {
        compileTestSource(
                "fromString.pure",
                """
                function {service.url='/hello'} myFunc():String[1]
                {
                   'ee';
                }
                """);
    }
}
