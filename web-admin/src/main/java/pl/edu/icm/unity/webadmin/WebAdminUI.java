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
import pl.edu.icm.unity.home.UserAccountComponent;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.AdminTopHeader.ViewSwitchCallback;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.bus.RefreshEvent;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class WebAdminUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private GroupsManagement test;

	private IdentitiesManagement testIdMan;
	
	private AttributesManagement testAttrMan;
	
	private ContentsManagementTab contentsManagement;
	
	private SchemaManagementTab schemaManagement;
	
	private UserAccountComponent userAccount;
	
	private MainTabPanel tabPanel;
	
	private EndpointDescription endpointDescription;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, GroupsManagement test,
			IdentitiesManagement testIdMan, AttributesManagement testAttrMan,
			ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement,
			UserAccountComponent userAccount)
	{
		super(msg);
		this.test = test;
		this.testIdMan = testIdMan;
		this.testAttrMan = testAttrMan;
		this.contentsManagement = contentsManagement;
		this.schemaManagement = schemaManagement;
		this.userAccount = userAccount;
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		tmpInitContents();

		VerticalLayout contents = new VerticalLayout();

		final VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();

		AdminTopHeader header = new AdminTopHeader(endpointDescription.getId(), msg, 
				new ViewSwitchCallback()
				{
					@Override
					public void showView(boolean admin)
					{
						switchView(mainWrapper, admin ? tabPanel : userAccount);
					}
				});

		
		createMainTabPanel();
		userAccount.setWidth(80, Unit.PERCENTAGE);

		contents.addComponent(header);
		contents.addComponent(mainWrapper);		
		contents.setExpandRatio(mainWrapper, 1.0f);		
		contents.setComponentAlignment(mainWrapper, Alignment.TOP_LEFT);
		contents.setSizeFull();
		
		setContent(contents);
	
		switchView(mainWrapper, tabPanel);
		
		tmpRefreshTypes();
	}

	private void createMainTabPanel()
	{
		tabPanel = new MainTabPanel();		
		tabPanel.addTab(contentsManagement);
		tabPanel.addTab(schemaManagement);
		tabPanel.setSizeFull();
	}
	
	private void switchView(VerticalLayout contents, com.vaadin.ui.Component component)
	{
		contents.removeAllComponents();
		contents.addComponent(component);
		contents.setComponentAlignment(component, Alignment.TOP_CENTER);
		contents.setExpandRatio(component, 1.0f);		
	}
	
	
	//TODO remove below this line
	private void tmpInitContents()
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

			AttributeType cn = new AttributeType("cn", new StringAttributeSyntax());
			cn.setDescription("Common name");
			((StringAttributeSyntax)cn.getValueType()).setMaxLength(100);
			testAttrMan.addAttributeType(cn);
			
			AttributeType org = new AttributeType("o", new StringAttributeSyntax());
			org.setDescription("Organization");
			((StringAttributeSyntax)org.getValueType()).setMaxLength(33);
			testAttrMan.addAttributeType(org);

			AttributeType email = new AttributeType("email", new StringAttributeSyntax());
			email.setDescription("E-main");
			((StringAttributeSyntax)email.getValueType()).setMaxLength(33);
			testAttrMan.addAttributeType(email);

			AttributeType height = new AttributeType("height", new FloatingPointAttributeSyntax());
			height.setMinElements(1);
			height.setDescription("He\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjhHe\n\n\nsdfjkhsdkfjhsd kfjh");
			testAttrMan.addAttributeType(height);
			
			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "foo", true);
			Identity base = testIdMan.addEntity(toAdd, "Password requirement", EntityState.valid, false);

			IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=test foo", true);
			testIdMan.addIdentity(toAddDn, new EntityParam(base.getEntityId()), true);
			
			test.addMemberFromParent("/A", new EntityParam(base.getEntityId()));
			
			EnumAttribute a = new EnumAttribute("sys:AuthorizationRole", "/", AttributeVisibility.local, "Regular User");
			testAttrMan.setAttribute(new EntityParam(base.getEntityId()), a, false);

			StringAttribute orgA = new StringAttribute("o", "/A", AttributeVisibility.full, "Example organization");
			testAttrMan.setAttribute(new EntityParam(base.getEntityId()), orgA, false);

			StringAttribute emailA = new StringAttribute("email", "/A", AttributeVisibility.full, "some@email.com");
			testAttrMan.setAttribute(new EntityParam(base.getEntityId()), emailA, false);

			StringAttribute cnA = new StringAttribute("cn", "/A", AttributeVisibility.full, "Hiper user");
			testAttrMan.setAttribute(new EntityParam(base.getEntityId()), cnA, false);

			testIdMan.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential", "a");
		} catch (Exception e)
		{
			return;
		} 
	}
	
	private void tmpRefreshTypes()
	{
		try
		{
			List<AttributeType> atList = testAttrMan.getAttributeTypes();
			EventsBus bus = WebSession.getCurrent().getEventBus();
			bus.fireEvent(new AttributeTypesUpdatedEvent(atList));
			bus.fireEvent(new RefreshEvent());
		} catch (EngineException e)
		{
			e.printStackTrace();
		}
	}
}
