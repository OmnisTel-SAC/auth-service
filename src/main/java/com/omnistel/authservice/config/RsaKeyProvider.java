package com.omnistel.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
public class RsaKeyProvider {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public RsaKeyProvider(
            @Value("${spring.security.jwt.private-key}") Resource privateKeyResource,
            @Value("${spring.security.jwt.public-key}") Resource publicKeyResource) {
        try (InputStream is = privateKeyResource.getInputStream()) {
            this.privateKey = RsaKeyConverters.pkcs8().convert(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read RSA private key", e);
        }
        try (InputStream is = publicKeyResource.getInputStream()) {
            this.publicKey = RsaKeyConverters.x509().convert(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read RSA public key", e);
        }
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
}
