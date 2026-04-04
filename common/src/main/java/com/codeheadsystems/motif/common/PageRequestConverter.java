package com.codeheadsystems.motif.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Converts {@link PageRequest} to and from a signed JWT string representation.
 */
@Singleton
public final class PageRequestConverter {

  private static final String PAGE_NUMBER_CLAIM = "pn";
  private static final String PAGE_SIZE_CLAIM = "ps";

  private final Supplier<SecretKey> keySupplier;

  @Inject
  public PageRequestConverter(final Supplier<SecretKey> keySupplier) {
    this.keySupplier = keySupplier;
  }

  /**
   * Encodes a {@link PageRequest} as a signed JWT string.
   */
  public String encode(PageRequest pageRequest) {
    try {
      JWTClaimsSet claims = new JWTClaimsSet.Builder()
          .claim(PAGE_NUMBER_CLAIM, pageRequest.pageNumber())
          .claim(PAGE_SIZE_CLAIM, pageRequest.pageSize())
          .build();
      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      JWSSigner signer = new MACSigner(keySupplier.get());
      signedJWT.sign(signer);
      return signedJWT.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign page token", e);
    }
  }

  /**
   * Decodes a signed JWT string back into a {@link PageRequest}.
   *
   * @throws IllegalArgumentException if the token is invalid or the signature does not verify.
   */
  public PageRequest decode(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWSVerifier verifier = new MACVerifier(keySupplier.get());
      if (!signedJWT.verify(verifier)) {
        throw new IllegalArgumentException("Invalid page token: signature verification failed");
      }
      JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
      int pageNumber = claims.getIntegerClaim(PAGE_NUMBER_CLAIM);
      int pageSize = claims.getIntegerClaim(PAGE_SIZE_CLAIM);
      return new PageRequest(pageNumber, pageSize);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (ParseException | JOSEException e) {
      throw new IllegalArgumentException("Invalid page token", e);
    }
  }
}
