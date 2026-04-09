package co.razkevich.sflocalstack.helpers;

public final class SoapTestHelper {

    private SoapTestHelper() {}

    public static String envelope(String operation, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    %s
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(body);
    }

    public static String describeMetadata() {
        return envelope("describeMetadata", "<met:describeMetadata/>");
    }

    public static String listMetadata(String type) {
        return envelope("listMetadata", """
                <met:listMetadata>
                  <met:queries>
                    <met:type>%s</met:type>
                  </met:queries>
                  <met:asOfVersion>60.0</met:asOfVersion>
                </met:listMetadata>
                """.formatted(type));
    }

    public static String readMetadata(String type, String... fullNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("<met:readMetadata>\n");
        sb.append("  <met:type>").append(type).append("</met:type>\n");
        for (String fullName : fullNames) {
            sb.append("  <met:fullNames>").append(fullName).append("</met:fullNames>\n");
        }
        sb.append("</met:readMetadata>");
        return envelope("readMetadata", sb.toString());
    }

    public static String deploy(String zipBase64) {
        return envelope("deploy", """
                <met:deploy>
                  <met:ZipFile>%s</met:ZipFile>
                </met:deploy>
                """.formatted(zipBase64));
    }

    public static String checkDeployStatus(String asyncId) {
        return envelope("checkDeployStatus", """
                <met:checkDeployStatus>
                  <met:asyncProcessId>%s</met:asyncProcessId>
                  <met:includeDetails>true</met:includeDetails>
                </met:checkDeployStatus>
                """.formatted(asyncId));
    }

    public static String retrieve(String type, String... members) {
        StringBuilder membersXml = new StringBuilder();
        for (String member : members) {
            membersXml.append("        <met:members>").append(member).append("</met:members>\n");
        }
        return envelope("retrieve", """
                <met:retrieve>
                  <met:retrieveRequest>
                    <met:apiVersion>60.0</met:apiVersion>
                    <met:unpackaged>
                      <met:types>
                %s        <met:name>%s</met:name>
                      </met:types>
                    </met:unpackaged>
                  </met:retrieveRequest>
                </met:retrieve>
                """.formatted(membersXml, type));
    }

    public static String checkRetrieveStatus(String asyncId) {
        return envelope("checkRetrieveStatus", """
                <met:checkRetrieveStatus>
                  <met:asyncProcessId>%s</met:asyncProcessId>
                  <met:includeZip>true</met:includeZip>
                </met:checkRetrieveStatus>
                """.formatted(asyncId));
    }

    public static String cancelDeploy(String asyncId) {
        return envelope("cancelDeploy", """
                <met:cancelDeploy>
                  <met:asyncProcessId>%s</met:asyncProcessId>
                </met:cancelDeploy>
                """.formatted(asyncId));
    }
}
