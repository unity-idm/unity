/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Implementation of the endpoint management. Currently only web application endpoints are supported.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class EndpointManagementImpl implements EndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, EndpointManagementImpl.class);
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private InternalEndpointManagement internalManagement;
	private EndpointsUpdater endpointsUpdater;
	private EndpointInstanceLoader endpointInstanceLoader;
	private InternalAuthorizationManager authz;
	private EndpointDB endpointDB;
	private RealmDB realmDB;
	private TransactionalRunner tx;
	private InternalCapacityLimitVerificator capacityLimitVerificator;
	
	@Autowired
	public EndpointManagementImpl(EndpointFactoriesRegistry endpointFactoriesReg,
			InternalEndpointManagement internalManagement,
			EndpointsUpdater endpointsUpdater,
			EndpointInstanceLoader endpointInstanceLoader, InternalAuthorizationManager authz,
			EndpointDB endpointDB, RealmDB realmDB, TransactionalRunner tx, InternalCapacityLimitVerificator capacityLimitVerificator)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.internalManagement = internalManagement;
		this.endpointsUpdater = endpointsUpdater;
		this.endpointInstanceLoader = endpointInstanceLoader;
		this.authz = authz;
		this.endpointDB = endpointDB;
		this.realmDB = realmDB;
		this.tx = tx;
		this.capacityLimitVerificator = capacityLimitVerificator;
	}

	@Override
	public List<EndpointTypeDescription> getEndpointTypes() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return endpointFactoriesReg.getDescriptions();
	}

	/**
	 * Deploys an endpoint as follows:
	 *  {@link EndpointFactory} is searched in registry by id
	 *  It is used to create a new endpoint instance
	 *  the instance is configured with JSON and context address
	 *  transaction is started
	 *  instance state is added to DB
	 *  instance is deployed into web server
	 *  transaction is committed
	 */
	@Override
	@Transactional
	public ResolvedEndpoint deploy(String typeId, String endpointName,
			String address, EndpointConfiguration configuration) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.EndpointsCount,
				() -> endpointDB.getCount());

		synchronized(internalManagement)
		{
			return deployInt(typeId, endpointName, address, configuration);
		}
	}

	private ResolvedEndpoint deployInt(String typeId, String endpointName,
			String address, EndpointConfiguration configuration) throws EngineException
	{
		log.info("Will deploy endpoint " + endpointName + " [" + typeId +"] at " + address);
		if (log.isTraceEnabled())
			log.trace("New endpoint configuration: " + configuration);
		EndpointFactory factory = endpointFactoriesReg.getById(typeId);
		if (factory == null)
			throw new WrongArgumentException("Endpoint type " + typeId + " is unknown");
		EndpointPathValidator.validateEndpointPath(address);
		EndpointInstance endpointInstance;
		try
		{
			Endpoint endpoint = new Endpoint(endpointName, typeId, address, configuration, 0);
			endpointInstance = endpointInstanceLoader.createEndpointInstance(endpoint);

			verifyAuthenticators(endpointInstance.getAuthenticationFlows(), 
					factory.getDescription().getSupportedBinding());
			
			Endpoint endpointExisting = getEndpointInt(endpointName);
			if (endpointExisting != null)
			{
				if (endpointExisting.getState().equals(EndpointState.DEPLOYED))
				{
					throw new EngineException("The [" + endpointName + "] endpoint already exists");
				}
				endpointDB.update(endpoint);
			}else
			{
				endpointDB.create(endpoint);
			}
			
			
		} catch (Exception e)
		{
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		}

		try
		{
			internalManagement.deploy(endpointInstance);
			log.info("Endpoint " + endpointName + " successfully deployed");
		} catch (Exception e)
		{
			if (endpointInstance.getEndpointDescription() != null)
				internalManagement.undeploy(endpointInstance.getEndpointDescription().getName());
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		}
		return endpointInstance.getEndpointDescription();
	}
	
	private Endpoint getEndpointInt(String name)
	{
		try
		{
			return endpointDB.get(name);
		} catch (IllegalArgumentException e)
		{
			return null;
		}
	}
	
	private void verifyAuthenticators(List<AuthenticationFlow> authenticators, String supported) throws WrongArgumentException
	{
		for (AuthenticationFlow auths: authenticators)
			auths.checkIfAuthenticatorsAreAmongSupported(Sets.newHashSet(supported));
	}

	@Override
	public List<ResolvedEndpoint> getDeployedEndpoints() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		List<EndpointInstance> endpoints = internalManagement.getDeployedEndpoints();
		List<ResolvedEndpoint> ret = new ArrayList<ResolvedEndpoint>(endpoints.size());
		for (EndpointInstance endpI: endpoints)
			ret.add(endpI.getEndpointDescription());
		return ret;
	}
	
	@Override
	public List<EndpointInstance> getDeployedEndpointInstances() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return internalManagement.getDeployedEndpoints();
	}
	
	@Override
	@Transactional
	public List<Endpoint> getEndpoints() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return endpointDB.getAll();
	}
	
	@Override
	@Transactional
	public Endpoint getEndpoint(String name) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return endpointDB.get(name);
	}
	
	@Override
	@Transactional
	public void removeEndpoint(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		endpointDB.delete(id);
		endpointsUpdater.update();		
	}

	@Override
	public void undeploy(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			undeployInt(id);
		}
	}

	private void undeployInt(String id) throws EngineException
	{
		log.info("Will undeploy endpoint " + id);
		tx.runInTransactionThrowing(() -> {
			try
			{
				Endpoint existing = endpointDB.get(id);
				Endpoint updatedEndpoint = new Endpoint(id, 
						existing.getTypeId(), 
						existing.getContextAddress(), 
						existing.getConfiguration(),
						existing.getRevision() + 1, EndpointState.UNDEPLOYED);
				endpointDB.update(updatedEndpoint);
			} catch (Exception e)
			{
				throw new EngineException("Unable to undeploy an endpoint: " + e.getMessage(), e);
			}
		});
		endpointsUpdater.update();
	}	
	
	@Override
	public void updateEndpoint(String id, EndpointConfiguration configuration) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			updateEndpointInt(id, configuration);
		}
	}

	/**
	 * -) Get from DB
	 * -) create a new instance
	 * -) in the new instance set all fields to the new ones if not null or to the existing values
	 * -) serialize and store in db
	 * -) trigger runtime system update.
	 */
	private void updateEndpointInt(String id, EndpointConfiguration configuration) throws EngineException
	{
		log.info("Will update configuration of endpoint " + id);
		if (log.isTraceEnabled())
			log.trace("Updated endpoint configuration: " + configuration);
		tx.runInTransactionThrowing(() -> {
			try
			{
				Endpoint existing = endpointDB.get(id);
				String endpointTypeId = existing.getTypeId();

				String jsonConf = (configuration.getConfiguration() != null) ?
						configuration.getConfiguration() :
						existing.getConfiguration().getConfiguration();
				String newDesc = (configuration.getDescription() != null) ?
						configuration.getDescription() :
						existing.getConfiguration().getDescription();

				List<String> newAuthn = 
						(configuration.getAuthenticationOptions() != null) ?
							configuration.getAuthenticationOptions() :
							existing.getConfiguration().getAuthenticationOptions();

				String newRealm = (configuration.getRealm() != null) ?
						configuration.getRealm() :
						existing.getConfiguration().getRealm();

				String realmName = newRealm !=null ? realmDB.get(newRealm).getName() : null;

				I18nString newDisplayedName = configuration.getDisplayedName() != null ?
						configuration.getDisplayedName() :
						existing.getConfiguration().getDisplayedName();

				EndpointConfiguration newConfiguration = new EndpointConfiguration(
						newDisplayedName, 
						newDesc, 
						newAuthn, 
						jsonConf, 
						realmName, configuration.getTag());
				
				
				Endpoint updatedEndpoint = new Endpoint(id, 
						endpointTypeId, 
						existing.getContextAddress(), 
						newConfiguration,
						existing.getRevision() + 1,
						existing.getState());
				endpointDB.update(updatedEndpoint);
				log.info("Endpoint " + id + " successfully updated in DB");
			} catch (Exception e)
			{
				throw new EngineException("Unable to reconfigure an endpoint: " + e.getMessage(), e);
			}

		});
		endpointsUpdater.updateManual();
	}
}
