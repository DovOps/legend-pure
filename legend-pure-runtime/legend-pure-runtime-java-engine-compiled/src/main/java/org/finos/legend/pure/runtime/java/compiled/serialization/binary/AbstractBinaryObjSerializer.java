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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValue;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValue;

import java.math.BigDecimal;

abstract class AbstractBinaryObjSerializer implements BinaryObjSerializer
{
    @Override
    public void serializeObj(Writer writer, Obj obj)
    {
        writer.writeByte(BinaryGraphSerializationTypes.getObjSerializationCode(obj));
        writeClassifier(writer, obj.getClassifier());
        writeIdentifier(writer, obj.getIdentifier());
        if (obj.getName() != null)
        {
            writeName(writer, obj.getName());
        }
        if (obj.getSourceInformation() != null)
        {
            writeSourceInformation(writer, obj.getSourceInformation());
        }
        writePropertyValues(writer, obj);
    }

    protected void writeSourceInformation(Writer writer, SourceInformation sourceInformation)
    {
        writeString(writer, sourceInformation.getSourceId());
        writer.writeInt(sourceInformation.getStartLine());
        writer.writeInt(sourceInformation.getStartColumn());
        writer.writeInt(sourceInformation.getLine());
        writer.writeInt(sourceInformation.getColumn());
        writer.writeInt(sourceInformation.getEndLine());
        writer.writeInt(sourceInformation.getEndColumn());
    }

    protected void writeIdentifier(Writer writer, String identifier)
    {
        writeString(writer, identifier);
    }

    protected void writeClassifier(Writer writer, String classifier)
    {
        writeString(writer, classifier);
    }

    protected void writeName(Writer writer, String name)
    {
        writeString(writer, name);
    }

    protected void writePropertyValues(Writer writer, Obj obj)
    {
        ListIterable<PropertyValue> propertyValues = obj.getPropertyValues();
        writer.writeInt(propertyValues.size());
        for (PropertyValue propertyValue : propertyValues)
        {
            writePropertyValue(writer, propertyValue);
        }
    }

    protected void writePropertyValue(Writer writer, PropertyValue propertyValue)
    {
        if (propertyValue instanceof PropertyValueMany many)
        {
            writer.writeBoolean(true);
            writeString(writer, many.getProperty());
            ListIterable<RValue> values = many.getValues();
            writer.writeInt(values.size());
            for (RValue rValue : values)
            {
                writeRValue(writer, rValue);
            }
        }
        else
        {
            PropertyValueOne propertyValueOne = (PropertyValueOne)propertyValue;
            writer.writeBoolean(false);
            writeString(writer, propertyValueOne.getProperty());
            writeRValue(writer, propertyValueOne.getValue());
        }
    }

    protected void writeRValue(Writer writer, RValue rValue)
    {
        if (rValue instanceof EnumRef enumeration)
        {
            writer.writeByte(BinaryGraphSerializationTypes.ENUM_REF);
            writeString(writer, enumeration.getEnumerationId());
            writeString(writer, enumeration.getEnumName());
        }
        else if (rValue instanceof ObjRef objRef)
        {
            writer.writeByte(BinaryGraphSerializationTypes.OBJ_REF);
            writeString(writer, objRef.getClassifierId());
            writeString(writer, objRef.getId());
        }
        else if (rValue instanceof Primitive primitive)
        {
            Object value = primitive.getValue();
            if (value instanceof Boolean boolean1)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_BOOLEAN);
                writer.writeBoolean(boolean1);
            }
            else if (value instanceof Double double1)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DOUBLE);
                writer.writeDouble(double1);
            }
            else if (value instanceof Long long1)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_LONG);
                writer.writeLong(long1);
            }
            else if (value instanceof String string)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_STRING);
                writeString(writer, string);
            }
            else if (value instanceof PureDate)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DATE);
                writeString(writer, value.toString());
            }
            else if (value instanceof BigDecimal decimal)
            {
                writer.writeByte(BinaryGraphSerializationTypes.PRIMITIVE_DECIMAL);
                writer.writeString(decimal.toPlainString());
            }
            else
            {
                throw new UnsupportedOperationException("Unsupported primitive type: " + value.getClass().getSimpleName());
            }
        }
        else
        {
            throw new UnsupportedOperationException("serialization for RValue type not supported: " + rValue.getClass().getName());
        }
    }

    protected abstract void writeString(Writer writer, String string);
}
