package eu.elixir.ega.ebi.htsget.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("htsget")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class HtsgetResponseV2 {
    private String format;
    private List<HtsgetUrlV2> urls;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String md5;

    public HtsgetResponseV2(String format) {
        this.format = format;
        this.urls = new ArrayList<>();
    }

    public String getFormat() {
        return format;
    }

    public List<HtsgetUrlV2> getUrls() {
        return urls;
    }

    public void addUrl(HtsgetUrlV2 url) {
        this.urls.add(url);
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
