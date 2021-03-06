package gov.samhsa.c2s.common.document.transformer;

import gov.samhsa.c2s.common.param.Params;
import org.w3c.dom.Document;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import java.util.Optional;

/**
 * The Interface XmlTransformer.
 */
public interface XmlTransformer {

    /**
     * Transform.
     *
     * @param xmlDocument the xml document
     * @param xslSource   the xsl source
     * @param params      the params
     * @param uriResolver the uri resolver
     * @return the string
     */
    String transform(Document xmlDocument, Source xslSource,
                     Optional<Params> params, Optional<URIResolver> uriResolver);

    /**
     * Transform.
     *
     * @param xmlDocument the xml document
     * @param xslFileName the xsl file name
     * @param params      the params
     * @param uriResolver the uri resolver
     * @return the string
     */
    String transform(Document xmlDocument, String xslFileName,
                     Optional<Params> params, Optional<URIResolver> uriResolver);

    /**
     * Transform.
     *
     * @param obj         the obj
     * @param xslFileName the xsl file name
     * @param params      the params
     * @param uriResolver the uri resolver
     * @return the string
     */
    String transform(Object obj, String xslFileName,
                     Optional<Params> params, Optional<URIResolver> uriResolver);

    /**
     * Transform.
     *
     * @param xmlSource   the xml source
     * @param xslSource   the xsl source
     * @param params      the params
     * @param uriResolver the uri resolver
     * @return the string
     */
    String transform(Source xmlSource, Source xslSource,
                     Optional<Params> params, Optional<URIResolver> uriResolver);

    /**
     * Transform.
     *
     * @param xml         the xml
     * @param xslFileName the xsl file name
     * @param params      the params
     * @param uriResolver the uri resolver
     * @return the string
     */
    String transform(String xml, String xslFileName,
                     Optional<Params> params, Optional<URIResolver> uriResolver);
}