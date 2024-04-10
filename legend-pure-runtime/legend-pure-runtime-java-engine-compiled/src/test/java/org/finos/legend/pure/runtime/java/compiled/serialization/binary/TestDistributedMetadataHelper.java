// Copyright 2021 Goldman Sachs
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

import org.eclipse.collections.impl.utility.ArrayIterate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestDistributedMetadataHelper
{
    @Test
    public void testValidateMetadataName()
    {
        Assertions.assertNull(DistributedMetadataHelper.validateMetadataNameIfPresent(null));

        IllegalArgumentException eNull = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataHelper.validateMetadataName(null));
        Assertions.assertEquals("Invalid metadata name: null", eNull.getMessage());

        IllegalArgumentException eEmpty = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataHelper.validateMetadataName(""));
        Assertions.assertEquals("Invalid metadata name: \"\"", eEmpty.getMessage());

        IllegalArgumentException eEmpty2 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataHelper.validateMetadataNameIfPresent(""));
        Assertions.assertEquals("Invalid metadata name: \"\"", eEmpty2.getMessage());


        IllegalArgumentException eInvalid = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataHelper.validateMetadataName("invalid name"));
        Assertions.assertEquals("Invalid metadata name: \"invalid name\"", eInvalid.getMessage());

        IllegalArgumentException eInvalid2 = Assertions.assertThrows(IllegalArgumentException.class, () -> DistributedMetadataHelper.validateMetadataNameIfPresent("invalid name"));
        Assertions.assertEquals("Invalid metadata name: \"invalid name\"", eInvalid2.getMessage());

        Assertions.assertEquals("valid_name", DistributedMetadataHelper.validateMetadataName("valid_name"));
        Assertions.assertEquals("valid_name", DistributedMetadataHelper.validateMetadataNameIfPresent("valid_name"));
    }

    @Test
    public void testIsValidMetadataName()
    {
        String[] validNames = {"abc", "_", "_abc_", "0123456789_AbC_xYz", "__", "\u0030\u0031\u0045", "0123456789_abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ"};
        Assertions.assertEquals(Collections.emptyList(), ArrayIterate.reject(validNames, DistributedMetadataHelper::isValidMetadataName), "should be valid");

        String[] invalidNames = {null, "", "xyz+abc", ".", "\u0080", "\u1234", "\u00EA", "a_\u9975_b", "$#abc"};
        Assertions.assertEquals(Collections.emptyList(), ArrayIterate.select(invalidNames, DistributedMetadataHelper::isValidMetadataName), "should be invalid");

        Assertions.assertTrue(DistributedMetadataHelper.isValidMetadataName("xyz+abc", 0, 3));
        Assertions.assertTrue(DistributedMetadataHelper.isValidMetadataName("xyz+abc", 4, 7));
        Assertions.assertTrue(DistributedMetadataHelper.isValidMetadataName("=xyz+abc", 1, 4));
        Assertions.assertTrue(DistributedMetadataHelper.isValidMetadataName("=xyzabc=", 2, 6));
        Assertions.assertTrue(DistributedMetadataHelper.isValidMetadataName("=xyzabc=", 1, 7));

        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("xyz+abc", 0, 0));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("xyz+abc", 2, 4));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("=xyz+abc", 0, 4));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("=xyzabc=", 2, 8));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("=xyzabc=", 1, 80000));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("=xyzabc=", 19, 80000));

        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName(null, 1, 80000));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("", 0, 80000));
        Assertions.assertFalse(DistributedMetadataHelper.isValidMetadataName("", 10, 80000));
    }

    @Test
    public void testGetMetadataIdPrefix()
    {
        Assertions.assertNull(DistributedMetadataHelper.getMetadataIdPrefix(null));
        Assertions.assertEquals("$$", DistributedMetadataHelper.getMetadataIdPrefix(""));
        Assertions.assertEquals("$abcd$", DistributedMetadataHelper.getMetadataIdPrefix("abcd"));
        Assertions.assertEquals("$_$", DistributedMetadataHelper.getMetadataIdPrefix("_"));
    }

    @Test
    public void testGetMetadataSpecificationsDirectory()
    {
        Assertions.assertEquals("metadata/specs/", DistributedMetadataHelper.getMetadataSpecificationsDirectory());
    }

    @Test
    public void testGetMetadataSpecificationFilePath()
    {
        Assertions.assertEquals("metadata/specs/abc.json", DistributedMetadataHelper.getMetadataSpecificationFilePath("abc"));
        Assertions.assertEquals("metadata/specs/_.json", DistributedMetadataHelper.getMetadataSpecificationFilePath("_"));
        Assertions.assertEquals("metadata/specs/core.json", DistributedMetadataHelper.getMetadataSpecificationFilePath("core"));
        Assertions.assertEquals("metadata/specs/platform.json", DistributedMetadataHelper.getMetadataSpecificationFilePath("platform"));
    }

    @Test
    public void testGetMetadataPartitionBinFilePath()
    {
        Assertions.assertEquals("metadata/bin/0.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath(null, 0));
        Assertions.assertEquals("metadata/bin/1.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath(null, 1));
        Assertions.assertEquals("metadata/bin/5706003.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath(null, 5706003));
        Assertions.assertEquals("metadata/bin/123456789.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath(null, 123456789));

        Assertions.assertEquals("metadata/bin/xyz/0.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath("xyz", 0));
        Assertions.assertEquals("metadata/bin/abc/1.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath("abc", 1));
        Assertions.assertEquals("metadata/bin/_/5706003.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath("_", 5706003));
        Assertions.assertEquals("metadata/bin/platform/123456789.bin", DistributedMetadataHelper.getMetadataPartitionBinFilePath("platform", 123456789));
    }

    @Test
    public void testGetClassifierIndexFilePath()
    {
        Assertions.assertEquals("metadata/classifiers/meta/pure/metamodel/type/Class.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::Class"));
        Assertions.assertEquals("metadata/classifiers/meta/pure/metamodel/relationship/Association.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::relationship::Association"));
        Assertions.assertEquals("metadata/classifiers/meta/pure/metamodel/type/Enumeration.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::Enumeration"));
        Assertions.assertEquals("metadata/classifiers/meta/pure/metamodel/type/generics/GenericType.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::generics::GenericType"));

        Assertions.assertEquals("metadata/classifiers/12345/meta/pure/metamodel/type/Class.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath("12345", "meta::pure::metamodel::type::Class"));
        Assertions.assertEquals("metadata/classifiers/core/meta/pure/metamodel/relationship/Association.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath("core", "meta::pure::metamodel::relationship::Association"));
        Assertions.assertEquals("metadata/classifiers/__/meta/pure/metamodel/type/Enumeration.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath("__", "meta::pure::metamodel::type::Enumeration"));
        Assertions.assertEquals("metadata/classifiers/platform/meta/pure/metamodel/type/generics/GenericType.idx", DistributedMetadataHelper.getMetadataClassifierIndexFilePath("platform", "meta::pure::metamodel::type::generics::GenericType"));
    }

    @Test
    public void testGetStringIndexFilePath()
    {
        Assertions.assertEquals("metadata/strings/classifiers.idx", DistributedMetadataHelper.getClassifierIdStringsIndexFilePath(null));
        Assertions.assertEquals("metadata/strings/platform/classifiers.idx", DistributedMetadataHelper.getClassifierIdStringsIndexFilePath("platform"));

        Assertions.assertEquals("metadata/strings/other.idx", DistributedMetadataHelper.getOtherStringsIndexFilePath(null));
        Assertions.assertEquals("metadata/strings/platform/other.idx", DistributedMetadataHelper.getOtherStringsIndexFilePath("platform"));

        Assertions.assertEquals("metadata/strings/other-0.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(null, 0));
        Assertions.assertEquals("metadata/strings/other-32768.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(null, 32768));
        Assertions.assertEquals("metadata/strings/other-65536.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(null, 65536));
        Assertions.assertEquals("metadata/strings/other-131072.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(null, 131072));
        Assertions.assertEquals("metadata/strings/other-1638400.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(null, 1638400));

        Assertions.assertEquals("metadata/strings/core/other-0.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath("core", 0));
        Assertions.assertEquals("metadata/strings/platform/other-32768.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath("platform", 32768));
        Assertions.assertEquals("metadata/strings/_ABC_/other-65536.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath("_ABC_", 65536));
        Assertions.assertEquals("metadata/strings/null/other-131072.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath("null", 131072));
        Assertions.assertEquals("metadata/strings/_-_/other-1638400.idx", DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath("_-_", 1638400));
    }
}
