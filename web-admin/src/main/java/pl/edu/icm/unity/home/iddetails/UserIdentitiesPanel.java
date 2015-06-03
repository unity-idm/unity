/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.SingleTypeIdentityEditor;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Label;

/**
 * Shows (optionally in edit mode) all configured identities.
 * 
 * @author K. Benedyczak
 */
public class UserIdentitiesPanel
{
	private UnityMessageSource msg;
	protected IdentityEditorRegistry identityEditorReg;
	protected IdentitiesManagement idsManagement;
	private long entityId;
	private List<SingleTypeIdentityEditor> identityEditors;
	private AbstractOrderedLayout parent;
	private Map<IdentityType, List<Identity>> editableIdsByType;
	private Map<IdentityType, List<Identity>> roIdsByType;
	private List<Label> roLabels;
	
	public UserIdentitiesPanel(UnityMessageSource msg, IdentityEditorRegistry identityEditorReg,
			IdentitiesManagement idsManagement, long entityId) throws EngineException
	{
		this.msg = msg;
		this.identityEditorReg = identityEditorReg;
		this.idsManagement = idsManagement;
		this.entityId = entityId;
		initIdentities();
	}

	private void initIdentities() throws EngineException
	{
		Entity entity = idsManagement.getEntity(new EntityParam(entityId));
		Identity[] identities = entity.getIdentities();
		editableIdsByType = new HashMap<>();
		roIdsByType = new HashMap<>();
		Collection<IdentityType> identityTypes = idsManagement.getIdentityTypes();
		for (IdentityType idType: identityTypes)
		{
			if (idType.isSelfModificable())
				editableIdsByType.put(idType, new ArrayList<Identity>());
		}
		for (Identity id: identities)
		{
			boolean editable = id.getType().isSelfModificable(); 
			List<Identity> list = (editable ? editableIdsByType : roIdsByType).get(id.getType());
			if (list == null)
			{
				list = new ArrayList<Identity>();
				(editable ? editableIdsByType : roIdsByType).put(id.getType(), list);
			}
			list.add(id);
		}
	}
	
	public void addIntoLayout(AbstractOrderedLayout layout) throws EngineException
	{
		this.parent = layout;
		initUI();
	}
	
	private void initUI() throws EngineException
	{
		identityEditors = new ArrayList<>();
		roLabels = new ArrayList<>();

		for (IdentityType typeKey: roIdsByType.keySet())
			addRoIdentity(typeKey);
		for (IdentityType typeKey: editableIdsByType.keySet())
			addEditableIdentity(typeKey);
	}
	
	private void addRoIdentity(IdentityType idType)
	{		
		List<Identity> idList = roIdsByType.get(idType);
		for (int i = 0; i < idList.size(); i++)
		{
			Identity id = idList.get(i);
			Label label = new Label(id.toPrettyStringNoPrefix());
			String caption = idType.getIdentityTypeProvider().getHumanFriendlyName(msg);
			caption += (i > 0) ? " (" + (i+1) + "):" : ":";
			label.setCaption(caption);
			roLabels.add(label);
			parent.addComponent(label);
		}
	}

	private void addEditableIdentity(IdentityType idType)
	{		
		List<Identity> idList = editableIdsByType.get(idType);
		SingleTypeIdentityEditor singleTypeIdentityEditor = new SingleTypeIdentityEditor(idType, 
				idList, identityEditorReg, msg, parent);
		identityEditors.add(singleTypeIdentityEditor);
	}
	
	private void clear()
	{
		for (Label l: roLabels)
			parent.removeComponent(l);
		for (SingleTypeIdentityEditor e: identityEditors)
			e.removeAll();
	}

	public void refresh() throws EngineException
	{
		clear();
		initIdentities();
		initUI();
	}
	
	public void validate() throws FormValidationException
	{
		for (SingleTypeIdentityEditor editor: identityEditors)
			editor.getIdentities();
	}
	
	public void saveChanges() throws Exception
	{
		Collection<IdentityParam> newIdentities = new HashSet<>();
		Collection<String> types = new HashSet<>();
		for (SingleTypeIdentityEditor editor: identityEditors)
		{
			try
			{
				newIdentities.addAll(editor.getIdentities());
			} catch (FormValidationException e)
			{
				throw new IllegalStateException("validation error on the idneities, "
						+ "i.e. validation was not performed earlier");
			}
			types.add(editor.getType().getIdentityTypeProvider().getId());
		}
		
		idsManagement.setIdentities(new EntityParam(entityId), types, newIdentities);
	}

	public boolean hasEditable()
	{
		return identityEditors.size() > 0;
	}
}
