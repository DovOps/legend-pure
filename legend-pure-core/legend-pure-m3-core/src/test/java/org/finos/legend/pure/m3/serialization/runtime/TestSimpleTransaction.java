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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestSimpleTransaction extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void clearRuntime()
    {
        runtime.delete("source2.pure");
    }

    @Test
    public void testSimpleGraph() throws Exception
    {
        compileTestSource("""
                Class myTest::Product
                {
                   name : String[1];
                }
                """);

        Assertions.assertEquals(1, processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());

        Source source = runtime.createInMemorySource("source2.pure", """
                Class myTest::Synonym
                {
                   name : String[1];
                }
                """);
        IncrementalCompilerTransaction transaction = runtime.getIncrementalCompiler().newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
            Assertions.assertEquals(2, processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        }
        Assertions.assertEquals(1, processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assertions.assertEquals(2, processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        }
        transaction.rollback();
        Assertions.assertEquals(1, processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
    }

    @Test
    public void testFunction() throws Exception
    {
        Source source = runtime.createInMemorySource("source2.pure", """
                function myFunc():Nil[0]\
                {\
                    print('ok',1);\
                }\
                """);
        IncrementalCompilerTransaction transaction = runtime.getIncrementalCompiler().newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
            // TODO fix this: if this is called, myFunc__Nil_0__ is cached in the context both inside and outside the scope of the transaction
//            Assert.assertNotNull(processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        }

        Assertions.assertNull(processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assertions.assertNotNull(processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        }

        transaction.commit();
        Assertions.assertNotNull(processorSupport.package_getByUserPath("myFunc__Nil_0_"));
    }
}
