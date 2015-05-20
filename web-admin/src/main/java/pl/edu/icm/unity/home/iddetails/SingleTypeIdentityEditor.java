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
		this.userFriendlyName = idType.getIdentityTypeProvider().getHumanFriendlyName(msg);

		initUI(initial);
	}
	
	private void initUI(Collection<Identity> initial)
	{
		int min = Math.min(initial.size(), idType.getMinInstances());
		ret = new ListOfEmbeddedElementsStub<IdentityParam>(
				msg, new IdentityEditorProvider(), 
				min, idType.getMaxInstances(), false, parent);
		ret.setEntries(initial);
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
			ComponentsContainer ret = editor.getEditor(true, false);
			if (value != null)
				editor.setDefaultValue(value.getValue());
			return ret;
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
