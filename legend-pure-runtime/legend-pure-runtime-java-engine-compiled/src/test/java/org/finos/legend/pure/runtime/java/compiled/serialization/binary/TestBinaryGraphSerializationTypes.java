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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestBinaryGraphSerializationTypes
{
    @Test
    public void testObjSerializationCode()
    {
        String classifier = "meta::pure::some::classifier";
        String id = "some_id";
        String name = "name";
        SourceInformation sourceInfo = new SourceInformation("file.pure", 1, 2, 3, 4, 5, 6);
        ListIterable<PropertyValue> properties = Lists.immutable.with(new PropertyValueOne("property", new Primitive("value")));

        byte codeNone = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, null, false));
        Assertions.assertFalse(BinaryGraphSerializationTypes.isEnum(codeNone));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasName(codeNone));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeNone));

        byte codeEnum = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, null, true));
        Assertions.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnum));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasName(codeEnum));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeEnum));

        byte codeEnumName = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, null, true));
        Assertions.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumName));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasName(codeEnumName));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumName));

        byte codeEnumSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, sourceInfo, true));
        Assertions.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumSource));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasName(codeEnumSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumSource));

        byte codeEnumNameSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, sourceInfo, true));
        Assertions.assertTrue(BinaryGraphSerializationTypes.isEnum(codeEnumNameSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasName(codeEnumNameSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeEnumNameSource));

        byte codeName = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, null, false));
        Assertions.assertFalse(BinaryGraphSerializationTypes.isEnum(codeName));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasName(codeName));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasSourceInfo(codeName));

        byte codeNameSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, name, properties, sourceInfo, false));
        Assertions.assertFalse(BinaryGraphSerializationTypes.isEnum(codeNameSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasName(codeNameSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeNameSource));

        byte codeSource = BinaryGraphSerializationTypes.getObjSerializationCode(Obj.newObj(classifier, id, null, properties, sourceInfo, false));
        Assertions.assertFalse(BinaryGraphSerializationTypes.isEnum(codeSource));
        Assertions.assertFalse(BinaryGraphSerializationTypes.hasName(codeSource));
        Assertions.assertTrue(BinaryGraphSerializationTypes.hasSourceInfo(codeSource));
    }
}
