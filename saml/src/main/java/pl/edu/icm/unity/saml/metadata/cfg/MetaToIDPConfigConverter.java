/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.RequestAcceptancePolicy;
import pl.edu.icm.unity.saml.idp.SamlEntityId;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import pl.edu.icm.unity.saml.idp.TrustedServiceProviders;
import pl.edu.icm.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.metadata.*;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesDocument;
import xmlbeans.org.oasis.saml2.metadata.extattribute.EntityAttributesType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;
import xmlbeans.org.w3.x2000.x09.xmldsig.X509DataType;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class MetaToIDPConfigConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetaToIDPConfigConverter.class);
	private static final String IDP_META_CERT = "_IDP_METADATA_CERT_";
	private final PKIManagement pkiManagement;
	private final MessageSource msg;

	public MetaToIDPConfigConverter(PKIManagement pkiManagement, MessageSource msg)
	{
		this.pkiManagement = pkiManagement;
		this.msg = msg;
	}
	protected Set<TrustedServiceProvider> convertToTrustedSps(EntitiesDescriptorDocument metadata, SAMLIdPConfiguration samlIdPConfiguration)
	{
		EntitiesDescriptorType meta = metadata.getEntitiesDescriptor();
		Set<TrustedServiceProvider> overrideConfigurations = new HashSet<>();
		for (EntityDescriptorType descriptorType : meta.getEntityDescriptorArray())
		{
			SPSSODescriptorType[] spDefs = descriptorType.getSPSSODescriptorArray();
			RequestAcceptancePolicy trustMode = samlIdPConfiguration.spAcceptPolicy;
			if (spDefs == null || spDefs.length == 0)
				continue;

			SamlEntityId entityId = new SamlEntityId(descriptorType.getEntityID(), null);
			for (SPSSODescriptorType spDef: spDefs)
			{
				TrustedServiceProvider spConfig = samlIdPConfiguration.trustedServiceProviders.getSPConfig(entityId);
				if(spConfig != null && spConfig.allowedKey != null)
				{
					log.trace("SP of entity " + entityId +	" is configured in property, so cannot be overwrite.");
					continue;
				}
				if (!MetaToConfigConverterHelper.supportsSaml2(spDef))
				{
					log.trace("SP of entity " + entityId +	" doesn't support SAML2 - ignoring.");
					continue;
				}
				EntityAttributesType entityAttributes = parseMDAttributes(descriptorType.getExtensions(), entityId);
				if (isDisabled(entityAttributes))
				{
					log.trace("SP of entity " + entityId +	" is hidden from discovery - ignoring.");
					continue;
				}

				KeyDescriptorType[] keys = spDef.getKeyDescriptorArray();
				List<X509Certificate> certs = getSigningCerts(keys, entityId);
				if (!certs.isEmpty())
				{
					try
					{
						updatePKICerts(certs, entityId, IDP_META_CERT);
					} catch (EngineException e)
					{
						log.error("Adding remote SPs certs to local certs store failed, "
								+ "skipping IdP: " + entityId, e);
						continue;
					}
				} else if (trustMode == RequestAcceptancePolicy.strict)
				{
					log.info("No signing certificate found for SP, skipping it as "
							+ "the 'strict' trust model is used: " + entityId);
					continue;
				}

				Map<Integer, String> endpointURLs = getEndpointURLs(spDef.getAssertionConsumerServiceArray(),
						SAMLConstants.BINDING_HTTP_POST);
				String defaultEndpoint = getDefaultEndpoint(spDef.getAssertionConsumerServiceArray(),
						SAMLConstants.BINDING_HTTP_POST);
				if (defaultEndpoint == null || endpointURLs.isEmpty())
					continue;

				EndpointType redirectSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(),
						SAMLConstants.BINDING_HTTP_REDIRECT);
				EndpointType postSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(),
						SAMLConstants.BINDING_HTTP_POST);
				EndpointType soapSLOEndpoint = selectEndpointByBinding(spDef.getSingleLogoutServiceArray(),
						SAMLConstants.BINDING_SOAP);

				UIInfoType uiInfo = MetaToConfigConverterHelper.parseMDUIInfo(spDef.getExtensions(), entityId.id);
				I18nString names = MetaToConfigConverterHelper.getLocalizedNamesAsI18nString(msg, uiInfo, spDef, descriptorType);
				I18nString logos = MetaToConfigConverterHelper.getLocalizedLogosAsI18nString(uiInfo);

				TrustedServiceProvider trustedServiceProvider = generateOverrodeSP(entityId, defaultEndpoint, endpointURLs,
						soapSLOEndpoint, postSLOEndpoint, redirectSLOEndpoint,
						samlIdPConfiguration.trustedServiceProviders, certs, names, logos);
				overrideConfigurations.add(trustedServiceProvider);
			}
		}
		return overrideConfigurations;
	}


	private boolean isDisabled(EntityAttributesType entityAttributes)
	{
		if (entityAttributes == null)
			return false;
		AttributeType[] attributeArray = entityAttributes.getAttributeArray();
		for (AttributeType a: attributeArray)
		{
			if ("http://macedir.org/entity-category".equals(a.getName()))
			{
				for (XmlObject value : a.getAttributeValueArray())
				{
					XmlCursor c = value.newCursor();
					String valueStr = c.getTextValue();
					c.dispose();
					if (valueStr.equals("http://refeds.org/category/hide-from-discovery"))
						return true;
				}
			}
		}
		return false;
	}

	private EntityAttributesType parseMDAttributes(ExtensionsType extensions, SamlEntityId entityId)
	{
		if (extensions == null)
			return null;
		NodeList nl = extensions.getDomNode().getChildNodes();
		for (int i=0; i<nl.getLength(); i++)
		{
			Node elementN = nl.item(i);
			if (elementN.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element = (Element) elementN;
			if ("EntityAttributes".equals(element.getLocalName()) &&
					"urn:oasis:names:tc:SAML:metadata:attribute".equals(element.getNamespaceURI()))
			{
				try
				{
					return EntityAttributesDocument.Factory.parse(element).getEntityAttributes();
				} catch (XmlException e)
				{
					log.warn("Can not parse entity attributes metadata extension for " + entityId, e);
				}
			}
		}
		return null;
	}

	private void updatePKICerts(List<X509Certificate> certs, SamlEntityId entityId, String prefix)
			throws EngineException
	{
		synchronized (pkiManagement)
		{
			for (X509Certificate cert : certs)
			{
				String pkiKey = getCertificateKey(cert, entityId, prefix);
				try
				{
					X509Certificate existingCert = pkiManagement.getCertificate(pkiKey).value;
					if (!existingCert.equals(cert))
					{
						pkiManagement.updateCertificate(new NamedCertificate(pkiKey, cert));
						log.debug("Updated already installed certificate of SAML entity {}, DN: {}, serial: {}",
								entityId, cert.getSubjectX500Principal().getName(),
								cert.getSerialNumber());
					}
				} catch (IllegalArgumentException e)
				{
					pkiManagement.addVolatileCertificate(pkiKey, cert);
					log.debug("Installed a new certificate for SAML entity {}, DN: {}, serial: {}",
							entityId, cert.getSubjectX500Principal().getName(),
							cert.getSerialNumber());
				}
			}
		}
	}

	private List<X509Certificate> getSigningCerts(KeyDescriptorType[] keys, SamlEntityId entityId)
	{
		List<X509Certificate> ret = new ArrayList<>();
		for (KeyDescriptorType key: keys)
		{
			if (!key.isSetUse() || KeyTypes.SIGNING.equals(key.getUse()))
			{
				X509DataType[] x509Keys = key.getKeyInfo().getX509DataArray();
				if (x509Keys == null || x509Keys.length == 0)
				{
					log.info("Key in SAML metadata is ignored as it doesn't contain "
							+ "X.509 certificate. Entity " + entityId);
					continue;
				}
				for (X509DataType x509Key: x509Keys)
				{
					byte[][] certsAsBytes = x509Key.getX509CertificateArray();
					X509Certificate cert;
					try
					{
						CertificateFactory instance = CertificateFactory.getInstance("X.509");
						cert = (X509Certificate) instance.generateCertificate(new ByteArrayInputStream(certsAsBytes[0]));
					} catch (CertificateException e)
					{
						log.warn("Can not load/parse a certificate from metadata of " + entityId
								+ ", ignoring it", e);
						continue;
					}
					ret.add(cert);
				}
			}
		}
		return ret;
	}
	private TrustedServiceProvider generateOverrodeSP(SamlEntityId entityId, String defaultServiceEndpoint,
	                                                  Map<Integer, String> indexedServiceEndpoints,
	                                                  EndpointType sloSoapEndpoint, EndpointType sloPostEndpoint, EndpointType sloRedirectEndpoint,
	                                                  TrustedServiceProviders providers,
	                                                  List<X509Certificate> certs, I18nString names, I18nString logos)
	{
		TrustedServiceProvider got = providers.getSPConfig(entityId);
		TrustedServiceProvider.TrustedServiceProviderConfigurationBuilder builder;

		boolean noPerSpConfig = got == null;
		if (got == null)
			builder = TrustedServiceProvider.builder();
		else
			builder = got.copyToBuilder();

		if (noPerSpConfig)
		{
			builder.withEntityId(entityId.id);
			builder.withReturnUrl(defaultServiceEndpoint);

			Set<String> urls = indexedServiceEndpoints.entrySet().stream()
					.map(entry -> "[" + entry.getKey() + "]" + entry.getValue())
					.collect(Collectors.toSet());
			builder.withReturnUrls(urls);
			builder.withReturnUrl(defaultServiceEndpoint);
			if (sloSoapEndpoint != null)
			{
				builder.withSoapLogoutUrl(sloSoapEndpoint.getLocation());
			}
			if (sloPostEndpoint != null)
			{
				builder.withPostLogoutUrl(sloPostEndpoint.getLocation());
				if (sloPostEndpoint.getResponseLocation() != null)
					builder.withPostLogoutRetUrl(sloPostEndpoint.getResponseLocation());
			}
			if (sloRedirectEndpoint != null)
			{
				builder.withRedirectLogoutUrl(sloRedirectEndpoint.getLocation());
				if (sloRedirectEndpoint.getResponseLocation() != null)
					builder.withRedirectLogoutRetUrl(sloRedirectEndpoint.getResponseLocation());
			}
			Set<String> certificates = certs.stream()
					.map(cert -> getCertificateKey(cert, entityId, IDP_META_CERT))
					.collect(Collectors.toSet());
			builder.withCertificateNames(certificates);
			builder.withCertificates(certificates.stream().map(this::getCertificate).collect(Collectors.toSet()));
			builder.withName(names);
			builder.withLogoUri(logos);
		}
		return builder.build();
	}

	private X509Certificate getCertificate(String certificateName)
	{
		try
		{
			return pkiManagement.getCertificate(certificateName).value;
		} catch (EngineException e)
		{
			throw new InternalException("Can't retrieve SAML credential", e);
		}
	}

	private static String getCertificateKey(X509Certificate cert, SamlEntityId entityId, String prefix)
	{
		String dn = X500NameUtils.getComparableForm(cert.getSubjectX500Principal().getName());
		String serial = cert.getSerialNumber().toString();
		return prefix + DigestUtils.md5Hex(entityId.id) + "#" + DigestUtils.md5Hex(dn) + "#" + serial;
	}

	private EndpointType selectEndpointByBinding(EndpointType[] endpoints, String binding)
	{
		for (EndpointType endpoint: endpoints)
		{
			if (endpoint.getBinding() == null || endpoint.getLocation() == null)
				continue;
			
			if(endpoint.getBinding().equals(binding))
				return endpoint;
		}
		return null;
	}
	
	private String getDefaultEndpoint(IndexedEndpointType[] assertionConsumerServiceArray, String binding)
	{
		Optional<IndexedEndpointType> explicitDefault = Arrays.stream(assertionConsumerServiceArray)
				.filter(e -> binding.equals(e.getBinding()))
				.filter(IndexedEndpointType::getIsDefault)
				.findFirst();
		EndpointType endpoint = explicitDefault.isPresent() ? explicitDefault.get() 
				: selectEndpointByBinding(assertionConsumerServiceArray, binding);
		return endpoint == null ? null : endpoint.getLocation();
	}

	private Map<Integer, String> getEndpointURLs(IndexedEndpointType[] assertionConsumerServiceArray, String binding)
	{
		return Arrays.stream(assertionConsumerServiceArray)
				.filter(e -> binding.equals(e.getBinding()))
				.collect(Collectors.toMap(e -> e.getIndex(), e -> e.getLocation()));
	}
}
