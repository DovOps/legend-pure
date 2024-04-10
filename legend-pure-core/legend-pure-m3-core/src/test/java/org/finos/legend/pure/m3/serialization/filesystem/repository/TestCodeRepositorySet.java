// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCodeRepositorySet
{
    @Test
    public void testBaseRepositories()
    {
        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).build();
        Assertions.assertEquals(1, set.size());
        Assertions.assertEquals("platform", set.getRepositories().getAny().getName());
        Assertions.assertTrue(set.getRepository("platform") instanceof GenericCodeRepository);

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> CodeRepositorySet.newBuilder().withoutCodeRepository("platform"));
        Assertions.assertEquals("The code repository platform may not be removed", e.getMessage());
    }

    @Test
    public void testBuilder()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository badDependenciesRepo = new GenericCodeRepository("test_repo_bad_deps", "test3::.*", "platform", "non_existent");
        CodeRepositorySet set1 = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB).build();
        Assertions.assertEquals(3, set1.size());
        Assertions.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set1.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assertions.assertSame(testRepoA, set1.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set1.getRepository("test_repo_b"));

        CodeRepositorySet set2 = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository())
                .withCodeRepositories(testRepoA, testRepoB, badDependenciesRepo)
                .withoutCodeRepository("test_repo_bad_deps")
                .build();
        Assertions.assertEquals(3, set2.size());
        Assertions.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set2.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assertions.assertSame(testRepoA, set2.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set2.getRepository("test_repo_b"));

        Assertions.assertEquals(set1, set2);
    }

    @Test
    public void testBuilder_NameConflict()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository fakePlatformRepo = new GenericCodeRepository("platform", "meta::.*");
        GenericCodeRepository testRepoA2 = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");

        // Try to add with name conflict with platform
        IllegalStateException e1 = Assertions.assertThrows(IllegalStateException.class, () -> CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).withCodeRepositories(testRepoA, testRepoB, fakePlatformRepo));
        Assertions.assertEquals("The code repository platform already exists!", e1.getMessage());

        // Try to add with name conflict among new repos
        IllegalStateException e2 = Assertions.assertThrows(IllegalStateException.class, () -> CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).withCodeRepositories(testRepoA, testRepoB, testRepoA2));
        Assertions.assertEquals("The code repository test_repo_a already exists!", e2.getMessage());
    }

    @Test
    public void testBuilder_BadDependency()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository badDependenciesRepo = new GenericCodeRepository("test_repo_bad_deps", "test3::.*", "platform", "non_existent");

        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).withCodeRepositories(testRepoA, testRepoB, badDependenciesRepo);
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, builder::build);
        Assertions.assertEquals("The dependency 'non_existent' required by the Code Repository 'test_repo_bad_deps' can't be found!", e.getMessage());
    }

    @Test
    public void testBuilderFromManager()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository testRepoC = new GenericCodeRepository("test_repo_c", "test::c::.*", "platform", "test_repo_b");

        CodeRepositorySet set1 = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB).build();
        Assertions.assertEquals(3, set1.size());
        Assertions.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b"), set1.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assertions.assertSame(testRepoA, set1.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set1.getRepository("test_repo_b"));

        CodeRepositorySet set2 = CodeRepositorySet.newBuilder(set1).withCodeRepository(testRepoC).build();
        Assertions.assertEquals(4, set2.size());
        Assertions.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b", "test_repo_c"), set2.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assertions.assertSame(testRepoA, set2.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set2.getRepository("test_repo_b"));
        Assertions.assertSame(testRepoC, set2.getRepository("test_repo_c"));
        set1.forEach(repo1 -> Assertions.assertSame(repo1, set2.getRepository(repo1.getName())));
    }

    @Test
    public void testGetRepository()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).withCodeRepositories(testRepoA, testRepoB).build();

        Assertions.assertSame(testRepoA, set.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set.getRepository("test_repo_b"));

        Assertions.assertFalse(set.getOptionalRepository("test_repo_c").isPresent());
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> set.getRepository("test_repo_c"));
        Assertions.assertEquals("The code repository 'test_repo_c' can't be found!", e.getMessage());
    }

    @Test
    public void testHasRepository()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories()).withCodeRepositories(testRepoA, testRepoB).build();
        Assertions.assertTrue(set.hasRepository("platform"));
        Assertions.assertTrue(set.hasRepository("test_repo_a"));
        Assertions.assertTrue(set.hasRepository("test_repo_b"));
        Assertions.assertFalse(set.hasRepository("test_repo_c"));
    }

    @Test
    public void testSubset()
    {
        GenericCodeRepository testRepoA = new GenericCodeRepository("test_repo_a", "test::a::.*", "platform");
        GenericCodeRepository testRepoB = new GenericCodeRepository("test_repo_b", "test::b::.*", "platform", "test_repo_a");
        GenericCodeRepository testRepoC = new GenericCodeRepository("test_repo_c", "test::c::.*", "platform", "test_repo_b");

        CodeRepositorySet set = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB, testRepoC).build();
        Assertions.assertEquals(4, set.size());
        Assertions.assertEquals(Sets.fixedSize.with("platform", "test_repo_a", "test_repo_b", "test_repo_c"), set.getRepositories().collect(CodeRepository::getName, Sets.mutable.empty()));
        Assertions.assertSame(testRepoA, set.getRepository("test_repo_a"));
        Assertions.assertSame(testRepoB, set.getRepository("test_repo_b"));
        Assertions.assertSame(testRepoC, set.getRepository("test_repo_c"));

        // Full subset
        Assertions.assertSame(set, set.subset(set.getRepositoryNames()));
        Assertions.assertSame(set, set.subset("platform", "test_repo_a", "test_repo_b", "test_repo_c"));
        Assertions.assertSame(set, set.subset("test_repo_a", "test_repo_b", "test_repo_c"));
        Assertions.assertSame(set, set.subset("test_repo_a", "test_repo_c"));
        Assertions.assertSame(set, set.subset("test_repo_b", "test_repo_c"));
        Assertions.assertSame(set, set.subset("test_repo_c"));

        // Minimal subset
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).build(), set.subset());
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).build(), set.subset("platform"));

        // In between
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB).build(), set.subset("platform", "test_repo_a", "test_repo_b"));
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB).build(), set.subset("test_repo_a", "test_repo_b"));
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA, testRepoB).build(), set.subset("test_repo_b"));

        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA).build(), set.subset("platform", "test_repo_a"));
        Assertions.assertEquals(CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findPlatformCodeRepository()).withCodeRepositories(testRepoA).build(), set.subset("test_repo_a"));
    }
}
