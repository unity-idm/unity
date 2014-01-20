package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;


/**
 * Show information about all translation profiles
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TranslationProfilesComponent extends VerticalLayout
{
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			TranslationProfilesComponent.class);
	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private TranslationProfileManagement profilesMan;
	private TranslationActionsRegistry tactionsRegistry;
	private ObjectMapper jsonMapper;
	
	
	
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg,
			UnityServerConfiguration config, TranslationProfileManagement profilesMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper)
	{
		
		this.msg = msg;
		this.config = config;
		this.profilesMan = profilesMan;
		this.tactionsRegistry = tactionsRegistry;
		this.jsonMapper = jsonMapper;
		initUI();
	}


	private void initUI()
	{
		
		setCaption(msg.getMessage("TranslationProfiles.caption"));
		setMargin(true);
		setSpacing(true);
		

		Button reloadTransProfileButton = new Button(
				msg.getMessage("TranslationProfiles.reloadAll"));
		reloadTransProfileButton.setIcon(Images.refresh.getResource());
		reloadTransProfileButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				reloadTranslationProfile();

			}

		});
		addComponent(reloadTransProfileButton);
		
		
		
		
	}
	
	
	private void reloadTranslationProfile()
	{	log.info("Reloading translation profile");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage("Endpoints.cannotReloadConfig"), e);
			return;
		}
		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{	
			log.error("Cannot load translation profiles",e);
			ErrorPopup.showError(
					msg,
					msg.getMessage("TranslationProfiles.cannotGetTranslationProfiles"),
					e);
			return;
		}
		Map<String, TranslationProfile> toRemove = new HashMap<>(existing);

		List<String> profileFiles = config
				.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("Endpoints.cannotReadJsonConfig"),
						e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (existing.containsKey(tp.getName()))
			{
				try
				{
					log.info("Update "+tp.getName());
					profilesMan.updateProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot update translation",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("TranslationProfiles.cannotUpdateTranslationProfile"),
							e);
					return;
				}

			} else
			{
				try
				{	log.info("Add "+tp.getName());
					profilesMan.addProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot add translation profile",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("TranslationProfiles.cannotAddTranslationProfile"),
							e);
					return;
				}

			}
			toRemove.remove(tp.getName());

		}

		for (String tp : toRemove.keySet())
		{
			try
			{	log.info("Remove "+tp);
				profilesMan.removeProfile(tp);
			} catch (Exception e)
			{
				log.error("Cannot remove translation profile",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("TranslationProfiles.cannotRemoveTranslationProfile"),
						e);
				return;
			}

		}

	}
	
	
}
