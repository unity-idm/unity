/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Properties;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DnFactory;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Willem Elbers <willem@clarin.eu>
 */
public class LdapNodeUtilsTest {
    
    private LdapServerProperties ldapServerProperties;
    private MockedDnFactory dnFactoryMock;
    
    @Before
    public void initialize() {
        Properties properties = new Properties();
        properties.put(LdapServerProperties.PREFIX + LdapServerProperties.USER_NAME_ALIASES, "uid,cn,mail");
        ldapServerProperties = new LdapServerProperties(properties);
        
        dnFactoryMock = new MockedDnFactory();
    }
    
    @Test
    public void testIsUserSearch() {
        ExprNode nodeUserSearch = new EqualityNode<>("cn", new StringValue("example1@domain.tdl"));
        boolean isUserSearch = LdapNodeUtils.isUserSearch(ldapServerProperties, nodeUserSearch);
        assertThat(isUserSearch, is(true));
        
        ExprNode nodeGroupSearch = new EqualityNode<>("memberOf", new StringValue("some group"));
        isUserSearch = LdapNodeUtils.isUserSearch(ldapServerProperties, nodeGroupSearch);
        assertThat(isUserSearch, is(false));
        
        ExprNode or = new OrNode(nodeGroupSearch, nodeUserSearch);
        isUserSearch = LdapNodeUtils.isUserSearch(ldapServerProperties, or);
        assertThat(isUserSearch, is(true));
        
        or = new OrNode(nodeGroupSearch, new EqualityNode<>("memberOf", new StringValue("some other group")));
        isUserSearch = LdapNodeUtils.isUserSearch(ldapServerProperties, or);
        assertThat(isUserSearch, is(false));
    }
   
    @Test
    public void testParseGroupOfNamesSearch() {       
        //TODO: is it ok to use an OrNode here, for the combined node? What is the alternative?
        ExprNode objectClassNode = new EqualityNode<>(SchemaConstants.OBJECT_CLASS_AT, new StringValue(SchemaConstants.GROUP_OF_NAMES_OC));
        ExprNode memberAtNode = new EqualityNode<>(SchemaConstants.MEMBER_AT, new StringValue("cn=example1@domain.tdl,ou=system,ou=users"));        
        ExprNode node = new OrNode(objectClassNode, memberAtNode);
        String username = LdapNodeUtils.parseGroupOfNamesSearch(new MockedDnFactory(), ldapServerProperties, node);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        memberAtNode = new EqualityNode<>(SchemaConstants.MEMBER_AT, new StringValue("cn=example1@domain.tdl,ou=system,ou=users"));
        node = new OrNode(objectClassNode, memberAtNode);
        username = LdapNodeUtils.parseGroupOfNamesSearch(new MockedDnFactory(), ldapServerProperties, node);
        assertThat(username, is(equalTo("example1@domain.tdl")));
    }
    
    @Test
    public void testGetUsernameFromDn() throws LdapInvalidDnException {
        Dn dn = new Dn("cn=example1@domain.tdl,ou=system,ou=users");
        String username = LdapNodeUtils.getUserName(ldapServerProperties, dn);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        dn = new Dn("mail=example1@domain.tdl");
        username = LdapNodeUtils.getUserName(ldapServerProperties, dn);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        dn = new Dn("cn=example1@domain.tdl");
        username = LdapNodeUtils.getUserName(ldapServerProperties, dn);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        dn = new Dn("somethingelse=example1@domain.tdl");
        username = LdapNodeUtils.getUserName(ldapServerProperties, dn);
        assertThat(username, is(nullValue()));
        
        dn = new Dn("");
        username = LdapNodeUtils.getUserName(ldapServerProperties, dn);
        assertThat(username, is(nullValue()));
    }
    
    @Test
    public void testGetUsernameFromNode() {
        ExprNode node = new EqualityNode<>("cn", new StringValue("example1@domain.tdl"));
        String username = LdapNodeUtils.getUserName(ldapServerProperties, node);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        node = new EqualityNode<>("mail", new StringValue("example1@domain.tdl"));
        username = LdapNodeUtils.getUserName(ldapServerProperties, node);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        node = new EqualityNode<>("somethingelse", new StringValue("example1@domain.tdl"));
        username = LdapNodeUtils.getUserName(ldapServerProperties, node);
        assertThat(username, is(nullValue()));
        
        node = new OrNode(
                new EqualityNode<>("cn", new StringValue("example1@domain.tdl")), 
                new EqualityNode<>("memberOf", new StringValue("some group")));
        username = LdapNodeUtils.getUserName(ldapServerProperties, node);
        assertThat(username, is(equalTo("example1@domain.tdl")));
        
        node = new OrNode(
                new EqualityNode<>("memberOf", new StringValue("some group 1")), 
                new EqualityNode<>("memberOf", new StringValue("some group 2")));
        username = LdapNodeUtils.getUserName(ldapServerProperties, node);
        assertThat(username, is(nullValue()));
    }
    
    private class MockedDnFactory implements DnFactory {
        @Override
        public Dn create(String... upRdns) throws LdapInvalidDnException {
            return new Dn(upRdns);
        }

        @Override
        public Dn create(String upDn) throws LdapInvalidDnException {
            return new Dn(upDn);
        }
    }
}
