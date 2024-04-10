// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License",
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

package org.finos.legend.pure.runtime.java.extension.functions.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteAlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteLegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.Profile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.ReplaceTreeNode;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.GetIfAbsentPutWithKey;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.GetMapStats;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.KeyValues;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.Keys;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.NewMap;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.NewMapWithProperties;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.Put;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.PutAllMaps;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.PutAllPairs;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.ReplaceAll;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.Values;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Decrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Encrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.*;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.AdjustDate;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DateDiff;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DatePart;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfMonth;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfWeekNumber;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasDay;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasHour;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasMinute;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasMonth;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasSecond;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasSubsecond;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.HasSubsecondWithAtLeastPrecision;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Hour;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Minute;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.MonthNumber;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.NewDate;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Now;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Second;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Today;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.WeekOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Year;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.hash.Hash;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.ReadFile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MatchWith;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.RawEvalProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.RemoveOverride;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ArcCosine;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ArcSine;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ArcTangent;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ArcTangent2;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Cbrt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Ceiling;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Cosine;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.CoTangent;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Exp;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Floor;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Log;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Log10;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Mod;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Pow;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Rem;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Round;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.RoundWithScale;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Sign;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Sine;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Sqrt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.StdDev;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.Tangent;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ToDecimal;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.ToFloat;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.CanReactivateDynamically;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.CompileValueSpecification;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.Deactivate;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.EnumName;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.EnumValues;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.FunctionDescriptorToId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.Generalizations;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsSourceReadOnly;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsValidFunctionDescriptor;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewAssociation;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewClass;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewEnumeration;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewLambdaFunction;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewQualifiedProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.OpenVariableValues;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.Reactivate;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.Stereotype;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.SubTypeOf;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.Tag;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.Guid;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ASCII;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Char;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Chunk;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Contains;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EndsWith;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.LTrim;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Matches;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ParseBoolean;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ParseDate;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ParseDecimal;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ParseFloat;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ParseInteger;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.RTrim;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ReverseString;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ToLower;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ToUpper;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Trim;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.tracing.TraceSpan;

import java.util.List;

public class FunctionsExtensionCompiled implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "FunctionsGen",
                """
                package org.finos.legend.pure.generated;
                
                import org.eclipse.collections.api.RichIterable;
                import org.eclipse.collections.api.block.function.Function0;
                import org.eclipse.collections.api.factory.Lists;
                import org.eclipse.collections.api.list.ListIterable;
                import org.eclipse.collections.api.list.MutableList;
                import org.eclipse.collections.impl.list.mutable.FastList;
                import org.finos.legend.pure.m3.coreinstance.Package;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
                import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
                import org.finos.legend.pure.m3.exception.PureExecutionException;
                import org.finos.legend.pure.m3.execution.ExecutionSupport;
                import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
                import org.finos.legend.pure.m4.coreinstance.SourceInformation;
                import org.finos.legend.pure.runtime.java.compiled.delta.CodeBlockDeltaCompiler;
                import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction0;
                import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
                import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
                import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;
                import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;
                import org.finos.legend.pure.runtime.java.shared.http.URLScheme;
                public class FunctionsGen extends org.finos.legend.pure.runtime.java.extension.functions.compiled.FunctionsHelper
                {
                    public static Root_meta_pure_functions_io_http_HTTPResponse executeHttpRaw(Root_meta_pure_functions_io_http_URL url, Object method, String mimeType, String body, ExecutionSupport executionSupport)
                    {
                        URLScheme scheme = URLScheme.http;
                        if (url._scheme() != null)
                        {
                            scheme = URLScheme.valueOf(url._scheme()._name());
                        }
                        return (Root_meta_pure_functions_io_http_HTTPResponse) HttpRawHelper.toHttpResponseInstance(HttpRawHelper.executeHttpService(scheme, url._host(), (int) url._port(), url._path(), HttpMethod.valueOf(((Enum) method)._name()), mimeType, body), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
                    }
                
                    public static Root_meta_pure_functions_meta_CompilationResult compileCodeBlock(String source, ExecutionSupport es)
                    {
                        Root_meta_pure_functions_meta_CompilationResult result = null;
                        if (source != null)
                        {
                            CodeBlockDeltaCompiler.CompilationResult compilationResult = CodeBlockDeltaCompiler.compileCodeBlock(source, ((CompiledExecutionSupport) es));
                            result = convertCompilationResult(compilationResult);
                        }
                        return result;
                    }
                
                    public static RichIterable<Root_meta_pure_functions_meta_CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, ExecutionSupport es)
                    {
                        RichIterable<CodeBlockDeltaCompiler.CompilationResult> compilationResults = CodeBlockDeltaCompiler.compileCodeBlocks(sources, ((CompiledExecutionSupport) es));
                        MutableList<Root_meta_pure_functions_meta_CompilationResult> results = FastList.newList(sources.size());
                
                        for (CodeBlockDeltaCompiler.CompilationResult compilationResult : compilationResults)
                        {
                            results.add(convertCompilationResult(compilationResult));
                        }
                        return results;
                    }
                
                
                    private static Root_meta_pure_functions_meta_CompilationResult convertCompilationResult(CodeBlockDeltaCompiler.CompilationResult compilationResult)
                    {
                        Root_meta_pure_functions_meta_CompilationResult result = new Root_meta_pure_functions_meta_CompilationResult_Impl("");
                
                        if (compilationResult.getFailureMessage() != null)
                        {
                            Root_meta_pure_functions_meta_CompilationFailure failure = new Root_meta_pure_functions_meta_CompilationFailure_Impl("");
                            failure._message(compilationResult.getFailureMessage());
                
                            SourceInformation si = compilationResult.getFailureSourceInformation();
                
                            if (si != null)
                            {
                                Root_meta_pure_functions_meta_SourceInformation sourceInformation = new Root_meta_pure_functions_meta_SourceInformation_Impl("");
                                sourceInformation._column(si.getColumn());
                                sourceInformation._line(si.getLine());
                                sourceInformation._endColumn(si.getEndColumn());
                                sourceInformation._endLine(si.getEndLine());
                                sourceInformation._startColumn(si.getStartColumn());
                                sourceInformation._startLine(si.getStartLine());
                                failure._sourceInformation(sourceInformation);
                            }
                            result._failure(failure);
                        }
                        else
                        {
                            ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) compilationResult.getResult();
                            result._result(cfd._expressionSequence().getFirst());
                        }
                        return result;
                    }
                
                    public static Object alloyTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)
                    {
                        return alloyTest(es, alloyTest, regular, CoreGen.bridge);
                    }
                
                    public static Object legendTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)
                    {
                        return legendTest(es, alloyTest, regular,  CoreGen.bridge);
                    }
                
                    public static PureMap newMap(RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<?, ?>> pairs, RichIterable<? extends Property<?, ?>> properties, ExecutionSupport es)
                    {
                        return newMap(pairs, properties, CoreGen.bridge, es);
                    }
                
                    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2)
                    {
                        return zip(l1, l2, new DefendedFunction0<Pair<U, V>>()
                        {
                            @Override
                            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> value()
                            {
                                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>("");
                            }
                        });
                    }
                
                    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2)
                    {
                        return zip(l1, l2, new DefendedFunction0<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>()
                        {
                            @Override
                            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> value()
                            {
                                return new Root_meta_pure_functions_collection_Pair_Impl<U, V>("");
                            }
                        });
                    }
                
                    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(Object l1, Object l2, Function0<? extends Pair<U, V>> pairBuilder)
                    {
                        return zipImpl((RichIterable<? extends U>) l1, (RichIterable<? extends V>) l2, pairBuilder);
                    }
                
                    public static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zip(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)
                    {
                        return zipImpl(l1, l2, pairBuilder);
                    }
                
                    private static <U, V> RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> zipImpl(RichIterable<? extends U> l1, RichIterable<? extends V> l2, final Function0<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>> pairBuilder)
                    {
                        return l1 == null || l2 == null ? FastList.<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V>>newList() : l1.zip(l2).collect(new DefendedFunction<org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V>, Pair<U, V>>()
                        {
                            @Override
                            public org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<U, V> valueOf(org.eclipse.collections.api.tuple.Pair<? extends U, ? extends V> pair)
                            {
                                return pairBuilder.value()._first(pair.getOne())._second(pair.getTwo());
                            }
                        });
                    }
                
                    public static Object dynamicMatchWith(Object obj, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>> funcs, Object var, ExecutionSupport es)
                    {
                        return FunctionsGen.dynamicMatchWith(obj, funcs, var, CoreGen.bridge, es);
                    }
                
                
                    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> newClass(String fullPathString, MetadataAccessor ma, SourceInformation si)
                    {
                        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
                        if (fullPath.isEmpty())
                        {
                            throw new PureExecutionException(null, "Cannot create a new Class: '" + fullPathString + "'");
                        }
                        String name = fullPath.getLast();
                        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
                        {
                            @Override
                            public Package valueOf(String s)
                            {
                                return new Package_Impl(s);
                            }
                        });
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> _class = new Root_meta_pure_metamodel_type_Class_Impl(name)._name(name)._package(_package);
                        return _class._classifierGenericType(
                                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                                                ._rawType(ma.getClass("Root::meta::pure::metamodel::type::Class"))
                                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(_class))))
                                ._generalizations(Lists.immutable.of(
                                        new Root_meta_pure_metamodel_relationship_Generalization_Impl("Anonymous_StripedId")
                                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(ma.getTopType()))
                                                ._specific(_class)));
                    }
                
                    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association newAssociation(String fullPathString, Property p1, Property p2, MetadataAccessor ma, SourceInformation si)
                    {
                        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
                        if (fullPath.isEmpty())
                        {
                            throw new PureExecutionException(null, "Cannot create a new Association: '" + fullPathString + "'");
                        }
                        String name = fullPath.getLast();
                        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
                        {
                            @Override
                            public Package valueOf(String s)
                            {
                                return new Package_Impl(s);
                            }
                        });
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = new Root_meta_pure_metamodel_relationship_Association_Impl(name)._name(name)._package(_package);
                        return _association._propertiesAdd(p1)._propertiesAdd(p2)._classifierGenericType(
                                new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                                        ._rawType(ma.getClass("Root::meta::pure::metamodel::relationship::Association")));
                    }
                
                    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> newEnumeration(final String fullPathString, RichIterable values, MetadataAccessor ma, SourceInformation si)
                    {
                        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
                        if (fullPath.isEmpty())
                        {
                            throw new PureExecutionException(null, "Cannot create a new Enumeration: '" + fullPathString + "'");
                        }
                        String name = fullPath.getLast();
                        String packageName = fullPath.subList(0, fullPath.size() - 1).makeString("::");
                        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
                        {
                            @Override
                            public Package valueOf(String s)
                            {
                                return new Package_Impl(s);
                            }
                        });
                        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> _enumeration = new Root_meta_pure_metamodel_type_Enumeration_Impl<Any>(name)._name(name)._package(_package);
                        return _enumeration._classifierGenericType(
                                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                                                ._rawType(ma.getClass("Root::meta::pure::metamodel::type::Enumeration"))
                                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(_enumeration))))
                                ._generalizations(Lists.immutable.of(
                                        new Root_meta_pure_metamodel_relationship_Generalization_Impl("Anonymous_StripedId")
                                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(ma.getClass("Root::meta::pure::metamodel::type::Enum")))
                                                ._specific(_enumeration)))
                                ._values(values.collect(new DefendedFunction<String, PureEnum>()
                                {
                                    public PureEnum valueOf(String valueName)
                                    {
                                        return new PureEnum(valueName, fullPathString);
                                    }
                                }));
                    }
                
                    public static PureMap getOpenVariables(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func)
                    {
                        return Pure.getOpenVariables(func, CoreGen.bridge);
                    }
                
                    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)
                    {
                        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, true, CoreGen.bridge, es);
                    }
                
                    public static Object reactivate(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, boolean allowJavaCompilation, ExecutionSupport es)
                    {
                        return Pure.reactivate(valueSpecification, lambdaOpenVariablesMap, allowJavaCompilation, CoreGen.bridge, es);
                    }
                
                    public static Object traceSpan(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function, String operationName, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function funcToGetTags, boolean tagsCritical)
                    {
                        return FunctionsGen.traceSpan(es, function, operationName, funcToGetTags, tagsCritical, CoreGen.bridge);
                    }
                
                
                }\
                """));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                // Cipher
                new Decrypt(),
                new Encrypt(),

                // Collection
                new Drop(),
                new Exists(),
                new Find(),
                new ForAll(),
                new Get(),
                new GroupBy(),
                new IndexOf(),
                new Last(),
                new RemoveAllOptimized(),
                new Repeat(),
                new Reverse(),
                new Slice(),
                new Take(),
                new Zip(),

                //Date
                new AdjustDate(),
                new DateDiff(),
                new DatePart(),
                new DayOfMonth(),
                new DayOfWeekNumber(),
                new DayOfYear(),
                new HasDay(),
                new HasHour(),
                new HasMinute(),
                new HasMonth(),
                new HasSecond(),
                new HasSubsecond(),
                new HasSubsecondWithAtLeastPrecision(),
                new Hour(),
                new Minute(),
                new MonthNumber(),
                new NewDate(),
                new Now(),
                new Second(),
                new Today(),
                new WeekOfYear(),
                new Year(),

                //Hash
                new Hash(),

                //IO
                new Http(),
                new ReadFile(),

                //Lang
                new MatchWith(),
                new MutateAdd(),
                new RawEvalProperty(),
                new RemoveOverride(),

                //Math
                new ArcCosine(),
                new ArcSine(),
                new ArcTangent(),
                new ArcTangent2(),
                new Cbrt(),
                new Ceiling(),
                new Cosine(),
                new CoTangent(),
                new Exp(),
                new Floor(),
                new Log(),
                new Log10(),
                new Mod(),
                new Pow(),
                new Rem(),
                new Round(),
                new RoundWithScale(),
                new Sign(),
                new Sine(),
                new Sqrt(),
                new StdDev(),
                new Tangent(),
                new ToDecimal(),
                new ToFloat(),

                // Meta
                new CanReactivateDynamically(),
                new CompileValueSpecification(),
                new Deactivate(),
                new EnumName(),
                new EnumValues(),
                new FunctionDescriptorToId(),
                new Generalizations(),
                new IsSourceReadOnly(),
                new IsValidFunctionDescriptor(),
                new NewAssociation(),
                new NewClass(),
                new NewEnumeration(),
                new NewLambdaFunction(),
                new NewProperty(),
                new NewQualifiedProperty(),
                new OpenVariableValues(),
                new Reactivate(),
                new SourceInformation(),
                new Stereotype(),
                new SubTypeOf(),
                new Tag(),

                //Runtime
                new CurrentUserId(),
                new IsOptionSet(),
                new Guid(),

                //String
                new ASCII(),
                new Char(),
                new Chunk(),
                new Contains(),
                new DecodeBase64(),
                new EncodeBase64(),
                new DecodeUrl(),
                new EncodeUrl(),
                new EndsWith(),
                new org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.IndexOf(),
                new org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.IndexOfWithFrom(),
                new LTrim(),
                new Matches(),
                new ParseBoolean(),
                new ParseDate(),
                new ParseFloat(),
                new ParseDecimal(),
                new ParseInteger(),
                new ReverseString(),
                new RTrim(),
                new ToLower(),
                new ToUpper(),
                new Trim(),

                //Tracing
                new TraceSpan(),

                // LegendTests
                new MayExecuteAlloyTest(),
                new MayExecuteLegendTest(),

                //Tools
                new Profile(),

                //Anonymous Collections
                new org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.anonymousCollections.map.Get(),
                new GetIfAbsentPutWithKey(),
                new GetMapStats(),
                new Keys(),
                new NewMap(),
                new KeyValues(),
                new NewMapWithProperties(),
                new Put(),
                new PutAllMaps(),
                new PutAllPairs(),
                new ReplaceAll(),
                new Values(),
                new ReplaceTreeNode()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_functions";
    }

    public static CompiledExtension extension()
    {
        return new FunctionsExtensionCompiled();
    }
}
