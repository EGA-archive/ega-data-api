/*
 * Copyright 2016 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author asenf
 */
public interface ResService {
    /*
             InputStream getSource(String sourceFormat,
                                            String sourceKey,
                                            String destintionFormat,
                                            String destinationKey,
                                            String fileLocation,
                                            long startCoordinate,
                                            long endCoordinate);
    */
    void transfer(String sourceFormat,
                  String sourceKey,
                  String destinationFormat,
                  String destinationKey,
                  String destinationIV,
                  String fileLocation,
                  long startCoordinate,
                  long endCoordinate,
                  long fileSize,
                  String httpAuth,
                  String id,
                  HttpServletRequest request,
                  HttpServletResponse response);
}
