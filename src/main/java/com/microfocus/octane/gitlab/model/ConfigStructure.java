package com.microfocus.octane.gitlab.model;

import javafx.util.Pair;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class ConfigStructure {

    @Value("${ciserver.identity:#{null}}")
    private String ciServerIdentity;

    @Value("${octane.location:#{null}}")
    private String octaneLocation;

    private String octaneSharedspace;

    @Value("${octane.apiClientID:#{null}}")
    private String octaneApiClientID;

    @Value("${octane.apiClientSecret:#{null}}")
    private String octaneApiClientSecret;

    @Value("${gitlab.location:#{null}}")
    private String gitlabLocation;

    @Value("${gitlab.personalAccessToken:#{null}}")
    private String gitlabPersonalAccessToken;

    @Value("${gitlab.testResultsFilePattern:**.xml}")
    private String gitlabTestResultsFilePattern;

    @Value("${server.baseUrl:#{null}}")
    private String serverBaseUrl;

    @Value("${http.proxyUrl:#{null}}")
    private String httpProxyUrl;

    @Value("${http.proxyUser:#{null}}")
    private String httpProxyUser;

    @Value("${http.proxyPassword:#{null}}")
    private String httpProxyPassword;

    @Value("${http.nonProxyHosts:#{null}}")
    private String httpNonProxyHosts;

    @Value("${https.proxyUrl:#{null}}")
    private String httpsProxyUrl;

    @Value("${https.proxyUser:#{null}}")
    private String httpsProxyUser;

    @Value("${https.proxyPassword:#{null}}")
    private String httpsProxyPassword;

    @Value("${https.nonProxyHosts:#{null}}")
    private String httpsNonProxyHosts;

    @PostConstruct
    public void init() throws URISyntaxException {
        List<Pair<String, Supplier<String>>> mandatoryGetters = new ArrayList<>();
        mandatoryGetters.add(new Pair<>("octane.location", this::getOctaneLocation));
        mandatoryGetters.add(new Pair<>("octane.apiClientID", this::getOctaneApiClientID));
        mandatoryGetters.add(new Pair<>("octane.apiClientSecret", this::getOctaneApiClientSecret));
        mandatoryGetters.add(new Pair<>("gitlab.location", this::getGitlabLocation));
        mandatoryGetters.add(new Pair<>("gitlab.personalAccessToken", this::getGitlabPersonalAccessToken));
        Set<String> validationErrors = new LinkedHashSet<>();
        mandatoryGetters.forEach(mg -> {
            if (mg.getValue().get() == null || mg.getValue().get().trim().isEmpty()) {
                validationErrors.add("Missing property " + mg.getKey());
            }
        });

        List<NameValuePair> params = URLEncodedUtils.parse(new URI(octaneLocation), Charset.forName("UTF-8"));
        Optional<NameValuePair> sharedspace = params.stream().filter(p -> p.getName().toLowerCase().equals("p")).findFirst();
        if (!sharedspace.isPresent()) {
            validationErrors.add("Missing 'p' query parameter in octane.location");
        } else {
            octaneSharedspace = sharedspace.get().getValue();
        }

        int contextPos = octaneLocation.indexOf("/ui");
        if (contextPos < 0) {
            validationErrors.add("Missing /ui path in octane.location");
        } else {
            octaneLocation = octaneLocation.substring(0, contextPos);
        }

        if (validationErrors.size() > 0) {
            AtomicInteger counter = new AtomicInteger(1);
            throw new ValidationException(validationErrors.stream().map(e -> (counter.getAndIncrement() + ": " + e)).collect(Collectors.joining("\n", "\n", "")));
        }
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public String getCiServerIdentity() {
        String val = ciServerIdentity != null ? ciServerIdentity : Hex.encodeHexString(DigestUtils.md5Digest(serverBaseUrl.getBytes()));
        return val.substring(0, Math.min(255, val.length()));
    }

    public String getOctaneLocation() {
        return octaneLocation;
    }

    public String getOctaneSharedspace() {
        return octaneSharedspace;
    }

    public String getOctaneApiClientID() {
        return octaneApiClientID;
    }

    public String getOctaneApiClientSecret() {
        return octaneApiClientSecret;
    }

    public String getGitlabLocation() {
        return gitlabLocation;
    }

    public String getGitlabPersonalAccessToken() {
        return gitlabPersonalAccessToken;
    }

    public String getGitlabTestResultsFilePattern() {
        return gitlabTestResultsFilePattern;
    }

    public String getProxyField(String protocol, String fieldName) {
        Optional<Field> field = Arrays.stream(this.getClass().getDeclaredFields()).filter(f -> f.getName().toLowerCase().equals(protocol.concat(fieldName).toLowerCase())).findFirst();
        if (!field.isPresent()) {
            throw new IllegalArgumentException(String.format("%s.%s", protocol, fieldName));
        }
        try {
            Object value = field.get().get(this);
            return value != null ? value.toString() : null;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("%s.%s field in not accessible", protocol, fieldName));
        }
    }
}
