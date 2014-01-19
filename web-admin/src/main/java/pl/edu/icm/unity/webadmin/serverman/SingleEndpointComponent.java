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
import pl.edu.icm.unity.webui.common.ErrorPopup;
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

@Component
public class SingleEndpointComponent extends CustomComponent
{

	public static final String STATUS_DEPLOYED = "deployed";
	public static final String STATUS_UNDEPLOYED = "undeployed";

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SingleEndpointComponent.class);
	private EndpointManagement endpointMan;
	private UnityServerConfiguration config;
	private EndpointDescription endpoint;
	private UnityMessageSource msg;
	private GridLayout header;
	private VerticalLayout content;
	private Button showHideContentButton;
	private Button undeplyButton;
	private Button reloadButton;
	private Button deployButton;
	private Image statusImage;
	

	public SingleEndpointComponent(EndpointManagement endpointMan,
			EndpointDescription endpoint, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		this.endpointMan = endpointMan;
		this.endpoint = endpoint;
		this.config = config;
		this.msg = msg;
		initUI();
		setStatus(status);
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		header = new GridLayout(8, 1);
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

		statusImage = new Image("");

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

		reloadButton = new Button();
		reloadButton.setIcon(Images.refresh.getResource());
		reloadButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadButton.setDescription(msg.getMessage("EndpointsStatus.reloadEndpoint"));
		reloadButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reloadEndpoint();

			}
		});

		undeplyButton = new Button();
		undeplyButton.setIcon(Images.delete.getResource());
		undeplyButton.addStyleName(Reindeer.BUTTON_LINK);
		undeplyButton.setDescription(msg.getMessage("EndpointsStatus.undeployEndpoint"));

		undeplyButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg
						.getMessage("EndpointsStatus.unDeployQuestion"),
						new ConfirmDialog.Callback()

						{

							@Override
							public void onConfirm()
							{

								undeployEndpoint();
							}
						}).show();

			}
		});

		deployButton = new Button();
		deployButton.setIcon(Images.add.getResource());
		deployButton.addStyleName(Reindeer.BUTTON_LINK);
		deployButton.setDescription(msg.getMessage("EndpointsStatus.deployEndpoint"));

		deployButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				deployEndpoint();

			}
		});

	}

	
	private void undeployEndpoint()
	{

		log.info("Undeploy " + endpoint.getId() + " endpoint");
		try
		{
			endpointMan.undeploy(endpoint.getId());
		} catch (EngineException e)
		{

			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.CannotundeployEndpoint"), e);
			return;

		}

		setStatus(STATUS_UNDEPLOYED);
	}

	private void deployEndpoint()
	{
		log.info("Deploy " + endpoint.getId() + " endpoint");
		boolean added = false;
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.CannotReloadConfig"), e);
			return;
		}

		log.info("Reading all configured endpoints");
		Set<String> endpointsList = config
				.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME)
					.equals(endpoint.getId()))
			{
				String description = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
				File configFile = config.getFileValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_CONFIGURATION,
						false);
				String authenticatorsSpec = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
				String type = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_TYPE);
				String address = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_ADDRESS);

				String[] authenticatorSets = authenticatorsSpec.split(";");
				List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
				for (String authenticatorSet : authenticatorSets)
				{
					Set<String> endpointAuthnSet = new HashSet<String>();
					String[] authenticators = authenticatorSet.split(",");
					for (String a : authenticators)
						endpointAuthnSet.add(a.trim());
					endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
				}

				String jsonConfiguration;
				try
				{
					jsonConfiguration = FileUtils.readFileToString(configFile);
				} catch (IOException e)
				{
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.CannotReadJsonConfig"),
							e);
					return;
				}
				log.info("Deploy " + endpoint.getId() + " endpoint");
				try
				{
					this.endpoint = endpointMan.deploy(type, endpoint.getId(),
							address, description, endpointAuthn,
							jsonConfiguration);
				} catch (EngineException e)
				{
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.CannotDeployEndpoint"),
							e);
					return;
				}
				setStatus(STATUS_DEPLOYED);
				added = true;

			}
		}

		if (!added)
		{
			ErrorPopup.showError(
					msg,
					msg.getMessage("EndpointsStatus.CannotDeployEndpoint"),
					msg.getMessage("EndpointsStatus.CannotDeployRemovedConfigEndpoint"));
			setVisible(false);
			return;

		}

	}
	
	private void reloadEndpoint()
	{
		log.info("Reload " + endpoint.getId() + " endpoint");
		boolean updated = false;
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.CannotReloadConfig"), e);
			return;
		}

		log.info("Reading all configured endpoints");
		Set<String> endpointsList = config
				.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME)
					.equals(endpoint.getId()))
			{
				String description = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
				File configFile = config.getFileValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_CONFIGURATION,
						false);
				String authenticatorsSpec = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);

				String[] authenticatorSets = authenticatorsSpec.split(";");
				List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
				for (String authenticatorSet : authenticatorSets)
				{
					Set<String> endpointAuthnSet = new HashSet<String>();
					String[] authenticators = authenticatorSet.split(",");
					for (String a : authenticators)
						endpointAuthnSet.add(a.trim());
					endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
				}

				String jsonConfiguration;
				try
				{
					jsonConfiguration = FileUtils.readFileToString(configFile);
				} catch (IOException e)
				{
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.CannotReadJsonConfig"),
							e);
					return;
				}
				log.info("Update " + endpoint.getId() + " endpoint");
				try
				{
					endpointMan.updateEndpoint(endpoint.getId(), description,
							endpointAuthn, jsonConfiguration);
				} catch (EngineException e)
				{
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.CannotUpdateEndpoint"),
							e);
					return;
				}

				updated = true;
				try
				{
					for (EndpointDescription en : endpointMan.getEndpoints())
					{
						if (en.getId().equals(endpoint.getId()))
						{
							this.endpoint = en;
						}
					}
				} catch (EngineException e)
				{
					ErrorPopup.showError(
							msg,
							msg.getMessage("error"),
							msg.getMessage("EndpointsStatus.cannotLoadEndpoints"));
				}
				setStatus(STATUS_DEPLOYED);

			}
		}

		if (!updated)
		{
			new ConfirmDialog(msg,
					msg.getMessage("EndpointsStatus.unDeployWhenRemoved"),
					new ConfirmDialog.Callback()

					{

						@Override
						public void onConfirm()
						{

							undeployEndpoint();

						}
					}).show();

		}
	}

	private void setStatusIcon(Resource img)
	{

		this.statusImage.setSource(img);

	}

	private void setStatus(String status)
	{
		if (status.equals(STATUS_DEPLOYED))
		{
			setStatusIcon(Images.ok.getResource());
			updateHeader();
			updateContent();
			showHideContentButton.setEnabled(true);
			showHideContentButton.setIcon(Images.zoomin.getResource());
			content.setVisible(false);
			undeplyButton.setVisible(true);
			reloadButton.setVisible(true);
			deployButton.setVisible(false);

		}
		if (status.equals(STATUS_UNDEPLOYED))
		{
			setStatusIcon(Images.error.getResource());
			updateHeader();
			showHideContentButton.setEnabled(false);
			showHideContentButton.setIcon(Images.zoomin.getResource());
			content.removeAllComponents();
			content.setVisible(false);
			undeplyButton.setVisible(false);
			reloadButton.setVisible(false);
			deployButton.setVisible(true);
		}

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
		// setStatusIcon(Images.ok.getResource());
		Label statusLabel = new Label(msg.getMessage("EndpointsStatus.status") + ":");
		statusLabel.addStyleName(Styles.bold.toString());
		header.addComponent(statusLabel);
		header.setComponentAlignment(statusLabel, Alignment.BOTTOM_CENTER);
		header.addComponent(statusImage);

		Label spacer = new Label();
		spacer.setWidth(10, Unit.PIXELS);
		header.addComponent(spacer);

		header.addComponent(reloadButton);
		header.setComponentAlignment(reloadButton, Alignment.BOTTOM_LEFT);

		header.addComponent(undeplyButton);
		header.setComponentAlignment(undeplyButton, Alignment.BOTTOM_LEFT);

		header.addComponent(deployButton);
		header.setComponentAlignment(deployButton, Alignment.BOTTOM_LEFT);

	}

	
	private void updateContent()
	{

		content.removeAllComponents();

		if (statusImage.equals(STATUS_DEPLOYED))
		{
			HorizontalLayout lt = addFieldWithLabel(content,
					msg.getMessage("EndpointsStatus.type"), endpoint.getType()
							.getName(), 19);
			addFieldWithLabel(lt, msg.getMessage("EndpointsStatus.type")
					+ " "
					+ msg.getMessage("EndpointsStatus.description")
							.toLowerCase(), endpoint.getType()
					.getDescription(), 2);

			if (endpoint.getDescription() != null
					&& endpoint.getDescription().length() > 0)
			{
				addFieldWithLabel(content,
						msg.getMessage("EndpointsStatus.description"),
						endpoint.getDescription(), 19);

			}
			addFieldWithLabel(content,
					msg.getMessage("EndpointsStatus.contextAddress"),
					endpoint.getContextAddress(), 19);

			int i = 0;

			addFieldWithLabel(content, msg.getMessage("EndpointsStatus.paths"), "", 19);

			for (Map.Entry<String, String> entry : endpoint.getType().getPaths()
					.entrySet())
			{
				i++;
				HorizontalLayout hp = new HorizontalLayout();
				// TODO Add server address prefix
				addFieldWithLabel(hp, String.valueOf(i),
						endpoint.getContextAddress() + entry.getKey(), 55);
				addFieldWithLabel(hp,
						msg.getMessage("EndpointsStatus.description"),
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
			addFieldWithLabel(content,
					msg.getMessage("EndpointsStatus.authenticatorsSet"), "", 19);
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
				// Authenticators
				addFieldWithLabel(content, String.valueOf(i), auth.toString(), 55);

			}
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
