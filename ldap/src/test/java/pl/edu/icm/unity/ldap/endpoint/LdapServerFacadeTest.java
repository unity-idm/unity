/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Properties;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

/**
 *
 * @author Willem Elbers <willem@clarin.eu>
 */
public class LdapServerFacadeTest {
       
    @Test
    public void testNotInitialized() {        
        DirectoryService ds = LdapServerFacadeMockFactory.create().getDs();
        assertThat(ds, is(nullValue()));
    }
    
    @Test(expected = NullPointerException.class)
    public void testNotInitializedGetAttribute() throws LdapException {
         LdapServerFacadeMockFactory.create().getAttribute("x", "y");
    }
    
    @Test
    public void testInitialize() throws Exception {
        LdapServerFacadeMockFactory.getNullMock();
    }
    
    @Test
    public void testInitializedGetAttribute() throws Exception {
        Attribute attr = LdapServerFacadeMockFactory.getNullMock()
                            .getAttribute(SchemaConstants.USER_PASSWORD_AT, SchemaConstants.USER_PASSWORD_AT_OID);
        assertThat(attr, is(not(nullValue())));
    }
    
    @Test(expected = LdapException.class)
    public void testInitializedGetNonExistingAttribute() throws Exception {
         Attribute attr = LdapServerFacadeMockFactory.getNullMock()
                            .getAttribute("non existent", "non existent");
    }
    
    @Test
    public void testGetAdminDn() {
        String adminDn = LdapServerFacadeMockFactory.create().getAdminDN();
        assertThat(adminDn, is(equalTo(ServerDNConstants.ADMIN_SYSTEM_DN)));
    }
}
