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

package org.finos.legend.pure.runtime.java.interpreted.natives.basics.math;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

public class Abs extends NativeFunction
{
    private final ModelRepository repository;

    public Abs(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        CoreInstance number = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        boolean bigDecimalToPureDecimal = NumericUtilities.IS_DECIMAL_CORE_INSTANCE(processorSupport).accept(number);
        Number javaNumber = NumericUtilities.toJavaNumber(number, processorSupport);
        if (javaNumber instanceof Integer integer)
        {
            return NumericUtilities.toPureNumberValueExpression(Math.abs(integer), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        if (javaNumber instanceof Long long1)
        {
            return NumericUtilities.toPureNumberValueExpression(Math.abs(long1), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        if (javaNumber instanceof Double double1)
        {
            return NumericUtilities.toPureNumberValueExpression(Math.abs(double1), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        if (javaNumber instanceof Float float1)
        {
            return NumericUtilities.toPureNumberValueExpression(Math.abs(float1), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        if (javaNumber instanceof BigInteger integer)
        {
            return NumericUtilities.toPureNumberValueExpression(integer.abs(), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        if (javaNumber instanceof BigDecimal decimal)
        {
            return NumericUtilities.toPureNumberValueExpression(decimal.abs(), bigDecimalToPureDecimal, this.repository, processorSupport);
        }
        throw new IllegalArgumentException("Unhandled number: " + javaNumber);
    }
}
