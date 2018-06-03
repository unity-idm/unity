/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Implements core functionality of LDAP search operation, using Unity users store.
 * 
 */
class LdapSearch
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapSearch.class);
	
	private final LdapServerProperties configuration;
	private final EntityManagement identitiesMan;
	private final AuthenticationRealm realm;
	private final UserMapper userMapper;
	private final DnFactory dnFactory;
	private final SchemaManager schemaManager;
	private final AttributesManagement attributesMan;
	private final LdapAttributeUtils attributeUtils;
	
	LdapSearch(LdapServerProperties configuration, EntityManagement identitiesMan,
			AuthenticationRealm realm, UserMapper userMapper, 
			DnFactory dnFactory, SchemaManager schemaManager,
			AttributesManagement attributesMan, LdapAttributeUtils attributeUtils)
	{
		this.configuration = configuration;
		this.identitiesMan = identitiesMan;
		this.realm = realm;
		this.userMapper = userMapper;
		this.dnFactory = dnFactory;
		this.schemaManager = schemaManager;
		this.attributesMan = attributesMan;
		this.attributeUtils = attributeUtils;
	}

	List<Entry> searchEntries(SearchOperationContext searchContext, ExprNode node) 
			throws LdapException
	{
		if (node instanceof OrNode)
		{
			//FIXME - shouldn't we merge attributes by DN here?
			//FIXME - test needed
			List<Entry> union = new ArrayList<>();
			for (ExprNode childNode : ((OrNode) node).getChildren())
				union.addAll(searchEntries(searchContext, childNode));
			return union;
		} else if (node instanceof NotNode)
		{
			// TODO: handle
			throw new LdapOtherException("NOT operator not supported in searches");
		} else if (node instanceof AndNode)
		{
			//FIXME - proper implementation with intersection of attributes by DNs.
			List<Entry> union = new ArrayList<>();
			for (ExprNode childNode : ((AndNode) node).getChildren())
				union.addAll(searchEntries(searchContext, childNode));
			return new ArrayList<>(union);
		} else if (node.isLeaf())
		{
			return searchLeafNodeEntries(searchContext, node);
		} else
		{
			throw new LdapOtherException("Operator " + node.getAssertionType() 
				+ " not supported: " + node.toString());
		}
	}

	private List<Entry> searchLeafNodeEntries(SearchOperationContext searchContext, ExprNode node) 
			throws LdapException
	{
		// admin bind will not return true (we look for cn)
		boolean userSearch = LdapNodeUtils.isUserSearch(configuration, node);
		List<Entry> entries = new ArrayList<>();
		// e.g., search by mail
		if (userSearch)
		{
			String username = LdapNodeUtils.getUserName(configuration, node);
			if (null == username)
			{
				return entries;
			}
			entries.add(getUnityUserEntry(searchContext, username));
		} else
		{
			String username = LdapNodeUtils.parseGroupOfNamesSearch(dnFactory,
					configuration, node);
			if (null != username)
			{
				long userEntityId;
				try
				{
					userEntityId = userMapper.resolveUser(username,	realm.getName());
					Map<String, GroupMembership> grps = 
							identitiesMan.getGroups(new EntityParam(userEntityId));
					String return_format = configuration.getValue(
							LdapServerProperties.GROUP_OF_NAMES_RETURN_FORMAT);
					if (null == return_format)
					{
						throw new LdapOtherException(
								"Configuration error in GROUP_OF_NAMES_RETURN_FORMAT");
					}

					for (Map.Entry<String, GroupMembership> agroup : grps
							.entrySet())
					{
						Entry e = new DefaultEntry(schemaManager);
						e.setDn(String.format(return_format,
								agroup.getKey()));
						entries.add(e);
					}
				} catch (EngineException e)
				{
					throw new LdapOtherException("Error when searching", e);
				}
			}
		}
		return entries;
	}
	

	/**
	 * Get Unity user from LDAP search context.
	 */
	private Entry getUnityUserEntry(SearchOperationContext searchContext, String username)
			throws LdapException
	{
		Entry entry = new DefaultEntry(schemaManager);
		try
		{
			long userEntityId = userMapper.resolveUser(username, realm.getName());
			Entity userEntity = identitiesMan.getEntity(new EntityParam(userEntityId));

			// get attributes if any
			Collection<AttributeExt> attrs = attributesMan
					.getAttributes(new EntityParam(userEntityId), null, null);

			Set<AttributeTypeOptions> attributes = new HashSet<AttributeTypeOptions>();
			if (null != searchContext.getReturningAttributes())
			{
				attributes.addAll(searchContext.getReturningAttributes());
			} else
			{
				String default_attributes = configuration.getValue(
						LdapServerProperties.RETURNED_USER_ATTRIBUTES);
				for (String at : default_attributes.split(","))
				{
					attributes.add(new AttributeTypeOptions(schemaManager
							.lookupAttributeTypeRegistry(at.trim())));
				}
			}

			// iterate through requested attributes and try to match
			// them with
			// identity attributes
			String[] aliases = configuration
					.getValue(LdapServerProperties.USER_NAME_ALIASES)
					.split(",");
			for (String alias : aliases)
			{
				for (AttributeTypeOptions ao : attributes)
				{
					String aName = ao.getAttributeType().getName();
					if (aName.equals(alias))
					{
						String requestDn = String.format("%s=%s", alias,
								username);
						// FIXME: is this what we want
						// in all cases?
						if (null != searchContext.getDn())
						{
							requestDn += "," + searchContext.getDn()
									.toString();
						}
						entry.setDn(requestDn);
					} else
					{
						attributeUtils.addAttribute(aName, userEntity,
								username, attrs, entry);

					}
				}
			}

			// the purpose of a search can be to get DN (from another attribute)
			// - even if no attributes are in getReturningAttributes()
			if (null == entry.getDn() || entry.getDn().isEmpty())
			{
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

		return null;// emptyResult(searchContext);
	}
}
