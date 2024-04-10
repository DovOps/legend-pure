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

package org.finos.legend.pure.runtime.java.shared.hash;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HashingUtilTest
{
    @Test
    public void testMD5Hash()
    {
        Assertions.assertEquals("65A8E27D8879283831B664BD8B7F0AD4", HashingUtil.hash("Hello, World!", HashType.MD5).toUpperCase());
    }

    @Test
    public void testSHA1Hash()
    {
        Assertions.assertEquals("0A0A9F2A6772942557AB5355D76AF442F8F65E01", HashingUtil.hash("Hello, World!", HashType.SHA1).toUpperCase());
    }

    @Test
    public void testSHA256Hash()
    {
        Assertions.assertEquals("DFFD6021BB2BD5B0AF676290809EC3A53191DD81C7F70A4B28688A362182986F", HashingUtil.hash("Hello, World!", HashType.SHA256).toUpperCase());
    }
}
