/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.credentials.CredentialChangeDialog;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.identities.CredentialRequirementDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.Action;
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
	public static final String ATTR_COL_PREFIX = "a::";
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private AttributesManagement attrMan;
	private IdentityEditorRegistry identityEditorReg;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private CredentialEditorRegistry credEditorsRegistry;
	private EventsBus bus;
	private String group;
	private Map<Entity, IdentitiesAndAttributes> data = new HashMap<Entity, IdentitiesTable.IdentitiesAndAttributes>();
	private boolean groupByEntity;
	private Entity selected;
	private List<Filter> containerFilters;

	@Autowired
	public IdentitiesTable(IdentitiesManagement identitiesMan, GroupsManagement groupsMan, 
			AuthenticationManagement authnMan, AttributesManagement attrMan,
			IdentityEditorRegistry identityEditorReg, CredentialEditorRegistry credEditorsRegistry,
			AttributeHandlerRegistry attrHandlerReg, UnityMessageSource msg)
	{
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.identityEditorReg = identityEditorReg;
		this.authnMan = authnMan;
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerReg;
		this.attrMan = attrMan;
		this.bus = WebSession.getCurrent().getEventBus();
		this.containerFilters = new ArrayList<Container.Filter>();
		this.credEditorsRegistry = credEditorsRegistry;
		
		addContainerProperty("entity", String.class, "");
		addContainerProperty("type", String.class, "");
		addContainerProperty("identity", String.class, "");
		addContainerProperty("enabled", String.class, "");
		addContainerProperty("local", String.class, "");
		addContainerProperty("localAuthnState", String.class, "");
		addContainerProperty("credReq", String.class, "");
		setColumnHeader("entity", msg.getMessage("Identities.entity"));
		setColumnHeader("type", msg.getMessage("Identities.type"));
		setColumnHeader("identity", msg.getMessage("Identities.identity"));
		setColumnHeader("enabled", msg.getMessage("Identities.enabled"));
		setColumnHeader("local", msg.getMessage("Identities.local"));
		setColumnHeader("localAuthnState", msg.getMessage("Identities.localAuthnState"));
		setColumnHeader("credReq", msg.getMessage("Identities.credReq"));
		
		setSelectable(true);
		setMultiSelect(false);
		setColumnCollapsingAllowed(true);
		setColumnCollapsible("entity", false);
		setColumnCollapsed("local", true);
		setColumnCollapsed("localAuthnState", true);
		setColumnCollapsed("credReq", true);
		
		setColumnWidth("entity", 60);
		setColumnWidth("type", 100);
		setColumnWidth("enabled", 100);
		setColumnWidth("local", 100);
		setColumnWidth("localAuthnState", 140);
		setColumnWidth("credReq", 180);
		
		addActionHandler(new RefreshHandler());
		addActionHandler(new RemoveFromGroupHandler());
		addActionHandler(new AddEntityActionHandler());
		addActionHandler(new AddIdentityActionHandler());
		addActionHandler(new DeleteEntityHandler());
		addActionHandler(new DeleteIdentityHandler());
		addActionHandler(new DisableIdentityHandler());
		addActionHandler(new EnableIdentityHandler());
		addActionHandler(new ChangeCredentialHandler());
		addActionHandler(new ChangeCredentialRequirementHandler());
		setDragMode(TableDragMode.ROW);
		
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
	
	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(group));
	}
	
	public void setInput(String group, List<String> entities) throws EngineException
	{
		this.group = group;
		data.clear();
		for (String entity: entities)
			resolveEntity(entity); 
		updateContents();
	}
	
	public void addAttributeColumn(String attribute)
	{
		String key = ATTR_COL_PREFIX+attribute;
		addContainerProperty(key, String.class, "");
		setColumnHeader(key, attribute);
		refresh();
	}

	public void removeAttributeColumn(String... attributes)
	{
		for (String attribute: attributes)
		{
			String key = ATTR_COL_PREFIX+attribute;
			removeContainerProperty(key);
		}
		refresh();
	}
	
	public Set<String> getAttributeColumns()
	{
		Collection<?> props = getContainerPropertyIds();
		Set<String> ret = new HashSet<String>();
		for (Object prop: props)
		{
			if (!(prop instanceof String))
				continue;
			String property = (String) prop;
			if (property.startsWith(ATTR_COL_PREFIX))
				ret.add(property.substring(ATTR_COL_PREFIX.length()));
		}
		return ret;
	}
	
	public void addFilter(Filter filter)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.addContainerFilter(filter);
		containerFilters.add(filter);
	}
	
	public void removeFilter(Filter filter)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.removeContainerFilter(filter);
		containerFilters.remove(filter);
		refresh();
	}
	
	private void updateContents()
	{
		removeAllItems();
		if (groupByEntity)
			setGroupedContents();
		else
			setFlatContents();
	}
	
	/*
	 * We use a hack here: filters are temporarly removed and readded after all data is set. 
	 * This is because Vaadin (tested at 7.0.4) seems to ignore parent elements when not matching filter
	 * during addition, but properly shows them afterwards.
	 */
	private void setGroupedContents()
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.removeAllContainerFilters();
		for (Map.Entry<Entity, IdentitiesAndAttributes> entry: data.entrySet())
		{
			IdentitiesAndAttributes resolved = entry.getValue();
			Entity entity = entry.getKey();
			Object parentKey = addRow(null, entity, resolved.getAttributes());
			for (Identity id: resolved.getIdentities())
			{
				Object key = addRow(id, entity, resolved.attributes);
				setParent(key, parentKey);
				setChildrenAllowed(key, false);
			}
		}
		for (Filter filter: containerFilters)
			filterable.addContainerFilter(filter);
	}

	private void setFlatContents()
	{
		for (Map.Entry<Entity, IdentitiesAndAttributes> entry: data.entrySet())
		{
			IdentitiesAndAttributes resolved = entry.getValue();
			for (Identity id: resolved.getIdentities())
			{
				Object itemId = addRow(id, entry.getKey(), resolved.attributes);
				setChildrenAllowed(itemId, false);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Object addRow(Identity id, Entity ent, Map<String, Attribute<?>> attributes)
	{
		Object itemId = id == null ? ent : new IdentityWithEntity(id, ent);
		setColumnWidth("entity", 60);
		setColumnWidth("type", 100);
		setColumnWidth("enabled", 100);
		setColumnWidth("local", 100);
		setColumnWidth("localAuthnState", 140);
		setColumnWidth("credReq", 180);
		
		Item newItem = addItem(itemId);
		newItem.getItemProperty("entity").setValue(ent.getId());
		newItem.getItemProperty("localAuthnState").setValue(ent.getCredentialInfo().getAuthenticationState().toString());
		newItem.getItemProperty("credReq").setValue(ent.getCredentialInfo().getCredentialRequirementId());
		if (id != null)
		{
			newItem.getItemProperty("type").setValue(id.getTypeId());
			newItem.getItemProperty("identity").setValue(id.toPrettyStringNoPrefix());
			newItem.getItemProperty("enabled").setValue(new Boolean(id.isEnabled()).toString());
			newItem.getItemProperty("local").setValue(new Boolean(id.isLocal()).toString());
		} else
		{
			newItem.getItemProperty("type").setValue("");
			newItem.getItemProperty("identity").setValue("");
			newItem.getItemProperty("enabled").setValue("");
			newItem.getItemProperty("local").setValue("");
		}
		
		Collection<?> propertyIds = newItem.getItemPropertyIds();
		for (Object propertyId: propertyIds)
		{
			if (!(propertyId instanceof String))
				continue;
			String propId = (String) propertyId;
			if (!propId.startsWith("a::"))
				continue;
			String attributeName = propId.substring(3);
			Attribute<?> attribute = attributes.get(attributeName);
			String val;
			if (attribute == null)
				val = msg.getMessage("Identities.attributeUndefined");
			else
				val = attrHandlerRegistry.getSimplifiedAttributeValuesRepresentation(attribute,
						AttributeHandlerRegistry.DEFAULT_MAX_LEN);
				
			newItem.getItemProperty(propId).setValue(val);
		}
		return itemId;
	}
	
	private void resolveEntity(String entity) throws EngineException
	{
		Entity resolvedEntity = identitiesMan.getEntity(new EntityParam(entity));
		Collection<AttributeExt<?>> rawAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, "/", null, true);
		Map<String, Attribute<?>> attrs = new HashMap<String, Attribute<?>>(rawAttrs.size());
		for (Attribute<?> a: rawAttrs)
			attrs.put(a.getName(), a);
		IdentitiesAndAttributes resolved = new IdentitiesAndAttributes(resolvedEntity.getIdentities(),
				attrs);
		data.put(resolvedEntity, resolved);
	}
	
	private void removeEntity(String entityId)
	{
		AuthenticatedEntity entity = InvocationContext.getCurrent().getAuthenticatedEntity();
		
		if (Long.valueOf(entityId) == entity.getEntityId())
		{
			ErrorPopup.showError(msg.getMessage("error"), msg.getMessage("Identities.notRemovingLoggedUser"));
			return;
		}
		try
		{
			identitiesMan.removeEntity(new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Identities.removeEntityError"), e);
		}
	}

	private void removeIdentity(Identity identity)
	{
		try
		{
			identitiesMan.removeIdentity(identity);
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Identities.removeIdentityError"), e);
		}
	}

	private void disableEnableIdentity(Identity identity, boolean how)
	{
		try
		{
			identitiesMan.setIdentityStatus(identity, how);
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Identities.disableIdentityError"), e);
		}
	}
	
	private void removeFromGroup(String entityId)
	{
		try
		{
			groupsMan.removeMember(group, new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("Identities.removeFromGroupError"), e);
		}
	}
	
	private class RemoveFromGroupHandler extends SingleActionHandler
	{
		public RemoveFromGroupHandler()
		{
			super(msg.getMessage("Identities.removeFromGroupAction"), 
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final String entityId = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntity().getId() : target.toString();
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmRemoveFromGroup", entityId, group),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeFromGroup(entityId);
				}
			}).show();
		}
	}

	private class AddEntityActionHandler extends SingleActionHandler
	{
		public AddEntityActionHandler()
		{
			super(msg.getMessage("Identities.addEntityAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			new EntityCreationDialog(msg, group, identitiesMan, groupsMan, 
					authnMan, identityEditorReg, new EntityCreationDialog.Callback()
					{
						@Override
						public void onCreated()
						{
							bus.fireEvent(new GroupChangedEvent(group));
						}
					}).show();
		}
	}

	private class AddIdentityActionHandler extends SingleActionHandler
	{
		public AddIdentityActionHandler()
		{
			super(msg.getMessage("Identities.addIdentityAction"), Images.add.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final String entityId = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntity().getId() : target.toString();
			new IdentityCreationDialog(msg, entityId, identitiesMan,  
					identityEditorReg, new IdentityCreationDialog.Callback()
					{
						@Override
						public void onCreated()
						{
							bus.fireEvent(new GroupChangedEvent(group));
						}
					}).show();
		}
	}

	private class DeleteEntityHandler extends SingleActionHandler
	{
		public DeleteEntityHandler()
		{
			super(msg.getMessage("Identities.deleteEntityAction"), 
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final String entityId = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntity().getId() : target.toString();
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmEntityDelete", entityId),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeEntity(entityId);
				}
			}).show();
		}
	}

	
	private class DeleteIdentityHandler extends SingleActionHandler
	{
		public DeleteIdentityHandler()
		{
			super(msg.getMessage("Identities.deleteIdentityAction"), 
					Images.delete.getResource());
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target != null && !(target instanceof IdentityWithEntity))
				return EMPTY;
			return super.getActions(target, sender);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final IdentityWithEntity node = (IdentityWithEntity) target;
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmIdentityDelete", node.identity),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeIdentity(node.identity);
				}
			}).show();
		}
	}

	private class DisableIdentityHandler extends SingleActionHandler
	{
		public DisableIdentityHandler()
		{
			super(msg.getMessage("Identities.disableIdentityAction"), 
					Images.unchecked.getResource());
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null)
				return EMPTY;
			if (!(target instanceof IdentityWithEntity))
				return EMPTY;
			if (!((IdentityWithEntity)target).identity.isEnabled())
				return EMPTY;
			return super.getActions(target, sender);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final IdentityWithEntity node = (IdentityWithEntity) target;
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmIdentityDisable", node.identity),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					disableEnableIdentity(node.identity, false);
				}
			}).show();
		}
	}

	private class EnableIdentityHandler extends SingleActionHandler
	{
		public EnableIdentityHandler()
		{
			super(msg.getMessage("Identities.enableIdentityAction"), 
					Images.checked.getResource());
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null)
				return EMPTY;
			if (!(target instanceof IdentityWithEntity))
				return EMPTY;
			if (((IdentityWithEntity)target).identity.isEnabled())
				return EMPTY;
			return super.getActions(target, sender);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final IdentityWithEntity node = (IdentityWithEntity) target;
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmIdentityEnable", node.identity),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					disableEnableIdentity(node.identity, true);
				}
			}).show();
		}
	}

	private class ChangeCredentialRequirementHandler extends SingleActionHandler
	{
		public ChangeCredentialRequirementHandler()
		{
			super(msg.getMessage("Identities.changeCredentialRequirementAction"), 
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final String entityId = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntity().getId() : target.toString();
			new CredentialRequirementDialog(msg, entityId, identitiesMan, authnMan, new Callback()
			{
				@Override
				public void onChanged()
				{
					refresh();
				}
			}).show();
		}
	}

	private class ChangeCredentialHandler extends SingleActionHandler
	{
		public ChangeCredentialHandler()
		{
			super(msg.getMessage("Identities.changeCredentialAction"), 
					Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			Entity entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntity() : (Entity)target;
			new CredentialChangeDialog(msg, entity, authnMan, identitiesMan,
					credEditorsRegistry, new CredentialChangeDialog.Callback()
					{
						@Override
						public void onClose(boolean changed)
						{
							if (changed)
								refresh();
						}
					}).show();
		}
	}

	private class RefreshHandler extends SingleActionHandler
	{
		public RefreshHandler()
		{
			super(msg.getMessage("Identities.refresh"), 
					Images.refresh.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			refresh();
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

		public IdentitiesAndAttributes(Identity[] identities, Map<String, Attribute<?>> attributes)
		{
			this.identities = identities;
			this.attributes = attributes;
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
	public static class IdentityWithEntity
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




