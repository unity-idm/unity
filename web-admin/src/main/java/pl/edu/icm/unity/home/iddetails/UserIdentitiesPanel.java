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

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter.ComposableComponents;
import pl.edu.icm.unity.webui.common.composite.GroupOfGroups;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.SingleTypeIdentityEditor;

/**
 * Shows (optionally in edit mode) all configured identities.
 * 
 * @author K. Benedyczak
 */
public class UserIdentitiesPanel
{
	private UnityMessageSource msg;
	protected IdentityEditorRegistry identityEditorReg;
	protected EntityManagement idsManagement;
	private long entityId;
	private List<SingleTypeIdentityEditor> identityEditors;
	private Map<IdentityType, List<Identity>> editableIdsByType;
	private Map<IdentityType, List<Identity>> roIdsByType;
	private List<ComponentsGroup> roLabels;
	private IdentityTypeSupport idTypeSupport;
	private GroupOfGroups componentsGroup;
	
	public UserIdentitiesPanel(UnityMessageSource msg, IdentityEditorRegistry identityEditorReg,
			EntityManagement idsManagement, long entityId, IdentityTypeSupport idTypeSupport) 
					throws EngineException
	{
		this.msg = msg;
		this.identityEditorReg = identityEditorReg;
		this.idsManagement = idsManagement;
		this.entityId = entityId;
		this.idTypeSupport = idTypeSupport;
		
		identityEditors = new ArrayList<>();
		roLabels = new ArrayList<>();
		componentsGroup = new GroupOfGroups();
		
		initIdentities();
		initUI();
	}

	private void initIdentities() throws EngineException
	{
		Entity entity = idsManagement.getEntity(new EntityParam(entityId));
		List<Identity> identities = entity.getIdentities();
		editableIdsByType = new HashMap<>();
		roIdsByType = new HashMap<>();
		Collection<IdentityType> identityTypes = idTypeSupport.getIdentityTypes();
		for (IdentityType idType: identityTypes)
		{
			if (idType.isSelfModificable())
				editableIdsByType.put(idType, new ArrayList<Identity>());
		}
		for (Identity id: identities)
		{
			IdentityType type = idTypeSupport.getType(id.getTypeId());
			boolean editable = type.isSelfModificable(); 
			List<Identity> list = (editable ? editableIdsByType : roIdsByType).get(type);
			if (list == null)
			{
				list = new ArrayList<Identity>();
				(editable ? editableIdsByType : roIdsByType).put(type, list);
			}
			list.add(id);
		}
	}
	
	public ComposableComponents getContents()
	{
		return componentsGroup;
	}
	
	private void initUI() throws EngineException
	{
		identityEditors.clear();
		roLabels.clear();
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
			IdentityTypeDefinition typeDef = idTypeSupport.getTypeDefinition(id.getTypeId());
			Label label = new Label(typeDef.toPrettyStringNoPrefix(id));
			String caption = typeDef.getHumanFriendlyName(msg);
			caption += (i > 0) ? " (" + (i+1) + "):" : ":";
			label.setCaption(caption);
			ComponentsGroup wrappedLabel = new ComponentsGroup(label);
			roLabels.add(wrappedLabel);
			componentsGroup.addComposableComponents(wrappedLabel);
		}
	}

	private void addEditableIdentity(IdentityType idType)
	{		
		List<Identity> idList = editableIdsByType.get(idType);
		SingleTypeIdentityEditor singleTypeIdentityEditor = new SingleTypeIdentityEditor(idType, 
				idList, identityEditorReg, msg, idTypeSupport);
		identityEditors.add(singleTypeIdentityEditor);
		componentsGroup.addComposableComponents(singleTypeIdentityEditor.getComponentsGroup());
	}
	
	private void clear()
	{
		for (ComponentsGroup l: roLabels)
			l.removeAll();
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
			types.add(editor.getType().getIdentityTypeProvider());
		}
		
		idsManagement.setIdentities(new EntityParam(entityId), types, newIdentities);
	}

	public boolean hasEditable()
	{
		return identityEditors.size() > 0;
	}
}
