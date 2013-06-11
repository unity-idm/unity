/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.identities.EntityCreationDialog;
import pl.edu.icm.unity.webadmin.identities.IdentitiesTable.IdentityWithEntity;
import pl.edu.icm.unity.webadmin.utils.GroupManagementUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Table.TableTransferable;

/**
 * Tree with groups obtained dynamically from the engine.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupsTree extends Tree
{
	private GroupsManagement groupsMan;
	private IdentitiesManagement identitiesMan;
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private IdentityEditorRegistry identityEditorReg;
	private EventsBus bus;

	@Autowired
	public GroupsTree(GroupsManagement groupsMan, IdentitiesManagement identitiesMan, 
			AuthenticationManagement authnMan, IdentityEditorRegistry identityEditorReg,
			UnityMessageSource msg)
	{
		this.groupsMan = groupsMan;
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.authnMan = authnMan;
		this.identityEditorReg = identityEditorReg;

		addExpandListener(new GroupExpandListener());
		addValueChangeListener(new ValueChangeListenerImpl());
		addActionHandler(new AddGroupActionHandler());
		addActionHandler(new EditGroupActionHandler());
		addActionHandler(new RefreshActionHandler());
		addActionHandler(new DeleteActionHandler());
		addActionHandler(new ExpandAllActionHandler());
		addActionHandler(new CollapseAllActionHandler());
		addActionHandler(new AddEntityActionHandler());
		setDropHandler(new GroupDropHandler());
		setImmediate(true);
		this.bus = WebSession.getCurrent().getEventBus();
		
		try
		{
			setupRoot();
		} catch (EngineException e)
		{
			//this will show error node
			TreeNode parent = new TreeNode(msg, "/");
			addItem(parent);
			expandItem(new TreeNode(msg, "/"));
		}
	}

	/**
	 * We can have two cases: either we can read '/' or not. In the latter case we take groups where the
	 * logged user is the member, and we put all of them as root groups.
	 * @throws EngineException 
	 */
	private void setupRoot() throws EngineException
	{
		try
		{
			groupsMan.getContents("/", GroupContents.GROUPS|GroupContents.LINKED_GROUPS);
			TreeNode parent = new TreeNode(msg, "/");
			addItem(parent);
			setItemIcon(parent, Images.folder.getResource());
			expandItem(new TreeNode(msg, "/"));
		} catch (AuthorizationException e)
		{
			setupAccessibleRoots();
		}
	}

	private void setupAccessibleRoots() throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		Collection<String> groups = identitiesMan.getGroups(
				new EntityParam(String.valueOf(ae.getEntityId())));
		List<String> accessibleGroups = new ArrayList<String>(groups.size());
		for (String group: groups)
		{
			try
			{
				groupsMan.getContents(group, GroupContents.GROUPS|GroupContents.LINKED_GROUPS);
			} catch (AuthorizationException e2)
			{
				continue;
			}
			accessibleGroups.add(group);
		}
		for (int i=0; i<accessibleGroups.size(); i++)
		{
			Group groupG = new Group(accessibleGroups.get(i));
			boolean parentFound = false;
			for (int j=0; j<accessibleGroups.size(); j++)
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
				TreeNode parent = new TreeNode(msg, accessibleGroups.get(i), true);
				addItem(parent);
				setItemIcon(parent, Images.folder.getResource());
			}
		}
	}
	
	public void refresh()
	{
		refreshNode(new TreeNode(msg, "/"));
	}
	
	private void refreshNode(TreeNode node)
	{
		if (node == null)
			node = new TreeNode(msg, "/");
		node.setContentsFetched(false);
		setChildrenAllowed(node, true);
		collapseItem(node);
		expandItem(node);
	}
	
	private void removeGroup(TreeNode parent, String path, boolean recursive)
	{
		try
		{
			groupsMan.removeGroup(path, recursive);
			refreshNode(parent);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.removeGroupError"), e);
		}
	}
	
	private void createGroup(Group toBeCreated)
	{
		try
		{
			groupsMan.addGroup(toBeCreated);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.addGroupError"), e);
		}
	}

	private void updateGroup(String path, Group group)
	{
		try
		{
			groupsMan.updateGroup(path, group);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.updateGroupError"), e);
		}
	}

	private void addToGroupVerification(String finalGroup, final String entityId)
	{
		final EntityParam entityParam = new EntityParam(entityId);
		Collection<String> existingGroups;
		try
		{
			existingGroups = identitiesMan.getGroups(entityParam);
		} catch (EngineException e1)
		{
			ErrorPopup.showError(msg.getMessage("GroupsTree.getMembershipError", entityId), e1);
			return;
		}
		final Deque<String> notMember = GroupManagementUtils.getMissingGroups(finalGroup, existingGroups);
		
		if (notMember.size() == 0)
		{
			ErrorPopup.showNotice(msg.getMessage("GroupsTree.alreadyMember", entityId, 
					finalGroup), "");
			return;
		}
		
		ConfirmDialog confirm = new ConfirmDialog(msg, 
				msg.getMessage("GroupsTree.confirmAddToGroup", entityId,
						groups2String(notMember)), 
				new Callback()
				{
					@Override
					public void onConfirm()
					{
						GroupManagementUtils.addToGroup(notMember, entityId, msg, groupsMan);
					}
				});
		confirm.show();
		
	}
	
	private String groups2String(Deque<String> groups)
	{
		StringBuilder ret = new StringBuilder(64);
		Iterator<String> it = groups.descendingIterator(); 
		while(it.hasNext())
			ret.append(it.next()).append("  ");
		return ret.toString();
	}
	
	private class GroupDropHandler implements DropHandler
	{

		@Override
		public void drop(DragAndDropEvent event)
		{
			Transferable rawTransferable = event.getTransferable();
			if (rawTransferable instanceof TableTransferable)
			{
				TableTransferable transferable = (TableTransferable) rawTransferable;
				Object draggedRaw = transferable.getItemId();
				String entityId = null;
				if (draggedRaw instanceof IdentityWithEntity)
				{
					IdentityWithEntity dragged = (IdentityWithEntity) draggedRaw;
					entityId = dragged.getEntity().getId();
				} else if (draggedRaw instanceof Entity)
				{
					entityId = ((Entity)draggedRaw).getId();
				}
				if (entityId != null)
				{
					AbstractSelectTargetDetails target = (AbstractSelectTargetDetails) event.getTargetDetails();
					final TreeNode node = (TreeNode) target.getItemIdOver();
					addToGroupVerification(node.getPath(), entityId);
				}
			}
		}

		@Override
		public AcceptCriterion getAcceptCriterion()
		{
			return VerticalLocationIs.MIDDLE;
		}
	}
	
	private class AddGroupActionHandler extends SingleActionHandler
	{
		public AddGroupActionHandler()
		{
			super(msg.getMessage("GroupsTree.addGroupAction"), Images.add.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final TreeNode node = (TreeNode) target;
			
			new GroupEditDialog(msg, new Group(node.getPath()),	false, new GroupEditDialog.Callback()
			{
				@Override
				public void onConfirm(Group toBeCreated)
				{
					createGroup(toBeCreated);
					refreshNode(node);
				}
			}).show();
		}
	}

	private class EditGroupActionHandler extends SingleActionHandler
	{
		public EditGroupActionHandler()
		{
			super(msg.getMessage("GroupsTree.editGroupAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final TreeNode node = (TreeNode) target;
			Group group;
			try
			{
				group = groupsMan.getContents(node.getPath(), GroupContents.METADATA).getGroup();
			} catch (Exception e)
			{
				ErrorPopup.showError(msg.getMessage("GroupsTree.resolveGroupError"), e);
				return;
			}
			
			new GroupEditDialog(msg, group, true, new GroupEditDialog.Callback()
			{
				@Override
				public void onConfirm(Group updated)
				{
					updateGroup(node.getPath(), updated);
					refreshNode(node.getParentNode());
					if (node.equals(getValue()))
						bus.fireEvent(new GroupChangedEvent(node.getPath()));				
				}
			}).show();
		}
	}
	
	private class AddEntityActionHandler extends SingleActionHandler
	{
		public AddEntityActionHandler()
		{
			super(msg.getMessage("GroupsTree.addEntityAction"), Images.add.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final TreeNode node = (TreeNode) target;
			
			new EntityCreationDialog(msg, node.getPath(), identitiesMan, groupsMan, 
					authnMan, identityEditorReg, new EntityCreationDialog.Callback()
					{
						@Override
						public void onCreated()
						{
							if (node.equals(getValue()))
								bus.fireEvent(new GroupChangedEvent(node.getPath()));
						}
					}).show();
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("GroupsTree.refreshGroupAction"), 
					Images.refresh.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			refreshNode((TreeNode) target);
		}
	}

	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("GroupsTree.deleteGroupAction"), 
					Images.delete.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final TreeNode node = (TreeNode) target;
			new ConfirmWithOptionDialog(msg, msg.getMessage("GroupRemovalDialog.confirmDelete", node.getPath()),
					msg.getMessage("GroupRemovalDialog.recursive"),
					new ConfirmWithOptionDialog.Callback()
			{
				@Override
				public void onConfirm(boolean recursive)
				{
					removeGroup(node.getParentNode(), node.getPath(), recursive);
				}
			}).show();
		}
	}

	private class ExpandAllActionHandler extends SingleActionHandler
	{
		public ExpandAllActionHandler()
		{
			super(msg.getMessage("GroupsTree.expandGroupsAction"), 
					Images.folder.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			expandItemsRecursively(target);
		}
	}

	private class CollapseAllActionHandler extends SingleActionHandler
	{
		public CollapseAllActionHandler()
		{
			super(msg.getMessage("GroupsTree.collapseGroupsAction"), 
					Images.collapse.getResource());
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			collapseItemsRecursively(target);
		}
	}

	private class GroupExpandListener implements ExpandListener
	{
		@Override
		public void nodeExpand(ExpandEvent event)
		{
			TreeNode expandedNode = (TreeNode)event.getItemId();
			if (expandedNode.isContentsFetched())
				return;

			//in case of refresh
			removeAllChildren(expandedNode);
			GroupContents contents;
			try
			{
				contents = groupsMan.getContents(expandedNode.getPath(), GroupContents.GROUPS|
						GroupContents.LINKED_GROUPS);
			} catch (Exception e)
			{
				setItemIcon(expandedNode, Images.noAuthzGrp.getResource());
				setChildrenAllowed(expandedNode, false);
				return;
			}

			if (contents.getLinkedGroups().isEmpty() && contents.getSubGroups().isEmpty())
				setChildrenAllowed(expandedNode, false);

			List<String> subgroups = contents.getSubGroups(); 
			Collections.sort(subgroups);
			for (String subgroup: subgroups)
			{
				TreeNode node = new TreeNode(msg, subgroup);
				addItem(node);
				setItemIcon(node, Images.folder.getResource());
				setParent(node, node.getParentNode());
			}

			expandedNode.setContentsFetched(true);
		}
		
		private void removeAllChildren(Object item)
		{
			//warning - a live collection is returned
			Collection<?> children = getChildren(item);
			if (children != null)
			{
				Set<Object> copied = new HashSet<Object>(children.size());
				copied.addAll(children);
				for (Object child: copied)
				{
					collapseItem(child);
					removeAllChildren(child);
					removeItem(child);
				}
			}
		}
	}
	
	private class ValueChangeListenerImpl implements ValueChangeListener
	{
		@Override
		public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
		{
			final TreeNode node = (TreeNode) getValue();
			bus.fireEvent(new GroupChangedEvent(node == null ? null : node.getPath()));
		}
	}
}
