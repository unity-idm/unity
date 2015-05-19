/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.ui.AbstractOrderedLayout;

/**
 * Editor component allowing to edit identities of a fixed {@link IdentityType}.
 * For smooth integration this is not Vaadin's component but it can be added to an existing layout. 
 * @author K. Benedyczak
 */
public class SingleTypeIdentityEditor 
{
	private IdentityType idType;
	private IdentityEditorRegistry idEdRegistry;
	private UnityMessageSource msg;
	private AbstractOrderedLayout parent;
	private String userFriendlyName;
	private ListOfEmbeddedElementsStub<IdentityParam> ret;

	public SingleTypeIdentityEditor(IdentityType idType, Collection<Identity> initial, 
			IdentityEditorRegistry idEdRegistry, UnityMessageSource msg, 
			AbstractOrderedLayout parent)
	{
		this.idType = idType;
		this.idEdRegistry = idEdRegistry;
		this.msg = msg;
		this.parent = parent;
		initUI(initial);
	}
	
	private void initUI(Collection<Identity> initial)
	{
		ret = new ListOfEmbeddedElementsStub<IdentityParam>(
				msg, new IdentityEditorProvider(), 
				0, Integer.MAX_VALUE, false, parent);
		for (Identity id: initial)
			ret.addEntry(id, null);
		userFriendlyName = idType.getIdentityTypeProvider().getHumanFriendlyName(msg);
		ret.setLonelyLabel(userFriendlyName + ":");
	}
	
	public void removeAll()
	{
		ret.clearContents();
	}
	
	public Collection<IdentityParam> getIdentities() throws FormValidationException
	{
		return ret.getElements();
	}
	
	public IdentityType getType()
	{
		return idType;
	}
	
	private class IdentityEditorProvider implements EditorProvider<IdentityParam>
	{
		@Override
		public Editor<IdentityParam> getEditor()
		{
			return new IdentityEditorWrapper();
		}
	}
	
	private class IdentityEditorWrapper implements Editor<IdentityParam>
	{
		private IdentityEditor editor;

		@Override
		public ComponentsContainer getEditorComponent(IdentityParam value, int position)
		{
			editor = idEdRegistry.getEditor(idType.getIdentityTypeProvider().getId());
			if (value != null)
				editor.setDefaultValue(value.getValue());
			return editor.getEditor(false, false);
		}

		@Override
		public void setEditedComponentPosition(int position)
		{
			editor.setLabel(esablishLabel(position));
		}

		private String esablishLabel(int position)
		{
			return (position > 0) ? userFriendlyName +" (" + (position+1) +"):" 
					: userFriendlyName + ":";
		}

		@Override
		public IdentityParam getValue() throws FormValidationException
		{
			try
			{
				return editor.getValue();
			} catch (IllegalIdentityValueException e)
			{
				throw new FormValidationException(e);
			}
		}
	}
}
