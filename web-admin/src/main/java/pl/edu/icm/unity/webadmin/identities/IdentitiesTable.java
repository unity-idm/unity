/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.data.Property;
import com.vaadin.ui.TreeTable;

/**
 * Displays a tree table with identities. Can present contents in two modes: 
 *  - flat, where each identity is a fully separate table row
 *  - grouped by entity, where each entity has all its entities as children
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentitiesTable extends TreeTable
{
	private IdentitiesManagement identitiesMan;
	private UnityMessageSource msg;
	private String group;
	private Map<Entity, IdentitiesAndAttributes> data = new HashMap<Entity, IdentitiesTable.IdentitiesAndAttributes>();
	private boolean groupByEntity;
	private Entity selected;

	@Autowired
	public IdentitiesTable(IdentitiesManagement identitiesMan, UnityMessageSource msg)
	{
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		addContainerProperty(msg.getMessage("Identities.entity"), String.class, "");
		addContainerProperty(msg.getMessage("Identities.type"), String.class, "");
		addContainerProperty(msg.getMessage("Identities.identity"), String.class, "");
		addContainerProperty("Some attribute", String.class, "<UNKNOWN>");
		setSelectable(true);
		setMultiSelect(false);
		final EventsBus bus = WebSession.getCurrent().getEventBus();
		setImmediate(true);
		setSizeFull();
		
		addValueChangeListener(new Property.ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				Object selected = getValue();
				if (selected == null)
				{
					if (selected == IdentitiesTable.this.selected)
						return;
					IdentitiesTable.this.selected = null;
					bus.fireEvent(new EntityChangedEvent(null, group));
				} else if (selected instanceof Entity)
				{
					if (selected.equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = (Entity)selected;
					bus.fireEvent(new EntityChangedEvent((Entity)selected, group));
				} else if (selected instanceof IdentityWithEntity)
				{
					IdentityWithEntity identity = (IdentityWithEntity) selected;
					if (identity.getEntity().equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = identity.getEntity();
					bus.fireEvent(new EntityChangedEvent(identity.getEntity(), group));
				}
			}
		});
	}

	public void setMode(boolean groupByEntity)
	{
		this.groupByEntity = groupByEntity;
		updateContents();
	}
	
	public void setInput(String group, List<String> entities)
	{
		this.group = group;
		data.clear();
		for (String entity: entities)
			resolveEntity(entity); 
		updateContents();
	}
	
	private void updateContents()
	{
		removeAllItems();
		if (groupByEntity)
			setGroupedContents();
		else
			setFlatContents();
	}
	
	private void setGroupedContents()
	{
		for (Map.Entry<Entity, IdentitiesAndAttributes> entry: data.entrySet())
		{
			IdentitiesAndAttributes resolved = entry.getValue();
			Entity entity = entry.getKey();
			addItem(new Object[] {entity.getId(), "", "", null }, entity);
			for (Identity id: resolved.getIdentities())
			{
				IdentityWithEntity key = new IdentityWithEntity(id, entity);
				addItem(new Object[] {entity.getId(), id.getTypeId(), 
						id.toPrettyStringNoPrefix(), null }, key);
				setChildrenAllowed(key, false);
				setParent(key, entity);
			}
		}
	}

	private void setFlatContents()
	{
		for (Map.Entry<Entity, IdentitiesAndAttributes> entry: data.entrySet())
		{
			IdentitiesAndAttributes resolved = entry.getValue();
			for (Identity id: resolved.getIdentities())
			{
				IdentityWithEntity itemId = new IdentityWithEntity(id, entry.getKey());
				addItem(new Object[] {entry.getKey().getId(), id.getTypeId(), 
						id.toPrettyStringNoPrefix(), ""}, itemId);
				setChildrenAllowed(itemId, false);
			}
		}
	}
	
	private void resolveEntity(String entity)
	{
		try
		{
			Entity resolvedEntity = identitiesMan.getEntity(new EntityParam(entity));
			//TODO attributes
			IdentitiesAndAttributes resolved = new IdentitiesAndAttributes(resolvedEntity.getIdentities());
			data.put(resolvedEntity, resolved);
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Complete info about entity: its identities and relevant attributes.
	 * Used to populate table.
	 */
	private static class IdentitiesAndAttributes
	{
		private Identity[] identities;
		private Map<String, Attribute<?>> attributes;

		public IdentitiesAndAttributes(Identity[] identities)
		{
			this.identities = identities;
		}
		public Identity[] getIdentities()
		{
			return identities;
		}
		public Map<String, Attribute<?>> getAttributes()
		{
			return attributes;
		}
	}
	
	/**
	 * Identity with its Entity. Used as item id for the rows with particular identities.
	 * @author K. Benedyczak
	 */
	private static class IdentityWithEntity
	{
		private Identity identity;
		private Entity entity;
		public IdentityWithEntity(Identity identity, Entity entity)
		{
			super();
			this.identity = identity;
			this.entity = entity;
		}
		public Identity getIdentity()
		{
			return identity;
		}
		public Entity getEntity()
		{
			return entity;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((identity == null) ? 0 : identity.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IdentityWithEntity other = (IdentityWithEntity) obj;
			if (identity == null)
			{
				if (other.identity != null)
					return false;
			} else if (!identity.equals(other.identity))
				return false;
			return true;
		}

	}
}




