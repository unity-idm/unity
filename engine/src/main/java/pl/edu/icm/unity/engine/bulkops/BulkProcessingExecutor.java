/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;

/**
 * Responsible for execution of rules. It is an internal engine, it does not 
 * touch scheduling, persistence etc.
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingExecutor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, BulkProcessingExecutor.class);
	
	@Autowired
	@Qualifier("insecure")
	private EntityManagement idManagement;
	
	@Autowired
	@Qualifier("insecure")
	private BulkGroupQueryService bulkService;
	
	public void execute(TranslationRuleInstance<EntityAction> rule)
	{
		NDC.push("[EntityAction " + rule.getAction().getName() + "]");
		try
		{
			log.info("Starting bulk entities processing rule");
			Instant start = Instant.now();
			GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
			Map<Long, EntityInGroupData> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);
		
			for (EntityInGroupData membership: membershipInfo.values())
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
	
	private void handleMember(TranslationRuleInstance<EntityAction> rule, EntityInGroupData membership)
	{
		try
		{
			Entity entity = idManagement.getEntity(new EntityParam(membership.entity.getId()));
			Map<String, Object> context = EntityMVELContextBuilder.getContext(membership);

			if (log.isDebugEnabled())
				log.debug("Entity processing context for {}:\n{}", 
						entity.getEntityInformation().getId(),
						EntityMVELContextBuilder.ctx2ReadableString(context, ""));
			
			if (rule.getConditionInstance().evaluate(context, log))
			{
				log.info("Executing action on entity with id {}", entity.getEntityInformation().getId());
				rule.getActionInstance().invoke(entity);
			} else
			{
				log.debug("Skipping entity with id {} not matching the condition",
							 entity.getEntityInformation().getId());
			}
		} catch (Exception e)
		{
			log.error("Processing entity action failed", e);
		}
	}
	
}
