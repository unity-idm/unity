/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.google.common.collect.Sets;
import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import io.imunity.console.views.directory_browser.GridSelectionSupport;
import io.imunity.console.views.directory_browser.identities.IdentityTreeGridDragItems;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.*;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.FOLDER_OPEN_O;
import static com.vaadin.flow.component.icon.VaadinIcon.TAGS;
import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;

@PrototypeComponent
public class GroupsTreeGrid extends TreeGrid<TreeNode>
{
	private final MessageSource msg;
	private final EventsBus bus;
	private final TreeDataProvider<TreeNode> dataProvider;
	private final Toolbar<TreeNode> toolbar;
	private final GroupBrowserController controller;
	private final boolean authzError;
	private TreeData<TreeNode> treeData;
	private boolean multiselectHasClicked;

	GroupsTreeGrid(MessageSource msg, GroupBrowserController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.authzError = false;

		GridSelectionSupport.installClickListener(this);
		addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

		SearchField search = new SearchField(msg.getMessage("search"), event -> {
			deselectAll();
			this.clearFilters();
			if (event == null || event.isEmpty())
			{
				return;
			}
			this.addFilter(e -> e.anyFieldsOrChildContains(event, treeData));
		});
		search.setWidth(10, Unit.EM);

		toolbar = new Toolbar<>();
		ActionMenuWithHandlerSupport<TreeNode> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		addSelectionListeners(hamburgerMenu);

		SingleActionHandler<TreeNode> expandAllAction = getExpandAllAction();
		SingleActionHandler<TreeNode> collapseAllAction = getCollapseAllAction();
		SingleActionHandler<TreeNode> deleteAction = getDeleteAction();
		SingleActionHandler<TreeNode> refreshAction = getRefreshAction();

		hamburgerMenu.addActionHandler(expandAllAction);
		hamburgerMenu.addActionHandler(collapseAllAction);
		hamburgerMenu.addActionHandler(refreshAction);
		hamburgerMenu.addActionHandler(deleteAction);

		toolbar.addHamburger(hamburgerMenu, FlexComponent.Alignment.END);
		ToggleButton toggle = new ToggleButton(msg.getMessage("GroupDetails.multiselect"));
		toggle.addValueChangeListener(event ->
		{
			multiselectHasClicked = true;
			if(event.getValue())
				setSelectionMode(SelectionMode.MULTI);
			else
				setSelectionMode(SelectionMode.SINGLE);
			getDataProvider().refreshAll();
		});
		VerticalLayout searchLayout = new VerticalLayout(toggle, search);
		searchLayout.setPadding(false);
		searchLayout.setSpacing(false);
		searchLayout.setAlignItems(FlexComponent.Alignment.END);
		toolbar.add(searchLayout);
		toolbar.setWidth(100, Unit.PERCENTAGE);

		this.bus = WebSession.getCurrent().getEventBus();

		treeData = new TreeData<>();
		dataProvider = new TreeDataProvider<>(treeData);
		setDataProvider(dataProvider);
		dataProvider.setSortComparator((g1, g2) -> g1.toString().compareTo(g2.toString()));

		addComponentHierarchyColumn(n ->
		{
			Div div = new Div(getIcon(n), new Span(" " + n.toString()));
			div.getElement().setAttribute("onclick", "event.stopPropagation();");
			div.addSingleClickListener(event -> select(n));
			return div;
		})
				.setFrozen(true)
				.setAutoWidth(true);
		addComponentColumn(this::getRowHamburgerMenuComponent)
				.setAutoWidth(true)
				.setFlexGrow(0);

		setSizeFull();
		ComponentUtil.setData(UI.getCurrent(), GroupsTreeGrid.class, this);
		setupDropListener();

		loadNode("/", null);
		expand(treeData.getRootItems());
		if (!treeData.getRootItems().isEmpty())
		{
			select(treeData.getRootItems().get(0));
		}
	}

	private void addSelectionListeners(ActionMenuWithHandlerSupport<TreeNode> hamburgerMenu)
	{
		setSelectionMode(SelectionMode.MULTI);
		addSelectionListener(event -> selectionChanged());
		addSelectionListener(e -> toolbar.getSelectionListener().accept(e.getAllSelectedItems()));
		addSelectionListener(hamburgerMenu.getSelectionListener());

		setSelectionMode(SelectionMode.SINGLE);
		addSelectionListener(event -> selectionChanged());
		addSelectionListener(e -> toolbar.getSelectionListener().accept(e.getAllSelectedItems()));
		addSelectionListener(hamburgerMenu.getSelectionListener());
	}

	Icon getIcon(TreeNode node)
	{
		return authzError ? VaadinIcon.LOCK.create()
				: node.isDelegated() ? VaadinIcon.WORKPLACE.create()
						: isExpanded(node) ? VaadinIcon.FOLDER_OPEN.create()
								: VaadinIcon.FOLDER.create();

	}
	
	private Component getRowHamburgerMenuComponent(TreeNode node)
	{
		ActionMenuWithHandlerSupport<TreeNode> menu = new ActionMenuWithHandlerSupport<>();
		menu.setTarget(Sets.newHashSet(node));
		
		SingleActionHandler<TreeNode> addAction = getAddAction();
		menu.addActionHandler(addAction);
		SingleActionHandler<TreeNode> editAction = getEditAction();
		SingleActionHandler<TreeNode> editACAction = getEditACsAction();
		SingleActionHandler<TreeNode> editDelegationConfigAction = getEditDelegationConfigAction();

		SingleActionHandler<TreeNode> expandAllAction = getExpandAction();
		MenuItem expandItem = menu.addActionHandler(expandAllAction);
		SingleActionHandler<TreeNode> collapseAllAction = getCollapseAction();
		MenuItem collapseItem = menu.addActionHandler(collapseAllAction);
		configExpandCollapseNode(node, expandItem, collapseItem);

		SingleActionHandler<TreeNode> deleteAction = getDeleteAction();
		
		menu.addActionHandlers(Arrays.asList(deleteAction, editAction, editACAction, editDelegationConfigAction));
		
		Component target = menu.getTarget();
		target.setVisible(node.equals(getSingleSelection()));
		addSelectionListener(
				event ->  target.setVisible(menuVisibleOnSelection(event.getAllSelectedItems(), node))
		);

		Div div = new Div(target);
		if(multiselectHasClicked) //fixed vaadin bug
			div.getStyle().set("padding-right", BASE_MARGIN.value());
		return div;
	}

	private void configExpandCollapseNode(TreeNode node, MenuItem expandItem, MenuItem collapseItem)
	{
		expandItem.addClickListener(event -> {
			expandItem.setEnabled(false);
			collapseItem.setEnabled(true);
		});
		collapseItem.addClickListener(event -> {
			expandItem.setEnabled(true);
			collapseItem.setEnabled(false);
		});
		addCollapseListener(event -> event.getItems().stream()
				.filter(item -> item.equals(node))
				.findAny().ifPresent(item -> {
					expandItem.setEnabled(true);
					collapseItem.setEnabled(false);
				}));

		addExpandListener(event -> event.getItems().stream()
				.filter(item -> item.equals(node))
				.findAny().ifPresent(item -> {
					expandItem.setEnabled(false);
					collapseItem.setEnabled(true);
				}));
	}

	private static boolean menuVisibleOnSelection(Set<TreeNode> selectedItems, TreeNode target)
	{
		return selectedItems.size() == 1 && selectedItems.contains(target);
	}
	
	private void setupDropListener()
	{
		addDropListener(e -> {
			IdentityTreeGridDragItems groupsTreeGrid = ComponentUtil.getData(UI.getCurrent(), IdentityTreeGridDragItems.class);
			controller.bulkAddToGroup(e.getDropTargetItem().get(), groupsTreeGrid.entityWithLabels);
		});
	}

	private void selectionChanged()
	{
		final TreeNode node = getSingleSelection();
		bus.fireEvent(new GroupChangedEvent(node == null ? null : node.getGroup()));
	}

	Toolbar<TreeNode> getToolbar()
	{
		return toolbar;
	}

	void refreshAndEnsureSelection()
	{
		for (TreeNode rootItem : treeData.getRootItems())
			refreshNode(rootItem);
		if (!treeData.getRootItems().isEmpty() && getSelectedItems().isEmpty())
			select(treeData.getRootItems().get(0));
	}

	
	public void refresh()
	{
		for (TreeNode rootItem : treeData.getRootItems())
			refreshNode(rootItem);
	}

	private void refreshNode(TreeNode node)
	{
		treeData.removeItem(node);
		loadNode(node.getGroup().getPathEncoded(), node.getParentNode());
		getDataProvider().refreshAll();
	}
	
	private void loadNode(String path, TreeNode parent)
	{
		Map<String, List<Group>> groupTree;

		groupTree = controller.getAllGroupWithSubgroups(path);

		List<Group> rootGrs = groupTree.get(null);
		if (rootGrs == null)
			return;
		for (Group rootGr : rootGrs)
		{
			TreeNode rootNode = new TreeNode(msg, rootGr, parent);
			treeData.addItem(parent, rootNode);
			addChildren(rootNode, groupTree);
		}
	}

	private void addChildren(TreeNode parentNode, Map<String, List<Group>> groupTree)
	{
		for (Group child : groupTree.get(parentNode.getGroup().getPathEncoded()))
		{
			TreeNode childNode = new TreeNode(msg, child, parentNode);
			treeData.addItem(parentNode, childNode);
			addChildren(childNode, groupTree);
		}
	}

	private TreeNode getSingleSelection()
	{	
		if (getSelectedItems() != null && getSelectedItems().size() == 1)
		{
			return getSelectedItems().iterator().next();
		} else
		{
			return null;
		}
	}

	private SingleActionHandler<TreeNode> getAddAction()
	{
		return SingleActionHandler.builder(TreeNode.class).withCaption(msg.getMessage("GroupsTree.createGroup"))
				.withIcon(VaadinIcon.PLUS_CIRCLE_O).withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Collection<TreeNode> target)
	{
		final TreeNode node = target.iterator().next();
		new GroupAddDialog(msg, node.getGroup(), g -> {
			createGroup(g);
			refreshNode(node);
			expand(node);
		}).open();
	}

	private void createGroup(Group toBeCreated)
	{
		controller.addGroup(toBeCreated);
	}

	private SingleActionHandler<TreeNode> getEditACsAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupDetails.editACAction"))
				.withIcon(TAGS).withHandler(this::showEditACsDialog).build();
	}

	private void showEditACsDialog(Collection<TreeNode> target)
	{
		final TreeNode node = target.iterator().next();
		controller.getGroupAttributesClassesDialog(node.getGroup(), bus).open();
	}

	private SingleActionHandler<TreeNode> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg::getMessage, TreeNode.class).withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<TreeNode> target)
	{
		TreeNode node = target.iterator().next();
		Group group;
		group = controller.getFreshGroup(node.getGroup().getPathEncoded());

		new GroupEditDialog(msg, group, g -> {
			updateGroup(node.getGroup().getPathEncoded(), g);
			if (node.getParentNode() != null)
				refreshNode(node.getParentNode());
			else
				refresh();
			if (node.equals(getSingleSelection()))
				bus.fireEvent(new GroupChangedEvent(node.getGroup()));
		}).open();

	}

	private SingleActionHandler<TreeNode> getEditDelegationConfigAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.editDelegationConfigAction"))
				.withIcon(VaadinIcon.FORWARD)
				.withHandler(this::showEditDelegationConfigDialog).build();
	}

	private void showEditDelegationConfigDialog(Collection<TreeNode> target)
	{
		TreeNode node = target.iterator().next();
		Group group = node.getGroup();

		controller.getGroupDelegationEditConfigDialog(bus, group, g -> {
			updateGroup(node.getGroup().getPathEncoded(), g);
			node.setGroupMetadata(g);
			dataProvider.refreshItem(node);
		}).open();
	}

	private SingleActionHandler<TreeNode> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg::getMessage, TreeNode.class).withHandler(n -> refresh()).build();
	}
	
	private SingleActionHandler<TreeNode> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg::getMessage, TreeNode.class).withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<TreeNode> items)
	{
		Set<TreeNode> realToRemove = getParentOnly(items);
		Checkbox checkbox = new Checkbox(msg.getMessage("GroupRemovalDialog.recursive"));
		ConfirmDialog confirmDialog = new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				"",
				msg.getMessage("ok"),
				e -> removeGroups(realToRemove, checkbox.getValue()),
				msg.getMessage("cancel"),
				e -> {}
		);
		confirmDialog.add(new Span(msg.getMessage("GroupRemovalDialog.confirmDelete",
				realToRemove.stream().map(TreeNode::toString)
						.collect(Collectors.joining(", ")))), checkbox);
		confirmDialog.setWidth("30em");
		confirmDialog.open();
	}

	private void removeGroups(Set<TreeNode> groups, boolean recursive)
	{
		List<TreeNode> removed = controller.removeGroups(groups, recursive);
		if(!removed.isEmpty())
			deselectAll();
		removed.forEach(group -> refreshNode(group.getParentNode()));
	}

	private SingleActionHandler<TreeNode> getExpandAllAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.expandAllGroupsAction")).dontRequireTarget()
				.withIcon(FOLDER_OPEN_O).withHandler(g -> expandRecursively(!g.isEmpty() ? g : treeData.getRootItems(),
						Integer.MAX_VALUE)).build();
	}

	private SingleActionHandler<TreeNode> getExpandAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.expandGroupAction")).dontRequireTarget()
				.withIcon(FOLDER_OPEN_O).withHandler(g -> expandRecursively(g, Integer.MAX_VALUE)).withDisabledPredicate(this::isExpanded).build();
	}

	private SingleActionHandler<TreeNode> getCollapseAllAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.collapseAllGroupsAction")).dontRequireTarget()
				.withIcon(VaadinIcon.FOLDER_O).withHandler(g -> {
					collapseRecursively(!g.isEmpty() ? g : treeData.getRootItems(),
							Integer.MAX_VALUE);
					if (g.isEmpty())
					{
						expandRecursively(treeData.getRootItems(), 0);
					}
				}).build();
	}

	private SingleActionHandler<TreeNode> getCollapseAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.collapseGroupAction")).dontRequireTarget()
				.withIcon(VaadinIcon.FOLDER_O).withHandler(g -> collapseRecursively(g, Integer.MAX_VALUE)).withDisabledPredicate(n -> !isExpanded(n)).build();
	}
	
	private void updateGroup(String path, Group group)
	{
		controller.updateGroup(path, group);
	}

	void addFilter(SerializablePredicate<TreeNode> filter)
	{
		dataProvider.addFilter(filter);

	}

	public void clearFilters()
	{
		dataProvider.clearFilters();
	}

	private Set<TreeNode> getParentOnly(Collection<TreeNode> items)
	{
		Set<TreeNode> parents = new HashSet<>();
		for (TreeNode node : items)
		{
			boolean child = false;
			for (TreeNode potentialParent : items)
			{
				if (node.isChild(potentialParent))
				{
					child = true;
					break;
				}
			}

			if (!child)
			{
				parents.add(node);
			}
		}
		return parents;
	}
}
