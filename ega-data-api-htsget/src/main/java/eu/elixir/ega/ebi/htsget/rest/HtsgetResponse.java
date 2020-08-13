package eu.elixir.ega.ebi.htsget.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("htsget")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class HtsgetResponse {
    private String format;
    private HtsgetUrl[] urls;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String md5;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public HtsgetUrl[] getUrls() {
        return urls;
    }

    public void setUrls(HtsgetUrl[] urls) {
        this.urls = urls;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

}
