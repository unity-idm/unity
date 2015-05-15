/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

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
		for (Identity id: identities)
		{
			boolean editable = id.getType().isSelfModificable(); 
			List<Identity> list = (editable ? editableIdsByType : roIdsByType).get(id.getTypeId());
			if (list == null)
			{
				list = new ArrayList<Identity>();
				(editable ? editableIdsByType : roIdsByType).put(id.getType(), list);
			}
			list.add(id);
		}
		Collection<IdentityType> identityTypes = idsManagement.getIdentityTypes();
		for (IdentityType idType: identityTypes)
		{
			String id = idType.getIdentityTypeProvider().getId();
			if (!editableIdsByType.containsKey(id) && idType.isSelfModificable())
				editableIdsByType.put(idType, new ArrayList<Identity>());
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

		for (IdentityType typeKey: roIdsByType.keySet())
			addRoIdentity(typeKey);
		for (IdentityType typeKey: editableIdsByType.keySet())
			addEditableIdentity(typeKey);
	}
	
	private void addRoIdentity(IdentityType idType)
	{		
		List<Identity> idList = roIdsByType.get(idType);
		for (Identity id: idList)
		{
			Label label = new Label(id.toPrettyStringNoPrefix());
			label.setCaption(idType.getIdentityTypeProvider().getHumanFriendlyName(msg) + 
					":");
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
	
	private void refreshEditable()
	{
		/* TODO
		for (FixedAttributeEditor editor: attributeEditors)
		{
			AttributeExt<?> attribute = getAttribute(editor.getAttributeType().getName(), 
					editor.getGroup());			
			editor.setAttributeValues(attribute.getValues());
		}
*/
	}
	
	private void saveChanges()
	{
		/*
		for (FixedAttributeEditor ae: attributeEditors)
		{
			try
			{
				Attribute<?> a = ae.getAttribute();
				if (a != null)
					updateAttribute(a);
				else
					removeAttribute(ae);
			} catch (FormValidationException e)
			{
				continue;
			}
		}
		*/
	}

	public boolean hasEditable()
	{
		return identityEditors.size() > 0;
	}
}
