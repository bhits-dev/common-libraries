/*******************************************************************************
 * Open Behavioral Health Information Technology Architecture (OBHITA.org)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.samhsa.acs.xdsb.common;

import static gov.samhsa.acs.xdsb.common.XdsbDocumentType.DEPRECATE_PRIVACY_CONSENT;
import static gov.samhsa.acs.xdsb.common.XdsbMetadataGeneratorParams.HomeCommunityId_Parameter_Name;
import static gov.samhsa.acs.xdsb.common.XdsbMetadataGeneratorParams.PatientUniqueId_Parameter_Name;
import static gov.samhsa.acs.xdsb.common.XdsbMetadataGeneratorParams.XdsDocumentEntry_EntryUUID_Parameter_Name;
import static gov.samhsa.acs.xdsb.common.XdsbMetadataGeneratorParams.XdsDocumentEntry_UniqueId_Parameter_Name;
import static gov.samhsa.acs.xdsb.common.XdsbMetadataGeneratorParams.XdsSubmissionSet_UniqueId_Parameter_Name;

import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import gov.samhsa.acs.common.exception.DS4PException;
import gov.samhsa.acs.common.param.Params;
import gov.samhsa.acs.common.param.ParamsBuilder;
import gov.samhsa.acs.common.tool.XmlTransformer;
import gov.samhsa.c2s.common.marshaller.SimpleMarshaller;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;

/**
 * The Class XdsbMetadataGeneratorImpl.
 */
public class XdsbMetadataGeneratorImpl implements XdsbMetadataGenerator {

	/** The Constant XDSB_METADATA_XSL_FILE_NAME_FOR_CLINICAL_DOCUMENT. */
	private static final String XDSB_METADATA_XSL_FILE_NAME_FOR_CLINICAL_DOCUMENT = "XdsbMetadata.xsl";

	/** The Constant XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT. */
	private static final String XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT = "XdsbMetadataForXacmlPolicy.xsl";

	/** The Constant XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT_DEPRECATED. */
	private static final String XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT_DEPRECATED = "XdsbMetadataForXacmlPolicyDeprecated.xsl";

	/** The logger. */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/** The unique oid provider. */
	private final UniqueOidProvider uniqueOidProvider;

	/** The document type for xdsb metadata. */
	private final XdsbDocumentType documentTypeForXdsbMetadata;

	/** The marshaller. */
	private final SimpleMarshaller marshaller;

	/** The xml transformer. */
	private final XmlTransformer xmlTransformer;

	/**
	 * Instantiates a new xdsb metadata generator impl.
	 *
	 * @param uniqueOidProvider
	 *            the unique oid provider
	 * @param documentTypeForXdsbMetadata
	 *            the document type for xdsb metadata
	 * @param marshaller
	 *            the marshaller
	 * @param xmlTransformer
	 *            the xml transformer
	 */
	public XdsbMetadataGeneratorImpl(UniqueOidProvider uniqueOidProvider,
			XdsbDocumentType documentTypeForXdsbMetadata,
			SimpleMarshaller marshaller, XmlTransformer xmlTransformer) {
		this.uniqueOidProvider = uniqueOidProvider;
		this.documentTypeForXdsbMetadata = documentTypeForXdsbMetadata;
		this.marshaller = marshaller;
		this.xmlTransformer = xmlTransformer;
	}

	@Override
	public SubmitObjectsRequest generateMetadata(String document, String homeCommunityId) {

		final String metadataXml = generateMetadataXml(document, homeCommunityId);
		SubmitObjectsRequest submitObjectsRequest = null;
		try {
			submitObjectsRequest = marshaller.unmarshalFromXml(
					SubmitObjectsRequest.class, metadataXml);
		} catch (final JAXBException e) {
			throw new DS4PException(e.toString(), e);
		}

		return submitObjectsRequest;
	}


	@Override
	public String generateMetadataXml(String document, String homeCommunityId) {
		// find xsl
		final String xslUrl = Thread.currentThread().getContextClassLoader()
				.getResource(resolveXslFileName(documentTypeForXdsbMetadata))
				.toString();
		// setup params
		final String xdsDocumentEntryUniqueId = uniqueOidProvider.getOid();
		final String xdsSubmissionSetUniqueId = uniqueOidProvider.getOid();
		final Params params = ParamsBuilder.withParam(
				XdsDocumentEntry_UniqueId_Parameter_Name,
				xdsDocumentEntryUniqueId).and(
				XdsSubmissionSet_UniqueId_Parameter_Name,
				xdsSubmissionSetUniqueId);
		addIfValueHasText(params, HomeCommunityId_Parameter_Name,
				homeCommunityId);

		// assert params
		assertDeprecatePrivacyPolicyConditions(params);
		// transform
		final String metadataXml = xmlTransformer.transform(document, xslUrl,
				Optional.of(params), Optional.empty());
		logger.debug("metadataXml:");
		logger.debug(metadataXml);
		return metadataXml;
	}
	
	private void addIfValueHasText(final Params params,
			final XdsbMetadataGeneratorParams paramName, String paramValue) {
		if (StringUtils.hasText(paramValue)) {
			params.and(paramName, paramValue.replace("'", ""));
		}
	}
	
	private void assertDeprecatePrivacyPolicyConditions(Params params) {
		final String errEntryUUID = "entryUUID can only be injected while deprecating a document. Current Action: "
				+ documentTypeForXdsbMetadata.toString();
		final String errPatientUniqueId = "patientUniqueId can only be injected while deprecating a document. Current Action: "
				+ documentTypeForXdsbMetadata.toString();

		if (documentTypeForXdsbMetadata.equals(DEPRECATE_PRIVACY_CONSENT)) {
			Assert.hasText(
					params.get(XdsDocumentEntry_EntryUUID_Parameter_Name),
					errEntryUUID);
			Assert.hasText(params.get(PatientUniqueId_Parameter_Name),
					errPatientUniqueId);
		} else {
			Assert.isNull(
					params.get(XdsDocumentEntry_EntryUUID_Parameter_Name),
					errEntryUUID);
			Assert.isNull(params.get(PatientUniqueId_Parameter_Name),
					errPatientUniqueId);
		}
	}
	
	private String resolveXslFileName(
			XdsbDocumentType documentTypeForXdsbMetadata) {

		switch (documentTypeForXdsbMetadata) {
		case CLINICAL_DOCUMENT:
			return XDSB_METADATA_XSL_FILE_NAME_FOR_CLINICAL_DOCUMENT;
		case PRIVACY_CONSENT:
			return XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT;
		case DEPRECATE_PRIVACY_CONSENT:
			return XDSB_METADATA_XSL_FILE_NAME_FOR_PRIVACY_CONSENT_DEPRECATED;
		default:
			throw new DS4PException(
					"Unsupported document type for XdsbMetadataGenerator.");
		}
	}

	
}
