/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Responsible for execution of rules. It is an internal engine, it does not 
 * touch scheduling, persistence etc.
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingExecutor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, BulkProcessingExecutor.class);
	
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groupsManagement;

	@Autowired
	@Qualifier("insecure")
	private IdentitiesManagement idManagement;
	
	public void execute(ProcessingRule rule)
	{
		NDC.push("[EntityAction " + rule.getAction().getActionDescription().getName() + "]");
		try
		{
			GroupContents contents = groupsManagement.getContents("/", GroupContents.MEMBERS);
			List<GroupMembership> members = contents.getMembers();
			for (GroupMembership membership: members)
				handleMember(rule, membership);
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
			Entity entity = idManagement.getEntity(new EntityParam(membership.getEntityId()));
			Serializable compiledCondition = rule.getCompiledCondition();
			Object context = getContext();

			if (TranslationCondition.evaluateCondition(compiledCondition, context, log))
				rule.getAction().invoke(entity);
		} catch (Exception e)
		{
			log.error("Processing entity action failed", e);
		}
	}
	
	private Map<String, Object> getContext()
	{
		//TODO
		throw new RuntimeException("not implemented yet");
	}
}
