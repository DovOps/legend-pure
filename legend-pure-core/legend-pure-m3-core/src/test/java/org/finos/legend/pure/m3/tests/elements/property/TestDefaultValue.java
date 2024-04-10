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

package org.finos.legend.pure.m3.tests.elements.property;

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDefaultValue extends AbstractPureTestWithCoreCompiledPlatform
{

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @AfterEach
    public void cleanRuntime()
    {
        runtime.delete("defaultValueSource.pure");
        runtime.compile();
    }

    @Test
    public void testDefaultValue()
    {
        compileTestSource("defaultValueSource.pure", """
import test::*;
Class my::exampleRootType
{
}
Class my::exampleSubType extends my::exampleRootType
{
}
Enum test::EnumWithDefault
{
   DefaultValue,
   AnotherValue
}
Class test::A
{
   stringProperty:String[1] = 'default';
   classProperty:my::exampleRootType[1] = ^my::exampleRootType();
   enumProperty:test::EnumWithDefault[1] = test::EnumWithDefault.DefaultValue;
   floatProperty:Float[1] = 0.12;
   inheritProperty:Number[1] = 0.12;
   booleanProperty:Boolean[1] = false;
   integerProperty:Integer[1] = 0;
   collectionProperty:String[1..*] = ['one', 'two'];
   enumCollection:EnumWithDefault[1..*] = [EnumWithDefault.DefaultValue, EnumWithDefault.AnotherValue];
   classCollection:my::exampleRootType[1..4] = [^my::exampleRootType(), ^my::exampleSubType()];
   singleProperty:String[1] = ['one'];
   anyProperty:Any[1] = 'anyString';
}

"""
        );

        CoreInstance classA = runtime.getCoreInstance("test::A");

        CoreInstance stringProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "stringProperty");
        CoreInstance classProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "classProperty");
        CoreInstance enumProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "enumProperty");
        CoreInstance floatProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "floatProperty");
        CoreInstance inheritProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "inheritProperty");
        CoreInstance booleanProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "booleanProperty");
        CoreInstance integerProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "integerProperty");
        CoreInstance collectionProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "collectionProperty");
        CoreInstance enumCollection = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "enumCollection");
        CoreInstance classCollection = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "classCollection");
        CoreInstance singleProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "singleProperty");
        CoreInstance anyProperty = classA.getValueInValueForMetaPropertyToMany(M3Properties.properties, "anyProperty");

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$stringProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$stringProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                String instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$stringProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        default instance String\
                """, stringProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$enumProperty$1 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$enumProperty$1 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                EnumWithDefault instance Enumeration
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance SimpleFunctionExpression
                                    func(Property):
                                        extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction
                                    functionName(Property):
                                        extractEnumValue instance String
                                    genericType(Property):
                                        Anonymous_StripedId instance InferredGenericType
                                            rawType(Property):
                                                EnumWithDefault instance Enumeration
                                    importGroup(Property):
                                        import_defaultValueSource_pure_1 instance ImportGroup
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    parametersValues(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Enumeration instance Class
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance InstanceValue
                                                                    [... >5]
                                                            propertyName(Property):
                                                                genericType instance String
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                EnumWithDefault instance Enumeration
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                    functionExpression(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                    offset(Property):
                                                        0 instance Integer
                                            values(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    idOrPath(Property):
                                                        test::EnumWithDefault instance String
                                                    importGroup(Property):
                                                        import_defaultValueSource_pure_1 instance ImportGroup
                                                    resolvedNode(Property):
                                                        EnumWithDefault instance Enumeration
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        String instance PrimitiveType
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                    functionExpression(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                    offset(Property):
                                                        1 instance Integer
                                            values(Property):
                                                DefaultValue instance String
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$enumProperty$1 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer\
                """, enumProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$booleanProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$booleanProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Boolean instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Boolean instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$booleanProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        false instance Boolean\
                """, booleanProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$classProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$classProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    [... >5]
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance SimpleFunctionExpression
                                    func(Property):
                                        new_Class_1__String_1__T_1_ instance NativeFunction
                                    functionName(Property):
                                        new instance String
                                    genericType(Property):
                                        Anonymous_StripedId instance InferredGenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance ImportStub
                                                    idOrPath(Property):
                                                        my::exampleRootType instance String
                                                    importGroup(Property):
                                                        import_defaultValueSource_pure_1 instance ImportGroup
                                                    resolvedNode(Property):
                                                        exampleRootType instance Class
                                                            classifierGenericType(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            generalizations(Property):
                                                                Anonymous_StripedId instance Generalization
                                                                    [... >5]
                                                            name(Property):
                                                                exampleRootType instance String
                                                            package(Property):
                                                                my instance Package
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                            specializations(Property):
                                                                Anonymous_StripedId instance Generalization
                                                                    [... >5]
                                    importGroup(Property):
                                        import_defaultValueSource_pure_1 instance ImportGroup
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    parametersValues(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        Class instance Class
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance InstanceValue
                                                                    [... >5]
                                                            propertyName(Property):
                                                                genericType instance String
                                                    typeArguments(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Anonymous_StripedId instance ImportStub
                                                                    [... >5]
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                    functionExpression(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                    offset(Property):
                                                        0 instance Integer
                                            values(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        String instance PrimitiveType
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance ParameterValueSpecificationContext
                                                    functionExpression(Property):
                                                        Anonymous_StripedId instance SimpleFunctionExpression
                                                    offset(Property):
                                                        1 instance Integer
                                            values(Property):
                                                 instance String
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$classProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer\
                """, classProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$integerProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$integerProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Integer instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Integer instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$integerProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        0 instance Integer\
                """, integerProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$floatProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$floatProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Float instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Float instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$floatProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        0.12 instance Float\
                """, floatProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$inheritProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$inheritProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                Float instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Float instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$inheritProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        0.12 instance Float\
                """, inheritProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$collectionProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$collectionProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        Anonymous_StripedId instance Multiplicity
                                                            lowerBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                            upperBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                String instance PrimitiveType
                                    multiplicity(Property):
                                        Anonymous_StripedId instance Multiplicity
                                            lowerBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                            upperBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$collectionProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        String instance PrimitiveType
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        0 instance Integer
                                            values(Property):
                                                one instance String
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        String instance PrimitiveType
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        1 instance Integer
                                            values(Property):
                                                two instance String\
                """, collectionProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$enumCollection$2 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$enumCollection$2 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        Anonymous_StripedId instance Multiplicity
                                                            lowerBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                            upperBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                EnumWithDefault instance Enumeration
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                EnumWithDefault instance Enumeration
                                    multiplicity(Property):
                                        Anonymous_StripedId instance Multiplicity
                                            lowerBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                            upperBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$enumCollection$2 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        Anonymous_StripedId instance SimpleFunctionExpression
                                            func(Property):
                                                extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction
                                            functionName(Property):
                                                extractEnumValue instance String
                                            genericType(Property):
                                                Anonymous_StripedId instance InferredGenericType
                                                    rawType(Property):
                                                        EnumWithDefault instance Enumeration
                                            importGroup(Property):
                                                import_defaultValueSource_pure_1 instance ImportGroup
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            parametersValues(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Enumeration instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                0 instance Integer
                                                    values(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                EnumWithDefault instance String
                                                            importGroup(Property):
                                                                import_defaultValueSource_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                EnumWithDefault instance Enumeration
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                1 instance Integer
                                                    values(Property):
                                                        DefaultValue instance String
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        0 instance Integer
                                        Anonymous_StripedId instance SimpleFunctionExpression
                                            func(Property):
                                                extractEnumValue_Enumeration_1__String_1__T_1_ instance NativeFunction
                                            functionName(Property):
                                                extractEnumValue instance String
                                            genericType(Property):
                                                Anonymous_StripedId instance InferredGenericType
                                                    rawType(Property):
                                                        EnumWithDefault instance Enumeration
                                            importGroup(Property):
                                                import_defaultValueSource_pure_1 instance ImportGroup
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            parametersValues(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Enumeration instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                0 instance Integer
                                                    values(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                EnumWithDefault instance String
                                                            importGroup(Property):
                                                                import_defaultValueSource_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                EnumWithDefault instance Enumeration
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                1 instance Integer
                                                    values(Property):
                                                        AnotherValue instance String
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        1 instance Integer\
                """, enumCollection.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$classCollection$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$classCollection$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        Anonymous_StripedId instance Multiplicity
                                                            lowerBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                            upperBound(Property):
                                                                Anonymous_StripedId instance MultiplicityValue
                                                                    [... >5]
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                exampleRootType instance Class
                                                                    [... >5]
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                exampleRootType instance Class
                                                    classifierGenericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Class instance Class
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                    generalizations(Property):
                                                        Anonymous_StripedId instance Generalization
                                                            general(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            specific(Property):
                                                                exampleRootType instance Class
                                                                    [... >5]
                                                    name(Property):
                                                        exampleRootType instance String
                                                    package(Property):
                                                        my instance Package
                                                    referenceUsages(Property):
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance InferredGenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                        Anonymous_StripedId instance ReferenceUsage
                                                            offset(Property):
                                                                0 instance Integer
                                                            owner(Property):
                                                                Anonymous_StripedId instance InferredGenericType
                                                                    [... >5]
                                                            propertyName(Property):
                                                                rawType instance String
                                                    specializations(Property):
                                                        Anonymous_StripedId instance Generalization
                                                            general(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                            specific(Property):
                                                                exampleSubType instance Class
                                                                    [... >5]
                                    multiplicity(Property):
                                        Anonymous_StripedId instance Multiplicity
                                            lowerBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                            upperBound(Property):
                                                Anonymous_StripedId instance MultiplicityValue
                                                    value(Property):
                                                        2 instance Integer
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$classCollection$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        Anonymous_StripedId instance SimpleFunctionExpression
                                            func(Property):
                                                new_Class_1__String_1__T_1_ instance NativeFunction
                                            functionName(Property):
                                                new instance String
                                            genericType(Property):
                                                Anonymous_StripedId instance InferredGenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                my::exampleRootType instance String
                                                            importGroup(Property):
                                                                import_defaultValueSource_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                exampleRootType instance Class
                                                                    [... >5]
                                            importGroup(Property):
                                                import_defaultValueSource_pure_1 instance ImportGroup
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            parametersValues(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Class instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                0 instance Integer
                                                    values(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                1 instance Integer
                                                    values(Property):
                                                         instance String
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        0 instance Integer
                                        Anonymous_StripedId instance SimpleFunctionExpression
                                            func(Property):
                                                new_Class_1__String_1__T_1_ instance NativeFunction
                                            functionName(Property):
                                                new instance String
                                            genericType(Property):
                                                Anonymous_StripedId instance InferredGenericType
                                                    rawType(Property):
                                                        Anonymous_StripedId instance ImportStub
                                                            idOrPath(Property):
                                                                my::exampleSubType instance String
                                                            importGroup(Property):
                                                                import_defaultValueSource_pure_1 instance ImportGroup
                                                            resolvedNode(Property):
                                                                exampleSubType instance Class
                                                                    [... >5]
                                            importGroup(Property):
                                                import_defaultValueSource_pure_1 instance ImportGroup
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            parametersValues(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                Class instance Class
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                                                            typeArguments(Property):
                                                                Anonymous_StripedId instance GenericType
                                                                    [... >5]
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                0 instance Integer
                                                    values(Property):
                                                Anonymous_StripedId instance InstanceValue
                                                    genericType(Property):
                                                        Anonymous_StripedId instance GenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                    multiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    usageContext(Property):
                                                        Anonymous_StripedId instance ParameterValueSpecificationContext
                                                            functionExpression(Property):
                                                                Anonymous_StripedId instance SimpleFunctionExpression
                                                                    [... >5]
                                                            offset(Property):
                                                                1 instance Integer
                                                    values(Property):
                                                         instance String
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        1 instance Integer\
                """, classCollection.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$singleProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$singleProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                String instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$singleProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        Anonymous_StripedId instance InstanceValue
                                            genericType(Property):
                                                Anonymous_StripedId instance GenericType
                                                    rawType(Property):
                                                        String instance PrimitiveType
                                            multiplicity(Property):
                                                PureOne instance PackageableMultiplicity
                                            usageContext(Property):
                                                Anonymous_StripedId instance InstanceValueSpecificationContext
                                                    instanceValue(Property):
                                                        Anonymous_StripedId instance InstanceValue
                                                    offset(Property):
                                                        0 instance Integer
                                            values(Property):
                                                one instance String\
                """, singleProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));

        Assertions.assertEquals("""
                Anonymous_StripedId instance DefaultValue
                    functionDefinition(Property):
                        defaultValue$test_A$anyProperty$0 instance LambdaFunction
                            classifierGenericType(Property):
                                Anonymous_StripedId instance GenericType
                                    rawType(Property):
                                        LambdaFunction instance Class
                                    typeArguments(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                Anonymous_StripedId instance FunctionType
                                                    function(Property):
                                                        defaultValue$test_A$anyProperty$0 instance LambdaFunction
                                                    returnMultiplicity(Property):
                                                        PureOne instance PackageableMultiplicity
                                                    returnType(Property):
                                                        Anonymous_StripedId instance InferredGenericType
                                                            rawType(Property):
                                                                String instance PrimitiveType
                                                            referenceUsages(Property):
                                                                Anonymous_StripedId instance ReferenceUsage
                                                                    [... >5]
                            expressionSequence(Property):
                                Anonymous_StripedId instance InstanceValue
                                    genericType(Property):
                                        Anonymous_StripedId instance GenericType
                                            rawType(Property):
                                                String instance PrimitiveType
                                    multiplicity(Property):
                                        PureOne instance PackageableMultiplicity
                                    usageContext(Property):
                                        Anonymous_StripedId instance ExpressionSequenceValueSpecificationContext
                                            functionDefinition(Property):
                                                defaultValue$test_A$anyProperty$0 instance LambdaFunction
                                            offset(Property):
                                                0 instance Integer
                                    values(Property):
                                        anyString instance String\
                """, anyProperty.getValueForMetaPropertyToOne(M3Properties.defaultValue).printWithoutDebug("", 5));
    }

    @Test
    public void testDefaultValueWithUnsupportedType()
    {
        PureParserException e = Assertions.assertThrows(PureParserException.class, () ->
                compileTestSource("defaultValueSource.pure", """
                        import test::*;
                        import meta::pure::metamodel::constraint::*;
                        Class test::A
                        {
                           stringProperty:Boolean[1] = {x: Number[1] | $x < 10};
                        }
                        """));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: '{'", 5, 32, e);
    }

    @Test
    public void testDefaultValueWithNotMatchingType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () ->
                compileTestSource("defaultValueSource.pure", """
                        import test::*;
                        Class test::A
                        {
                           stringProperty:String[1] = false;
                        }
                        """));
        assertPureException(PureCompilationException.class, "Default value for property: 'stringProperty' / Type Error: 'Boolean' not a subtype of 'String'", 4, 31, e);
    }

    @Test
    public void testDefaultValueWithNotMatchingClassType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () ->
                compileTestSource("defaultValueSource.pure", """
import test::*;
Class my::A
{
}
Class my::B\s
{
}
Class test::C
{
   classProperty: my::A[1] = ^my::B();
}
"""));
        assertPureException(PureCompilationException.class, "Default value for property: 'classProperty' / Type Error: 'B' not a subtype of 'A'", 10, 30, e);
    }

    @Test
    public void testDefaultValueForOptionalProperty()
    {
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, () ->
                compileTestSource("defaultValueSource.pure", """
                        import test::*;
                        Class test::A
                        {
                           stringProperty: String[0..4] = 'optional';
                        }
                        """));
        assertPureException(PureCompilationException.class, "Default values are supported only for mandatory fields, and property 'stringProperty' is optional.", 4, 4, e1);

        runtime.modify("defaultValueSource.pure", """
                import test::*;
                Class test::A
                {
                   stringProperty: String[*] = 'optional';
                }
                
                """
        );
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Default values are supported only for mandatory fields, and property 'stringProperty' is optional.", 4, 4, e2);
    }

    @Test
    public void testDefaultValueWithNotMatchingEnumType()
    {
        PureCompilationException e = Assertions.assertThrows(PureCompilationException.class, () ->
                compileTestSource("defaultValueSource.pure", """
import test::*;
Enum example::EnumWithDefault
{
   DefaultValue,
   AnotherValue
}
Enum example::DifferentEnum
{
   DefaultValue,
   AnotherValue
}
Class test::C
{
   enumProperty: example::EnumWithDefault[1] = example::DifferentEnum.DefaultValue;
}
"""));
        assertPureException(PureCompilationException.class, "Default value for property: 'enumProperty' / Type Error: 'DifferentEnum' not a subtype of 'EnumWithDefault'", 14, 71, e);
    }

    @Test
    public void testDefaultValueMultiplicityMatches()
    {
        PureCompilationException e1 = Assertions.assertThrows(PureCompilationException.class, () ->
                compileTestSource("defaultValueSource.pure", """
                    import test::*;
                    Class test::A
                    {
                       stringProperty: String[1] = ['one', 'two'];
                    }
                    """));
        assertPureException(PureCompilationException.class, "The default value's multiplicity does not match the multiplicity of property 'stringProperty'.", 4, 4, e1);

        runtime.modify("defaultValueSource.pure", """
                import test::*;
                Class test::A
                {
                   stringProperty: String[1..3] = ['one', 'two', 'three', 'four'];
                }
                
                """
        );
        PureCompilationException e2 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The default value's multiplicity does not match the multiplicity of property 'stringProperty'.", 4, 4, e2);

        runtime.modify("defaultValueSource.pure", """
                import test::*;
                Class test::A
                {
                   stringProperty: String[2..3] = ['one'];
                }
                
                """
        );
        PureCompilationException e3 = Assertions.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The default value's multiplicity does not match the multiplicity of property 'stringProperty'.", 4, 4, e3);
    }
}
