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

package org.opengroup.osdu.indexer.provider.gcp.indexing.config;

import java.util.Arrays;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.indexer.IndexerApplication;
import org.opengroup.osdu.indexer.ServerletInitializer;
import org.opengroup.osdu.indexer.provider.gcp.web.config.WebAppMainContextConfiguration;
import org.opengroup.osdu.indexer.provider.gcp.web.security.GcpSecurityConfig;
import org.opengroup.osdu.indexer.swagger.SwaggerConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@RequiredArgsConstructor
@ComponentScan(
    value = {"org.opengroup.osdu"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            value = {
                WebAppMainContextConfiguration.class,
                IndexerApplication.class,
                ServerletInitializer.class,
                SwaggerConfiguration.class,
                GcpSecurityConfig.class,
                SecurityAutoConfiguration.class,
                ManagementWebSecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
            })
    })
public class CustomContextConfiguration {

    private final ApplicationContext applicationContext;

    @PostConstruct
    public void setUp() {
        log.debug("Messaging context initialized with id: {}.", applicationContext.getId());
        log.debug("Messaging context status: {}.", applicationContext);
        String[] allBeansNames = applicationContext.getBeanDefinitionNames();
        log.debug("Messaging context beans definitions: {}.", Arrays.toString(allBeansNames));
    }
}
