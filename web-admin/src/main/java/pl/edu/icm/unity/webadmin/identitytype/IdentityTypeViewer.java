/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.confirmations.EmailConfirmationConfigurationViewer;

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
	private EmailConfirmationConfigurationViewer confirmationViewer;

	private IdentityTypeSupport idTypeSupport;
	
	public IdentityTypeViewer(UnityMessageSource msg, IdentityTypeSupport idTypeSupport)
	{
		super();
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		
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
		
		confirmationViewer = new EmailConfirmationConfigurationViewer(msg);
		addComponent(confirmationViewer);
		
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
		
		IdentityTypeDefinition typeDefinition = idTypeSupport.getTypeDefinition(iType.getName());
		
		setContentsVisible(true);
		name.setValue(iType.getIdentityTypeProvider());
		description.setValue(iType.getDescription());
		removable.setValue(msg.getYesNo(typeDefinition.isRemovable()));
		dynamic.setValue(msg.getYesNo(typeDefinition.isDynamic()));
		selfModificable.setValue(msg.getYesNo(iType.isSelfModificable()));
		targeted.setValue(msg.getYesNo(typeDefinition.isTargeted()));
		confirmable.setValue(msg.getYesNo(typeDefinition.isEmailVerifiable()));
		min.setValue(String.valueOf(iType.getMinInstances()));
		max.setValue(String.valueOf(iType.getMaxInstances()));
		minVerified.setValue(String.valueOf(iType.getMinVerifiedInstances()));
		
		confirmationViewer.setVisible(false);
		if (!typeDefinition.isEmailVerifiable())
		{
			minVerified.setVisible(false);
		} else
		{
			confirmationViewer.setValue(iType.getEmailConfirmationConfiguration());
			confirmationViewer.setVisible(true);
		}
			
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
