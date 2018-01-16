/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.components.grid.TreeGridDropTarget;
import com.vaadin.ui.renderers.HtmlRenderer;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.groupdetails.GroupAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.identities.EntityCreationHandler;
import pl.edu.icm.unity.webadmin.utils.GroupManagementHelper;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;

/**
 * Tree with groups obtained dynamically from the engine.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupsTree extends TreeGrid<TreeNode>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupsTree.class);
	private GroupsManagement groupsMan;
	private EntityManagement identitiesMan;
	private UnityMessageSource msg;
	private GroupManagementHelper groupManagementHelper;
	private EventsBus bus;
	private AttributeClassManagement acMan;
	private TreeData<TreeNode> treeData;
	private GridContextMenuSupport<TreeNode> contextMenuSupp;
	private EntityCreationHandler entityCreationDialogHandler;

	@Autowired
	public GroupsTree(GroupsManagement groupsMan, EntityManagement identitiesMan,
			UnityMessageSource msg, AttributeClassManagement acMan,
			EntityCreationHandler entityCreationDialogHandler,
			GroupManagementHelper groupManagementHelper)
	{
		this.groupsMan = groupsMan;
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.acMan = acMan;
		this.entityCreationDialogHandler = entityCreationDialogHandler;
		this.groupManagementHelper = groupManagementHelper;
		contextMenuSupp = new GridContextMenuSupport<>(this);
		addExpandListener(new GroupExpandListener());
		addSelectionListener(e -> {
			final TreeNode node = getSelection();
			bus.fireEvent(new GroupChangedEvent(node == null ? null : node.getPath()));
		});
		
		SingleSelectionModel<TreeNode> singleSelect =
				      (SingleSelectionModel<TreeNode>) getSelectionModel();
			
		singleSelect.setDeselectAllowed(false);
	
		addActionHandler(getRefreshAction());
		addActionHandler(getExpandAllAction());
		addActionHandler(getCollapseAllAction());
		addActionHandler(getAddAction());
		addActionHandler(getEditAction());
		addActionHandler(getEditACsAction());
		addActionHandler(getDeleteAction());
		addActionHandler(getAddEntityAction());
		// setDropHandler(new GroupDropHandler());

		this.bus = WebSession.getCurrent().getEventBus();

		treeData = new TreeData<>();
		setDataProvider(new GroupsDataProvider(treeData));

		addColumn(n -> {
			return n.getIcon() + " " + n.getPath();
		}, new HtmlRenderer());
		setHeaderVisible(false);
		setSizeFull();

		//TODO Drop support
		TreeGridDropTarget<TreeNode> dropTarget = new TreeGridDropTarget<>(this,
				DropMode.ON_TOP);
		dropTarget.setDropEffect(DropEffect.MOVE);
		// dropTarget.addGridDropListener(e ->
		// System.out.println(e.getDropTargetRow().get().getPath()));

		
		try
		{
			setupRoot();
		} catch (EngineException e)
		{
			// this will show error node
			TreeNode parent = new TreeNode(msg, new Group("/"),
					Images.vaadinFolder.getHtml());
			treeData.addItems(null, parent);
			getDataProvider().refreshAll();
			expand(parent);
		}

	}

	private void addActionHandler(SingleActionHandler2<TreeNode> actionHandler)
	{
		contextMenuSupp.addActionHandler(actionHandler);

	}

	public List<SingleActionHandler2<TreeNode>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
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
					Images.vaadinFolder.getHtml());
			treeData.clear();
			treeData.addItem(null, parent);
			setExpandEnable(parent);
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
							Images.vaadinFolder.getHtml());
					treeData.addItem(null, parent);
					setExpandEnable(parent);

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
		setExpandEnable(node);
		getDataProvider().refreshAll();
		collapse(node);		
		expand(node);
	}

	/**
	 * Adding mock child, then node from param is expandable
	 * 
	 * @param node
	 */
	private void setExpandEnable(TreeNode node)
	{
//		if (treeData.getChildren(node).isEmpty())
//			treeData.addItem(node, node.getAsEmpty());
	}

	private void removeGroup(TreeNode parent, String path, boolean recursive)
	{
		try
		{
			groupsMan.removeGroup(path, recursive);
			refreshNode(parent);
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

	private void addToGroupVerification(String finalGroup, final EntityWithLabel entity)
	{
		final EntityParam entityParam = new EntityParam(entity.getEntity().getId());
		Collection<String> existingGroups;
		try
		{
			existingGroups = identitiesMan.getGroups(entityParam).keySet();
		} catch (EngineException e1)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.getMembershipError", entity),
					e1);
			return;
		}
		final Deque<String> notMember = Group.getMissingGroups(finalGroup, existingGroups);

		if (notMember.size() == 0)
		{
			NotificationPopup.showNotice(msg, msg.getMessage("GroupsTree.alreadyMember",
					entity, finalGroup), "");
			return;
		}

		ConfirmDialog confirm = new ConfirmDialog(msg,
				msg.getMessage("GroupsTree.confirmAddToGroup", entity,
						groups2String(notMember)),
				new Callback()
				{
					@Override
					public void onConfirm()
					{
						groupManagementHelper.addToGroup(notMember,
								entity.getEntity().getId(),
								new GroupManagementHelper.Callback()
								{
									@Override
									public void onAdded(
											String toGroup)
									{
									}
								});
					}
				});
		confirm.show();

	}

	private String groups2String(Deque<String> groups)
	{
		StringBuilder ret = new StringBuilder(64);
		Iterator<String> it = groups.descendingIterator();
		while (it.hasNext())
			ret.append(it.next()).append("  ");
		return ret.toString();
	}
	
//	private class GroupDropHandler implements DropHandler
//	{
//
//		@Override
//		public void drop(DragAndDropEvent event)
//		{
//			Transferable rawTransferable = event.getTransferable();
//			if (rawTransferable instanceof TableTransferable)
//			{
//				TableTransferable transferable = (TableTransferable) rawTransferable;
//				Object draggedRaw = transferable.getItemId();
//				EntityWithLabel entity = null;
//				if (draggedRaw instanceof IdentityWithEntity)
//				{
//					IdentityWithEntity dragged = (IdentityWithEntity) draggedRaw;
//					entity = dragged.getEntityWithLabel();
//				} else if (draggedRaw instanceof EntityWithLabel)
//				{
//					entity = (EntityWithLabel)draggedRaw;
//				}
//				if (entity != null)
//				{
//					AbstractSelectTargetDetails target = 
//							(AbstractSelectTargetDetails) event.getTargetDetails();
//					final TreeNode node = (TreeNode) target.getItemIdOver();
//					addToGroupVerification(node.getPath(), entity);
//				}
//			}
//		}
//
//		@Override
//		public AcceptCriterion getAcceptCriterion()
//		{
//			return VerticalLocationIs.MIDDLE;
//		}
//	}
	
	private SingleActionHandler2<TreeNode> getAddAction()
	{
		return SingleActionHandler2.builder(TreeNode.class)
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

	private SingleActionHandler2<TreeNode> getEditACsAction()
	{
		return SingleActionHandler2.builder(TreeNode.class)
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

	private SingleActionHandler2<TreeNode> getEditAction()
	{
		return SingleActionHandler2.builder4Edit(msg, TreeNode.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<TreeNode> target)
	{
		TreeNode node = target.iterator().next();
		Group group;
		try
		{
			group = groupsMan.getContents(node.getPath(), GroupContents.METADATA)
					.getGroup();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("GroupsTree.resolveGroupError"), e);
			return;
		}

		new GroupEditDialog(msg, group, true, g -> {
			updateGroup(node.getPath(), g);
			refreshNode(node.getParentNode());
			if (node.equals(getSelection()))
				bus.fireEvent(new GroupChangedEvent(node.getPath()));
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

	private SingleActionHandler2<TreeNode> getAddEntityAction()
	{
		
		return SingleActionHandler2.builder(TreeNode.class)
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

	private SingleActionHandler2<TreeNode> getRefreshAction()
	{
		return SingleActionHandler2.builder4Refresh(msg, TreeNode.class)
				.withHandler(selection -> selection.forEach(this::refreshNode))
				.build();
	}

	private SingleActionHandler2<TreeNode> getDeleteAction()
	{
		return SingleActionHandler2.builder4Delete(msg, TreeNode.class)
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

	private SingleActionHandler2<TreeNode> getExpandAllAction()
	{
		return SingleActionHandler2.builder(TreeNode.class)
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

	private SingleActionHandler2<TreeNode> getCollapseAllAction()
	{
		return SingleActionHandler2.builder(TreeNode.class)
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
			
			GroupContents contents;
			try
			{
				contents = groupsMan.getContents(expandedNode.getPath(),
						GroupContents.GROUPS | GroupContents.METADATA);
			} catch (Exception e)
			{
				expandedNode.setIcon(Images.vaadinNoAuthzGrp.getHtml());
				expandedNode.setContentsFetched(true);
				expand(expandedNode);
				return;
			}
			expandedNode.setIcon(Images.vaadinFolder.getHtml());
			expandedNode.setGroupMetadata(contents.getGroup());

			List<String> subgroups = contents.getSubGroups();
			Collections.sort(subgroups);
			for (String subgroup : subgroups)
			{
				GroupContents contents2;
				try
				{
					contents2 = groupsMan.getContents(subgroup,
							GroupContents.METADATA);
					TreeNode node = new TreeNode(msg, contents2.getGroup(),
							Images.vaadinFolder.getHtml(),
							expandedNode);
					treeData.addItem(node.getParentNode(), node);
					setExpandEnable(node);
				} catch (EngineException e)
				{
					log.debug("Group " + subgroup
							+ " won't be shown - metadata not readable.");
				}
			}
			
			expandedNode.setContentsFetched(true);
			getDataProvider().refreshAll();
			//we expand empty node before, we have to expand one more time to reload
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
