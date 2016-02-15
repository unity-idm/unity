/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;

/**
 * Editor of a preset group {@link Selection}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetMembershipEditor extends PresetEditorBase<Selection>
{
	private List<GroupRegistrationParam> formParams;
	private CheckBox cb;
	
	public PresetMembershipEditor(UnityMessageSource msg, List<GroupRegistrationParam> formParams)
	{
		super(msg);
		this.formParams = formParams;
	}

	@Override
	protected Selection getValueInternal() throws FormValidationException
	{
		return new Selection(cb.getValue());
	}
	
	@Override
	public void setEditedComponentPosition(int position)
	{
		cb.setCaption(formParams.get(position).getGroupPath());
	}
	
	@Override
	protected Component getEditorComponentsInternal(PrefilledEntry<Selection> value,
			int position)
	{
		cb = new CheckBox();
		setEditedComponentPosition(position);
		return cb;
	}
}
