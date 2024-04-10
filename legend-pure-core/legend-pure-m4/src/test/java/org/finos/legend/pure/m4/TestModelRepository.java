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

package org.finos.legend.pure.m4;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestModelRepository
{
    @Test
    public void testIsAnonymousInstanceName()
    {
        Assertions.assertTrue(ModelRepository.isAnonymousInstanceName("@_0000001234"));

        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName(null));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName(""));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("Class"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("Package"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@1234"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@abcd"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@_"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@_abcd"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@_a234"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@_1234a"));
        Assertions.assertFalse(ModelRepository.isAnonymousInstanceName("@_1_5_67"));
    }
}
