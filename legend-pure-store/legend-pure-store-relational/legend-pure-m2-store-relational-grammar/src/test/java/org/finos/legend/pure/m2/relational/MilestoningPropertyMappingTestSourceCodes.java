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

package org.finos.legend.pure.m2.relational;

public class MilestoningPropertyMappingTestSourceCodes
{
    public static final String MODEL_ID = "model.pure";
    public static final String EMBEDDED_MODEL_ID = "embeddedModel.pure";
    public static final String STORE_ID = "store.pure";
    public static final String MAPPING_ID = "mapping.pure";

    public static final String EXTENDED_MODEL_ID = "extendedModel.pure";
    public static final String EXTENDED_MAPPING_ID = "extendedMapping.pure";

    public static final String BUSINESS_MILESTONING_MODEL_CODE = """
            ###Pure
            
            Class <<temporal.businesstemporal>> milestoning::A
            {
               aId : Integer[1];
            }
            """;

    public static final String PROCESSING_MILESTONING_MODEL_CODE = """
            ###Pure
            
            Class <<temporal.processingtemporal>> milestoning::A
            {
               aId : Integer[1];
            }
            """;

    public static final String BI_TEMPORAL_MILESTONING_MODEL_CODE = """
            ###Pure
            
            Class <<temporal.bitemporal>> milestoning::A
            {
               aId : Integer[1];
            }
            """;

    public static final String NON_MILESTONING_MODEL_CODE = """
            ###Pure
            
            Class milestoning::A
            {
               aId : Integer[1];
            }
            """;

    public static final String BUSINESS_MILESTONING_EXTENDED_MODEL_CODE = """
            ###Pure
            import milestoning::*;
            
            Class <<temporal.businesstemporal>> milestoning::B extends A
            {
            }
            
            """;

    public static final String EMBEDDED_MODEL_CODE = """
            ###Pure
            
            Class milestoning::B
            {
               bId : Integer[1];
               a : milestoning::A[1];
            }
            """;

    public static final String TEMPORAL_EMBEDDED_MODEL_CODE = """
            ###Pure
            
            Class <<temporal.businesstemporal>> milestoning::B
            {
               bId : Integer[1];
               a : milestoning::A[1];
            }
            """;

    public static final String MILESTONED_STORE_CODE = """
            ###Relational
            
            Database milestoning::myDB
            (
               Table myTable(
                  milestoning(
                     processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z),
                     business(BUS_FROM=from_z, BUS_THRU=thru_z)
                  )
                  aId INT, bId INT, in_z DATE, out_z DATE, from_z DATE, thru_z DATE
               )
            )
            """;

    public static final String NON_MILESTONED_STORE_CODE = """
            ###Relational
            
            Database milestoning::myDB
            (
               Table myTable(
                  aId INT, bId INT, in_z DATE, out_z DATE, from_z DATE, thru_z DATE
               )
            )
            """;

    public static final String UPDATED_MILESTONED_STORE_CODE = """
            ###Relational
            
            Database milestoning::myDB
            (
               Table myTable(
                  milestoning(
                     processing(PROCESSING_IN=in_z, PROCESSING_OUT=out_z),
                     business(BUS_FROM=from_x, BUS_THRU=thru_x)
                  )
                  aId INT, bId INT, in_z DATE, out_z DATE, from_x DATE, thru_x DATE
               )
            )
            """;

    public static final String MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Amap
            (
               A[a] : Relational
               {
                  aId : [myDB]myTable.aId
               }
            )
            """;

    public static final String UPDATED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::AMap
            (
               A[a1] : Relational
               {
                  aId : [myDB]myTable.aId
               }
            )
            """;

    public static final String EXTENDED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            ( \s
               include milestoning::Amap
              \s
               B[b] extends [a] : Relational
               {
                 \s
               }
            )
            """;

    public static final String EMBEDDED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            ( \s
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a[b_a](\s
                     aId : [myDB]myTable.aId
                  )
               }
            )
            """;

    public static final String UPDATED_EMBEDDED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            ( \s
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a[b_a1](\s
                     aId : [myDB]myTable.aId
                  )
               }
            )
            """;

    public static final String INLINE_EMBEDDED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            (
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a() Inline[a]
               }
              \s
               A[a] : Relational
               {
                  aId : [myDB]myTable.aId
               }
            )
            """;

    public static final String UPDATED_INLINE_EMBEDDED_MAPPING_CODE = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            (
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a() Inline[a1]
               }
              \s
               A[a1] : Relational
               {
                  aId : [myDB]myTable.aId
               }
            )
            """;

    public static final String MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Amap
            ( \s
               A[a] : Relational
               {
                  aId : [myDB]myTable.aId,
                  milestoning[a_m](\s
                     from : [myDB]myTable.from_z,
                     thru : [myDB]myTable.thru_z
                  )
               }
            )
            """;

    public static final String EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            ( \s
               include milestoning::Amap
              \s
               B[b] extends [a] : Relational
               {
                  milestoning[b_m](\s
                     from : [myDB]myTable.from_z,
                     thru : [myDB]myTable.thru_z
                  )
               }
            )
            """;

    public static final String EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            ( \s
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a[b_a](\s
                     aId : [myDB]myTable.aId,
                     milestoning[b_a_m](\
                        from : [myDB]myTable.from_z,
                        thru : [myDB]myTable.thru_z
                     )
                  )
               }
            )
            """;

    public static final String INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED = """
            ###Mapping
            import milestoning::*;
            
            Mapping milestoning::Bmap
            (
               B[b] : Relational
               {
                  bId : [myDB]myTable.bId,
                  a() Inline[a]
               }
              \s
               A[a] : Relational
               {
                  aId : [myDB]myTable.aId,
                  milestoning[a_m](\s
                     from : [myDB]myTable.from_z,
                     thru : [myDB]myTable.thru_z
                  )
               }
            )
            """;
}
