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

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m3.serialization.runtime.cache.CompressedMemoryPureGraphCache;
import org.finos.legend.pure.m3.serialization.runtime.cache.PureGraphCache;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPureRuntime
{
    @Test
    public void testLoadSourceDoesNotReload()
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> runtime.loadSource("/platform/pure/grammar/m3.pure"));
        Assertions.assertEquals("/platform/pure/grammar/m3.pure is already loaded", e.getMessage());
    }

    @Test
    public void testM3PureInstancesPopulated()
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();

        Source m3Pure = runtime.getSourceById("/platform/pure/grammar/m3.pure");
        Assertions.assertNotNull(m3Pure);
        Assertions.assertTrue(m3Pure.getNewInstances().notEmpty(), "No instances registered in m3.pure");
    }

    @Test
    public void testInstancesInContextDuringCaching()
    {
        MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()));

        PureGraphCache cache = new CompressedMemoryPureGraphCache();
        Assertions.assertFalse(cache.getCacheState().isCached());
        PureRuntime runtime = new PureRuntimeBuilder(codeStorage).withCache(cache).buildAndTryToInitializeFromCache();
        runtime.loadAndCompileCore();
        cache.cacheRepoAndSources();
        Assertions.assertTrue(cache.getCacheState().isCached());
        Assertions.assertTrue(cache.getCacheState().getCurrentCacheSize() > 0);

        PureRuntime newRuntime = new PureRuntimeBuilder(codeStorage).withCache(cache).buildAndTryToInitializeFromCache();
        Assertions.assertEquals(runtime.getContext().getAllInstances().collect(CoreInstance.GET_NAME).toSortedList(), newRuntime.getContext().getAllInstances().collect(CoreInstance.GET_NAME).toSortedList());
    }

    @Test
    public void testThreadLocalState()
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();

        IncrementalCompilerTransaction transaction = runtime.getIncrementalCompiler().newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Source source = new Source("test", false, true, "native function makeString(s:Any[*]):String[1];function myFuncThreadLocal():String[1]{[1,2,3]->filter(a|$a == 2)->makeString()}");
            runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
        }

        // FIXME: prevent this from entering the Context
        FunctionAccessor currentThreadFunctionAccessor = new FunctionAccessor(runtime);
        currentThreadFunctionAccessor.run();
        Assertions.assertNull(currentThreadFunctionAccessor.getFunctionInstance(), "Function is not found");

        FunctionAccessor otherThreadFunctionAccessor = new FunctionAccessor(runtime);

        try
        {
            Thread t = new Thread(otherThreadFunctionAccessor);
            t.start();
            t.join();
        }
        catch (InterruptedException ignore)
        {
            // ignore
        }

        Assertions.assertNull(otherThreadFunctionAccessor.getFunctionInstance(), "Function is not found");

        transaction.commit();

        currentThreadFunctionAccessor.run();
        Assertions.assertNotNull(currentThreadFunctionAccessor.getFunctionInstance(), "Function is found");

        Assertions.assertNotNull(runtime.getFunction("myFuncThreadLocal():String[1]"));
        try
        {
            Thread t = new Thread(otherThreadFunctionAccessor);
            t.start();
            t.join();
        }
        catch (InterruptedException ignore)
        {
            // ignore
        }

        Assertions.assertNotNull(otherThreadFunctionAccessor.getFunctionInstance(), "Another thread can access core instances that have been committed");
    }

    @Test
    public void testCompiles()
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository(), GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform")))).build();
        runtime.loadAndCompileCore();
        runtime.createInMemoryAndCompile(AbstractPureTestWithCoreCompiledPlatform.EXTRA);

        Assertions.assertTrue(runtime.compiles("1 + 2"));
        Assertions.assertTrue(runtime.compiles("split('the quick brown fox', ' ')"));
        Assertions.assertFalse(runtime.compiles("1 + 'the quick brown fox'"));
        Assertions.assertFalse(runtime.compiles("'the quick brown"));
        Assertions.assertFalse(runtime.compiles("asjkgljasdfjhasgasdsfdgrgrefrewfreswfreawfe"));
    }

    @Test
    public void testPartialCompilation()
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();

        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () -> runtime.createInMemoryAndCompile(
                Tuples.pair("/platform/testFile.pure", "function meta::pure::testFn():String[1] {'the quick brown fox'}"),
                Tuples.pair("testBad.pure", "function sandbox::testFn2():Integer[1] { 1 + '7'}")
        ));

        Assertions.assertNotNull(e.getSourceInformation());
        Assertions.assertEquals("testBad.pure", e.getSourceInformation().getSourceId());

        Source testFile = runtime.getSourceById("/platform/testFile.pure");
        Assertions.assertNotNull(testFile);
        Assertions.assertTrue(testFile.isCompiled());

        Source testBad = runtime.getSourceById("testBad.pure");
        Assertions.assertNotNull(testBad);
        Assertions.assertFalse(testBad.isCompiled());
    }

    private static class FunctionAccessor implements Runnable
    {
        private final PureRuntime runtime;
        private CoreInstance functionInstance;

        private FunctionAccessor(PureRuntime runtime)
        {
            this.runtime = runtime;
        }

        @Override
        public void run()
        {
            this.functionInstance = this.runtime.getFunction("myFuncThreadLocal():String[1]");
        }

        public CoreInstance getFunctionInstance()
        {
            return this.functionInstance;
        }
    }
}
