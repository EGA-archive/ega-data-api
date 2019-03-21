package eu.elixir.ega.ebi.it.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.jayway.restassured.response.Response;

public class Common {

    public static String getMd5DigestFromResponse(Response response) throws Exception { 
        final InputStream inputStream = response.body().asInputStream();

        final MessageDigest md = MessageDigest.getInstance("MD5");
        // Read Content for MD5
        final DigestInputStream dIn = new DigestInputStream(inputStream, md);

        // NullOutputStream
        final OutputStream nullOutputStream = new OutputStream() {
            @Override
            public void write(int b) {
            }

            @Override
            public void write(byte[] b) {
            }
        };

        // Read
        byte[] b = new byte[10 * 1024 * 1024];
        int s = dIn.read(b);
        while (s > -1) {
            nullOutputStream.write(b);
            s = dIn.read(b);
        }
        dIn.close();

        return getMd5DigestString(md);
    }

    public static String getMd5DigestString(MessageDigest md) {
        final BigInteger bigInt = new BigInteger(1, md.digest());
        String mDigest = bigInt.toString(16);
        while (mDigest.length() < 32) {
            mDigest = "0" + mDigest;
        }
        return mDigest;
    }

}
