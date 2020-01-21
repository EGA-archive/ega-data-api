/*
 * Copyright 2016 ELIXIR EBI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.ac.ebi.ega.fire.ingestion.service.IFireServiceNew;
import uk.ac.ebi.ega.fire.models.FireObjectResponse;
import uk.ac.ebi.ega.fire.models.FireResponse;

/**
 * Test class for {@link ArchiveAdapterServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ArchiveAdapterServiceImpl.class)
public class ArchiveAdapterServiceImplTest {

    private ArchiveAdapterServiceImpl archiveAdapterServiceImpl;

    @Mock 
    private IFireServiceNew fireService;
    
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for {@link ArchiveAdapterServiceImpl#getPath(String)}. Verify
     * output length and its values.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPath() throws Exception {

        final String pathInput = "EGAF00000094601.bam.cip";
        final Long object_length = 2556580787L;
        final String PATH_OBJECTS = "objects/blob/path/";
        final String fireURL = "https://hx.fire.sdo.ebi.ac.uk/fire/v1.1";
        archiveAdapterServiceImpl= new ArchiveAdapterServiceImpl(fireService, fireURL);
        FireObjectResponse fireObjectResponse = new FireObjectResponse();
        fireObjectResponse.setObjectSize(object_length);  

        when(fireService.findFile(anyString())).thenReturn(Optional.of(new FireResponse(fireObjectResponse)));

        final String[] ouput = archiveAdapterServiceImpl.getPath(pathInput, "");

        assertThat(ouput.length, equalTo(2));
        assertThat(ouput[0], equalTo(fireURL+PATH_OBJECTS + pathInput));
        assertThat(ouput[1], equalTo(object_length.toString()));

    }

}
