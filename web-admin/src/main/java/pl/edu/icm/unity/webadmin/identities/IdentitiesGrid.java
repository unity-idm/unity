/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.MultiSelectionModel;

import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Displays a tree grid with identities. Can present contents in two modes: 
 *  - flat, where each identity is a fully separate table row
 *  - grouped by entity, where each entity has all its entities as children
 * @author K. Benedyczak
 */
@PrototypeComponent
public class IdentitiesGrid extends TreeGrid<IdentityEntry>
{
	public static final String ATTR_COL_PREFIX = "a::";
	public static final String ATTR_ROOT_COL_PREFIX = ATTR_COL_PREFIX + "root::";
	public static final String ATTR_CURRENT_COL_PREFIX = ATTR_COL_PREFIX + "current::";
	public static final int ATTR_COL_RATIO = 180;
	
	enum BaseColumn {
		entity("Identities.entity", false, false, 200), 
		type("Identities.type", true, false, 100), 
		identity("Identities.identity", true, false, 300), 
		status("Identities.status", true, false, 100), 
		local("Identities.local", true, true, 100), 
		dynamic("Identities.dynamic", true, true, 100), 
		credReq("Identities.credReq", true, true, 180), 
		target("Identities.target", true, true, 180), 
		realm("Identities.realm", true, true, 100),
		remoteIdP("Identities.remoteIdP", true, true, 200), 
		profile("Identities.profile", true, true, 100), 
		scheduledOperation("Identities.scheduledOperation", true, true, 200);

		private String captionKey;
		private boolean collapsingAllowed;
		private boolean initiallyCollapsed;
		private int defWidth;
		
		BaseColumn(String captionKey, boolean collapsingAllowed, 
				boolean initiallyCollapsed, int defWidth)
		{
			this.captionKey = captionKey;
			this.collapsingAllowed = collapsingAllowed;
			this.initiallyCollapsed = initiallyCollapsed;
			this.defWidth = defWidth;
		}
	};

	private final AttributeSupport attributeSupport;
	private final IdentityTypeSupport idTypeSupport;
	private final UnityMessageSource msg;
	private final EntitiesLoader entitiesLoader;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	
	private boolean groupByEntity;
	private boolean showTargeted;
	private String group;
	private String entityNameAttribute = null;
	private Map<String, IdentityTypeDefinition> typeDefinitionsMap;
	private List<ResolvedEntity> cachedEntitites;
	private TreeData<IdentityEntry> treeData;
	private TreeDataProvider<IdentityEntry> dataProvider;
	private List<EntityFilter> filters;
	private GridContextMenuSupport<IdentityEntry> contextMenuSupp;
	private EventsBus bus;
	private IdentityEntry lastSelected;
	
	@SuppressWarnings("unchecked")
	@Autowired
	public IdentitiesGrid(UnityMessageSource msg, AttributeSupport attributeSupport,
			IdentityTypeSupport idTypeSupport, EntitiesLoader entitiesLoader,
			AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.idTypeSupport = idTypeSupport;
		this.entitiesLoader = entitiesLoader;
		this.attrHandlerRegistry = attrHandlerRegistry;
		createBaseColumns();
		cachedEntitites = new ArrayList<>(200);
		dataProvider = (TreeDataProvider<IdentityEntry>) getDataProvider();
		treeData = dataProvider.getTreeData();
		filters = new ArrayList<>();
		contextMenuSupp = new GridContextMenuSupport<>(this);
		this.bus = WebSession.getCurrent().getEventBus();
		
		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);
		
		((MultiSelectionModel<IdentityEntry>)getSelectionModel())
			.addMultiSelectionListener(this::selectionChanged);
		setSizeFull();
	}
	
	private void createBaseColumns()
	{
		for (BaseColumn column: BaseColumn.values())
		{
			addColumn(ie -> ie.getBaseValue(column))
				.setCaption(msg.getMessage(column.captionKey))
				.setExpandRatio(column.defWidth)
				.setHidable(column.collapsingAllowed)
				.setHidden(column.initiallyCollapsed)
				.setId(column.name());
		}
	}

	public void addActionHandler(SingleActionHandler2<IdentityEntry> actionHandler) 
	{
		contextMenuSupp.addActionHandler(actionHandler);
	}

	public List<SingleActionHandler2<IdentityEntry>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
	}
	
	public void setMode(boolean groupByEntity)
	{
		this.groupByEntity = groupByEntity;
		//TODO
		//if (entitiesLoader.isDone())
			reloadTableContentsFromData();
	}
	
	public void setShowTargeted(boolean showTargeted) throws EngineException
	{
		this.showTargeted = showTargeted;
		showGroup(group);
	}	
	
	public String getGroup()
	{
		return this.group;
	}
	
	public void showGroup(String group) throws EngineException
	{
		this.group = group;
		AttributeType nameAt = attributeSupport.getAttributeTypeWithSingeltonMetadata(
				EntityNameMetadataProvider.NAME);
		this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		typeDefinitionsMap = idTypeSupport.getTypeDefinitionsMap();
		updateAttributeColumnHeaders();

		Set<IdentityEntry> selected = getSelectedItems();
		
		treeData.clear();
		dataProvider.refreshAll();
		cachedEntitites.clear();
		if (group != null)
			entitiesLoader.reload(selected, group, showTargeted, 
					this::addAndCacheResolvedEntities);
	}
	
	private void reloadTableContentsFromData()
	{
		Set<IdentityEntry> selected = getSelectedItems();
		treeData.clear();
		dataProvider.refreshAll();
		addResolvedEntities(cachedEntitites, selected, -1);
	}
	
	private void addAndCacheResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected,
			float progress)
	{
		cachedEntitites.addAll(entities);
		addResolvedEntities(entities, selected, progress);
	}
	
	private void addResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected,
			float progress)
	{
		for (ResolvedEntity entity: entities)
		{
			if (groupByEntity)
				addGroupedEntriesToTable(entity, selected);
			else
				addFlatEntriesToTable(entity, selected);
		}
		dataProvider.refreshAll();
	}
	
	private void addGroupedEntriesToTable(ResolvedEntity resolvedEntity, 
			Set<IdentityEntry> savedSelection)
	{
		Entity entity = resolvedEntity.getEntity();
		IdentityEntry parentEntry = createEntry(null, entity, 
				resolvedEntity.getRootAttributes(), 
				resolvedEntity.getCurrentAttributes());
		
		treeData.addItem(null, parentEntry);
		restoreSelectionIfMatching(savedSelection, parentEntry);
		for (Identity id: resolvedEntity.getIdentities())
		{
			IdentityEntry childEntry = createEntry(id, entity, 
					resolvedEntity.getRootAttributes(), 
					resolvedEntity.getCurrentAttributes());
			treeData.addItem(parentEntry, childEntry);
			restoreSelectionIfMatching(savedSelection, childEntry);
		}
	}

	private void restoreSelectionIfMatching(Set<IdentityEntry> savedSelection, IdentityEntry currentEntry)
	{
		if (savedSelection.contains(currentEntry) && getSelectedItems().isEmpty())
			select(currentEntry);
	}
	
	private void addFlatEntriesToTable(ResolvedEntity resolvedEntity, 
			Set<IdentityEntry> savedSelection)
	{
		for (Identity id: resolvedEntity.getIdentities())
		{
			IdentityEntry idEntry = createEntry(id, resolvedEntity.getEntity(), 
					resolvedEntity.getRootAttributes(), 
					resolvedEntity.getCurrentAttributes());
			treeData.addItem(null, idEntry);
			restoreSelectionIfMatching(savedSelection, idEntry);
		}
	}
	
	private IdentityEntry createEntry(Identity id, Entity ent, Map<String, Attribute> rootAttributes,
			Map<String, Attribute> curAttributes)
	{
		String label = null;
		if (entityNameAttribute != null && rootAttributes.containsKey(entityNameAttribute))
			label = rootAttributes.get(entityNameAttribute).getValues().get(0).toString() + " ";
		EntityWithLabel entWithLabel = new EntityWithLabel(ent, label);
		
		Map<String, String> attributesByColumnId = new HashMap<>();
		List<Column<IdentityEntry, ?>> columns = getColumns();
		for (Column<IdentityEntry, ?> column: columns)
		{
			String columnId = column.getId();
			if (columnId == null || !columnId.startsWith(ATTR_COL_PREFIX))
				continue;
			Attribute attribute = getAttributeForColumnProperty(columnId, rootAttributes, curAttributes);
			String val;
			if (attribute == null)
				val = msg.getMessage("Identities.attributeUndefined");
			else
				val = attrHandlerRegistry.getSimplifiedAttributeValuesRepresentation(attribute);
			attributesByColumnId.put(columnId, val);
		}
		
		return id == null ? 
				new IdentityEntry(entWithLabel, attributesByColumnId, msg) : 
				new IdentityEntry(entWithLabel, attributesByColumnId, id, 
						typeDefinitionsMap.get(id.getTypeId()), msg);
	}

	/**
	 * Adds a new attribute column.
	 * 
	 * @param attribute
	 * @param group group from where the attribute's value should be displayed. If it is null then the current 
	 * group is used. Otherwise root group is assumed (in future other 'fixed' groups might be supported, 
	 * but it isn't implemented yet)
	 */
	public void addAttributeColumn(String attribute, String group)
	{
		String key = (group == null) ? ATTR_CURRENT_COL_PREFIX + attribute 
				: ATTR_ROOT_COL_PREFIX + attribute;
		addColumn(ie -> ie.getAttribute(key))
			.setCaption(attribute + (group == null ? "@" + this.group : "@/"))
			.setExpandRatio(ATTR_COL_RATIO)
			.setHidable(true)
			.setHidden(false)
			.setId(key);
		try
		{
			showGroup(this.group);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("Identities.internalError", e.getMessage()), e);
		}
	}

	public void removeAttributeColumn(String group, String... attributes)
	{
		for (String attribute: attributes)
		{
			if (group.equals("/"))
				removeColumn(ATTR_ROOT_COL_PREFIX + attribute);
			else if (group.equals(this.group))
				removeColumn(ATTR_CURRENT_COL_PREFIX + attribute);
		}
		reloadTableContentsFromData();
	}

	public Set<String> getAttributeColumns(boolean root)
	{
		List<Column<IdentityEntry, ?>> columns = getColumns();
		Set<String> ret = new HashSet<String>();
		for (Column<IdentityEntry, ?> column: columns)
		{
			String property = column.getId();
			if (root)
			{
				if (property.startsWith(ATTR_ROOT_COL_PREFIX))
					ret.add(property.substring(ATTR_ROOT_COL_PREFIX.length()));
			} else
			{
				if (property.startsWith(ATTR_CURRENT_COL_PREFIX))
					ret.add(property.substring(ATTR_CURRENT_COL_PREFIX.length()));
			}
		}
		return ret;
	}

	private void updateAttributeColumnHeaders()
	{
		List<Column<IdentityEntry, ?>> columns = getColumns();
		for (Column<IdentityEntry, ?> column: columns)
		{
			String property = column.getId();
			if (property.startsWith(ATTR_CURRENT_COL_PREFIX))
			{
				String attrName = property.substring(ATTR_CURRENT_COL_PREFIX.length());
				column.setCaption(attrName + "@" + this.group);
			}
		}
	}
		
	private Attribute getAttributeForColumnProperty(String propId, Map<String, Attribute> rootAttributes, 
			Map<String, Attribute> curAttributes)
	{
		if (propId.startsWith(ATTR_CURRENT_COL_PREFIX))
		{
			String attributeName = propId.substring(ATTR_CURRENT_COL_PREFIX.length());
			return curAttributes.get(attributeName);
		} else
		{
			String attributeName = propId.substring(ATTR_ROOT_COL_PREFIX.length());
			return rootAttributes.get(attributeName);
		}
	}

	public void addFilter(EntityFilter filter)
	{
		dataProvider.addFilter(filter);
		filters.add(filter);
	}

	public void removeFilter(EntityFilter filter)
	{
		filters.remove(filter);
		setFilters();
	}

	private void setFilters()
	{
		dataProvider.clearFilters();
		for (EntityFilter filter: filters)
			dataProvider.addFilter(filter);
	}

	public List<String> getColumnIds()
	{
		return getColumns().stream()
				.map(column -> column.getId())
				.collect(Collectors.toList());
	}
	
	public Set<String> getVisibleColumnIds()
	{
		return getColumns().stream()
				.filter(column -> !column.isHidden())
				.map(column -> column.getId())
				.collect(Collectors.toSet());
	}
	
	void removeIdentity(IdentityEntry entry)
	{
		for (ResolvedEntity cached: cachedEntitites)
		{
			if (cached.getEntity().getId() == entry.getSourceEntity().getEntity().getId())
			{
				cached.getIdentities().remove(entry.getSourceIdentity());
				break;
			}
		}
		treeData.removeItem(entry);
		dataProvider.refreshAll();
	}
	
	void removeEntity(EntityWithLabel removed)
	{
		long removedId = removed.getEntity().getId();
		for (int i = 0; i<cachedEntitites.size(); i++)
			if (cachedEntitites.get(i).getEntity().getId() == removedId)
			{
				cachedEntitites.remove(i);
				break;
			}
		
		
		if (!groupByEntity)
		{
			HierarchicalQuery<IdentityEntry, SerializablePredicate<IdentityEntry>> query = 
					new HierarchicalQuery<>(
							ie -> ie.getSourceEntity().getEntity().getId() == removedId,
							null);
			List<IdentityEntry> fetched = dataProvider.fetch(query).collect(Collectors.toList());
			for (IdentityEntry ie: fetched)
				treeData.removeItem(ie);
		} else
		{
			HierarchicalQuery<IdentityEntry, SerializablePredicate<IdentityEntry>> query = 
					new HierarchicalQuery<>(
							ie -> ie.getSourceEntity().getEntity().getId() == removedId, 
							null);
			List<IdentityEntry> fetched = dataProvider.fetch(query).collect(Collectors.toList());
			//should be only one entry - parent node
			for (IdentityEntry ie: fetched)
				treeData.removeItem(ie);
		}
		dataProvider.refreshAll();
	}
	
	private void selectionChanged(MultiSelectionEvent<IdentityEntry> event)
	{
		Set<IdentityEntry> selectedItems = event.getAllSelectedItems();
		IdentityEntry selected = null;
		if (selectedItems.size() == 1)
		{
			selected = selectedItems.iterator().next();
		}
		if (selected == null)
		{
			lastSelected = null;
			bus.fireEvent(new EntityChangedEvent(null, group));
		} else if (selected.getSourceIdentity() == null)
		{
			if (selected.equals(lastSelected))
				return;
			lastSelected = selected;
			bus.fireEvent(new EntityChangedEvent(selected.getSourceEntity(), group));
		} else 
		{
			if (lastSelected != null && selected.getSourceEntity().getEntity().equals(
					lastSelected.getSourceEntity().getEntity()))
				return;
			lastSelected = selected;
			bus.fireEvent(new EntityChangedEvent(selected.getSourceEntity(), group));
		}
	}
}
