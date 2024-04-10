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

package org.finos.legend.pure.runtime.java.extension.relation;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTestTDS
{
    @org.junit.jupiter.api.Test
    public void testSort()
    {
        String initialTDS = """
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """;
        TestTDS tds = new TestTDS(initialTDS);

        TestTDS t = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.ASC), new SortInfo("name", SortDirection.ASC))).getOne();
        Assertions.assertEquals("""
                id, name, otherOne
                1, Pierre, F
                2, Bla, B
                2, Neema, F
                3, Ephrim, C
                3, Nop, E
                3, Ok, D
                4, Simple, D
                4, Simple, A\
                """, t.toString());

        TestTDS t2 = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.ASC), new SortInfo("name", SortDirection.ASC), new SortInfo("otherOne", SortDirection.ASC))).getOne();
        Assertions.assertEquals("""
                id, name, otherOne
                1, Pierre, F
                2, Bla, B
                2, Neema, F
                3, Ephrim, C
                3, Nop, E
                3, Ok, D
                4, Simple, A
                4, Simple, D\
                """, t2.toString());

        TestTDS t3 = tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.DESC), new SortInfo("name", SortDirection.ASC), new SortInfo("otherOne", SortDirection.DESC))).getOne();
        Assertions.assertEquals("""
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                3, Nop, E
                3, Ok, D
                2, Bla, B
                2, Neema, F
                1, Pierre, F\
                """, t3.toString());

        Assertions.assertEquals(initialTDS, tds.toString());
    }


    @org.junit.jupiter.api.Test
    public void testGetRanges()
    {
        String initialTDS = """
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """;
        TestTDS tds = new TestTDS(initialTDS);

        Assertions.assertEquals("[0:1, 1:2, 2:3, 3:4, 4:5, 5:6, 6:8]", tds.sort(Lists.mutable.with(new SortInfo("id", SortDirection.ASC), new SortInfo("name", SortDirection.ASC))).getTwo().toString());
    }

    @org.junit.jupiter.api.Test
    public void testDistinct()
    {
        String initialTDS = """
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """;
        TestTDS tds = new TestTDS(initialTDS);

        Assertions.assertEquals("""
                id, name, otherOne
                1, Pierre, F
                2, Bla, B
                3, Ephrim, C
                4, Simple, D\
                """, tds.distinct(Lists.mutable.with("id")).toString());

        Assertions.assertEquals("""
                id, name, otherOne
                1, Pierre, F
                2, Bla, B
                2, Neema, F
                3, Ephrim, C
                3, Nop, E
                3, Ok, D
                4, Simple, D\
                """, tds.distinct(Lists.mutable.with("id", "name")).toString());

        Assertions.assertEquals("""
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """, tds.toString());
    }

    @org.junit.jupiter.api.Test
    public void testSortWithNull()
    {
        String resTDS = """
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2\
                """;

        String leftTDS = """
                id, name
                1, George
                3, Sachin
                2, Pierre
                4, David\
                """;

        TestTDS res = new TestTDS(resTDS);
        TestTDS left = new TestTDS(leftTDS);

        TestTDS t = left.compensateLeft(res).sort(new SortInfo("id", SortDirection.ASC)).getOne();
        Assertions.assertEquals("""
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                1, 1, More George 2, George, 2
                2, NULL, NULL, Pierre, NULL
                3, NULL, NULL, Sachin, NULL
                4, 4, More David, David, 1\
                """, t.toString());

        Assertions.assertEquals(resTDS, res.toString());
        Assertions.assertEquals(leftTDS, left.toString());
    }

    @org.junit.jupiter.api.Test
    public void testSlice()
    {
        String initialTDS = """
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """;
        TestTDS tds = new TestTDS(initialTDS);

        TestTDS t = tds.slice(1, 3);
        Assertions.assertEquals("""
                id, name, otherOne
                4, Simple, A
                3, Ephrim, C\
                """, t.toString());

        Assertions.assertEquals(initialTDS, tds.toString());
    }

    @org.junit.jupiter.api.Test
    public void testSliceWithNull()
    {
        String resTDS = """
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2\
                """;

        String leftTDS = """
                id, name
                1, George
                3, Sachin
                2, Pierre
                4, David\
                """;

        TestTDS res = new TestTDS(resTDS);
        TestTDS left = new TestTDS(leftTDS);

        TestTDS t = left.compensateLeft(res).slice(2, 4);
        Assertions.assertEquals("""
                id, id2, extra, name, extraInt
                1, 1, More George 2, George, 2
                2, NULL, NULL, Pierre, NULL\
                """, t.toString());

        Assertions.assertEquals(resTDS, res.toString());
        Assertions.assertEquals(leftTDS, left.toString());
    }


    @org.junit.jupiter.api.Test
    public void testConcatenate()
    {
        String initialTDS1 = """
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F\
                """;

        String initialTDS2 = """
                id, name, otherOne
                1, SimpleAA, D
                3, SimpleEE, A
                4, EphrimWW, C\
                """;

        TestTDS tds1 = new TestTDS(initialTDS1);
        TestTDS tds2 = new TestTDS(initialTDS2);

        TestTDS t = tds1.concatenate(tds2);
        Assertions.assertEquals("""
                id, name, otherOne
                4, Simple, D
                4, Simple, A
                3, Ephrim, C
                2, Bla, B
                3, Ok, D
                3, Nop, E
                2, Neema, F
                1, Pierre, F
                1, SimpleAA, D
                3, SimpleEE, A
                4, EphrimWW, C\
                """, t.toString());

        Assertions.assertEquals(initialTDS1, tds1.toString());
        Assertions.assertEquals(initialTDS2, tds2.toString());
    }

    @org.junit.jupiter.api.Test
    public void testConcatenateWithNull()
    {
        String resTDS = """
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2\
                """;

        String leftTDS = """
                id, name
                1, George
                3, Sachin
                2, Pierre
                4, David\
                """;

        TestTDS res = new TestTDS(resTDS);
        TestTDS left = new TestTDS(leftTDS);

        TestTDS t = res.concatenate(left.compensateLeft(res));

        Assertions.assertEquals("""
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2
                2, NULL, NULL, Pierre, NULL
                3, NULL, NULL, Sachin, NULL\
                """, t.toString());

        Assertions.assertEquals(resTDS, res.toString());
        Assertions.assertEquals(leftTDS, left.toString());
    }

    @org.junit.jupiter.api.Test
    public void testJoin()
    {
        String initialTds1 = """
                id, name
                1, A
                2, B
                3, C\
                """;

        String initialTds2 = """
                extra
                X
                Y
                Z\
                """;

        TestTDS tds1 = new TestTDS(initialTds1);
        TestTDS tds2 = new TestTDS(initialTds2);

        TestTDS t = tds1.join(tds2);

        Assertions.assertEquals("""
                id, extra, name
                1, X, A
                1, Y, A
                1, Z, A
                2, X, B
                2, Y, B
                2, Z, B
                3, X, C
                3, Y, C
                3, Z, C\
                """, t.toString());

        Assertions.assertEquals(initialTds1, tds1.toString());
        Assertions.assertEquals(initialTds2, tds2.toString());
    }

    @org.junit.jupiter.api.Test
    public void testJoinWithNull()
    {
        String initialRes = """
                id, id2, extra, name, extraInt
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2\
                """;

        String initialLeft = """
                id, name
                1, George
                3, Sachin\
                """;

        String initialThird = """
                id, boom
                1, A1
                3, A3\
                """;

        TestTDS res = new TestTDS(initialRes);
        TestTDS left = new TestTDS(initialLeft);
        TestTDS third = new TestTDS(initialThird);

        TestTDS t = left.compensateLeft(res).join(third);

        Assertions.assertEquals("""
                id, id2, extra, name, boom, extraInt
                1, 4, More David, David, A1, 1
                3, 4, More David, David, A3, 1
                1, 1, More George 2, George, A1, 2
                3, 1, More George 2, George, A3, 2
                1, NULL, NULL, Sachin, A1, NULL
                3, NULL, NULL, Sachin, A3, NULL\
                """, t.toString());

        Assertions.assertEquals(initialRes, res.toString());
        Assertions.assertEquals(initialLeft, left.toString());
        Assertions.assertEquals(initialThird, third.toString());
    }

    @org.junit.jupiter.api.Test
    public void testCompensateLeft()
    {
        String resTDS = """
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2\
                """;

        String leftTDS = """
                id, name
                1, George
                3, Sachin
                2, Pierre
                4, David\
                """;

        TestTDS res = new TestTDS(resTDS);
        TestTDS left = new TestTDS(leftTDS);
        TestTDS t = left.compensateLeft(res);

        Assertions.assertEquals("""
                id, id2, extra, name, extraInt
                1, 1, More George 1, George, 1
                4, 4, More David, David, 1
                1, 1, More George 2, George, 2
                2, NULL, NULL, Pierre, NULL
                3, NULL, NULL, Sachin, NULL\
                """, t.toString());

        Assertions.assertEquals(resTDS, res.toString());
        Assertions.assertEquals(leftTDS, left.toString());
    }


    @org.junit.jupiter.api.Test
    public void testSentinel()
    {
        String resTDS = """
                id,name,id2,col,other
                1,George,1,More George 1,1
                1,George,1,More George 2,2
                2,Pierre,-2147483648,null,-2147483648
                3,Sachin,-2147483648,null,-2147483648
                4,David,4,More David,1\
                """;

        TestTDS res = new TestTDS(resTDS);

        Assertions.assertEquals("""
                id, col, other, id2, name
                1, More George 1, 1, 1, George
                1, More George 2, 2, 1, George
                2, NULL, NULL, NULL, Pierre
                3, NULL, NULL, NULL, Sachin
                4, More David, 1, 4, David\
                """, res.toString());
    }
}