/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_browser.GridSelectionSupport;
import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.console.views.directory_browser.group_browser.GroupsTreeGrid;
import io.imunity.console.views.directory_browser.identities.credentials.CredentialsChangeDialog;
import io.imunity.vaadin.elements.ColumnToggleMenu;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.CachedAttributeHandlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.ObjectFactory;
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

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;


@PrototypeComponent
public class IdentitiesTreeGrid extends TreeGrid<IdentityEntry>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesTreeGrid.class);

	private final AttributeSupport attributeSupport;
	private final CredentialManagement credentialManagement;
	private final IdentityTypeSupport idTypeSupport;
	private final MessageSource msg;
	private final EntitiesLoader entitiesLoader;
	private final AttributeHandlerRegistry attrHandlerRegistry;

	private final List<ResolvedEntity> cachedEntitites;
	private final TreeData<IdentityEntry> treeData;
	private final TreeDataProvider<IdentityEntry> dataProvider;
	private final List<EntityFilter> filters;
	private final EventsBus bus;
	private final PreferencesManagement preferencesMan;
	private final ColumnToggleMenu columnToggleMenu;

	private final EntityDetailsHandler entityDetailsHandler;
	private final AddToGroupHandler addToGroupHandler;
	private final RemoveFromGroupHandler removeFromGroupHandler;
	private final IdentityCreationDialog.IdentityCreationDialogHandler identityCreationDialogHanlder;
	private final ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory;
	private final ChangeEntityStateHandler changeEntityStateHandler;
	private final ChangeCredentialRequirementHandler credentialRequirementHandler;
	private final EntityAttributeClassHandler entityAttributeClassHandler;
	private final IdentityConfirmationResendHandler confirmationResendHandler;
	private final IdentityConfirmHandler confirmHandler;
	private final DeleteIdentityHandler deleteIdentityHandler;
	private final DeleteEntityHandler deleteEntityHandler;
	private final NotificationPresenter notificationPresenter;

	private boolean groupByEntity;
	private boolean showTargeted;
	private Group group;
	private String entityNameAttribute = null;
	private Map<String, IdentityTypeDefinition> typeDefinitionsMap;
	private Map<String, CredentialDefinition> credentialDefinitions;
	private IdentityEntry lastSelected;
	private Column<IdentityEntry> actionColumn;

	IdentitiesTreeGrid(MessageSource msg, AttributeSupport attributeSupport,
	                          IdentityTypeSupport idTypeSupport, EntitiesLoader entitiesLoader,
	                          AttributeHandlerRegistry attrHandlerRegistry, PreferencesManagement preferencesMan,
	                          CredentialManagement credentialManagement, EntityDetailsHandler entityDetailsHandler,
	                          AddToGroupHandler addToGroupHandler, RemoveFromGroupHandler removeFromGroupHandler,
	                          IdentityCreationDialog.IdentityCreationDialogHandler identityCreationDialogHanlder,
	                          ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory,
	                          ChangeEntityStateHandler changeEntityStateHandler,
	                          ChangeCredentialRequirementHandler credentialRequirementHandler,
	                          EntityAttributeClassHandler entityAttributeClassHandler,
	                          IdentityConfirmationResendHandler confirmationResendHandler,
	                          IdentityConfirmHandler confirmHandler, DeleteIdentityHandler deleteIdentityHandler,
	                          DeleteEntityHandler deleteEntityHandler, NotificationPresenter notificationPresenter)

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
		this.notificationPresenter = notificationPresenter;
		this.columnToggleMenu = new ColumnToggleMenu(this::savePreferences);
		this.cachedEntitites = new ArrayList<>(200);
		this.dataProvider = (TreeDataProvider<IdentityEntry>) getDataProvider();
		this.treeData = dataProvider.getTreeData();
		this.filters = new ArrayList<>();
		this.bus = WebSession.getCurrent().getEventBus();

		addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
		addClassName("u-directory-browser-members-grid");
		createBaseColumns();
		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);
		addSelectionListener(event -> selectionChanged(event.getAllSelectedItems()));
		setSizeFull();
		addColumnResizeListener(event -> savePreferences());
		addColumnReorderListener(event ->
		{
			if(event.isFromClient())
				setColumnOrder(event.getColumns());
			else
				savePreferences();
		});
		setColumnReorderingAllowed(true);
		updateCredentialStatusColumns();
		loadPreferences();
		setupDragAndDrop();
		refreshActionColumn();
	}

	private void setupDragAndDrop()
	{
		setRowsDraggable(true);
		UI ui = UI.getCurrent();
		addDragStartListener(event ->
		{
			ComponentUtil.getData(ui, GroupsTreeGrid.class).setDropMode(GridDropMode.BETWEEN);
			Set<EntityWithLabel> entityWithLabels = event.getDraggedItems().stream().map(IdentityEntry::getSourceEntity)
					.collect(Collectors.toSet());
			ComponentUtil.setData(UI.getCurrent(), IdentityTreeGridDragItems.class, new IdentityTreeGridDragItems(entityWithLabels));
		});
		addDragEndListener(event -> ComponentUtil.getData(ui, GroupsTreeGrid.class).setDropMode(null));
	}

	private void createBaseColumns()
	{
		addComponentHierarchyColumn(ie ->
		{
			Div div = new Div(new Span(ie.getBaseValue(BaseColumn.entity)));
			div.getElement().setAttribute("onclick", "event.stopPropagation();");
			div.addSingleClickListener(event -> select(ie));
			return div;
		})
				.setHeader(msg.getMessage(BaseColumn.entity.captionKey))
				.setWidth(BaseColumn.entity.defWidth + "px")
				.setResizable(true)
				.setKey(BaseColumn.entity.name());
		for (BaseColumn column : BaseColumn.values())
		{
			if(BaseColumn.entity == column)
				continue;
			Column<IdentityEntry> baseColumn = addColumn(ie -> ie.getBaseValue(column))
					.setHeader(msg.getMessage(column.captionKey))
					.setWidth(column.defWidth + "px")
					.setResizable(true)
					.setSortable(true)
					.setKey(column.name());
			baseColumn.setVisible(!column.initiallyCollapsed);
			columnToggleMenu.addColumn(msg.getMessage(column.captionKey), baseColumn);
		}
	}

	private void refreshActionColumn()
	{
		if (actionColumn != null)
			removeColumn(actionColumn);
		actionColumn = addComponentColumn(n -> getRowHamburgerMenuComponent(Sets.newHashSet(n)))
				.setHeader(getActions())
				.setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.END)
				.setKey(IdentitiesGridColumnConstants.ACTION_COLUMN_ID);
	}

	private HorizontalLayout getActions()
	{
		Span actions = new Span(msg.getMessage("actions"));
		HorizontalLayout horizontalLayout = new HorizontalLayout(actions, columnToggleMenu.getTarget());
		horizontalLayout.setSpacing(false);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		actions.getStyle().set("margin-right", SMALL_MARGIN.value());
		return horizontalLayout;
	}

	private Component getRowHamburgerMenuComponent(Set<IdentityEntry> target)
	{

		ActionMenuWithHandlerSupport<IdentityEntry> menu = new ActionMenuWithHandlerSupport<>();
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

		return menu.getTarget();
	}

	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(getGroup(), false));
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
			entitiesLoader.reload(selected, group.getPathEncoded(), showTargeted,
					(entities, selected1, selected12) -> addAndCacheResolvedEntities(entities, selected1));
	}

	private void reloadTableContentsFromData()
	{
		Set<IdentityEntry> selected = getSelectedItems();
		treeData.clear();
		dataProvider.refreshAll();
		addResolvedEntities(cachedEntitites, selected);
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

	private void addAndCacheResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected)
	{
		cachedEntitites.addAll(entities);
		addResolvedEntities(entities, selected);
	}

	private void addResolvedEntities(List<ResolvedEntity> entities, Set<IdentityEntry> selected)
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
		setDataProvider(dataProvider);
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
			label = rootAttributes.get(entityNameAttribute).getValues().get(0) + " ";
		EntityWithLabel entWithLabel = new EntityWithLabel(ent, label);

		Map<String, String> attributesByColumnId = new HashMap<>();
		List<Column<IdentityEntry>> columns = getColumns();
		for (Column<IdentityEntry> column : columns)
		{
			String columnId = column.getKey();
			if (columnId == null || !columnId.startsWith(IdentitiesGridColumnConstants.ATTR_COL_PREFIX))
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
					.collect(Collectors.toMap(CredentialDefinition::getName, cd -> cd));
		} catch (EngineException e)
		{
			throw new InternalException("Can not load credentials", e);
		}
		for (Map.Entry<String, CredentialDefinition> cd : credentialDefinitions.entrySet())
		{
			String colKey = IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX + cd.getKey();
			if (getColumnByKey(colKey) == null)
			{
				Column<IdentityEntry> entryColumn = addColumn(ie -> ie.getCredentialStatus(cd.getKey()))
						.setHeader(cd.getValue().getName())
						.setSortable(true)
						.setResizable(true)
						.setWidth(IdentitiesGridColumnConstants.ATTR_COL_RATIO + "px")
						.setKey(colKey);
				entryColumn.setVisible(false);
				columnToggleMenu.addColumn(cd.getValue().getName(), entryColumn);
			}
		}

		getColumnIds().stream()
				.filter(colId -> colId.startsWith(IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX))
				.map(colId -> colId.substring(
						IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX.length()))
				.filter(credId -> !credentialDefinitions.containsKey(credId))
				.forEach(credId -> removeColumnByKey(
						IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX + credId));
	}

	void addAttributeColumn(String attribute, String group)
	{
		String key = (group == null) ? IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX + attribute
				: IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX + attribute;
		if (getColumnByKey(key) != null)
		{
			notificationPresenter.showError(msg.getMessage("Identities.customColumnExists"), "");
			return;
		}

		Column<IdentityEntry> entryColumn = addColumn(ie -> ie.getAttribute(key))
				.setHeader(attribute + (group == null ? "@" + this.group : "@/"))
				.setWidth(IdentitiesGridColumnConstants.ATTR_COL_RATIO + "px")
				.setResizable(true)
				.setSortable(true)
				.setKey(key);
		columnToggleMenu.addColumn(entryColumn.getHeaderText(), entryColumn);
		refreshActionColumn();

		savePreferences();
		try
		{
			showGroup(this.group);
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.internalError", e.getMessage()), e.getMessage());
		}
	}

	void removeAttributeColumn(String group, String attribute)
	{
		if (Strings.isEmpty(group))
			removeColumnByKey(IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX + attribute);
		else if (group.equals(this.group.getPathEncoded()))
			removeColumnByKey(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX + attribute);
		reloadTableContentsFromData();
		savePreferences();
	}

	Set<String> getAttributeColumns(boolean root)
	{
		List<Column<IdentityEntry>> columns = getColumns();
		Set<String> ret = new HashSet<>();
		for (Column<IdentityEntry> column : columns)
		{
			String property = column.getKey();
			if (root)
			{
				if (property.startsWith(IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX))
					ret.add(property.substring(
							IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX.length()));
			} else
			{
				if (property.startsWith(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX))
					ret.add(property.substring(
							IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX.length()));
			}
		}
		return ret;
	}

	private void updateAttributeColumnHeaders()
	{
		List<Column<IdentityEntry>> columns = getColumns();
		for (Column<IdentityEntry> column : columns)
		{
			String property = column.getKey();
			if (property.startsWith(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX))
			{
				String attrName = property.substring(
						IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX.length());
				column.setHeader(attrName + "@" + this.group);
			}
		}
	}

	private Attribute getAttributeForColumnProperty(String propId, Map<String, ? extends Attribute> rootAttributes,
			Map<String, ? extends Attribute> curAttributes)
	{
		if (propId.startsWith(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX))
		{
			String attributeName = propId
					.substring(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX.length());
			return curAttributes.get(attributeName);
		} else
		{
			String attributeName = propId
					.substring(IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX.length());
			return rootAttributes.get(attributeName);
		}
	}

	private SingleActionHandler<IdentityEntry> getChangeCredentialAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeCredentialAction"))
				.withIcon(VaadinIcon.KEY).withHandler(this::showChangeCredentialDialog)
				.build();
	}

	private void showChangeCredentialDialog(Set<IdentityEntry> selection)
	{
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		credentialChangeDialogFactory.getObject().init(entity.getEntity().getId(), false, changed -> {
			if (changed)
				refresh();
		}).open();
	}

	void addFilter(EntityFilter filter)
	{
		dataProvider.addFilter(filter);
		filters.add(filter);
	}

	void removeFilter(EntityFilter filter)
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
		return getColumns().stream().map(Column::getKey).collect(Collectors.toList());
	}

	public Set<String> getVisibleColumnIds()
	{
		return getColumns().stream().filter(Component::isVisible)
				.map(Column::getKey)
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
			if (Objects.equals(cached.getEntity().getId(), entry.getSourceEntity().getEntity().getId()))
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
			List<IdentityEntry> fetched = dataProvider.fetch(query).toList();
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
			List<IdentityEntry> fetched = dataProvider.fetch(query).toList();
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
		if(selectedItems.isEmpty())
		{
			lastSelected = null;
			bus.fireEvent(new EntityChangedEvent(null, group, false));
			return;
		}
		if (selectedItems.size() == 1)
		{
			selected = selectedItems.iterator().next();
		}
		if (selectedItems.size() > 1)
		{
			lastSelected = null;
			bus.fireEvent(new EntityChangedEvent(null, group, true));
		} else if (selected.getSourceIdentity() == null)
		{
			if (selected.equals(lastSelected))
				return;
			lastSelected = selected;
			bus.fireEvent(new EntityChangedEvent(selected.getSourceEntity(), group, false));
		} else
		{
			if (lastSelected != null && selected.getSourceEntity().getEntity()
					.equals(lastSelected.getSourceEntity().getEntity()))
				return;
			lastSelected = selected;
			bus.fireEvent(new EntityChangedEvent(selected.getSourceEntity(), group, false));
		}
	}

	private void savePreferences()
	{
		IdentitiesTablePreferences preferences = new IdentitiesTablePreferences();
		List<String> columns = getColumnIds();
		columns.remove(IdentitiesGridColumnConstants.ACTION_COLUMN_ID);

		for (String column : columns)
		{
			IdentitiesTablePreferences.ColumnSettings settings = new IdentitiesTablePreferences.ColumnSettings();
			settings.setCollapsed(!getColumnByKey(column).isVisible());
			String width = getColumnByKey(column).getWidth();
			if(width != null)
				settings.setWidth(width);
			settings.setOrder(columns.indexOf(column));
			preferences.addColumnSettings(column, settings);
		}

		preferences.setGroupByEntitiesSetting(groupByEntity);
		preferences.setShowTargetedSetting(showTargeted);
		try
		{
			preferences.savePreferences(preferencesMan);
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("Identities.cannotSavePrefernces"));
		}

	}

	private void loadPreferences()
	{
		IdentitiesTablePreferences preferences;
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

		Set<String> columns = getColumns().stream()
				.map(Column::getKey)
				.collect(Collectors.toSet());

		if (!preferences.getColumnSettings().isEmpty())
		{
			Map<String, Integer> columnsOrder = new HashMap<>();

			for (Map.Entry<String, IdentitiesTablePreferences.ColumnSettings> entry : preferences
					.getColumnSettings().entrySet())
			{
				if (!columns.contains(entry.getKey()))
				{
					if (entry.getKey().startsWith(IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX))
						addAttributeColumn(entry.getKey().substring(
								IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX.length()), "/");
					else if (entry.getKey().startsWith(IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX))
						addAttributeColumn(entry.getKey().substring(
								IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX.length()), null);
					else
						continue;

					handleColumnVisibility(entry);
				}
				else
					if (!entry.getKey().equals(BaseColumn.entity.toString()))
						handleColumnVisibility(entry);

				if (!StringUtils.isBlank(entry.getValue().getWidth()))
					getColumnByKey(entry.getKey()).setWidth(entry.getValue().getWidth());

				columnsOrder.put(entry.getKey(), entry.getValue().getOrder());
			}

			List<Column<IdentityEntry>> orderedColumns = new ArrayList<>(getColumns());
			orderedColumns.sort(Comparator.comparingInt(a -> columnsOrder.getOrDefault(a.getKey(), Integer.MAX_VALUE)));
			setColumnOrder(orderedColumns);
		}
	}

	private void handleColumnVisibility(Map.Entry<String, IdentitiesTablePreferences.ColumnSettings> entry)
	{
		Column<IdentityEntry> column = getColumnByKey(entry.getKey());
		column.setVisible(!entry.getValue().isCollapsed());
		columnToggleMenu.setChecked(column, !entry.getValue().isCollapsed());
	}
}
