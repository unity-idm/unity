/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.log4j.Logger;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;

/**
 *
 * @author Willem Elbers <willem@clarin.eu>
 */
public class LdapAttributeUtils {
    
    private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapAttributeUtils.class);
    
    private final IdentitiesManagement identitiesMan;
    private final LdapServerFacade lsf;
    private final LdapServerProperties props;
    private final Set<String> keys;
    
    public final static String MEMBER_OF_AT = "memberof";
    public final static String MEMBER_OF_AT_OID = "2.16.840.1.113894.1.1.424";
    
    public LdapAttributeUtils(LdapServerFacade lsf, IdentitiesManagement identitiesMan, LdapServerProperties props) {
        this.lsf = lsf;
        this.identitiesMan = identitiesMan;
        this.props = props;
        this.keys = props.getStructuredListKeys(LdapServerProperties.ATTRIBUTES_MAP_PFX);
    }
    
    /**
     * Add user attributes to LDAP answer
     *
     * Fixed mappings:
     * - SchemaConstants.USER_PASSWORD_AT: never released
     * - SchemaConstants.CN_AT: release username
     * - MEMBER_OF_AT
     * - All other attributes are dynamicall configured
     * 
     * TODO: this should be configurable
     * KB: and part should clearly be in a separated class with generic value type -> LDAP representation handling.
     * 
     * @param name
     * @param userEntity
     * @param username
     * @param attrs
     * @param toEntry
     * @throws org.apache.directory.api.ldap.model.exception.LdapException
     * @throws pl.edu.icm.unity.exceptions.EngineException
     */
    public void addAttribute(String name, Entity userEntity, String username, Collection<AttributeExt<?>> attrs,
            Entry toEntry) throws LdapException, EngineException
    {
        
            
        Attribute da = null;
        switch (name)
        {
        case SchemaConstants.USER_PASSWORD_AT:
            da = lsf.getAttribute(SchemaConstants.USER_PASSWORD_AT, SchemaConstants.USER_PASSWORD_AT_OID);
            da.add("not disclosing");
            break;
        case SchemaConstants.CN_AT:
            da = lsf.getAttribute(SchemaConstants.CN_AT, SchemaConstants.CN_AT_OID);
            da.add(username);
            break;
        case MEMBER_OF_AT:
            Map<String, GroupMembership> grps = identitiesMan.getGroups(new EntityParam(userEntity.getId()));
            if (grps != null) {
                da = lsf.getAttribute(MEMBER_OF_AT, MEMBER_OF_AT_OID);            
                for (Map.Entry<String, GroupMembership> agroup : grps.entrySet()) {
                        String group = agroup.getValue().getGroup();
                        String[] groupParts = group.split("/");
                        String values = "";
                        for(int i = groupParts.length-1; i >= 0; i--) {
                            String g = groupParts[i];
                            if(!g.isEmpty()) {
                                values += "cn="+g+",";
                            }
                        }
                        //Remove trailing comma
                        if(values.endsWith(",")) {
                            values = values.substring(0, values.length()-1);
                        }
                        //Add value to attribute
                        if(!values.isEmpty()) {
                            da.add(values);
                        }
                }
            }
            break;            
        default:
            da = addDynamicMappedAttribute(name, attrs, userEntity);
            break;
        }
            
/*            
        case SchemaConstants.MAIL_AT:
            da = mapUnityIdentityToLdapAttribute(userEntity, "email", SchemaConstants.MAIL_AT, SchemaConstants.MAIL_AT_OID);
            //TODO: Also support mail attribute in addition to email identity?
            break;
        case SchemaConstants.EMAIL_AT:
            da = mapUnityIdentityToLdapAttribute(userEntity, "email", SchemaConstants.EMAIL_AT, SchemaConstants.EMAIL_AT_OID);
            //TODO: Also support email attribute in addition to email identity?
            break;    
        case SchemaConstants.DISPLAY_NAME_AT:
            da = mapUnityAttributeToLdapAttribute(attrs, "fullName", SchemaConstants.DISPLAY_NAME_AT, SchemaConstants.DISPLAY_NAME_AT_OID);
            break;
        
        default:
            for (AttributeExt<?> ae : attrs)
            {
                if (ae.getName().equals(name))
                {
                    da = lsf.getAttribute(name, null);

                    Object o = ae.getValues().get(0);
                    if (o instanceof BufferedImage)
                    {
                        try
                        {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write((BufferedImage) o, "jpg",	baos);
                            baos.flush();
                            byte[] imageInByte = baos.toByteArray();
                            da.add(imageInByte);
                            baos.close();
                        } catch (IOException e)
                        {
                            throw new LdapOtherException(
                                    "Can not serialize image to byte array", e);
                        }
                    } else
                    {
                        da.add(o.toString());
                    }
                }
            }
            break;
        }
*/
        if (null != da && da.get() != null)
        {
            toEntry.add(da);
        }
    }
        
    /**
     * 
     * 
     * @param name
     * @param attrs
     * @param userEntity
     * @return
     * @throws LdapException 
     */
    private Attribute addDynamicMappedAttribute(String name, Collection<AttributeExt<?>> attrs, Entity userEntity) throws LdapException {
        Attribute da = null;
        
        for(String key: keys) {
            String atName = props.getValue(key+LdapServerProperties.ATTRIBUTES_MAP_LDAP_AT);
            String oidName = props.getValue(key+LdapServerProperties.ATTRIBUTES_MAP_LDAP_OID);                
            String unityAttribute = props.getValue(key+LdapServerProperties.ATTRIBUTES_MAP_UNITY_ATRIBUTE);
            String unityIdentity = props.getValue(key+LdapServerProperties.ATTRIBUTES_MAP_UNITY_IDENTITY);
            
            if (name.equalsIgnoreCase(atName)) {            
                if (!unityAttribute.isEmpty()) {
                    da = mapUnityAttributeToLdapAttribute(attrs, unityAttribute, atName, oidName);
                } else if (!unityIdentity.isEmpty()) {
                    da = mapUnityIdentityToLdapAttribute(userEntity, unityIdentity, atName, oidName);
                } else {
                    //error
                }
                break;
            }
        }
        
        return da;
    }
    
    /**
     * Return an LDAP attribute with its value set to the first value of the 
     * unity attribute specified by attributeName, or null if the unity attribute 
     * is not found in the collection.
     * 
     * @param unityAttrs Collection of unity attributes
     * @param attributeName Name of the unity attribute
     * @param uid LDAP attribute name
     * @param upId LDAP attribute name (OID)
     * @return  LDAP attribute
     * @throws LdapException
     */
    private Attribute mapUnityAttributeToLdapAttribute(Collection<AttributeExt<?>> unityAttrs, String attributeName, String uid, String upId) throws LdapException {
        //Lookup the first value of the unity attribute
        String value = null;
        for (AttributeExt<?> ae : unityAttrs) {
            if (ae.getName().equals(attributeName)) {
                value = ae.getValues().get(0).toString();
            }                            
        }
        
        //Add the unity attribute value to the LDAP attribute
        if (value != null) {
            Attribute da = lsf.getAttribute(uid, upId);
            da.add(value);
            log.debug(String.format("Mapped unity attribute [%s] to ldap attribute [%s/%s] with value: %s.", attributeName, uid, upId, value));
            return da;
        } else {
            log.warn(String.format("Failed to map unity attribute [%s] to ldap attribute [%s/%s]. Cause: unity attribute not found.", attributeName, uid, upId));
            String availableAttributes = "";
            for (AttributeExt<?> ae : unityAttrs) {
                availableAttributes += ae.getName()+",";                 
            }
            log.debug(String.format("Available unity attributes [%s].", availableAttributes.substring(0, availableAttributes.length()-1)));
            return null;
        }
    }
    
    /**
     * Return an LDAP attribute with its value set to value of the unity  
     * identity of the specified type, or null if the unity identity of that 
     * type does not exist.
     * 
     * @param userEntity
     * @param identityTypeId
     * @param uid
     * @param upId
     * @return
     * @throws LdapException 
     */
    private Attribute mapUnityIdentityToLdapAttribute(Entity userEntity, String identityTypeId, String uid, String upId) throws LdapException {
        //Lookup identity of the requested type
        Identity id = null;
        for(Identity identity : userEntity.getIdentities()) {
            if (identity.getTypeId().equals(identityTypeId)) {
                id = identity;
                break;
            }
        }
            
        //Add the unity identity value to the LDAP attribute
        if (id != null) {
            Attribute da = lsf.getAttribute(uid, upId);
            da.add(id.getValue());
            log.debug(String.format("Mapped unity identity of type [%s] to ldap attribute [%s/%s] with value: %s.", identityTypeId, uid, upId, id.getValue()));
            return da;
        } else {
            log.warn(String.format("Failed to map unity identity to ldap attribute [%s/%s]. Cause: no unity identity of type %s found.", uid, upId, identityTypeId));
            String availableIdentities = "";
            for(Identity identity : userEntity.getIdentities()) {
                availableIdentities += identity.getTypeId() + ",";
            }
            if (availableIdentities.length() > 0) {
                log.debug(String.format("Available unity identities [%s].", availableIdentities.substring(0, availableIdentities.length()-1)));
            } else {
                log.debug(String.format("No identities available."));
            }
        }
        
        return null;        
    }
}
