package com.ai.st.microservice.ili.dto;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Ili2pgExportDto", description = "Ili2pg Export")
public class Ili2pgExportDto implements Serializable {

	private static final long serialVersionUID = -4791993304492124731L;

	@ApiModelProperty(required = true, notes = "Database host")
	private String databaseHost;

	@ApiModelProperty(required = true, notes = "Database port")
	private String databasePort;

	@ApiModelProperty(required = true, notes = "Database schema")
	private String databaseSchema;

	@ApiModelProperty(required = true, notes = "Database username")
	private String databaseUsername;

	@ApiModelProperty(required = true, notes = "Database password")
	private String databasePassword;

	@ApiModelProperty(required = true, notes = "Database name")
	private String databaseName;

	@ApiModelProperty(required = true, notes = "Path file export")
	private String pathFileXTF;

	@ApiModelProperty(required = true, notes = "Integration ID")
	private Long integrationId;

	@ApiModelProperty(required = true, notes = "its required stats?")
	private Boolean withStats;

	@ApiModelProperty(required = false, notes = "Model version")
	private String versionModel;

	public Ili2pgExportDto() {
		this.versionModel = "2.9.4";
	}

	public String getDatabaseHost() {
		return databaseHost;
	}

	public void setDatabaseHost(String databaseHost) {
		this.databaseHost = databaseHost;
	}

	public String getDatabasePort() {
		return databasePort;
	}

	public void setDatabasePort(String databasePort) {
		this.databasePort = databasePort;
	}

	public String getDatabaseSchema() {
		return databaseSchema;
	}

	public void setDatabaseSchema(String databaseSchema) {
		this.databaseSchema = databaseSchema;
	}

	public String getDatabaseUsername() {
		return databaseUsername;
	}

	public void setDatabaseUsername(String databaseUsername) {
		this.databaseUsername = databaseUsername;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public void setDatabasePassword(String databasePassword) {
		this.databasePassword = databasePassword;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getPathFileXTF() {
		return pathFileXTF;
	}

	public void setPathFileXTF(String pathFileXTF) {
		this.pathFileXTF = pathFileXTF;
	}

	public Long getIntegrationId() {
		return integrationId;
	}

	public void setIntegrationId(Long integrationId) {
		this.integrationId = integrationId;
	}

	public Boolean getWithStats() {
		return withStats;
	}

	public void setWithStats(Boolean withStats) {
		this.withStats = withStats;
	}

	public String getVersionModel() {
		return versionModel;
	}

	public void setVersionModel(String versionModel) {
		this.versionModel = versionModel;
	}

}
