/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.TestBasicDataSource;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.h2.Driver;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import javax.sql.XADataSource;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * TestSuite for BasicManagedDataSource
 */
public class TestBasicManagedDataSource extends TestBasicDataSource {

    @Override
    protected BasicDataSource createDataSource() throws Exception {
        final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
        return basicManagedDataSource;
    }

    /**
     * JIRA: DBCP-294
     * Verify that PoolableConnections created by BasicManagedDataSource unregister themselves
     * when reallyClosed.
     */
    @Test
    public void testReallyClose() throws Exception {
        final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
        basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
        basicManagedDataSource.setUsername("userName");
        basicManagedDataSource.setPassword("password");
        basicManagedDataSource.setMaxIdle(1);
        // Create two connections
        final ManagedConnection<?> conn = (ManagedConnection<?>) basicManagedDataSource.getConnection();
        assertNotNull(basicManagedDataSource.getTransactionRegistry().getXAResource(conn));
        final ManagedConnection<?> conn2 = (ManagedConnection<?>) basicManagedDataSource.getConnection();
        conn2.close(); // Return one connection to the pool
        conn.close();  // No room at the inn - this will trigger reallyClose(), which should unregister
        try {
            basicManagedDataSource.getTransactionRegistry().getXAResource(conn);
            fail("Expecting SQLException - XAResources orphaned");
        } catch (final SQLException ex) {
            // expected
        }
        conn2.close();
        basicManagedDataSource.close();
    }

    @Test
    public void testXADataSource() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setXADataSource("anything");
            assertEquals("anything", basicManagedDataSource.getXADataSource());
        }
    }

    @Test
    public void testXaDataSourceInstance() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            final XADataSource ds = new JdbcDataSource();
            basicManagedDataSource.setXaDataSourceInstance(ds);
            assertEquals(ds, basicManagedDataSource.getXaDataSourceInstance());
        }
    }

    @Test(expected=SQLException.class)
    public void testTransactionManagerNotSet() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.createConnectionFactory();
        }
    }

    @Test
    public void testSetDriverName() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setDriverClassName("adams");
            assertEquals("adams", basicManagedDataSource.getDriverClassName());
            basicManagedDataSource.setDriverClassName(null);
            assertNull(basicManagedDataSource.getDriverClassName());
        }
    }

    @Test
    public void testCreateXaDataSourceNewInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setXADataSource(JdbcDataSource.class.getCanonicalName());
            basicManagedDataSource.setDriverClassName(Driver.class.getName());
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    public void testCreateXaDataSourceNoInstanceSetAndNoDataSource() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test(expected=NullPointerException.class)
    public void testRuntimeExceptionsAreRethrown() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            // results in a NPE
            basicManagedDataSource.createPoolableConnectionFactory(null);
        }
    }

    @Test
    public void testSetXaDataSourceInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            basicManagedDataSource.setXaDataSourceInstance(new JdbcDataSource());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    public void testSetNullXaDataSourceInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            basicManagedDataSource.setXaDataSourceInstance(null);
            assertNull(basicManagedDataSource.getXaDataSourceInstance());
        }
    }

    @Test
    public void testTransactionSynchronizationRegistry() throws Exception {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImple());
            TransactionSynchronizationRegistry tsr = new TransactionSynchronizationRegistryImple();
            basicManagedDataSource.setTransactionSynchronizationRegistry(tsr);
            JdbcDataSource xaDataSource = new JdbcDataSource();
            xaDataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            basicManagedDataSource.setXaDataSourceInstance(xaDataSource);
            basicManagedDataSource.setMaxIdle(1);

            TransactionManager tm = basicManagedDataSource.getTransactionManager();
            tm.begin();
            tsr.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    Connection connection = null;
                    try {
                        connection = basicManagedDataSource.getConnection();
                        assertNotNull(connection);
                    } catch (SQLException e) {
                        fail(e.getMessage());
                    } finally {
                        if (connection != null) {
                            try {
                                connection.close();
                            } catch (SQLException e) {
                                fail(e.getMessage());
                            }
                        }
                    }
                }

                @Override
                public void afterCompletion(int i) {

                }
            });
            tm.commit();
        }
    }
}
