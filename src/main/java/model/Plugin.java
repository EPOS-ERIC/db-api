package model;

import jakarta.persistence.*;
import org.epos.handler.dbapi.service.CacheInvalidationListener;

@Entity
@Table(name = "plugin")
@EntityListeners(CacheInvalidationListener.class)
@Cacheable()
public class Plugin {
    @Id
    @jakarta.validation.constraints.Size(max = 1024)
    @Column(name = "id", nullable = false, length = 1024)
    private String id;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "software_source_code_id", nullable = false, length = 1024)
    private String softwareSourceCodeId;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "software_application_id", nullable = false, length = 1024)
    private String softwareApplicationId;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "version", nullable = false, length = 1024)
    private String version;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "proxy_type", nullable = false, length = 1024)
    private String proxyType;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "runtime", nullable = false, length = 1024)
    private String runtime;

    @jakarta.validation.constraints.Size(max = 1024)
    @jakarta.validation.constraints.NotNull
    @Column(name = "execution", nullable = false, length = 1024)
    private String execution;

    @jakarta.validation.constraints.NotNull
    @Column(name = "installed", nullable = false)
    private Boolean installed = false;

    @jakarta.validation.constraints.NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSoftwareSourceCodeId() {
        return softwareSourceCodeId;
    }

    public void setSoftwareSourceCodeId(String softwareSourceCodeId) {
        this.softwareSourceCodeId = softwareSourceCodeId;
    }

    public String getSoftwareApplicationId() {
        return softwareApplicationId;
    }

    public void setSoftwareApplicationId(String softwareApplicationId) {
        this.softwareApplicationId = softwareApplicationId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getExecution() {
        return execution;
    }

    public void setExecution(String execution) {
        this.execution = execution;
    }

    public Boolean getInstalled() {
        return installed;
    }

    public void setInstalled(Boolean installed) {
        this.installed = installed;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}