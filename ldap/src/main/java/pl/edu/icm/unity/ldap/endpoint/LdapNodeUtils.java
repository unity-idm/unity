/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.server.core.api.DnFactory;

/**
 * Static utility methods helping to extract data from {@link ExprNode}
 */
public class LdapNodeUtils {

    /**
     * @param configuration
     * @param node
     * @return Whether the LDAP query a user search?
     */
    public static boolean isUserSearch(LdapServerProperties configuration, ExprNode node) {
        if (node instanceof BranchNode) {
            BranchNode n = (BranchNode) node;
            for (ExprNode en : n.getChildren()) {
                if (isUserSearch(configuration, en)) {
                    return true;
                }
            }
        } else if (node instanceof SimpleNode) { //Switch to EqualityNode instead?
            SimpleNode<?> sns = (SimpleNode<?>) node;
            String[] aliases = configuration.getValue(
                    LdapServerProperties.USER_NAME_ALIASES
            ).split(",");            
            if (ArrayUtils.contains(aliases, sns.getAttribute())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param dnf
     * @param configuration
     * @param node
     * @return Whether the LDAP query a groupofnames search?
     */
    public static String parseGroupOfNamesSearch(DnFactory dnf, LdapServerProperties configuration, ExprNode node) {
        boolean is_gofn_search = false;
        String user = null;
        // no recursion and we expect at least two children
        // - member
        // - objectClass
        if (node instanceof BranchNode) {
            BranchNode n = (BranchNode) node;
            for (ExprNode en : n.getChildren()) {
                if (!(en instanceof SimpleNode)) {
                    continue;
                }
                SimpleNode<?> sns = (SimpleNode<?>) en;
                if (sns.getAttribute().equals(SchemaConstants.OBJECT_CLASS_AT)
                        && sns.getValue().toString().toLowerCase().equals(SchemaConstants.GROUP_OF_NAMES_OC.toLowerCase())) {
                    is_gofn_search = true;
                } else if (sns.getAttribute().equals(SchemaConstants.MEMBER_AT)) {
                    try {
                        Dn dn = dnf.create(sns.getValue().toString());
                        user = getUserName(configuration, dn);
                    } catch (LdapInvalidDnException e) {
                        return null;
                    }
                }
            }
        }

        if (is_gofn_search) {
            return user;
        }
        return null;
    }

    /**
     * 
     * @param configuration
     * @param dn
     * @return 
     */
    public static String getUserName(LdapServerProperties configuration, Dn dn) {
        String aliasesValue = configuration.getValue(
                LdapServerProperties.USER_NAME_ALIASES
        );
        if (aliasesValue == null) {
            //TODO: throw exception or log warning?
            return null;
        }
        String[] aliases = aliasesValue.split(",");
        for (String alias : aliases) {
            String part = getPart(dn, alias);
            if (null != part) {
                return part;
            }
        }
        return null;
    }

    /**
     * 
     * @param configuration
     * @param node
     * @return 
     */
    public static String getUserName(LdapServerProperties configuration, ExprNode node) {
        String[] aliases = configuration.getValue(
                LdapServerProperties.USER_NAME_ALIASES
        ).split(",");
        return getUserName(node, aliases);

    }

    //
    // private
    //
    
    /**
     * 
     * @param node
     * @param attribute
     * @return 
     */
    private static String getUserName(ExprNode node, String attribute) {

        if (node instanceof BranchNode) {
            BranchNode n = (BranchNode) node;
            for (ExprNode en : n.getChildren()) {
                String username = getUserName(en, attribute);
                if (null != username) {
                    return username;
                }
            }
        } else if (node instanceof SimpleNode) {
            try {
                SimpleNode<?> sns = (SimpleNode<?>) node;
                if (sns.getAttribute().equals(attribute)) {
                    return sns.getValue().toString();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * User name extracted from LDAP query based on attributes 
     * 
     * @param node
     * @param attributes
     * @return 
     */
    private static String getUserName(ExprNode node, String[] attributes) {
        for (int i = 0; i < attributes.length; ++i) {
            String uname = getUserName(node, attributes[i]);
            if (null != uname) {
                return uname;
            }
        }
        return null;
    }

    /**
     * Get query value from LDAP DN.
     * 
     * @param dn
     * @param query
     * @return 
     */
    private static String getPart(Dn dn, String query) {
        for (Rdn rdn : dn.getRdns()) {
            Ava ava = rdn.getAva();
            if (null == ava) {
                continue;
            }
            // can be e.g.,  2.5.4.3
            String key_name = ava.getType();
            if (null != ava.getAttributeType()) {
                // use name from schema
                key_name = ava.getAttributeType().getName();
            }
            if (key_name.equals(query)) {
                return ava.getValue().getString();
            }
        }
        return null;
    }

}
