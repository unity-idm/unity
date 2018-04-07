/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.cursor.ListCursor;


import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
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
import pl.edu.icm.unity.types.basic.Entity;
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

    private final LdapServerAuthentication auth;

    private final UserMapper userMapper;

    private LdapServerFacade lsf;

    private final SessionManagement sessionMan;

    private final AuthenticationRealm realm;

    private final AttributesManagement attributesMan;

    private final IdentitiesManagement identitiesMan;

    private final LdapServerProperties configuration;
    
    private final LdapAttributeUtils attributeUtils;

    /**
     * Creates a new instance of DefaultAuthorizationInterceptor.
     * @param auth
     * @param sessionMan
     * @param realm
     * @param attributesMan
     * @param identitiesMan
     * @param configuration
     * @param userMapper
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
        this.attributeUtils = new LdapAttributeUtils(lsf, identitiesMan, configuration);
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

    
    protected void handleSearchNode(SearchOperationContext searchContext, ExprNode node, List<Entry> entries) throws LdapException {
        //TODO: handle all node types    
        if (node instanceof OrNode) {
            for( ExprNode childNode : ((OrNode)node).getChildren()) {
                handleSearchNode(searchContext, childNode, entries);
            }                
        } else if (node instanceof AndNode) {
            log.warn("And operator not supported");
        } else if (node.isLeaf()) {
            handleLeafNode(searchContext, node, entries);
        } else {
            log.warn("Operator not supported");
        }
    }
    
    protected void handleLeafNode(SearchOperationContext searchContext, ExprNode node, List<Entry> entries) throws LdapException {
        // admin bind will not return true (we look for cn)
        boolean userSearch = LdapNodeUtils.isUserSearch(configuration, node);
        
        // e.g., search by mail
        if (userSearch)
        {
            String username = LdapNodeUtils.getUserName(configuration, node);
            if (null == username) {
                return;
            }
            entries.add(getUnityUserEntry(searchContext, username));
        } else {
            String username = 
                LdapNodeUtils.parseGroupOfNamesSearch(dnFactory, configuration, node);
            if (null != username) {
                long userEntityId;
                try {
                    userEntityId = userMapper.resolveUser(username, realm.getName());
                    Map<String, GroupMembership> grps = identitiesMan.getGroups(
                        new EntityParam(userEntityId)
                    );
                    String return_format = configuration.getValue(
                        LdapServerProperties.GROUP_OF_NAMES_RETURN_FORMAT
                    );
                    if (null == return_format) {
                        throw new LdapOtherException("Configuration error in GROUP_OF_NAMES_RETURN_FORMAT");
                    }

                    for (Map.Entry<String, GroupMembership> agroup : grps.entrySet()) {
                        Entry e = new DefaultEntry(lsf.getDs().getSchemaManager());
                        e.setDn(String.format(return_format, agroup.getKey()));
                        entries.add(e);
                    }                   
                } catch (EngineException e) {
                    throw new LdapOtherException("Error when searching", e);
                }
            }
        }
    }
    
    /**
     * Search operation is very limited in respect to complete LDAP.
     *
     * We support direct user search and general (non user) attribute search.
     * Even though it may seem as very limited, it is enough to connect
     * several important applications.
     * 
     * @param searchContext
     * @return 
     * @throws org.apache.directory.api.ldap.model.exception.LdapException
     */
    @Override
    public EntryFilteringCursor search(SearchOperationContext searchContext)
            throws LdapException
    {
	CoreSessionExt session = (CoreSessionExt) searchContext.getSession();
	setUnityInvocationContext(session.getSession());
	try
	{
            List<Entry> entries = new ArrayList<>();
            ExprNode rootNode = searchContext.getFilter();
            handleSearchNode(searchContext, rootNode, entries);
            
            if (entries.isEmpty()) {
                return emptyResult(searchContext);
            } else {
                return new EntryFilteringCursorImpl(
                    new ListCursor<>(entries), searchContext, this.schemaManager
                );
            }
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
     * 
     * @param compareContext
     * @return 
     * @throws org.apache.directory.api.ldap.model.exception.LdapException
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

            // we know how to do group members only
            if (!at.getName().equals(group_member)) {
                log.warn(String.format("comparing with unsupported attribute [%s]", at.getName()));
                notSupported();
            }

            // ensure the DN is what we expect it to be
            String group_member_dn_regexp = configuration.getValue(
                LdapServerProperties.GROUP_MEMBER_DN_REGEXP
            );
            Pattern dn_meber_regexp = Pattern.compile(group_member_dn_regexp);
            Matcher m = dn_meber_regexp.matcher(compareContext.getDn().toString());
            if (!m.find()) {
                log.warn("comparing with unsupported DN");
                notSupported();
            }

            // if regexp contained capturing group
            // use it as the group name
            String group = null;
            if (0 < m.groupCount()) {
                group = m.group(1);
            }

            try
            {
                if (null == group) {
                    group = compareContext.getDn().getRdn().getValue();
                }
                assert null != group;

                // unity groups always start with /
                if (!group.startsWith("/")) {
                    group = "/" + group;
                }

                String user = LdapNodeUtils.getUserName(
                    configuration, new Dn(schemaManager, compareContext.getValue().toString())
                );

                long userEntityId = userMapper.resolveUser(user, realm.getName());
                Map<String, GroupMembership> grps = identitiesMan.getGroups(
                    new EntityParam(userEntityId)
                );
                return grps.containsKey(group);
            } catch (EngineException e)
            {
                throw new LdapOtherException("Error when comparing", e);
            }
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
     * Get Unity user from LDAP search context.
     */
    private Entry getUnityUserEntry(SearchOperationContext searchContext, String username)
            throws LdapException
    {
        Entry entry = new DefaultEntry(lsf.getDs().getSchemaManager());
        try
        {
            long userEntityId = userMapper.resolveUser(username, realm.getName());            
            Entity userEntity = identitiesMan.getEntity(new EntityParam(userEntityId));            
            
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
                    if (aName.equals(alias))
                    {
                        String requestDn = String.format("%s=%s", alias, username);
                        // FIXME: is this what we want in all cases?
                        if (null != searchContext.getDn())
                        {
                            requestDn += "," + searchContext.getDn().toString();
                        }
                        entry.setDn(requestDn);
                    } else {
                        attributeUtils.
                            addAttribute(aName, userEntity, username, attrs, entry);
                        
                    }
                }
            }

            // the purpose of a search can be to get DN (from another attribute)
            //  - even if no attributes are in getReturningAttributes()
            if (null == entry.getDn() || entry.getDn().isEmpty() ) {
                entry.setDn(searchContext.getDn());
            }

            return entry;
        } catch (IllegalIdentityValueException ignored)
        {
            log.warn("Requested user not found: " + username, ignored);
        } catch (Exception e)
        {
            
            throw new LdapOtherException("Error establishing user information", e);
        }

        return null;//emptyResult(searchContext);
    }

    /**
     * Get Unity user from LDAP search context.
     */
    private EntryFilteringCursor getUnityUser(SearchOperationContext searchContext, String username)
            throws LdapException
    {
        try
        {
            Entry userEntry = getUnityUserEntry(searchContext, username);
            if (userEntry != null) {
                return new EntryFilteringCursorImpl(new SingletonCursor<>(userEntry),
                        searchContext, lsf.getDs().getSchemaManager()
                );
            }

        } catch (Exception e)
        {
            
            throw new LdapOtherException("Error establishing user information", e);
        }

        return emptyResult(searchContext);
    }
}