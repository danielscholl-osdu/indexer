/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.util;

import com.google.common.base.Strings;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
@ToString
public class AnthosHTTPClient extends HTTPClient {

    public static final String INTEGRATION_TESTER_TOKEN = "ROOT_USER_TOKEN";
    private static String token = null;
    private static OpenIDTokenProvider openIDTokenProvider;

    public AnthosHTTPClient() {
        token = System.getProperty(INTEGRATION_TESTER_TOKEN, System.getenv(INTEGRATION_TESTER_TOKEN));

        if (Strings.isNullOrEmpty(token)) {
            openIDTokenProvider = new OpenIDTokenProvider();
        }
    }

    @Override
    public synchronized String getAccessToken() {
        if (Strings.isNullOrEmpty(token)) {
            token = openIDTokenProvider.getToken();
        }
        return "Bearer " + token;
    }
}
