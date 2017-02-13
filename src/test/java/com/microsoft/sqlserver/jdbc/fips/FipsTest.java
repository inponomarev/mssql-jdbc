/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.fips;

import java.sql.Connection;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.StringUtils;
import com.microsoft.sqlserver.testframework.PrepUtil;
import com.microsoft.sqlserver.testframework.Utils;

/**
 * Test class for testing FIPS property settings.
 */
@RunWith(JUnitPlatform.class)
public class FipsTest {

    private static String connectionString;
    private static String[] dataSourceProps;

    @BeforeAll
    public static void init() {
        connectionString = Utils.getConfiguredProperty("mssql_jdbc_test_connection_properties");
        dataSourceProps = getDataSourceProperties();
    }

    /**
     * Test after setting TrustServerCertificate as true.
     * 
     * @throws Exception
     */
    @Test
    public void fipsTrustServerCertificateTest() throws Exception {
        try {
            Properties props = buildConnectionProperties();
            props.setProperty("TrustServerCertificate", "true");
            Connection con = PrepUtil.getConnection(connectionString, props);
            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Could not enable FIPS due to either encrypt is not true or using trusted certificate settings."),
                    "Should create exception for invalid TrustServerCertificate value");
        }
    }

    /**
     * Test after passing encrypt as false.
     * 
     * @throws Exception
     */
    @Test
    public void fipsEncryptTest() throws Exception {
        try {
            Properties props = buildConnectionProperties();
            props.setProperty("encrypt", "false");
            Connection con = PrepUtil.getConnection(connectionString, props);
            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Could not enable FIPS due to either encrypt is not true or using trusted certificate settings."),
                    "Should create exception for invalid encrypt value");
        }
    }

    /**
     * Test after removing FIPS PROVIDER
     * 
     * @throws Exception
     */
    @Test
    public void fipsProviderTest() throws Exception {
        try {
            Properties props = buildConnectionProperties();
            props.remove("fipsProvider");
            props.setProperty("trustStore", "/SOME_PATH");
            Connection con = PrepUtil.getConnection(connectionString, props);
            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(e.getMessage().contains("Could not enable FIPS due to invalid FIPSProvider or TrustStoreType"),
                    "Should create exception for invalid FIPSProvider");
        }
    }

    /**
     * Test after removing fips, encrypt & trustStore it should work appropriately.
     * 
     * @throws Exception
     */
    @Test
    public void fipsPropertyTest() throws Exception {
        Properties props = buildConnectionProperties();
        props.remove("fips");
        props.remove("trustStoreType");
        props.remove("encrypt");
        Connection con = PrepUtil.getConnection(connectionString, props);
        Assertions.assertTrue(!StringUtils.isEmpty(con.getSchema()));
        con.close();
        con = null;
    }

    /**
     * Tests after removing all FIPS related properties.
     * 
     * @throws Exception
     */
    @Test
    public void fipsDataSourcePropertyTest() throws Exception {
        SQLServerDataSource ds = new SQLServerDataSource();
        setDataSourceProperties(ds);
        ds.setFIPS(false);
        ds.setFIPSProvider("");
        ds.setEncrypt(false);
        ds.setTrustStoreType("JKS");
        Connection con = ds.getConnection();
        Assertions.assertTrue(!StringUtils.isEmpty(con.getSchema()));
        con.close();
        con = null;
    }

    /**
     * Test after removing encrypt in FIPS Data Source.
     */
    @Test
    public void fipsDatSourceEncrypt() {
        try {
            SQLServerDataSource ds = new SQLServerDataSource();
            setDataSourceProperties(ds);
            ds.setEncrypt(false);
            Connection con = ds.getConnection();

            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Could not enable FIPS due to either encrypt is not true or using trusted certificate settings."),
                    "Should create exception for invalid encrypt value");
        }
    }

    /**
     * Test after removing FIPS PROVIDER
     * 
     * @throws Exception
     */
    @Test
    public void fipsDataSourceProviderTest() throws Exception {
        try {
            SQLServerDataSource ds = new SQLServerDataSource();
            setDataSourceProperties(ds);
            ds.setFIPSProvider("");
            ds.setTrustStore("/SOME_PATH");
            Connection con = ds.getConnection();
            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(e.getMessage().contains("Could not enable FIPS due to invalid FIPSProvider or TrustStoreType"),
                    "Should create exception for invalid FIPSProvider");
        }
    }

    /**
     * Test after setting TrustServerCertificate as true.
     * 
     * @throws Exception
     */
    @Test
    public void fipsDataSourceTrustServerCertificateTest() throws Exception {
        try {
            SQLServerDataSource ds = new SQLServerDataSource();
            setDataSourceProperties(ds);
            ds.setTrustServerCertificate(true);
            Connection con = ds.getConnection();
            Assertions.fail("It should fail as we are not passing appropriate params");
        }
        catch (SQLServerException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Could not enable FIPS due to either encrypt is not true or using trusted certificate settings."),
                    "Should create exception for invalid TrustServerCertificate value");
        }
    }

    /**
     * Setting appropriate data source properties including FIPS
     * @param ds
     */
    private void setDataSourceProperties(SQLServerDataSource ds) {
        ds.setServerName(dataSourceProps[0]);
        ds.setUser(dataSourceProps[1]);
        ds.setPassword(dataSourceProps[2]);
        ds.setDatabaseName(dataSourceProps[3]);

        // Set all properties for FIPS
        ds.setFIPS(true);
        ds.setEncrypt(true);
        ds.setTrustServerCertificate(false);
        ds.setIntegratedSecurity(false);
        ds.setTrustStoreType("PKCS12");
        ds.setFIPSProvider("BCFIPS");
    }

    /**
     * Build Connection properties for FIPS
     * 
     * @return
     */
    private Properties buildConnectionProperties() {
        Properties connectionProps = new Properties();

        connectionProps.setProperty("encrypt", "true");
        connectionProps.setProperty("integratedSecurity", "false");

        // In case of false we need to pass keystore etc. which is not passing by default.
        connectionProps.setProperty("TrustServerCertificate", "false");

        // For New Code
        connectionProps.setProperty("trustStoreType", "PKCS12");
        connectionProps.setProperty("fipsProvider", "BCFIPS");
        connectionProps.setProperty("fips", "true");

        return connectionProps;
    }

    /**
     * It will return String array. [dbServer,username,password,dbname/database]
     * 
     * -ea -Dmssql_jdbc_test_connection_properties=jdbc:sqlserver://SQL-2K16-01.galaxy.ad;userName=sa;password=Moonshine4me;database=test;
     * -Djava.library.path=C:\Downloads\sqljdbc_6.0.7728.100_enu.tar\sqljdbc_6.0\enu\auth\x64
     * 
     * @param connectionProperty
     * @return
     */
    private static String[] getDataSourceProperties() {
        String[] params = connectionString.split(";");
        String[] dataSoureParam = new String[4];

        for (String strParam : params) {
            if (strParam.startsWith("jdbc:sqlserver")) {
                dataSoureParam[0] = strParam.replace("jdbc:sqlserver://", "");
            }
            else if (strParam.startsWith("userName")) {
                dataSoureParam[1] = strParam.replace("userName=", "");
            }
            else if (strParam.startsWith("password")) {
                dataSoureParam[2] = strParam.replace("password=", "");
            }
            else if (strParam.startsWith("database")) {
                dataSoureParam[3] = strParam.replace("database=", "");
            }

        }

        return dataSoureParam;
    }

}
