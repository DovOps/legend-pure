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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;

import java.util.List;

public class CoreExtensionCompiled implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "CoreGen",
                """
                package org.finos.legend.pure.generated;
                
                import org.eclipse.collections.api.RichIterable;
                import org.eclipse.collections.api.factory.Lists;
                import org.eclipse.collections.api.list.MutableList;
                import org.eclipse.collections.impl.map.mutable.UnifiedMap;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.GetterOverride;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
                import org.finos.legend.pure.m3.execution.ExecutionSupport;
                import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
                import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.GetterOverrideExecutor;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction2Wrapper;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction0;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction2;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedProcedure;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
                import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
                
                import java.lang.reflect.Constructor;
                import java.lang.reflect.InvocationTargetException;
                import java.lang.reflect.Method;
                import java.math.BigDecimal;
                
                public class CoreGen
                {
                    public static final Bridge bridge = new BridgeImpl();
                
                    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType safeGetGenericType(Object val, ExecutionSupport es)
                    {
                        return Pure.safeGetGenericType(val, new DefendedFunction0<GenericType>()
                        {
                            @Override
                            public GenericType value()
                            {
                                return new Root_meta_pure_metamodel_type_generics_GenericType_Impl("");
                            }
                        }, es);
                    }
                
                    public static SharedPureFunction getSharedPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, ExecutionSupport es)
                    {
                        return Pure.getSharedPureFunction(func, bridge, es);
                    }
                
                    public static Object evaluate(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Object... instances)
                    {
                        return Pure.evaluate(es, func, bridge, instances);
                    }
                
                
                    public static Object evaluateToMany(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, RichIterable<? extends List<?>> instances)
                    {
                        MutableList<Object> inputs = Lists.mutable.of();
                        if (instances != null)
                        {
                            for (List<?> obj : instances)
                            {
                                inputs.add(obj._values());
                            }
                        }
                        return Pure._evaluateToMany(es, bridge, func, inputs);
                    }
                
                    public static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, ExecutionSupport es)
                    {
                        return Pure.dynamicMatch(obj, funcs, bridge, es);
                    }
                
                    private static Object dynamicMatch(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, boolean isMatchWith, ExecutionSupport es)
                    {
                        return Pure.dynamicMatch(obj, funcs, var, isMatchWith, bridge, es);
                    }
                
                    public static <T, V> RichIterable<T> removeDuplicates(RichIterable<T> list, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> keyFn, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> eqlFn, ExecutionSupport es)
                    {
                        return Pure.removeDuplicates(list, keyFn, eqlFn, bridge, es);
                    }
                
                
                
                
                    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es)
                    {
                        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(UnifiedMap.newMap()), bridge);
                    }
                
                    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap)
                    {
                        return Pure.canReactivateWithoutJavaCompilation(valueSpecification, es, lambdaOpenVariablesMap, bridge);
                    }
                
                
                
                
                    public static Object newObject(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, String name, RichIterable<? extends KeyExpression> root_meta_pure_functions_lang_keyExpressions, ExecutionSupport es)
                    {
                        return Pure.newObject(bridge, aClass, name, root_meta_pure_functions_lang_keyExpressions, es);
                    }
                
                
                    private static class BridgeImpl implements Bridge
                    {
                        @Override
                        public <T> List<T> buildList()
                        {
                            return new Root_meta_pure_functions_collection_List_Impl<>("");
                        }
                
                        @Override
                        public LambdaCompiledExtended buildLambda(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> lambdaFunction, SharedPureFunction<Object> pureFunction)
                        {
                            return new PureCompiledLambda(lambdaFunction, pureFunction);
                        }
                    }
                
                    public static String format(String formatString, Object formatArgs, ExecutionSupport es)
                    {
                        return CompiledSupport.format(formatString, formatArgs, new DefendedFunction2<Object, ExecutionSupport, String>()
                        {
                            public String value(Object any, ExecutionSupport executionSupport)
                            {
                                return toRepresentation(any, executionSupport);
                            }
                        }, es);
                    }
                
                    public static String toRepresentation(Object any, ExecutionSupport es)
                    {
                        if (any instanceof String)
                        {
                            return "'" + CompiledSupport.replace((String) any, "'", "\\\\'") + "'";
                        }
                        if (any instanceof org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate)
                        {
                            return "%" + CompiledSupport.pureToString((PureDate) any, es);
                        }
                        if (any instanceof BigDecimal)
                        {
                            return CompiledSupport.pureToString((BigDecimal) any, es) + "D";
                        }
                        if (any instanceof Number)
                        {
                            return CompiledSupport.pureToString((Number) any, es);
                        }
                        if (any instanceof Boolean)
                        {
                            return CompiledSupport.pureToString(((Boolean) any).booleanValue(), es);
                        }
                        if (any instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement p = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) any;
                            if (p._name() != null)
                            {
                                return Pure.elementToPath(p, "::");
                            }
                        }
                        return "<" + Pure.manageId(any) + "instanceOf " + Pure.elementToPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) CoreGen.safeGetGenericType(any, es)._rawType(), "::") + ">";
                    }
                
                    public static Object newObject(final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> aClass, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> keyExpressions, ElementOverride override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
                    {
                        final ClassCache classCache = ((CompiledExecutionSupport) es).getClassCache();
                        Constructor<?> constructor = classCache.getIfAbsentPutConstructorForType(aClass);
                        final Any result;
                        try
                        {
                            result = (Any) constructor.newInstance("");
                        }
                        catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
                        {
                            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                            StringBuilder builder = new StringBuilder("Error instantiating ");
                            org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);
                            String eMessage = cause.getMessage();
                            if (eMessage != null)
                            {
                                builder.append(": ").append(eMessage);
                            }
                            throw new RuntimeException(builder.toString(), cause);
                        }
                        keyExpressions.forEach(new DefendedProcedure<Root_meta_pure_functions_lang_KeyValue>()
                        {
                            @Override
                            public void value(Root_meta_pure_functions_lang_KeyValue keyValue)
                            {
                                Method m = classCache.getIfAbsentPutPropertySetterMethodForType(aClass, keyValue._key());
                                try
                                {
                                    m.invoke(result, keyValue._value());
                                }
                                catch (InvocationTargetException | IllegalAccessException e)
                                {
                                    Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                                    StringBuilder builder = new StringBuilder("Error setting property '").append(keyValue._key()).append("' for instance of ");
                                    org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, aClass);
                                    String eMessage = cause.getMessage();
                                    if (eMessage != null)
                                    {
                                        builder.append(": ").append(eMessage);
                                    }
                                    throw new RuntimeException(builder.toString(), cause);
                                }
                
                            }
                        });
                        PureFunction2Wrapper getterToOneExecFunc = getterToOneExec == null ? null : new PureFunction2Wrapper(getterToOneExec, es);
                        PureFunction2Wrapper getterToManyExecFunc = getterToManyExec == null ? null : new PureFunction2Wrapper(getterToManyExec, es);
                        ElementOverride elementOverride = override;
                        if (override instanceof GetterOverride)
                        {
                            elementOverride = ((GetterOverride) elementOverride)._getterOverrideToOne(getterToOne)._getterOverrideToMany(getterToMany)._hiddenPayload(payload);
                            ((GetterOverrideExecutor) elementOverride).__getterOverrideToOneExec(getterToOneExecFunc);
                            ((GetterOverrideExecutor) elementOverride).__getterOverrideToManyExec(getterToManyExecFunc);
                        }
                        result._elementOverride(elementOverride);
                        return result;
                    }
                
                    public static Object newObject
                            (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType
                                     genericType, RichIterable<? extends Root_meta_pure_functions_lang_KeyValue> root_meta_pure_functions_lang_keyExpressions, ElementOverride
                                     override, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function
                                     getterToOne, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function getterToMany, Object
                                     payload, PureFunction2 getterToOneExec, PureFunction2 getterToManyExec, ExecutionSupport es)
                    {
                        return newObject((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) genericType._rawType(), root_meta_pure_functions_lang_keyExpressions, override, getterToOne, getterToMany, payload, getterToOneExec, getterToManyExec, es);
                    }
                }\
                """));
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform";
    }

    public static CompiledExtension extension()
    {
        return new CoreExtensionCompiled();
    }
}
