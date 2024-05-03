package com.example.core;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@Controller
public class CoreApplication {
	RestTemplate restTemplate = new RestTemplate();

	public static void main(String[] args) {
		SpringApplication.run(CoreApplication.class, args);
	}

	@GetMapping("/")
	public String index(@RequestParam(name = "queue", defaultValue = "default") String queue,
						@RequestParam(name = "user_id") Long userId,
						HttpServletRequest request) {

		var cookies = request.getCookies();
		var cookieName = "user-queue-%s-token".formatted(queue);

		String token = "";
		if (cookies != null) {
			var cookie = Arrays.stream(cookies).filter(i-> i.getName().equalsIgnoreCase(cookieName)).findFirst();
			token = cookie.orElse(new Cookie(cookieName, "")).getValue();
		}

		// 허용된 유저인지 검사
		var uri = UriComponentsBuilder
				.fromUriString("http://127.0.0.1:9010")
				.path("/api/v1/queue/allowed")
				.queryParam("queue", queue)
				.queryParam("user_id", userId)
				.queryParam("token", token)
				.encode()
				.build()
				.toUri();

		ResponseEntity<AllowedUserResponse> response = restTemplate.getForEntity(uri, AllowedUserResponse.class);

		// 허용되지 않는다면 대기페이지로
		if(response.getBody() == null || !response.getBody().allowed()) {
			log.info("now allowed");
			return "redirect:http://127.0.0.1:9010/waiting-room?user_id=%d&redirect_url=%s".formatted(
					userId, "http://127.0.0.1:9000?user_id=%d".formatted(userId));
		}

		// 허용 상태, 해당 페이지 진입
		return "index";
	}

	public record AllowedUserResponse(Boolean allowed) {
	}
}
