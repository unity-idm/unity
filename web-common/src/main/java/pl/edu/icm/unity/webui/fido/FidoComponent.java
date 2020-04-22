/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.fido;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.FidoManagement;
import pl.edu.icm.unity.exceptions.FidoException;
import pl.edu.icm.unity.types.authn.FidoCredentialInfo;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import java.util.AbstractMap;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

/**
 * BE part of FidoComponent. Realize communication between BE and Javascript Client.
 *
 * @author R. Ledzinski
 */
@JavaScript({"fido.js"})
public class FidoComponent extends AbstractJavaScriptComponent
{
    private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoComponent.class);

    private final FidoManagement fidoManagement;

    private final Long entityId;
    private final String userName;

    private FidoComponent(final FidoManagement fidoManagement,
                          final Long entityId,
                          final String userName,
                          final boolean showSuccessNotification,
                          final Consumer<FidoCredentialInfo> newCredentialListener)
    {
        this.fidoManagement = fidoManagement;
        this.entityId = entityId;
        this.userName = userName;

        // Validate registration function
        addFunction("finalizeRegistration", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.info("Invoke finalize registration for reqId={}", arguments.getString(0));
                try
                {
                    FidoCredentialInfo newCred = fidoManagement.createFidoCredentials(arguments.getString(0), arguments.getString(1));
                    if (newCredentialListener != null)
                    {
                        newCredentialListener.accept(newCred);
                    }
                    if (showSuccessNotification)
                    {
                        NotificationPopup.showSuccess("Fido registration", "New FIDO credentials created.");
                    }
                } catch (FidoException e)
                {
                    NotificationPopup.showError("Fido registration failed", e.getLocalizedMessage());
                }
            }
        });

        // Validate authentication function
        addFunction("finalizeAuthentication", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.info("Invoke finalize authentication for reqId={}", arguments.getString(0));
                try
                {
                    fidoManagement.verifyAuthentication(arguments.getString(0), arguments.getString(1));
                    if (showSuccessNotification)
                    {
                        NotificationPopup.showSuccess("Fido authentication", "User successfully authenticated.");
                    }
                } catch (FidoException e)
                {
                    showError("Fido authentication failed", e.getLocalizedMessage());
                }
            }
        });

        // Show error notification function
        addFunction("showError", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                showError(arguments.getString(0), arguments.getString(1));
            }
        });
    }

    public void showError(final String title, final String errorMsg)
    {
        NotificationPopup.showError(title, errorMsg);
    }

    public void invokeRegistration()
    {
        if (isNull(entityId) && isNull(userName))
            throw new IllegalArgumentException("entityId has to be set before using invokeRegistration() method");

        invokeRegistration(userName);
    }

    public void invokeRegistration(final String username)
    {
        try
        {
            AbstractMap.SimpleEntry<String, String> options = fidoManagement.getRegistrationOptions(entityId, username);
            log.debug("reqId={}", options.getKey());
            callFunction("createCredentials", options.getKey(), options.getValue());
        } catch (FidoException e)
        {
            showError("Fido registration failed", e.getLocalizedMessage());
        }
    }

    public void invokeAuthentication(final String username)
    {
        try
        {
            AbstractMap.SimpleEntry<String, String> options = fidoManagement.getAuthenticationOptions(entityId, username);
            log.debug("reqId={}", options.getKey());
            callFunction("getCredentials", options.getKey(), options.getValue());
        } catch (FidoException e)
        {
            showError("Fido registration failed", e.getLocalizedMessage());
        }
    }

    public static FidoComponentBuilder builder(final FidoManagement fidoManagement)
    {
        return new FidoComponentBuilder(fidoManagement);
    }

    public static final class FidoComponentBuilder
    {

        private final FidoManagement fidoService;
        private boolean showSuccessNotification = true;
        private Long entityId;
        private String userName;
        private Consumer<FidoCredentialInfo> newCredentialListener;

        private FidoComponentBuilder(final FidoManagement fidoManagement)
        {
            this.fidoService = fidoManagement;
        }

        public FidoComponentBuilder showSuccessNotification(boolean showSuccessNotification)
        {
            this.showSuccessNotification = showSuccessNotification;
            return this;
        }

        public FidoComponentBuilder entityId(Long entityId)
        {
            this.entityId = entityId;
            return this;
        }

        public FidoComponentBuilder userName(String userName)
        {
            this.userName = userName;
            return this;
        }

        public FidoComponentBuilder newCredentialListener(Consumer<FidoCredentialInfo> newCredentialListener)
        {
            this.newCredentialListener = newCredentialListener;
            return this;
        }

        public FidoComponent build()
        {
            return new FidoComponent(fidoService,
                    entityId,
                    userName,
                    showSuccessNotification,
                    newCredentialListener);
        }
    }
}
