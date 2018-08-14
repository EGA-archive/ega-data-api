/*
 * Copyright 2017 ELIXIR EGA
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
package eu.elixir.ega.ebi.dataedge.service;

import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author asenf
 */
public interface TicketService {

    Object getTicket(Authentication auth,
                     String fileId,
                     String format,
                     int referenceIndex,
                     String referenceName,
                     String referenceMD5,
                     String start,
                     String end,
                     List<String> fields,
                     List<String> tags,
                     List<String> notags,
                     HttpServletRequest request,
                     HttpServletResponse response);

    Object getVariantTicket(Authentication auth,
                            String fileId,
                            String format,
                            int referenceIndex,
                            String referenceName,
                            String referenceMD5,
                            String start,
                            String end,
                            List<String> fields,
                            List<String> tags,
                            List<String> notags,
                            HttpServletRequest request,
                            HttpServletResponse response);
    
//    Object getDirectTicket(Authentication auth,
//                           String file_id,
//                           String format,
//                           int referenceIndex,
//                           String referenceName,
//                           String referenceMD5,
//                           String start,
//                           String end,
//                           List<String> fields,
//                           List<String> tags,
//                           List<String> notags,
//                           HttpServletRequest request,
//                           HttpServletResponse response);

}
