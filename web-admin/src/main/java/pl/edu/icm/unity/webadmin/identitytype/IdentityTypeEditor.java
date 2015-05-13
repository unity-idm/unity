/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;

/**
 * Allows to edit an identity type. It is only possible to edit description and self modifiable flag. 
 * 
 * @author K. Benedyczak
 */
public class IdentityTypeEditor extends FormLayout
{
	private UnityMessageSource msg;
	
	private IdentityType original;
	private Label name;
	private TextArea description;
	private CheckBox selfModifiable;
	
	public IdentityTypeEditor(UnityMessageSource msg, IdentityType toEdit)
	{
		super();
		this.msg = msg;
		original = toEdit;
		
		initUI(toEdit);
	}

	private void initUI(IdentityType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new Label(toEdit.getIdentityTypeProvider().getId());
		name.setCaption(msg.getMessage("IdentityType.name"));
		addComponent(name);
		
		description = new TextArea(msg.getMessage("IdentityType.description"));
		description.setWidth(100, Unit.PERCENTAGE);
		addComponent(description);
		
		selfModifiable = new CheckBox(msg.getMessage("IdentityType.selfModificable"));
		addComponent(selfModifiable);
		
		setInitialValues(toEdit);
	}
	
	private void setInitialValues(IdentityType aType)
	{
		description.setValue(aType.getDescription());
		selfModifiable.setValue(aType.isSelfModificable());
	}
	
	public IdentityType getIdentityType()
	{
		IdentityType ret = new IdentityType(original.getIdentityTypeProvider());
		ret.setDescription(description.getValue());
		ret.setSelfModificable(selfModifiable.getValue());
		ret.setExtractedAttributes(original.getExtractedAttributes());
		return ret;
	}
}
