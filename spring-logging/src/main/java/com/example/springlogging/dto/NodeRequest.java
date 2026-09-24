package com.example.springlogging.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class NodeRequest {

    @NotBlank(message = "runNodeID is required")
    @JsonProperty("runNodeID")
    private String runNodeID;

    @NotBlank(message = "correlationId is required")
    @JsonProperty("correlationId")
    @JsonAlias({"cooreleationId", "correlation_id"})
    private String correlationId;

    @NotBlank(message = "address is required")
    private String address;

    public String getRunNodeID() {
        return runNodeID;
    }

    public void setRunNodeID(String runNodeID) {
        this.runNodeID = runNodeID;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
