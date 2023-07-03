/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.MultiSelectionModel;

import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupChangedEvent;
import io.imunity.webconsole.directoryBrowser.identities.IdentityCreationDialog.IdentityCreationDialogHandler;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.CachedAttributeHandlers;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;

/**
 * Displays a tree grid with identities. Can present contents in two modes: -
 * flat, where each identity is a fully separate table row - grouped by entity,
 * where each entity has all its entities as children
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class IdentitiesTreeGrid extends TreeGrid<IdentityEntry>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesTreeGrid.class);
	public static final String ENTITY_DND_TYPE = "entity";

	private final AttributeSupport attributeSupport;
	private final CredentialManagement credentialManagement;
	private final IdentityTypeSupport idTypeSupport;
	private final MessageSource msg;
	private final EntitiesLoader entitiesLoader;
	private final AttributeHandlerRegistryV8 attrHandlerRegistry;

	private boolean groupByEntity;
	private boolean showTargeted;
	private Group group;
	private String entityNameAttribute = null;
	private Map<String, IdentityTypeDefinition> typeDefinitionsMap;
	private Map<String, CredentialDefinition> credentialDefinitions;
	private List<ResolvedEntity> cachedEntitites;
	private TreeData<IdentityEntry> treeData;
	private TreeDataProvider<IdentityEntry> dataProvider;
	private List<EntityFilter> filters;
	private EventsBus bus;
	private IdentityEntry lastSelected;
	private PreferencesManagement preferencesMan;

	private Column<IdentityEntry, MenuBar> actionColumn;

	private EntityDetailsHandler entityDetailsHandler;
	private AddToGroupHandler addToGroupHandler;
	private RemoveFromGroupHandler removeFromGroupHandler;
	private IdentityCreationDialogHandler identityCreationDialogHanlder;
	private ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory;
	private ChangeEntityStateHandler changeEntityStateHandler;
	private ChangeCredentialRequirementHandler credentialRequirementHandler;
	private EntityAttributeClassHandler entityAttributeClassHandler;
	private IdentityConfirmationResendHandler confirmationResendHandler;
	private IdentityConfirmHandler confirmHandler;
	private DeleteIdentityHandler deleteIdentityHandler;
	private DeleteEntityHandler deleteEntityHandler;

	@SuppressWarnings("unchecked")
	@Autowired
	public IdentitiesTreeGrid(MessageSource msg, AttributeSupport attributeSupport,
	                          IdentityTypeSupport idTypeSupport, EntitiesLoader entitiesLoader,
	                          AttributeHandlerRegistryV8 attrHandlerRegistry, PreferencesManagement preferencesMan,
	                          CredentialManagement credentialManagement, EntityDetailsHandler entityDetailsHandler,
	                          AddToGroupHandler addToGroupHandler, RemoveFromGroupHandler removeFromGroupHandler,
	                          IdentityCreationDialogHandler identityCreationDialogHanlder,
	                          ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory,
	                          ChangeEntityStateHandler changeEntityStateHandler,
	                          ChangeCredentialRequirementHandler credentialRequirementHandler,
	                          EntityAttributeClassHandler entityAttributeClassHandler,
	                          IdentityConfirmationResendHandler confirmationResendHandler,
	                          IdentityConfirmHandler confirmHandler, DeleteIdentityHandler deleteIdentityHandler,
	                          DeleteEntityHandler deleteEntityHandler)

	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.idTypeSupport = idTypeSupport;
		this.entitiesLoader = entitiesLoader;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.preferencesMan = preferencesMan;
		this.credentialManagement = credentialManagement;

		this.entityDetailsHandler = entityDetailsHandler;
		this.addToGroupHandler = addToGroupHandler;
		this.removeFromGroupHandler = removeFromGroupHandler;
		this.identityCreationDialogHanlder = identityCreationDialogHanlder;
		this.credentialChangeDialogFactory = credentialChangeDialogFactory;
		this.changeEntityStateHandler = changeEntityStateHandler;
		this.credentialRequirementHandler = credentialRequirementHandler;
		this.entityAttributeClassHandler = entityAttributeClassHandler;
		this.confirmationResendHandler = confirmationResendHandler;
		this.confirmHandler = confirmHandler;
		this.deleteIdentityHandler = deleteIdentityHandler;
		this.deleteEntityHandler = deleteEntityHandler;

		createBaseColumns();
		cachedEntitites = new ArrayList<>(200);
		dataProvider = (TreeDataProvider<IdentityEntry>) getDataProvider();
		treeData = dataProvider.getTreeData();
		filters = new ArrayList<>();
		this.bus = WebSession.getCurrent().getEventBus();

		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);

		((MultiSelectionModel<IdentityEntry>) getSelectionModel())
				.addMultiSelectionListener(event -> selectionChanged(event.getAllSelectedItems()));
		setSizeFull();
		setColumnReorderingAllowed(true);
		setStyleName(Styles.denseTreeGrid.toString());
		updateCredentialStatusColumns();

		loadPreferences();

		addColumnVisibilityChangeListener(event -> savePreferences());
		addColumnResizeListener(event -> savePreferences());
		addColumnReorderListener(event -> {
			savePreferences();
			refreshActionColumn();
		});

		GridDragSource<IdentityEntry> drag = new GridDragSource<>(this);
		drag.setEffectAllowed(EffectAllowed.MOVE);
		drag.setDragDataGenerator(ENTITY_DND_TYPE, entity -> "{}");
		drag.addGridDragStartListener(event -> {
			drag.setDragData(event.getDraggedItems().stream().map(i -> i.getSourceEntity())
					.collect(Collectors.toSet()));
		});
		drag.addGridDragEndListener(event -> drag.setDragData(null));
		refreshActionColumn();
	}

	private void createBaseColumns()
	{
		for (BaseColumn column : BaseColumn.values())
		{
			addColumn(ie -> ie.getBaseValue(column)).setCaption(msg.getMessage(column.captionKey))
					.setExpandRatio(column.defWidth).setHidable(column.collapsingAllowed)
					.setHidden(column.initiallyCollapsed).setId(column.name());
		}
	}

	private void refreshActionColumn()
	{
		if (actionColumn != null)
			removeColumn(actionColumn.getId());
		actionColumn = addComponentColumn(n -> getRowHamburgerMenuComponent(Sets.newHashSet(n)))
				.setExpandRatio(0).setId(IdentitiesGridColumnConstans.ACTION_COLUMN_ID)
				.setHidable(false).setSortable(false).setResizable(false).setMinimumWidth(60)
				.setCaption(msg.getMessage("actions"));
	}

	private MenuBar getRowHamburgerMenuComponent(Set<IdentityEntry> target)
	{

		HamburgerMenu<IdentityEntry> menu = new HamburgerMenu<IdentityEntry>();
		menu.setTarget(target);
		menu.addActionHandlers(Arrays.asList(entityDetailsHandler.getShowEntityAction(),
				addToGroupHandler.getAction(),
				removeFromGroupHandler.getAction(this::getGroupPath, this::refresh),
				identityCreationDialogHanlder.getAction(a -> refresh()),
				changeEntityStateHandler.getAction(this::refresh), getChangeCredentialAction(),
				credentialRequirementHandler.getAction(this::refresh),
				entityAttributeClassHandler.getAction(this::refresh, this::getGroupPath),
				confirmationResendHandler.getAction(), confirmHandler.getAction(this::refresh),
				deleteIdentityHandler.getAction(this::removeIdentity, this::refresh),
				deleteEntityHandler.getAction(this::removeEntity)));

		return menu;
	}

	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(getGroup()));
	}

	public void setMode(boolean groupByEntity)
	{
		this.groupByEntity = groupByEntity;
		reloadTableContentsFromData();
		savePreferences();
	}

	public void setShowTargeted(boolean showTargeted) throws EngineException
	{
		this.showTargeted = showTargeted;
		showGroup(group);
		savePreferences();
	}

	public String getGroupPath()
	{
		return group.getPathEncoded();
	}

	
	public Group getGroup()
	{
		return group;
	}

	public void showGroup(Group group) throws EngineException
	{
		this.group = group;
		AttributeType nameAt = attributeSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		typeDefinitionsMap = idTypeSupport.getTypeDefinitionsMap();
		updateCredentialStatusColumns();
		updateAttributeColumnHeaders();

		Set<IdentityEntry> selected = getSelectedItems();

		treeData.clear();
		dataProvider.refreshAll();
		cachedEntitites.clear();
		getSelectionModel().deselectAll();
		if (group != null)
			entitiesLoader.reload(selected, group.getPathEncoded(), showTargeted, this::addAndCacheResolvedEntities);
	}

	private void reloadTableContentsFromData()
	{
		Set<IdentityEntry> selected = getSelectedItems();
		treeData.clear();
		dataProvider.refreshAll();
		addResolvedEntities(cachedEntitites, selected, -1);
	}
	
	public List<IdentityEntry> getItems()
	{
		
		List<IdentityEntry> entries = new ArrayList<>();
				
		for (IdentityEntry e : dataProvider.getTreeData().getRootItems())
		{
			entries.add(e);
			entries.addAll(dataProvider.getTreeData().getChildren(e));
		}
		return entries;	
	}

	private void addAndCacheResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected,
			float progress)
	{
		cachedEntitites.addAll(entities);
		addResolvedEntities(entities, selected, progress);
	}

	private void addResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected, float progress)
	{
		CachedAttributeHandlers attributeHandlers = new CachedAttributeHandlers(attrHandlerRegistry);
		for (ResolvedEntity entity : entities)
		{
			if (groupByEntity)
				addGroupedEntriesToTable(entity, selected, attributeHandlers);
			else
				addFlatEntriesToTable(entity, selected, attributeHandlers);
		}
		dataProvider.refreshAll();
	}

	private void addGroupedEntriesToTable(ResolvedEntity resolvedEntity, Set<IdentityEntry> savedSelection,
			CachedAttributeHandlers attributeHandlers)
	{
		Entity entity = resolvedEntity.getEntity();
		IdentityEntry parentEntry = createEntry(null, entity, resolvedEntity.getRootAttributes(),
				resolvedEntity.getCurrentAttributes(), attributeHandlers);
		treeData.addItem(null, parentEntry);
		restoreSelectionIfMatching(savedSelection, parentEntry);
		for (Identity id : resolvedEntity.getIdentities())
		{
			IdentityEntry childEntry = createEntry(id, entity, resolvedEntity.getRootAttributes(),
					resolvedEntity.getCurrentAttributes(), attributeHandlers);
			treeData.addItem(parentEntry, childEntry);
			restoreSelectionIfMatching(savedSelection, childEntry);
		}
	}

	private void restoreSelectionIfMatching(Set<IdentityEntry> savedSelection, IdentityEntry currentEntry)
	{
		if (savedSelection.contains(currentEntry) && getSelectedItems().isEmpty())
			select(currentEntry);
	}

	private void addFlatEntriesToTable(ResolvedEntity resolvedEntity, Set<IdentityEntry> savedSelection,
			CachedAttributeHandlers attributeHandlers)
	{
		for (Identity id : resolvedEntity.getIdentities())
		{
			IdentityEntry idEntry = createEntry(id, resolvedEntity.getEntity(),
					resolvedEntity.getRootAttributes(), resolvedEntity.getCurrentAttributes(),
					attributeHandlers);
			treeData.addItem(null, idEntry);
			restoreSelectionIfMatching(savedSelection, idEntry);
		}
	}

	private IdentityEntry createEntry(Identity id, Entity ent, Map<String, ? extends Attribute> rootAttributes,
			Map<String, ? extends Attribute> curAttributes, CachedAttributeHandlers attributeHandlers)
	{
		String label = null;
		if (entityNameAttribute != null && rootAttributes.containsKey(entityNameAttribute))
			label = rootAttributes.get(entityNameAttribute).getValues().get(0).toString() + " ";
		EntityWithLabel entWithLabel = new EntityWithLabel(ent, label);

		Map<String, String> attributesByColumnId = new HashMap<>();
		List<Column<IdentityEntry, ?>> columns = getColumns();
		for (Column<IdentityEntry, ?> column : columns)
		{
			String columnId = column.getId();
			if (columnId == null || !columnId.startsWith(IdentitiesGridColumnConstans.ATTR_COL_PREFIX))
				continue;
			Attribute attribute = getAttributeForColumnProperty(columnId, rootAttributes, curAttributes);
			String val;
			if (attribute == null)
				val = msg.getMessage("Identities.attributeUndefined");
			else
				val = attributeHandlers.getSimplifiedAttributeValuesRepresentation(attribute);
			attributesByColumnId.put(columnId, val);
		}

		return id == null ? new IdentityEntry(entWithLabel, attributesByColumnId, msg)
				: new IdentityEntry(entWithLabel, attributesByColumnId, id,
						typeDefinitionsMap.get(id.getTypeId()), msg);
	}

	private void updateCredentialStatusColumns()
	{
		try
		{
			credentialDefinitions = credentialManagement.getCredentialDefinitions().stream()
					.collect(Collectors.toMap(credDef -> credDef.getName(), cd -> cd));
		} catch (EngineException e)
		{
			throw new InternalException("Can not load credentials", e);
		}
		for (Map.Entry<String, CredentialDefinition> cd : credentialDefinitions.entrySet())
		{
			String colKey = IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX + cd.getKey();
			if (getColumn(colKey) == null)
			{
				addColumn(ie -> ie.getCredentialStatus(cd.getKey())).setId(colKey)
						.setCaption(cd.getValue().getName())
						.setExpandRatio(IdentitiesGridColumnConstans.CRED_STATUS_COL_RATIO)
						.setHidable(true).setHidden(true);
			}
		}

		getColumnIds().stream()
				.filter(colId -> colId.startsWith(IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX))
				.map(colId -> colId.substring(
						IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX.length()))
				.filter(credId -> !credentialDefinitions.containsKey(credId))
				.forEach(credId -> removeColumn(
						IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX + credId));
	}

	/**
	 * Adds a new attribute column.
	 * 
	 * @param attribute
	 * @param group
	 *                group from where the attribute's value should be
	 *                displayed. If it is null then the current group is
	 *                used. Otherwise root group is assumed (in future other
	 *                'fixed' groups might be supported, but it isn't
	 *                implemented yet)
	 */
	void addAttributeColumn(String attribute, String group)
	{
		String key = (group == null) ? IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX + attribute
				: IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX + attribute;
		if (getColumn(key) != null)
		{
			NotificationPopup.showError(msg.getMessage("Identities.customColumnExists"), "");
			return;
		}

		addColumn(ie -> ie.getAttribute(key)).setCaption(attribute + (group == null ? "@" + this.group : "@/"))
				.setExpandRatio(IdentitiesGridColumnConstans.ATTR_COL_RATIO).setHidable(true)
				.setHidden(false).setId(key);

		savePreferences();
		try
		{
			showGroup(this.group);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.internalError", e.getMessage()), e);
		}
	}

	void removeAttributeColumn(String group, String attribute)
	{
		if (Strings.isEmpty(group))
			removeColumn(IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX + attribute);
		else if (group.equals(this.group.getPathEncoded()))
			removeColumn(IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX + attribute);
		reloadTableContentsFromData();
		savePreferences();
	}

	Set<String> getAttributeColumns(boolean root)
	{
		List<Column<IdentityEntry, ?>> columns = getColumns();
		Set<String> ret = new HashSet<String>();
		for (Column<IdentityEntry, ?> column : columns)
		{
			String property = column.getId();
			if (root)
			{
				if (property.startsWith(IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX))
					ret.add(property.substring(
							IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX.length()));
			} else
			{
				if (property.startsWith(IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX))
					ret.add(property.substring(
							IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX.length()));
			}
		}
		return ret;
	}

	private void updateAttributeColumnHeaders()
	{
		List<Column<IdentityEntry, ?>> columns = getColumns();
		for (Column<IdentityEntry, ?> column : columns)
		{
			String property = column.getId();
			if (property.startsWith(IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX))
			{
				String attrName = property.substring(
						IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX.length());
				column.setCaption(attrName + "@" + this.group);
			}
		}
	}

	private Attribute getAttributeForColumnProperty(String propId, Map<String, ? extends Attribute> rootAttributes,
			Map<String, ? extends Attribute> curAttributes)
	{
		if (propId.startsWith(IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX))
		{
			String attributeName = propId
					.substring(IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX.length());
			return curAttributes.get(attributeName);
		} else
		{
			String attributeName = propId
					.substring(IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX.length());
			return rootAttributes.get(attributeName);
		}
	}

	private SingleActionHandler<IdentityEntry> getChangeCredentialAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeCredentialAction"))
				.withIcon(Images.key.getResource()).withHandler(this::showChangeCredentialDialog)
				.build();
	}

	private void showChangeCredentialDialog(Set<IdentityEntry> selection)
	{
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		credentialChangeDialogFactory.getObject().init(entity.getEntity().getId(), false, changed -> {
			if (changed)
				refresh();
		}).show();
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
		for (EntityFilter filter : filters)
			dataProvider.addFilter(filter);
	}
	
	public void clearFilters()
	{
		dataProvider.clearFilters();
	}


	public List<String> getColumnIds()
	{
		return getColumns().stream().map(column -> column.getId()).collect(Collectors.toList());
	}

	public Set<String> getVisibleColumnIds()
	{
		return getColumns().stream().filter(column -> !column.isHidden()).map(column -> column.getId())
				.collect(Collectors.toSet());
	}

	public Boolean isGroupByEntity()
	{
		return groupByEntity;
	}

	public Boolean isShowTargeted()
	{
		return showTargeted;
	}

	void removeIdentity(IdentityEntry entry)
	{
		Set<IdentityEntry> selected = getSelectedItems();	
		for (ResolvedEntity cached : cachedEntitites)
		{
			if (cached.getEntity().getId() == entry.getSourceEntity().getEntity().getId())
			{
				cached.getIdentities().remove(entry.getSourceIdentity());
				break;
			}
		}
		if (selected.contains(entry))
		{
			deselect(entry);
		}
		treeData.removeItem(entry);
		dataProvider.refreshAll();
	}

	void removeEntity(EntityWithLabel removed)
	{
		Set<IdentityEntry> selected = getSelectedItems();
		long removedId = removed.getEntity().getId();
		for (int i = 0; i < cachedEntitites.size(); i++)
			if (cachedEntitites.get(i).getEntity().getId() == removedId)
			{
				cachedEntitites.remove(i);
				break;
			}

		if (!groupByEntity)
		{
			HierarchicalQuery<IdentityEntry, SerializablePredicate<IdentityEntry>> query = new HierarchicalQuery<>(
					ie -> ie.getSourceEntity().getEntity().getId() == removedId, null);
			List<IdentityEntry> fetched = dataProvider.fetch(query).collect(Collectors.toList());
			for (IdentityEntry ie : fetched)
			{
				if (selected.contains(ie))
				{
					deselect(ie);
				}
				treeData.removeItem(ie);
			}
		} else
		{
			HierarchicalQuery<IdentityEntry, SerializablePredicate<IdentityEntry>> query = new HierarchicalQuery<>(
					ie -> ie.getSourceEntity().getEntity().getId() == removedId, null);
			List<IdentityEntry> fetched = dataProvider.fetch(query).collect(Collectors.toList());
			// should be only one entry - parent node
			for (IdentityEntry ie : fetched)
			{
				if (selected.contains(ie))
				{
					deselect(ie);
				}
				treeData.removeItem(ie);
				
			}
		}
		dataProvider.refreshAll();
	}
	
	public void expandParent(IdentityEntry entry)
	{
		if (dataProvider.getTreeData().getParent(entry) == null)
		{
			expand(entry);
		}else
		{
			expand(dataProvider.getTreeData().getParent(entry));
		}
	}
	
	public void selectionChanged(Set<IdentityEntry> selectedItems)
	{
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
			if (lastSelected != null && selected.getSourceEntity().getEntity()
					.equals(lastSelected.getSourceEntity().getEntity()))
				return;
			lastSelected = selected;
			bus.fireEvent(new EntityChangedEvent(selected.getSourceEntity(), group));
		}
	}

	private void savePreferences()
	{
		IdentitiesTablePreferences preferences = new IdentitiesTablePreferences();
		List<String> columns = getColumnIds();
		columns.remove(IdentitiesGridColumnConstans.ACTION_COLUMN_ID);

		for (String column : columns)
		{
			IdentitiesTablePreferences.ColumnSettings settings = new IdentitiesTablePreferences.ColumnSettings();
			settings.setCollapsed(getColumn(column).isHidden());
			settings.setWidth(getColumn(column).getWidth());

			Iterator<Column<IdentityEntry, ?>> iterator = getColumns().iterator();
			int i = 0;
			while (iterator.hasNext())
			{
				if (iterator.next().getId().equals(column))
				{
					settings.setOrder(i);
					break;
				}
				i++;
			}

			preferences.addColumneSettings(column, settings);
		}

		preferences.setGroupByEntitiesSetting(groupByEntity);
		preferences.setShowTargetedSetting(showTargeted);
		try
		{
			preferences.savePreferences(preferencesMan);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("Identities.cannotSavePrefernces"));
			return;

		}

	}

	private void loadPreferences()
	{
		IdentitiesTablePreferences preferences = null;
		try
		{
			preferences = IdentitiesTablePreferences.getPreferences(preferencesMan);
		} catch (EngineException e)
		{
			log.warn("Can not load preferences for identities table", e);
			return;
		}
		groupByEntity = preferences.getGroupByEntitiesSetting();
		showTargeted = preferences.getShowTargetedSetting();

		Set<String> columns = new HashSet<>();
		columns.addAll(getColumnIds());

		if (preferences != null && preferences.getColumnSettings().size() > 0)
		{
			Map<String, Integer> columnsOrder = new HashMap<>();

			for (Map.Entry<String, IdentitiesTablePreferences.ColumnSettings> entry : preferences
					.getColumnSettings().entrySet())
			{
				if (!columns.contains(entry.getKey().toString()))
				{
					if (entry.getKey()
							.startsWith(IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX))
					{
						addAttributeColumn(entry.getKey().substring(
								IdentitiesGridColumnConstans.ATTR_ROOT_COL_PREFIX
										.length()),
								"/");
					} else if (entry.getKey().startsWith(
							IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX))
					{
						addAttributeColumn(entry.getKey().substring(
								IdentitiesGridColumnConstans.ATTR_CURRENT_COL_PREFIX
										.length()),
								null);
					} else
					{
						continue;
					}

					getColumn(entry.getKey()).setHidden(entry.getValue().isCollapsed());
					if (entry.getValue().getWidth() > 0)
						getColumn(entry.getKey()).setWidth(entry.getValue().getWidth());

				} else
				{
					if (!entry.getKey().equals(BaseColumn.entity.toString()))
					{
						getColumn(entry.getKey()).setHidden(entry.getValue().isCollapsed());
					}

					if (entry.getValue().getWidth() > 0)
						getColumn(entry.getKey()).setWidth(entry.getValue().getWidth());
				}

				columnsOrder.put(entry.getKey(), entry.getValue().getOrder());
			}

			// all which are not in prefs land at the end. Important
			// for prefs from older version.
			List<String> orderedColumns = new ArrayList<>(columns);
			orderedColumns.sort((a, b) -> Integer.compare(columnsOrder.getOrDefault(a, Integer.MAX_VALUE),
					columnsOrder.getOrDefault(b, Integer.MAX_VALUE)));
			setColumnOrder(orderedColumns.toArray(new String[orderedColumns.size()]));
		}
	}

}
