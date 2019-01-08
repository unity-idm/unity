/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
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
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * This is the integration part between ApacheDS and Unity.
 *
 * The ultimate goal here is to implement LDAP protocol and map it to objects
 * and functions from Unity. At the moment, minimalistic implementation is
 * provided that makes connecting e.g., Drupal/Apache via their LDAP modules
 * possible.
 *
 * The most important methods are `lookup`, `bind` (`unbind`), `search`,
 * `compare` and `unbind`. LDAP messages (arguments to the methods above) are
 * inspected and if we support the specific operation, Unity API is called.
 *
 * Authentication is provided via InvocationContexts.
 *
 * Notes: - SASL not supported
 */
class LdapApacheDSInterceptor extends BaseInterceptor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT,
			LdapApacheDSInterceptor.class);
	
	private final LdapServerAuthentication auth;
	private final UserMapper userMapper;
	private final LdapServerFacade lsf;
	private final SessionManagement sessionMan;
	private final AuthenticationRealm realm;
	private final EntityManagement identitiesMan;
	private final LdapServerProperties configuration;
	private final LdapSearch ldapSearch;
	private final String authenticatorId;

	LdapApacheDSInterceptor(LdapServerAuthentication auth, SessionManagement sessionMan,
			AuthenticationRealm realm, AttributesManagement attributesMan,
			EntityManagement identitiesMan, LdapServerProperties configuration,
			UserMapper userMapper, LdapServerFacade lsf, String authenticatorId)
	{
		this.userMapper = userMapper;
		this.auth = auth;
		this.sessionMan = sessionMan;
		this.realm = realm;
		this.identitiesMan = identitiesMan;
		this.configuration = configuration;
		this.lsf = lsf;
		this.authenticatorId = authenticatorId;
		LdapAttributeUtils attributeUtils = new LdapAttributeUtils(lsf, identitiesMan, configuration);
		this.ldapSearch = new LdapSearch(configuration, identitiesMan, realm, 
				userMapper, dnFactory, schemaManager, attributesMan, attributeUtils);
	}

	@Override
	public void init(DirectoryService directoryService) throws LdapException
	{
		super.init(directoryService);
	}

	//FIXME what for is lookup? Do we need to support it?
	@Override
	public Entry lookup(LookupOperationContext lookupContext) throws LdapException
	{
		// lookup is performed for many reasons, in case the user that performs
		// the lookup is identified as `ADMIN_SYSTEM_DN` let LDAP core handle the search
		if (!lookupContext.getDn().isEmpty())
		{
			boolean isMainBindUser = lookupContext.getDn().toString()
					.equals(ServerDNConstants.ADMIN_SYSTEM_DN);
			if (isMainBindUser)
			{
				Entry entry = next(lookupContext);
				return entry;
			}
		}

		//FIXME what is that?
		// Require ldap-user - cn should be enough
		// Require ldap-group - groups
		// Require ldap-dn - cn should be enough
		// Require ldap-attribute - not supported
		// Require ldap-filter - not supported

		Entry entry = new DefaultEntry(schemaManager, lookupContext.getDn());
		Optional<String> user = LdapNodeUtils.getUserName(configuration, lookupContext.getDn());
		if (user.isPresent())
		{
			if (lookupContext.isAllUserAttributes())
			{
				AttributeType OBJECT_CLASS_AT = schemaManager
						.lookupAttributeTypeRegistry(
								SchemaConstants.OBJECT_CLASS_AT);
				//FIXME this must be configurable
				entry.put("objectClass", OBJECT_CLASS_AT, "top", "person",
						"inetOrgPerson", "organizationalPerson");
				// FIXME - what shall be returned?
				// entry.put( "cn",
				// schemaManager.lookupAttributeTypeRegistry("cn"),
				// "we do not want to expose this"
				// );
				return new ClonedServerEntry(entry);
			}
		}

		// we let lookup use the Apache DS core LDAP implementation
		return next(lookupContext);
	}


	/**
	 * Search operation is very limited in respect to complete LDAP.
	 *
	 * We support direct user search and general (non user) attribute
	 * search. Even though it may seem as very limited, it is enough to
	 * connect several important applications.
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
			ExprNode rootNode = searchContext.getFilter();
			List<Entry> entries = ldapSearch.searchEntries(searchContext, rootNode);

			if (entries.isEmpty())
			{
				return emptyResult(searchContext);
			} else
			{
				return new EntryFilteringCursorImpl(new ListCursor<>(entries),
						searchContext, this.schemaManager);
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
			throw new LdapAuthenticationException(
					"SASL authentication is not supported");
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
			if (authnResult.getAuthenticatedEntity().getOutdatedCredentialId() != null)
			{
				log.debug("Blocking access as an outdated credential was used for bind, user is "
						+ bindContext.getDn());
				throw new LdapAuthenticationException("Outdated credential");
			}

			LdapPrincipal policyConfig = new LdapPrincipal(schemaManager,
					bindContext.getDn(), AuthenticationLevel.SIMPLE);
			// do not expose anything private
			bindContext.setCredentials(null);
			policyConfig.setUserPassword(new byte[][] { new byte[] {} });
			LoginSession ls = sessionMan.getCreateSession(
					authnResult.getAuthenticatedEntity().getEntityId(), realm,
					"", null, new RememberMeInfo(false, false), 
					authenticatorId, null);
			CoreSessionExt mods = new CoreSessionExt(policyConfig, directoryService,
					ls);
			bindContext.setSession(mods);
		} else
		{
			log.debug("LDAP authentication failed for " + bindContext.getDn());
			throw new LdapAuthenticationException("Invalid credential");
		}
	}

	private InvocationContext setUnityInvocationContext(LoginSession ls)
	{
		InvocationContext ctx = new InvocationContext(null, realm, Collections.emptyList());
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
			if (null == at)
			{
				log.warn("compare got invalid AttributeType");
				return false;
			}

			String group_member = configuration
					.getValue(LdapServerProperties.GROUP_MEMBER);

			// we know how to do group members only
			if (!at.getName().equals(group_member))
			{
				log.warn(String.format("comparing with unsupported attribute [%s]",
						at.getName()));
				notSupported();
			}

			// ensure the DN is what we expect it to be
			String group_member_dn_regexp = configuration
					.getValue(LdapServerProperties.GROUP_MEMBER_DN_REGEXP);
			Pattern dn_meber_regexp = Pattern.compile(group_member_dn_regexp);
			Matcher m = dn_meber_regexp.matcher(compareContext.getDn().toString());
			if (!m.find())
			{
				log.warn("comparing with unsupported DN");
				notSupported();
			}

			// if regexp contained capturing group
			// use it as the group name
			String group = null;
			if (0 < m.groupCount())
			{
				group = m.group(1);
			}

			try
			{
				if (null == group)
				{
					group = compareContext.getDn().getRdn().getValue();
				}
				assert null != group;

				// unity groups always start with /
				if (!group.startsWith("/"))
				{
					group = "/" + group;
				}

				String user = LdapNodeUtils.extractUserName(configuration, new Dn(
						schemaManager,
						compareContext.getValue().toString()));

				long userEntityId = userMapper.resolveUser(user, realm.getName());
				Map<String, GroupMembership> grps = identitiesMan
						.getGroups(new EntityParam(userEntityId));
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

	/**
	 * @return Empty LDAP result.
	 */
	private EntryFilteringCursorImpl emptyResult(SearchOperationContext searchContext)
	{
		return new EntryFilteringCursorImpl(new EmptyCursor<>(), searchContext,
				lsf.getDs().getSchemaManager());
	}
	

	/**
	 * Indicate we do not support the operation or particular code flow.
	 * 
	 * @throws LdapException
	 */
	private void notSupported() throws LdapException
	{
		throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM);
	}
}