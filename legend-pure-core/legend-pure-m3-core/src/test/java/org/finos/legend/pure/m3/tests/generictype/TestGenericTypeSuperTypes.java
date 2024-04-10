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

package org.finos.legend.pure.m3.tests.generictype;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestGenericTypeSuperTypes extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testDSBCase()
    {
        compileTestSource("fromString.pure", """
                import test::*;
                
                Class test::Post
                {
                }
                
                Class test::Pre\s
                {
                }
                
                Class test::DataSetFilterOperation<F>
                {
                }
                
                Class test::DataSetComparisonPostFilterOperation<T|m> extends DataSetFilterOperation<Post>
                {
                     value : T[m];
                }
                
                Class test::DataSetCompositeFilterOperation<F> extends DataSetFilterOperation<F>
                {
                     rules : DataSetFilterOperation<F>[*];
                }
                """);
        CoreInstance genericType1 = Type.wrapGenericType(runtime.getCoreInstance("test::DataSetCompositeFilterOperation"), processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(runtime.getCoreInstance("test::Post"), processorSupport), processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::DataSetCompositeFilterOperation<test::Post>", "test::DataSetFilterOperation<test::Post>", M3Paths.Any), getSuperTypesAsStrings(genericType1));

        CoreInstance genericType2 = Type.wrapGenericType(runtime.getCoreInstance("test::DataSetComparisonPostFilterOperation"), processorSupport);
        Instance.addValueToProperty(genericType2, M3Properties.typeArguments, Type.wrapGenericType(runtime.getCoreInstance(M3Paths.Any), processorSupport), processorSupport);
        Instance.addValueToProperty(genericType2, M3Properties.multiplicityArguments, runtime.getCoreInstance(M3Paths.PureOne), processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::DataSetComparisonPostFilterOperation<" + M3Paths.Any + "|1>", "test::DataSetFilterOperation<test::Post>", M3Paths.Any), getSuperTypesAsStrings(genericType2));
    }

    @Test
    public void testWithMultiLevelTypeAndMultiplicityArguments()
    {
        compileTestSource("fromString.pure", """
                import test::*;
                
                Class test::A<T,U,V|a,b,c> {}
                Class test::B<W,X|d,e> extends A<W,X,String|d,e,1> {}
                Class test::C<Y|f> extends B<Y,Integer|f,*> {}
                """);

        CoreInstance genericType1 = Type.wrapGenericType(runtime.getCoreInstance("test::C"), processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(runtime.getCoreInstance(M3Paths.Date), processorSupport), processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.multiplicityArguments, runtime.getCoreInstance(M3Paths.ZeroOne), processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::C<Date|0..1>", "test::B<Date, Integer|0..1, *>", "test::A<Date, Integer, String|0..1, *, 1>", M3Paths.Any), getSuperTypesAsStrings(genericType1));
    }

    @Test
    public void testWithNestedTypeArguments()
    {
        compileTestSource("fromString.pure", """
                import test::*;
                
                Class test::A<W> {}
                Class test::B<X> {}
                Class test::C<Y> extends A<B<Y>> {}
                Class test::D<Z> extends test::C<Z> {}
                """);
        CoreInstance genericType1 = Type.wrapGenericType(runtime.getCoreInstance("test::D"), processorSupport);
        Instance.addValueToProperty(genericType1, M3Properties.typeArguments, Type.wrapGenericType(runtime.getCoreInstance(M3Paths.Boolean), processorSupport), processorSupport);
        Verify.assertListsEqual(Lists.mutable.with("test::D<Boolean>", "test::C<Boolean>", "test::A<test::B<Boolean>>", M3Paths.Any), getSuperTypesAsStrings(genericType1));
    }

    private MutableList<String> getSuperTypesAsStrings(CoreInstance genericType)
    {
        ListIterable<CoreInstance> superTypes = GenericType.getAllSuperTypesIncludingSelf(genericType, processorSupport);
        MutableList<String> result = FastList.newList(superTypes.size());
        for (CoreInstance superType : superTypes)
        {
            result.add(GenericType.print(superType, true, processorSupport));
        }
        return result;
    }
}
