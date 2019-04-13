package com.abugrov.helpinghand.config;

import com.cloudinary.Cloudinary;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Value("${upload.path}")
    private String uploadPath;
    @Value("${cloudinary.cloud_name}")
    private String cloud_name;
    @Value("${cloudinary.api_key}")
    private String api_key;
    @Value("${cloudinary.api_secret}")
    private String api_secret;
    @Value("${hostname}")
    private String host;

    @Bean
    public RestTemplate getRestTemplate() {
        String fixieUrl = System.getenv("FIXIE_URL");

        String[] fixieValues = fixieUrl.split("[/(:\\/@)/]+");
        String fixieUser = fixieValues[1];
        String fixiePassword = fixieValues[2];
        String fixieHost = fixieValues[3];
        int fixiePort = Integer.parseInt(fixieValues[4]);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        Authenticator proxyAuthenticator = (route, response) -> {
            String credential = Credentials.basic(fixieUser, fixiePassword);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        };

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(fixieHost, fixiePort));
        clientBuilder.proxy(proxy).proxyAuthenticator(proxyAuthenticator);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setProxy(proxy);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public Cloudinary getCloudinary() {
        return new Cloudinary("cloudinary://" + api_key + ":" + api_secret + "@" + cloud_name);
    }

    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**")
                .addResourceLocations(uploadPath);
    }
}