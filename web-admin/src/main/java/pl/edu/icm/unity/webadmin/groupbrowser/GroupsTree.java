/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.components.grid.TreeGridDropTarget;
import com.vaadin.ui.renderers.HtmlRenderer;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.groupdetails.GroupAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.identities.EntityCreationHandler;
import pl.edu.icm.unity.webadmin.identities.IdentitiesGrid;
import pl.edu.icm.unity.webadmin.reg.formman.EnquiryFormEditor;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormEditor;
import pl.edu.icm.unity.webadmin.utils.GroupManagementHelper;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.DnDGridUtils;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Tree with groups obtained dynamically from the engine.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupsTree extends TreeGrid<TreeNode>
{
	private GroupsManagement groupsMan;
	private EntityManagement identitiesMan;
	private UnityMessageSource msg;
	private EventsBus bus;
	private AttributeClassManagement acMan;
	private TreeData<TreeNode> treeData;
	private GridContextMenuSupport<TreeNode> contextMenuSupp;
	private EntityCreationHandler entityCreationDialogHandler;
	private Toolbar<TreeNode> toolbar;
	private BulkGroupQueryService bulkQueryService;
	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;
	private AttributeTypeManagement attrTypeMan;	
	private ObjectFactory<RegistrationFormEditor> regFormEditorFactory;
	private ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory;
	private GroupDelegationConfigGenerator delConfigUtils;

	@Autowired
	public GroupsTree(GroupsManagement groupsMan, EntityManagement identitiesMan,
			UnityMessageSource msg, AttributeClassManagement acMan,
			EntityCreationHandler entityCreationDialogHandler,
			GroupManagementHelper groupManagementHelper,
			BulkGroupQueryService bulkQueryService, 
			RegistrationsManagement registrationMan,
			EnquiryManagement enquiryMan,
			AttributeTypeManagement attrTypeMan, ObjectFactory<RegistrationFormEditor> regFormEditorFactory,
			ObjectFactory<EnquiryFormEditor> enquiryFormEditorFactory,
			GroupDelegationConfigGenerator delConfigGenerator)
	{
		this.groupsMan = groupsMan;
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.acMan = acMan;
		this.entityCreationDialogHandler = entityCreationDialogHandler;
		this.bulkQueryService = bulkQueryService;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.attrTypeMan = attrTypeMan;
		this.regFormEditorFactory = regFormEditorFactory;
		this.enquiryFormEditorFactory = enquiryFormEditorFactory;
		this.delConfigUtils = delConfigGenerator;

		contextMenuSupp = new GridContextMenuSupport<>(this);
		addExpandListener(new GroupExpandListener());
		addSelectionListener(e -> {
			final TreeNode node = getSelection();
			bus.fireEvent(new GroupChangedEvent(node == null ? null : node.getPath()));
		});

		SingleSelectionModel<TreeNode> singleSelect = (SingleSelectionModel<TreeNode>) getSelectionModel();

		singleSelect.setDeselectAllowed(false);

		toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		addSelectionListener(toolbar.getSelectionListener());

		HamburgerMenu<TreeNode> hamburgerMenu = new HamburgerMenu<>();
		addSelectionListener(hamburgerMenu.getSelectionListener());

		SingleActionHandler<TreeNode> refreshAction = getRefreshAction();
		addActionHandler(refreshAction);

		SingleActionHandler<TreeNode> expandAllAction = getExpandAllAction();
		addActionHandler(expandAllAction);

		SingleActionHandler<TreeNode> collapseAllAction = getCollapseAllAction();
		addActionHandler(collapseAllAction);

		SingleActionHandler<TreeNode> addAction = getAddAction();
		addActionHandler(addAction);

		SingleActionHandler<TreeNode> editAction = getEditAction();
		addActionHandler(editAction);

		SingleActionHandler<TreeNode> editACAction = getEditACsAction();
		addActionHandler(editACAction);

		SingleActionHandler<TreeNode> editDelegationConfigAction = getEditDelegationConfigAction();
		addActionHandler(editDelegationConfigAction);		
		
		SingleActionHandler<TreeNode> deleteAction = getDeleteAction();
		addActionHandler(deleteAction);

		SingleActionHandler<TreeNode> addEntityAction = getAddEntityAction();
		addActionHandler(addEntityAction);

		
		toolbar.addActionHandler(addAction);
		toolbar.addActionHandler(deleteAction);
		hamburgerMenu.addActionHandler(refreshAction);
		hamburgerMenu.addActionHandler(expandAllAction);
		hamburgerMenu.addActionHandler(collapseAllAction);
		hamburgerMenu.addActionHandler(editAction);
		hamburgerMenu.addActionHandler(editACAction);
		hamburgerMenu.addActionHandler(editDelegationConfigAction);
		hamburgerMenu.addActionHandler(addEntityAction);
		toolbar.addHamburger(hamburgerMenu);

		this.bus = WebSession.getCurrent().getEventBus();

		treeData = new TreeData<>();
		setDataProvider(new GroupsDataProvider(treeData));

		addColumn(n -> n.getIcon() + " " + n.toString(), new HtmlRenderer());
		setHeaderVisible(false);
		setPrimaryStyleName(Styles.vGroupBrowser.toString());
		setRowHeight(34);

		setSizeFull();

		setupDragNDrop(groupManagementHelper);

		try
		{
			setupRoot();
		} catch (EngineException e)
		{
			// this will show error node
			TreeNode parent = new TreeNode(msg, new Group("/"),
					Images.folder.getHtml());
			treeData.addItems(null, parent);
			getDataProvider().refreshAll();
			expand(parent);
		}

	}

	@SuppressWarnings("unchecked")
	private void setupDragNDrop(GroupManagementHelper groupManagementHelper)
	{
		TreeGridDropTarget<TreeNode> dropTarget = new TreeGridDropTarget<>(this,
				DropMode.ON_TOP);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.setDropCriteriaScript(DnDGridUtils
				.getTypedCriteriaScript(IdentitiesGrid.ENTITY_DND_TYPE));
		dropTarget.addGridDropListener(e -> {
			e.getDragSourceExtension().ifPresent(source -> {
				if (source instanceof GridDragSource
						&& e.getDropTargetRow().isPresent()
						&& source.getDragData() != null)
				{
					Set<EntityWithLabel> dragData = (Set<EntityWithLabel>) source
							.getDragData();
					groupManagementHelper.bulkAddToGroup(
							e.getDropTargetRow().get().getPath(),
							dragData, true);
				}
			});
		});
	}

	private void addActionHandler(SingleActionHandler<TreeNode> actionHandler)
	{
		contextMenuSupp.addActionHandler(actionHandler);

	}

	public List<SingleActionHandler<TreeNode>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
	}

	public Toolbar<TreeNode> getToolbar()
	{
		return toolbar;
	}

	/**
	 * We can have two cases: either we can read '/' or not. In the latter
	 * case we take groups where the logged user is the member, and we put
	 * all of them as root groups.
	 * 
	 * @throws EngineException
	 */
	private void setupRoot() throws EngineException
	{
		try
		{
			GroupContents contents = groupsMan.getContents("/",
					GroupContents.GROUPS | GroupContents.METADATA);
			TreeNode parent = new TreeNode(msg, contents.getGroup(),
					Images.folder.getHtml());
			treeData.clear();
			treeData.addItem(null, parent);
			getDataProvider().refreshAll();
			expand(parent);
		} catch (AuthorizationException e)
		{
			setupAccessibleRoots();
		}
	}

	private void setupAccessibleRoots() throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		Collection<String> groups = identitiesMan
				.getGroups(new EntityParam(ae.getEntityId())).keySet();
		List<String> accessibleGroups = new ArrayList<>(groups.size());
		for (String groupM : groups)
		{
			try
			{
				groupsMan.getContents(groupM, GroupContents.GROUPS);
			} catch (AuthorizationException e2)
			{
				continue;
			}
			accessibleGroups.add(groupM);
		}
		treeData.clear();
		for (int i = 0; i < accessibleGroups.size(); i++)
		{
			Group groupG = new Group(accessibleGroups.get(i));
			boolean parentFound = false;
			for (int j = 0; j < accessibleGroups.size(); j++)
			{
				if (i == j)
					continue;
				if (groupG.isChild(new Group(accessibleGroups.get(j))))
				{
					parentFound = true;
					break;
				}
			}
			if (!parentFound)
			{
				try
				{
					GroupContents contents = groupsMan.getContents(
							accessibleGroups.get(i),
							GroupContents.METADATA);
					TreeNode parent = new TreeNode(msg, contents.getGroup(),
							Images.folder.getHtml());
					treeData.addItem(null, parent);

				} catch (AuthorizationException e2)
				{
					continue;
				}
			}
			getDataProvider().refreshAll();
		}
	}

	public void refresh()
	{
		for (TreeNode rootItem : treeData.getRootItems())
			refreshNode(rootItem);
	}

	private void refreshNode(TreeNode node)
	{
		if (node == null)
		{
			refresh();
			return;
		}
		node.setContentsFetched(false);
		getDataProvider().refreshAll();
		collapse(node);
		expand(node);
	}

	private void removeGroup(TreeNode parent, String path, boolean recursive)
	{
		try
		{
			groupsMan.removeGroup(path, recursive);
			refreshNode(parent);
			select(parent);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.removeGroupError"), e);
		}
	}

	private void createGroup(Group toBeCreated)
	{
		try
		{
			groupsMan.addGroup(toBeCreated);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("GroupsTree.addGroupError"),
					e);
		}
	}

	private void updateGroup(String path, Group group)
	{
		try
		{
			groupsMan.updateGroup(path, group);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.updateGroupError"), e);
		}
	}

	private SingleActionHandler<TreeNode> getAddAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("add"))
				.withIcon(Images.add.getResource()).withHandler(this::showAddDialog)
				.build();
	}

	private void showAddDialog(Collection<TreeNode> target)
	{

		final TreeNode node = target.iterator().next();
		new GroupEditDialog(msg, new Group(node.getPath()), false, g -> {
			createGroup(g);
			refreshNode(node);
		}).show();
	}

	private SingleActionHandler<TreeNode> getEditACsAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupDetails.editACAction"))
				.withIcon(Images.attributes.getResource())
				.withHandler(this::showEditACsDialog).build();
	}

	private void showEditACsDialog(Collection<TreeNode> target)
	{
		final TreeNode node = target.iterator().next();
		GroupAttributesClassesDialog dialog = new GroupAttributesClassesDialog(msg,
				node.getPath(), acMan, groupsMan,
				g -> bus.fireEvent(new GroupChangedEvent(node.getPath())));
		dialog.show();

	}
	
	private Group resolveGroup(TreeNode node)
	{
		Group group = null;
		try
		{
			group = groupsMan.getContents(node.getPath(), GroupContents.METADATA)
					.getGroup();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.resolveGroupError"), e);
		}
		return group;
	}

	private SingleActionHandler<TreeNode> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, TreeNode.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<TreeNode> target)
	{
		TreeNode node = target.iterator().next();
		Group group = resolveGroup(node);
		if (group == null)
			return;

		new GroupEditDialog(msg, group, true, g -> {
			updateGroup(node.getPath(), g);
			refreshNode(node.getParentNode());
			if (node.equals(getSelection()))
				bus.fireEvent(new GroupChangedEvent(node.getPath()));
		}).show();

	}

	private SingleActionHandler<TreeNode> getEditDelegationConfigAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.editDelegationConfigAction"))
				.withIcon(Images.forward.getResource())
				.withHandler(this::showEditDelegationCondigDialog).build();
	}

	private void showEditDelegationCondigDialog(Collection<TreeNode> target)
	{
		TreeNode node = target.iterator().next();
		Group group = resolveGroup(node);
		if (group == null)
			return;

		new GroupDelegationEditConfigDialog(msg, registrationMan, enquiryMan, attrTypeMan, regFormEditorFactory,
				enquiryFormEditorFactory, bus, delConfigUtils, group, delConfig -> {
					group.setDelegationConfiguration(delConfig);
					updateGroup(node.getPath(), group);
				}).show();

	}

	private TreeNode getSelection()
	{
		try
		{
			return getSelectedItems().iterator().next();
		} catch (Exception e)
		{
			return null;
		}
	}

	private SingleActionHandler<TreeNode> getAddEntityAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.addEntityAction"))
				.withIcon(Images.addEntity.getResource())
				.withHandler(this::showAddEntityDialog).build();
	}

	private void showAddEntityDialog(Collection<TreeNode> target)
	{
		final TreeNode node = target.iterator().next();
		entityCreationDialogHandler.showAddEntityDialog(() -> node.getPath(),
				i -> onCreatedIdentity(node, i));
	}

	private void onCreatedIdentity(TreeNode node, Identity newIdentity)
	{
		if (node.equals(getSelection()))
			bus.fireEvent(new GroupChangedEvent(node.getPath()));
	}

	private SingleActionHandler<TreeNode> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, TreeNode.class)
				.withHandler(selection -> selection.forEach(this::refreshNode))
				.build();
	}

	private SingleActionHandler<TreeNode> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, TreeNode.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<TreeNode> items)
	{
		final TreeNode node = items.iterator().next();
		new ConfirmWithOptionDialog(msg,
				msg.getMessage("GroupRemovalDialog.confirmDelete", node.getPath()),
				msg.getMessage("GroupRemovalDialog.recursive"),
				r -> removeGroup(node.getParentNode(), node.getPath(), r)

		).show();
	}

	private SingleActionHandler<TreeNode> getExpandAllAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.expandGroupsAction"))
				.withIcon(Images.expand.getResource())
				.withHandler(this::expandItemsRecursively).build();
	}

	private void expandItemsRecursively(Collection<TreeNode> items)
	{
		for (TreeNode node : items)
		{
			expand(node);
			for (TreeNode child : treeData.getChildren(node))
				expandItemsRecursively(Arrays.asList(child));
		}
	}

	private SingleActionHandler<TreeNode> getCollapseAllAction()
	{
		return SingleActionHandler.builder(TreeNode.class)
				.withCaption(msg.getMessage("GroupsTree.collapseGroupsAction"))
				.withIcon(Images.collapse.getResource())
				.withHandler(this::collapseItemsRecursively).build();
	}

	private void collapseItemsRecursively(Collection<TreeNode> items)
	{
		for (TreeNode node : items)
		{
			collapse(node);
			for (TreeNode child : treeData.getChildren(node))
				collapseItemsRecursively(Arrays.asList(child));
		}
	}

	private class GroupExpandListener implements ExpandListener<TreeNode>
	{
		private void removeAllChildren(TreeNode item)
		{
			// warning - a live collection is returned
			Collection<TreeNode> children = treeData.getChildren(item);
			if (children != null)
			{
				Set<TreeNode> copied = new HashSet<>(children.size());
				copied.addAll(children);
				for (TreeNode child : copied)
				{
					collapse(child);
					treeData.removeItem(child);
				}
			}
		}

		@Override
		public void itemExpand(ExpandEvent<TreeNode> event)
		{
			TreeNode expandedNode = event.getExpandedItem();

			if (expandedNode.isContentsFetched())
				return;

			// in case of refresh
			removeAllChildren(expandedNode);

			GroupStructuralData bulkData;
			try
			{
				bulkData = bulkQueryService
						.getBulkStructuralData(expandedNode.getPath());
			} catch (Exception e)
			{
				expandedNode.setIcon(Images.noAuthzGrp.getHtml());
				expandedNode.setContentsFetched(true);
				expand(expandedNode);
				return;
			}
			Map<String, GroupContents> groupAndSubgroups = bulkQueryService
					.getGroupAndSubgroups(bulkData);

			expandedNode.setIcon(Images.folder.getHtml());
			GroupContents contents = groupAndSubgroups.get(expandedNode.getPath());
			expandedNode.setGroupMetadata(contents.getGroup());

			List<String> subgroups = contents.getSubGroups();
			Collections.sort(subgroups);
			for (String subgroup : subgroups)
			{
				GroupContents contents2 = groupAndSubgroups.get(subgroup);
				TreeNode node = new TreeNode(msg, contents2.getGroup(),
						Images.folder.getHtml(), expandedNode);
				treeData.addItem(node.getParentNode(), node);
			}

			expandedNode.setContentsFetched(true);
			getDataProvider().refreshAll();
			// we expand empty node before, we have to expand one
			// more time to reload
			expand(expandedNode);
		}
	}

	private class GroupsDataProvider extends TreeDataProvider<TreeNode>
	{
		public GroupsDataProvider(TreeData<TreeNode> treeData)
		{
			super(treeData);
		}

		@Override
		public boolean hasChildren(TreeNode item)
		{
			if (!item.isContentsFetched())
				return true;
			return super.hasChildren(item);
		}
	}
}
