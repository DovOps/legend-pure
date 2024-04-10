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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunctionInstance;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.statelistener.StatsStateListener;
import org.finos.legend.pure.m3.statelistener.VoidM3M4StateListener;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestM3AntlrParser extends AbstractPureTestWithCoreCompiledPlatform
{
    MutableList<CoreInstance> newInstances = Lists.fixedSize.empty();
    StatsStateListener stateListener = new StatsStateListener();

    @BeforeAll
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @BeforeEach
    public void setup()
    {
        this.newInstances = Lists.mutable.empty();
        stateListener = new StatsStateListener();
    }


    @Test
    public void testImports()
    {
        String imports = """
                import a::b::c::*;
                import aa::bb::*;
                """;
        String code = """
                Class a::b::c::Person
                      {
                         firstName: String[1];
                         lastName: String[1];
                         fullName(title: String[*]) {\
                               title + ' ' +  this.firstName + ' ' + this.lastName\
                          }: String[1];\
                      }\
                """;
        new M3AntlrParser(null).parse(imports + code, "test1", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testSimpleClass()
    {
        String code = """
                Class Person
                      {
                         firstName: String[1];\
                         lastName: String[1];\
                      }
                Class datamarts::DataM23::domain::BatchIDMilestone
                {  \s
                   inZ: Date[1];
                   outZ: Date[1];
                   vlfBatchIdIn: Integer[1];
                   vlfBatchIdOut: Integer[1];  \s
                   vlfDigest : String[1];
                  \s
                   filterByBatchID(batchId: Integer[1]) { $this.vlfBatchIdIn == $batchId; } : Boolean[1];
                   filterByBatchIDRange(batchId: Integer[1]) { ($batchId >= $this.vlfBatchIdIn) && ($batchId <= $this.vlfBatchIdOut) ; } : Boolean[1];
                   // Pure does not support contains or in today. Hence, difficul to support multiple batch id
                   //filterByBatchIDs(batchIds: Float[*]) { and ($batchIds->map(batchId|$batchId == $this.vlfBatchIdIn)); } : Boolean[1];
                   filterByProcessingTime(processingTime: Date[1]) { ($processingTime >= $this.inZ) && ($processingTime <= $this.outZ); } : Boolean[1];
                }\
                """;
        new M3AntlrParser(null).parse(code, "test2", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testClassClassRef()
    {
        String code = """
                Class meta::pure::mapping::test::Mapping extends PackageableElement
                {
                    includes : MappingInclude[*];
                    classMappings : SetImplementation[*];
                    enumerationMappings : EnumerationMapping<Any>[*];
                    associationMappings : AssociationImplementation[*];
                
                    enumerationMappingByName(name:String[1])
                    {
                        $this.includes->map(i | $i.included)
                                      ->map(m | $m.enumerationMappingByName($name))
                                      ->concatenate($this.enumerationMappings->filter(em|$em.name == $name))
                    }:EnumerationMapping<Any>[*];
                
                    classMappingByClassName(name:String[1])
                    {
                        let assocPropertyMappings = $this.associationMappings.propertyMappings;
                        $this.includes->map(i | $i.included)
                                      ->map(m | $m.classMappingByClassName($name))
                                      ->concatenate($this.classMappings->filter(cm|$cm.class.name == $name))
                                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));
                    }:SetImplementation[*];
                
                    rootClassMappingByClassName(name:String[1])
                    {
                        $this.classMappingByClassName($name)->filter(s|$s.root == true)->last();
                    }:SetImplementation[0..1];
                
                    classMappingByClass(class:Class<Any>[1])
                    {
                        let assocPropertyMappings = $this.associationMappings.propertyMappings;
                        $this.includes->map(i | $i.included)
                                      ->map(m | $m.classMappingByClass($class))
                                      ->concatenate($this.classMappings->filter(cm|$cm.class == $class))
                                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));
                    }:SetImplementation[*];
                
                    rootClassMappingByClass(class:Class<Any>[1])
                    {
                        $this.classMappingByClass($class)->filter(s|$s.root == true)->last();
                    }:SetImplementation[0..1];
                
                
                    _classMappingByIdRecursive(id:String[*])
                    {
                        let result = $this.includes->map(i | $i.included)
                                                   ->map(m | $m._classMappingByIdRecursive($id))
                                                   ->concatenate($this.classMappings->filter(cm|$cm.id == $id));
                    }:SetImplementation[*];
                
                    classMappingById(id:String[1])
                    {
                        let assocPropertyMappings = $this._associationPropertyMappingsByIdRecursive($id)->removeDuplicates();
                        let allClassMappings = $this._classMappingByIdRecursive($id)->removeDuplicates();
                        let result = $allClassMappings->toOne()->addAssociationMappingsIfRequired($assocPropertyMappings);
                        if($result->isEmpty(),|[],|$result->removeDuplicates()->toOne());
                    }:SetImplementation[0..1];
                
                    _associationPropertyMappingsByIdRecursive(id:String[1])
                    {
                        let result = $this.includes->map(i | $i.included)
                                                   ->map(m | $m._associationPropertyMappingsByIdRecursive($id))
                                                   ->concatenate($this.associationMappings.propertyMappings->filter(pm | $pm.sourceSetImplementationId == $id));
                    }:PropertyMapping[*];
                
                    classMappings()
                    {
                        let assocPropertyMappings = $this.associationMappings.propertyMappings;
                        $this.includes->map(i | $i.included)
                                      ->map(m | $m.classMappings())
                                      ->concatenate($this.classMappings)
                                      ->map(cm | $cm->addAssociationMappingsIfRequired($assocPropertyMappings));
                    }:SetImplementation[*];
                
                    findSubstituteStore(store:Store[1])
                    {
                        $this.includes->fold({inc:MappingInclude[1], sub:Store[0..1] | if($sub->isEmpty(), |$inc.findSubstituteStore($store), |$sub)}, [])
                    }:Store[0..1];
                
                    resolveStore(store:Store[1])
                    {
                        let substitute = $this.findSubstituteStore($store);
                        if($substitute->isEmpty(), |$store, |$substitute->toOne());
                    }:Store[1];
                }\
                """;
        new M3AntlrParser(null).parse(code, "test3", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testGenericClass()
    {
        String code = """
                import meta::pure::metamodel::path::*;
                
                Class meta::pure::metamodel::path::test::Path<-U,V|m> extends Function<{U[1]->V[m]}>
                {
                   start : GenericType[1];
                   path : PathElement[1..*];
                   referenceUsages : ReferenceUsage[*];
                   name : String[0..1];
                }\
                """;
        new M3AntlrParser(null).parse(code, "test4", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }


    @Test
    public void testClassWithQualifiedProperty()
    {
        String code = """
                Class Person2
                      {
                         firstName: String[1];
                         lastName: String[1];
                         fullName(title: String[*]) {\
                               title + ' ' +  this.firstName + ' ' + this.lastName\
                          }: String[1];\
                      }\
                """;
        new M3AntlrParser(null).parse(code, "test5", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testClassWithPropertiesHavingSpaces()
    {
        String code = """
                Class Person4
                      {
                         'first Name': String[1];
                         lastName: String[1];
                         fullName(title: String[*]) {\
                               title + ' ' +  this.firstName + ' ' + this.lastName\
                          }: String[1];\
                      }\
                """;
        new M3AntlrParser(null).parse(code, "test6", true, 0, repository, this.newInstances, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context, 0, null);
    }

    @Test
    public void testAccessingPropertyWithSpace()
    {
        String code = """
                Class Person5
                      {
                         'first Name': String[1];
                         lastName: String[1];
                         fullName(title: String[*]) {\
                               title + ' ' +  this.firstName + ' ' + this.lastName\
                          }: String[1];
                      }
                      function myT():Person3[*]\
                      {\
                           Person5.all()->filter(p|$p.'first name' == 'ok')\
                      }\
                """;
        new M3AntlrParser(null).parse(code, "test7", true, 0, repository, this.newInstances, VoidM3M4StateListener.VOID_M3_M4_STATE_LISTENER, context, 0, null);
    }


    @Test
    public void testFunctionDefinition()
    {
        String code = """
                Class Person3
                      {
                         firstName: String[1];
                         lastName: String[1];
                         fullName(title: String[*]) {\
                               title + ' ' +  this.firstName + ' ' + this.lastName\
                          }: String[1];
                      }
                      function myT():String[1]\
                      {\
                           'hello world'\
                      }\
                """;
        new M3AntlrParser(null).parse(code, "test8", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testComplexFunctions()
    {
        String code = """
                import meta::json::*;
                import meta::PR1::*;
                import datamarts::dm::mapping::*;
                import datamarts::dm::store::*;
                import datamarts::dm::domain::pactnext::reporting::subdom1::*;
                import datamarts::dm::domain::pactnext::reporting::*;
                import datamarts::dm::mapping::pact::*;
                import meta::pure::mapping::*;
                
                function\s
                {service.url='/pact/reporting/sessions/{start}/{end}'}
                datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: String[1], end: String[1]):String[1]
                {
                   datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(parseDate($start), parseDate($end))->toJSONStringStream([],true)->makeString()
                }
                
                function datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: Date[1], end: Date[1]):TabularDataSet[1]
                {
                   datamarts::dm::domain::pactnext::reporting::queries::PACTSessions($start, $end, 'union')
                }
                
                function datamarts::dm::domain::pactnext::reporting::queries::PACTSessions(start: Date[1], end: Date[1], environment: String[1]):TabularDataSet[1]
                {
                   execute(\s
                      | datamarts::dm::domain::pactnext::reporting::Session.all()
                         ->filter(s | greaterThanEqual(datePart($s.startTime), $start) && lessThanEqual(datePart($s.startTime), $end))
                         ->filter(s | isNotEmpty($s.eventType))
                         ->filter(s | isNotEmpty($s.accessType))     \s
                //TODO:         ->filter(s | !$s.device->isEmpty())
                         ->project(
                            [
                              x | dateDiff($x.endTime, $x.startTime, meta::pure::functions::date::DurationUnit.SECONDS),
                 		      x | $x.endTime,
                 		      x | $x.environment,
                              x | $x.pactImplementation,
                		      x | $x.sessionId,
                		      x | $x.reasonForAccess,
                		      x | $x.reviewable,
                		      x | $x.sourceSystemName,
                		      x | $x.startTime,
                		      x | $x.accessType,
                             \s
                              x | if(isNotEmpty($x.device.hostname),|'APPLICATION_SERVER',|if(isNotEmpty($x.device.dataserver),|'DATABASE_SERVER',|if(isNotEmpty($x.device.database),|'DATABASE',|'UNKNOWN'))),
                		      x | $x.device.hostname,
                              x | $x.device.dataserver,
                              x | $x.device.database,
                             \s
                		      x | $x.eventType,
                       \s
                		      x | $x.review.comment,
                		      x | $x.review.closureTime,
                		      x | $x.review.reviewer.businessUnitName,
                		      x | $x.review.reviewer.city,
                		      x | $x.review.reviewer.departmentName,
                              x | $x.review.reviewer.departmentCode,
                		      x | $x.review.reviewer.divisionName,
                		      x | $x.review.reviewer.firstName,
                		      x | $x.review.reviewer.kerberos,
                		      x | $x.review.reviewer.lastName,
                		      x | $x.review.reviewer.title,
                       \s
                		      x | $x.requestor.businessUnitName,
                		      x | $x.requestor.city,
                		      x | $x.requestor.departmentName,
                              x | $x.requestor.departmentCode,
                		      x | $x.requestor.divisionName,
                		      x | '', //employeeNumber does not exist
                		      x | $x.requestor.firstName,
                		      x | $x.requestor.lastName,
                		      x | $x.requestor.kerberos,
                		      x | $x.requestor.title,
                       \s
                              x | $x.systemAccountName,
                              x | $x.accessType,
                              x | $x.userKerberosId
                           ],
                	       [
                              'Duration',
                		      'end',
                		      'environment',
                              'pact.implementation',
                   		      'id',
                		      'reasonForAccess',
                		      'reviewabilityStatus',
                		      'sourceSystemName',
                		      'start',
                		      'accessType.name',
                             \s
                              'device.type',
                		      'device.hostname',
                		      'device.dataserver',
                		      'device.database',
                             \s
                		      'eventType.name',
                             \s
                		      'review.comment',
                		      'review.time',
                		      'review.reviewer.businessUnitName',
                		      'review.reviewer.city',
                		      'review.reviewer.departmentName',
                              'review.reviewer.departmentCode',
                		      'review.reviewer.divisionName',
                		      'review.reviewer.firstName',
                		      'review.reviewer.kerberos',
                		      'review.reviewer.lastName',
                		      'review.reviewer.title',
                             \s
                		      'user.businessUnitName',
                		      'requestor.city',
                		      'requestor.departmentName',
                              'requestor.departmentCode',
                		      'requestor.divisionName',
                		      'requestor.employeeNumber',
                		      'requestor.firstName',
                		      'requestor.lastName',
                		      'requestor.kerberos',
                		      'requestor.title',
                             \s
                              'targetUserId',
                              'accessTypeCode',
                              'eventLogin'
                           ]
                   ), datamarts::dm::domain::pactnext::reporting::queries::determineMapping($environment), datamarts::dm::store::EPDAIQRuntime('EP DA IQ CDC')).values->at(0);
                }
                
                function\s
                {service.url='/pact/reporting/ccmpactsessionsexploded/{start}/{end}'}
                datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: String[1], end: String[1]):String[1]
                {
                   datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(parseDate($start), parseDate($end))->toJSONStringStream([],true)->makeString()
                }
                
                function datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: Date[1], end: Date[1]):TabularDataSet[1]
                {
                   datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded($start, $end, 'union')
                }
                
                function datamarts::dm::domain::pactnext::reporting::queries::CCMPACTSessionsExploded(start: Date[1], end: Date[1], environment: String[1]):TabularDataSet[1]
                {
                   execute(\s
                      | datamarts::dm::domain::pactnext::reporting::Session.all()
                         ->filter(s | greaterThanEqual(datePart($s.startTime), $start) && lessThanEqual(datePart($s.startTime), $end))
                         ->filter(s | isNotEmpty($s.eventType))
                         ->filter(s | isNotEmpty($s.accessType))     \s
                //TODO:         ->filter(s | !$s.device->isEmpty())
                         ->project(
                            [
                              x | dateDiff($x.endTime, $x.startTime, meta::pure::functions::date::DurationUnit.SECONDS),
                 		      x | $x.endTime,
                 		      x | $x.environment,
                              x | $x.pactImplementation,
                		      x | $x.sessionId,
                		      x | $x.reasonForAccess,
                		      x | $x.reviewable,
                		      x | $x.sourceSystemName,
                		      x | $x.startTime,
                		      x | $x.accessType,
                             \s
                              x | if(isNotEmpty($x.device.hostname),|'APPLICATION_SERVER',|if(isNotEmpty($x.device.dataserver),|'DATABASE_SERVER',|if(isNotEmpty($x.device.database),|'DATABASE',|'UNKNOWN'))),
                		      x | $x.device.hostname,
                		      x | $x.device.dataserver,
                		      x | $x.device.database,
                             \s
                		      x | $x.eventType,
                             \s
                		      x | $x.review.comment,
                		      x | $x.review.closureTime,
                		      x | $x.review.reviewer.businessUnitName,
                		      x | $x.review.reviewer.city,
                		      x | $x.review.reviewer.departmentName,
                              x | $x.review.reviewer.departmentCode,
                		      x | $x.review.reviewer.divisionName,
                		      x | $x.review.reviewer.firstName,
                		      x | $x.review.reviewer.kerberos,
                		      x | $x.review.reviewer.lastName,
                		      x | $x.review.reviewer.title,
                             \s
                			  x | $x.requestor.businessUnitName,
                			  x | $x.requestor.city,
                			  x | $x.requestor.departmentName,
                			  x | $x.requestor.departmentCode,
                			  x | $x.requestor.divisionName,
                			  x | '', //employeeNumber does not exist
                			  x | $x.requestor.firstName,
                			  x | $x.requestor.lastName,
                			  x | $x.requestor.kerberos,
                			  x | $x.requestor.title,
                             \s
                              x | $x.systemAccountName,
                             \s
                              x | $x.reviewabilityList.reviewable,
                              x | $x.reviewabilityList.deployment.deploymentName,
                              x | $x.reviewabilityList.deployment.deploymentId,
                              x | $x.reviewabilityList.deployment.application.applicationName,
                              x | $x.reviewabilityList.deployment.application.applicationId,
                              x | $x.reviewabilityList.deployment.application.family.familyName,
                              x | $x.reviewabilityList.deployment.application.family.familyId,
                              x | $x.reviewabilityList.deployment.application.family.subBu.subBuName,
                              x | $x.reviewabilityList.deployment.application.family.subBu.subBuId,
                              x | $x.reviewabilityList.deployment.application.family.subBu.bu.buName,
                              x | $x.reviewabilityList.deployment.application.family.subBu.bu.buId,
                              x | $x.reviewabilityList.deployment.environment,
                             \s
                              x | $x.accessType,
                              x | $x.userKerberosId
                           ],
                		   [
                              'Duration',
                		      'end',
                		      'environment',
                              'pact.implementation',
                		      'id',
                		      'reasonForAccess',
                		      'overallReviewabilityStatus',
                		      'sourceSystemName',
                		      'start',
                		      'accessType.name',
                             \s
                              'device.type',
                		      'device.hostname',
                		      'device.dataserver',
                		      'device.database',
                             \s
                		      'eventType.name',
                             \s
                		      'review.comment',
                		      'review.time',
                		      'review.reviewer.businessUnitName',
                		      'review.reviewer.city',
                		      'review.reviewer.departmentName',
                              'review.reviewer.departmentCode',
                		      'review.reviewer.divisionName',
                		      'review.reviewer.firstName',
                		      'review.reviewer.kerberos',
                		      'review.reviewer.lastName',
                		      'review.reviewer.title',
                		     \s
                              'requestor.businessUnitName',
                		      'requestor.city',
                		      'requestor.departmentName',
                              'requestor.departmentCode',
                		      'requestor.divisionName',
                		      'requestor.employeeNumber',
                		      'requestor.firstName',
                		      'requestor.lastName',
                		      'requestor.kerberos',
                		      'requestor.title',
                             \s
                              'targetUserId',
                             \s
                              'reviewable',
                              'deployment.name',
                              'deployment.id',
                              'application.name',
                              'application.id',
                              'family.name',
                              'family.id',
                              'subBU.name',
                              'subBU.id',
                              'BU.name',
                              'BU.id',
                              'deploymentEnvironment',
                             \s
                              'accessTypeCode',
                              'eventLogin'
                           ]
                	), datamarts::dm::domain::pactnext::reporting::queries::determineMapping($environment), EPDAIQRuntime('EP DA IQ CDC')).values->at(0); \s
                }
                
                function datamarts::dm::domain::pactnext::reporting::queries::determineMapping(environment: String[1]):Mapping[1]
                {
                   if($environment == 'legacy',
                      | datamarts::dm::mapping::ParLegacyMapping,\s
                      | if($environment == 'up',
                            | datamarts::dm::mapping::ParUpMapping,
                            | datamarts::dm::mapping::ParUnionMapping))
                }\
                """;
        new M3AntlrParser(null).parse(code, "test9", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testEnum()
    {
        String code = """
                Enum apps::global::dsb::domain::uiModel::DataSetRelativeDateUnit
                {
                   {JsonSerializationInfo.enumValue = 'Days'} Days,
                   {JsonSerializationInfo.enumValue = 'Weeks'} Weeks,
                   {JsonSerializationInfo.enumValue = 'Months'} Months,
                   {JsonSerializationInfo.enumValue = 'Years'} Years
                }
                
                Enum apps::global::dsb::domain::uiModel::DataSetRelativeDateStart
                {
                   {JsonSerializationInfo.enumValue = 'Today'} Today,
                   {JsonSerializationInfo.enumValue = 'StartOfWeek'} StartOfWeek,
                   {JsonSerializationInfo.enumValue = 'StartOfMonth'} StartOfMonth,
                   {JsonSerializationInfo.enumValue = 'StartOfYear'} StartOfYear,
                   {JsonSerializationInfo.enumValue = 'PreviousFriday'} PreviousFriday,
                   {JsonSerializationInfo.enumValue = 'StartOfQuarter'} StartOfQuarter,
                   {JsonSerializationInfo.enumValue = 'RunDate'} RunDate
                }
                Enum apps::global::dsb::domain::uiModel::DataSetCompositeOperation
                {
                    {JsonSerializationInfo.enumValue = 'AND'} And,
                    {JsonSerializationInfo.enumValue = 'OR'} Or
                }\
                Enum apps::global::dsb::domain::uiModel::DataSetComparisonOperation
                {
                    {JsonSerializationInfo.enumValue = '=='} Equal,
                    {JsonSerializationInfo.enumValue = '!='} NotEqual,
                    {JsonSerializationInfo.enumValue = '>'} GreaterThan,\s
                    {JsonSerializationInfo.enumValue = '>='} GreaterThanOrEqual,\s
                    {JsonSerializationInfo.enumValue = '<'} LessThan,\s
                    {JsonSerializationInfo.enumValue = '<='} LessThanOrEqual,
                    {JsonSerializationInfo.enumValue = 'startsWith'} StartsWith,\s
                    {JsonSerializationInfo.enumValue = 'doesNotStartWith'} DoesNotStartWith,\s
                    {JsonSerializationInfo.enumValue = 'endsWith'} EndsWith,\s
                    {JsonSerializationInfo.enumValue = 'doesNotEndWith'} DoesNotEndWith,\s
                    {JsonSerializationInfo.enumValue = 'contains'} Contains,
                    {JsonSerializationInfo.enumValue = 'doesNotContain'} DoesNotContain,
                    {JsonSerializationInfo.enumValue = 'in'} In,\s
                    {JsonSerializationInfo.enumValue = 'notIn'} NotIn,\s
                    {JsonSerializationInfo.enumValue = 'isEmpty'} IsEmpty,\s
                    {JsonSerializationInfo.enumValue = 'isNotEmpty'} IsNotEmpty\s
                }
                """;
        new M3AntlrParser(null).parse(code, "test10", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testAssociation()
    {
        String code = """
                Association datamarts::dm::domain::sayo::Product_Technical_Owner_Product
                {
                    technical_owner_product: Product[1];
                    {up.relationshipType = 'shared'}
                    product_technical_owners: ProductPrimaryOwner[*];
                }
                
                Association datamarts::dm::domain::sayo::Product_Technical_Owner_Person
                {
                    technical_owner_person: GsPerson[1];
                    {up.relationshipType = 'shared'}
                    product_technical_owners: ProductPrimaryOwner[*];
                }
                
                Association datamarts::dm::domain::sayo::Test_Maturity_Champion
                {
                    {up.relationshipType = 'shared'}
                    test_maturity_champion: GsPerson[1];
                    test_maturity_champion_products: Product[*];
                }
                
                Association datamarts::dm::domain::sayo::Test_Maturity_Advocate
                {
                    {up.relationshipType = 'shared'}
                    test_maturity_advocate: GsPerson[1];
                    test_maturity_advocate_products: Product[*];
                }
                """;
        new M3AntlrParser(null).parse(code, "test11", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testProfile()
    {
        String code = """
                
                Profile apps::global::dsb::mapping::json::JsonSerializationInfo
                {
                    stereotypes: [ignore, execute];
                    tags: [enumValue, propertyName];
                }
                """;
        new M3AntlrParser(null).parse(code, "test12", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
        code = """
                
                Profile apps::global::dsb::mapping::json::JsonSerializationInfoST
                {
                    stereotypes: [ignore, execute];
                }
                """;
        new M3AntlrParser(null).parse(code, "test13", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
        code = """
                
                Profile apps::global::dsb::mapping::json::JsonSerializationInfoTO
                {
                    tags: [enumValue, propertyName];
                }
                """;
        new M3AntlrParser(null).parse(code, "test14", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
        code = """
                
                Profile apps::global::dsb::mapping::json::JsonSerializationInfoBlank
                {
                }
                """;
        new M3AntlrParser(null).parse(code, "test15", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testNativeFunction()
    {
        String code = """
                // Functions for constructing dates
                native function meta::pure::functions::date::date(year:Integer[1]):Date[1];
                native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1]):Date[1];
                native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1]):Date[1];
                native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1]):Date[1];
                native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1], minute:Integer[1]):Date[1];
                native function meta::pure::functions::date::date(year:Integer[1], month:Integer[1], day:Integer[1], hour:Integer[1], minute:Integer[1], second:Number[1]):Date[1];
                """;
        new M3AntlrParser(null).parse(code, "test16", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }

    @Test
    public void testStereotypeOnNativeFunction()
    {
        String code =
                """
                
                Profile meta::pure::function::dummyProfile
                {
                   stereotypes : [Dummy];\
                }
                native function <<dummyProfile.Dummy>> meta::pure::functions::date::date(year:Integer[1]):Date[1];
                """;
        new M3AntlrParser(null).parse(code, "test17", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
        Assertions.assertEquals(1, this.newInstances.selectInstancesOf(NativeFunctionInstance.class).getOnly()._stereotypesCoreInstance().size());
    }

    @Test
    public void testInstanceParsingWithRootPackageReference()
    {
        String code = "^meta::pure::functions::lang::KeyValue(key='pkg', value=::)";
        new M3AntlrParser(null).parse(code, "test18", true, 0, repository, this.newInstances, this.stateListener, context, 0, null);
    }
}
