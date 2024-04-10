// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class TestDistributedMetadataSpecification
{
    @TempDir
    public static File TMP;

    @Test
    public void testWithNoDependencies()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("abc");
        Assertions.assertEquals("abc", metadata.getName());
        Assertions.assertEquals(Collections.emptySet(), metadata.getDependencies());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testWithDependenciesAsVarArgs()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("def", "ghi", "jkl");
        Assertions.assertEquals("def", metadata.getName());
        Assertions.assertEquals(Sets.mutable.with("ghi", "jkl"), metadata.getDependencies());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testWithDependenciesAsIterable()
    {
        DistributedMetadataSpecification metadata = DistributedMetadataSpecification.newSpecification("mno", Sets.mutable.with("pqr", "stu"));
        Assertions.assertEquals("mno", metadata.getName());
        Assertions.assertEquals(Sets.mutable.with("pqr", "stu"), metadata.getDependencies());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().add("something"));
    }

    @Test
    public void testInvalidMetadataName()
    {
        String[] invalidNames = {"", "$$%", "invalid name"};
        for (String name : invalidNames)
        {
            IllegalArgumentException e1 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(name));
            Assertions.assertEquals("Invalid metadata name: \"" + name + "\"", e1.getMessage(), name);

            IllegalArgumentException e2 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(name, "some_dependency"));
            Assertions.assertEquals("Invalid metadata name: \"" + name + "\"", e2.getMessage(), name);
        }

        IllegalArgumentException e1 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(null));
        Assertions.assertEquals("Invalid metadata name: null", e1.getMessage());

        IllegalArgumentException e2 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification(null, "some_other_dependency"));
        Assertions.assertEquals("Invalid metadata name: null", e2.getMessage());
    }

    @Test
    public void testInvalidDependencies()
    {
        IllegalArgumentException e1 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification("a", "b", "c", null, "d"));
        Assertions.assertEquals("Invalid dependency: null", e1.getMessage());

        IllegalArgumentException e2 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataSpecification.newSpecification("a", "b", "999abc#", null, "$", "d"));
        Assertions.assertEquals("Invalid dependencies: null, \"$\", \"999abc#\"", e2.getMessage());
    }

    @Test
    public void testReadWrite() throws IOException
    {
        Set<DistributedMetadataSpecification> metadata = Sets.mutable.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"), DistributedMetadataSpecification.newSpecification("ghi", "abc", "def"));

        Path directory = newFolder(TMP, "junit").toPath();
        List<String> paths = DistributedMetadataSpecification.writeSpecifications(directory, metadata);
        Assertions.assertEquals(Sets.mutable.with("metadata/specs/abc.json", "metadata/specs/def.json", "metadata/specs/ghi.json"), Sets.mutable.withAll(paths));

        for (DistributedMetadataSpecification m : metadata)
        {
            Path file = directory.resolve(Paths.get("metadata", "specs", m.getName() + ".json"));
            DistributedMetadataSpecification loaded = DistributedMetadataSpecification.readSpecification(file);
            Assertions.assertEquals(m, loaded, m.getName());
        }
    }

    @Test
    public void testLoadMetadata_CurrentClassLoader()
    {
        Assertions.assertEquals(Collections.emptyList(), ListIterate.select(DistributedMetadataSpecification.loadAllSpecifications(Thread.currentThread().getContextClassLoader()), x -> !x.getName().equals("platform")));

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(Thread.currentThread().getContextClassLoader(), "non_existent"));
        Assertions.assertEquals("""
                Cannot find metadata "non_existent" (resource name "metadata/specs/non_existent.json")
                Directly asked for: [non_existent]
                Loaded up to now (with dependencies): {}
                The requested repos are coming from PAR projects scanning. You may not have included the project containing the distributed metadata for Java generation.\
                """, e.getMessage());
    }

    @Test
    public void testLoadMetadata_Directories() throws IOException
    {
        List<DistributedMetadataSpecification> dir1Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"));
        List<DistributedMetadataSpecification> dir2Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("ghi", "def"), DistributedMetadataSpecification.newSpecification("jkl", "xyz"));

        Path dir1 = newFolder(TMP, "junit").toPath();
        DistributedMetadataSpecification.writeSpecifications(dir1, dir1Metadata);

        Path dir2 = newFolder(TMP, "junit").toPath();
        DistributedMetadataSpecification.writeSpecifications(dir2, dir2Metadata);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{dir1.toUri().toURL(), dir2.toUri().toURL()}))
        {
            Assertions.assertEquals(Sets.mutable.withAll(dir1Metadata).withAll(dir2Metadata), Sets.mutable.withAll(DistributedMetadataSpecification.loadAllSpecifications(classLoader)).select(r -> !r.getName().equals("platform")));

            Assertions.assertEquals(Lists.mutable.with(DistributedMetadataSpecification.newSpecification("abc")), DistributedMetadataSpecification.loadSpecifications(classLoader, "abc"));

            Assertions.assertEquals(Sets.mutable.withAll(dir1Metadata).with(dir2Metadata.get(0)), Sets.mutable.withAll(DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi")));

            RuntimeException e1 = Assertions.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "jkl"));
            Assertions.assertEquals("""
                    Cannot find metadata "xyz" (resource name "metadata/specs/xyz.json")
                    Directly asked for: [ghi, jkl]
                    Loaded up to now (with dependencies): {jkl=[xyz]}
                    The requested repos are coming from PAR projects scanning. You may not have included the project containing the distributed metadata for Java generation.\
                    """, e1.getMessage());

            RuntimeException e2 = Assertions.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "mno"));
            Assertions.assertEquals("""
                    Cannot find metadata "mno" (resource name "metadata/specs/mno.json")
                    Directly asked for: [ghi, mno]
                    Loaded up to now (with dependencies): {}
                    The requested repos are coming from PAR projects scanning. You may not have included the project containing the distributed metadata for Java generation.\
                    """, e2.getMessage());
        }
    }

    @Test
    public void testLoadMetadata_Jars() throws IOException
    {
        List<DistributedMetadataSpecification> jar1Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("abc"), DistributedMetadataSpecification.newSpecification("def", "abc"));
        List<DistributedMetadataSpecification> jar2Metadata = Lists.fixedSize.with(DistributedMetadataSpecification.newSpecification("ghi", "def"), DistributedMetadataSpecification.newSpecification("jkl", "xyz"));

        Path dir = newFolder(TMP, "junit").toPath();
        Path jar1 = dir.resolve("jar1.jar");
        Path jar2 = dir.resolve("jar2.jar");

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar1))))
        {
            jarStream.putNextEntry(new ZipEntry("metadata/"));
            jarStream.closeEntry();
            jarStream.putNextEntry(new ZipEntry("metadata/specs/"));
            jarStream.closeEntry();
            DistributedMetadataSpecification.writeSpecifications(jarStream, jar1Metadata);
        }

        try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar2))))
        {
            jarStream.putNextEntry(new ZipEntry("metadata/"));
            jarStream.closeEntry();
            jarStream.putNextEntry(new ZipEntry("metadata/specs/"));
            jarStream.closeEntry();
            DistributedMetadataSpecification.writeSpecifications(jarStream, jar2Metadata);
        }

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar1.toUri().toURL(), jar2.toUri().toURL()}))
        {
            Assertions.assertEquals(Sets.mutable.withAll(jar1Metadata).withAll(jar2Metadata), Sets.mutable.withAll(DistributedMetadataSpecification.loadAllSpecifications(classLoader)).select(r -> !r.getName().equals("platform")));

            Assertions.assertEquals(Lists.mutable.with(DistributedMetadataSpecification.newSpecification("abc")), DistributedMetadataSpecification.loadSpecifications(classLoader, "abc"));

            Assertions.assertEquals(Sets.mutable.withAll(jar1Metadata).with(jar2Metadata.get(0)), Sets.mutable.withAll(DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi")));

            RuntimeException e1 = Assertions.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "jkl"));
            Assertions.assertEquals("""
                    Cannot find metadata "xyz" (resource name "metadata/specs/xyz.json")
                    Directly asked for: [ghi, jkl]
                    Loaded up to now (with dependencies): {jkl=[xyz]}
                    The requested repos are coming from PAR projects scanning. You may not have included the project containing the distributed metadata for Java generation.\
                    """, e1.getMessage());

            RuntimeException e2 = Assertions.assertThrows(RuntimeException.class, () -> DistributedMetadataSpecification.loadSpecifications(classLoader, "ghi", "mno"));
            Assertions.assertEquals("""
                    Cannot find metadata "mno" (resource name "metadata/specs/mno.json")
                    Directly asked for: [ghi, mno]
                    Loaded up to now (with dependencies): {}
                    The requested repos are coming from PAR projects scanning. You may not have included the project containing the distributed metadata for Java generation.\
                    """, e2.getMessage());
        }
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
