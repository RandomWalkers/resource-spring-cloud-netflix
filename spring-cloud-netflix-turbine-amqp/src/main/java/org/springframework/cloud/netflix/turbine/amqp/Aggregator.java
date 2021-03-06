/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.turbine.amqp;

import java.io.IOException;
import java.util.Map;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.StringUtils;

import rx.subjects.PublishSubject;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Spencer Gibb
 */
@MessageEndpoint
@CommonsLog
public class Aggregator {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PublishSubject<Map<String, Object>> subject;

	@ServiceActivator(inputChannel = "hystrixStreamAggregator")
	public void handle(String payload) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = this.objectMapper.readValue(payload, Map.class);
			Map<String, Object> data = getPayloadData(map);

			log.debug("Received hystrix stream payload: " + data);
			this.subject.onNext(data);
		}
		catch (IOException ex) {
			log.error("Error receiving hystrix stream payload: " + payload, ex);
		}
	}

	public static Map<String, Object> getPayloadData(Map<String, Object> jsonMap) {
		@SuppressWarnings("unchecked")
		Map<String, Object> origin = (Map<String, Object>) jsonMap.get("origin");
		String instanceId = null;
		if (origin.containsKey("id")) {
			instanceId = origin.get("id").toString();
		}
		if (!StringUtils.hasText(instanceId)) {
			// TODO: instanceid template
			instanceId = origin.get("serviceId") + ":" + origin.get("host") + ":"
					+ origin.get("port");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
		data.put("instanceId", instanceId);
		return data;
	}

}
