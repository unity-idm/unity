package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import eu.unicore.util.configuration.ConfigurationException;

@Component
public class SingleEndpointComponent extends CustomComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleEndpointComponent.class);
	
	private EndpointManagement endpointMan;

	private UnityMessageSource msg;

	private GridLayout header;

	private VerticalLayout content;

	private Button showHideContentButton;

	private Image status;
	
	private UnityServerConfiguration config;
	
	
	private EndpointDescription endpoint;

	public SingleEndpointComponent(EndpointManagement endpointMan,EndpointDescription endpoint,UnityServerConfiguration config ,UnityMessageSource msg)
	{
		this.endpointMan = endpointMan;
		this.endpoint = endpoint;
		this.config = config;
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		header = new GridLayout(7, 1);
		header.setSpacing(true);

		showHideContentButton = new Button();
		showHideContentButton.setIcon(Images.zoomin.getResource());
		showHideContentButton.addStyleName(Reindeer.BUTTON_LINK);

		main.addComponent(header);

		content = new VerticalLayout();
		content.setVisible(false);
		main.addComponent(content);

		Label line = new Label();
		line.addStyleName(Styles.horizontalLine.toString());
		main.addComponent(line);

		status = new Image("");

		setCompositionRoot(main);

		showHideContentButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				if (content.isVisible())
				{
					showHideContentButton.setIcon(Images.zoomin.getResource());
					content.setVisible(false);
				} else
				{
					showHideContentButton.setIcon(Images.zoomout.getResource());
					content.setVisible(true);
				}

			}
		});
		updateHeader();
		updateContent();

	}

	private void updateHeader()
	{
		header.removeAllComponents();

		header.addComponent(showHideContentButton);
		header.setComponentAlignment(showHideContentButton, Alignment.BOTTOM_LEFT);

		// Name
		HorizontalLayout nameFieldLayout = addFieldWithLabel(header,
				msg.getMessage("EndpointsStatus.name"), endpoint.getId(), 0);
		nameFieldLayout.setMargin(false);
		header.setComponentAlignment(nameFieldLayout, Alignment.BOTTOM_CENTER);

		// Status
		setStatusIcon(Images.ok.getResource());
		Label statusLabel = new Label(msg.getMessage("EndpointsStatus.status") + ":");
		statusLabel.addStyleName(Styles.bold.toString());
		header.addComponent(statusLabel);
		header.setComponentAlignment(statusLabel, Alignment.BOTTOM_CENTER);
		header.addComponent(status);

		Label spacer = new Label();
		spacer.setWidth(10, Unit.PIXELS);
		header.addComponent(spacer);

		Button reloadButton = new Button();
		reloadButton.setIcon(Images.refresh.getResource());
		reloadButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadButton.setDescription(msg.getMessage("EndpointsStatus.reloadEndpoint"));
		header.addComponent(reloadButton);
		header.setComponentAlignment(reloadButton, Alignment.BOTTOM_LEFT);

		reloadButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reloadEndpoint();

			}
		});

		Button deleteButton = new Button();
		deleteButton.setIcon(Images.delete.getResource());
		deleteButton.addStyleName(Reindeer.BUTTON_LINK);
		deleteButton.setDescription(msg.getMessage("EndpointsStatus.undeployEndpoint"));
		header.addComponent(deleteButton);
		header.setComponentAlignment(deleteButton, Alignment.BOTTOM_LEFT);

		deleteButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg.getMessage("EndpointsStatus.unDeployQuestion"), new ConfirmDialog.Callback()
				
				{
					
					@Override
					public void onConfirm()
					{
						
						undeployEndpoint();
					}
				}).show();
				undeployEndpoint();

			}
		});

	}

	private void undeployEndpoint()
	{
		try
		{
			endpointMan.undeploy(endpoint.getId());
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setVisible(false);
		
		
		

	}

	private void setStatusIcon(Resource img)
	{
		
		this.status.setSource(img);

	}

	private void reloadEndpoint()
	{
		boolean updated=false;
		
		try
		{
			config.reloadIfChanged();
		} catch (ConfigurationException | IOException e)
		{
			// TODO Auto-generated catch block
			
		}
		
	//	log.info("Loading all configured endpoints");
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey: endpointsList)
		{
			if(config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME).equals(endpoint.getId()))
			{
				String description = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_DESCRIPTION);
				//String type = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_TYPE);
				File configFile = config.getFileValue(endpointKey+UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
			//	String address = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_ADDRESS);
			//	String name = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME);
				String authenticatorsSpec = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
				
				String[] authenticatorSets = authenticatorsSpec.split(";");
				List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
				for (String authenticatorSet: authenticatorSets)
				{
					Set<String> endpointAuthnSet = new HashSet<String>();
					String[] authenticators = authenticatorSet.split(",");
					for (String a: authenticators)
						endpointAuthnSet.add(a.trim());
					endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
				}
				
				//TODO ERRORS
				String jsonConfiguration = null;
				try
				{
					jsonConfiguration = FileUtils.readFileToString(configFile);
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
				try
				{
					endpointMan.updateEndpoint(endpoint.getId(), description,endpointAuthn, jsonConfiguration);
					updated=true;
				} catch (EngineException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	//		log.info(" - " + name + ": " + type + " " + description);
			}
		}
		
		
		
		
		if(!updated){
			new ConfirmDialog(msg, msg.getMessage("EndpointsStatus.unDeployWhenRemoved"), new ConfirmDialog.Callback()
			
			{
				
				@Override
				public void onConfirm()
				{
					
					undeployEndpoint();
				}
			}).show();
			
		}
		
		
		
		
		
		
		// TODO
	}

	private void updateContent()
	{

		content.removeAllComponents();

		HorizontalLayout lt = addFieldWithLabel(content,
				msg.getMessage("EndpointsStatus.type"), endpoint.getType()
						.getName(), 19);
		addFieldWithLabel(lt, msg.getMessage("EndpointsStatus.type") + " "
				+ msg.getMessage("EndpointsStatus.description").toLowerCase(),
				endpoint.getType().getDescription(), 2);

		if (endpoint.getDescription() != null && endpoint.getDescription().length() > 0)
		{
			addFieldWithLabel(content, msg.getMessage("EndpointsStatus.description"),
					endpoint.getDescription(), 19);

		}
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.contextAddress"),
				endpoint.getContextAddress(), 19);

		int i = 0;

		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.paths"), "", 19);

		for (Map.Entry<String, String> entry : endpoint.getType().getPaths().entrySet())
		{
			i++;
			HorizontalLayout hp=new HorizontalLayout();
			addFieldWithLabel(hp, String.valueOf(i),
					endpoint.getContextAddress() + entry.getKey(), 55);
			addFieldWithLabel(hp, msg.getMessage("EndpointsStatus.description"),
					entry.getValue(), 2);
			content.addComponent(hp);

		}

		StringBuilder bindings = new StringBuilder();
		for (String s : endpoint.getType().getSupportedBindings())
		{
			if (bindings.length() > 0)

				bindings.append(",");
			bindings.append(s);

		}
		// Bindings
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.bindings"),
				bindings.toString(), 19);

		i = 0;
		addFieldWithLabel(content, msg.getMessage("EndpointsStatus.authenticatorsSet"), "",
				19);
		for (AuthenticatorSet s : endpoint.getAuthenticatorSets())
		{
			i++;
			StringBuilder auth = new StringBuilder();
			for (String a : s.getAuthenticators())
			{
				if (auth.length() > 0)
					auth.append(",");
				auth.append(a);
			}
			addFieldWithLabel(content, String.valueOf(i), auth.toString(), 55);
			// Authenticators

		}

	}

	private HorizontalLayout addFieldWithLabel(Layout parent, String name, String value,
			int space)
	{
		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
		}
		Label namel = new Label(name + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(value);
		HorizontalLayout fieldLayout = new HorizontalLayout();
		fieldLayout.setSpacing(true);

		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
			fieldLayout.addComponents(spacer, namel, nameVal);
		} else
		{
			fieldLayout.addComponents(namel, nameVal);
		}

		parent.addComponent(fieldLayout);
		return fieldLayout;

	}

}
