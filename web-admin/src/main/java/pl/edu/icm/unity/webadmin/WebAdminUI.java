/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.attribute.AttributesPanel;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupBrowserComponent;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

/**
 * The main entry point of the web administration UI.
 * 
 * TODO - currently only a mess
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WebAdminUI extends UI implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	@Autowired
	private GroupsManagement test;

	@Autowired
	private IdentitiesManagement testIdMan;
	
	@Autowired
	private AttributesManagement testAttrMan;
	
	@Autowired
	private GroupBrowserComponent groupBrowser;
	
	@Autowired
	private UnityMessageSource msg;
	
	@Autowired
	private AttributeHandlerRegistry attrRegistry;
	
	@Autowired
	private AttributesPanel viewer;
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void init(VaadinRequest request)
	{
		try
		{
			test.addGroup(new Group("/A"));
			test.addGroup(new Group("/A/B"));
			test.addGroup(new Group("/A/B/C"));
			test.addGroup(new Group("/D"));
			test.addGroup(new Group("/D/E"));
			test.addGroup(new Group("/D/G"));
			test.addGroup(new Group("/D/F"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		groupBrowser.refresh();
		VerticalLayout contents = new VerticalLayout();
		contents.addComponent(groupBrowser);
		
		List<AttributeType> attributeTypes;
		AttributeType height;
		final EntityParam entity;
		try
		{
			
			AttributeType userPicture = new AttributeType("picture", new JpegImageAttributeSyntax());
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxSize(1400000);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxWidth(900);
			((JpegImageAttributeSyntax)userPicture.getValueType()).setMaxHeight(900);
			userPicture.setMaxElements(1);
			userPicture.setDescription("Picture of the user");
			testAttrMan.addAttributeType(userPicture);
			
			AttributeType postalcode = new AttributeType("postalcode", new StringAttributeSyntax());
			postalcode.setMaxElements(Integer.MAX_VALUE);
			postalcode.setDescription("Postal code");
			((StringAttributeSyntax)postalcode.getValueType()).setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
			((StringAttributeSyntax)postalcode.getValueType()).setMaxLength(6);
			testAttrMan.addAttributeType(postalcode);

			height = new AttributeType("height", new FloatingPointAttributeSyntax());
			height.setMinElements(1);
			height.setDescription("He\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjh");
			testAttrMan.addAttributeType(height);
			
			attributeTypes = testAttrMan.getAttributeTypes();
			
			
			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "foo", true, true);
			Identity added = testIdMan.addIdentity(toAdd, "Password requirement", LocalAuthenticationState.outdated);
			entity = new EntityParam(added);
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} 
		
		contents.addComponent(viewer);
		viewer.setWidth(400, Unit.PIXELS);
		viewer.setHeight(300, Unit.PIXELS);
		
		viewer.setInput(entity, "/", attributeTypes);

		
		Button logout = new Button("logout");
		logout.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				VaadinSession vs = VaadinSession.getCurrent();
				WrappedSession s = vs.getSession();
				Page p = Page.getCurrent();
				s.invalidate();
				//TODO
				p.setLocation("/admin/admin");
			}
		});
		contents.addComponent(logout);
		setContent(contents);
	}

}
