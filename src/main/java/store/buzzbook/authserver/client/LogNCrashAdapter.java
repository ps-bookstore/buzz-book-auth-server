package store.buzzbook.authserver.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import store.buzzbook.authserver.dto.LogNCrashRequest;

@FeignClient(name = "LogNCrashAdapter", url = "${logncrash.url}")
public interface LogNCrashAdapter {

	@PostMapping("/v2/log")
	void sendLog(@RequestBody LogNCrashRequest request);
}
