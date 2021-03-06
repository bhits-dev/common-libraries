package gov.samhsa.c2s.common.consentgen;

public enum XslResource {

    CDAR2XSLNAME("c2cdar2.xsl"), XACMLXSLNAME("c2xacml.xsl"), CDAR2CONSENTDIRECTIVEXSLNAME("c2consentDirective.xsl"), XACMLPDFCONSENTFROMXSLNAME(
            "c2xacmlpdfConsentFrom.xsl"), XACMLPDFCONSENTTOXSLNAME(
            "c2xacmlpdfConsentTo.xsl");

    private String fileName;

    XslResource(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }
}
