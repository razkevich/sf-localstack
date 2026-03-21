package co.razkevich.sflocalstack.model;

import java.util.List;

public class SalesforceError {

    private String message;
    private String errorCode;
    private List<String> fields;

    public SalesforceError() {}

    public SalesforceError(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
        this.fields = List.of();
    }

    public SalesforceError(String message, String errorCode, List<String> fields) {
        this.message = message;
        this.errorCode = errorCode;
        this.fields = fields;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }
}
