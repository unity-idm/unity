/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Loads entities from a given group, and resolves their attributes. Operation is done
 * in async way for larger amounts of entities. Client has to provide consumer for collected data.  
 *  
 * @author K. Benedyczak
 */
@PrototypeComponent
class EntitiesLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EntitiesLoader.class);
	private static final int LOAD_IN_SYNC = 40;
	private static final int CHUNK = 500;
	
	private final EntityManagement identitiesMan;
	private final GroupsManagement groupsMan;
	private final AttributesManagement attrMan;
	
	
/*	
	private InvocationContext ctx;
	private Future<?> controller;
	private int offset;
*/	
	@Autowired
	EntitiesLoader(EntityManagement identitiesMan, GroupsManagement groupsMan,
			AttributesManagement attrMan)
	{
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attrMan = attrMan;
	}

	void reload(Set<IdentityEntry> selected, String group, boolean includeTargeted,
			EntitiesConsumer consumer) throws EngineException
	{
		
		List<Long> members = getMembers(group);
		int toSyncLoad = members.size() > LOAD_IN_SYNC ? LOAD_IN_SYNC : members.size();
		resolveEntitiesAndUpdateTableSync(members, toSyncLoad, selected,
				group, includeTargeted, consumer);
	}
		
		//TODO
		/*
		if (!entitiesLoader.isDone())
		{
			entitiesLoader.cancel(false);
			try
			{
				entitiesLoader.get();
			} catch (CancellationException e)
			{
				//ok, expected
			}catch (Exception e)
			{
				log.warn("Background identities loader threw an exception", e);
			}
		}
		
		int toSyncLoad = entities.size() > LOAD_IN_SYNC ? LOAD_IN_SYNC : entities.size();
		resolveEntitiesAndUpdateTableSync(entities, toSyncLoad, selected);
		
		if (entities.size() > LOAD_IN_SYNC)
		{
			UI.getCurrent().setPollInterval(500);
			removeAllFiltersFromTable();
			loadingProgress.removeStyleName(Styles.hidden.toString());
			loadingProgress.setValue(0f);
			EntitiesLoader loaderTask = new EntitiesLoader(entities, selected, toSyncLoad);
			entitiesLoader = new FutureTask<Object>(loaderTask, null);
			loaderTask.setController(entitiesLoader);
			executor.getService().execute(entitiesLoader);
		}
	}
	
	private void load(List<Long> entities, Object selected, int offset)
	{
		this.entities = entities;
		this.selected = selected;
		this.offset = offset;
		this.ctx = InvocationContext.getCurrent();
	}
	
	private void run()
	{
		try
		{
			InvocationContext.setCurrent(ctx);
			resolveEntitiesAndUpdateTable(entities);
		} catch (EngineException e)
		{
			log.error("Problem retrieving group contents of " + group, e);
		}
	}
*/
	private void resolveEntitiesAndUpdateTableSync(List<Long> entities, int amount, 
			Set<IdentityEntry> selected, String group, boolean includeTargeted,
			EntitiesConsumer consumer) throws EngineException
	{
		for (int i=0; i<amount; i++)
		{
			long entity = entities.get(i);
			try
			{
				ResolvedEntity resolvedEntity = resolveEntity(entity, group, includeTargeted);
				consumer.consume(Collections.singletonList(resolvedEntity), 
						selected,
						(float)entities.size() / (i+1));
			} catch (AuthorizationException e)
			{
				log.debug("Entity " + entity + " information can not be loaded, "
						+ "won't be in the identities table", e);
			}
		}
	}
/*	
	private void resolveEntitiesAndUpdateTable(List<Long> entities) throws EngineException
	{
		for (int i=offset; i<entities.size(); i+=CHUNK)
		{
			List<IdentitiesAndAttributes> resolved = resolveEntities(entities, i, CHUNK);
			if (controller.isCancelled())
				return;
			updateTable(resolved, (float)(i+CHUNK)/entities.size());
		}
		ui.accessSynchronously(() -> {
			if (controller.isCancelled())
				return;					
			addAllFilters();
			loadingProgress.addStyleName(Styles.hidden.toString());
			if (groupByEntity != groupByEntityLocal)
				reloadTableContentsFromData();
			ui.setPollInterval(-1);
		}); 
	}
	
	private List<IdentitiesAndAttributes> resolveEntities(List<Long> entities, 
			int first, int amount) throws EngineException
	{
		int limit = first + amount > entities.size() ? entities.size() : amount + first;
		List<IdentitiesAndAttributes> toAdd = new LinkedList<>();
		for (int i=first; i<limit; i++)
		{
			long entity = entities.get(i);
			if (controller.isCancelled())
				break;
			try
			{
				IdentitiesAndAttributes resolvedEntity = resolveEntity(entity);
				toAdd.add(resolvedEntity);
				if (controller.isCancelled())
					break;
			} catch (AuthorizationException e)
			{
				log.debug("Entity " + entity + " information can not be loaded, "
						+ "won't be in the identities table", e);
			}
		}
		return toAdd;
	}
*/	
	private ResolvedEntity resolveEntity(long entity, String group, boolean showTargeted) throws EngineException
	{		
		Entity resolvedEntity = showTargeted ? identitiesMan
				.getEntityNoContext(new EntityParam(entity), group) : identitiesMan
				.getEntity(new EntityParam(entity), null, false, group);
		Collection<AttributeExt> rawCurAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, group, null, true);
		Collection<AttributeExt> rawRootAttrs = new ArrayList<AttributeExt>();
		try
		{
			rawRootAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, "/", null, true);
		} catch (AuthorizationException e)
		{
			log.debug("can not resolve attributes in '/' for entity, " + entity + 
					" only group's attributes will be available: " + e.toString());
		}
		Map<String, Attribute> rootAttrs = new HashMap<String, Attribute>(rawRootAttrs.size());
		Map<String, Attribute> curAttrs = new HashMap<String, Attribute>(rawRootAttrs.size());
		for (Attribute a: rawRootAttrs)
			rootAttrs.put(a.getName(), a);
		for (Attribute a: rawCurAttrs)
			curAttrs.put(a.getName(), a);
		return new ResolvedEntity(resolvedEntity, 
				resolvedEntity.getIdentities(),	rootAttrs, curAttrs);
	}
	
	private List<Long> getMembers(String group) throws EngineException
	{
		GroupContents contents = groupsMan.getContents(group, GroupContents.MEMBERS);
		return contents.getMembers().stream().
				map(GroupMembership::getEntityId).
				collect(Collectors.toList());
	}
	
	@FunctionalInterface
	interface EntitiesConsumer
	{
		void consume(List<ResolvedEntity> toAdd, Set<IdentityEntry> selected,
				float progress);
	}
}
