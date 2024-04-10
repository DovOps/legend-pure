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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class NewDate extends AbstractNative
{
    public NewDate()
    {
        super("date_Integer_1__Date_1_",
                "date_Integer_1__Integer_1__Date_1_",
                "date_Integer_1__Integer_1__Integer_1__StrictDate_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
                "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        SourceInformation sourceInformation = functionExpression.getSourceInformation();
        return transformedParams.makeString("FunctionsGen.newDate(", ", ", ", " + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")");
    }

    @Override
    public String buildBody()
    {
        return """
                new SharedPureFunction<org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate>()
                {
                   @Override
                   public org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate execute(ListIterable<?> vars, final ExecutionSupport es)
                   {
                       switch (vars.size())
                       {
                           case 1:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), null);
                           }
                           case 2:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), (long) vars.get(1), null);
                           }
                           case 3:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), (long) vars.get(1), (long) vars.get(2), null);
                           }
                           case 4:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), (long) vars.get(1), (long) vars.get(2), (long) vars.get(3), null);
                           }
                           case 5:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), (long) vars.get(1), (long) vars.get(2), (long) vars.get(3), (long) vars.get(4), null);
                           }
                           default:
                           {
                               return FunctionsGen.newDate((long) vars.get(0), (long) vars.get(1), (long) vars.get(2), (long) vars.get(3), (long) vars.get(4), (Number) vars.get(5), null);
                           }
                       }
                   }
                }\
                """;
    }
}
