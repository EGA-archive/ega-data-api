package eu.elixir.ega.ebi.commons.config;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import eu.elixir.ega.ebi.commons.shared.service.FileDatasetService;
import eu.elixir.ega.ebi.commons.shared.service.Ga4ghService;
import eu.elixir.ega.ebi.commons.shared.service.JWTService;
import eu.elixir.ega.ebi.commons.shared.service.UserDetailsService;

@RunWith(SpringRunner.class)
public class MyAccessTokenConverterTest {

	private MyAccessTokenConverter accessTokenConverter;

	@MockBean
	private JWTService jwtService;

	@MockBean
	private Ga4ghService ga4ghService;

	@MockBean
	private FileDatasetService fileDatasetService;

	@MockBean
	private UserDetailsService userDetailsService;

	private final String dataset1 = "EGAD00001";
	private final String dataset2 = "EGAD00002";

	@Before
	public void before() {
		accessTokenConverter = new MyAccessTokenConverter(jwtService, ga4ghService, fileDatasetService,
				userDetailsService);
	}

	@Test
	public void getDatasetFileMap_whenPassDuplicateDatasets_thenReturnWithoutDuplicates()
			throws JsonProcessingException, IOException, ParseException, URISyntaxException {
		commonMock();

		Map<String, List<String>> userDatasets = accessTokenConverter.getDatasetFileMap(get3Ga4GhDatasets());
		assertEquals(2, userDatasets.size());
		assertTrue(userDatasets.containsKey(dataset1));
		assertTrue(userDatasets.containsKey(dataset2));
	}

	@Test
	public void getDatasetFileMap_whenPassNoDatasets_thenReturnZeroDatasets()
			throws JsonProcessingException, IOException, ParseException, URISyntaxException {
		commonMock();

		Map<String, List<String>> userDatasets = accessTokenConverter.getDatasetFileMap(new ArrayList<>());
		assertEquals(0, userDatasets.size());
	}

	private List<String> get3Ga4GhDatasets() {
		List<String> ga4ghDatasets = new ArrayList<>();
		ga4ghDatasets.add("header1." + dataset1 + ".signature1");
		ga4ghDatasets.add("header2." + dataset2 + ".signature2");
		ga4ghDatasets.add("header3." + dataset2 + ".signature3");
		return ga4ghDatasets;
	}

	private SignedJWT getSignedJWT(String dataset)
			throws JsonProcessingException, IOException, ParseException, URISyntaxException {
		ObjectMapper mapper = new ObjectMapper();
		String PassportVisaObjectJson = "{ \"type\":\"ControlledAccessGrants\", \"value\":\"" + dataset + "\" }";
		String json = "{ \"exp\":" + (currentTimeMillis() + 9000) + " , \"ga4gh_visa_v1\":" + PassportVisaObjectJson + " }";
		JsonNode node = mapper.readTree(json);

		final JWTClaimsSet jwtClaimsSet = JWTClaimsSet.parse(node.toString());
		return new SignedJWT(getJWSHeader(), jwtClaimsSet);
	}

	private JWSHeader getJWSHeader() throws URISyntaxException {
		return new JWSHeader.Builder(JWSAlgorithm.parse("RS256"))
				.keyID("test")
				.type(JOSEObjectType.JWT)
				.jwkURL(new URI("http://example.com/"))
				.build();
	}

	private void commonMock() throws IOException, ParseException, URISyntaxException {
		when(jwtService.parse(get3Ga4GhDatasets().get(0))).thenReturn(Optional.of(getSignedJWT(dataset1)));
		when(jwtService.parse(get3Ga4GhDatasets().get(1))).thenReturn(Optional.of(getSignedJWT(dataset2)));
		when(jwtService.parse(get3Ga4GhDatasets().get(2))).thenReturn(Optional.of(getSignedJWT(dataset2)));
		when(jwtService.isValidSignature(any())).thenReturn(true);
		when(fileDatasetService.getFileIds(anyString())).thenReturn(new ArrayList<>());
	}
}
