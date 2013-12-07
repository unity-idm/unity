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
import pl.edu.icm.unity.home.iddetails.EntityDetailsDialog;
import pl.edu.icm.unity.home.iddetails.EntityDetailsPanel;
import pl.edu.icm.unity.server.api.AttributesInternalProcessing;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.identities.CredentialRequirementDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;
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
	private AttributesInternalProcessing attrProcessor;
	private IdentityEditorRegistry identityEditorReg;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private CredentialEditorRegistry credEditorsRegistry;
	private EventsBus bus;
	private String group;
	private Map<Long, IdentitiesAndAttributes> data = new HashMap<Long, IdentitiesAndAttributes>();
	private boolean groupByEntity;
	private Entity selected;
	private List<Filter> containerFilters;
	private String entityNameAttribute = null;
	private List<SingleActionHandler> actionHandlers;
	
	@Autowired
	public IdentitiesTable(IdentitiesManagement identitiesMan, GroupsManagement groupsMan, 
			AuthenticationManagement authnMan, AttributesManagement attrMan,
			AttributesInternalProcessing attrProcessor,
			IdentityEditorRegistry identityEditorReg, CredentialEditorRegistry credEditorsRegistry,
			AttributeHandlerRegistry attrHandlerReg, UnityMessageSource msg)
	{
		this.identitiesMan = identitiesMan;
		this.attrProcessor = attrProcessor;
		this.groupsMan = groupsMan;
		this.identityEditorReg = identityEditorReg;
		this.authnMan = authnMan;
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerReg;
		this.attrMan = attrMan;
		this.bus = WebSession.getCurrent().getEventBus();
		this.containerFilters = new ArrayList<Container.Filter>();
		this.credEditorsRegistry = credEditorsRegistry;
		this.actionHandlers = new ArrayList<>();
		
		addContainerProperty("entity", String.class, null);
		addContainerProperty("type", String.class, "");
		addContainerProperty("identity", String.class, "");
		addContainerProperty("status", String.class, "");
		addContainerProperty("local", String.class, "");
		addContainerProperty("credReq", String.class, "");
		setColumnHeader("entity", msg.getMessage("Identities.entity"));
		setColumnHeader("type", msg.getMessage("Identities.type"));
		setColumnHeader("identity", msg.getMessage("Identities.identity"));
		setColumnHeader("status", msg.getMessage("Identities.status"));
		setColumnHeader("local", msg.getMessage("Identities.local"));
		setColumnHeader("credReq", msg.getMessage("Identities.credReq"));
		
		setSelectable(true);
		setMultiSelect(false);
		setColumnReorderingAllowed(true);
		setColumnCollapsingAllowed(true);
		setColumnCollapsible("entity", false);
		setColumnCollapsed("local", true);
		setColumnCollapsed("credReq", true);
		
		setColumnWidth("entity", 200);
		setColumnWidth("type", 100);
		setColumnWidth("status", 100);
		setColumnWidth("local", 100);
		setColumnWidth("credReq", 180);
		
		addActionHandler(new RefreshHandler());
		addActionHandler(new ShowEntityDetailsHandler());
		addActionHandler(new RemoveFromGroupHandler());
		addActionHandler(new AddEntityActionHandler());
		addActionHandler(new AddIdentityActionHandler());
		addActionHandler(new DeleteEntityHandler());
		addActionHandler(new DeleteIdentityHandler());
		addActionHandler(new ChangeEntityStatusHandler());
		addActionHandler(new ChangeCredentialHandler());
		addActionHandler(new ChangeCredentialRequirementHandler());
		addActionHandler(new EntityAttributesClassesHandler());
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
					IdentitiesTable.this.selected = null;
					bus.fireEvent(new EntityChangedEvent(null, group));
				} else if (selected instanceof EntityWithLabel)
				{
					if (selected.equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = ((EntityWithLabel)selected).getEntity();
					bus.fireEvent(new EntityChangedEvent((EntityWithLabel)selected, group));
				} else if (selected instanceof IdentityWithEntity)
				{
					IdentityWithEntity identity = (IdentityWithEntity) selected;
					if (identity.getEntityWithLabel().getEntity().equals(IdentitiesTable.this.selected))
						return;
					IdentitiesTable.this.selected = identity.getEntityWithLabel().getEntity();
					bus.fireEvent(new EntityChangedEvent(identity.getEntityWithLabel(), group));
				}
			}
		});
	}

	@Override
	public void addActionHandler(Action.Handler actionHandler) {
		super.addActionHandler(actionHandler);
		if (actionHandler instanceof SingleActionHandler)
			actionHandlers.add((SingleActionHandler) actionHandler);
	}

	public List<SingleActionHandler> getActionHandlers()
	{
		return actionHandlers;
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
	
	public String getGroup()
	{
		return group;
	}
	
	public void setInput(String group, List<Long> entities) throws EngineException
	{
		this.group = group;
		AttributeType nameAt = attrProcessor.getAttributeTypeWithSingeltonMetadata(
				EntityNameMetadataProvider.NAME);
		this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		data.clear();
		for (Long entity: entities)
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
		Object selected = getValue();
		removeAllItems();
		if (groupByEntity)
			setGroupedContents(selected);
		else
			setFlatContents(selected);
	}
	
	/*
	 * We use a hack here: filters are temporarly removed and readded after all data is set. 
	 * This is because Vaadin (tested at 7.0.4) seems to ignore parent elements when not matching filter
	 * during addition, but properly shows them afterwards.
	 */
	private void setGroupedContents(Object selected)
	{
		Container.Filterable filterable = (Filterable) getContainerDataSource();
		filterable.removeAllContainerFilters();
		for (IdentitiesAndAttributes entry: data.values())
		{
			Entity entity = entry.getEntity();
			Object parentKey = addRow(null, entity, entry.getAttributes());
			if (selected != null && selected.equals(parentKey))
				setValue(parentKey);
			for (Identity id: entry.getIdentities())
			{
				Object key = addRow(id, entity, entry.getAttributes());
				setParent(key, parentKey);
				setChildrenAllowed(key, false);
				if (selected != null && selected.equals(key))
					setValue(key);
			}
		}
		for (Filter filter: containerFilters)
			filterable.addContainerFilter(filter);
	}

	private void setFlatContents(Object selected)
	{
		for (IdentitiesAndAttributes entry: data.values())
		{
			for (Identity id: entry.getIdentities())
			{
				Object itemId = addRow(id, entry.getEntity(), entry.getAttributes());
				setChildrenAllowed(itemId, false);
				if (selected != null && selected.equals(itemId))
					setValue(itemId);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Object addRow(Identity id, Entity ent, Map<String, Attribute<?>> attributes)
	{
		String label = null;
		if (entityNameAttribute != null && attributes.containsKey(entityNameAttribute))
			label = attributes.get(entityNameAttribute).getValues().get(0).toString() + " ";
		EntityWithLabel entWithLabel = new EntityWithLabel(ent, label);
		Object itemId = id == null ? entWithLabel : new IdentityWithEntity(id, entWithLabel);
		Item newItem = addItem(itemId);
		
		newItem.getItemProperty("entity").setValue(entWithLabel.toString());
		newItem.getItemProperty("credReq").setValue(ent.getCredentialInfo().getCredentialRequirementId());
		newItem.getItemProperty("status").setValue(msg.getMessage("EntityState."+ent.getState().name()));
		if (id != null)
		{
			newItem.getItemProperty("type").setValue(id.getTypeId());
			newItem.getItemProperty("identity").setValue(id.toPrettyStringNoPrefix());
			newItem.getItemProperty("local").setValue(new Boolean(id.isLocal()).toString());
		} else
		{
			newItem.getItemProperty("type").setValue("");
			newItem.getItemProperty("identity").setValue("");
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
						AttributeHandlerRegistry.DEFAULT_MAX_LEN*3);
				
			newItem.getItemProperty(propId).setValue(val);
		}
		return itemId;
	}
	
	private void resolveEntity(long entity) throws EngineException
	{
		Entity resolvedEntity = identitiesMan.getEntity(new EntityParam(entity));
		Collection<AttributeExt<?>> rawAttrs = attrMan.getAllAttributes(new EntityParam(entity), 
				true, "/", null, true);
		Map<String, Attribute<?>> attrs = new HashMap<String, Attribute<?>>(rawAttrs.size());
		for (Attribute<?> a: rawAttrs)
			attrs.put(a.getName(), a);
		IdentitiesAndAttributes resolved = new IdentitiesAndAttributes(resolvedEntity, 
				resolvedEntity.getIdentities(),	attrs);
		data.put(resolvedEntity.getId(), resolved);
	}
	
	private void removeEntity(long entityId)
	{
		AuthenticatedEntity entity = InvocationContext.getCurrent().getAuthenticatedEntity();
		
		if (Long.valueOf(entityId) == entity.getEntityId())
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), msg.getMessage("Identities.notRemovingLoggedUser"));
			return;
		}
		try
		{
			identitiesMan.removeEntity(new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("Identities.removeEntityError"), e);
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
			ErrorPopup.showError(msg, msg.getMessage("Identities.removeIdentityError"), e);
		}
	}

	private boolean setEntityStatus(long entityId, EntityState newState)
	{
		try
		{
			identitiesMan.setEntityStatus(new EntityParam(entityId), newState);
			refresh();
			return true;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("Identities.changeEntityStatusError"), e);
			return false;
		}
	}
	
	private void removeFromGroup(long entityId)
	{
		try
		{
			groupsMan.removeMember(group, new EntityParam(entityId));
			refresh();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("Identities.removeFromGroupError"), e);
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
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmRemoveFromGroup", entity, group),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeFromGroup(entity.getEntity().getId());
				}
			}).show();
		}
	}

	private class AddEntityActionHandler extends SingleActionHandler
	{
		public AddEntityActionHandler()
		{
			super(msg.getMessage("Identities.addEntityAction"), Images.addEntity.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			new EntityCreationDialog(msg, group, identitiesMan, groupsMan, 
					authnMan, attrHandlerRegistry,
					attrMan, identityEditorReg, 
					new EntityCreationDialog.Callback()
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
			super(msg.getMessage("Identities.addIdentityAction"), Images.addIdentity.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			new IdentityCreationDialog(msg, entity.getEntity().getId(), identitiesMan,  
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
					Images.deleteEntity.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			new ConfirmDialog(msg, msg.getMessage("Identities.confirmEntityDelete", entity),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeEntity(entity.getEntity().getId());
				}
			}).show();
		}
	}

	
	private class DeleteIdentityHandler extends SingleActionHandler
	{
		public DeleteIdentityHandler()
		{
			super(msg.getMessage("Identities.deleteIdentityAction"), 
					Images.deleteIdentity.getResource());
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

	private class ChangeEntityStatusHandler extends SingleActionHandler
	{
		public ChangeEntityStatusHandler()
		{
			super(msg.getMessage("Identities.changeEntityStatusAction"), 
					Images.editUser.getResource());
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			EntityState currentState = data.get(entity.getEntity().getId()).getEntity().getState();
			new ChangeEntityStateDialog(msg, entity, currentState, new ChangeEntityStateDialog.Callback()
			{
				@Override
				public boolean onChanged(EntityState newState)
				{
					return setEntityStatus(entity.getEntity().getId(), newState);
				}
			}).show();
		}
	}

	private class ChangeCredentialRequirementHandler extends SingleActionHandler
	{
		public ChangeCredentialRequirementHandler()
		{
			super(msg.getMessage("Identities.changeCredentialRequirementAction"), 
					Images.key.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			IdentitiesAndAttributes info = data.get(entity.getEntity().getId());
			String currentCredId = info.getEntity().getCredentialInfo().getCredentialRequirementId();
			new CredentialRequirementDialog(msg, entity, currentCredId,
					identitiesMan, authnMan, new Callback()
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
					Images.key.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			new CredentialsChangeDialog(msg, entity.getEntity().getId(), authnMan, identitiesMan,
					credEditorsRegistry, new CredentialsChangeDialog.Callback()
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
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			refresh();
		}
	}

	private void showEntityDetails(EntityWithLabel entity)
	{
		final EntityDetailsPanel identityDetailsPanel = new EntityDetailsPanel(msg);
		Collection<String> groups;
		try
		{
			groups = identitiesMan.getGroups(new EntityParam(entity.getEntity().getId()));
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), e);
			return;
		}
		identityDetailsPanel.setInput(entity, groups);
		new EntityDetailsDialog(msg, identityDetailsPanel).show();
	}
	
	private class ShowEntityDetailsHandler extends SingleActionHandler
	{
		public ShowEntityDetailsHandler()
		{
			super(msg.getMessage("Identities.showEntityDetails"), 
					Images.userMagnifier.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			showEntityDetails(entity);
		}
	}

	private class EntityAttributesClassesHandler extends SingleActionHandler
	{
		public EntityAttributesClassesHandler()
		{
			super(msg.getMessage("Identities.editEntityACs"), 
					Images.attributes.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final EntityWithLabel entity = target instanceof IdentityWithEntity ? 
					((IdentityWithEntity) target).getEntityWithLabel() : ((EntityWithLabel)target);
			EntityAttributesClassesDialog dialog = new EntityAttributesClassesDialog(msg, group, 
					entity, attrMan, groupsMan, new EntityAttributesClassesDialog.Callback()
					{
						@Override
						public void onChange()
						{
							refresh();
						}
					});
			dialog.show();
		}
	}

	
	/**
	 * Complete info about entity: its identities and relevant attributes.
	 * Used to populate table.
	 */
	private static class IdentitiesAndAttributes
	{
		private Entity entity;
		private Identity[] identities;
		private Map<String, Attribute<?>> attributes;

		public IdentitiesAndAttributes(Entity entity, Identity[] identities, Map<String, Attribute<?>> attributes)
		{
			this.identities = identities;
			this.attributes = attributes;
			this.entity = entity;
		}
		public Identity[] getIdentities()
		{
			return identities;
		}
		public Map<String, Attribute<?>> getAttributes()
		{
			return attributes;
		}
		public Entity getEntity()
		{
			return entity;
		}
	}
	
	/**
	 * Identity with its Entity. Used as item id for the rows with particular identities.
	 * @author K. Benedyczak
	 */
	public static class IdentityWithEntity
	{
		private Identity identity;
		private EntityWithLabel entity;
		public IdentityWithEntity(Identity identity, EntityWithLabel entity)
		{
			super();
			this.identity = identity;
			this.entity = entity;
		}
		public Identity getIdentity()
		{
			return identity;
		}
		public EntityWithLabel getEntityWithLabel()
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




