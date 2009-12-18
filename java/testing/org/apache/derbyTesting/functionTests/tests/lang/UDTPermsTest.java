/*

   Derby - Class org.apache.derbyTesting.functionTests.tests.lang.UDTPermsTest

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.apache.derbyTesting.functionTests.tests.lang;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.util.ArrayList;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.derby.iapi.util.StringUtil;
import org.apache.derby.catalog.DefaultInfo;
import org.apache.derbyTesting.junit.BaseJDBCTestCase;
import org.apache.derbyTesting.junit.JDBC;
import org.apache.derbyTesting.junit.DatabasePropertyTestSetup;
import org.apache.derbyTesting.junit.JDBC;
import org.apache.derbyTesting.junit.TestConfiguration;
import org.apache.derbyTesting.junit.CleanDatabaseTestSetup;
import org.apache.derbyTesting.junit.JDBC;

import org.apache.derby.catalog.types.RoutineAliasInfo;

/**
 * <p>
 * Test permissions on UDTs. See DERBY-651.
 * </p>
 */
public class UDTPermsTest extends GeneratedColumnsHelper
{
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTANTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    private static  final   String      TEST_DBO = "TEST_DBO";
    private static  final   String      RUTH = "RUTH";
    private static  final   String      ALICE = "ALICE";
    private static  final   String[]    LEGAL_USERS = { TEST_DBO, ALICE, RUTH  };

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // STATE
    //
    ///////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new instance.
     */

    public UDTPermsTest(String name)
    {
        super(name);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // JUnit BEHAVIOR
    //
    ///////////////////////////////////////////////////////////////////////////////////


    /**
     * Construct top level suite in this JUnit test
     */
    public static Test suite()
    {
        TestSuite suite = (TestSuite) TestConfiguration.embeddedSuite(UDTPermsTest.class);

        Test        cleanTest = new CleanDatabaseTestSetup( suite );
        Test        authenticatedTest = DatabasePropertyTestSetup.builtinAuthentication
            ( cleanTest, LEGAL_USERS, "udtPermissions" );
        Test        authorizedTest = TestConfiguration.sqlAuthorizationDecorator( authenticatedTest );

        return authorizedTest;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // TESTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Test that you need USAGE privilege on a type in order to select columns
     * of that type and in order to declare objects which mention that type.
     * </p>
     */
    public  void    test_001_basicGrant()
        throws Exception
    {
        Connection  dboConnection = openUserConnection( TEST_DBO );
        Connection  ruthConnection = openUserConnection( RUTH );
        Connection  aliceConnection = openUserConnection( ALICE );

        //
        // Create a type, function, and table. make the function and table
        // public. Verify that they are still not generally usable because the
        // type is not public yet.
        //
        goodStatement
            (
             ruthConnection,
             "create type price_ruth_01_a external name 'org.apache.derbyTesting.functionTests.tests.lang.Price' language java\n"
             );
        goodStatement
            (
             ruthConnection,
             "create function makePrice_ruth_01( )\n" +
             "returns price_ruth_01_a\n" +
             "language java\n" +
             "parameter style java\n" +
             "no sql\n" +
             "external name 'org.apache.derbyTesting.functionTests.tests.lang.Price.makePrice'\n"
             );
        goodStatement
            (
             ruthConnection,
             "create table t_ruth_01( a price_ruth_01_a )\n"
             );
        goodStatement
            (
             ruthConnection,
             "insert into t_ruth_01( a ) values ( makePrice_ruth_01() )\n"
             );
        goodStatement
            (
             ruthConnection,
             "grant select on t_ruth_01 to public\n"
             );
        goodStatement
            (
             ruthConnection,
             "grant execute on function makePrice_ruth_01 to public\n"
             );

        expectExecutionError
            (
             aliceConnection,
             LACK_USAGE_PRIV,
             "select * from ruth.t_ruth_01\n"
             );
        expectExecutionError
            (
             aliceConnection,
             LACK_USAGE_PRIV,
             "values( ruth.makePrice_ruth_01() )\n"
             );
        expectExecutionError
            (
             aliceConnection,
             LACK_USAGE_PRIV,
             "create table t_alice_01( a ruth.price_ruth_01_a )\n"
             );
        expectExecutionError
            (
             aliceConnection,
             LACK_USAGE_PRIV,
             "create function makePrice_alice_01_a( )\n" +
             "returns ruth.price_ruth_01_a\n" +
             "language java\n" +
             "parameter style java\n" +
             "no sql\n" +
             "external name 'org.apache.derbyTesting.functionTests.tests.lang.Price.makePrice'\n"
             );
        expectExecutionError
            (
             aliceConnection,
             LACK_USAGE_PRIV,
             "create function makePrice_alice_01_b( a ruth.price_ruth_01_a )\n" +
             "returns int\n" +
             "language java\n" +
             "parameter style java\n" +
             "no sql\n" +
             "external name 'org.apache.derbyTesting.functionTests.tests.lang.Price.makePrice'\n"
             );

        //
        // The DBO however is almighty.
        //
        assertResults
            (
             dboConnection,
             "select * from ruth.t_ruth_01",
             new String[][]
             {
                 { "Price( USD, 1, 1969-12-31 16:00:00.0 )" },
             },
             false
             );
        assertResults
            (
             dboConnection,
             "values( ruth.makePrice_ruth_01() )\n",
             new String[][]
             {
                 { "Price( USD, 1, 1969-12-31 16:00:00.0 )" },
             },
             false
             );
        goodStatement
            (
             dboConnection,
             "create table t_dbo_01( a ruth.price_ruth_01_a )\n"
             );
        goodStatement
            (
             dboConnection,
             "insert into t_dbo_01( a ) values ( ruth.makePrice_ruth_01() )\n"
             );
        assertResults
            (
             dboConnection,
             "select * from t_dbo_01\n",
             new String[][]
             {
                 { "Price( USD, 1, 1969-12-31 16:00:00.0 )" },
             },
             false
             );
        goodStatement
            (
             dboConnection,
             "drop table t_dbo_01\n"
             );

        //
        // Now grant USAGE on the type. User Alice should now have all the
        // privileges she needs.
        //
        goodStatement
            (
             ruthConnection,
             "grant usage on type price_ruth_01_a to public\n"
             );
        
        assertResults
            (
             aliceConnection,
             "select * from ruth.t_ruth_01",
             new String[][]
             {
                 { "Price( USD, 1, 1969-12-31 16:00:00.0 )" },
             },
             false
             );
        goodStatement
            (
             aliceConnection,
             "create table t_alice_01( a ruth.price_ruth_01_a )\n"
             );
        goodStatement
            (
             aliceConnection,
             "insert into t_alice_01( a ) values ( ruth.makePrice_ruth_01() )\n"
             );
        assertResults
            (
             aliceConnection,
             "select * from t_alice_01\n",
             new String[][]
             {
                 { "Price( USD, 1, 1969-12-31 16:00:00.0 )" },
             },
             false
             );

    }
    
   /**
     * <p>
     * Test that USAGE privilege can't be revoked if it would make objects
     * unusable by their owners.
     * </p>
     */
    public  void    test_002_basicRevoke()
        throws Exception
    {
        Connection  dboConnection = openUserConnection( TEST_DBO );
        Connection  ruthConnection = openUserConnection( RUTH );
        Connection  aliceConnection = openUserConnection( ALICE );

        goodStatement
            (
             ruthConnection,
             "create type price_ruth_02_a external name 'org.apache.derbyTesting.functionTests.tests.lang.Price' language java\n"
             );

        // only RESTRICTed revocations allowed
        expectCompilationError( ruthConnection, SYNTAX_ERROR, "revoke usage on type price_ruth_02_a from ruth\n" );

        // can't revoke USAGE from owner
        expectCompilationError
            (
             ruthConnection,
             GRANT_REVOKE_NOT_ALLOWED,
             "revoke usage on type price_ruth_02_a from ruth restrict\n"
             );

        String grantUsage = "grant usage on type price_ruth_02_a to alice\n";
        String revokeUsage = "revoke usage on type price_ruth_02_a from alice restrict\n";
        String createStatement;
        String dropStatement;
        String badRevokeSQLState;
        
        // can't revoke USAGE if a routine depends on it
        createStatement =
             "create function makePrice_alice_02( )\n" +
             "returns ruth.price_ruth_02_a\n" +
             "language java\n" +
             "parameter style java\n" +
             "no sql\n" +
             "external name 'org.apache.derbyTesting.functionTests.tests.lang.Price.makePrice'\n"
            ;
        dropStatement = "drop function makePrice_alice_02\n";
        badRevokeSQLState = ROUTINE_DEPENDS_ON_TYPE;
        verifyRevokePrivilege
            (
             ruthConnection,
             aliceConnection,
             grantUsage,
             revokeUsage,
             createStatement,
             dropStatement,
             badRevokeSQLState
             );

        // can't revoke USAGE if a table depends on it
        createStatement = "create table t_alice_02( a ruth.price_ruth_02_a )\n";
        dropStatement = "drop table t_alice_02\n";
        badRevokeSQLState = TABLE_DEPENDS_ON_TYPE;
        verifyRevokePrivilege
            (
             ruthConnection,
             aliceConnection,
             grantUsage,
             revokeUsage,
             createStatement,
             dropStatement,
             badRevokeSQLState
             );

        // can't revoke USAGE if a view depends on it
        createStatement = "create view v_alice_02( a ) as select cast (null as ruth.price_ruth_02_a ) from sys.systables\n";
        dropStatement = "drop view v_alice_02\n";
        badRevokeSQLState = VIEW_DEPENDS_ON_PRIVILEGE;
        verifyRevokePrivilege
            (
             ruthConnection,
             aliceConnection,
             grantUsage,
             revokeUsage,
             createStatement,
             dropStatement,
             badRevokeSQLState
             );
    }
    
}