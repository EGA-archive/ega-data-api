package eu.elixir.ega.ebi.keyproviderservice.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.dto.PublicKey;

/**
 * Test class for {@link MyCipherConfig}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class MyCipherConfigTest {

    public final static Long KEY_ID = new Long("-6324474004599780284");
    public static ClassLoader classLoader;
    public static File file;
    public static File filePassPath;
    public static File fileOutput;

    @Autowired
    MyCipherConfig myCipherConfig;

    @MockBean
    private RestTemplate restTemplate;

    /**
     * Test {@link MyCipherConfig#getPrivateKey(Long)}. Verify the PGPPrivateKey
     * keyid.
     */
    @Test
    public void testGetPrivateKey() {
        PGPPrivateKey pgpPrivateKeys = myCipherConfig.getPrivateKey(KEY_ID);
        assertThat(pgpPrivateKeys.getKeyID(), equalTo(KEY_ID));
    }

    /**
     * Test {@link MyCipherConfig#getPublicKey(Long)}. Verify the PGPPrivateKey
     * keyid.
     */
    @Test
    public void testGetPublicKey() {
        PGPPublicKey pgpPublicKey = myCipherConfig.getPublicKey(KEY_ID);
        assertThat(pgpPublicKey.getKeyID(), equalTo(KEY_ID));
    }

    /**
     * Test {@link MyCipherConfig#getKeyPaths(Long)}. Verify the keyPath and
     * keyPassPath.
     */
    @Test
    public void testGetKeyPaths() {
        KeyPath keyPath = myCipherConfig.getKeyPaths(KEY_ID);
        assertThat(keyPath.getKeyPath(), equalTo(file.getAbsolutePath()));
        assertThat(keyPath.getKeyPassPath(), equalTo(filePassPath.getAbsolutePath()));

    }

    /**
     * Test {@link MyCipherConfig#getKeyPaths(Long)}. Verify the asciiArmouredKey
     * not null. We cannot check the content of asciiArmouredKey as it is
     * reArmoured.
     */
    @Test
    public void testGetAsciiArmouredKeys() {
        String asciiArmouredKey = myCipherConfig.getAsciiArmouredKey(KEY_ID);
        assertNotNull(asciiArmouredKey);
    }

    /**
     * Test {@link MyCipherConfig#getKeyPaths(Long)}. Verify size of the keyIds.
     */
    @Test
    public void testGetKeyIDs() {
        Set<Long> keyIDs = myCipherConfig.getKeyIDs();
        assertThat(keyIDs.iterator().next(), equalTo(KEY_ID));
        assertThat(keyIDs.size(), equalTo(1));
    }

    /**
     * Test {@link MyCipherConfig#readFileAsString(String)}. Verify size of the
     * privateKey read from file is same as expected.
     */
    @Test
    public void testReadFileAsString() throws IOException {
        String privateKey = myCipherConfig.readFileAsString(file.getAbsolutePath());
        assertThat(privateKey, equalTo(getPrivateKey()));
    }

    /**
     * Test {@link MyCipherConfig#extractKey(String, String)}. Verify the
     * pgpPrivateKey id.
     */
    @Test
    public void testExtractKey() throws IOException {
        PGPPrivateKey pgpPrivateKey = myCipherConfig.extractKey(getPrivateKey(), getPassphrase());
        assertThat(pgpPrivateKey.getKeyID(), equalTo(KEY_ID));
    }

    /**
     * Test {@link MyCipherConfig#getPublicKeyById(String)}. Verify the
     * publicKeyArmored.
     */
    @Test
    public void testGetPublicKeyById() throws IOException {
        String armoredString = "test";
        PublicKey mockPublicKey = mock(PublicKey.class);
        @SuppressWarnings("unchecked")
        ResponseEntity<Object> publicKeyMock = mock(ResponseEntity.class);

        when(publicKeyMock.getBody()).thenReturn(mockPublicKey);
        when(mockPublicKey.getPublicKeyArmored()).thenReturn(armoredString);
        when(restTemplate.getForEntity(any(String.class), any())).thenReturn(publicKeyMock);

        String publicKeyArmored = myCipherConfig.getPublicKeyById("id");
        assertThat(publicKeyArmored, equalTo(armoredString));
    }

    /**
     * Test {@link MyCipherConfig#getPublicKeyByEmail(String)}. Verify the
     * publicKeyArmored.
     */
    @Test
    public void testGetPublicKeyByEmail() throws IOException {
        String armoredString = "test";
        PublicKey mockPublicKey = mock(PublicKey.class);
        ResponseEntity<Object> publicKeyMock = mock(ResponseEntity.class);

        when(publicKeyMock.getBody()).thenReturn(mockPublicKey);
        when(mockPublicKey.getPublicKeyArmored()).thenReturn(armoredString);
        when(restTemplate.getForEntity(any(String.class), any())).thenReturn(publicKeyMock);

        String publicKeyArmored = myCipherConfig.getPublicKeyByEmail("email");
        assertThat(publicKeyArmored, equalTo(armoredString));
    }

    private String getPublicKey() {
        return "-----BEGIN PGP PUBLIC KEY BLOCK-----\r\n" + "\r\n"
                + "mQENBFtfBO4BCACoaIvZtXfOATCz/xzQY3tAekuXmQXiPRwqfKBoOXHQv9mYeNWN\r\n"
                + "zI62es7TxevWzjeDvhSYy5bd9cAv1Ia+8soP1CyqxGwTDEg+8TILn5LCe3NtZimY\r\n"
                + "oIxMjeqHoixuI0CofTEB1oS/24YtWrBxCTF1k/XUtCBx0aYMDmNVjF53LXzDe6Ka\r\n"
                + "D7kb7E6Tc/3C5ddoCKV3D0goYosMEdK/3aFGEFyxQvmXQ8UPy7xHvXbOy2NZ8C8Y\r\n"
                + "Xa+dFh+mK2ahZUH+L3NkUX79Gn/3lPBLA2Mf13DLGsnOXHjAC/d1W5XSb8vFANx9\r\n"
                + "/ZtRC34BV/7FIW7ogk0nK5yqL1xvELPlagFvABEBAAG0HWFuYW5kbW9oYW4gPGFt\r\n"
                + "b2hhbkBlYmkuYWMudWs+iQE3BBMBCAAhBQJbXwTuAhsDBQsJCAcCBhUICQoLAgQW\r\n"
                + "AgMBAh4BAheAAAoJEKg69Ga8o0xEfQoH/iZebak+4xpVjyFPrDOKTao6yl6CChtu\r\n"
                + "0nmoEYSrgXCF+9ELcDFKxyAjUKF/+tuo3PZDjsw4I12pRnJU+mieXlisELjfbQxu\r\n"
                + "y66qVIlQ8L3Q+okHHfCyYmcV7buErmnKiZza/s5IgnQ+kz4bsYXPzenXNbr0loXd\r\n"
                + "WvpJIdvcErLMQFUuTJ21WlWcVJV9tUwRGNbZIMQ8DvUooSvXiy6JSYNXIKj1e0KN\r\n"
                + "NtZ60OUE3NU8ghQV3ZKeUN1ta/3G2suEvWxam4aTJY4zIQ8+EmaU2gz1xnD0HnEk\r\n"
                + "eOwWUXHOsp6kMzhhATEB3Beixy410D6r4c+QQOpj6efcCW3MjP581da5AQ0EW18E\r\n"
                + "7gEIANwXA53AaZfDbZwUZfJkpJJtP6B2xBmy6E5OwWH3JQ/VPazuNlzWsx1Z1yEk\r\n"
                + "zWf1GKY/lCB6ynPyF+hS5M3Z07IMjhaiAlsFD101xbrxAvIgfyEBcSbUmNLE+Kc0\r\n"
                + "NzETdhmOS0EgiRWlB7qZn+vnfGf6w/hxGMwfP4CR5iQ5tlE6DRThbiQvfqNi75wA\r\n"
                + "mEuxHEKI2UpVVVkYcdpYeweOfLCAkSCkXGJXur8O1DYCdOTVdNkUR+4dM+paouDq\r\n"
                + "hc7MYfAvfY4DFrih4QQESIYO3FnRGRwCbbjJU2cp4PXzpzO4ksqwminENltw6OZE\r\n"
                + "WWBA4RMHTL1J3I8sYPIyl3Ks4nMAEQEAAYkBHwQYAQgACQUCW18E7gIbDAAKCRCo\r\n"
                + "OvRmvKNMRJG9B/4vMMEJVDpQ2sfgJOPluFxq+vS9I9jBD2WeKPBNboRU7JxVcjnG\r\n"
                + "rarUEAV7AXuY9eQ9rFKw1h8JyfoA6l54hQYymhz8qIwg9d1DGnc26kf8VZd9Cq51\r\n"
                + "9cRxcHJ4cL6pUkPSPdvVkHHRraEfIVpFLzPKFH1x7I+K3V73m8HX6tWsRi2TgaPh\r\n"
                + "Q1wDtwulv4OktN+HE7Bbqxt5wKNVpgBqR8SNCA/DjSYqIEbK7w3O5XQVpx8xVHzH\r\n"
                + "KDGgKFj6X0igxGfBmQY5HeMKG9UG5k7aMej/lH8PU09JONlT1iqzYMe/O/VOYSSe\r\n"
                + "m7MErecMWSm89D60y4vVR2U1gCRfApU8rSkq\r\n" + "=L5fP\r\n" + "-----END PGP PUBLIC KEY BLOCK-----\r";
    }

    private String getPrivateKey() {
        return "-----BEGIN PGP PRIVATE KEY BLOCK-----\r\n" + "\r\n"
                + "lQPGBFtfBO4BCACoaIvZtXfOATCz/xzQY3tAekuXmQXiPRwqfKBoOXHQv9mYeNWN\r\n"
                + "zI62es7TxevWzjeDvhSYy5bd9cAv1Ia+8soP1CyqxGwTDEg+8TILn5LCe3NtZimY\r\n"
                + "oIxMjeqHoixuI0CofTEB1oS/24YtWrBxCTF1k/XUtCBx0aYMDmNVjF53LXzDe6Ka\r\n"
                + "D7kb7E6Tc/3C5ddoCKV3D0goYosMEdK/3aFGEFyxQvmXQ8UPy7xHvXbOy2NZ8C8Y\r\n"
                + "Xa+dFh+mK2ahZUH+L3NkUX79Gn/3lPBLA2Mf13DLGsnOXHjAC/d1W5XSb8vFANx9\r\n"
                + "/ZtRC34BV/7FIW7ogk0nK5yqL1xvELPlagFvABEBAAH+BwMCVAfHEd1L4JTpjQ5u\r\n"
                + "yzv67mdGxX15P4pznzoGu8NDSAc6+Ps3XS3tdmVwoqihUj/Oyi1Z9J77diyYKOV8\r\n"
                + "J/We5ekPYN9Uko8okzDDRc+E+4QZtK0K3MAxVACYSqcfnpuuH5DBsTw44YdGZiG5\r\n"
                + "lD7Ipx/xslnBxnpelZxK8QohKl+3X2sk0CVyeSZI8p2DR/AHMR+U1DSPZaVi+aeL\r\n"
                + "RGbVCy7mWoTFqE+XyynFJOJSFoq4kf4Iw3Z6I6RW3uXp26OyIRjAVU+GMTSnnv+p\r\n"
                + "b2bMLJ8m15/jmU5g7jwULuXNLbk7+odo1ibP9c+sHN79E7y33l7Pg5rpp2gWtb2u\r\n"
                + "vxrBacoDv24MlwYjimJoSdNxpBsTXWTnUuvMtS/+kFC0nLuk0VzDrwv76WJ+rTEh\r\n"
                + "rXCUnmICBTLJYSYE1QfoSrO6vq2mfdFqoR2vrbpnpS7dIuzfCsg4OdknXxJS5pyc\r\n"
                + "kuBc3z3+llFwPSJ6mPST6Kdm+TRoTO5RAoEBTKObz3MNHyDe+3pUxAcZNcOKYc7H\r\n"
                + "XRaxFwmKRb3K1wW0Ua5ZLtSfhPFZ5+RhmH2kJIIGS2FhNYUVUMTGa6RcJFPAehNq\r\n"
                + "2r1E2Fj9CRvjImojoLH6xdHqoCETJBX8ziI0LnjDYPYM+teGVFxK/e8mNXQ67RsF\r\n"
                + "sT5hiQ/HYIbOM684VJU8XP/Ef4NY82aW97UlW0sh4L8cRNXuxFfeJWwRPYHVMf4g\r\n"
                + "52jpLe/Ep7PLfQjbaP4D7JgbVg98xeACgpRTELFNFrpFRBdN/fnHaaxZBPEZr+Ds\r\n"
                + "s69KmDULTHjxHa6Qn108+HNtyaNNVWhCRbInNXtERWPlLGas36Y9PgJFu4bnb9KK\r\n"
                + "Ez96lADmgqAReywanCAM8uTQlyj8wFDc+MG6oKcQVgmTLJLQ00cAahef3c3s9aFb\r\n"
                + "XeunHQzcg5iRtB1hbmFuZG1vaGFuIDxhbW9oYW5AZWJpLmFjLnVrPokBNwQTAQgA\r\n"
                + "IQUCW18E7gIbAwULCQgHAgYVCAkKCwIEFgIDAQIeAQIXgAAKCRCoOvRmvKNMRH0K\r\n"
                + "B/4mXm2pPuMaVY8hT6wzik2qOspeggobbtJ5qBGEq4FwhfvRC3AxSscgI1Chf/rb\r\n"
                + "qNz2Q47MOCNdqUZyVPponl5YrBC4320MbsuuqlSJUPC90PqJBx3wsmJnFe27hK5p\r\n"
                + "yomc2v7OSIJ0PpM+G7GFz83p1zW69JaF3Vr6SSHb3BKyzEBVLkydtVpVnFSVfbVM\r\n"
                + "ERjW2SDEPA71KKEr14suiUmDVyCo9XtCjTbWetDlBNzVPIIUFd2SnlDdbWv9xtrL\r\n"
                + "hL1sWpuGkyWOMyEPPhJmlNoM9cZw9B5xJHjsFlFxzrKepDM4YQExAdwXoscuNdA+\r\n"
                + "q+HPkEDqY+nn3AltzIz+fNXWnQPGBFtfBO4BCADcFwOdwGmXw22cFGXyZKSSbT+g\r\n"
                + "dsQZsuhOTsFh9yUP1T2s7jZc1rMdWdchJM1n9RimP5Qgespz8hfoUuTN2dOyDI4W\r\n"
                + "ogJbBQ9dNcW68QLyIH8hAXEm1JjSxPinNDcxE3YZjktBIIkVpQe6mZ/r53xn+sP4\r\n"
                + "cRjMHz+AkeYkObZROg0U4W4kL36jYu+cAJhLsRxCiNlKVVVZGHHaWHsHjnywgJEg\r\n"
                + "pFxiV7q/DtQ2AnTk1XTZFEfuHTPqWqLg6oXOzGHwL32OAxa4oeEEBEiGDtxZ0Rkc\r\n"
                + "Am24yVNnKeD186czuJLKsJopxDZbcOjmRFlgQOETB0y9SdyPLGDyMpdyrOJzABEB\r\n"
                + "AAH+BwMCCXA/ksWfuFPpda9KVH0cBMDo6UudC2pjBes6mTPLyMDEeZx08YKrP7r4\r\n"
                + "h/h+zN6RiRJsey+1pTbaBLzEIi1H9l+sl/7YODYO6DvKCqhv30jmojvaS02ypceW\r\n"
                + "eGSPhoEX2/i+AApKpJjreaXEuRPuqd2k/uqJqtU9gPNXHcbuEFkLdWjAWMjy5laL\r\n"
                + "LO1HoAm317TmOlxUbuH5KT+ATV/cBnzm2ikB4u2K1I4MjQgohINm6egL/38R8FtG\r\n"
                + "JKFbP96jCKChB1OP9HCq8DHm7rh7VwsM8Tq+vK/OxiH6IGCgYpnsoRQfeRTm8YQ5\r\n"
                + "YHhrRRhNxniiUgZEa/8/eXgu15PTML63kKEP+20lndt2AggaXsK36KNiPA/iK+HG\r\n"
                + "0MEy6bF9cYQ2Mb2fP7W+WytVAcZLQvEanUNYihLHIgV2Tk14OiJOCRvqIUdWlOeH\r\n"
                + "sncXF+orLJI3fq0lg7wMjUlCJyvemVE7IKuoyL1dz4ESqP/sVwawyEDhsczpZZGR\r\n"
                + "uap5H//y/mmpeGqv97DtzX5rDKf5Ykd75LgSg5EYS8EHnqYl0AWQ90etSbt4WdE5\r\n"
                + "Y/aW1229Xm7xx1CsvR9/X3XhvDUutxQHcROForVBgboJgX+1rucAtrULvuK02LXN\r\n"
                + "n/D/OOwPKhLLbg49B5L3uM6UIOFF2fgwOp7jB2MBcN/msNj18pwFes8s6GADC4hW\r\n"
                + "nA9P6yyz3kZDKJlti0s5vPPTKnrtJKuK6pSwi4b1KEeuwxTKiWdwe70YjQJi9oF8\r\n"
                + "clIZaXekpQtQqGCrUA5cuT/zYvlhk6TSGe8qg4eGPuNnPL+CeFk0YU78gslTfcOY\r\n"
                + "HB67VYyREZ+qZHhzsSY2UHLK6L3PfLVJDwXkszbaUW0h1mAq7t+NVAzbYf3JvUuE\r\n"
                + "FEbeY+eJetUe9+dE2jL7agwTBWTE16yPMo/6iQEfBBgBCAAJBQJbXwTuAhsMAAoJ\r\n"
                + "EKg69Ga8o0xEkb0H/i8wwQlUOlDax+Ak4+W4XGr69L0j2MEPZZ4o8E1uhFTsnFVy\r\n"
                + "OcatqtQQBXsBe5j15D2sUrDWHwnJ+gDqXniFBjKaHPyojCD13UMadzbqR/xVl30K\r\n"
                + "rnX1xHFwcnhwvqlSQ9I929WQcdGtoR8hWkUvM8oUfXHsj4rdXvebwdfq1axGLZOB\r\n"
                + "o+FDXAO3C6W/g6S034cTsFurG3nAo1WmAGpHxI0ID8ONJiogRsrvDc7ldBWnHzFU\r\n"
                + "fMcoMaAoWPpfSKDEZ8GZBjkd4wob1QbmTtox6P+Ufw9TT0k42VPWKrNgx7879U5h\r\n"
                + "JJ6bswSt5wxZKbz0PrTLi9VHZTWAJF8ClTytKSo=\r\n" + "=H1Pf\r\n"
                + "-----END PGP PRIVATE KEY BLOCK-----\r\n";
    }

    private String getPassphrase() {
        return "anandmohan777";
    }

    @TestConfiguration
    static class MyCipherConfigTestContextConfiguration {
        @Bean
        public MyCipherConfig myCipher() {
            classLoader = getClass().getClassLoader();
            file = new File(classLoader.getResource("sample.txt").getFile());
            filePassPath = new File(classLoader.getResource("pass.txt").getFile());
            fileOutput = new File(classLoader.getResource("output.txt").getFile());
            String[] keyPath = { file.getAbsolutePath() };
            String[] keyPassPath = { filePassPath.getAbsolutePath() };
            return new MyCipherConfig(keyPath, keyPassPath, fileOutput.getAbsolutePath(), "publicKeyUrl",
                    fileOutput.getAbsolutePath());
        }
    }
}
