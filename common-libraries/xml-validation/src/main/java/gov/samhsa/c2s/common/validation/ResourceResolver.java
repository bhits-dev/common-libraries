/*******************************************************************************
 * Open Behavioral Health Information Technology Architecture (OBHITA.org)
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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
package gov.samhsa.c2s.common.validation;

import gov.samhsa.c2s.common.log.Logger;
import gov.samhsa.c2s.common.log.LoggerFactory;
import gov.samhsa.c2s.common.filereader.FileReader;
import gov.samhsa.c2s.common.filereader.FileReaderImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * The Class ResourceResolver.
 */
public class ResourceResolver implements LSResourceResolver {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The schema base path. */
    private final String schemaBasePath;

    /** The path map. */
    private Map<String, String> pathMap;

    /** The file reader. */
    private FileReader fileReader;

    /**
     * Instantiates a new resource resolver.
     *
     * @param schemaBasePath the schema base path
     */
    public ResourceResolver(String schemaBasePath) {
        this.schemaBasePath = schemaBasePath;
        this.pathMap = createPathMap();
        this.fileReader = new FileReaderImpl();
        logger.debug(() -> new StringBuilder()
                .append("This LSResourceResolver implementation assumes that all XSD files have a unique name. ")
                .append("If you have some XSD files with same name but different content (at different paths) in your schema structure, ")
                .append("this resolver will fail to include the other XSD files except the first one found.")
                .toString());
    }

    /* (non-Javadoc)
     * @see org.w3c.dom.ls.LSResourceResolver#resolveResource(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId, String baseURI) {
        // The base resource that includes this current resource
        String baseResourceName = null;
        String baseResourcePath = null;
        // Extract the current resource name
        String currentResourceName = systemId.substring(systemId
                .lastIndexOf("/") + 1);

        // If this resource hasn't been added yet
        if (!pathMap.containsKey(currentResourceName)) {
            if (baseURI != null) {
                baseResourceName = baseURI
                        .substring(baseURI.lastIndexOf("/") + 1);
            }

            // we dont need "./" since getResourceAsStream cannot understand it
            if (systemId.startsWith("./")) {
                systemId = systemId.substring(2, systemId.length());
            }

            // If the baseResourcePath has already been discovered, get that
            // from pathMap
            if (pathMap.containsKey(baseResourceName)) {
                baseResourcePath = pathMap.get(baseResourceName);
            } else {
                // The baseResourcePath should be the schemaBasePath
                baseResourcePath = schemaBasePath;
            }

            // Read the resource as input stream
            String normalizedPath = getNormalizedPath(baseResourcePath, systemId);
            InputStream resourceAsStream = getResourceAsStream(normalizedPath);

            // if the current resource is not in the same path with base
            // resource, add current resource's path to pathMap
            if (systemId.contains("/")) {
                pathMap.put(currentResourceName, normalizedPath.substring(0, normalizedPath.lastIndexOf("/") + 1));
            } else {
                // The current resource should be at the same path as the base
                // resource
                pathMap.put(systemId, baseResourcePath);
            }
            Scanner s = setScanner(resourceAsStream);
            String s1 = s.next().replaceAll("\\n", " ") // the parser cannot understand elements broken down multiple lines e.g. (<xs:element \n name="buxing">)
                    .replace("\\t", " ") // these two about whitespaces is only for decoration
                    .replaceAll("\\s+", " ").replaceAll("[^\\x20-\\x7e]", ""); // some files has a special character as a first character indicating utf-8 file
            InputStream is = new ByteArrayInputStream(s1.getBytes());
            s.close();
            return new LSInputImpl(publicId, systemId, is, fileReader);
        }

        // If this resource has already been added, do not add the same resource again. It throws
        // "org.xml.sax.SAXParseException: sch-props-correct.2: A schema cannot contain two global components with the same name; this schema contains two occurrences of ..."
        // return null instead.
        return null;
    }

    Scanner setScanner(InputStream resourceAsStream) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(resourceAsStream).useDelimiter("\\A");
        return s;
    }

    InputStream getResourceAsStream(String normalizedPath) {
        InputStream resourceAsStream = this.getClass().getClassLoader()
                .getResourceAsStream(normalizedPath);
        return resourceAsStream;
    }

    HashMap<String, String> createPathMap() {
        return new HashMap<String, String>();
    }

    /**
     * Gets the normalized path.
     *
     * @param basePath the base path
     * @param relativePath the relative path
     * @return the normalized path
     */
    private String getNormalizedPath(String basePath, String relativePath) {
        if (!relativePath.startsWith("../")) {
            StringBuilder builder = new StringBuilder();
            builder.append(basePath);
            builder.append(relativePath);
            return builder.toString();
        } else {
            while (relativePath.startsWith("../")) {
                basePath = basePath.substring(0, basePath.substring(0, basePath.length() - 1).lastIndexOf("/") + 1);
                relativePath = relativePath.substring(3);
            }
            StringBuilder builder = new StringBuilder();
            builder.append(basePath);
            builder.append(relativePath);
            return builder.toString();
        }
    }
}
