/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Label;

/**
 * Allows to inspect a single identity type
 * @author K. Benedyczak
 */
public class IdentityTypeViewer extends CompactFormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private Label description;
	private Label removable;
	private Label dynamic;
	private Label selfModificable;
	private Label targeted;
	private Label confirmable;
	private Label min;
	private Label max;
	private Label minVerified;
	
	public IdentityTypeViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("IdentityType.name"));
		addComponent(name);
		
		description = new Label();
		description.setCaption(msg.getMessage("IdentityType.description"));
		addComponent(description);

		dynamic = new Label();
		dynamic.setCaption(msg.getMessage("IdentityType.dynamic"));
		addComponent(dynamic);
		
		selfModificable = new Label();
		selfModificable.setCaption(msg.getMessage("IdentityType.selfModificableCaption"));
		addComponent(selfModificable);
		
		min = new Label();
		min.setCaption(msg.getMessage("IdentityType.min"));
		addComponent(min);
		
		minVerified = new Label();
		minVerified.setCaption(msg.getMessage("IdentityType.minVerified"));
		addComponent(minVerified);

		max = new Label();
		max.setCaption(msg.getMessage("IdentityType.max"));
		addComponent(max);
		
		
		removable = new Label();
		removable.setCaption(msg.getMessage("IdentityType.removable"));
		removable.addStyleName(Styles.immutableAttribute.toString());
		addComponent(removable);
		
		targeted = new Label();
		targeted.setCaption(msg.getMessage("IdentityType.targeted"));
		targeted.addStyleName(Styles.immutableAttribute.toString());
		addComponent(targeted);
		
		confirmable = new Label();
		confirmable.setCaption(msg.getMessage("IdentityType.confirmable"));
		confirmable.addStyleName(Styles.immutableAttribute.toString());
		addComponent(confirmable);
		
		setContentsVisible(false);
	}
	
	private void setContentsVisible(boolean how)
	{
		name.setVisible(how);
		description.setVisible(how);
		removable.setVisible(how);
		dynamic.setVisible(how);
		selfModificable.setVisible(how);
		targeted.setVisible(how);
		confirmable.setVisible(how);
		min.setVisible(how);
		max.setVisible(how);
		minVerified.setVisible(how);
	}
	
	public void setInput(IdentityType iType)
	{
		if (iType == null)
		{
			setContentsVisible(false);
			return;
		}
		
		setContentsVisible(true);
		name.setValue(iType.getIdentityTypeProvider().getId());
		description.setValue(iType.getDescription());
		removable.setValue(msg.getYesNo(iType.getIdentityTypeProvider().isRemovable()));
		dynamic.setValue(msg.getYesNo(iType.getIdentityTypeProvider().isDynamic()));
		selfModificable.setValue(msg.getYesNo(iType.isSelfModificable()));
		targeted.setValue(msg.getYesNo(iType.getIdentityTypeProvider().isTargeted()));
		confirmable.setValue(msg.getYesNo(iType.getIdentityTypeProvider().isVerifiable()));
		min.setValue(String.valueOf(iType.getMinInstances()));
		max.setValue(String.valueOf(iType.getMaxInstances()));
		minVerified.setValue(String.valueOf(iType.getMinVerifiedInstances()));
		
		if (!iType.getIdentityTypeProvider().isVerifiable())
			minVerified.setVisible(false);
		
		if (iType.getMaxInstances() == Integer.MAX_VALUE)
			max.setVisible(false);

		if (!iType.isSelfModificable())
		{
			minVerified.setVisible(false);
			min.setVisible(false);
			max.setVisible(false);
		}
	}
}
