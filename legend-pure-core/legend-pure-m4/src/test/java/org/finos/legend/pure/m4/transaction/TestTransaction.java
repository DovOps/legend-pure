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

package org.finos.legend.pure.m4.transaction;

import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.finos.legend.pure.m4.transaction.framework.Transaction;
import org.finos.legend.pure.m4.transaction.framework.TransactionManager;
import org.finos.legend.pure.m4.transaction.framework.TransactionStateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTransaction
{
    private final StubTransactionManager manager = new StubTransactionManager();

    @Test
    public void testStateAfterCommit()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assertions.assertTrue(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertFalse(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertFalse(transaction.isRolledBack());
        Assertions.assertFalse(transaction.isInvalid());
        Assertions.assertTrue(this.manager.isRegistered(transaction));

        transaction.commit();
        Assertions.assertFalse(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertTrue(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertFalse(transaction.isRolledBack());
        Assertions.assertFalse(transaction.isInvalid());
        Assertions.assertFalse(this.manager.isRegistered(transaction));
    }

    @Test
    public void testCommitNonCommittable()
    {
        StubTransaction transaction = this.manager.newTransaction(false);
        try
        {
            transaction.commit();
            Assertions.fail("Expected exception");
        }
        catch (IllegalStateException e)
        {
            Assertions.assertEquals("Transaction is not committable", e.getMessage());
        }
    }

    @Test
    public void testCommitAfterCommit()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.commit();
            transaction.commit();
        });
    }

    @Test
    public void testRollbackAfterCommit()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.commit();
            transaction.rollback();
        });
    }

    @Test
    public void testOpenInThreadCurrentThreadAfterCommit()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.commit();
            transaction.openInCurrentThread();
        });
    }

    @Test
    public void testStateAfterRollback()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assertions.assertTrue(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertFalse(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertFalse(transaction.isRolledBack());
        Assertions.assertFalse(transaction.isInvalid());
        Assertions.assertTrue(this.manager.isRegistered(transaction));

        transaction.rollback();
        Assertions.assertFalse(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertFalse(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertTrue(transaction.isRolledBack());
        Assertions.assertFalse(transaction.isInvalid());
        Assertions.assertFalse(this.manager.isRegistered(transaction));
    }

    @Test
    public void testCommitAfterRollback()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.rollback();
            transaction.commit();
        });
    }

    @Test
    public void testRollbackAfterRollback()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.rollback();
            transaction.rollback();
        });
    }

    @Test
    public void testOpenInThreadCurrentThreadAfterRollback()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            transaction.rollback();
            transaction.openInCurrentThread();
        });
    }

    @Test
    public void testOpenTransactionInCurrentThread() throws Exception
    {
        Transaction transaction = this.manager.newTransaction(true);
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
            final StubTransaction[] otherThreadResult = new StubTransaction[1];
            Thread otherThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    otherThreadResult[0] = TestTransaction.this.manager.getThreadLocalTransaction();
                }
            });
            otherThread.setDaemon(true);
            otherThread.start();
            otherThread.join();
            Assertions.assertNull(otherThreadResult[0]);
        }
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testOpenTransactionInOtherThread() throws Exception
    {
        final Transaction transaction = this.manager.newTransaction(true);
        Assertions.assertNull(this.manager.getThreadLocalTransaction());

        final AtomicBoolean transactionOpened = new AtomicBoolean(false);
        final AtomicBoolean transactionChecked = new AtomicBoolean(false);
        StubTransaction[] currentThreadResults = new StubTransaction[3];
        final StubTransaction[] otherThreadResults = new StubTransaction[3];
        Thread otherThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                otherThreadResults[0] = TestTransaction.this.manager.getThreadLocalTransaction();
                try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
                {
                    transactionOpened.set(true);
                    otherThreadResults[1] = TestTransaction.this.manager.getThreadLocalTransaction();
                    while (!transactionChecked.get())
                    {
                        // wait
                    }
                }
                otherThreadResults[2] = TestTransaction.this.manager.getThreadLocalTransaction();
            }
        });
        otherThread.setDaemon(true);

        currentThreadResults[0] = this.manager.getThreadLocalTransaction();
        otherThread.start();
        while (!transactionOpened.get())
        {
            // wait
        }
        currentThreadResults[1] = this.manager.getThreadLocalTransaction();
        transactionChecked.set(true);
        otherThread.join();
        currentThreadResults[2] = this.manager.getThreadLocalTransaction();

        Assertions.assertNull(otherThreadResults[0]);
        Assertions.assertSame(transaction, otherThreadResults[1]);
        Assertions.assertNull(otherThreadResults[2]);

        Assertions.assertNull(currentThreadResults[0]);
        Assertions.assertNull(currentThreadResults[1]);
        Assertions.assertNull(currentThreadResults[2]);
    }

    @Test
    public void testNestedOpenTransactionInCurrentThread() throws Exception
    {
        Transaction transaction = this.manager.newTransaction(true);
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore1 = transaction.openInCurrentThread())
        {
            Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
            try (ThreadLocalTransactionContext ignore2 = transaction.openInCurrentThread())
            {
                Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
                try (ThreadLocalTransactionContext ignore3 = transaction.openInCurrentThread())
                {
                    Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
                }
                Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
            }
            Assertions.assertSame(transaction, this.manager.getThreadLocalTransaction());
        }
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testOpenDifferentTransactionsInSameThread() throws Exception
    {
        Transaction transaction1 = this.manager.newTransaction(false);
        Transaction transaction2 = this.manager.newTransaction(false);
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
        openTwoTransactionsInSameThread(transaction1, transaction2);
        openTwoTransactionsInSameThread(transaction2, transaction1);
    }

    private void openTwoTransactionsInSameThread(Transaction transaction1, Transaction transaction2)
    {
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
        try (ThreadLocalTransactionContext ignore1 = transaction1.openInCurrentThread())
        {
            Assertions.assertSame(transaction1, this.manager.getThreadLocalTransaction());
            try (ThreadLocalTransactionContext ignore2 = transaction2.openInCurrentThread())
            {
                Assertions.fail("Expected exception");
            }
            catch (Exception e)
            {
                // expected
            }
            Assertions.assertSame(transaction1, this.manager.getThreadLocalTransaction());
        }
        Assertions.assertNull(this.manager.getThreadLocalTransaction());
    }

    @Test
    public void testStateAfterClearingManager()
    {
        StubTransaction transaction = this.manager.newTransaction(true);
        Assertions.assertTrue(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertFalse(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertFalse(transaction.isRolledBack());
        Assertions.assertFalse(transaction.isInvalid());

        this.manager.clear();
        Assertions.assertFalse(transaction.isOpen());
        Assertions.assertFalse(transaction.isCommitting());
        Assertions.assertFalse(transaction.isCommitted());
        Assertions.assertFalse(transaction.isRollingBack());
        Assertions.assertFalse(transaction.isRolledBack());
        Assertions.assertTrue(transaction.isInvalid());
    }

    @Test
    public void testCommitAfterClearingManager()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            this.manager.clear();
            transaction.commit();
        });
    }

    @Test
    public void testRollbackAfterClearingManager()
    {
        assertThrows(TransactionStateException.class, () -> {
            StubTransaction transaction = this.manager.newTransaction(true);
            this.manager.clear();
            transaction.rollback();
        });
    }

    private static class StubTransaction extends Transaction
    {
        private StubTransaction(TransactionManager<?> transactionManager, boolean committable)
        {
            super(transactionManager, committable);
        }

        @Override
        protected void doCommit()
        {
            // Do nothing
        }

        @Override
        protected void doRollback()
        {
            // Do nothing
        }
    }

    private static class StubTransactionManager extends TransactionManager<StubTransaction>
    {
        @Override
        protected StubTransaction createTransaction(boolean committable)
        {
            return new StubTransaction(this, committable);
        }
    }
}
