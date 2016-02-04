/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;
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
	private IdentitiesManagement idManagement;

	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attrManagement;
	
	public void execute(ProcessingRule rule)
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
	
	private void handleMember(ProcessingRule rule, GroupMembership membership)
	{
		try
		{
			EntityParam entityP = new EntityParam(membership.getEntityId());
			Entity entity = idManagement.getEntity(entityP);
			Map<String, GroupMembership> groups = idManagement.getGroups(entityP);
			Collection<AttributeExt<?>> allAttributes = 
					attrManagement.getAllAttributes(entityP, false, "/", null, false);
			Serializable compiledCondition = rule.getCompiledCondition();
			Object context = getContext(entity, groups.keySet(), allAttributes);

			if (TranslationCondition.evaluateCondition(compiledCondition, context, log))
			{
				if (log.isDebugEnabled())
					log.debug("Executing action on entity with id " + entity.getId());
				rule.getAction().invoke(entity);
			} else
			{
				if (log.isDebugEnabled())
					log.debug("Skipping entity with id " + entity.getId() + 
							" not matching the condition");
			}
		} catch (Exception e)
		{
			log.error("Processing entity action failed", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getContext(Entity entity, Set<String> groups, 
			Collection<AttributeExt<?>> attributes)
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
		Map<String, List<Object>> attrs = new HashMap<>();
		
		for (AttributeExt<?> attribute: attributes)
		{
			Object v = attribute.getValues().isEmpty() ? "" : attribute.getValues().get(0);
			attr.put(attribute.getName(), v);
			attrs.put(attribute.getName(), (List<Object>) attribute.getValues());
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
