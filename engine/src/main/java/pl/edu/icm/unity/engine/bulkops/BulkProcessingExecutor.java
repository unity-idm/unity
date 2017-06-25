/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Responsible for execution of rules. It is an internal engine, it does not 
 * touch scheduling, persistence etc.
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingExecutor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, BulkProcessingExecutor.class);
	
	private static final Set<String> SENSITIVE = Sets.newHashSet("hash", "cred", "pass");
	
	public enum ContextKey
	{
		idsByType,
		idsByTypeObj,
		attrs,
		attr,
		groups,
		status,
		credReq
	}
	
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groupsManagement;

	@Autowired
	@Qualifier("insecure")
	private EntityManagement idManagement;

	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attrManagement;
	
	public void execute(TranslationRuleInstance<EntityAction> rule)
	{
		NDC.push("[EntityAction " + rule.getAction().getName() + "]");
		try
		{
			log.info("Starting bulk entities processing rule");
			Instant start = Instant.now();
			GroupContents contents = groupsManagement.getContents("/", GroupContents.MEMBERS);
			List<GroupMembership> members = contents.getMembers();
			for (GroupMembership membership: members)
				handleMember(rule, membership);
			Instant end = Instant.now();
			
			log.info("Finished bulk entities processing, took " + 
					start.until(end, ChronoUnit.MILLIS) + "ms");
		} catch (Exception e)
		{
			log.error("Processing bulk entity actions failed", e);
		} finally
		{
			NDC.pop();
		}
	}
	
	private void handleMember(TranslationRuleInstance<EntityAction> rule, GroupMembership membership)
	{
		try
		{
			EntityParam entityP = new EntityParam(membership.getEntityId());
			Entity entity = idManagement.getEntity(entityP);
			Map<String, GroupMembership> groups = idManagement.getGroups(entityP);
			Collection<AttributeExt> allAttributes = 
					attrManagement.getAllAttributes(entityP, false, "/", null, false);
			Map<String, Object> context = getContext(entity, groups.keySet(), allAttributes);

			if (log.isDebugEnabled())
				log.debug("Entity processing context for {}:\n{}", 
						entity.getEntityInformation().getId(),
						ctx2ReadableString(context, ""));
			
			if (rule.getConditionInstance().evaluate(context, log))
			{
				if (log.isDebugEnabled())
					log.debug("Executing action on entity with id " + 
							entity.getEntityInformation().getId());
				rule.getActionInstance().invoke(entity);
			} else
			{
				if (log.isDebugEnabled())
					log.debug("Skipping entity with id {} not matching the condition",
							 entity.getEntityInformation().getId());
			}
		} catch (Exception e)
		{
			log.error("Processing entity action failed", e);
		}
	}
	
	private String ctx2ReadableString(Object context, String pfx)
	{
		if (!(context instanceof Map))
			return context.toString();
		Map<?, ?> map = (Map<?, ?>) context;
		StringBuilder ret = new StringBuilder(10240);
		map.forEach((k, v) -> {
			String key = k.toString();
			ret.append(pfx).append(k).append(": ");
			if (seemsSensitive(key))
				ret.append("--MASKED--").append("\n"); 
			else
				ret.append(ctx2ReadableString(v, pfx+"  ")).append("\n");
		});
		
		
		return ret.toString();
	}
	
	private boolean seemsSensitive(String key)
	{
		for (String checked: SENSITIVE)
			if (key.contains(checked))
				return true;
		return false;
	}
	
	private Map<String, Object> getContext(Entity entity, Set<String> groups, 
			Collection<AttributeExt> attributes)
	{
		Map<String, Object> ctx = new HashMap<>();
		
		Map<String, List<String>> idsByType = new HashMap<>();
		Map<String, List<Object>> idsByTypeObj = new HashMap<>();
		for (Identity identity: entity.getIdentities())
		{
			List<String> vals = idsByType.get(identity.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(identity.getTypeId(), vals);
			}
			vals.add(identity.getValue());

			List<Object> valsObj = idsByTypeObj.get(identity.getTypeId());
			if (valsObj == null)
			{
				valsObj = new ArrayList<>();
				idsByTypeObj.put(identity.getTypeId(), valsObj);
			}
			valsObj.add(identity.getValue());
		}

		Map<String, Object> attr = new HashMap<>();
		Map<String, List<String>> attrs = new HashMap<>();
		
		for (AttributeExt attribute: attributes)
		{
			Object v = attribute.getValues().isEmpty() ? "" : attribute.getValues().get(0);
			attr.put(attribute.getName(), v);
			attrs.put(attribute.getName(), attribute.getValues());
		}
		ctx.put(ContextKey.attr.name(), attr);
		ctx.put(ContextKey.attrs.name(), attrs);
		
		ctx.put(ContextKey.groups.name(), groups);
		ctx.put(ContextKey.idsByType.name(), idsByType);
		ctx.put(ContextKey.idsByTypeObj.name(), idsByTypeObj);
		ctx.put(ContextKey.status.name(), entity.getEntityInformation().getState().toString());
		ctx.put(ContextKey.credReq.name(), entity.getCredentialInfo().getCredentialRequirementId());
		
		return ctx;
	}
}
