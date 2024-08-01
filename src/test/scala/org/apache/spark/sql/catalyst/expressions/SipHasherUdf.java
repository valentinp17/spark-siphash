package org.apache.spark.sql.catalyst.expressions;

import org.apache.spark.sql.api.java.UDF2;

import java.nio.charset.StandardCharsets;


/**
 * Udf how it used before rewriting to Expression.
 * For performance testing
 */
public class SipHasherUdf implements UDF2<String, String, Long> {
    private static final long serialVersionUID = 1L;

    public SipHasherUdf() {
    }

    public Long call(String key, String data) throws Exception {
        return SipHasher.hash(key.getBytes(StandardCharsets.UTF_8), data.getBytes(StandardCharsets.UTF_8));
    }
}
