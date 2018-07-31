/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Stopwatch;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
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
	private static final int LOAD_IN_SYNC = 100;
	private static final int CHUNK = 100;
	private static final int UI_REFRESH = 250;
	
	private final EntityManagement identitiesMan;
	private final GroupsManagement groupsMan;
	private final AttributesManagement attrMan;
	private final ExecutorsService executor;
	private FutureTask<Object> loaderFuture;

	@Autowired
	EntitiesLoader(EntityManagement identitiesMan, GroupsManagement groupsMan,
			AttributesManagement attrMan, ExecutorsService executor)
	{
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attrMan = attrMan;
		this.executor = executor;
	}

	void reload(Set<IdentityEntry> selected, String group, boolean includeTargeted,
			EntitiesConsumer consumer) throws EngineException
	{
		cancelPreviousTask();
		List<Long> members = getMembers(group);
		UI ui = UI.getCurrent();
		int toSyncLoad = (members.size() > LOAD_IN_SYNC) && (ui != null) ? 
				LOAD_IN_SYNC : members.size();
		resolveEntitiesAndUpdateTableSync(members, toSyncLoad, selected,
				group, includeTargeted, consumer);
		
		int alreadyLoaded = toSyncLoad;
		if (members.size() > alreadyLoaded)
		{
			ui.setPollInterval(UI_REFRESH);
			AsyncLoader asyncLoader = new AsyncLoader();
			loaderFuture = new FutureTask<>(() -> 
					asyncLoader.resolveEntitiesAndUpdateTableAsync(members, group, 
							includeTargeted, alreadyLoaded, 
							selected, consumer), null);
			asyncLoader.controller = loaderFuture;
			executor.getService().execute(loaderFuture);
		}
	}
	
	private void cancelPreviousTask()
	{
		if (loaderFuture != null && !loaderFuture.isDone())
		{
			loaderFuture.cancel(false);
			try
			{
				loaderFuture.get();
			} catch (CancellationException e)
			{
				//ok, expected
			}catch (Exception e)
			{
				log.warn("Background identities loader threw an exception", e);
			}
		}
	}

	private void resolveEntitiesAndUpdateTableSync(List<Long> entities, int amount, 
			Set<IdentityEntry> selected, String group, boolean includeTargeted,
			EntitiesConsumer consumer) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
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
		watch.stop();
		log.debug("Resolved {} users in {}, {} users/s", amount, watch.toString(), 
				(1000.0*amount/watch.elapsed(TimeUnit.MILLISECONDS)));
	}


	private class AsyncLoader
	{
		private Future<?> controller;
		private InvocationContext ctx;
		private UI ui;

		public AsyncLoader()
		{
			this.ctx = InvocationContext.getCurrent();
			this.ui = UI.getCurrent();
		}

		private void resolveEntitiesAndUpdateTableAsync(List<Long> entities, String group, 
				boolean showTargeted, int alreadyLoaded,
				Set<IdentityEntry> selected, EntitiesConsumer consumer)
		{
			InvocationContext.setCurrent(ctx);
			try
			{
				for (int i=alreadyLoaded; i<entities.size(); i+=CHUNK)
				{
					List<ResolvedEntity> resolved = resolveEntitiesAsync(entities, group, 
							showTargeted, i, CHUNK);
					if (controller.isCancelled())
						return;
					int finalI = i;
					ui.accessSynchronously(() ->
						consumer.consume(resolved, selected, 
							(float)(finalI+CHUNK)/entities.size())
					);
				}
			} finally
			{
				ui.accessSynchronously(() ->
					ui.setPollInterval(-1)
				);
				InvocationContext.setCurrent(null);
			}
		}

		private List<ResolvedEntity> resolveEntitiesAsync(List<Long> entities, 
			String group, boolean showTargeted, int first, int amount)
		{
			int limit = first + amount > entities.size() ? entities.size() : amount + first;
			List<ResolvedEntity> toAdd = new LinkedList<>();
			for (int i=first; i<limit; i++)
			{
				long entity = entities.get(i);
				if (controller.isCancelled())
					break;
				try
				{
					ResolvedEntity resolvedEntity = resolveEntity(entity, group, showTargeted);
					toAdd.add(resolvedEntity);
				} catch (AuthorizationException e)
				{
					log.debug("Entity " + entity + " information can not be loaded, "
							+ "won't be in the identities table", e);
				} catch (EngineException e)
				{
					log.warn("Entity " + entity + " information can not be loaded, "
							+ "won't be in the identities table", e);
				}
			}
			return toAdd;
		}
	}
	
	
	private ResolvedEntity resolveEntity(long entity, String group, boolean showTargeted) throws EngineException
	{		
		Entity resolvedEntity = showTargeted ? 
				identitiesMan.getEntityNoContext(new EntityParam(entity), group) : 
				identitiesMan.getEntity(new EntityParam(entity), null, false, group);
		Collection<AttributeExt> rawCurAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, group, null, true);
		Collection<AttributeExt> rawRootAttrs = new ArrayList<>();
		try
		{
			rawRootAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, "/", null, true);
		} catch (AuthorizationException e)
		{
			log.debug("can not resolve attributes in '/' for entity, " + entity + 
					" only group's attributes will be available: " + e.toString());
		}
		Map<String, Attribute> rootAttrs = new HashMap<>(rawRootAttrs.size());
		Map<String, Attribute> curAttrs = new HashMap<>(rawRootAttrs.size());
		for (Attribute a: rawRootAttrs)
			rootAttrs.put(a.getName(), a);
		for (Attribute a: rawCurAttrs)
			curAttrs.put(a.getName(), a);
		return new ResolvedEntity(resolvedEntity, resolvedEntity.getIdentities(), 
				rootAttrs, curAttrs);
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
