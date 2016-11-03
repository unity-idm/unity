/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.util.StringConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * This is the integration part between ApacheDS and Unity.
 *
 * The ultimate goal here is to implement LDAP protocol and map it to objects and
 * functions from Unity. At the moment, minimalistic implementation is provided
 * that makes connecting e.g., Drupal/Apache via their LDAP modules possible.
 *
 * The most important methods are `lookup`, `bind` (`unbind`), `search`, `compare` and `unbind`.
 * LDAP messages (arguments to the methods above) are inspected and if we support
 * the specific operation, Unity API is called.
 *
 * Authentication is provided via InvocationContexts.
 *
 * Notes:
 *  - SASL not supported
 */
public class LdapApacheDSInterceptor extends BaseInterceptor
{
    private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapApacheDSInterceptor.class);

    private LdapServerAuthentication auth;

    private UserMapper userMapper;

    private LdapServerFacade lsf;

    private SessionManagement sessionMan;

    private AuthenticationRealm realm;

    private AttributesManagement attributesMan;

    private IdentitiesManagement identitiesMan;

    private LdapServerProperties configuration;

    /**
     * Creates a new instance of DefaultAuthorizationInterceptor.
     */
    public LdapApacheDSInterceptor(LdapServerAuthentication auth, SessionManagement sessionMan,
            AuthenticationRealm realm, AttributesManagement attributesMan,
            IdentitiesManagement identitiesMan, LdapServerProperties configuration,
            UserMapper userMapper)
    {
        super();
        this.userMapper = userMapper;
        this.lsf = null;
        this.auth = auth;
        this.sessionMan = sessionMan;
        this.realm = realm;
        this.attributesMan = attributesMan;
        this.identitiesMan = identitiesMan;
        this.configuration = configuration;
    }

    public void setLdapServerFacade(LdapServerFacade lsf)
    {
        this.lsf = lsf;
    }

    /**
     * Indicate we do not support the operation or particular code flow.
     * @throws LdapException
     */
    private void notSupported() throws LdapException
    {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM);
    }

    @Override
    public void init(DirectoryService directoryService) throws LdapException
    {
        super.init(directoryService);
    }

    //
    // supported operations
    //

    @Override
    public Entry lookup(LookupOperationContext lookupContext) throws LdapException
    {
        // lookup is performed for many reasons, in case the user that performs
        // the lookup is identified as `ADMIN_SYSTEM_DN` let LDAP core handle the
        // search
        if (!lookupContext.getDn().isEmpty())
        {
            boolean isMainBindUser = lookupContext.getDn().toString().equals(
                ServerDNConstants.ADMIN_SYSTEM_DN
            );
            if (isMainBindUser)
            {
                Entry entry = next(lookupContext);
                return entry;
            }
        }

        // Require ldap-user - cn should be enough
        // Require ldap-group - groups
        // Require ldap-dn - cn should be enough
        // Require ldap-attribute - not supported
        // Require ldap-filter - not supported

        Entry entry = new DefaultEntry(schemaManager, lookupContext.getDn());

        // specific lookup for
        String group = LdapNodeUtils.getGroup(configuration, lookupContext.getDn());
        if (null != group)
        {
            if (lookupContext.isAllOperationalAttributes())
            {
                notSupported();
                // example what can be returned when implemented properly
//				AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry(
//					SchemaConstants.OBJECT_CLASS_AT
//				);
//				entry.put(
//					"objectClass", OBJECT_CLASS_AT,
//					"top", "person",
//					"inetOrgPerson", "organizationalPerson"
//				);
//				entry.put(
//					"cn", schemaManager.lookupAttributeTypeRegistry("cn"), "test"
//				);
//				return new ClonedServerEntry(entry);
            }
        }

        String user = LdapNodeUtils.getUserName(configuration, lookupContext.getDn());
        if (null != user)
        {
            if (lookupContext.isAllUserAttributes())
            {
                AttributeType OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry(
                    SchemaConstants.OBJECT_CLASS_AT
                );
                entry.put(
                    "objectClass", OBJECT_CLASS_AT,
                    "top", "person",
                    "inetOrgPerson", "organizationalPerson"
                );
                // FIXME - what shall be returned?
                //entry.put(
                // 	"cn", schemaManager.lookupAttributeTypeRegistry("cn"),
                //	"we do not want to expose this"
                //);
                return new ClonedServerEntry(entry);
            }
        }

        // we let lookup use the Apache DS core LDAP implementation
        return next(lookupContext);
    }

    /**
     * Search operation is very limited in respect to complete LDAP.
     *
     * We support direct user search and general (non user) attribute search.
     * Even though it may seem as very limited, it is enough to connect
     * several important applications.
     */
    @Override
    public EntryFilteringCursor search(SearchOperationContext searchContext)
            throws LdapException
    {
	CoreSessionExt session = (CoreSessionExt) searchContext.getSession();
	setUnityInvocationContext(session.getSession());
	try
	{
		// admin bind will not return true (we look for cn)
		boolean userSearch = LdapNodeUtils.isUserSearch(
				configuration, searchContext.getFilter()
				);
		String username = LdapNodeUtils.getUserName(
				configuration, searchContext.getFilter()
				);

		// e.g., search by mail
		if (userSearch)
		{
			if (null == username) {
				return emptyResult(searchContext);
			}else {
				return getUnityUser(searchContext, username);
			}
		}

		EntryFilteringCursor ec = next(searchContext);
		return ec;
	} finally
	{
		InvocationContext.setCurrent(null);
	}
    }

    @Override
    public void bind(BindOperationContext bindContext) throws LdapException
    {
        if (bindContext.isSaslBind())
        {
            log.debug("Blocking unsupported SASL bind");
            throw new LdapAuthenticationException("SASL authentication is not supported");
        }

        // check username and password using Unity's API
        AuthenticationResult authnResult;
        try
        {
            authnResult = auth.authenticate(configuration, bindContext);
        } catch (Exception e)
        {
            throw new LdapOtherException("Error while authenticating", e);
        }

        // if successful, create sessions
        //
        if (authnResult.getStatus() == Status.success)
        {
            if (authnResult.getAuthenticatedEntity().isUsedOutdatedCredential())
            {
                log.debug("Blocking access as an outdated credential was used for bind, user is " +
                    bindContext.getDn());
                throw new LdapAuthenticationException("Outdated credential");
            }

            LdapPrincipal policyConfig = new LdapPrincipal(
                schemaManager, bindContext.getDn(), AuthenticationLevel.SIMPLE
            );
            // do not expose anything private
            bindContext.setCredentials(null);
            policyConfig.setUserPassword(new byte[][] { StringConstants.EMPTY_BYTES });
            LoginSession ls = sessionMan.getCreateSession(
        	                authnResult.getAuthenticatedEntity().getEntityId(), realm, "", false, null);
            CoreSessionExt mods = new CoreSessionExt(policyConfig, directoryService, ls);
            bindContext.setSession(mods);
        } else
        {
            log.debug("LDAP authentication failed for " + bindContext.getDn());
            throw new LdapAuthenticationException("Invalid credential");
        }
    }

    private InvocationContext setUnityInvocationContext(LoginSession ls)
    {
        InvocationContext ctx = new InvocationContext(null, realm);
        ctx.setLoginSession(ls);
        InvocationContext.setCurrent(ctx);
        return ctx;
    }

    @Override
    public void unbind(UnbindOperationContext unbindContext) throws LdapException
    {
	CoreSessionExt session = (CoreSessionExt) unbindContext.getSession();
	LoginSession unitySession = session.getSession();

	sessionMan.removeSession(unitySession.getId(), false);
        InvocationContext.setCurrent(null);
    }

    /**
     * Find user, get his groups and return true if he is the member of
     * desired group.
     */
    @Override
    public boolean compare(CompareOperationContext compareContext) throws LdapException
    {
        CoreSessionExt session = (CoreSessionExt) compareContext.getSession();
        setUnityInvocationContext(session.getSession());
        try
        {

            AttributeType at = compareContext.getAttributeType();
            if (null == at) {
                log.warn("compare got invalid AttributeType");
                return false;
            }

            String group_member = configuration.getValue(
                LdapServerProperties.GROUP_MEMBER
            );
            // we know how to do members only
            if (!at.getName().equals(group_member)) {
                return next(compareContext);
            }

            String group_member_user_regexp = configuration.getValue(
                LdapServerProperties.GROUP_MEMBER_USER_REGEXP
            );
            try
            {
                String user = compareContext.getValue().getString();
                Pattern p = Pattern.compile(group_member_user_regexp);
                Matcher m = p.matcher(user);
                if (m.find())
                {
                    if (0 == m.groupCount()) {
                        String msg = String.format(
                            "Invalid [%s] settings - no group specified",
                            LdapServerProperties.GROUP_MEMBER_USER_REGEXP
                        );
                        throw new LdapOtherException(msg);
                    }
                    user = m.group(1);
                    long userEntityId = userMapper.resolveUser(user, realm.getName());
                    Map<String, GroupMembership> grps = identitiesMan.getGroups(
                        new EntityParam(userEntityId)
                    );
                    String group = LdapNodeUtils.getGroup(
                        configuration, compareContext.getOriginalEntry().getDn()
                    );
                    return grps.containsKey(group);
                }
            } catch (EngineException e)
            {
                throw new LdapOtherException("Error when comparing", e);
            }
            return false;
        } finally
        {
            InvocationContext.setCurrent(null);
        }
    }

    //
    // not supported
    //

    @Override
    public void rename(RenameOperationContext renameContext) throws LdapException
    {
        notSupported();
    }

    @Override
    public void moveAndRename(MoveAndRenameOperationContext moveAndRenameContext)
            throws LdapException
    {
        notSupported();
    }

    @Override
    public void move(MoveOperationContext moveContext) throws LdapException
    {
        notSupported();
    }

    @Override
    public void modify(ModifyOperationContext modifyContext) throws LdapException
    {
        notSupported();
    }

    @Override
    public boolean hasEntry(HasEntryOperationContext hasEntryContext) throws LdapException
    {
        notSupported();
        return false;
    }

    @Override
    public Entry getRootDse(GetRootDseOperationContext getRootDseContext) throws LdapException
    {
        notSupported();
        return null;
    }

    @Override
    public void delete(DeleteOperationContext deleteContext) throws LdapException
    {
        notSupported();
    }

    @Override
    public void add(AddOperationContext addContext) throws LdapException
    {
        notSupported();
    }

    //
    // helpers
    //

    /**
     * @return Empty LDAP result.
     */
    private EntryFilteringCursorImpl emptyResult(SearchOperationContext searchContext)
    {
        return new EntryFilteringCursorImpl(new EmptyCursor<>(), searchContext, lsf
                .getDs().getSchemaManager());
    }

    /**
     * Add user attributes to LDAP answer
     *
     * TODO: this should be configurable
     * KB: and part should clearly be in a separated class with generic value type -> LDAP representation handling.
     */
    private void addAttribute(String name, String username, Collection<AttributeExt<?>> attrs,
            Entry toEntry) throws LdapException
    {
        Attribute da = null;

        switch (name)
        {
        case SchemaConstants.USER_PASSWORD_AT:
            da = lsf.getAttribute(SchemaConstants.USER_PASSWORD_AT,
                    SchemaConstants.USER_PASSWORD_AT_OID);
            da.add("not disclosing");
            break;
        case SchemaConstants.CN_AT:
            da = lsf.getAttribute(SchemaConstants.CN_AT, SchemaConstants.CN_AT_OID);
            da.add(username);
            break;
        case SchemaConstants.MAIL_AT:
        case SchemaConstants.EMAIL_AT:
            da = lsf.getAttribute(SchemaConstants.MAIL_AT, SchemaConstants.MAIL_AT_OID);
            for (AttributeExt<?> ae : attrs)
            {
                if (ae.getName().equals(SchemaConstants.MAIL_AT)
                        || ae.getName().equals(SchemaConstants.EMAIL_AT))
                {
                    Object o = ae.getValues().get(0);
                    da.add(o.toString());
                }
            }
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

        if (null != da)
        {
            toEntry.add(da);
        }
    }

    /**
     * Get Unity user from LDAP search context.
     */
    private EntryFilteringCursor getUnityUser(SearchOperationContext searchContext, String username)
            throws LdapException
    {
        Entry entry = new DefaultEntry(lsf.getDs().getSchemaManager());
        try
        {
            long userEntityId = userMapper.resolveUser(username, realm.getName());
            // get attributes if any
            Collection<AttributeExt<?>> attrs = attributesMan.getAttributes(
                new EntityParam(userEntityId), null, null
            );

            Set<AttributeTypeOptions> attributes = new HashSet<AttributeTypeOptions>();
            if (null != searchContext.getReturningAttributes())
            {
                attributes.addAll(searchContext.getReturningAttributes());
            } else
            {
                String default_attributes = configuration.getValue(
                    LdapServerProperties.RETURNED_USER_ATTRIBUTES
                );
                for (String at : default_attributes.split(","))
                {
                    attributes.add(
                        new AttributeTypeOptions(
                            schemaManager.lookupAttributeTypeRegistry(at.trim())
                        )
                    );
                }
            }

            // iterate through requested attributes and try to match them with
            // identity attributes
            String[] aliases = configuration.getValue(
                LdapServerProperties.USER_NAME_ALIASES
            ).split(",");
            for (String alias : aliases)
            {
                for (AttributeTypeOptions ao : attributes)
                {
                    String aName = ao.getAttributeType().getName();
                    addAttribute(aName, username, attrs, entry);
                    if (aName.equals(alias))
                    {
                        String requestDn = String.format("%s=%s", alias, username);
                        // FIXME: is this what we want in all cases?
                        if (null != searchContext.getDn())
                        {
                            requestDn += "," + searchContext.getDn().toString();
                        }
                        entry.setDn(requestDn);
                    }
                }
            }

            // the purpose of a search can be to get DN (from another attribute)
            //  - even if no attributes are in getReturningAttributes()
            if (null == entry.getDn() || entry.getDn().isEmpty() ) {
                entry.setDn(searchContext.getDn());
            }

            return new EntryFilteringCursorImpl(new SingletonCursor<>(entry),
                    searchContext, lsf.getDs().getSchemaManager()
            );

        } catch (IllegalIdentityValueException ignored)
        {
            log.debug("Requested user not found: " + username, ignored);
        } catch (Exception e)
        {
            throw new LdapOtherException("Error establishing user information", e);
        }

        return emptyResult(searchContext);
    }

}