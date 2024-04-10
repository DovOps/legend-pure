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

package org.finos.legend.pure.m4.coreinstance.primitive.date;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPureDate
{
    @Test
    public void testFormat()
    {
        PureDate date = DateFunctions.newPureDate(2014, 3, 10, 16, 12, 35, "070004235");
        Assertions.assertEquals("2014", date.format("yyyy"));

        Assertions.assertEquals("2014-3", date.format("yyyy-M"));
        Assertions.assertEquals("2014-03", date.format("yyyy-MM"));
        Assertions.assertEquals("2014-003", date.format("yyyy-MMM"));
        Assertions.assertEquals("2014-03-10", date.format("yyyy-MM-d"));
        Assertions.assertEquals("2014-03-10", date.format("yyyy-MM-dd"));
        Assertions.assertEquals("2014-03-10 4:12:35PM", date.format("yyyy-MM-dd h:mm:ssa"));
        Assertions.assertEquals("2014-03-10 16:12:35.070004235 GMT", date.format("yyyy-MM-dd HH:mm:ss.SSSS z"));
        Assertions.assertEquals("2014-03-10T16:12:35.070004235+0000", date.format("yyyy-MM-dd\"T\"HH:mm:ss.SSSSZ"));
        Assertions.assertEquals("2014-03-10 16:12:35.070Z", date.format("yyyy-MM-dd HH:mm:ss.SSSX"));
    }

    @Test
    public void testFormatWithTimeZoneShift()
    {
        PureDate date = DateFunctions.newPureDate(2014, 1, 1, 1, 1, 1, "070004235");
        Assertions.assertEquals("2014-01-01 01:01:01.070+0000", date.format("yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assertions.assertEquals("2014-01-01 01:01:01.070 GMT", date.format("yyyy-MM-dd HH:mm:ss.SSS z"));
        Assertions.assertEquals("2014-01-01 01:01:01.070Z", date.format("yyyy-MM-dd HH:mm:ss.SSSX"));

        Assertions.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assertions.assertEquals("2013-12-31 20:01:01.070 EST", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assertions.assertEquals("2013-12-31 20:01:01.070-05", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSX"));

        Assertions.assertEquals("2013-12-31 19:01:01.070-0600", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assertions.assertEquals("2013-12-31 19:01:01.070 CST", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assertions.assertEquals("2013-12-31 19:01:01.070-06", date.format("[CST]yyyy-MM-dd HH:mm:ss.SSSX"));

        Assertions.assertEquals("2014-01-01 02:01:01.070+0100", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assertions.assertEquals("2014-01-01 02:01:01.070 CET", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSS z"));
        Assertions.assertEquals("2014-01-01 02:01:01.070+01", date.format("[CET]yyyy-MM-dd HH:mm:ss.SSSX"));
    }

    @Test
    public void testFormatWithTimeZoneShiftButNoHour()
    {
        PureDate date = StrictDate.newStrictDate(2015, 8, 15);
        Assertions.assertEquals("2015-08-15", date.format("yyyy-MM-dd"));
        Assertions.assertEquals("2015-08-15", date.format("[EST]yyyy-MM-dd"));
        Assertions.assertEquals("2015-08-15", date.format("[CST]yyyy-MM-dd"));
        Assertions.assertEquals("2015-08-15", date.format("[CET]yyyy-MM-dd"));
    }

    @Test
    public void testFormatWithMultipleTimeZones()
    {
        PureDate date = DateFunctions.newPureDate(2014, 1, 1, 1, 1, 1, "070004235");
        Assertions.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd HH:mm:ss.SSSZ"));
        Assertions.assertEquals("2013-12-31 20:01:01.070-0500", date.format("[EST]yyyy-MM-dd [EST]HH:mm:ss.SSSZ"));
        try
        {
            date.format("[EST]yyyy-MM-dd [CST] HH:mm:ss.SSSZ");
            Assertions.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Cannot set multiple timezones: EST, CST", e.getMessage());
        }
    }

    @Test
    public void testFormatRefersToNonexistentComponent()
    {
        PureDate date = DateFunctions.newPureDate(2014, 1, 1);
        try
        {
            date.format("[EST]yyyy-MM-dd [CST] HH:mm:ss.SSSZ");
            Assertions.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Date has no hour: 2014-01-01", e.getMessage());
        }
    }

    @Test
    public void testInvalidSubseconds()
    {
        try
        {
            DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, null);
            Assertions.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid subsecond value: null", e.getMessage());
        }

        try
        {
            DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "");
            Assertions.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid subsecond value: \"\"", e.getMessage());
        }

        try
        {
            DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "789as9898");
            Assertions.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid subsecond value: \"789as9898\"", e.getMessage());
        }

        try
        {
            DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "-789");
            Assertions.fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            Assertions.assertEquals("Invalid subsecond value: \"-789\"", e.getMessage());
        }
    }

    @Test
    public void testAddYears()
    {
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 2, 28), DateFunctions.newPureDate(2016, 2, 29).addYears(1));
        Assertions.assertEquals(DateFunctions.newPureDate(2020, 2, 29), DateFunctions.newPureDate(2016, 2, 29).addYears(4));

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> DateFunctions.newPureDate(2016, 2, 29).addYears(12345678912L));
        Assertions.assertEquals("Year incremented beyond supported bounds", e.getMessage());
    }

    @Test
    public void testAddMonths()
    {
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 2, 28), DateFunctions.newPureDate(2017, 1, 31).addMonths(1));
        Assertions.assertEquals(DateFunctions.newPureDate(2020, 2, 29), DateFunctions.newPureDate(2020, 1, 31).addMonths(1));
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 3, 31), DateFunctions.newPureDate(2017, 1, 31).addMonths(2));
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 4, 30), DateFunctions.newPureDate(2017, 1, 31).addMonths(3));

        Assertions.assertEquals(DateFunctions.newPureDate(2010, 1, 29), DateFunctions.newPureDate(2012, 2, 29).addMonths(-25));
    }

    @Test
    public void testAddDays()
    {
        Assertions.assertEquals(DateFunctions.newPureDate(2017, 3, 1), DateFunctions.newPureDate(2017, 2, 28).addDays(1));
        Assertions.assertEquals(DateFunctions.newPureDate(2020, 2, 29), DateFunctions.newPureDate(2020, 2, 28).addDays(1));
        Assertions.assertEquals(DateFunctions.newPureDate(2015, 3, 30), DateFunctions.newPureDate(2015, 4, 16).addDays(-17));
        Assertions.assertEquals(DateFunctions.newPureDate(2015, 3, 30), DateFunctions.newPureDate(2014, 3, 30).addDays(365));
        Assertions.assertEquals(DateFunctions.newPureDate(2013, 3, 30), DateFunctions.newPureDate(2014, 3, 30).addDays(-365));
    }

    @Test
    public void testAddMilliseconds()
    {
        PureDate date = DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assertions.assertSame(date, date.addMilliseconds(0));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "779013429"), date.addMilliseconds(999));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "781013429"), date.addMilliseconds(-999));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "783013429"), date.addMilliseconds(3));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "777013429"), date.addMilliseconds(-3));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addMilliseconds(1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addMilliseconds(-1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addMilliseconds(10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addMilliseconds(-10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 44, "603013429"), date.addMilliseconds(10823));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 22, "957013429"), date.addMilliseconds(-10823));
    }

    @Test
    public void testAddMicroseconds()
    {
        PureDate date = DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assertions.assertSame(date, date.addMicroseconds(0));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "781012429"), date.addMicroseconds(999));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "779014429"), date.addMicroseconds(-999));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780016429"), date.addMicroseconds(3));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780010429"), date.addMicroseconds(-3));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "781013429"), date.addMicroseconds(1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "779013429"), date.addMicroseconds(-1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "790013429"), date.addMicroseconds(10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "770013429"), date.addMicroseconds(-10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "790836429"), date.addMicroseconds(10823));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "769190429"), date.addMicroseconds(-10823));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addMicroseconds(1_000_000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addMicroseconds(-1_000_000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addMicroseconds(10_000_000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addMicroseconds(-10_000_000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 44, "603014429"), date.addMicroseconds(10_823_001));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 22, "957012429"), date.addMicroseconds(-10_823_001));
    }

    @Test
    public void testAddNanoseconds()
    {
        PureDate date = DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assertions.assertSame(date, date.addNanoseconds(0));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780014428"), date.addNanoseconds(999));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780012430"), date.addNanoseconds(-999));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013432"), date.addNanoseconds(3));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013426"), date.addNanoseconds(-3));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780014429"), date.addNanoseconds(1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780012429"), date.addNanoseconds(-1000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780023429"), date.addNanoseconds(10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780003429"), date.addNanoseconds(-10000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780024252"), date.addNanoseconds(10823));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780002606"), date.addNanoseconds(-10823));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "780013429"), date.addNanoseconds(1_000_000_000));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "780013429"), date.addNanoseconds(-1_000_000_000));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 43, "780013429"), date.addNanoseconds(10_000_000_000L));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 23, "780013429"), date.addNanoseconds(-10_000_000_000L));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 44, "603013430"), date.addNanoseconds(10_823_000_001L));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 22, "957013428"), date.addNanoseconds(-10_823_000_001L));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 18, 10, 26, 33, "780013430"), date.addNanoseconds(86_400_000_000_001L));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 16, 10, 26, 33, "780013428"), date.addNanoseconds(-86_400_000_000_001L));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 6, 6, 10, 26, 36, "780013430"), date.addNanoseconds(1_728_003_000_000_001L));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 4, 27, 10, 26, 30, "780013428"), date.addNanoseconds(-1_728_003_000_000_001L));

        Assertions.assertEquals(DateFunctions.newPureDate(2021, 11, 7, 10, 26, 36, "780013430"), date.addNanoseconds(172_800_003_000_000_001L));
        Assertions.assertEquals(DateFunctions.newPureDate(2010, 11, 25, 10, 26, 30, "780013428"), date.addNanoseconds(-172_800_003_000_000_001L));
    }

    @Test
    public void testAddSubseconds()
    {
        PureDate date = DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assertions.assertSame(date, date.addSubseconds("0"));
        Assertions.assertSame(date, date.addSubseconds("00000000000000000000000000000000000"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "880013429"), date.addSubseconds("1"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "880013429"), date.addSubseconds("1000000"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "580013429"), date.addSubseconds("8"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "790013429"), date.addSubseconds("01"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "800013429"), date.addSubseconds("02"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "779013429"), date.addSubseconds("999"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "783013429"), date.addSubseconds("003"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 34, "582213500"), date.addSubseconds("802200071"));
    }

    @Test
    public void testSubtractSubseconds()
    {
        PureDate date = DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "780013429");

        Assertions.assertSame(date, date.subtractSubseconds("0"));
        Assertions.assertSame(date, date.subtractSubseconds("00000000000000000000000000000000000"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "680013429"), date.subtractSubseconds("1"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "680013429"), date.subtractSubseconds("1000000"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "980013429"), date.subtractSubseconds("8"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "770013429"), date.subtractSubseconds("01"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "760013429"), date.subtractSubseconds("02"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "781013429"), date.subtractSubseconds("999"));
        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 33, "777013429"), date.subtractSubseconds("003"));

        Assertions.assertEquals(DateFunctions.newPureDate(2016, 5, 17, 10, 26, 32, "977813358"), date.subtractSubseconds("802200071"));
    }
}
